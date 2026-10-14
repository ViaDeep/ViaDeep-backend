package pe.viadeep.incident.application;

import java.util.UUID;
import pe.viadeep.incident.domain.AiDetection;
import pe.viadeep.incident.domain.GeoLocation;
import pe.viadeep.incident.domain.IncidentType;

/** aiDetection es null cuando el ciudadano registra el reporte de forma manual. */
public record RegisterIncidentCommand(UUID reporterId, String accessToken, UUID evidenceId, IncidentType type,
                                      GeoLocation location, String description, AiDetection aiDetection) {
}
