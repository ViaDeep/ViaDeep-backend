package pe.viadeep.evidence.application;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "viadeep.evidence")
public record EvidenceProperties(long maxSizeBytes, Duration urlTtl) {
}
