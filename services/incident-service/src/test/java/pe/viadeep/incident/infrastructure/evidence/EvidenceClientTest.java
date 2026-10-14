package pe.viadeep.incident.infrastructure.evidence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import pe.viadeep.incident.application.EvidenceGateway.VerifiedEvidence;
import pe.viadeep.incident.application.EvidenceServiceUnavailableException;

/** Prueba el cliente ACL contra un servidor HTTP local que imita al servicio de evidencias. */
class EvidenceClientTest {

    private HttpServer server;
    private EvidenceClient client;
    private final AtomicReference<Integer> status = new AtomicReference<>(200);
    private final AtomicReference<String> body = new AtomicReference<>("");
    private final AtomicReference<String> authorization = new AtomicReference<>();
    private final AtomicReference<Long> delayMillis = new AtomicReference<>(0L);

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/evidences/", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            try {
                Thread.sleep(delayMillis.get());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            byte[] bytes = body.get().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status.get(), bytes.length == 0 ? -1 : bytes.length);
            if (bytes.length > 0) {
                exchange.getResponseBody().write(bytes);
            }
            exchange.close();
        });
        server.start();
        String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        client = new EvidenceClient(RestClient.builder(), new EvidenceServiceProperties(baseUrl, Duration.ofMillis(500)));
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void translatesTheEvidenceAndForwardsTheUserToken() {
        UUID id = UUID.randomUUID();
        UUID owner = UUID.randomUUID();
        body.set("""
                {"id": "%s", "contentType": "image/jpeg", "sizeBytes": 10, "uploadedBy": "%s", "uploadedAt": "2026-10-14T00:00:00Z"}
                """.formatted(id, owner));

        assertThat(client.find(id, "token-123")).contains(new VerifiedEvidence(id, owner));
        assertThat(authorization.get()).isEqualTo("Bearer token-123");
    }

    @Test
    void missingOrForbiddenEvidenceIsEmpty() {
        status.set(404);
        assertThat(client.find(UUID.randomUUID(), "t")).isEmpty();
        status.set(403);
        assertThat(client.find(UUID.randomUUID(), "t")).isEmpty();
    }

    @Test
    void serverErrorsAndTimeoutsMeanTheServiceIsUnavailable() {
        status.set(500);
        assertThatThrownBy(() -> client.find(UUID.randomUUID(), "t")).isInstanceOf(EvidenceServiceUnavailableException.class);

        status.set(200);
        body.set("{}");
        delayMillis.set(2000L);
        assertThatThrownBy(() -> client.find(UUID.randomUUID(), "t")).isInstanceOf(EvidenceServiceUnavailableException.class);
    }
}
