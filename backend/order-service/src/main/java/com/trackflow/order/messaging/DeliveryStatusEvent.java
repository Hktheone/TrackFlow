package com.trackflow.order.messaging;

import java.time.Instant;
import java.util.UUID;

/**
 * Payload of the {@code delivery-status-events} topic, as published by delivery-service.
 * {@code status} is kept as a string so a new delivery status doesn't break deserialization here.
 */
public record DeliveryStatusEvent(
        UUID eventId,
        UUID deliveryId,
        UUID orderId,
        String status,
        UUID courierId,
        String courierName,
        String note,
        Instant occurredAt) {
}
