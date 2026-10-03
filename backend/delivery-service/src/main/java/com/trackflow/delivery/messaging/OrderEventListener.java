package com.trackflow.delivery.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.trackflow.delivery.service.DeliveryService;

import tools.jackson.databind.json.JsonMapper;

/** Reacts to order lifecycle events: a confirmed order becomes a delivery, a cancelled one stops it. */
@Component
public class OrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

    private final DeliveryService deliveryService;
    private final JsonMapper jsonMapper;

    public OrderEventListener(DeliveryService deliveryService, JsonMapper jsonMapper) {
        this.deliveryService = deliveryService;
        this.jsonMapper = jsonMapper;
    }

    @KafkaListener(topics = KafkaTopics.ORDER_EVENTS, groupId = "delivery-service")
    public void onOrderEvent(String payload) {
        OrderEvent event = jsonMapper.readValue(payload, OrderEvent.class);
        log.info("Received {} for order {}", event.eventType(), event.orderNumber());
        switch (event.eventType()) {
            case ORDER_CONFIRMED -> deliveryService.createForConfirmedOrder(event);
            case ORDER_CANCELLED -> deliveryService.cancelForOrder(event.orderId());
            case ORDER_CREATED -> {
                // Nothing to do until the order is confirmed.
            }
        }
    }
}
