package pe.viadeep.notification.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Datos de la incidencia que el servicio de incidencias envía con cada solicitud de aviso. Se guardan junto al aviso
 * para mostrarlo sin consultar a otro servicio. incidentType es el código del modelo (D00, D10, D20 o D40).
 */
public record IncidentFacts(UUID incidentId, UUID reporterId, String incidentType, String district, String status,
                            int possibleDuplicates, String comment, Instant reportedAt) {
}
