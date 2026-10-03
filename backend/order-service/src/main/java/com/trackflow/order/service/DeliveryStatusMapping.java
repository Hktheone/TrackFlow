package com.trackflow.order.service;

import java.util.Optional;

import com.trackflow.order.domain.OrderStatus;

/** Translates delivery-service's vocabulary into order statuses. */
final class DeliveryStatusMapping {

    private DeliveryStatusMapping() {
    }

    static Optional<OrderStatus> toOrderStatus(String deliveryStatus) {
        if (deliveryStatus == null) {
            return Optional.empty();
        }
        return switch (deliveryStatus) {
            case "ASSIGNED" -> Optional.of(OrderStatus.COURIER_ASSIGNED);
            case "PICKED_UP" -> Optional.of(OrderStatus.PICKED_UP);
            case "IN_TRANSIT" -> Optional.of(OrderStatus.IN_TRANSIT);
            case "DELIVERED" -> Optional.of(OrderStatus.DELIVERED);
            case "FAILED" -> Optional.of(OrderStatus.DELIVERY_FAILED);
            // PENDING_ASSIGNMENT keeps the order CONFIRMED; CANCELLED only ever follows an order cancellation.
            default -> Optional.empty();
        };
    }
}
