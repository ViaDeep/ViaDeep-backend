package pe.viadeep.incident.presentation;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.viadeep.incident.application.IncidentQueryService;
import pe.viadeep.incident.application.IncidentViews.IncidentDetail;
import pe.viadeep.incident.application.IncidentViews.IncidentFilter;
import pe.viadeep.incident.application.IncidentViews.IncidentSummary;
import pe.viadeep.incident.application.IncidentViews.PageResult;
import pe.viadeep.incident.application.IncidentViews.Statistics;
import pe.viadeep.incident.domain.IncidentStatus;
import pe.viadeep.incident.domain.IncidentType;
import pe.viadeep.incident.domain.InvalidIncidentException;
import pe.viadeep.incident.domain.Severity;

/** Lecturas (CQRS): listados con filtros, detalle con posibles duplicados y estadísticas. */
@RestController
@RequestMapping("/incidents")
class IncidentQueryController {

    private static final ZoneId LIMA = ZoneId.of("America/Lima");

    private final IncidentQueryService queries;

    IncidentQueryController(IncidentQueryService queries) {
        this.queries = queries;
    }

    @GetMapping
    PageResult<IncidentSummary> search(@RequestParam(required = false) IncidentStatus status,
                                       @RequestParam(required = false) IncidentType type,
                                       @RequestParam(required = false) Severity severity,
                                       @RequestParam(required = false) String district,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "20") int size,
                                       @AuthenticationPrincipal Jwt jwt) {
        IncidentFilter filter = new IncidentFilter(status, type, severity, district, null);
        return queries.search(filter, Jwts.requester(jwt), page, size);
    }

    @GetMapping("/{id}")
    IncidentDetail detail(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return queries.detail(id, Jwts.requester(jwt));
    }

    /** Indicadores por estado, tipo y distrito entre dos fechas (hora de Lima, ambas incluidas). */
    @GetMapping("/stats")
    Statistics statistics(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate end = to == null ? LocalDate.now(LIMA) : to;
        LocalDate start = from == null ? end.minusDays(29) : from;
        if (start.isAfter(end)) {
            throw new InvalidIncidentException("La fecha inicial no puede ser posterior a la final");
        }
        Instant fromInstant = start.atStartOfDay(LIMA).toInstant();
        Instant toInstant = end.plusDays(1).atStartOfDay(LIMA).toInstant();
        return queries.statistics(fromInstant, toInstant);
    }
}
