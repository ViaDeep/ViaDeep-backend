package pe.viadeep.incident.domain;

import java.util.UUID;

/** Evento de dominio: una incidencia cambió de estado. Lleva los datos para el aviso al ciudadano. */
public record IncidentStatusChanged(UUID incidentId, UUID reporterId, IncidentStatus fromStatus, IncidentStatus toStatus,
                                    String comment) {
}
