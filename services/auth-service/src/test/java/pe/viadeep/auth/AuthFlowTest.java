package pe.viadeep.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.SignedJWT;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthFlowTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Test
    void registersLogsInAndReadsProfileWithTheIssuedToken() throws Exception {
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"email": "Ana@Correo.pe", "password": "secreta123", "fullName": "Ana Quispe"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email", is("ana@correo.pe")))
                .andExpect(jsonPath("$.role", is("CITIZEN")));

        String token = login("ana@correo.pe", "secreta123");

        mvc.perform(get("/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName", is("Ana Quispe")))
                .andExpect(jsonPath("$.enabled", is(true)))
                .andExpect(jsonPath("$.createdAt", notNullValue()))
                .andExpect(jsonPath("$.lastLoginAt", notNullValue()));
    }

    @Test
    void issuedTokenIsSignedWithRs256AndVerifiableWithThePublishedJwks() throws Exception {
        register("luis@correo.pe");
        SignedJWT jwt = SignedJWT.parse(login("luis@correo.pe", "secreta123"));

        String jwks = mvc.perform(get("/auth/.well-known/jwks.json"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        RSAKey publicKey = JWKSet.parse(jwks).getKeyByKeyId(jwt.getHeader().getKeyID()).toRSAKey();

        assertThat(publicKey.isPrivate()).isFalse();
        assertThat(jwt.getHeader().getAlgorithm().getName()).isEqualTo("RS256");
        assertThat(jwt.verify(new com.nimbusds.jose.crypto.RSASSAVerifier(publicKey))).isTrue();
        assertThat(jwt.getJWTClaimsSet().getStringClaim("role")).isEqualTo("CITIZEN");
    }

    @Test
    void rejectsDuplicateEmailWrongPasswordAndMissingToken() throws Exception {
        register("rosa@correo.pe");

        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"email": "ROSA@correo.pe", "password": "otraclave1", "fullName": "Rosa"}"""))
                .andExpect(status().isConflict());

        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                        {"email": "rosa@correo.pe", "password": "incorrecta"}"""))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void theAdministratorExistsAtStartupAndIsTheOnlyOneWhoCreatesOperators() throws Exception {
        String operatorToken = login("operador@muni.pe", "operador123");
        mvc.perform(get("/auth/me").header("Authorization", "Bearer " + operatorToken))
                .andExpect(jsonPath("$.role", is("OPERATOR")))
                .andExpect(jsonPath("$.administrator", is(true)))
                .andExpect(jsonPath("$.fullName", is("Operadora de prueba")));
        assertThat(SignedJWT.parse(operatorToken).getJWTClaimsSet().getBooleanClaim("admin")).isTrue();

        String newOperator = """
                {"email": "Carla@Muni.pe", "password": "operadora1", "fullName": "Carla Huamán",
                 "documentNumber": "40000001"}""";
        mvc.perform(post("/auth/operators").contentType(MediaType.APPLICATION_JSON).content(newOperator))
                .andExpect(status().isUnauthorized());

        register("pedro@correo.pe");
        String citizenToken = login("pedro@correo.pe", "secreta123");
        mvc.perform(post("/auth/operators").header("Authorization", "Bearer " + citizenToken)
                        .contentType(MediaType.APPLICATION_JSON).content(newOperator))
                .andExpect(status().isForbidden());

        mvc.perform(post("/auth/operators").header("Authorization", "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON).content(newOperator))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email", is("carla@muni.pe")))
                .andExpect(jsonPath("$.role", is("OPERATOR")))
                .andExpect(jsonPath("$.documentNumber", is("40000001")))
                .andExpect(jsonPath("$.administrator", is(false)));
        mvc.perform(post("/auth/operators").header("Authorization", "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON).content(newOperator))
                .andExpect(status().isConflict());

        SignedJWT jwt = SignedJWT.parse(login("carla@muni.pe", "operadora1"));
        assertThat(jwt.getJWTClaimsSet().getStringClaim("role")).isEqualTo("OPERATOR");
        assertThat(jwt.getJWTClaimsSet().getBooleanClaim("admin")).isFalse();
    }

    @Test
    void aRegularOperatorCannotCreateOrManageAccounts() throws Exception {
        String adminToken = login("operador@muni.pe", "operador123");
        createOperator(adminToken, "rosa@muni.pe", "Rosa Quispe", "40000002").andExpect(status().isCreated());
        String operatorToken = login("rosa@muni.pe", "temporal1");
        String citizenId = registerNamed("ana.ccori@correo.pe", "Ana Ccori");

        createOperator(operatorToken, "rosa.otra@muni.pe", "Rosa Quispe", "40000099").andExpect(status().isForbidden());
        mvc.perform(get("/auth/users").header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isForbidden());
        changeUserStatus(operatorToken, citizenId, false).andExpect(status().isForbidden());
        // Sí puede ver los datos de quien hizo un reporte.
        mvc.perform(get("/auth/users/" + citizenId).header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName", is("Ana Ccori")));
    }

    @Test
    void anAdministratorAppointsAndRemovesOtherAdministrators() throws Exception {
        String adminToken = login("operador@muni.pe", "operador123");
        String adminId = json.readTree(mvc.perform(get("/auth/me").header("Authorization", "Bearer " + adminToken))
                .andReturn().getResponse().getContentAsString()).get("id").asText();
        String marcoId = json.readTree(createOperator(adminToken, "marco@muni.pe", "Marco Salas", "40000006")
                .andReturn().getResponse().getContentAsString()).get("id").asText();
        String citizenId = registerNamed("vecina.admin@correo.pe", "Vecina");

        changeAdministrator(adminToken, marcoId, true).andExpect(status().isOk())
                .andExpect(jsonPath("$.administrator", is(true)));
        // Con el nuevo token, el segundo administrador ya puede gestionar cuentas, incluso quitarle el permiso al primero.
        String marcoToken = login("marco@muni.pe", "temporal1");
        assertThat(SignedJWT.parse(marcoToken).getJWTClaimsSet().getBooleanClaim("admin")).isTrue();
        mvc.perform(get("/auth/users").header("Authorization", "Bearer " + marcoToken)).andExpect(status().isOk());

        changeAdministrator(marcoToken, marcoId, false).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", is("No puedes quitarte tu propio permiso de administrador")));
        changeAdministrator(adminToken, citizenId, true).andExpect(status().isUnprocessableEntity());

        changeAdministrator(adminToken, marcoId, false).andExpect(status().isOk())
                .andExpect(jsonPath("$.administrator", is(false)));
        String demotedToken = login("marco@muni.pe", "temporal1");
        changeAdministrator(demotedToken, adminId, false).andExpect(status().isForbidden());
    }

    @Test
    void aPersonCanOnlyHaveOneOperatorAccount() throws Exception {
        String adminToken = login("operador@muni.pe", "operador123");
        createOperator(adminToken, "jose@muni.pe", "José Paredes", "40000003").andExpect(status().isCreated());

        createOperator(adminToken, "jose.paredes@muni.pe", "José Paredes", "40000003")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", is("Ya existe una cuenta de operador con el DNI 40000003")));
        mvc.perform(post("/auth/operators").header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"email": "sin.dni@muni.pe", "password": "temporal1", "fullName": "Sin DNI"}"""))
                .andExpect(status().isBadRequest());

        // El operador no puede cambiar su DNI desde su perfil.
        String operatorToken = login("jose@muni.pe", "temporal1");
        mvc.perform(put("/auth/me").header("Authorization", "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"fullName": "José Paredes", "documentNumber": "40000004"}"""))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(put("/auth/me").header("Authorization", "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"fullName": "José A. Paredes", "phone": "987654321", "documentNumber": "40000003"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName", is("José A. Paredes")));

        // Un ciudadano puede tener el mismo DNI que un operador: es la misma persona usando la app.
        register("jose.vecino@correo.pe");
        String citizenToken = login("jose.vecino@correo.pe", "secreta123");
        mvc.perform(put("/auth/me").header("Authorization", "Bearer " + citizenToken)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"fullName": "José Paredes", "documentNumber": "40000003"}"""))
                .andExpect(status().isOk());
    }

    @Test
    void operatorsNeedAnInstitutionalEmailAndMustReplaceTheTemporaryPassword() throws Exception {
        String operatorToken = login("operador@muni.pe", "operador123");
        mvc.perform(post("/auth/operators").header("Authorization", "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"email": "luis@gmail.com", "password": "temporal1", "fullName": "Luis Rojas",
                                 "documentNumber": "40000005"}"""))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail", is("Las cuentas de operador deben usar un correo institucional (@muni.pe)")));

        mvc.perform(post("/auth/operators").header("Authorization", "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"email": "luis@muni.pe", "password": "temporal1", "fullName": "Luis Rojas",
                                 "documentNumber": "40000005"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.passwordChangeRequired", is(true)));

        String firstLogin = loginBody("luis@muni.pe", "temporal1");
        assertThat(json.readTree(firstLogin).get("passwordChangeRequired").asBoolean()).isTrue();
        String token = json.readTree(firstLogin).get("accessToken").asText();
        mvc.perform(put("/auth/me/password").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"currentPassword": "temporal1", "newPassword": "mi-clave-propia"}"""))
                .andExpect(status().isNoContent());

        assertThat(json.readTree(loginBody("luis@muni.pe", "mi-clave-propia")).get("passwordChangeRequired").asBoolean())
                .isFalse();
        assertThat(json.readTree(loginBody("operador@muni.pe", "operador123")).get("passwordChangeRequired").asBoolean())
                .isFalse();
    }

    @Test
    void citizenEditsTheProfileAndChangesThePassword() throws Exception {
        register("maria@correo.pe");
        String token = login("maria@correo.pe", "secreta123");

        mvc.perform(put("/auth/me").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName": " María Torres ", "phone": "987654321", "documentNumber": "45678912"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName", is("María Torres")))
                .andExpect(jsonPath("$.phone", is("987654321")))
                .andExpect(jsonPath("$.documentNumber", is("45678912")))
                .andExpect(jsonPath("$.email", is("maria@correo.pe")))
                .andExpect(jsonPath("$.role", is("CITIZEN")));
        mvc.perform(get("/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.phone", is("987654321")));

        // Los campos opcionales se pueden vaciar; los formatos inválidos se rechazan.
        mvc.perform(put("/auth/me").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName": "María Torres", "phone": "", "documentNumber": null}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").doesNotExist());
        mvc.perform(put("/auth/me").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName": "María", "phone": "12345", "documentNumber": "abc"}"""))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/auth/me").contentType(MediaType.APPLICATION_JSON).content("{\"fullName\": \"X\"}"))
                .andExpect(status().isUnauthorized());

        mvc.perform(put("/auth/me/password").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword": "incorrecta", "newPassword": "nuevaclave1"}"""))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(put("/auth/me/password").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword": "secreta123", "newPassword": "nuevaclave1"}"""))
                .andExpect(status().isNoContent());
        login("maria@correo.pe", "nuevaclave1");
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                        {"email": "maria@correo.pe", "password": "secreta123"}"""))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void operatorListsFiltersAndReadsUsers() throws Exception {
        String operatorToken = login("operador@muni.pe", "operador123");
        String first = registerNamed("elsa.zevallos@correo.pe", "Elsa Zevallos");
        String second = registerNamed("zevallos.jorge@correo.pe", "Jorge Ramos");
        login("elsa.zevallos@correo.pe", "secreta123");

        // Busca en nombre o correo sin distinguir mayúsculas; las cuentas más recientes van primero.
        mvc.perform(get("/auth/users").param("q", "ZEVALLOS").header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems", is(2)))
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.size", is(20)))
                .andExpect(jsonPath("$.items[*].id", contains(second, first)))
                .andExpect(jsonPath("$.items[0].role", is("CITIZEN")))
                .andExpect(jsonPath("$.items[0].enabled", is(true)))
                .andExpect(jsonPath("$.items[0].createdAt", notNullValue()))
                .andExpect(jsonPath("$.items[0].lastLoginAt", nullValue()))
                .andExpect(jsonPath("$.items[1].lastLoginAt", notNullValue()));
        mvc.perform(get("/auth/users").param("q", "jorge").param("role", "CITIZEN")
                        .header("Authorization", "Bearer " + operatorToken))
                .andExpect(jsonPath("$.items[*].id", contains(second)));
        mvc.perform(get("/auth/users").param("q", "%").header("Authorization", "Bearer " + operatorToken))
                .andExpect(jsonPath("$.totalItems", is(0)));
        mvc.perform(get("/auth/users").param("role", "OPERATOR").param("size", "500")
                        .header("Authorization", "Bearer " + operatorToken))
                .andExpect(jsonPath("$.size", is(100)))
                .andExpect(jsonPath("$.items[*].role", everyItem(is("OPERATOR"))));

        mvc.perform(get("/auth/users/" + first).header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("elsa.zevallos@correo.pe")))
                .andExpect(jsonPath("$.fullName", is("Elsa Zevallos")));
        mvc.perform(get("/auth/users/" + UUID.randomUUID()).header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isNotFound());

        String citizenToken = login("elsa.zevallos@correo.pe", "secreta123");
        mvc.perform(get("/auth/users").header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isForbidden());
        mvc.perform(get("/auth/users/" + first).header("Authorization", "Bearer " + citizenToken))
                .andExpect(status().isForbidden());
        mvc.perform(get("/auth/users")).andExpect(status().isUnauthorized());
    }

    @Test
    void operatorDisablesAndEnablesAccounts() throws Exception {
        String operatorToken = login("operador@muni.pe", "operador123");
        String id = registerNamed("tomas@correo.pe", "Tomás Vega");
        String citizenToken = login("tomas@correo.pe", "secreta123");

        changeUserStatus(operatorToken, id, false).andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(id)))
                .andExpect(jsonPath("$.enabled", is(false)));
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                        {"email": "tomas@correo.pe", "password": "secreta123"}"""))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail", is("Tu cuenta está desactivada. Contacta a la municipalidad.")));
        // Con una contraseña incorrecta no se revela que la cuenta está desactivada.
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                        {"email": "tomas@correo.pe", "password": "incorrecta"}"""))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/auth/users").param("q", "tomas@correo.pe").param("enabled", "false")
                        .header("Authorization", "Bearer " + operatorToken))
                .andExpect(jsonPath("$.items[*].id", contains(id)));
        mvc.perform(get("/auth/users").param("q", "tomas@correo.pe").param("enabled", "true")
                        .header("Authorization", "Bearer " + operatorToken))
                .andExpect(jsonPath("$.totalItems", is(0)));

        changeUserStatus(operatorToken, id, true).andExpect(status().isOk()).andExpect(jsonPath("$.enabled", is(true)));
        login("tomas@correo.pe", "secreta123");

        changeUserStatus(citizenToken, id, false).andExpect(status().isForbidden());
        changeUserStatus(operatorToken, UUID.randomUUID().toString(), false).andExpect(status().isNotFound());
        mvc.perform(patch("/auth/users/" + id + "/status").header("Authorization", "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anOperatorCannotDisableTheirOwnAccount() throws Exception {
        String operatorToken = login("operador@muni.pe", "operador123");
        String operatorId = json.readTree(mvc.perform(get("/auth/me").header("Authorization", "Bearer " + operatorToken))
                .andReturn().getResponse().getContentAsString()).get("id").asText();

        changeUserStatus(operatorToken, operatorId, false).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", is("No puedes desactivar tu propia cuenta")));
        changeUserStatus(operatorToken, operatorId, true).andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled", is(true)));
    }

    @Test
    void rejectsInvalidRegistrationData() throws Exception {
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"email": "no-es-correo", "password": "corta", "fullName": ""}"""))
                .andExpect(status().isBadRequest());
    }

    private void register(String email) throws Exception {
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"email": "%s", "password": "secreta123", "fullName": "Usuario Prueba"}""".formatted(email)))
                .andExpect(status().isCreated());
    }

    private String registerNamed(String email, String fullName) throws Exception {
        String body = mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"email": "%s", "password": "secreta123", "fullName": "%s"}""".formatted(email, fullName)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.enabled", is(true)))
                .andExpect(jsonPath("$.lastLoginAt", nullValue()))
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("id").asText();
    }

    private ResultActions createOperator(String token, String email, String fullName, String dni) throws Exception {
        return mvc.perform(post("/auth/operators").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"email": "%s", "password": "temporal1", "fullName": "%s", "documentNumber": "%s"}"""
                        .formatted(email, fullName, dni)));
    }

    private ResultActions changeAdministrator(String token, String id, boolean administrator) throws Exception {
        return mvc.perform(patch("/auth/users/" + id + "/administrator").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"administrator\": %s}".formatted(administrator)));
    }

    private ResultActions changeUserStatus(String token, String id, boolean enabled) throws Exception {
        return mvc.perform(patch("/auth/users/" + id + "/status").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"enabled\": %s}".formatted(enabled)));
    }

    private String login(String email, String password) throws Exception {
        JsonNode node = json.readTree(loginBody(email, password));
        return node.get("accessToken").asText();
    }

    private String loginBody(String email, String password) throws Exception {
        return mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                        {"email": "%s", "password": "%s"}""".formatted(email, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType", is("Bearer")))
                .andReturn().getResponse().getContentAsString();
    }
}
