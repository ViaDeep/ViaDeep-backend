package pe.viadeep.incident.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

class JwtValidationTest {

    private static final String EXPECTED_ISSUER = "viadeep-auth";

    private KeyPair trustedKey;
    private JwtDecoder decoder;

    @BeforeEach
    void setUp() throws Exception {
        trustedKey = keyPair();
        NimbusJwtDecoder configuredDecoder = NimbusJwtDecoder
                .withPublicKey((RSAPublicKey) trustedKey.getPublic())
                .build();
        configuredDecoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(EXPECTED_ISSUER));
        decoder = configuredDecoder;
    }

    @Test
    void acceptsAValidRs256TokenFromTheExpectedIssuer() {
        String token = token(trustedKey, EXPECTED_ISSUER, Instant.now().plusSeconds(300));

        var decoded = decoder.decode(token);
        assertThat(decoded.getClaimAsString("iss")).isEqualTo(EXPECTED_ISSUER);
        assertThat(decoded.getHeaders().get("alg")).isEqualTo("RS256");
    }

    @Test
    void rejectsATokenFromAnotherIssuer() {
        String token = token(trustedKey, "another-issuer", Instant.now().plusSeconds(300));

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtValidationException.class);
    }

    @Test
    void rejectsATokenSignedWithAnotherKey() throws Exception {
        String token = token(keyPair(), EXPECTED_ISSUER, Instant.now().plusSeconds(300));

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsAnExpiredToken() {
        String token = token(trustedKey, EXPECTED_ISSUER, Instant.now().minusSeconds(300));

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtValidationException.class);
    }

    private static String token(KeyPair signingKey, String issuer, Instant expiresAt) {
        try {
            String keyId = "test-key";
            RSAKey rsaKey = new RSAKey.Builder((RSAPublicKey) signingKey.getPublic())
                    .privateKey(signingKey.getPrivate())
                    .keyID(keyId)
                    .build();
            JwtEncoder encoder = new NimbusJwtEncoder(
                    new ImmutableJWKSet<SecurityContext>(new JWKSet(rsaKey)));
            Instant issuedAt = expiresAt.isAfter(Instant.now())
                    ? Instant.now().minusSeconds(10)
                    : expiresAt.minusSeconds(300);
            JwtClaimsSet claims = JwtClaimsSet.builder()
                    .issuer(issuer)
                    .subject("test-user")
                    .issuedAt(issuedAt)
                    .expiresAt(expiresAt)
                    .claim("role", "CITIZEN")
                    .build();
            JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).keyId(keyId).build();
            return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        } catch (Exception exception) {
            throw new AssertionError("No se pudo crear el JWT de prueba", exception);
        }
    }

    private static KeyPair keyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }
}
