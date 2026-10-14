package pe.viadeep.evidence.application;

import java.util.UUID;

public class EvidenceNotFoundException extends RuntimeException {

    public EvidenceNotFoundException(UUID id) {
        super("No existe la evidencia " + id);
    }
}
