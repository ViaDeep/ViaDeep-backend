package pe.viadeep.incident.application;

import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.viadeep.incident.domain.Incident;
import pe.viadeep.incident.domain.IncidentNotFoundException;
import pe.viadeep.incident.domain.IncidentRepository;

/** El operador asigna (o reasigna) una incidencia abierta a un responsable y su área. */
@Service
public class AssignIncidentHandler {

    private final IncidentRepository incidents;
    private final Clock clock;

    public AssignIncidentHandler(IncidentRepository incidents, Clock clock) {
        this.incidents = incidents;
        this.clock = clock;
    }

    @Transactional
    public Incident handle(UUID incidentId, String assignee, String area, String note, UUID operatorId) {
        Incident incident = incidents.findById(incidentId).orElseThrow(() -> new IncidentNotFoundException(incidentId));
        incident.assign(assignee, area, note, operatorId, clock.instant());
        return incidents.save(incident);
    }
}
