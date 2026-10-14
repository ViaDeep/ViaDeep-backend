package pe.viadeep.incident.application;

import java.util.Optional;
import pe.viadeep.incident.domain.GeoLocation;

/**
 * Contrato que necesita Incidencias para conocer el distrito de un punto GPS (geocodificación inversa).
 * Devuelve vacío si no se pudo determinar: el registro del reporte nunca depende de este servicio.
 */
public interface DistrictResolver {

    Optional<String> resolve(GeoLocation location);
}
