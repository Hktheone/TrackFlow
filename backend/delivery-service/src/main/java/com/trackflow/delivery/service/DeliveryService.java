package com.trackflow.delivery.service;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.trackflow.delivery.domain.Courier;
import com.trackflow.delivery.domain.Delivery;
import com.trackflow.delivery.domain.DeliveryStatus;
import com.trackflow.delivery.domain.InvalidDeliveryTransitionException;
import com.trackflow.delivery.domain.LocationUpdate;
import com.trackflow.delivery.messaging.DeliveryStatusEvent;
import com.trackflow.delivery.messaging.OrderEvent;
import com.trackflow.delivery.repository.CourierRepository;
import com.trackflow.delivery.repository.DeliveryRepository;
import com.trackflow.delivery.repository.LocationUpdateRepository;
import com.trackflow.delivery.security.CurrentUser;
import com.trackflow.delivery.web.dto.DeliveryResponse;
import com.trackflow.delivery.web.dto.LocationPointResponse;

@Service
public class DeliveryService {

    private static final Logger log = LoggerFactory.getLogger(DeliveryService.class);

    /** Statuses a rider/admin may set directly; assignment and cancellation have their own paths. */
    private static final Set<DeliveryStatus> MANUAL_STATUSES =
            EnumSet.of(DeliveryStatus.PICKED_UP, DeliveryStatus.IN_TRANSIT, DeliveryStatus.DELIVERED, DeliveryStatus.FAILED);

    /** Statuses in which the rider is out on the job and their location is tracked. */
    private static final Set<DeliveryStatus> TRACKABLE_STATUSES =
            EnumSet.of(DeliveryStatus.ASSIGNED, DeliveryStatus.PICKED_UP, DeliveryStatus.IN_TRANSIT);

    private final DeliveryRepository deliveries;
    private final CourierRepository couriers;
    private final LocationUpdateRepository locations;
    private final ApplicationEventPublisher events;
    private final boolean autoAssign;

    public DeliveryService(DeliveryRepository deliveries, CourierRepository couriers,
                           LocationUpdateRepository locations, ApplicationEventPublisher events,
                           @Value("${trackflow.delivery.auto-assign:false}") boolean autoAssign) {
        this.deliveries = deliveries;
        this.couriers = couriers;
        this.locations = locations;
        this.events = events;
        this.autoAssign = autoAssign;
    }

    /**
     * Creates the delivery for a newly confirmed order. By default it then waits for the customer
     * (or an admin) to pick a rider; with auto-assign on, the longest-idle rider gets it straight away.
     * Safe to call twice for the same order (Kafka redelivery).
     */
    @Transactional
    public void createForConfirmedOrder(OrderEvent event) {
        if (deliveries.existsByOrderId(event.orderId())) {
            log.info("Delivery for order {} already exists; ignoring duplicate event", event.orderNumber());
            return;
        }
        Delivery delivery = deliveries.save(new Delivery(event.orderId(), event.orderNumber(), event.customerUsername(),
                event.customerName(), event.customerPhone(), event.deliveryAddress()));
        events.publishEvent(DeliveryStatusEvent.of(delivery, "Delivery created, awaiting a rider"));

        if (!autoAssign) {
            return;
        }
        couriers.findNextAvailable().ifPresentOrElse(
                courier -> {
                    delivery.assign(courier);
                    events.publishEvent(DeliveryStatusEvent.of(delivery, "Auto-assigned"));
                    log.info("Delivery for order {} auto-assigned to {}", event.orderNumber(), courier.getName());
                },
                () -> log.info("No rider free for order {}; waiting for manual assignment", event.orderNumber()));
    }

    /** Stops a delivery whose order was cancelled, provided the parcel hasn't been picked up yet. */
    @Transactional
    public void cancelForOrder(UUID orderId) {
        deliveries.findByOrderId(orderId).ifPresentOrElse(delivery -> {
            if (!delivery.getStatus().isBeforePickup()) {
                log.warn("Order {} was cancelled but its delivery is already {}; leaving it running",
                        delivery.getOrderNumber(), delivery.getStatus());
                return;
            }
            delivery.transitionTo(DeliveryStatus.CANCELLED, "Order cancelled");
            events.publishEvent(DeliveryStatusEvent.of(delivery, "Order cancelled"));
        }, () -> log.debug("Order {} cancelled before a delivery existed", orderId));
    }

    /** Admins see every delivery, customers those of their orders, riders those assigned to them. */
    @Transactional(readOnly = true)
    public List<DeliveryResponse> findAll(DeliveryStatus status, CurrentUser user) {
        List<Delivery> result;
        if (user.isAdmin()) {
            result = status == null
                    ? deliveries.findAllByOrderByCreatedAtDesc()
                    : deliveries.findByStatusOrderByCreatedAtDesc(status);
        } else {
            List<Delivery> own = user.isRider()
                    ? deliveries.findByCourierUsernameOrderByCreatedAtDesc(user.username())
                    : deliveries.findByCustomerUsernameOrderByCreatedAtDesc(user.username());
            result = own.stream().filter(d -> status == null || d.getStatus() == status).toList();
        }
        return result.stream().map(DeliveryResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public DeliveryResponse findById(UUID id, CurrentUser user) {
        return DeliveryResponse.from(loadVisible(id, user));
    }

    @Transactional(readOnly = true)
    public DeliveryResponse findByOrderId(UUID orderId, CurrentUser user) {
        return deliveries.findByOrderId(orderId)
                .filter(user::canView)
                .map(DeliveryResponse::from)
                .orElseThrow(() -> new NotFoundException("No delivery for order " + orderId));
    }

    @Transactional
    public DeliveryResponse assignCourier(UUID deliveryId, UUID courierId, CurrentUser user) {
        Delivery delivery = loadVisible(deliveryId, user);
        if (!user.canAssign(delivery)) {
            throw new AccessDeniedException("Only the customer who placed the order or an admin can assign a rider");
        }
        Courier courier = couriers.findById(courierId)
                .orElseThrow(() -> new NotFoundException("Rider " + courierId + " not found"));
        delivery.assign(courier);
        String by = user.isAdmin() ? "admin" : "customer";
        events.publishEvent(DeliveryStatusEvent.of(delivery, "Assigned by " + by));
        return DeliveryResponse.from(delivery);
    }

    @Transactional
    public DeliveryResponse updateStatus(UUID deliveryId, DeliveryStatus next, String note, CurrentUser user) {
        if (!MANUAL_STATUSES.contains(next)) {
            throw new InvalidDeliveryTransitionException(next + " cannot be set directly");
        }
        Delivery delivery = loadVisible(deliveryId, user);
        if (!user.canDrive(delivery)) {
            throw new AccessDeniedException("Only the assigned rider or an admin can update this delivery");
        }
        delivery.transitionTo(next, note);
        events.publishEvent(DeliveryStatusEvent.of(delivery, note));
        return DeliveryResponse.from(delivery);
    }

    @Transactional
    public LocationPointResponse recordLocation(UUID deliveryId, double latitude, double longitude, CurrentUser user) {
        Delivery delivery = loadVisible(deliveryId, user);
        if (!user.canDrive(delivery)) {
            throw new AccessDeniedException("Only the assigned rider or an admin can report this delivery's location");
        }
        if (!TRACKABLE_STATUSES.contains(delivery.getStatus()) || delivery.getCourier() == null) {
            throw new InvalidDeliveryTransitionException("Delivery is " + delivery.getStatus() + "; location can't be tracked");
        }
        Courier courier = delivery.getCourier();
        courier.moveTo(latitude, longitude);
        LocationUpdate update = locations.save(new LocationUpdate(delivery.getId(), courier.getId(), latitude, longitude));
        return LocationPointResponse.from(update);
    }

    @Transactional(readOnly = true)
    public List<LocationPointResponse> trail(UUID deliveryId, CurrentUser user) {
        loadVisible(deliveryId, user);
        return locations.findByDeliveryIdOrderByRecordedAtAscIdAsc(deliveryId).stream()
                .map(LocationPointResponse::from)
                .toList();
    }

    /** Someone else's delivery is reported as not found rather than forbidden, so ids can't be probed. */
    private Delivery loadVisible(UUID id, CurrentUser user) {
        return deliveries.findById(id)
                .filter(user::canView)
                .orElseThrow(() -> new NotFoundException("Delivery " + id + " not found"));
    }
}
