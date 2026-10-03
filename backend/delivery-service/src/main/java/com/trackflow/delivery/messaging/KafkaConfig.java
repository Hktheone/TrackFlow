package com.trackflow.delivery.messaging;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import tools.jackson.core.JacksonException;

@Configuration
public class KafkaConfig {

    @Bean
    NewTopic orderEventsTopic() {
        return TopicBuilder.name(KafkaTopics.ORDER_EVENTS).partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic deliveryStatusEventsTopic() {
        return TopicBuilder.name(KafkaTopics.DELIVERY_STATUS_EVENTS).partitions(3).replicas(1).build();
    }

    /**
     * Retries a failing record 3 times, one second apart, then parks it on the topic's dead-letter
     * topic instead of blocking the partition. Malformed JSON goes straight to the DLT since retrying can't fix it.
     * Spring Boot wires this bean into the default listener container factory.
     */
    @Bean
    DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> kafkaTemplate) {
        var recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate);
        var handler = new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3));
        handler.addNotRetryableExceptions(JacksonException.class);
        return handler;
    }
}
