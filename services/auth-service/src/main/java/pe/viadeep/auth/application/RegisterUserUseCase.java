package pe.viadeep.auth.application;

import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.viadeep.auth.domain.EmailAlreadyRegisteredException;
import pe.viadeep.auth.domain.Role;
import pe.viadeep.auth.domain.User;
import pe.viadeep.auth.domain.UserRepository;

/**
 * Alta de usuarios. El registro público siempre crea ciudadanos; los operadores municipales solo los crea otro
 * operador o la cuenta inicial que se configura al desplegar, y siempre con un correo del dominio institucional.
 */
@Service
@EnableConfigurationProperties(OperatorEmailPolicy.class)
public class RegisterUserUseCase {

    private final UserRepository users;
    private final PasswordHasher passwordHasher;
    private final OperatorEmailPolicy operatorEmails;
    private final Clock clock;

    public RegisterUserUseCase(UserRepository users, PasswordHasher passwordHasher, OperatorEmailPolicy operatorEmails,
                               Clock clock) {
        this.users = users;
        this.passwordHasher = passwordHasher;
        this.operatorEmails = operatorEmails;
        this.clock = clock;
    }

    @Transactional
    public User register(String email, String rawPassword, String fullName) {
        return create(email, rawPassword, fullName, Role.CITIZEN);
    }

    /** Operador inicial: su contraseña viene de la configuración del despliegue, así que no es temporal. */
    @Transactional
    public User registerOperator(String email, String rawPassword, String fullName) {
        operatorEmails.check(User.normalizeEmail(email));
        return create(email, rawPassword, fullName, Role.OPERATOR);
    }

    /** Un operador da de alta a otro con una contraseña temporal que el nuevo debe cambiar al primer ingreso. */
    @Transactional
    public User inviteOperator(String email, String temporaryPassword, String fullName) {
        String normalizedEmail = User.normalizeEmail(email);
        operatorEmails.check(normalizedEmail);
        ensureAvailable(normalizedEmail);
        return users.save(User.inviteOperator(normalizedEmail, passwordHasher.hash(temporaryPassword), fullName,
                clock.instant()));
    }

    private User create(String email, String rawPassword, String fullName, Role role) {
        String normalizedEmail = User.normalizeEmail(email);
        ensureAvailable(normalizedEmail);
        User user = User.register(normalizedEmail, passwordHasher.hash(rawPassword), fullName, role, clock.instant());
        return users.save(user);
    }

    private void ensureAvailable(String normalizedEmail) {
        if (users.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyRegisteredException(normalizedEmail);
        }
    }
}
