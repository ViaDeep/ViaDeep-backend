package pe.viadeep.incident.infrastructure.notification;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import java.net.http.HttpClient;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import pe.viadeep.incident.application.NotificationGateway;
import pe.viadeep.incident.domain.IncidentRegistered;
import pe.viadeep.incident.domain.IncidentStatus;
import pe.viadeep.incident.domain.IncidentStatusChanged;

/**
 * Traduce los eventos de Incidencias en solicitudes de aviso e invoca POST /notifications con tiempo de espera acotado,
 * reintentos y circuit breaker (Resilience4j). Si el aviso no puede enviarse, lo registra en los logs y sigue.
 */
@Component
@EnableConfigurationProperties(NotificationServiceProperties.class)
class NotificationClient implements NotificationGateway {

    private static final Logger log = LoggerFactory.getLogger(NotificationClient.class);
    static final String TOKEN_HEADER = "X-Internal-Token";

    private final RestClient rest;
    private final String token;
    private final Retry retry;
    private final CircuitBreaker circuitBreaker;

    NotificationClient(RestClient.Builder builder, NotificationServiceProperties properties) {
        HttpClient http = HttpClient.newBuilder().connectTimeout(properties.timeout()).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(properties.timeout());
        this.rest = builder.baseUrl(properties.baseUrl()).requestFactory(factory).build();
        this.token = properties.internalToken() == null ? "" : properties.internalToken();
        this.circuitBreaker = CircuitBreaker.of("notification-service", CircuitBreakerConfig.custom()
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .failureRateThreshold(50)
                .waitDurationInOpenState(properties.openCircuitWait())
                .ignoreExceptions(HttpClientErrorException.class)
                .build());
        this.retry = Retry.of("notification-service", RetryConfig.custom()
                .maxAttempts(Math.max(properties.maxAttempts(), 1))
                .waitDuration(properties.retryWait())
                .ignoreExceptions(HttpClientErrorException.class, CallNotPermittedException.class)
                .build());
    }

    @Override
    public void incidentRegistered(IncidentRegistered event) {
        send(new NotificationRequest("NEW_REPORT", event.incidentId(), event.reporterId(), event.type().name(),
                event.district(), IncidentStatus.PENDING.name(), event.possibleDuplicates().size(), null,
                event.reportedAt()));
    }

    @Override
    public void statusChanged(IncidentStatusChanged event) {
        String type = event.toStatus() == IncidentStatus.REJECTED ? "REJECTED" : "STATUS_CHANGED";
        send(new NotificationRequest(type, event.incidentId(), event.reporterId(), event.type().name(), event.district(),
                event.toStatus().name(), 0, event.comment(), event.reportedAt()));
    }

    private void send(NotificationRequest request) {
        Runnable call = CircuitBreaker.decorateRunnable(circuitBreaker, () -> rest.post()
                .uri("/notifications")
                .header(TOKEN_HEADER, token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toBodilessEntity());
        try {
            Retry.decorateRunnable(retry, call).run();
        } catch (RuntimeException e) {
            log.warn("Aviso no enviado al servicio de notificaciones: tipo={} incidencia={} estado={} causa={}",
                    request.type(), request.incidentId(), request.status(), e.toString());
        }
    }

    CircuitBreaker.State circuitState() {
        return circuitBreaker.getState();
    }

    /** Cuerpo de POST /notifications del servicio de notificaciones. */
    record NotificationRequest(String type, UUID incidentId, UUID reporterId, String incidentType, String district,
                               String status, int possibleDuplicates, String comment, Instant reportedAt) {
    }
}
