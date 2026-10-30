package pe.viadeep.incident.domain;

import java.time.Instant;
import java.util.UUID;

/** Registro del historial de estados. fromStatus es null en el registro inicial (PENDING). */
public record StatusChange(UUID id, IncidentStatus fromStatus, IncidentStatus toStatus, UUID changedBy, String comment,
                           Instant changedAt) {
}
