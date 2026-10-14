package pe.viadeep.incident.infrastructure.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import pe.viadeep.incident.domain.IncidentRegistered;
import pe.viadeep.incident.domain.IncidentStatus;
import pe.viadeep.incident.domain.IncidentStatusChanged;
import pe.viadeep.incident.domain.IncidentType;

/** Prueba el cliente ACL contra un servidor HTTP local que imita al servicio de notificaciones. */
class NotificationClientTest {

    private static final Instant REPORTED = Instant.parse("2026-10-14T15:00:00Z");

    private HttpServer server;
    private NotificationClient client;
    private final AtomicInteger status = new AtomicInteger(201);
    private final AtomicInteger calls = new AtomicInteger();
    private final AtomicReference<String> body = new AtomicReference<>();
    private final AtomicReference<String> token = new AtomicReference<>();

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/notifications", exchange -> {
            calls.incrementAndGet();
            token.set(exchange.getRequestHeaders().getFirst("X-Internal-Token"));
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            exchange.sendResponseHeaders(status.get(), -1);
            exchange.close();
        });
        server.start();
        String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        client = new NotificationClient(RestClient.builder(), new NotificationServiceProperties(baseUrl,
                Duration.ofMillis(500), "secret", 3, Duration.ofMillis(10), Duration.ofSeconds(30)));
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void sendsTheNewReportWithItsPossibleDuplicatesAndTheInternalToken() {
        UUID incident = UUID.randomUUID();
        client.incidentRegistered(new IncidentRegistered(incident, UUID.randomUUID(), IncidentType.D40, "San Borja",
                List.of(UUID.randomUUID(), UUID.randomUUID()), REPORTED));

        assertThat(calls.get()).isEqualTo(1);
        assertThat(token.get()).isEqualTo("secret");
        assertThat(body.get()).contains("\"type\":\"NEW_REPORT\"", "\"incidentId\":\"" + incident + "\"",
                "\"incidentType\":\"D40\"", "\"possibleDuplicates\":2", "\"status\":\"PENDING\"");
    }

    @Test
    void aRejectionIsSentAsRejectedWithItsReason() {
        client.statusChanged(new IncidentStatusChanged(UUID.randomUUID(), UUID.randomUUID(), IncidentType.D00, null,
                IncidentStatus.PENDING, IncidentStatus.REJECTED, "Foto borrosa", REPORTED));

        assertThat(body.get()).contains("\"type\":\"REJECTED\"", "\"comment\":\"Foto borrosa\"");
    }

    @Test
    void retriesServerErrorsAndNeverThrows() {
        status.set(503);
        assertThatCode(() -> client.statusChanged(changed(IncidentStatus.RESOLVED))).doesNotThrowAnyException();
        assertThat(calls.get()).isEqualTo(3);
    }

    @Test
    void doesNotRetryARejectedRequest() {
        status.set(403);
        client.statusChanged(changed(IncidentStatus.IN_PROGRESS));
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void opensTheCircuitAfterRepeatedFailuresAndStopsCallingTheService() {
        status.set(500);
        for (int i = 0; i < 5; i++) {
            client.statusChanged(changed(IncidentStatus.IN_PROGRESS));
        }
        assertThat(client.circuitState()).isEqualTo(CircuitBreaker.State.OPEN);
        int before = calls.get();
        client.statusChanged(changed(IncidentStatus.IN_PROGRESS));
        assertThat(calls.get()).isEqualTo(before);
    }

    private static IncidentStatusChanged changed(IncidentStatus to) {
        return new IncidentStatusChanged(UUID.randomUUID(), UUID.randomUUID(), IncidentType.D20, "Lince",
                IncidentStatus.PENDING, to, null, REPORTED);
    }
}
