package pe.viadeep.incident.domain;

import java.util.List;
import java.util.UUID;

/** Evento de dominio: se registró una incidencia. Lleva los datos para el aviso a los operadores. */
public record IncidentRegistered(UUID incidentId, UUID reporterId, IncidentType type, List<UUID> possibleDuplicates) {
}
