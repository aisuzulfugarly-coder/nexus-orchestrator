package com.nexus.nexusorchestrator.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class WebhookProducer {

    private static final String TOPIC = "webhook-events";
    private final KafkaTemplate<String, String> kafkaTemplate;

    public WebhookProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendEvent(String eventId, String payload) {
        kafkaTemplate.send(TOPIC, eventId, payload);
        System.out.println("--> Kafka Producer: Event [ID: " + eventId + "] növbəyə atıldı.");
    }
}
