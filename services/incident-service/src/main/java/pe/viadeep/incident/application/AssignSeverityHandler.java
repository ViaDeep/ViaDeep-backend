package pe.viadeep.incident.application;

import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.viadeep.incident.domain.Incident;
import pe.viadeep.incident.domain.IncidentNotFoundException;
import pe.viadeep.incident.domain.IncidentRepository;
import pe.viadeep.incident.domain.Severity;

@Service
public class AssignSeverityHandler {

    private final IncidentRepository incidents;
    private final Clock clock;

    public AssignSeverityHandler(IncidentRepository incidents, Clock clock) {
        this.incidents = incidents;
        this.clock = clock;
    }

    @Transactional
    public Incident handle(UUID incidentId, Severity severity) {
        Incident incident = incidents.findById(incidentId).orElseThrow(() -> new IncidentNotFoundException(incidentId));
        incident.assignSeverity(severity, clock.instant());
        return incidents.save(incident);
    }
}
