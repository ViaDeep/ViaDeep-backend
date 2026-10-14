package pe.viadeep.notification.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "notification_reads")
@IdClass(NotificationReadJpaEntity.Key.class)
class NotificationReadJpaEntity {

    @Id
    @Column(name = "notification_id")
    private UUID notificationId;

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "read_at", nullable = false)
    private Instant readAt;

    protected NotificationReadJpaEntity() {
    }

    NotificationReadJpaEntity(UUID notificationId, UUID userId, Instant readAt) {
        this.notificationId = notificationId;
        this.userId = userId;
        this.readAt = readAt;
    }

    static final class Key implements Serializable {

        private UUID notificationId;
        private UUID userId;

        Key() {
        }

        Key(UUID notificationId, UUID userId) {
            this.notificationId = notificationId;
            this.userId = userId;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Key key && Objects.equals(notificationId, key.notificationId)
                    && Objects.equals(userId, key.userId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(notificationId, userId);
        }
    }
}
