package com.trackflow.order.domain;

public class InvalidStatusTransitionException extends RuntimeException {

    public InvalidStatusTransitionException(OrderStatus from, OrderStatus to) {
        super("Cannot move order from " + from + " to " + to);
    }
}
