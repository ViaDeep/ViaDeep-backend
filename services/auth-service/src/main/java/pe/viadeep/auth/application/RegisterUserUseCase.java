package pe.viadeep.auth.application;

import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.viadeep.auth.domain.DocumentNumberAlreadyRegisteredException;
import pe.viadeep.auth.domain.EmailAlreadyRegisteredException;
import pe.viadeep.auth.domain.Role;
import pe.viadeep.auth.domain.User;
import pe.viadeep.auth.domain.UserRepository;

/**
 * Alta de usuarios. El registro público siempre crea ciudadanos. Los operadores municipales solo los crea el
 * administrador (la cuenta inicial que se configura al desplegar), con un correo del dominio institucional y el DNI
 * de la persona, que no puede tener dos cuentas de operador.
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

    /**
     * Administrador inicial: lo designa la municipalidad al desplegar y su contraseña viene de esa configuración, así
     * que no es temporal.
     */
    @Transactional
    public User registerAdministrator(String email, String rawPassword, String fullName) {
        String normalizedEmail = User.normalizeEmail(email);
        operatorEmails.check(normalizedEmail);
        ensureAvailable(normalizedEmail);
        User admin = User.register(normalizedEmail, passwordHasher.hash(rawPassword), fullName, Role.OPERATOR,
                clock.instant()).asAdministrator();
        return users.save(admin);
    }

    /** El administrador da de alta a un operador con una contraseña temporal que debe cambiar al primer ingreso. */
    @Transactional
    public User inviteOperator(String email, String temporaryPassword, String fullName, String documentNumber) {
        String normalizedEmail = User.normalizeEmail(email);
        operatorEmails.check(normalizedEmail);
        ensureAvailable(normalizedEmail);
        String dni = documentNumber.strip();
        if (users.operatorDocumentTaken(dni, null)) {
            throw new DocumentNumberAlreadyRegisteredException(dni);
        }
        return users.save(User.inviteOperator(normalizedEmail, passwordHasher.hash(temporaryPassword), fullName, dni,
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
