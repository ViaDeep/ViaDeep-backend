package pe.viadeep.incident.infrastructure.geocoding;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import pe.viadeep.incident.domain.GeoLocation;

/** Prueba la geocodificación inversa contra un servidor HTTP local que imita a Nominatim. */
class NominatimDistrictResolverTest {

    private static final GeoLocation MIRAFLORES = new GeoLocation(-12.1211, -77.0297);

    private HttpServer server;
    private String baseUrl;
    private final AtomicReference<Integer> status = new AtomicReference<>(200);
    private final AtomicReference<String> body = new AtomicReference<>("{}");
    private final AtomicReference<String> query = new AtomicReference<>();
    private final AtomicReference<String> userAgent = new AtomicReference<>();
    private final AtomicReference<Long> delayMillis = new AtomicReference<>(0L);

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/reverse", exchange -> {
            query.set(exchange.getRequestURI().getQuery());
            userAgent.set(exchange.getRequestHeaders().getFirst("User-Agent"));
            try {
                Thread.sleep(delayMillis.get());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            byte[] bytes = body.get().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status.get(), bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private NominatimDistrictResolver resolver(boolean enabled) {
        return new NominatimDistrictResolver(RestClient.builder(),
                new GeocodingProperties(enabled, baseUrl, "ViaDeep-test", Duration.ofMillis(500)));
    }

    @Test
    void readsTheDistrictAndIdentifiesTheApplication() {
        body.set("""
                {"display_name": "Avenida Larco, Miraflores, Lima, Perú",
                 "address": {"road": "Avenida Larco", "suburb": "Miraflores", "city": "Lima", "country": "Perú"}}
                """);

        assertThat(resolver(true).resolve(MIRAFLORES)).contains("Miraflores");
        assertThat(query.get()).contains("lat=-12.1211", "lon=-77.0297", "format=jsonv2", "accept-language=es");
        assertThat(userAgent.get()).isEqualTo("ViaDeep-test");
    }

    @Test
    void failuresAndTimeoutsLeaveTheDistrictEmpty() {
        status.set(503);
        assertThat(resolver(true).resolve(MIRAFLORES)).isEmpty();

        status.set(200);
        delayMillis.set(1500L);
        assertThat(resolver(true).resolve(MIRAFLORES)).isEmpty();

        delayMillis.set(0L);
        body.set("{\"error\": \"Unable to geocode\"}");
        assertThat(resolver(true).resolve(MIRAFLORES)).isEmpty();
    }

    @Test
    void doesNotCallOpenStreetMapWhenDisabled() {
        assertThat(resolver(false).resolve(MIRAFLORES)).isEmpty();
        assertThat(query.get()).isNull();
    }

    @Test
    void prefersAKnownLimaDistrictWhicheverFieldHoldsIt() {
        assertThat(NominatimDistrictResolver.districtOf(Map.of("suburb", "Urb. Las Gardenias", "city", "Santiago de Surco")))
                .contains("Santiago de Surco");
        assertThat(NominatimDistrictResolver.districtOf(Map.of("city_district", "Distrito de Jesus Maria", "city", "Lima")))
                .contains("Jesús María");
        assertThat(NominatimDistrictResolver.districtOf(Map.of("suburb", "Cercado de Lima"))).contains("Lima");
        assertThat(NominatimDistrictResolver.districtOf(Map.of("town", "Lurigancho-Chosica"))).contains("Lurigancho");
        assertThat(NominatimDistrictResolver.districtOf(Map.of("city", "Callao"))).contains("Callao");
    }

    @Test
    void outsideLimaUsesTheFirstAvailableField() {
        assertThat(NominatimDistrictResolver.districtOf(Map.of("city", "Arequipa", "county", "Arequipa"))).contains("Arequipa");
        assertThat(NominatimDistrictResolver.districtOf(Map.of("road", "Panamericana"))).isEmpty();
        assertThat(NominatimDistrictResolver.districtOf(null)).isEmpty();
    }
}
