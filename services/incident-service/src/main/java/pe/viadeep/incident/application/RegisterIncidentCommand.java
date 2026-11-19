package pe.viadeep.incident.application;

import java.util.List;
import java.util.UUID;
import pe.viadeep.incident.domain.AiDetection;
import pe.viadeep.incident.domain.DetectedDamage;
import pe.viadeep.incident.domain.GeoLocation;
import pe.viadeep.incident.domain.IncidentType;

/**
 * aiDetection es null cuando el reporte es manual. detections trae todas las detecciones de la foto (vacía si la app
 * envió solo la principal).
 */
public record RegisterIncidentCommand(UUID reporterId, String accessToken, UUID evidenceId, IncidentType type,
                                      GeoLocation location, String description, AiDetection aiDetection,
                                      List<DetectedDamage> detections) {

    public RegisterIncidentCommand {
        detections = detections == null ? List.of() : List.copyOf(detections);
    }
}
