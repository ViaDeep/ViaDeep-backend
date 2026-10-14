package pe.viadeep.evidence.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import pe.viadeep.evidence.domain.Evidence;
import pe.viadeep.evidence.domain.EvidenceRepository;

@Repository
class EvidenceRepositoryJpa implements EvidenceRepository {

    private final SpringDataEvidenceRepository jpa;

    EvidenceRepositoryJpa(SpringDataEvidenceRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Evidence save(Evidence evidence) {
        return jpa.save(EvidenceJpaEntity.fromDomain(evidence)).toDomain();
    }

    @Override
    public Optional<Evidence> findById(UUID id) {
        return jpa.findById(id).map(EvidenceJpaEntity::toDomain);
    }
}
