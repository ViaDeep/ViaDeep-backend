package pe.viadeep.auth.infrastructure.identity;

import java.util.EnumMap;
import java.util.Map;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;
import pe.viadeep.auth.application.ExternalIdentity;
import pe.viadeep.auth.application.ExternalIdentityVerifier;
import pe.viadeep.auth.application.IdentityProvider;
import pe.viadeep.auth.application.InvalidExternalTokenException;
import pe.viadeep.auth.application.SocialLoginUnavailableException;

/**
 * Verifica los tokens de identidad (OpenID Connect) con las claves públicas que publica cada proveedor. Las claves se
 * descargan la primera vez que se usan y quedan en caché; no se crean beans JwtDecoder para no reemplazar el que
 * valida los tokens propios de ViaDeep.
 */
@Component
@EnableConfigurationProperties(SocialLoginProperties.class)
class OidcIdentityVerifier implements ExternalIdentityVerifier {

    private static final Map<IdentityProvider, String> JWKS = Map.of(
            IdentityProvider.GOOGLE, "https://www.googleapis.com/oauth2/v3/certs",
            IdentityProvider.MICROSOFT, "https://login.microsoftonline.com/consumers/discovery/v2.0/keys");

    private final SocialLoginProperties properties;
    private final Map<IdentityProvider, JwtDecoder> decoders = new EnumMap<>(IdentityProvider.class);

    OidcIdentityVerifier(SocialLoginProperties properties) {
        this.properties = properties;
    }

    @Override
    public ExternalIdentity verify(IdentityProvider provider, String idToken) {
        boolean configured = provider == IdentityProvider.GOOGLE
                ? !properties.googleClientIds().isEmpty()
                : properties.microsoftClientId() != null;
        if (!configured) {
            throw new SocialLoginUnavailableException(provider);
        }
        Jwt jwt;
        try {
            jwt = decoder(provider).decode(idToken);
        } catch (JwtException e) {
            throw new InvalidExternalTokenException("No pudimos comprobar tu cuenta. Intenta de nuevo.");
        }
        return provider == IdentityProvider.GOOGLE
                ? IdTokenClaims.google(jwt, properties.googleClientIds())
                : IdTokenClaims.microsoft(jwt, properties.microsoftClientId());
    }

    private synchronized JwtDecoder decoder(IdentityProvider provider) {
        return decoders.computeIfAbsent(provider, key -> {
            NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(JWKS.get(key)).build();
            decoder.setJwtValidator(JwtValidators.createDefault()); // firma y vigencia; el resto en IdTokenClaims
            return decoder;
        });
    }
}
