package com.nexus.nexusorchestrator.contorller;

import com.nexus.nexusorchestrator.mocktarget.MockTargetService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/mock-target")
public class MockTargetController {

    private final MockTargetService mockTargetService;

    public MockTargetController(MockTargetService mockTargetService) {
        this.mockTargetService = mockTargetService;
    }

    @GetMapping("/status")
    public Map<String, Boolean> status() {
        return Map.of("down", mockTargetService.isDown());
    }

    @PostMapping("/toggle")
    public Map<String, Boolean> toggle() {
        return Map.of("down", mockTargetService.toggle());
    }
}
