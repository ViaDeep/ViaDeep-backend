package pe.viadeep.incident.domain;

import java.util.Set;

/**
 * Estados de una incidencia y sus transiciones válidas (OB2, agregado Incident):
 * PENDING → IN_PROGRESS → RESOLVED y PENDING → REJECTED.
 */
public enum IncidentStatus {
    PENDING,
    IN_PROGRESS,
    RESOLVED,
    REJECTED;

    public boolean canTransitionTo(IncidentStatus target) {
        return allowedTargets().contains(target);
    }

    private Set<IncidentStatus> allowedTargets() {
        return switch (this) {
            case PENDING -> Set.of(IN_PROGRESS, REJECTED);
            case IN_PROGRESS -> Set.of(RESOLVED);
            case RESOLVED, REJECTED -> Set.of();
        };
    }
}
