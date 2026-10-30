package pe.viadeep.incident.domain;

public class InvalidIncidentException extends RuntimeException {

    public InvalidIncidentException(String message) {
        super(message);
    }
}
