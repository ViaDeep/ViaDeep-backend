package pe.viadeep.auth.application;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.Base64;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.viadeep.auth.domain.Role;
import pe.viadeep.auth.domain.User;
import pe.viadeep.auth.domain.UserRepository;

/**
 * Inicio de sesión de ciudadanos con su cuenta de Google o de Microsoft (Outlook/Hotmail). El proveedor ya comprobó
 * que el correo existe y es de la persona, así que no se envía código. Si el correo aún no tiene cuenta, se crea una
 * de ciudadano; si ya la tiene, se entra a esa misma cuenta.
 */
@Service
public class SocialLoginUseCase {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final ExternalIdentityVerifier verifier;
    private final UserRepository users;
    private final PasswordHasher passwordHasher;
    private final TokenIssuer tokenIssuer;
    private final Clock clock;

    public SocialLoginUseCase(ExternalIdentityVerifier verifier, UserRepository users, PasswordHasher passwordHasher,
                              TokenIssuer tokenIssuer, Clock clock) {
        this.verifier = verifier;
        this.users = users;
        this.passwordHasher = passwordHasher;
        this.tokenIssuer = tokenIssuer;
        this.clock = clock;
    }

    @Transactional
    public LoginUseCase.Session login(IdentityProvider provider, String idToken) {
        ExternalIdentity identity = verifier.verify(provider, idToken);
        String email = User.normalizeEmail(identity.email());
        User user = users.findByEmail(email)
                .map(existing -> {
                    if (existing.role() != Role.CITIZEN) {
                        throw new SocialLoginNotAllowedException();
                    }
                    // Si se registró con contraseña y no confirmó el código, el proveedor ya confirmó el correo.
                    return existing.emailVerified() ? existing : existing.withEmailVerified();
                })
                .orElseGet(() -> newCitizen(email, identity.fullName()));
        if (!user.enabled()) {
            throw new AccountDisabledException();
        }
        User saved = users.save(user.withLastLoginAt(clock.instant()));
        return new LoginUseCase.Session(tokenIssuer.issue(saved), saved.passwordChangeRequired());
    }

    /**
     * La cuenta nueva no tiene una contraseña que la persona conozca: se guarda una aleatoria. Si luego quiere entrar
     * con correo y contraseña, la elige con "¿Olvidaste tu contraseña?".
     */
    private User newCitizen(String email, String fullName) {
        byte[] secret = new byte[32];
        RANDOM.nextBytes(secret);
        String name = fullName == null || fullName.isBlank() ? email.substring(0, email.indexOf('@')) : fullName;
        String hash = passwordHasher.hash(Base64.getUrlEncoder().withoutPadding().encodeToString(secret));
        return User.signUp(email, hash, name.length() > 120 ? name.substring(0, 120) : name, clock.instant())
                .withEmailVerified();
    }
}
