package pe.viadeep.incident.infrastructure.evidence;

import java.util.UUID;
import pe.viadeep.incident.application.EvidenceGateway.VerifiedEvidence;

/** Traduce la respuesta del servicio de evidencias al tipo propio del contexto Incidencias (ACL). */
final class EvidenceTranslator {

    private EvidenceTranslator() {
    }

    static VerifiedEvidence toVerifiedEvidence(EvidenceResponse response) {
        return new VerifiedEvidence(response.id(), response.uploadedBy());
    }

    /** Solo los campos que Incidencias necesita de la respuesta de GET /evidences/{id}. */
    record EvidenceResponse(UUID id, UUID uploadedBy) {
    }
}
