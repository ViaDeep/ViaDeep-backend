package pe.viadeep.notification.application;

import java.util.UUID;
import pe.viadeep.notification.domain.RecipientRole;

/** Usuario que hace la solicitud, tomado del JWT. */
public record Requester(UUID userId, RecipientRole role) {
}
