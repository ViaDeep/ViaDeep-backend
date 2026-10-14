package pe.viadeep.notification.domain;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Agregado Notification (OB2). Cada aviso tiene exactamente un destinatario, un usuario o un rol: los de nuevo reporte
 * se dirigen al rol de operador y los de cambio de estado y rechazo, al ciudadano que registró la incidencia.
 */
public final class Notification {

    private static final Map<String, String> TYPE_NAMES = Map.of(
            "D00", "grieta longitudinal",
            "D10", "grieta transversal",
            "D20", "piel de cocodrilo",
            "D40", "bache");
    private static final Map<String, String> STATUS_NAMES = Map.of(
            "PENDING", "Registrada",
            "IN_PROGRESS", "En proceso",
            "RESOLVED", "Resuelta",
            "REJECTED", "Rechazada");

    private final UUID id;
    private final NotificationType type;
    private final UUID recipientUserId;
    private final RecipientRole recipientRole;
    private final IncidentFacts incident;
    private final String title;
    private final String message;
    private final Instant createdAt;

    public Notification(UUID id, NotificationType type, UUID recipientUserId, RecipientRole recipientRole,
                        IncidentFacts incident, String title, String message, Instant createdAt) {
        if ((recipientUserId == null) == (recipientRole == null)) {
            throw new InvalidNotificationException("El aviso debe tener exactamente un destinatario: un usuario o un rol");
        }
        this.id = Objects.requireNonNull(id);
        this.type = Objects.requireNonNull(type);
        this.recipientUserId = recipientUserId;
        this.recipientRole = recipientRole;
        this.incident = Objects.requireNonNull(incident);
        this.title = Objects.requireNonNull(title);
        this.message = Objects.requireNonNull(message);
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    /** Crea el aviso de un evento de incidencia y determina su destinatario según el tipo. */
    public static Notification forEvent(NotificationType type, IncidentFacts incident, Instant now) {
        Objects.requireNonNull(type, "Falta el tipo de aviso");
        if (incident == null || incident.incidentId() == null) {
            throw new InvalidNotificationException("Falta la incidencia del aviso");
        }
        String kind = TYPE_NAMES.getOrDefault(incident.incidentType(), "incidencia vial");
        String place = incident.district() == null || incident.district().isBlank() ? "Lima Metropolitana" : incident.district();
        UUID id = UUID.randomUUID();
        return switch (type) {
            case NEW_REPORT -> new Notification(id, type, null, RecipientRole.OPERATOR, incident,
                    "Nuevo reporte registrado", newReportMessage(kind, place, incident.possibleDuplicates()), now);
            case STATUS_CHANGED -> {
                String status = STATUS_NAMES.getOrDefault(incident.status(), incident.status());
                boolean resolved = "RESOLVED".equals(incident.status());
                yield new Notification(id, type, reporter(incident), null, incident,
                        resolved ? "Tu reporte fue resuelto" : "Tu reporte cambió de estado",
                        resolved
                                ? "La municipalidad atendió tu reporte de " + kind + " en " + place + "."
                                : "Tu reporte de " + kind + " en " + place + " ahora está " + status + ".",
                        now);
            }
            case REJECTED -> new Notification(id, type, reporter(incident), null, incident, "Tu reporte fue rechazado",
                    incident.comment() == null || incident.comment().isBlank()
                            ? "La municipalidad rechazó tu reporte de " + kind + " en " + place + "."
                            : "La municipalidad rechazó tu reporte de " + kind + " en " + place + ". Motivo: "
                                    + incident.comment().strip(),
                    now);
        };
    }

    private static String newReportMessage(String kind, String place, int duplicates) {
        String base = capitalize(kind) + " reportado en " + place + ".";
        if (duplicates <= 0) {
            return base;
        }
        return base + (duplicates == 1
                ? " Posible duplicado: hay 1 reporte abierto cercano del mismo tipo."
                : " Posible duplicado: hay " + duplicates + " reportes abiertos cercanos del mismo tipo.");
    }

    private static UUID reporter(IncidentFacts incident) {
        if (incident.reporterId() == null) {
            throw new InvalidNotificationException("Falta el ciudadano que registró la incidencia");
        }
        return incident.reporterId();
    }

    private static String capitalize(String text) {
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    /** Un aviso es visible para su usuario destinatario o para cualquier usuario del rol destinatario. */
    public boolean isFor(UUID userId, RecipientRole role) {
        return userId.equals(recipientUserId) || (recipientRole != null && recipientRole == role);
    }

    public UUID id() {
        return id;
    }

    public NotificationType type() {
        return type;
    }

    public UUID recipientUserId() {
        return recipientUserId;
    }

    public RecipientRole recipientRole() {
        return recipientRole;
    }

    public IncidentFacts incident() {
        return incident;
    }

    public String title() {
        return title;
    }

    public String message() {
        return message;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
