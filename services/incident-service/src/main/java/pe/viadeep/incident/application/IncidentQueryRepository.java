package pe.viadeep.incident.application;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import pe.viadeep.incident.application.IncidentViews.IncidentDetail;
import pe.viadeep.incident.application.IncidentViews.IncidentFilter;
import pe.viadeep.incident.application.IncidentViews.IncidentSort;
import pe.viadeep.incident.application.IncidentViews.IncidentSummary;
import pe.viadeep.incident.application.IncidentViews.PageResult;
import pe.viadeep.incident.application.IncidentViews.Statistics;

/** Consultas de lectura que devuelven proyecciones sin cargar el agregado. */
public interface IncidentQueryRepository {

    PageResult<IncidentSummary> search(IncidentFilter filter, IncidentSort sort, int page, int size);

    Optional<IncidentDetail> findDetail(UUID id, double duplicateRadiusMeters);

    Statistics statistics(Instant from, Instant to);
}
