package pe.viadeep.notification.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import pe.viadeep.notification.domain.Notification;
import pe.viadeep.notification.domain.NotificationRepository;
import pe.viadeep.notification.domain.RecipientRole;

@Repository
class NotificationRepositoryJpa implements NotificationRepository {

    private final SpringDataNotificationRepository notifications;
    private final SpringDataNotificationReadRepository reads;

    NotificationRepositoryJpa(SpringDataNotificationRepository notifications, SpringDataNotificationReadRepository reads) {
        this.notifications = notifications;
        this.reads = reads;
    }

    @Override
    public Notification save(Notification notification) {
        return notifications.save(NotificationJpaEntity.fromDomain(notification)).toDomain();
    }

    @Override
    public Optional<Notification> findById(UUID id) {
        return notifications.findById(id).map(NotificationJpaEntity::toDomain);
    }

    @Override
    public List<Notification> findFor(UUID userId, RecipientRole role, boolean onlyUnread, int page, int size) {
        return notifications.findVisible(userId, role, onlyUnread, PageRequest.of(page, size)).stream()
                .map(NotificationJpaEntity::toDomain)
                .toList();
    }

    @Override
    public long countFor(UUID userId, RecipientRole role, boolean onlyUnread) {
        return notifications.countVisible(userId, role, onlyUnread);
    }

    @Override
    public List<UUID> readBy(UUID userId, List<UUID> notificationIds) {
        return notificationIds.isEmpty() ? List.of() : reads.findReadIds(userId, notificationIds);
    }

    @Override
    public void markRead(UUID notificationId, UUID userId, Instant readAt) {
        if (!reads.existsById(new NotificationReadJpaEntity.Key(notificationId, userId))) {
            reads.save(new NotificationReadJpaEntity(notificationId, userId, readAt));
        }
    }

    @Override
    public int markAllRead(UUID userId, RecipientRole role, Instant readAt) {
        return notifications.markAllRead(userId, role.name(), readAt);
    }
}
