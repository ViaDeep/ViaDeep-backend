package pe.viadeep.incident.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataIncidentRepository extends JpaRepository<IncidentJpaEntity, UUID> {

    boolean existsByEvidenceId(UUID evidenceId);
}
