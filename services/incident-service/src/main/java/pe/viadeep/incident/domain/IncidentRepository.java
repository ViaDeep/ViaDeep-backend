package pe.viadeep.incident.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IncidentRepository {

    Incident save(Incident incident);

    Optional<Incident> findById(UUID id);

    boolean existsByEvidenceId(UUID evidenceId);

    /** Incidencias abiertas (PENDING o IN_PROGRESS) del mismo tipo dentro del radio, sin contar la indicada. */
    List<UUID> findOpenNearby(IncidentType type, GeoLocation location, double radiusMeters, UUID excludeId);
}
