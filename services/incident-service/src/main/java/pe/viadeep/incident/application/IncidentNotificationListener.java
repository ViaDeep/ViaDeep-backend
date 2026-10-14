package pe.viadeep.incident.application;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import pe.viadeep.incident.domain.IncidentRegistered;
import pe.viadeep.incident.domain.IncidentStatusChanged;

/**
 * Solicita los avisos una vez confirmada la transacción y en segundo plano (ADR-013): el registro y el cambio de estado
 * responden al usuario sin esperar al servicio de notificaciones, y una falla del aviso no los revierte.
 */
@Component
class IncidentNotificationListener {

    private final NotificationGateway notifications;

    IncidentNotificationListener(NotificationGateway notifications) {
        this.notifications = notifications;
    }

    @Async("notificationExecutor")
    @TransactionalEventListener
    void on(IncidentRegistered event) {
        notifications.incidentRegistered(event);
    }

    @Async("notificationExecutor")
    @TransactionalEventListener
    void on(IncidentStatusChanged event) {
        notifications.statusChanged(event);
    }
}
