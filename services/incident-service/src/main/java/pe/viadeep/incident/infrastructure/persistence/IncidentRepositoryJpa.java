package pe.viadeep.incident.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import pe.viadeep.incident.domain.GeoLocation;
import pe.viadeep.incident.domain.Incident;
import pe.viadeep.incident.domain.IncidentRepository;
import pe.viadeep.incident.domain.IncidentType;

@Repository
class IncidentRepositoryJpa implements IncidentRepository {

    private final SpringDataIncidentRepository jpa;
    private final JdbcClient jdbc;

    IncidentRepositoryJpa(SpringDataIncidentRepository jpa, JdbcClient jdbc) {
        this.jpa = jpa;
        this.jdbc = jdbc;
    }

    @Override
    public Incident save(Incident incident) {
        return jpa.save(IncidentJpaEntity.fromDomain(incident)).toDomain();
    }

    @Override
    public Optional<Incident> findById(UUID id) {
        return jpa.findById(id).map(IncidentJpaEntity::toDomain);
    }

    @Override
    public boolean existsByEvidenceId(UUID evidenceId) {
        return jpa.existsByEvidenceId(evidenceId);
    }

    /** Búsqueda espacial con PostGIS: ST_DWithin sobre geography mide el radio en metros y usa el índice GiST. */
    @Override
    public List<UUID> findOpenNearby(IncidentType type, GeoLocation location, double radiusMeters, UUID excludeId) {
        return jdbc.sql("""
                        SELECT id FROM incidents
                        WHERE incident_type_id = :typeId
                          AND status IN ('PENDING', 'IN_PROGRESS')
                          AND id <> :excludeId
                          AND ST_DWithin(location, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography, :radius)
                        ORDER BY created_at
                        """)
                .param("typeId", IncidentTypeIds.idOf(type))
                .param("excludeId", excludeId)
                .param("lng", location.longitude())
                .param("lat", location.latitude())
                .param("radius", radiusMeters)
                .query(UUID.class)
                .list();
    }
}
