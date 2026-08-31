# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Nexus Orchestrator — a Spring Boot 3.2 (Java 17) service that ingests webhooks over REST, queues them on Kafka, and delivers them to a target service with Resilience4j retry/circuit-breaker protection. Log messages and some API response text in the codebase are written in Azerbaijani — keep that style if you add similar user/log-facing strings in the same classes, don't silently switch them to English.

## Commands

Local infra (Postgres + Zookeeper + Kafka) is not started automatically — bring it up first:
```
docker-compose up -d
```

Build / run / test (via the Maven wrapper, no local Maven install needed):
```
./mvnw clean install        # build
./mvnw spring-boot:run       # run the app (port 8080)
./mvnw test                  # run all tests
./mvnw test -Dtest=NexusOrchestratorApplicationTests   # run a single test class
```

There is no linter/formatter configured in this repo.

## Architecture

Request flow: `WebhookController` (`POST /api/v1/webhooks/ingest`) → `WebhookProducer` publishes the raw payload to the Kafka topic `webhook-events` → `WebhookConsumer` (`@KafkaListener`, group `nexus-group`) picks it up and forwards it via `RestTemplate` to a hardcoded target (`http://localhost:9999/fake-service`).

Resilience: `WebhookConsumer.consumeEvent` is wrapped with Resilience4j `@Retry(name = "webhookRetry")` and `@CircuitBreaker(name = "webhookCircuitBreaker")` (config in `application.yml`). Both fall back to `fallbackDeliver`, which currently only logs to stderr — it does not persist anything, republish to a DLQ topic, or call an AI diagnostic step. Treat any "AI-diagnosed DLQ" or "event persistence" behavior mentioned in code/comments as intent, not implemented behavior, unless you see it actually wired up.

Persistence model exists but is **not yet connected** to the produce/consume flow: `WebhookEvent` (entity, has `status: EventStatus`, `retryCount`, `aiDiagnostic` fields) and `WebhookEventRepository` (Spring Data JPA) are defined but nothing in `WebhookProducer`/`WebhookConsumer` currently saves or reads through them. If you're asked to add DLQ handling, event-status tracking, or AI failure diagnostics, this is the intended integration point — wire the consumer's fallback path to persist `WebhookEvent` rows via `WebhookEventRepository` rather than just logging.

`EventStatus` enum values (`PENDING`, `PROCESSING`, `DELIVERED`, `RETRYING`, `FAILED`, `SENT_TO_DLQ`) describe the full intended lifecycle even though only `PENDING` is ever set today (in `WebhookEvent.onCreate`).

Note the package is `com.nexus.nexusorchestrator.contorller` (typo, missing an "n") — match this exact spelling when adding to or importing from that package.

## Configuration

`src/main/resources/application.yml` holds datasource (Postgres on `localhost:5432/nexus_db`), Kafka bootstrap/serializer settings, and the Resilience4j `webhookRetry` / `webhookCircuitBreaker` instance configs. `spring.jpa.hibernate.ddl-auto` is `update`, so entity changes apply automatically against the dev database — no migration tooling (e.g. Flyway/Liquibase) is set up.