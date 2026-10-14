package pe.viadeep.auth.infrastructure.bootstrap;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Cuenta del primer operador municipal. Si el correo o la contraseña están vacíos no se crea ninguna. */
@ConfigurationProperties(prefix = "viadeep.initial-operator")
public record InitialOperatorProperties(String email, String password, String fullName) {

    boolean configured() {
        return email != null && !email.isBlank() && password != null && !password.isBlank();
    }
}
