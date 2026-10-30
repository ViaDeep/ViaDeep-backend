package pe.viadeep.auth.infrastructure.bootstrap;

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import pe.viadeep.auth.application.RegisterUserUseCase;
import pe.viadeep.auth.domain.Role;
import pe.viadeep.auth.domain.User;
import pe.viadeep.auth.domain.UserRepository;

/**
 * Crea el administrador al iniciar el servicio: el encargado que designa la municipalidad, que entra a la plataforma
 * web y da de alta al resto de operadores. Si el correo ya existe como operador, solo se asegura de que sea el
 * administrador (las bases creadas antes de existir ese rol lo tenían como operador común).
 */
@Component
@EnableConfigurationProperties(InitialOperatorProperties.class)
class InitialOperatorSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(InitialOperatorSeeder.class);
    private static final int MIN_PASSWORD_LENGTH = 8;

    private final InitialOperatorProperties properties;
    private final UserRepository users;
    private final RegisterUserUseCase registerUser;

    InitialOperatorSeeder(InitialOperatorProperties properties, UserRepository users, RegisterUserUseCase registerUser) {
        this.properties = properties;
        this.users = users;
        this.registerUser = registerUser;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.configured()) {
            return;
        }
        Optional<User> existing = users.findByEmail(User.normalizeEmail(properties.email()));
        if (existing.isPresent()) {
            User user = existing.get();
            if (user.role() == Role.OPERATOR && !user.administrator()) {
                users.save(user.asAdministrator());
                log.info("El operador {} ahora es el administrador", user.email());
            } else if (user.role() != Role.OPERATOR) {
                log.warn("OPERATOR_EMAIL {} pertenece a una cuenta de ciudadano; no se convierte en administrador",
                        user.email());
            } else {
                log.info("El administrador {} ya existe", user.email());
            }
            return;
        }
        if (properties.password().length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalStateException("OPERATOR_PASSWORD debe tener al menos " + MIN_PASSWORD_LENGTH + " caracteres");
        }
        String fullName = properties.fullName() == null || properties.fullName().isBlank()
                ? "Operador municipal" : properties.fullName();
        User admin = registerUser.registerAdministrator(properties.email(), properties.password(), fullName);
        log.info("Administrador creado: {}", admin.email());
    }
}
