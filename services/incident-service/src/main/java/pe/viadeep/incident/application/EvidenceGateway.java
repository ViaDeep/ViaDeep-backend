package pe.viadeep.incident.application;

import java.util.Optional;
import java.util.UUID;

/**
 * Contrato (ACL) que necesita Incidencias para verificar la evidencia de un reporte en el servicio de evidencias.
 * Devuelve vacío si la evidencia no existe o el usuario no puede verla.
 */
public interface EvidenceGateway {

    Optional<VerifiedEvidence> find(UUID evidenceId, String accessToken);

    record VerifiedEvidence(UUID id, UUID uploadedBy) {
    }
}
