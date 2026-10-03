package com.trackflow.delivery.web.dto;

import java.time.Instant;
import java.util.UUID;

import com.trackflow.delivery.domain.Courier;
import com.trackflow.delivery.domain.Delivery;
import com.trackflow.delivery.domain.DeliveryStatus;
import com.trackflow.delivery.domain.VehicleType;

public record DeliveryResponse(
        UUID id,
        UUID orderId,
        String orderNumber,
        String customerUsername,
        String customerName,
        String customerPhone,
        String deliveryAddress,
        DeliveryStatus status,
        CourierSummary courier,
        String note,
        Instant createdAt,
        Instant updatedAt,
        Instant assignedAt,
        Instant pickedUpAt,
        Instant deliveredAt) {

    public record CourierSummary(UUID id, String name, String username, String phone, VehicleType vehicleType,
                                 Double latitude, Double longitude, Instant lastLocationAt) {

        static CourierSummary from(Courier c) {
            return c == null ? null : new CourierSummary(c.getId(), c.getName(), c.getUsername(), c.getPhone(), c.getVehicleType(),
                    c.getCurrentLatitude(), c.getCurrentLongitude(), c.getLastLocationAt());
        }
    }

    public static DeliveryResponse from(Delivery d) {
        return new DeliveryResponse(d.getId(), d.getOrderId(), d.getOrderNumber(), d.getCustomerUsername(), d.getCustomerName(),
                d.getCustomerPhone(), d.getDeliveryAddress(), d.getStatus(), CourierSummary.from(d.getCourier()),
                d.getNote(), d.getCreatedAt(), d.getUpdatedAt(), d.getAssignedAt(), d.getPickedUpAt(),
                d.getDeliveredAt());
    }
}
