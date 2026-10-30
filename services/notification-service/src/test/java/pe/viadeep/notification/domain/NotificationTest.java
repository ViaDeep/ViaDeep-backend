package pe.viadeep.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NotificationTest {

    private static final Instant NOW = Instant.parse("2026-10-14T15:00:00Z");
    private static final UUID REPORTER = UUID.randomUUID();

    private static IncidentFacts facts(String status, int duplicates, String comment) {
        return new IncidentFacts(UUID.randomUUID(), REPORTER, "D40", "San Borja", status, duplicates, comment, NOW);
    }

    @Test
    void newReportGoesToOperatorsAndMentionsPossibleDuplicates() {
        Notification n = Notification.forEvent(NotificationType.NEW_REPORT, facts("PENDING", 2, null), NOW);

        assertThat(n.recipientRole()).isEqualTo(RecipientRole.OPERATOR);
        assertThat(n.recipientUserId()).isNull();
        assertThat(n.message()).isEqualTo(
                "Bache reportado en San Borja. Posible duplicado: hay 2 reportes abiertos cercanos del mismo tipo.");
    }

    @Test
    void statusChangeAndRejectionGoToTheCitizenWhoReported() {
        Notification inProgress = Notification.forEvent(NotificationType.STATUS_CHANGED, facts("IN_PROGRESS", 0, null), NOW);
        Notification resolved = Notification.forEvent(NotificationType.STATUS_CHANGED, facts("RESOLVED", 0, null), NOW);
        Notification rejected = Notification.forEvent(NotificationType.REJECTED, facts("REJECTED", 0, "Foto borrosa"), NOW);

        assertThat(inProgress.recipientUserId()).isEqualTo(REPORTER);
        assertThat(inProgress.message()).isEqualTo("Tu reporte de bache en San Borja ahora está En proceso.");
        assertThat(resolved.title()).isEqualTo("Tu reporte fue resuelto");
        assertThat(rejected.recipientUserId()).isEqualTo(REPORTER);
        assertThat(rejected.message()).endsWith("Motivo: Foto borrosa");
        assertThat(rejected.isFor(REPORTER, RecipientRole.CITIZEN)).isTrue();
        assertThat(rejected.isFor(UUID.randomUUID(), RecipientRole.OPERATOR)).isFalse();
    }

    @Test
    void requiresExactlyOneRecipient() {
        IncidentFacts facts = facts("PENDING", 0, null);
        assertThatThrownBy(() -> new Notification(UUID.randomUUID(), NotificationType.NEW_REPORT, REPORTER,
                RecipientRole.OPERATOR, facts, "t", "m", NOW)).isInstanceOf(InvalidNotificationException.class);
        assertThatThrownBy(() -> Notification.forEvent(NotificationType.STATUS_CHANGED,
                new IncidentFacts(UUID.randomUUID(), null, "D40", null, "RESOLVED", 0, null, NOW), NOW))
                .isInstanceOf(InvalidNotificationException.class);
    }
}
