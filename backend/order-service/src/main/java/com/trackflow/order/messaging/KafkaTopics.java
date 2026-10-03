package com.trackflow.order.messaging;

public final class KafkaTopics {

    /** Published by order-service; keyed by orderId. */
    public static final String ORDER_EVENTS = "order-events";

    /** Published by delivery-service; keyed by orderId so all events for one order stay ordered. */
    public static final String DELIVERY_STATUS_EVENTS = "delivery-status-events";

    private KafkaTopics() {
    }
}
