package com.trackflow.delivery.messaging;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Payload of the {@code order-events} topic, as published by order-service. */
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
}
