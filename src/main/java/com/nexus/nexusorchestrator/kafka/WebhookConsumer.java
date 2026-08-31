package com.nexus.nexusorchestrator.kafka;

import com.nexus.nexusorchestrator.entity.EventStatus;
import com.nexus.nexusorchestrator.entity.WebhookEvent;
import com.nexus.nexusorchestrator.repository.WebhookEventRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class WebhookConsumer {

    private static final String TARGET_URL = "http://localhost:9999/fake-service";

    private final RestTemplate restTemplate = new RestTemplate();
    private final WebhookEventRepository eventRepository;

    public WebhookConsumer(WebhookEventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @KafkaListener(topics = "webhook-events", groupId = "nexus-group")
    @Retry(name = "webhookRetry", fallbackMethod = "fallbackDeliver")
    @CircuitBreaker(name = "webhookCircuitBreaker", fallbackMethod = "fallbackDeliver")
    public void consumeEvent(@Header(KafkaHeaders.RECEIVED_KEY) String eventId, @Payload String payload) {
        System.out.println("<-- Kafka Consumer: Event alındı, hədəf servisə göndərilir: " + payload);

        markAttempt(eventId);

        restTemplate.postForEntity(TARGET_URL, payload, String.class);

        markDelivered(eventId);
        System.out.println("SUCCESS: Event uğurla çatdırıldı!");
    }

    public void fallbackDeliver(String eventId, String payload, Exception e) {
        System.err.println("CRITICAL: Hədəf servis cavab vermədi!");
        System.err.println("FALLBACK WORKER: Circuit Breaker / Retry işə düşdü. Səbəb: " + e.getMessage());
        System.err.println("STATUS: Event DLQ növbəsinə atıldı və AI analizinə yönləndirildi.");

        markSentToDlq(eventId);
    }

    private void markAttempt(String eventId) {
        findEvent(eventId).ifPresent(event -> {
            EventStatus nextStatus = event.getRetryCount() > 0 ? EventStatus.RETRYING : EventStatus.PROCESSING;
            event.setStatus(nextStatus);
            event.setRetryCount(event.getRetryCount() + 1);
            eventRepository.save(event);
        });
    }

    private void markDelivered(String eventId) {
        findEvent(eventId).ifPresent(event -> {
            event.setStatus(EventStatus.DELIVERED);
            eventRepository.save(event);
        });
    }

    private void markSentToDlq(String eventId) {
        findEvent(eventId).ifPresent(event -> {
            event.setStatus(EventStatus.SENT_TO_DLQ);
            eventRepository.save(event);
        });
    }

    private java.util.Optional<WebhookEvent> findEvent(String eventId) {
        try {
            return eventRepository.findById(Long.valueOf(eventId));
        } catch (NumberFormatException ex) {
            return java.util.Optional.empty();
        }
    }
}