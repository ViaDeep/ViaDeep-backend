package pe.viadeep.incident.application;

public class EvidenceServiceUnavailableException extends RuntimeException {

    public EvidenceServiceUnavailableException(Throwable cause) {
        super("El servicio de evidencias no respondió; intenta de nuevo", cause);
    }
}
