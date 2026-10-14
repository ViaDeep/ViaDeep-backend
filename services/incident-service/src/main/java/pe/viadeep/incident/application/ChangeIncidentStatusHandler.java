package pe.viadeep.incident.application;

import java.time.Clock;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.viadeep.incident.domain.Incident;
import pe.viadeep.incident.domain.IncidentNotFoundException;
import pe.viadeep.incident.domain.IncidentRepository;
import pe.viadeep.incident.domain.IncidentStatus;
import pe.viadeep.incident.domain.IncidentStatusChanged;
import pe.viadeep.incident.domain.StatusChange;

@Service
public class ChangeIncidentStatusHandler {

    private final IncidentRepository incidents;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public ChangeIncidentStatusHandler(IncidentRepository incidents, ApplicationEventPublisher events, Clock clock) {
        this.incidents = incidents;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public Incident handle(UUID incidentId, IncidentStatus target, UUID operatorId, String comment) {
        Incident incident = incidents.findById(incidentId).orElseThrow(() -> new IncidentNotFoundException(incidentId));
        StatusChange change = incident.changeStatus(target, operatorId, comment, clock.instant());
        Incident saved = incidents.save(incident);
        events.publishEvent(new IncidentStatusChanged(saved.id(), saved.reporterId(), saved.type(),
                saved.district().orElse(null), change.fromStatus(), change.toStatus(), change.comment(), saved.createdAt()));
        return saved;
    }
}
