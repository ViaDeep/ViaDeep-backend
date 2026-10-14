package pe.viadeep.evidence.application;

public class EvidenceAccessDeniedException extends RuntimeException {

    public EvidenceAccessDeniedException() {
        super("No tienes permiso para ver esta evidencia");
    }
}
