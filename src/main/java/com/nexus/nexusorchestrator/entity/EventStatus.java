package com.nexus.nexusorchestrator.entity;

public enum EventStatus {
    PENDING,
    PROCESSING,
    DELIVERED,
    RETRYING,
    FAILED,
    SENT_TO_DLQ
}