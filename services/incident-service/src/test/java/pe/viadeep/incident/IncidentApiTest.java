package pe.viadeep.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import pe.viadeep.incident.application.DistrictResolver;
import pe.viadeep.incident.application.EvidenceGateway;
import pe.viadeep.incident.application.EvidenceGateway.VerifiedEvidence;
import pe.viadeep.incident.application.EvidenceServiceUnavailableException;
import pe.viadeep.incident.application.NotificationGateway;
import pe.viadeep.incident.domain.GeoLocation;
import pe.viadeep.incident.domain.IncidentRegistered;
import pe.viadeep.incident.domain.IncidentStatus;
import pe.viadeep.incident.domain.IncidentStatusChanged;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestDatabaseConfig.class)
@RecordApplicationEvents
class IncidentApiTest {

    /** Aproximadamente 10 metros de latitud. */
    private static final double TEN_METERS = 0.00009;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private ApplicationEvents events;

    @MockitoBean
    private EvidenceGateway evidenceGateway;

    @MockitoBean
    private DistrictResolver districtResolver;

    @MockitoBean
    private NotificationGateway notificationGateway;

    private final UUID citizen = UUID.randomUUID();
    private final UUID operator = UUID.randomUUID();
    // Cada prueba usa un punto distinto de Lima para no detectar duplicados de otras pruebas.
    private final double lat = -12.0 - ThreadLocalRandom.current().nextDouble(0.1);
    private final double lng = -77.0 - ThreadLocalRandom.current().nextDouble(0.1);

    @Test
    void citizenRegistersAnAiValidatedReportAndSeesItsDetail() throws Exception {
        when(districtResolver.resolve(new GeoLocation(lat, lng))).thenReturn(Optional.of("Miraflores"));
        String id = register(citizen, "D40", lat, lng, true).andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.aiValidated", is(true)))
                .andReturn().getResponse().getContentAsString();
        String incidentId = json.readTree(id).get("id").asText();

        mvc.perform(get("/incidents/" + incidentId).with(as(citizen, "CITIZEN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type", is("D40")))
                .andExpect(jsonPath("$.latitude").value(lat))
                .andExpect(jsonPath("$.longitude").value(lng))
                .andExpect(jsonPath("$.aiResult.confidence", is(0.87)))
                .andExpect(jsonPath("$.description", is("Bache frente al mercado")))
                .andExpect(jsonPath("$.district", is("Miraflores")))
                .andExpect(jsonPath("$.history", hasSize(1)))
                .andExpect(jsonPath("$.history[0].toStatus", is("PENDING")))
                .andExpect(jsonPath("$.history[0].changedBy", is(citizen.toString())));

        mvc.perform(get("/incidents/" + incidentId).with(as(UUID.randomUUID(), "CITIZEN"))).andExpect(status().isNotFound());
        mvc.perform(get("/incidents/" + incidentId).with(as(operator, "OPERATOR"))).andExpect(status().isOk());
    }

    @Test
    void acceptsManualReportsAndChecksTheAiResultIsConsistent() throws Exception {
        register(citizen, "D20", lat, lng, false).andExpect(status().isCreated())
                .andExpect(jsonPath("$.aiValidated", is(false)));

        UUID evidence = ownedEvidence(citizen);
        mvc.perform(post("/incidents").with(as(citizen, "CITIZEN")).contentType(MediaType.APPLICATION_JSON).content("""
                        {"evidenceId": "%s", "type": "D20", "latitude": %s, "longitude": %s, "aiValidated": true}
                        """.formatted(evidence, lat, lng)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsInappropriateDescriptions() throws Exception {
        UUID evidence = ownedEvidence(citizen);
        mvc.perform(post("/incidents").with(as(citizen, "CITIZEN")).contentType(MediaType.APPLICATION_JSON).content("""
                        {"evidenceId": "%s", "type": "D40", "latitude": %s, "longitude": %s,
                         "description": "Arreglen esta mierda", "aiValidated": false}
                        """.formatted(evidence, lat, lng)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail", is("La descripción contiene palabras inapropiadas. Describe solo el daño en la vía.")));
    }

    @Test
    void rejectsEvidenceThatIsMissingForeignOrAlreadyUsed() throws Exception {
        UUID foreign = UUID.randomUUID();
        when(evidenceGateway.find(eq(foreign), any())).thenReturn(Optional.of(new VerifiedEvidence(foreign, UUID.randomUUID())));
        registerWith(citizen, foreign).andExpect(status().isUnprocessableEntity());

        UUID missing = UUID.randomUUID();
        when(evidenceGateway.find(eq(missing), any())).thenReturn(Optional.empty());
        registerWith(citizen, missing).andExpect(status().isUnprocessableEntity());

        UUID used = ownedEvidence(citizen);
        registerWith(citizen, used).andExpect(status().isCreated());
        registerWith(citizen, used).andExpect(status().isUnprocessableEntity());

        UUID down = UUID.randomUUID();
        when(evidenceGateway.find(eq(down), any())).thenThrow(new EvidenceServiceUnavailableException(new RuntimeException()));
        registerWith(citizen, down).andExpect(status().isServiceUnavailable());
    }

    @Test
    void detectsOpenReportsOfTheSameTypeNearbyAsPossibleDuplicates() throws Exception {
        String first = idOf(register(citizen, "D40", lat, lng, true));
        register(citizen, "D10", lat + TEN_METERS, lng, true); // otro tipo: no cuenta
        register(citizen, "D40", lat + 20 * TEN_METERS, lng, true); // ~200 m: no cuenta
        String second = idOf(register(UUID.randomUUID(), "D40", lat + TEN_METERS, lng, true));

        mvc.perform(get("/incidents/" + second).with(as(operator, "OPERATOR")))
                .andExpect(jsonPath("$.possibleDuplicates", contains(first)));
        assertThat(events.stream(IncidentRegistered.class))
                .filteredOn(event -> event.incidentId().toString().equals(second))
                .singleElement()
                .satisfies(event -> assertThat(event.possibleDuplicates()).extracting(UUID::toString).containsExactly(first));

        // Una incidencia cerrada deja de contar como duplicado.
        changeStatus(first, "REJECTED", "Duplicado").andExpect(status().isOk());
        mvc.perform(get("/incidents/" + second).with(as(operator, "OPERATOR")))
                .andExpect(jsonPath("$.possibleDuplicates", empty()));
    }

    @Test
    void operatorManagesTheLifecycleAndSeverity() throws Exception {
        String id = idOf(register(citizen, "D00", lat, lng, true));

        changeStatus(id, "RESOLVED", null).andExpect(status().isConflict());
        mvc.perform(patch("/incidents/" + id + "/severity").with(as(operator, "OPERATOR"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"severity\": \"HIGH\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.severity", is("HIGH")));
        changeStatus(id, "IN_PROGRESS", "Cuadrilla asignada").andExpect(status().isOk());
        changeStatus(id, "RESOLVED", null).andExpect(status().isOk()).andExpect(jsonPath("$.status", is("RESOLVED")));

        mvc.perform(get("/incidents/" + id).with(as(citizen, "CITIZEN")))
                .andExpect(jsonPath("$.severity", is("HIGH")))
                .andExpect(jsonPath("$.history[*].toStatus", contains("PENDING", "IN_PROGRESS", "RESOLVED")))
                .andExpect(jsonPath("$.history[1].comment", is("Cuadrilla asignada")))
                .andExpect(jsonPath("$.history[2].changedBy", is(operator.toString())));
        mvc.perform(patch("/incidents/" + id + "/severity").with(as(operator, "OPERATOR"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"severity\": \"LOW\"}"))
                .andExpect(status().isBadRequest());

        assertThat(events.stream(IncidentStatusChanged.class))
                .filteredOn(event -> event.incidentId().toString().equals(id))
                .extracting(IncidentStatusChanged::toStatus)
                .containsExactly(IncidentStatus.IN_PROGRESS, IncidentStatus.RESOLVED);
        // El aviso al ciudadano se solicita en segundo plano, después de confirmar la transacción.
        verify(notificationGateway, timeout(2000)).statusChanged(argThat(event ->
                event.incidentId().toString().equals(id) && event.toStatus() == IncidentStatus.RESOLVED
                        && event.reporterId().equals(citizen)));
        verify(notificationGateway, timeout(2000)).incidentRegistered(argThat(event -> event.incidentId().toString().equals(id)));
    }

    @Test
    void operatorAssignsAndReassignsAnOpenIncident() throws Exception {
        String id = idOf(register(citizen, "D40", lat, lng, true));

        assign(id, "Juan Pérez", "Obras Públicas", "Llevar asfalto en frío").andExpect(status().isOk())
                .andExpect(jsonPath("$.assignee", is("Juan Pérez")))
                .andExpect(jsonPath("$.assignedArea", is("Obras Públicas")))
                .andExpect(jsonPath("$.assignmentNote", is("Llevar asfalto en frío")))
                .andExpect(jsonPath("$.assignedBy", is(operator.toString())))
                .andExpect(jsonPath("$.assignedAt", notNullValue()))
                .andExpect(jsonPath("$.status", is("PENDING")));
        assign(id, "Rosa Díaz", "Mantenimiento vial", null).andExpect(status().isOk())
                .andExpect(jsonPath("$.assignee", is("Rosa Díaz")))
                .andExpect(jsonPath("$.assignmentNote", nullValue()));

        mvc.perform(get("/incidents/" + id).with(as(citizen, "CITIZEN")))
                .andExpect(jsonPath("$.assignee", is("Rosa Díaz")))
                .andExpect(jsonPath("$.assignedArea", is("Mantenimiento vial")))
                .andExpect(jsonPath("$.assignmentNote", nullValue()))
                .andExpect(jsonPath("$.assignedAt", notNullValue()));

        assign(id, " ", "Obras Públicas", null).andExpect(status().isBadRequest());
        assign(id, "Juan Pérez", "x".repeat(121), null).andExpect(status().isBadRequest());
        assign(UUID.randomUUID().toString(), "Juan Pérez", "Obras Públicas", null).andExpect(status().isNotFound());
        mvc.perform(patch("/incidents/" + id + "/assignment").with(as(citizen, "CITIZEN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assignee\": \"Juan\", \"area\": \"Obras\"}"))
                .andExpect(status().isForbidden());

        changeStatus(id, "REJECTED", "Duplicado").andExpect(status().isOk());
        assign(id, "Juan Pérez", "Obras Públicas", null).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", is("No se puede asignar una incidencia REJECTED")));
    }

    @Test
    void listFiltersByAssignmentAndPeriodAndSortsByLastUpdate() throws Exception {
        UUID someone = UUID.randomUUID();
        String older = idOf(register(someone, "D10", lat, lng, true));
        String newer = idOf(register(someone, "D20", lat, lng + 0.01, false));
        assign(older, "Juan Pérez", "Obras Públicas", null).andExpect(status().isOk());

        mvc.perform(get("/incidents").with(as(someone, "CITIZEN")))
                .andExpect(jsonPath("$.items[*].id", contains(newer, older)))
                .andExpect(jsonPath("$.items[0].assignee", nullValue()))
                .andExpect(jsonPath("$.items[0].updatedAt", notNullValue()))
                .andExpect(jsonPath("$.items[1].assignee", is("Juan Pérez")))
                .andExpect(jsonPath("$.items[1].assignedArea", is("Obras Públicas")))
                .andExpect(jsonPath("$.items[1].assignedAt", notNullValue()));
        mvc.perform(get("/incidents").param("sort", "updatedAt").with(as(someone, "CITIZEN")))
                .andExpect(jsonPath("$.items[*].id", contains(older, newer)));
        mvc.perform(get("/incidents").param("assigned", "true").with(as(someone, "CITIZEN")))
                .andExpect(jsonPath("$.items[*].id", contains(older)));
        mvc.perform(get("/incidents").param("assigned", "false").with(as(someone, "CITIZEN")))
                .andExpect(jsonPath("$.items[*].id", contains(newer)));

        LocalDate today = LocalDate.now(ZoneId.of("America/Lima"));
        mvc.perform(get("/incidents").param("from", today.toString()).param("to", today.toString())
                        .with(as(someone, "CITIZEN")))
                .andExpect(jsonPath("$.totalItems", is(2)));
        mvc.perform(get("/incidents").param("from", today.plusDays(1).toString()).with(as(someone, "CITIZEN")))
                .andExpect(jsonPath("$.totalItems", is(0)));
        mvc.perform(get("/incidents").param("to", today.minusDays(1).toString()).with(as(someone, "CITIZEN")))
                .andExpect(jsonPath("$.totalItems", is(0)));
        mvc.perform(get("/incidents").param("from", "2026-10-10").param("to", "2026-10-01")
                .with(as(someone, "CITIZEN"))).andExpect(status().isBadRequest());
        mvc.perform(get("/incidents").param("sort", "type").with(as(someone, "CITIZEN")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectionNeedsAReason() throws Exception {
        String id = idOf(register(citizen, "D00", lat, lng, false));
        changeStatus(id, "REJECTED", "").andExpect(status().isBadRequest());
        changeStatus(id, "REJECTED", "La foto no corresponde a una pista").andExpect(status().isOk());
    }

    @Test
    void appliesRolePermissions() throws Exception {
        String id = idOf(register(citizen, "D00", lat, lng, false));

        mvc.perform(patch("/incidents/" + id + "/status").with(as(citizen, "CITIZEN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\": \"IN_PROGRESS\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/incidents").with(as(operator, "OPERATOR")).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/incidents/stats").with(as(citizen, "CITIZEN"))).andExpect(status().isForbidden());
        mvc.perform(get("/incidents")).andExpect(status().isUnauthorized());
    }

    @Test
    void citizenListsOnlyTheirReportsAndOperatorFilters() throws Exception {
        UUID someone = UUID.randomUUID();
        register(someone, "D10", lat, lng, true);
        register(someone, "D20", lat, lng + 0.01, false);
        register(UUID.randomUUID(), "D10", lat, lng + 0.02, true);

        mvc.perform(get("/incidents").with(as(someone, "CITIZEN")))
                .andExpect(jsonPath("$.totalItems", is(2)))
                .andExpect(jsonPath("$.items", hasSize(2)));
        mvc.perform(get("/incidents").param("type", "D20").with(as(someone, "CITIZEN")))
                .andExpect(jsonPath("$.items[*].type", contains("D20")));
        mvc.perform(get("/incidents").param("type", "D10").param("status", "PENDING").param("size", "500")
                        .with(as(operator, "OPERATOR")))
                .andExpect(jsonPath("$.size", is(100)))
                .andExpect(jsonPath("$.items[*].type", everyItem(is("D10"))));
    }

    @Test
    void statisticsCountTheReportsOfThePeriod() throws Exception {
        JsonNode before = stats();
        String first = idOf(register(citizen, "D40", lat, lng, true));
        register(citizen, "D40", lat, lng + 0.01, false);
        mvc.perform(patch("/incidents/" + first + "/severity").with(as(operator, "OPERATOR"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"severity\": \"HIGH\"}"))
                .andExpect(status().isOk());
        assign(first, "Juan Pérez", "Obras Públicas", null).andExpect(status().isOk());

        JsonNode after = stats();
        assertThat(after.get("total").asLong() - before.get("total").asLong()).isEqualTo(2);
        assertThat(count(after, "bySeverity", "HIGH") - count(before, "bySeverity", "HIGH")).isEqualTo(1);
        assertThat(after.get("assigned").asLong() - before.get("assigned").asLong()).isEqualTo(1);
        assertThat(count(after, "byType", "D40") - count(before, "byType", "D40")).isEqualTo(2);
        assertThat(count(after, "byStatus", "PENDING") - count(before, "byStatus", "PENDING")).isEqualTo(2);
        assertThat(count(after, "byDistrict", "Sin distrito") - count(before, "byDistrict", "Sin distrito")).isEqualTo(2);

        mvc.perform(get("/incidents/stats").param("from", "2026-10-10").param("to", "2026-10-01")
                .with(as(operator, "OPERATOR"))).andExpect(status().isBadRequest());
    }

    private JsonNode stats() throws Exception {
        return json.readTree(mvc.perform(get("/incidents/stats").with(as(operator, "OPERATOR")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    private static long count(JsonNode stats, String group, String key) {
        JsonNode value = stats.get(group).get(key);
        return value == null ? 0 : value.asLong();
    }

    private ResultActions register(UUID reporter, String type, double latitude, double longitude, boolean ai)
            throws Exception {
        UUID evidence = ownedEvidence(reporter);
        String aiResult = ai ? """
                , "aiResult": {"confidence": 0.87, "bboxX": 0.1, "bboxY": 0.2, "bboxWidth": 0.3, "bboxHeight": 0.25}"""
                : "";
        return mvc.perform(post("/incidents").with(as(reporter, "CITIZEN")).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"evidenceId": "%s", "type": "%s", "latitude": %s, "longitude": %s,
                         "description": "Bache frente al mercado", "aiValidated": %s%s}
                        """.formatted(evidence, type, latitude, longitude, ai, aiResult)));
    }

    private ResultActions registerWith(UUID reporter, UUID evidence) throws Exception {
        return mvc.perform(post("/incidents").with(as(reporter, "CITIZEN")).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"evidenceId": "%s", "type": "D40", "latitude": %s, "longitude": %s, "aiValidated": false}
                        """.formatted(evidence, lat, lng)));
    }

    private ResultActions changeStatus(String id, String status, String comment) throws Exception {
        String body = comment == null ? "{\"status\": \"%s\"}".formatted(status)
                : "{\"status\": \"%s\", \"comment\": \"%s\"}".formatted(status, comment);
        return mvc.perform(patch("/incidents/" + id + "/status").with(as(operator, "OPERATOR"))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions assign(String id, String assignee, String area, String note) throws Exception {
        String body = note == null ? "{\"assignee\": \"%s\", \"area\": \"%s\"}".formatted(assignee, area)
                : "{\"assignee\": \"%s\", \"area\": \"%s\", \"note\": \"%s\"}".formatted(assignee, area, note);
        return mvc.perform(patch("/incidents/" + id + "/assignment").with(as(operator, "OPERATOR"))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private String idOf(ResultActions result) throws Exception {
        return json.readTree(result.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString())
                .get("id").asText();
    }

    private UUID ownedEvidence(UUID owner) {
        UUID evidence = UUID.randomUUID();
        when(evidenceGateway.find(eq(evidence), any())).thenReturn(Optional.of(new VerifiedEvidence(evidence, owner)));
        return evidence;
    }

    private static RequestPostProcessor as(UUID userId, String role) {
        return jwt().jwt(token -> token.subject(userId.toString()).claim("role", role))
                .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + role));
    }
}
