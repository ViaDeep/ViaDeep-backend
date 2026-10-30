package pe.viadeep.incident.infrastructure.geocoding;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Geocodificación inversa con Nominatim (OpenStreetMap). La política de uso del servidor público exige un
 * User-Agent que identifique a la aplicación y como máximo una consulta por segundo; para mayor volumen se puede
 * apuntar base-url a una instancia propia.
 */
@ConfigurationProperties(prefix = "viadeep.geocoding")
public record GeocodingProperties(boolean enabled, String baseUrl, String userAgent, Duration timeout) {
}
