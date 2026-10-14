package pe.viadeep.auth.infrastructure.security;

import java.time.Clock;
import java.time.Instant;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;
import pe.viadeep.auth.application.TokenIssuer;
import pe.viadeep.auth.domain.User;

/**
 * Emite tokens JWT firmados con RS256 (OB2): sub = id del usuario, role = rol en la plataforma, admin = true si es el
 * administrador que gestiona las cuentas.
 */
@Component
class JwtTokenIssuer implements TokenIssuer {

    private final JwtEncoder encoder;
    private final JwtProperties properties;
    private final Clock clock;

    JwtTokenIssuer(JwtEncoder encoder, JwtProperties properties, Clock clock) {
        this.encoder = encoder;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public IssuedToken issue(User user) {
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .subject(user.id().toString())
                .issuedAt(now)
                .expiresAt(now.plus(properties.ttl()))
                .claim("email", user.email())
                .claim("name", user.fullName())
                .claim("role", user.role().name())
                .claim("admin", user.administrator())
                .build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).keyId(properties.keyId()).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedToken(token, properties.ttl().toSeconds());
    }
}
