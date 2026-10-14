package pe.viadeep.notification.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.viadeep.notification.domain.RecipientRole;

interface SpringDataNotificationRepository extends JpaRepository<NotificationJpaEntity, UUID> {

    String VISIBLE = """
            (n.recipientUserId = :userId or n.recipientRole = :role)
            and (:onlyUnread = false or not exists (
                select 1 from NotificationReadJpaEntity r where r.notificationId = n.id and r.userId = :userId))
            """;

    @Query("select n from NotificationJpaEntity n where " + VISIBLE + " order by n.createdAt desc, n.id")
    List<NotificationJpaEntity> findVisible(@Param("userId") UUID userId, @Param("role") RecipientRole role,
                                            @Param("onlyUnread") boolean onlyUnread, Pageable page);

    @Query("select count(n) from NotificationJpaEntity n where " + VISIBLE)
    long countVisible(@Param("userId") UUID userId, @Param("role") RecipientRole role,
                      @Param("onlyUnread") boolean onlyUnread);

    @Modifying
    @Query(nativeQuery = true, value = """
            insert into {h-schema}notification_reads (notification_id, user_id, read_at)
            select n.id, :userId, :readAt from {h-schema}notifications n
            where (n.recipient_user_id = :userId or n.recipient_role = :role)
              and not exists (select 1 from {h-schema}notification_reads r where r.notification_id = n.id and r.user_id = :userId)
            """)
    int markAllRead(@Param("userId") UUID userId, @Param("role") String role, @Param("readAt") Instant readAt);
}
