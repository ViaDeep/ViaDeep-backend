package pe.viadeep.incident.domain;

import java.util.UUID;

public class IncidentNotFoundException extends RuntimeException {

    public IncidentNotFoundException(UUID id) {
        super("No existe la incidencia " + id);
    }
}
