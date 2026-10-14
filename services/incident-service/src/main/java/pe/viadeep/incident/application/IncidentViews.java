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

    /** assignee, assignedArea y assignedAt son null mientras la incidencia no tiene responsable. */
    public record IncidentSummary(UUID id, IncidentType type, IncidentStatus status, Severity severity, double latitude,
                                  double longitude, String district, boolean aiValidated, Instant createdAt,
                                  Instant updatedAt, String assignee, String assignedArea, Instant assignedAt) {
    }

    public record AiResult(double confidence, double bboxX, double bboxY, double bboxWidth, double bboxHeight) {
    }

    public record HistoryEntry(IncidentStatus fromStatus, IncidentStatus toStatus, UUID changedBy, String comment,
                               Instant changedAt) {
    }

    public record IncidentDetail(UUID id, UUID reporterId, UUID evidenceId, IncidentType type, IncidentStatus status,
                                 Severity severity, double latitude, double longitude, String district,
                                 String description, boolean aiValidated, AiResult aiResult, Instant createdAt,
                                 Instant updatedAt, String assignee, String assignedArea, String assignmentNote,
                                 Instant assignedAt, List<HistoryEntry> history, List<UUID> possibleDuplicates) {
    }

    /**
     * Filtros del listado; los null no filtran. assigned: true solo con responsable, false solo sin responsable.
     * createdFrom (incluido) y createdTo (excluido) acotan la fecha de registro.
     */
    public record IncidentFilter(IncidentStatus status, IncidentType type, Severity severity, String district,
                                 UUID reporterId, Boolean assigned, Instant createdFrom, Instant createdTo) {

        /** El mismo filtro restringido a los reportes de un ciudadano. */
        public IncidentFilter forReporter(UUID reporter) {
            return new IncidentFilter(status, type, severity, district, reporter, assigned, createdFrom, createdTo);
        }
    }

    /** Orden del listado, siempre del más reciente al más antiguo. */
    public enum IncidentSort {
        CREATED_AT,
        UPDATED_AT
    }

    public record PageResult<T>(List<T> items, int page, int size, long totalItems) {
    }

    /** bySeverity omite las incidencias aún sin severidad; assigned cuenta las que tienen responsable. */
    public record Statistics(long total, Map<IncidentStatus, Long> byStatus, Map<IncidentType, Long> byType,
                             Map<String, Long> byDistrict, Map<Severity, Long> bySeverity, long assigned) {
    }
}
