package com.trackflow.order.messaging;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.trackflow.order.domain.Order;

/**
 * Payload of the {@code order-events} topic. This record is the contract with delivery-service,
 * which keeps its own copy rather than sharing a library, so each service can evolve independently.
 */
public record OrderEvent(
        UUID eventId,
        OrderEventType eventType,
        UUID orderId,
        String orderNumber,
        String customerUsername,
        String customerName,
        String customerPhone,
        String deliveryAddress,
        BigDecimal totalAmount,
        String status,
        Instant occurredAt) {

    public static OrderEvent of(OrderEventType type, Order order) {
        return new OrderEvent(
                UUID.randomUUID(),
                type,
                order.getId(),
                order.getOrderNumber(),
                order.getCustomerUsername(),
                order.getCustomerName(),
                order.getCustomerPhone(),
                order.getDeliveryAddress(),
                order.getTotalAmount(),
                order.getStatus().name(),
                Instant.now());
    }
}
