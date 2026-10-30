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
    private final DistrictResolver districts;
    private final IncidentProperties properties;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public RegisterIncidentHandler(IncidentRepository incidents, EvidenceGateway evidenceGateway, DistrictResolver districts,
                                   IncidentProperties properties, ApplicationEventPublisher events, Clock clock) {
        this.incidents = incidents;
        this.evidenceGateway = evidenceGateway;
        this.districts = districts;
        this.properties = properties;
        this.events = events;
        this.clock = clock;
    }

    /**
     * Verifica que la evidencia exista y sea del ciudadano, completa el distrito a partir del GPS, crea la incidencia
     * en PENDING y publica IncidentRegistered con los posibles duplicados cercanos.
     */
    @Transactional
    public Incident handle(RegisterIncidentCommand command) {
        EvidenceGateway.VerifiedEvidence evidence = evidenceGateway.find(command.evidenceId(), command.accessToken())
                .filter(found -> found.uploadedBy().equals(command.reporterId()))
                .orElseThrow(() -> new EvidenceRejectedException("La evidencia no existe o no pertenece al usuario"));
        if (incidents.existsByEvidenceId(evidence.id())) {
            throw new EvidenceRejectedException("La evidencia ya está asociada a otra incidencia");
        }
        String district = districts.resolve(command.location()).orElse(null);
        Incident incident = Incident.register(command.reporterId(), evidence.id(), command.type(), command.location(),
                district, command.description(), command.aiDetection(), clock.instant());
        List<UUID> duplicates = incidents.findOpenNearby(incident.type(), incident.location(),
                properties.duplicateRadiusMeters(), incident.id());
        Incident saved = incidents.save(incident);
        events.publishEvent(new IncidentRegistered(saved.id(), saved.reporterId(), saved.type(), saved.district().orElse(null),
                duplicates, saved.createdAt()));
        return saved;
    }
}
