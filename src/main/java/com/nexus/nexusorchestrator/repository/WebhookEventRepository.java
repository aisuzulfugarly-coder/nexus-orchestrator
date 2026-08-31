package com.nexus.nexusorchestrator.repository;

import com.nexus.nexusorchestrator.entity.EventStatus;
import com.nexus.nexusorchestrator.entity.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WebhookEventRepository extends JpaRepository<WebhookEvent, Long> {
    List<WebhookEvent> findByStatus(EventStatus status);

    List<WebhookEvent> findTop100ByOrderByIdDesc();

    long countByStatus(EventStatus status);
}