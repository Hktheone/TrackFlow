package com.trackflow.order.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import tools.jackson.databind.json.JsonMapper;

/**
 * Forwards {@link OrderEvent}s to Kafka only after the database transaction that produced them
 * has committed, so consumers never see an event for an order change that was rolled back.
 * <p>
 * (The remaining gap — a crash between commit and send — is what a transactional outbox would close.)
 */
@Component
public class OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JsonMapper jsonMapper;

    public OrderEventPublisher(KafkaTemplate<String, String> kafkaTemplate, JsonMapper jsonMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.jsonMapper = jsonMapper;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(OrderEvent event) {
        String payload = jsonMapper.writeValueAsString(event);
        kafkaTemplate.send(KafkaTopics.ORDER_EVENTS, event.orderId().toString(), payload)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish {} for order {}", event.eventType(), event.orderNumber(), ex);
                    } else {
                        log.info("Published {} for order {} (partition {}, offset {})",
                                event.eventType(), event.orderNumber(),
                                result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                    }
                });
    }
}
