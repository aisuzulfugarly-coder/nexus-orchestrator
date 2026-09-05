# Nexus Orchestrator

Webhook-ları qəbul edib, Kafka növbəsi üzərindən hədəf servisə etibarlı şəkildə çatdıran Spring Boot backend. Çatdırma uğursuz olarsa, Resilience4j retry + circuit breaker işə düşür və hadisə DLQ-ya yönləndirilir.

Canlı izləmə paneli: tətbiq işə düşdükdən sonra **http://localhost:8080** ünvanında.

## Son yenilik: AI Diaqnostika

Əvvəllər çatdırma uğursuz olanda sistem yalnız konsola xəta yazırdı və səbəb heç yerdə saxlanılmırdı. İndi:

- Yeni `FailureDiagnosticService` xətanın növünə görə (server əlçatan deyil, timeout, HTTP 5xx/4xx, circuit breaker açıq və s.) insan-anlayan izah yaradır.
- Bu izah hər DLQ-ya düşən hadisə üçün bazada (`ai_diagnostic` sahəsi) saxlanılır.
- Dashboard-da "AI Diaqnostika" sütununda hər hadisənin niyə uğursuz olduğu görünür.

Qeyd: bu, xarici AI çağırışı olmayan **qayda-əsaslı** analizdir — API açarı tələb olunmur, dərhal işləyir.

## İşə salmaq

```bash
docker-compose up -d        # Postgres + Kafka + Zookeeper
./mvnw spring-boot:run       # tətbiq, port 8080
```
