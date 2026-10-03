package com.trackflow.order.domain;

/** Who caused a status change: a user through the REST API, or an event from the delivery service. */
public enum StatusChangeSource {
    API,
    DELIVERY_EVENT
}
