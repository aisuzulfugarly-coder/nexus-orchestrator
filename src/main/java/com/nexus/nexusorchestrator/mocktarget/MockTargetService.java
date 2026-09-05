package com.nexus.nexusorchestrator.mocktarget;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.ReentrantLock;

/**
 * WebhookConsumer-in sabit hədəfi olan localhost:9999/fake-service üçün canlı mock server.
 * "İşlək" rejimdə hər sorğuya təsadüfi nəticə verir (uğur / 500 / 400 / şəbəkə səviyyəsində qırılma),
 * beləliklə dashboard-da DELIVERED və SENT_TO_DLQ hadisələri müxtəlif AI diaqnozları ilə canlı görünür.
 * "Söndürülmüş" rejimdə server portu tamamilə buraxır ki, real connection-refused ssenarisi yaransın.
 */
@Service
public class MockTargetService {

    private static final int PORT = 9999;
    private static final String CONTEXT = "/fake-service";

    private final ReentrantLock lock = new ReentrantLock();
    private HttpServer server;
    private volatile boolean down = false;

    @PostConstruct
    public void start() {
        lock.lock();
        try {
            startServerLocked();
        } finally {
            lock.unlock();
        }
    }

    @PreDestroy
    public void stop() {
        lock.lock();
        try {
            stopServerLocked();
        } finally {
            lock.unlock();
        }
    }

    public boolean isDown() {
        return down;
    }

    public boolean toggle() {
        lock.lock();
        try {
            if (down) {
                startServerLocked();
                down = false;
            } else {
                stopServerLocked();
                down = true;
            }
            return down;
        } finally {
            lock.unlock();
        }
    }

    private void startServerLocked() {
        if (server != null) {
            return;
        }
        try {
            HttpServer newServer = HttpServer.create(new InetSocketAddress(PORT), 0);
            newServer.createContext(CONTEXT, this::handle);
            newServer.setExecutor(null);
            newServer.start();
            server = newServer;
        } catch (IOException e) {
            throw new IllegalStateException("Mock hədəf servis " + PORT + " portunda başladıla bilmədi", e);
        }
    }

    private void stopServerLocked() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    private void handle(HttpExchange exchange) throws IOException {
        try {
            int roll = ThreadLocalRandom.current().nextInt(100);
            if (roll < 70) {
                respond(exchange, 200, "{\"status\":\"OK\"}");
            } else if (roll < 85) {
                respond(exchange, 500, "{\"error\":\"internal server error\"}");
            } else if (roll < 95) {
                respond(exchange, 400, "{\"error\":\"bad request\"}");
            }
            // qalan 5%: heç bir cavab göndərilmir, exchange finally-də açıq-saçıq bağlanır
            // — client tərəfdə şəbəkə səviyyəsində uğursuzluq simulyasiya edir.
        } finally {
            exchange.close();
        }
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes();
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
