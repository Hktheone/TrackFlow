package com.trackflow.delivery.domain;

public class InvalidDeliveryTransitionException extends RuntimeException {

    public InvalidDeliveryTransitionException(String message) {
        super(message);
    }

    public InvalidDeliveryTransitionException(DeliveryStatus from, DeliveryStatus to) {
        this("Cannot move delivery from " + from + " to " + to);
    }
}
