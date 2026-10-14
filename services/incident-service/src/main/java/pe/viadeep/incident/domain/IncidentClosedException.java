package pe.viadeep.incident.domain;

/** Se intentó asignar una incidencia que ya está cerrada (RESOLVED o REJECTED). */
public class IncidentClosedException extends RuntimeException {

    public IncidentClosedException(IncidentStatus status) {
        super("No se puede asignar una incidencia " + status);
    }
}
