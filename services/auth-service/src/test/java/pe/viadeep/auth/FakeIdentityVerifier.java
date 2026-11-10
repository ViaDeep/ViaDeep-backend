package pe.viadeep.auth;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import pe.viadeep.auth.application.ExternalIdentity;
import pe.viadeep.auth.application.ExternalIdentityVerifier;
import pe.viadeep.auth.application.IdentityProvider;
import pe.viadeep.auth.application.InvalidExternalTokenException;

/**
 * En las pruebas no se llama a Google ni a Microsoft: el "token" es {@code PROVEEDOR|correo|nombre} y solo es válido
 * para el proveedor que nombra.
 */
@Component
@Primary
class FakeIdentityVerifier implements ExternalIdentityVerifier {

    static String token(IdentityProvider provider, String email, String name) {
        return provider + "|" + email + "|" + name;
    }

    @Override
    public ExternalIdentity verify(IdentityProvider provider, String idToken) {
        String[] parts = idToken.split("\\|", -1);
        if (parts.length != 3 || !parts[0].equals(provider.name())) {
            throw new InvalidExternalTokenException("No pudimos comprobar tu cuenta. Intenta de nuevo.");
        }
        return new ExternalIdentity(parts[1], parts[2]);
    }
}
