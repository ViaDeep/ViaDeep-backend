package pe.viadeep.auth.application;

import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.viadeep.auth.domain.EmailAlreadyRegisteredException;
import pe.viadeep.auth.domain.Role;
import pe.viadeep.auth.domain.User;
import pe.viadeep.auth.domain.UserRepository;

/** Registro público: siempre crea ciudadanos. Los operadores municipales se dan de alta por otra vía. */
@Service
public class RegisterUserUseCase {

    private final UserRepository users;
    private final PasswordHasher passwordHasher;
    private final Clock clock;

    public RegisterUserUseCase(UserRepository users, PasswordHasher passwordHasher, Clock clock) {
        this.users = users;
        this.passwordHasher = passwordHasher;
        this.clock = clock;
    }

    @Transactional
    public User register(String email, String rawPassword, String fullName) {
        String normalizedEmail = User.normalizeEmail(email);
        if (users.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyRegisteredException(normalizedEmail);
        }
        User user = User.register(normalizedEmail, passwordHasher.hash(rawPassword), fullName, Role.CITIZEN, clock.instant());
        return users.save(user);
    }
}
