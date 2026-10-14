package pe.viadeep.incident.presentation;

import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;
import pe.viadeep.incident.application.Requester;

final class Jwts {

    private Jwts() {
    }

    static UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    static Requester requester(Jwt jwt) {
        return new Requester(userId(jwt), "OPERATOR".equals(jwt.getClaimAsString("role")));
    }
}
