package pe.viadeep.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

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
                .andExpect(jsonPath("$.fullName", is("Ana Quispe")));
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

    private String login(String email, String password) throws Exception {
        String body = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                        {"email": "%s", "password": "%s"}""".formatted(email, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType", is("Bearer")))
                .andReturn().getResponse().getContentAsString();
        JsonNode node = json.readTree(body);
        return node.get("accessToken").asText();
    }
}
