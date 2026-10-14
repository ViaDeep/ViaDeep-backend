package pe.viadeep.auth.application;

import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.viadeep.auth.domain.User;
import pe.viadeep.auth.domain.UserRepository;

@Service
public class LoginUseCase {

    private final UserRepository users;
    private final PasswordHasher passwordHasher;
    private final TokenIssuer tokenIssuer;
    private final Clock clock;

    public LoginUseCase(UserRepository users, PasswordHasher passwordHasher, TokenIssuer tokenIssuer, Clock clock) {
        this.users = users;
        this.passwordHasher = passwordHasher;
        this.tokenIssuer = tokenIssuer;
        this.clock = clock;
    }

    /**
     * Verifica la contraseña antes de revisar si la cuenta está activa, para no revelar el estado de una cuenta a
     * quien no conoce su contraseña. Registra la fecha del inicio de sesión.
     */
    @Transactional
    public TokenIssuer.IssuedToken login(String email, String rawPassword) {
        User user = users.findByEmail(User.normalizeEmail(email))
                .filter(found -> passwordHasher.matches(rawPassword, found.passwordHash()))
                .orElseThrow(InvalidCredentialsException::new);
        if (!user.enabled()) {
            throw new AccountDisabledException();
        }
        return tokenIssuer.issue(users.save(user.withLastLoginAt(clock.instant())));
    }
}
