package pe.viadeep.incident.application;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import pe.viadeep.incident.domain.IncidentStatus;
import pe.viadeep.incident.domain.IncidentType;
import pe.viadeep.incident.domain.Severity;

/** Proyecciones de lectura (lado de consultas de CQRS). */
public final class IncidentViews {

    private IncidentViews() {
    }

    public record IncidentSummary(UUID id, IncidentType type, IncidentStatus status, Severity severity, double latitude,
                                  double longitude, String district, boolean aiValidated, Instant createdAt) {
    }

    public record AiResult(double confidence, double bboxX, double bboxY, double bboxWidth, double bboxHeight) {
    }

    public record HistoryEntry(IncidentStatus fromStatus, IncidentStatus toStatus, UUID changedBy, String comment,
                               Instant changedAt) {
    }

    public record IncidentDetail(UUID id, UUID reporterId, UUID evidenceId, IncidentType type, IncidentStatus status,
                                 Severity severity, double latitude, double longitude, String district,
                                 String description, boolean aiValidated, AiResult aiResult, Instant createdAt,
                                 Instant updatedAt, List<HistoryEntry> history, List<UUID> possibleDuplicates) {
    }

    public record IncidentFilter(IncidentStatus status, IncidentType type, Severity severity, String district,
                                 UUID reporterId) {
    }

    public record PageResult<T>(List<T> items, int page, int size, long totalItems) {
    }

    public record Statistics(long total, Map<IncidentStatus, Long> byStatus, Map<IncidentType, Long> byType,
                             Map<String, Long> byDistrict) {
    }
}
