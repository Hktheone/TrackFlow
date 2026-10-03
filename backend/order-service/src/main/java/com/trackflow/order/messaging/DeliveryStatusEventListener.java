package com.trackflow.order.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.trackflow.order.service.OrderService;

import tools.jackson.databind.json.JsonMapper;

@Component
public class DeliveryStatusEventListener {

    private static final Logger log = LoggerFactory.getLogger(DeliveryStatusEventListener.class);

    private final OrderService orderService;
    private final JsonMapper jsonMapper;

    public DeliveryStatusEventListener(OrderService orderService, JsonMapper jsonMapper) {
        this.orderService = orderService;
        this.jsonMapper = jsonMapper;
    }

    @KafkaListener(topics = KafkaTopics.DELIVERY_STATUS_EVENTS, groupId = "order-service")
    public void onDeliveryStatus(String payload) {
        DeliveryStatusEvent event = jsonMapper.readValue(payload, DeliveryStatusEvent.class);
        log.info("Received delivery status {} for order {}", event.status(), event.orderId());
        orderService.applyDeliveryStatus(event);
    }
}
