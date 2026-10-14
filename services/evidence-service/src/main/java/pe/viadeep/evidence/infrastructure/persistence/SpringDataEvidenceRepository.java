package pe.viadeep.evidence.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataEvidenceRepository extends JpaRepository<EvidenceJpaEntity, UUID> {
}
