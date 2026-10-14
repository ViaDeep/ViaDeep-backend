package pe.viadeep.incident.application;

import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.viadeep.incident.domain.Incident;
import pe.viadeep.incident.domain.IncidentRegistered;
import pe.viadeep.incident.domain.IncidentRepository;

@Service
public class RegisterIncidentHandler {

    private final IncidentRepository incidents;
    private final EvidenceGateway evidenceGateway;
    private final IncidentProperties properties;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public RegisterIncidentHandler(IncidentRepository incidents, EvidenceGateway evidenceGateway,
                                   IncidentProperties properties, ApplicationEventPublisher events, Clock clock) {
        this.incidents = incidents;
        this.evidenceGateway = evidenceGateway;
        this.properties = properties;
        this.events = events;
        this.clock = clock;
    }

    /**
     * Verifica que la evidencia exista y sea del ciudadano, crea la incidencia en PENDING y publica
     * IncidentRegistered con los posibles duplicados cercanos.
     */
    @Transactional
    public Incident handle(RegisterIncidentCommand command) {
        EvidenceGateway.VerifiedEvidence evidence = evidenceGateway.find(command.evidenceId(), command.accessToken())
                .filter(found -> found.uploadedBy().equals(command.reporterId()))
                .orElseThrow(() -> new EvidenceRejectedException("La evidencia no existe o no pertenece al usuario"));
        if (incidents.existsByEvidenceId(evidence.id())) {
            throw new EvidenceRejectedException("La evidencia ya está asociada a otra incidencia");
        }
        // El distrito se completa en una siguiente etapa con la geocodificación inversa (OpenStreetMap).
        Incident incident = Incident.register(command.reporterId(), evidence.id(), command.type(), command.location(),
                null, command.description(), command.aiDetection(), clock.instant());
        List<UUID> duplicates = incidents.findOpenNearby(incident.type(), incident.location(),
                properties.duplicateRadiusMeters(), incident.id());
        Incident saved = incidents.save(incident);
        events.publishEvent(new IncidentRegistered(saved.id(), saved.reporterId(), saved.type(), duplicates));
        return saved;
    }
}
