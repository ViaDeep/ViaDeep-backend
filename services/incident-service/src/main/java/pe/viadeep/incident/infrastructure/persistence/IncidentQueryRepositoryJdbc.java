package pe.viadeep.incident.infrastructure.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import pe.viadeep.incident.application.IncidentQueryRepository;
import pe.viadeep.incident.application.IncidentViews.AiResult;
import pe.viadeep.incident.application.IncidentViews.HistoryEntry;
import pe.viadeep.incident.application.IncidentViews.IncidentDetail;
import pe.viadeep.incident.application.IncidentViews.IncidentFilter;
import pe.viadeep.incident.application.IncidentViews.IncidentSummary;
import pe.viadeep.incident.application.IncidentViews.PageResult;
import pe.viadeep.incident.application.IncidentViews.Statistics;
import pe.viadeep.incident.domain.IncidentStatus;
import pe.viadeep.incident.domain.IncidentType;
import pe.viadeep.incident.domain.Severity;

/** Lado de lectura: SQL directo que devuelve proyecciones, sin cargar el agregado. */
@Repository
class IncidentQueryRepositoryJdbc implements IncidentQueryRepository {

    private static final String SELECT = """
            SELECT i.id, i.reporter_id, i.evidence_id, t.code, i.status, i.severity,
                   ST_Y(i.location::geometry) AS latitude, ST_X(i.location::geometry) AS longitude,
                   i.district, i.description, i.ai_validated, i.confidence,
                   i.bbox_x, i.bbox_y, i.bbox_width, i.bbox_height, i.created_at, i.updated_at
            FROM incidents i JOIN incident_types t ON t.id = i.incident_type_id
            """;

    private final JdbcClient jdbc;

    IncidentQueryRepositoryJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public PageResult<IncidentSummary> search(IncidentFilter filter, int page, int size) {
        List<String> conditions = new ArrayList<>();
        Map<String, Object> params = new LinkedHashMap<>();
        if (filter.status() != null) {
            conditions.add("i.status = :status");
            params.put("status", filter.status().name());
        }
        if (filter.type() != null) {
            conditions.add("t.code = :type");
            params.put("type", filter.type().name());
        }
        if (filter.severity() != null) {
            conditions.add("i.severity = :severity");
            params.put("severity", filter.severity().name());
        }
        if (filter.district() != null && !filter.district().isBlank()) {
            conditions.add("i.district = :district");
            params.put("district", filter.district().strip());
        }
        if (filter.reporterId() != null) {
            conditions.add("i.reporter_id = :reporterId");
            params.put("reporterId", filter.reporterId());
        }
        String where = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);

        long total = jdbc.sql("SELECT count(*) FROM incidents i JOIN incident_types t ON t.id = i.incident_type_id" + where)
                .params(params).query(Long.class).single();
        List<IncidentSummary> items = jdbc.sql(SELECT + where + " ORDER BY i.created_at DESC LIMIT :limit OFFSET :offset")
                .params(params)
                .param("limit", size)
                .param("offset", (long) page * size)
                .query((rs, row) -> new IncidentSummary(uuid(rs, "id"), IncidentType.valueOf(rs.getString("code")),
                        IncidentStatus.valueOf(rs.getString("status")), severity(rs), rs.getDouble("latitude"),
                        rs.getDouble("longitude"), rs.getString("district"), rs.getBoolean("ai_validated"),
                        instant(rs, "created_at")))
                .list();
        return new PageResult<>(items, page, size, total);
    }

    @Override
    public Optional<IncidentDetail> findDetail(UUID id, double duplicateRadiusMeters) {
        return jdbc.sql(SELECT + " WHERE i.id = :id")
                .param("id", id)
                .query((rs, row) -> detail(rs, history(id), duplicates(id, duplicateRadiusMeters)))
                .optional();
    }

    @Override
    public Statistics statistics(Instant from, Instant to) {
        String where = " WHERE i.created_at >= :from AND i.created_at < :to";
        Map<String, Object> range = Map.of("from", OffsetDateTime.ofInstant(from, java.time.ZoneOffset.UTC),
                "to", OffsetDateTime.ofInstant(to, java.time.ZoneOffset.UTC));

        Map<IncidentStatus, Long> byStatus = new EnumMap<>(IncidentStatus.class);
        jdbc.sql("SELECT i.status AS k, count(*) AS n FROM incidents i" + where + " GROUP BY i.status")
                .params(range)
                .query(rs -> {
                    byStatus.put(IncidentStatus.valueOf(rs.getString("k")), rs.getLong("n"));
                });
        Map<IncidentType, Long> byType = new EnumMap<>(IncidentType.class);
        jdbc.sql("SELECT t.code AS k, count(*) AS n FROM incidents i JOIN incident_types t ON t.id = i.incident_type_id"
                        + where + " GROUP BY t.code")
                .params(range)
                .query(rs -> {
                    byType.put(IncidentType.valueOf(rs.getString("k")), rs.getLong("n"));
                });
        Map<String, Long> byDistrict = new LinkedHashMap<>();
        jdbc.sql("SELECT coalesce(i.district, 'Sin distrito') AS k, count(*) AS n FROM incidents i" + where
                        + " GROUP BY 1 ORDER BY 2 DESC, 1")
                .params(range)
                .query(rs -> {
                    byDistrict.put(rs.getString("k"), rs.getLong("n"));
                });
        long total = byStatus.values().stream().mapToLong(Long::longValue).sum();
        return new Statistics(total, byStatus, byType, byDistrict);
    }

    private List<HistoryEntry> history(UUID incidentId) {
        return jdbc.sql("""
                        SELECT from_status, to_status, changed_by, comment, changed_at
                        FROM status_histories WHERE incident_id = :id ORDER BY changed_at, id
                        """)
                .param("id", incidentId)
                .query((rs, row) -> new HistoryEntry(
                        rs.getString("from_status") == null ? null : IncidentStatus.valueOf(rs.getString("from_status")),
                        IncidentStatus.valueOf(rs.getString("to_status")), uuid(rs, "changed_by"),
                        rs.getString("comment"), instant(rs, "changed_at")))
                .list();
    }

    /** Otras incidencias abiertas del mismo tipo dentro del radio configurado. */
    private List<UUID> duplicates(UUID incidentId, double radiusMeters) {
        return jdbc.sql("""
                        SELECT other.id FROM incidents me
                        JOIN incidents other
                          ON other.incident_type_id = me.incident_type_id
                         AND other.id <> me.id
                         AND other.status IN ('PENDING', 'IN_PROGRESS')
                         AND ST_DWithin(other.location, me.location, :radius)
                        WHERE me.id = :id
                        ORDER BY other.created_at
                        """)
                .param("id", incidentId)
                .param("radius", radiusMeters)
                .query(UUID.class)
                .list();
    }

    private static IncidentDetail detail(ResultSet rs, List<HistoryEntry> history, List<UUID> duplicates)
            throws SQLException {
        boolean aiValidated = rs.getBoolean("ai_validated");
        AiResult ai = aiValidated ? new AiResult(rs.getDouble("confidence"), rs.getDouble("bbox_x"),
                rs.getDouble("bbox_y"), rs.getDouble("bbox_width"), rs.getDouble("bbox_height")) : null;
        return new IncidentDetail(uuid(rs, "id"), uuid(rs, "reporter_id"), uuid(rs, "evidence_id"),
                IncidentType.valueOf(rs.getString("code")), IncidentStatus.valueOf(rs.getString("status")),
                severity(rs), rs.getDouble("latitude"), rs.getDouble("longitude"), rs.getString("district"),
                rs.getString("description"), aiValidated, ai, instant(rs, "created_at"), instant(rs, "updated_at"),
                history, duplicates);
    }

    private static Severity severity(ResultSet rs) throws SQLException {
        String value = rs.getString("severity");
        return value == null ? null : Severity.valueOf(value);
    }

    private static UUID uuid(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, UUID.class);
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, OffsetDateTime.class).toInstant();
    }
}
