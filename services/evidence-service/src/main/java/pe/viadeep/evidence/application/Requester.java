package pe.viadeep.evidence.application;

import java.util.UUID;

/** Usuario que hace la solicitud, tomado del JWT. */
public record Requester(UUID userId, boolean isOperator) {
}
