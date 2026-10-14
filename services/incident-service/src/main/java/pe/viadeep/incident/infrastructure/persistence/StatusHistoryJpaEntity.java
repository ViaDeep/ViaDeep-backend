package pe.viadeep.incident.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import pe.viadeep.incident.domain.IncidentStatus;
import pe.viadeep.incident.domain.StatusChange;

@Entity
@Table(name = "status_histories")
class StatusHistoryJpaEntity {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 20)
    private IncidentStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 20)
    private IncidentStatus toStatus;

    @Column(name = "changed_by", nullable = false)
    private UUID changedBy;

    @Column(length = 500)
    private String comment;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    protected StatusHistoryJpaEntity() {
    }

    static StatusHistoryJpaEntity fromDomain(StatusChange change) {
        StatusHistoryJpaEntity entity = new StatusHistoryJpaEntity();
        entity.id = change.id();
        entity.fromStatus = change.fromStatus();
        entity.toStatus = change.toStatus();
        entity.changedBy = change.changedBy();
        entity.comment = change.comment();
        entity.changedAt = change.changedAt();
        return entity;
    }

    StatusChange toDomain() {
        return new StatusChange(id, fromStatus, toStatus, changedBy, comment, changedAt);
    }
}
