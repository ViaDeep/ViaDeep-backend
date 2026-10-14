package pe.viadeep.incident.application;

import pe.viadeep.incident.domain.IncidentRegistered;
import pe.viadeep.incident.domain.IncidentStatusChanged;

/**
 * Contrato (ACL) que necesita Incidencias para solicitar avisos al servicio de notificaciones. Las implementaciones no
 * lanzan excepciones: si el aviso no puede enviarse, lo registran en los logs (ADR-013).
 */
public interface NotificationGateway {

    void incidentRegistered(IncidentRegistered event);

    void statusChanged(IncidentStatusChanged event);
}
