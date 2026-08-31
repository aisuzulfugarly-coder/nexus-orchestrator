package com.nexus.nexusorchestrator.contorller;

import com.nexus.nexusorchestrator.entity.EventStatus;
import com.nexus.nexusorchestrator.entity.WebhookEvent;
import com.nexus.nexusorchestrator.repository.WebhookEventRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/events")
public class WebhookEventController {

    private final WebhookEventRepository eventRepository;

    public WebhookEventController(WebhookEventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @GetMapping
    public List<WebhookEvent> listRecentEvents() {
        return eventRepository.findTop100ByOrderByIdDesc();
    }

    @GetMapping("/stats")
    public Map<String, Long> stats() {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (EventStatus status : EventStatus.values()) {
            counts.put(status.name(), eventRepository.countByStatus(status));
        }
        counts.put("TOTAL", eventRepository.count());
        return counts;
    }
}
