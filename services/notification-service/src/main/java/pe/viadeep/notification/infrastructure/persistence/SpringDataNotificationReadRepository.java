package pe.viadeep.notification.infrastructure.persistence;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataNotificationReadRepository extends JpaRepository<NotificationReadJpaEntity, NotificationReadJpaEntity.Key> {

    @Query("select r.notificationId from NotificationReadJpaEntity r where r.userId = :userId and r.notificationId in :ids")
    List<UUID> findReadIds(@Param("userId") UUID userId, @Param("ids") Collection<UUID> ids);
}
