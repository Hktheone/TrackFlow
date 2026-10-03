package com.trackflow.delivery.domain;

import java.util.EnumSet;
import java.util.Set;

public enum DeliveryStatus {
    PENDING_ASSIGNMENT,
    ASSIGNED,
    PICKED_UP,
    IN_TRANSIT,
    DELIVERED,
    FAILED,
    CANCELLED;

    public Set<DeliveryStatus> allowedNext() {
        return switch (this) {
            case PENDING_ASSIGNMENT -> EnumSet.of(ASSIGNED, CANCELLED);
            case ASSIGNED -> EnumSet.of(PICKED_UP, FAILED, CANCELLED);
            case PICKED_UP -> EnumSet.of(IN_TRANSIT, DELIVERED, FAILED);
            case IN_TRANSIT -> EnumSet.of(DELIVERED, FAILED);
            case DELIVERED, FAILED, CANCELLED -> EnumSet.noneOf(DeliveryStatus.class);
        };
    }

    public boolean canTransitionTo(DeliveryStatus next) {
        return allowedNext().contains(next);
    }

    public boolean isTerminal() {
        return allowedNext().isEmpty();
    }

    /** Before pickup the courier can still be swapped and the order can still be cancelled. */
    public boolean isBeforePickup() {
        return this == PENDING_ASSIGNMENT || this == ASSIGNED;
    }
}
