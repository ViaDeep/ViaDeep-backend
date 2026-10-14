package pe.viadeep.notification.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import pe.viadeep.notification.domain.IncidentFacts;
import pe.viadeep.notification.domain.Notification;
import pe.viadeep.notification.domain.NotificationType;
import pe.viadeep.notification.domain.RecipientRole;

@Entity
@Table(name = "notifications")
class NotificationJpaEntity {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationType type;

    @Column(name = "recipient_user_id")
    private UUID recipientUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "recipient_role", length = 10)
    private RecipientRole recipientRole;

    @Column(name = "incident_id", nullable = false)
    private UUID incidentId;

    @Column(name = "incident_type", nullable = false, length = 3)
    private String incidentType;

    @Column(length = 80)
    private String district;

    @Column(name = "incident_status", nullable = false, length = 20)
    private String incidentStatus;

    @Column(name = "possible_duplicates", nullable = false)
    private int possibleDuplicates;

    @Column(length = 500)
    private String comment;

    @Column(name = "reported_at", nullable = false)
    private Instant reportedAt;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 700)
    private String message;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected NotificationJpaEntity() {
    }

    static NotificationJpaEntity fromDomain(Notification notification) {
        NotificationJpaEntity entity = new NotificationJpaEntity();
        IncidentFacts incident = notification.incident();
        entity.id = notification.id();
        entity.type = notification.type();
        entity.recipientUserId = notification.recipientUserId();
        entity.recipientRole = notification.recipientRole();
        entity.incidentId = incident.incidentId();
        entity.incidentType = incident.incidentType();
        entity.district = incident.district();
        entity.incidentStatus = incident.status();
        entity.possibleDuplicates = incident.possibleDuplicates();
        entity.comment = incident.comment();
        entity.reportedAt = incident.reportedAt();
        entity.title = notification.title();
        entity.message = notification.message();
        entity.createdAt = notification.createdAt();
        return entity;
    }

    Notification toDomain() {
        // reporterId no se guarda: el destinatario ya identifica al ciudadano cuando el aviso es para él.
        IncidentFacts incident = new IncidentFacts(incidentId, recipientUserId, incidentType, district, incidentStatus,
                possibleDuplicates, comment, reportedAt);
        return new Notification(id, type, recipientUserId, recipientRole, incident, title, message, createdAt);
    }
}
