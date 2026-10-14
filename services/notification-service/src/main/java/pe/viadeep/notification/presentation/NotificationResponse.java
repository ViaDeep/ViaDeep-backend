package pe.viadeep.notification.presentation;

import java.time.Instant;
import java.util.UUID;
import pe.viadeep.notification.application.NotificationService;
import pe.viadeep.notification.domain.IncidentFacts;
import pe.viadeep.notification.domain.Notification;
import pe.viadeep.notification.domain.NotificationType;

/** Un aviso tal como lo ve su destinatario, con el indicador de lectura de ese usuario. */
record NotificationResponse(UUID id, NotificationType type, UUID incidentId, String incidentType, String district,
                            String incidentStatus, int possibleDuplicates, String comment, Instant reportedAt, String title,
                            String message, Instant createdAt, boolean read) {

    static NotificationResponse from(NotificationService.Item item) {
        Notification n = item.notification();
        IncidentFacts incident = n.incident();
        return new NotificationResponse(n.id(), n.type(), incident.incidentId(), incident.incidentType(),
                incident.district(), incident.status(), incident.possibleDuplicates(), incident.comment(),
                incident.reportedAt(), n.title(), n.message(), n.createdAt(), item.read());
    }
}
