package pe.viadeep.incident.infrastructure.geocoding;

import java.net.http.HttpClient;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import pe.viadeep.incident.application.DistrictResolver;
import pe.viadeep.incident.domain.GeoLocation;

/** Obtiene el distrito con GET /reverse de Nominatim. Cualquier falla deja el distrito vacío. */
@Component
@EnableConfigurationProperties(GeocodingProperties.class)
class NominatimDistrictResolver implements DistrictResolver {

    private static final Logger log = LoggerFactory.getLogger(NominatimDistrictResolver.class);

    /** Campos de la dirección de Nominatim donde suele estar el distrito, del más al menos específico. */
    private static final List<String> FIELDS = List.of("city_district", "suburb", "town", "city", "municipality", "village", "county");

    /** El distrito se guarda en una columna VARCHAR(80). */
    private static final int MAX_LENGTH = 80;

    private final GeocodingProperties properties;
    private final RestClient rest;

    NominatimDistrictResolver(RestClient.Builder builder, GeocodingProperties properties) {
        this.properties = properties;
        HttpClient http = HttpClient.newBuilder().connectTimeout(properties.timeout()).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(properties.timeout());
        this.rest = builder.baseUrl(properties.baseUrl())
                .defaultHeader(HttpHeaders.USER_AGENT, properties.userAgent())
                .requestFactory(factory)
                .build();
    }

    @Override
    public Optional<String> resolve(GeoLocation location) {
        if (!properties.enabled()) {
            return Optional.empty();
        }
        try {
            ReverseResponse response = rest.get()
                    .uri("/reverse?format=jsonv2&lat={lat}&lon={lon}&zoom=14&addressdetails=1&accept-language=es",
                            location.latitude(), location.longitude())
                    .retrieve()
                    .body(ReverseResponse.class);
            return Optional.ofNullable(response).map(ReverseResponse::address).flatMap(NominatimDistrictResolver::districtOf);
        } catch (RestClientException e) {
            log.warn("No se pudo obtener el distrito de {}: {}", location, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Prefiere un distrito conocido de Lima y el Callao en cualquiera de los campos; si el punto está fuera, usa el
     * primer campo con valor.
     */
    static Optional<String> districtOf(Map<String, String> address) {
        if (address == null) {
            return Optional.empty();
        }
        Optional<String> known = FIELDS.stream()
                .map(address::get)
                .map(LimaDistricts::match)
                .flatMap(Optional::stream)
                .findFirst();
        if (known.isPresent()) {
            return known;
        }
        return FIELDS.stream()
                .map(address::get)
                .filter(value -> value != null && !value.isBlank())
                .map(String::strip)
                .map(value -> value.length() > MAX_LENGTH ? value.substring(0, MAX_LENGTH) : value)
                .findFirst();
    }

    record ReverseResponse(Map<String, String> address) {
    }
}
