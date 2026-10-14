package pe.viadeep.notification.presentation;

import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pe.viadeep.notification.application.NotificationService;
import pe.viadeep.notification.application.Requester;
import pe.viadeep.notification.domain.RecipientRole;

/** Avisos de ciudadanos y operadores: listar, contar los no leídos y marcarlos como leídos. */
@RestController
@RequestMapping("/notifications")
class NotificationController {

    private final NotificationService notificationService;

    NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    PageResponse list(@RequestParam(defaultValue = "false") boolean unread, @RequestParam(defaultValue = "0") int page,
                      @RequestParam(defaultValue = "20") int size, @AuthenticationPrincipal Jwt jwt) {
        NotificationService.Page result = notificationService.list(requester(jwt), unread, page, size);
        return new PageResponse(result.items().stream().map(NotificationResponse::from).toList(), result.page(),
                result.size(), result.totalItems());
    }

    @GetMapping("/unread-count")
    UnreadCount unreadCount(@AuthenticationPrincipal Jwt jwt) {
        return new UnreadCount(notificationService.unreadCount(requester(jwt)));
    }

    @PatchMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void markRead(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        notificationService.markRead(id, requester(jwt));
    }

    @PatchMapping("/read-all")
    MarkedRead markAllRead(@AuthenticationPrincipal Jwt jwt) {
        return new MarkedRead(notificationService.markAllRead(requester(jwt)));
    }

    private static Requester requester(Jwt jwt) {
        RecipientRole role = "OPERATOR".equals(jwt.getClaimAsString("role")) ? RecipientRole.OPERATOR : RecipientRole.CITIZEN;
        return new Requester(UUID.fromString(jwt.getSubject()), role);
    }

    record PageResponse(List<NotificationResponse> items, int page, int size, long totalItems) {
    }

    record UnreadCount(long count) {
    }

    record MarkedRead(int marked) {
    }
}
