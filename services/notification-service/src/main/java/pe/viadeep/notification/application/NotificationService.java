package pe.viadeep.notification.application;

import java.time.Clock;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.viadeep.notification.domain.IncidentFacts;
import pe.viadeep.notification.domain.Notification;
import pe.viadeep.notification.domain.NotificationRepository;
import pe.viadeep.notification.domain.NotificationType;

/** Casos de uso: crear el aviso de un evento, listar los avisos del usuario y de su rol, contar y marcar lecturas. */
@Service
public class NotificationService {

    public static final int MAX_PAGE_SIZE = 100;

    private final NotificationRepository notifications;
    private final Clock clock;

    public NotificationService(NotificationRepository notifications, Clock clock) {
        this.notifications = notifications;
        this.clock = clock;
    }

    @Transactional
    public Notification create(NotificationType type, IncidentFacts incident) {
        return notifications.save(Notification.forEvent(type, incident, clock.instant()));
    }

    @Transactional(readOnly = true)
    public Page list(Requester requester, boolean onlyUnread, int page, int size) {
        int safeSize = Math.clamp(size, 1, MAX_PAGE_SIZE);
        int safePage = Math.max(page, 0);
        List<Notification> found = notifications.findFor(requester.userId(), requester.role(), onlyUnread, safePage, safeSize);
        Set<UUID> read = new HashSet<>(notifications.readBy(requester.userId(), found.stream().map(Notification::id).toList()));
        List<Item> items = found.stream().map(n -> new Item(n, read.contains(n.id()))).toList();
        long total = notifications.countFor(requester.userId(), requester.role(), onlyUnread);
        return new Page(items, safePage, safeSize, total);
    }

    @Transactional(readOnly = true)
    public long unreadCount(Requester requester) {
        return notifications.countFor(requester.userId(), requester.role(), true);
    }

    @Transactional
    public void markRead(UUID id, Requester requester) {
        Notification notification = notifications.findById(id)
                .filter(found -> found.isFor(requester.userId(), requester.role()))
                .orElseThrow(() -> new NotificationNotFoundException(id));
        notifications.markRead(notification.id(), requester.userId(), clock.instant());
    }

    @Transactional
    public int markAllRead(Requester requester) {
        return notifications.markAllRead(requester.userId(), requester.role(), clock.instant());
    }

    public record Item(Notification notification, boolean read) {
    }

    public record Page(List<Item> items, int page, int size, long totalItems) {
    }
}
