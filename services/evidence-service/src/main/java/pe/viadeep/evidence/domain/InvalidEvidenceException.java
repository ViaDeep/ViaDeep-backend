package pe.viadeep.evidence.domain;

public class InvalidEvidenceException extends RuntimeException {

    public InvalidEvidenceException(String message) {
        super(message);
    }
}
