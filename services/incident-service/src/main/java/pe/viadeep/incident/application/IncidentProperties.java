package pe.viadeep.incident.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "viadeep.incidents")
public record IncidentProperties(double duplicateRadiusMeters) {
}
