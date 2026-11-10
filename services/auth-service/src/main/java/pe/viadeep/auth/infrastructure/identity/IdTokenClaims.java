package pe.viadeep.auth.infrastructure.identity;

import java.util.List;
import java.util.Set;
import org.springframework.security.oauth2.jwt.Jwt;
import pe.viadeep.auth.application.ExternalIdentity;
import pe.viadeep.auth.application.InvalidExternalTokenException;

/**
 * Reglas sobre el contenido de un token de identidad ya firmado y vigente. Están separadas de la descarga de claves
 * para poder probarlas sin red.
 */
final class IdTokenClaims {

    static final Set<String> GOOGLE_ISSUERS = Set.of("https://accounts.google.com", "accounts.google.com");

    /** Inquilino de Microsoft de las cuentas personales (Outlook, Hotmail, Live): su correo siempre es verificado. */
    static final String MICROSOFT_CONSUMERS_TENANT = "9188040d-6c67-4c5b-b112-36a304b66dad";
    static final String MICROSOFT_CONSUMERS_ISSUER =
            "https://login.microsoftonline.com/" + MICROSOFT_CONSUMERS_TENANT + "/v2.0";

    private IdTokenClaims() {
    }

    static ExternalIdentity google(Jwt jwt, List<String> clientIds) {
        if (!GOOGLE_ISSUERS.contains(issuer(jwt))) {
            throw invalid();
        }
        requireAudience(jwt, clientIds);
        if (!isTrue(jwt.getClaim("email_verified"))) {
            throw new InvalidExternalTokenException("Tu cuenta de Google no tiene un correo verificado");
        }
        return new ExternalIdentity(requireEmail(jwt.getClaimAsString("email")), jwt.getClaimAsString("name"));
    }

    /**
     * Solo cuentas personales de Microsoft: en las de empresas o universidades el administrador puede poner cualquier
     * correo sin verificarlo, y alguien podría hacerse pasar por otra persona.
     */
    static ExternalIdentity microsoft(Jwt jwt, String clientId) {
        if (!MICROSOFT_CONSUMERS_ISSUER.equals(issuer(jwt))
                || !MICROSOFT_CONSUMERS_TENANT.equals(jwt.getClaimAsString("tid"))) {
            throw new InvalidExternalTokenException("Usa una cuenta personal de Microsoft (Outlook, Hotmail o Live)");
        }
        requireAudience(jwt, List.of(clientId));
        String email = jwt.getClaimAsString("email");
        if (email == null || email.isBlank()) {
            email = jwt.getClaimAsString("preferred_username");
        }
        return new ExternalIdentity(requireEmail(email), jwt.getClaimAsString("name"));
    }

    /** Como texto: Google a veces emite "accounts.google.com", que no es una URL. */
    private static String issuer(Jwt jwt) {
        return jwt.getClaimAsString("iss");
    }

    private static void requireAudience(Jwt jwt, List<String> accepted) {
        List<String> audience = jwt.getAudience();
        if (audience == null || audience.stream().noneMatch(accepted::contains)) {
            throw invalid();
        }
    }

    private static boolean isTrue(Object claim) {
        return Boolean.TRUE.equals(claim) || "true".equals(claim);
    }

    private static String requireEmail(String email) {
        if (email == null || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new InvalidExternalTokenException("La cuenta no compartió un correo válido");
        }
        return email;
    }

    private static InvalidExternalTokenException invalid() {
        return new InvalidExternalTokenException("No pudimos comprobar tu cuenta. Intenta de nuevo.");
    }
}
