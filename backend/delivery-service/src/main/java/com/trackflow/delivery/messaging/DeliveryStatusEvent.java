package com.trackflow.delivery.messaging;

import java.time.Instant;
import java.util.UUID;

import com.trackflow.delivery.domain.Courier;
import com.trackflow.delivery.domain.Delivery;

/** Payload of the {@code delivery-status-events} topic; the contract order-service consumes. */
public record DeliveryStatusEvent(
        UUID eventId,
        UUID deliveryId,
        UUID orderId,
        String status,
        UUID courierId,
        String courierName,
        String note,
        Instant occurredAt) {

    public static DeliveryStatusEvent of(Delivery delivery, String note) {
        Courier courier = delivery.getCourier();
        return new DeliveryStatusEvent(
                UUID.randomUUID(),
                delivery.getId(),
                delivery.getOrderId(),
                delivery.getStatus().name(),
                courier == null ? null : courier.getId(),
                courier == null ? null : courier.getName(),
                note,
                Instant.now());
    }
}
