package pe.viadeep.incident.infrastructure.evidence;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "viadeep.evidence-service")
public record EvidenceServiceProperties(String baseUrl, Duration timeout) {
}
