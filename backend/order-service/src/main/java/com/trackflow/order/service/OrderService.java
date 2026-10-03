package com.trackflow.order.service;

import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.trackflow.order.domain.Order;
import com.trackflow.order.domain.OrderItem;
import com.trackflow.order.domain.OrderStatus;
import com.trackflow.order.domain.OrderStatusHistory;
import com.trackflow.order.domain.StatusChangeSource;
import com.trackflow.order.messaging.DeliveryStatusEvent;
import com.trackflow.order.messaging.OrderEvent;
import com.trackflow.order.messaging.OrderEventType;
import com.trackflow.order.repository.OrderRepository;
import com.trackflow.order.repository.OrderStatusHistoryRepository;
import com.trackflow.order.security.CurrentUser;
import com.trackflow.order.web.dto.CreateOrderRequest;
import com.trackflow.order.web.dto.OrderResponse;
import com.trackflow.order.web.dto.StatusHistoryResponse;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private static final String ORDER_NUMBER_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final OrderRepository orders;
    private final OrderStatusHistoryRepository history;
    private final ApplicationEventPublisher events;

    public OrderService(OrderRepository orders, OrderStatusHistoryRepository history,
                        ApplicationEventPublisher events) {
        this.orders = orders;
        this.history = history;
        this.events = events;
    }

    @Transactional
    public OrderResponse create(CreateOrderRequest request, CurrentUser user) {
        List<OrderItem> items = request.items().stream()
                .map(i -> new OrderItem(i.productName(), i.quantity(), i.unitPrice()))
                .toList();
        Order order = orders.save(new Order(nextOrderNumber(), user.username(), request.customerName(),
                request.customerPhone(), request.deliveryAddress(), items));

        history.save(new OrderStatusHistory(order.getId(), order.getStatus(), StatusChangeSource.API, "Order placed"));
        events.publishEvent(OrderEvent.of(OrderEventType.ORDER_CREATED, order));
        log.info("Created order {}", order.getOrderNumber());
        return OrderResponse.from(order);
    }

    /** Admins see every order; everyone else sees only the orders they placed. */
    @Transactional(readOnly = true)
    public List<OrderResponse> findAll(OrderStatus status, CurrentUser user) {
        List<Order> result;
        if (user.isAdmin()) {
            result = status == null
                    ? orders.findAllByOrderByCreatedAtDesc()
                    : orders.findByStatusOrderByCreatedAtDesc(status);
        } else {
            result = status == null
                    ? orders.findByCustomerUsernameOrderByCreatedAtDesc(user.username())
                    : orders.findByCustomerUsernameAndStatusOrderByCreatedAtDesc(user.username(), status);
        }
        return result.stream().map(OrderResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse findById(UUID id, CurrentUser user) {
        return OrderResponse.from(load(id, user));
    }

    @Transactional(readOnly = true)
    public List<StatusHistoryResponse> history(UUID id, CurrentUser user) {
        load(id, user);
        return history.findByOrderIdOrderByChangedAtAscIdAsc(id).stream()
                .map(StatusHistoryResponse::from)
                .toList();
    }

    /** Confirming is what hands the order to delivery-service (via ORDER_CONFIRMED). */
    @Transactional
    public OrderResponse confirm(UUID id, CurrentUser user) {
        Order order = changeStatus(load(id, user), OrderStatus.CONFIRMED, StatusChangeSource.API, "Order confirmed");
        events.publishEvent(OrderEvent.of(OrderEventType.ORDER_CONFIRMED, order));
        return OrderResponse.from(order);
    }

    @Transactional
    public OrderResponse cancel(UUID id, String reason, CurrentUser user) {
        String note = reason == null || reason.isBlank() ? "Order cancelled" : "Order cancelled: " + reason;
        Order order = changeStatus(load(id, user), OrderStatus.CANCELLED, StatusChangeSource.API, note);
        events.publishEvent(OrderEvent.of(OrderEventType.ORDER_CANCELLED, order));
        return OrderResponse.from(order);
    }

    /**
     * Mirrors a delivery status onto the order. Duplicate, stale or out-of-lifecycle events are
     * logged and skipped rather than failing, so redelivery from Kafka is always safe.
     */
    @Transactional
    public void applyDeliveryStatus(DeliveryStatusEvent event) {
        Order order = orders.findById(event.orderId()).orElse(null);
        if (order == null) {
            log.warn("Ignoring delivery event {} for unknown order {}", event.eventId(), event.orderId());
            return;
        }

        OrderStatus target = DeliveryStatusMapping.toOrderStatus(event.status()).orElse(null);
        if (target == null) {
            log.debug("Delivery status {} has no order-level equivalent", event.status());
            return;
        }
        if (!order.getStatus().canTransitionTo(target)) {
            log.info("Skipping delivery status {} for order {}: already {}",
                    event.status(), order.getOrderNumber(), order.getStatus());
            return;
        }
        changeStatus(order, target, StatusChangeSource.DELIVERY_EVENT, describe(event));
    }

    private Order changeStatus(Order order, OrderStatus next, StatusChangeSource source, String note) {
        order.transitionTo(next);
        history.save(new OrderStatusHistory(order.getId(), next, source, note));
        log.info("Order {} -> {} ({})", order.getOrderNumber(), next, source);
        return order;
    }

    private static String describe(DeliveryStatusEvent event) {
        StringBuilder note = new StringBuilder("Delivery ").append(event.status().toLowerCase().replace('_', ' '));
        if (event.courierName() != null) {
            note.append(" · courier ").append(event.courierName());
        }
        if (event.note() != null && !event.note().isBlank()) {
            note.append(" · ").append(event.note());
        }
        return note.length() > 300 ? note.substring(0, 300) : note.toString();
    }

    /**
     * Loads an order the caller may act on. Someone else's order is reported as not found rather
     * than forbidden, so order ids can't be probed.
     */
    private Order load(UUID id, CurrentUser user) {
        return orders.findById(id)
                .filter(order -> user.canAccessOrderOf(order.getCustomerUsername()))
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    private String nextOrderNumber() {
        String candidate;
        do {
            StringBuilder sb = new StringBuilder("TF-");
            for (int i = 0; i < 6; i++) {
                sb.append(ORDER_NUMBER_ALPHABET.charAt(RANDOM.nextInt(ORDER_NUMBER_ALPHABET.length())));
            }
            candidate = sb.toString();
        } while (orders.existsByOrderNumber(candidate));
        return candidate;
    }
}
