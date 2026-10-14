package pe.viadeep.incident.infrastructure.notification;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Conexión con el servicio de notificaciones: tiempo de espera, reintentos y circuit breaker (ADR-013). internalToken es
 * la clave compartida que exige POST /notifications.
 */
@ConfigurationProperties(prefix = "viadeep.notification-service")
public record NotificationServiceProperties(String baseUrl, Duration timeout, String internalToken, int maxAttempts,
                                            Duration retryWait, Duration openCircuitWait) {
}
