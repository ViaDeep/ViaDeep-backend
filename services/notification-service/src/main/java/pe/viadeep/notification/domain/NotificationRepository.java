package pe.viadeep.notification.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository {

    Notification save(Notification notification);

    Optional<Notification> findById(UUID id);

    /** Avisos del usuario y de su rol, del más reciente al más antiguo; con onlyUnread, solo los que no leyó. */
    List<Notification> findFor(UUID userId, RecipientRole role, boolean onlyUnread, int page, int size);

    long countFor(UUID userId, RecipientRole role, boolean onlyUnread);

    /** Ids, entre los dados, que el usuario ya leyó. */
    List<UUID> readBy(UUID userId, List<UUID> notificationIds);

    /** Registra la lectura; si ya estaba leído, no hace nada. */
    void markRead(UUID notificationId, UUID userId, Instant readAt);

    /** Marca como leídos todos los avisos pendientes del usuario y de su rol. Devuelve cuántos marcó. */
    int markAllRead(UUID userId, RecipientRole role, Instant readAt);
}
