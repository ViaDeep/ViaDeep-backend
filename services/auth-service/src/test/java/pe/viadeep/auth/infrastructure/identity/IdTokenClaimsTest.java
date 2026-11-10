package pe.viadeep.auth.infrastructure.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import pe.viadeep.auth.application.InvalidExternalTokenException;

class IdTokenClaimsTest {

    private static final String GOOGLE_CLIENT = "123-web.apps.googleusercontent.com";
    private static final String MS_CLIENT = "00000000-1111-2222-3333-444444444444";

    @Test
    void acceptsAGoogleTokenForViaDeepWithAVerifiedEmail() {
        var identity = IdTokenClaims.google(jwt(Map.of("iss", "https://accounts.google.com", "aud", GOOGLE_CLIENT,
                "email", "ana@gmail.com", "email_verified", true, "name", "Ana Quispe")), List.of(GOOGLE_CLIENT));
        assertThat(identity.email()).isEqualTo("ana@gmail.com");
        assertThat(identity.fullName()).isEqualTo("Ana Quispe");
    }

    @Test
    void rejectsGoogleTokensForAnotherAppAnotherIssuerOrAnUnverifiedEmail() {
        assertInvalid(() -> IdTokenClaims.google(jwt(Map.of("iss", "https://accounts.google.com", "aud", "otra-app",
                "email", "ana@gmail.com", "email_verified", true)), List.of(GOOGLE_CLIENT)));
        assertInvalid(() -> IdTokenClaims.google(jwt(Map.of("iss", "https://evil.example", "aud", GOOGLE_CLIENT,
                "email", "ana@gmail.com", "email_verified", true)), List.of(GOOGLE_CLIENT)));
        assertInvalid(() -> IdTokenClaims.google(jwt(Map.of("iss", "accounts.google.com", "aud", GOOGLE_CLIENT,
                "email", "ana@gmail.com", "email_verified", false)), List.of(GOOGLE_CLIENT)));
    }

    @Test
    void acceptsOnlyPersonalMicrosoftAccounts() {
        var identity = IdTokenClaims.microsoft(jwt(Map.of("iss", IdTokenClaims.MICROSOFT_CONSUMERS_ISSUER,
                "tid", IdTokenClaims.MICROSOFT_CONSUMERS_TENANT, "aud", MS_CLIENT,
                "preferred_username", "luis@outlook.com", "name", "Luis Rojas")), MS_CLIENT);
        assertThat(identity.email()).isEqualTo("luis@outlook.com");

        String companyTenant = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";
        assertInvalid(() -> IdTokenClaims.microsoft(jwt(Map.of(
                "iss", "https://login.microsoftonline.com/" + companyTenant + "/v2.0", "tid", companyTenant,
                "aud", MS_CLIENT, "email", "ceo@empresa.pe")), MS_CLIENT));
        assertInvalid(() -> IdTokenClaims.microsoft(jwt(Map.of("iss", IdTokenClaims.MICROSOFT_CONSUMERS_ISSUER,
                "tid", IdTokenClaims.MICROSOFT_CONSUMERS_TENANT, "aud", "otra-app", "email", "luis@outlook.com")),
                MS_CLIENT));
    }

    private static void assertInvalid(org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
        assertThatThrownBy(call).isInstanceOf(InvalidExternalTokenException.class);
    }

    private static Jwt jwt(Map<String, Object> claims) {
        Jwt.Builder builder = Jwt.withTokenValue("t").header("alg", "RS256")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(300));
        claims.forEach((name, value) -> builder.claim(name, name.equals("aud") ? List.of(value) : value));
        return builder.build();
    }
}
