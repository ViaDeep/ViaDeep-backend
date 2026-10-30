package pe.viadeep.notification.presentation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import pe.viadeep.notification.application.InternalApiProperties;
import pe.viadeep.notification.application.NotificationService;
import pe.viadeep.notification.domain.IncidentFacts;
import pe.viadeep.notification.domain.Notification;
import pe.viadeep.notification.domain.NotificationType;

/**
 * Interfaz pública del contexto para el servicio de incidencias (NotificationClient): crea el aviso de un evento.
 * No se expone en el API Gateway y exige la clave interna en la cabecera X-Internal-Token.
 */
@RestController
@RequestMapping("/notifications")
class InternalNotificationController {

    static final String TOKEN_HEADER = "X-Internal-Token";

    private final NotificationService notificationService;
    private final byte[] token;

    InternalNotificationController(NotificationService notificationService, InternalApiProperties properties) {
        this.notificationService = notificationService;
        this.token = properties.token() == null ? new byte[0] : properties.token().getBytes(StandardCharsets.UTF_8);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    Created create(@RequestHeader(value = TOKEN_HEADER, required = false) String providedToken,
                   @Valid @RequestBody CreateNotificationRequest request) {
        if (!authorized(providedToken)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Clave interna inválida");
        }
        IncidentFacts incident = new IncidentFacts(request.incidentId(), request.reporterId(), request.incidentType(),
                request.district(), request.status(), request.possibleDuplicates(), request.comment(), request.reportedAt());
        Notification created = notificationService.create(request.type(), incident);
        return new Created(created.id());
    }

    /** Sin clave configurada no se acepta ninguna solicitud. */
    private boolean authorized(String providedToken) {
        return token.length > 0 && providedToken != null
                && MessageDigest.isEqual(token, providedToken.getBytes(StandardCharsets.UTF_8));
    }

    record CreateNotificationRequest(@NotNull NotificationType type, @NotNull UUID incidentId, UUID reporterId,
                                     @NotBlank @Size(max = 3) String incidentType, @Size(max = 80) String district,
                                     @NotBlank @Size(max = 20) String status, @Min(0) int possibleDuplicates,
                                     @Size(max = 500) String comment, @NotNull Instant reportedAt) {
    }

    record Created(UUID id) {
    }
}
