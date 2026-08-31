package com.nexus.nexusorchestrator.contorller;

import com.nexus.nexusorchestrator.entity.EventStatus;
import com.nexus.nexusorchestrator.entity.WebhookEvent;
import com.nexus.nexusorchestrator.kafka.WebhookProducer;
import com.nexus.nexusorchestrator.repository.WebhookEventRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/webhooks")
public class WebhookController {

    private static final String TARGET_URL = "http://localhost:9999/fake-service";

    private final WebhookProducer producer;
    private final WebhookEventRepository eventRepository;

    public WebhookController(WebhookProducer producer, WebhookEventRepository eventRepository) {
        this.producer = producer;
        this.eventRepository = eventRepository;
    }

    @PostMapping("/ingest")
    public ResponseEntity<Map<String, String>> receiveWebhook(@RequestBody String payload) {
        WebhookEvent event = WebhookEvent.builder()
                .eventType("webhook.ingest")
                .payload(payload)
                .targetUrl(TARGET_URL)
                .status(EventStatus.PENDING)
                .retryCount(0)
                .build();
        event = eventRepository.save(event);

        String eventId = String.valueOf(event.getId());
        producer.sendEvent(eventId, payload);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
                "status", "ACCEPTED",
                "message", "Webhook event qəbul edildi və Kafka növbəsinə yazıldı.",
                "eventId", eventId
        ));
    }
}