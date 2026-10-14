package pe.viadeep.evidence.domain;

import java.util.Optional;
import java.util.UUID;

public interface EvidenceRepository {

    Evidence save(Evidence evidence);

    Optional<Evidence> findById(UUID id);
}
