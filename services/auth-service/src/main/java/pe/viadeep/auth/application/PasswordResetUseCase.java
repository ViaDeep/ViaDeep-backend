package pe.viadeep.auth.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.viadeep.auth.domain.EmailCodePurpose;
import pe.viadeep.auth.domain.User;
import pe.viadeep.auth.domain.UserRepository;

/** "¿Olvidaste tu contraseña?": un código al correo permite elegir una contraseña nueva sin conocer la anterior. */
@Service
public class PasswordResetUseCase {

    private final UserRepository users;
    private final EmailCodeService codes;
    private final PasswordHasher passwordHasher;

    public PasswordResetUseCase(UserRepository users, EmailCodeService codes, PasswordHasher passwordHasher) {
        this.users = users;
        this.codes = codes;
        this.passwordHasher = passwordHasher;
    }

    /** Envía el código solo si la cuenta existe y está activa, pero responde igual en todos los casos. */
    @Transactional
    public void requestReset(String email) {
        String normalized = User.normalizeEmail(email);
        users.findByEmail(normalized)
                .filter(User::enabled)
                .ifPresent(user -> codes.send(normalized, EmailCodePurpose.RESET_PASSWORD));
    }

    /**
     * Cambia la contraseña. Escribir el código también prueba que el correo es suyo, así que la cuenta queda verificada
     * y deja de tener una contraseña temporal.
     */
    @Transactional(noRollbackFor = InvalidEmailCodeException.class)
    public void reset(String email, String code, String newPassword) {
        String normalized = User.normalizeEmail(email);
        User user = users.findByEmail(normalized)
                .orElseThrow(() -> new InvalidEmailCodeException("El código no es válido. Pide uno nuevo."));
        codes.consume(normalized, EmailCodePurpose.RESET_PASSWORD, code);
        users.save(user.withPasswordHash(passwordHasher.hash(newPassword)).withEmailVerified());
    }
}
