package pe.viadeep.auth.infrastructure.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import pe.viadeep.auth.application.RegisterUserUseCase;
import pe.viadeep.auth.domain.User;
import pe.viadeep.auth.domain.UserRepository;

/**
 * Crea el primer operador al iniciar el servicio, para que alguien pueda entrar a la plataforma web y dar de alta
 * al resto. Si el correo ya existe no lo modifica.
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
        if (users.existsByEmail(User.normalizeEmail(properties.email()))) {
            log.info("El operador inicial {} ya existe", properties.email());
            return;
        }
        if (properties.password().length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalStateException("OPERATOR_PASSWORD debe tener al menos " + MIN_PASSWORD_LENGTH + " caracteres");
        }
        String fullName = properties.fullName() == null || properties.fullName().isBlank()
                ? "Operador municipal" : properties.fullName();
        User operator = registerUser.registerOperator(properties.email(), properties.password(), fullName);
        log.info("Operador inicial creado: {}", operator.email());
    }
}
