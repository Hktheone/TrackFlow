package com.trackflow.delivery.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import tools.jackson.databind.json.JsonMapper;

/**
 * Sends {@link DeliveryStatusEvent}s to Kafka once the transaction that changed the delivery commits.
 * Events are keyed by orderId so every update for one order lands on the same partition, in order.
 */
@Component
public class DeliveryEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(DeliveryEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JsonMapper jsonMapper;

    public DeliveryEventPublisher(KafkaTemplate<String, String> kafkaTemplate, JsonMapper jsonMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.jsonMapper = jsonMapper;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(DeliveryStatusEvent event) {
        String payload = jsonMapper.writeValueAsString(event);
        kafkaTemplate.send(KafkaTopics.DELIVERY_STATUS_EVENTS, event.orderId().toString(), payload)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish delivery status {} for order {}", event.status(), event.orderId(), ex);
                    } else {
                        log.info("Published delivery status {} for order {}", event.status(), event.orderId());
                    }
                });
    }
}
