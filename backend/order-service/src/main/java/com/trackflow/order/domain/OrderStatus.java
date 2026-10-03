package com.trackflow.order.domain;

import java.util.EnumSet;
import java.util.Set;

/**
 * Lifecycle of an order. PENDING → CONFIRMED is driven by the API; everything after that
 * (courier assigned, picked up, in transit, delivered/failed) is driven by delivery-status-events.
 * <p>
 * Delivery stages may be skipped forward (e.g. a missed PICKED_UP event followed by IN_TRANSIT)
 * but never move backwards, which makes replayed or duplicate events harmless.
 */
public enum OrderStatus {
    PENDING,
    CONFIRMED,
    COURIER_ASSIGNED,
    PICKED_UP,
    IN_TRANSIT,
    DELIVERED,
    DELIVERY_FAILED,
    CANCELLED;

    public Set<OrderStatus> allowedNext() {
        return switch (this) {
            case PENDING -> EnumSet.of(CONFIRMED, CANCELLED);
            case CONFIRMED -> EnumSet.of(COURIER_ASSIGNED, PICKED_UP, IN_TRANSIT, DELIVERED, DELIVERY_FAILED, CANCELLED);
            case COURIER_ASSIGNED -> EnumSet.of(PICKED_UP, IN_TRANSIT, DELIVERED, DELIVERY_FAILED, CANCELLED);
            case PICKED_UP -> EnumSet.of(IN_TRANSIT, DELIVERED, DELIVERY_FAILED);
            case IN_TRANSIT -> EnumSet.of(DELIVERED, DELIVERY_FAILED);
            case DELIVERED, DELIVERY_FAILED, CANCELLED -> EnumSet.noneOf(OrderStatus.class);
        };
    }

    public boolean canTransitionTo(OrderStatus next) {
        return allowedNext().contains(next);
    }

    public boolean isTerminal() {
        return allowedNext().isEmpty();
    }
}
