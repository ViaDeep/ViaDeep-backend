package pe.viadeep.incident.application;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.viadeep.incident.application.IncidentViews.IncidentDetail;
import pe.viadeep.incident.application.IncidentViews.IncidentFilter;
import pe.viadeep.incident.application.IncidentViews.IncidentSort;
import pe.viadeep.incident.application.IncidentViews.IncidentSummary;
import pe.viadeep.incident.application.IncidentViews.PageResult;
import pe.viadeep.incident.application.IncidentViews.Statistics;
import pe.viadeep.incident.domain.IncidentNotFoundException;

@Service
@Transactional(readOnly = true)
public class IncidentQueryService {

    private static final int MAX_PAGE_SIZE = 100;

    private final IncidentQueryRepository queries;
    private final IncidentProperties properties;

    public IncidentQueryService(IncidentQueryRepository queries, IncidentProperties properties) {
        this.queries = queries;
        this.properties = properties;
    }

    /** Un ciudadano solo ve sus propios reportes; el operador ve todos. */
    public PageResult<IncidentSummary> search(IncidentFilter filter, IncidentSort sort, Requester requester, int page,
                                              int size) {
        IncidentFilter scoped = requester.isOperator() ? filter : filter.forReporter(requester.userId());
        return queries.search(scoped, sort == null ? IncidentSort.CREATED_AT : sort, Math.max(page, 0),
                Math.clamp(size, 1, MAX_PAGE_SIZE));
    }

    public IncidentDetail detail(UUID id, Requester requester) {
        IncidentDetail detail = queries.findDetail(id, properties.duplicateRadiusMeters())
                .orElseThrow(() -> new IncidentNotFoundException(id));
        if (!requester.isOperator() && !detail.reporterId().equals(requester.userId())) {
            // Para otro ciudadano la incidencia no existe: no se revela su existencia.
            throw new IncidentNotFoundException(id);
        }
        return detail;
    }

    public Statistics statistics(Instant from, Instant to) {
        return queries.statistics(from, to);
    }
}
