package pe.viadeep.incident.infrastructure.persistence;

import java.util.Map;
import pe.viadeep.incident.domain.IncidentType;

/** Identificadores fijos del catálogo incident_types, tal como los inserta la migración V1. */
final class IncidentTypeIds {

    private static final Map<IncidentType, Short> IDS = Map.of(
            IncidentType.D00, (short) 1,
            IncidentType.D10, (short) 2,
            IncidentType.D20, (short) 3,
            IncidentType.D40, (short) 4);

    private IncidentTypeIds() {
    }

    static short idOf(IncidentType type) {
        return IDS.get(type);
    }

    static IncidentType typeOf(short id) {
        return IDS.entrySet().stream()
                .filter(entry -> entry.getValue() == id)
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Tipo de incidencia desconocido: " + id));
    }
}
