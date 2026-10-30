package pe.viadeep.incident.domain;

public class InvalidStatusTransitionException extends RuntimeException {

    public InvalidStatusTransitionException(IncidentStatus from, IncidentStatus to) {
        super("No se puede pasar de " + from + " a " + to);
    }
}
