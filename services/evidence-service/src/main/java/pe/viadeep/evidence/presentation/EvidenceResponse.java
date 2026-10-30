package pe.viadeep.evidence.presentation;

import java.time.Instant;
import java.util.UUID;
import pe.viadeep.evidence.domain.Evidence;

record EvidenceResponse(UUID id, String contentType, long sizeBytes, UUID uploadedBy, Instant uploadedAt) {

    static EvidenceResponse from(Evidence evidence) {
        return new EvidenceResponse(evidence.id(), evidence.format().contentType(), evidence.sizeBytes(),
                evidence.uploadedBy(), evidence.uploadedAt());
    }
}
