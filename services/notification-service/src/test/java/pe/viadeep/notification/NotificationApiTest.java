package pe.viadeep.notification;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class NotificationApiTest {

    private static final String TOKEN = "test-internal-token";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Test
    void newReportIsVisibleToEveryOperatorWithItsOwnReadState() throws Exception {
        create("NEW_REPORT", UUID.randomUUID(), "PENDING", null);
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        mvc.perform(get("/notifications").with(user(first, "OPERATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].type", is("NEW_REPORT")))
                .andExpect(jsonPath("$.items[0].district", is("San Borja")))
                .andExpect(jsonPath("$.items[0].read", is(false)))
                .andExpect(jsonPath("$.totalItems", is(1)));
        String id = firstId(first, "OPERATOR");

        mvc.perform(patch("/notifications/" + id + "/read").with(user(first, "OPERATOR"))).andExpect(status().isNoContent());
        mvc.perform(patch("/notifications/" + id + "/read").with(user(first, "OPERATOR"))).andExpect(status().isNoContent());

        mvc.perform(get("/notifications/unread-count").with(user(first, "OPERATOR"))).andExpect(jsonPath("$.count", is(0)));
        mvc.perform(get("/notifications/unread-count").with(user(second, "OPERATOR"))).andExpect(jsonPath("$.count", is(1)));
        mvc.perform(get("/notifications?unread=true").with(user(first, "OPERATOR"))).andExpect(jsonPath("$.items", hasSize(0)));
    }

    @Test
    void statusChangeIsOnlyForTheCitizenWhoReported() throws Exception {
        UUID reporter = UUID.randomUUID();
        create("STATUS_CHANGED", reporter, "IN_PROGRESS", null);
        create("REJECTED", reporter, "REJECTED", "Foto borrosa");

        mvc.perform(get("/notifications").with(user(reporter, "CITIZEN")))
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].type", is("REJECTED")))
                .andExpect(jsonPath("$.items[0].comment", is("Foto borrosa")));
        mvc.perform(get("/notifications").with(user(UUID.randomUUID(), "CITIZEN"))).andExpect(jsonPath("$.items", hasSize(0)));
        mvc.perform(get("/notifications").with(user(UUID.randomUUID(), "OPERATOR"))).andExpect(jsonPath("$.items", hasSize(0)));

        String id = firstId(reporter, "CITIZEN");
        mvc.perform(patch("/notifications/" + id + "/read").with(user(UUID.randomUUID(), "CITIZEN")))
                .andExpect(status().isNotFound());

        mvc.perform(patch("/notifications/read-all").with(user(reporter, "CITIZEN"))).andExpect(jsonPath("$.marked", is(2)));
        mvc.perform(get("/notifications/unread-count").with(user(reporter, "CITIZEN"))).andExpect(jsonPath("$.count", is(0)));
    }

    @Test
    void creatingRequiresTheInternalTokenAndListingRequiresAJwt() throws Exception {
        mvc.perform(post("/notifications").contentType(MediaType.APPLICATION_JSON).content(body("NEW_REPORT", null, "PENDING", null)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/notifications").header("X-Internal-Token", "otra").contentType(MediaType.APPLICATION_JSON)
                .content(body("NEW_REPORT", null, "PENDING", null))).andExpect(status().isForbidden());
        mvc.perform(get("/notifications")).andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsAStatusChangeWithoutReporter() throws Exception {
        mvc.perform(post("/notifications").header("X-Internal-Token", TOKEN).contentType(MediaType.APPLICATION_JSON)
                .content(body("STATUS_CHANGED", null, "RESOLVED", null))).andExpect(status().isBadRequest());
    }

    private void create(String type, UUID reporter, String status, String comment) throws Exception {
        mvc.perform(post("/notifications").header("X-Internal-Token", TOKEN).contentType(MediaType.APPLICATION_JSON)
                        .content(body(type, reporter, status, comment)))
                .andExpect(status().isCreated());
    }

    private String body(String type, UUID reporter, String status, String comment) throws Exception {
        var request = new java.util.LinkedHashMap<String, Object>();
        request.put("type", type);
        request.put("incidentId", UUID.randomUUID());
        request.put("reporterId", reporter);
        request.put("incidentType", "D40");
        request.put("district", "San Borja");
        request.put("status", status);
        request.put("possibleDuplicates", 0);
        request.put("comment", comment);
        request.put("reportedAt", "2026-10-14T15:00:00Z");
        return json.writeValueAsString(request);
    }

    private String firstId(UUID userId, String role) throws Exception {
        String response = mvc.perform(get("/notifications").with(user(userId, role))).andReturn().getResponse().getContentAsString();
        JsonNode node = json.readTree(response);
        return node.get("items").get(0).get("id").asText();
    }

    private static RequestPostProcessor user(UUID id, String role) {
        return jwt().jwt(token -> token.subject(id.toString()).claim("role", role));
    }
}
