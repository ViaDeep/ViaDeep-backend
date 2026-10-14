package pe.viadeep.incident.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Agregado raíz del contexto Incidencias: concentra las reglas de estado, severidad e historial. */
public final class Incident {

    private final UUID id;
    private final UUID reporterId;
    private final UUID evidenceId;
    private final IncidentType type;
    private final GeoLocation location;
    private final String district;
    private final String description;
    private final AiDetection aiDetection;
    private final Instant createdAt;
    private final List<StatusChange> history;
    private IncidentStatus status;
    private Severity severity;
    private Instant updatedAt;

    public Incident(UUID id, UUID reporterId, UUID evidenceId, IncidentType type, GeoLocation location, String district,
                    String description, IncidentStatus status, Severity severity, AiDetection aiDetection,
                    Instant createdAt, Instant updatedAt, List<StatusChange> history) {
        this.id = Objects.requireNonNull(id);
        this.reporterId = Objects.requireNonNull(reporterId);
        this.evidenceId = Objects.requireNonNull(evidenceId);
        this.type = Objects.requireNonNull(type);
        this.location = Objects.requireNonNull(location);
        this.district = district;
        this.description = description;
        this.status = Objects.requireNonNull(status);
        this.severity = severity;
        this.aiDetection = aiDetection;
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
        this.history = new ArrayList<>(history);
    }

    /** Crea la incidencia en PENDING con su primer registro de historial, a nombre de quien la reporta. */
    public static Incident register(UUID reporterId, UUID evidenceId, IncidentType type, GeoLocation location,
                                    String district, String description, AiDetection aiDetection, Instant now) {
        String cleanDescription = description == null || description.isBlank() ? null : description.strip();
        UUID id = UUID.randomUUID();
        StatusChange initial = new StatusChange(UUID.randomUUID(), null, IncidentStatus.PENDING, reporterId, null, now);
        return new Incident(id, reporterId, evidenceId, type, location, district, cleanDescription,
                IncidentStatus.PENDING, null, aiDetection, now, now, List.of(initial));
    }

    /** Aplica una transición válida (PENDING → IN_PROGRESS → RESOLVED, PENDING → REJECTED). El rechazo exige motivo. */
    public StatusChange changeStatus(IncidentStatus target, UUID changedBy, String comment, Instant now) {
        if (!status.canTransitionTo(target)) {
            throw new InvalidStatusTransitionException(status, target);
        }
        String cleanComment = comment == null || comment.isBlank() ? null : comment.strip();
        if (target == IncidentStatus.REJECTED && cleanComment == null) {
            throw new InvalidIncidentException("El rechazo debe indicar el motivo");
        }
        StatusChange change = new StatusChange(UUID.randomUUID(), status, target, changedBy, cleanComment, now);
        status = target;
        updatedAt = now;
        history.add(change);
        return change;
    }

    /** El operador asigna la severidad mientras la incidencia sigue abierta. */
    public void assignSeverity(Severity newSeverity, Instant now) {
        if (!isOpen()) {
            throw new InvalidIncidentException("No se puede cambiar la severidad de una incidencia " + status);
        }
        severity = Objects.requireNonNull(newSeverity);
        updatedAt = now;
    }

    public boolean isOpen() {
        return status == IncidentStatus.PENDING || status == IncidentStatus.IN_PROGRESS;
    }

    public boolean aiValidated() {
        return aiDetection != null;
    }

    public UUID id() {
        return id;
    }

    public UUID reporterId() {
        return reporterId;
    }

    public UUID evidenceId() {
        return evidenceId;
    }

    public IncidentType type() {
        return type;
    }

    public GeoLocation location() {
        return location;
    }

    public Optional<String> district() {
        return Optional.ofNullable(district);
    }

    public Optional<String> description() {
        return Optional.ofNullable(description);
    }

    public IncidentStatus status() {
        return status;
    }

    public Optional<Severity> severity() {
        return Optional.ofNullable(severity);
    }

    public Optional<AiDetection> aiDetection() {
        return Optional.ofNullable(aiDetection);
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public List<StatusChange> history() {
        return Collections.unmodifiableList(history);
    }
}
