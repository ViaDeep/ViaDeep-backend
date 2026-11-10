package pe.viadeep.auth.application;

import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.viadeep.auth.domain.EmailCodePurpose;
import pe.viadeep.auth.domain.User;
import pe.viadeep.auth.domain.UserRepository;

/** Confirmación del correo de un ciudadano recién registrado. */
@Service
public class EmailVerificationUseCase {

    private final UserRepository users;
    private final EmailCodeService codes;
    private final TokenIssuer tokenIssuer;
    private final Clock clock;

    public EmailVerificationUseCase(UserRepository users, EmailCodeService codes, TokenIssuer tokenIssuer, Clock clock) {
        this.users = users;
        this.codes = codes;
        this.tokenIssuer = tokenIssuer;
        this.clock = clock;
    }

    /**
     * Con el código correcto la cuenta queda verificada y se inicia sesión de una vez. Un intento fallido se guarda
     * aunque la respuesta sea un error, por eso corre en su propia transacción sin deshacerse.
     */
    @Transactional(noRollbackFor = InvalidEmailCodeException.class)
    public LoginUseCase.Session verify(String email, String code) {
        String normalized = User.normalizeEmail(email);
        User user = users.findByEmail(normalized)
                .orElseThrow(() -> new InvalidEmailCodeException("El código no es válido. Pide uno nuevo."));
        if (!user.emailVerified()) {
            codes.consume(normalized, EmailCodePurpose.VERIFY_EMAIL, code);
            user = users.save(user.withEmailVerified());
        }
        if (!user.enabled()) {
            throw new AccountDisabledException();
        }
        User saved = users.save(user.withLastLoginAt(clock.instant()));
        return new LoginUseCase.Session(tokenIssuer.issue(saved), saved.passwordChangeRequired());
    }

    /** Reenvía el código. No dice si el correo está registrado, para no revelar qué correos tienen cuenta. */
    @Transactional
    public void resend(String email) {
        String normalized = User.normalizeEmail(email);
        users.findByEmail(normalized)
                .filter(user -> !user.emailVerified())
                .ifPresent(user -> codes.send(normalized, EmailCodePurpose.VERIFY_EMAIL));
    }
}
