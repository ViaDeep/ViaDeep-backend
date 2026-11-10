package pe.viadeep.auth.application;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import pe.viadeep.auth.domain.EmailCode;
import pe.viadeep.auth.domain.EmailCodePurpose;
import pe.viadeep.auth.domain.EmailCodeRepository;

/**
 * Genera, envía y comprueba los códigos de 6 dígitos que llegan al correo. Un correo inventado nunca recibe el código,
 * así que la cuenta solo se activa si el correo existe y es de quien se registra.
 */
@Service
@EnableConfigurationProperties(EmailCodeProperties.class)
public class EmailCodeService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final EmailCodeRepository codes;
    private final PasswordHasher hasher;
    private final EmailSender sender;
    private final EmailCodeProperties properties;
    private final Clock clock;

    public EmailCodeService(EmailCodeRepository codes, PasswordHasher hasher, EmailSender sender,
                            EmailCodeProperties properties, Clock clock) {
        this.codes = codes;
        this.hasher = hasher;
        this.sender = sender;
        this.properties = properties;
        this.clock = clock;
    }

    /** Envía un código nuevo, que reemplaza al anterior. Respeta un tiempo mínimo entre envíos al mismo correo. */
    public void send(String normalizedEmail, EmailCodePurpose purpose) {
        Instant now = clock.instant();
        codes.findLatest(normalizedEmail, purpose).ifPresent(last -> {
            Duration since = Duration.between(last.createdAt(), now);
            if (since.compareTo(properties.resendCooldown()) < 0) {
                throw new EmailCodeTooSoonException(properties.resendCooldown().minus(since).toSeconds() + 1);
            }
        });
        String code = "%06d".formatted(RANDOM.nextInt(1_000_000));
        codes.deleteAll(normalizedEmail, purpose);
        codes.save(EmailCode.issue(normalizedEmail, purpose, hasher.hash(code), now, now.plus(properties.ttl())));
        long minutes = properties.ttl().toMinutes();
        if (purpose == EmailCodePurpose.VERIFY_EMAIL) {
            sender.send(normalizedEmail, "Tu código de verificación de ViaDeep: " + code, """
                    Hola:

                    Tu código para activar tu cuenta de ViaDeep es: %s

                    Escríbelo en la app para confirmar tu correo. Vence en %d minutos.
                    Si no creaste una cuenta en ViaDeep, ignora este mensaje.
                    """.formatted(code, minutes));
        } else {
            sender.send(normalizedEmail, "Código para restablecer tu contraseña de ViaDeep: " + code, """
                    Hola:

                    Recibimos un pedido para restablecer la contraseña de tu cuenta de ViaDeep.
                    Tu código es: %s

                    Escríbelo en la app junto con tu nueva contraseña. Vence en %d minutos.
                    Si no lo pediste, ignora este mensaje: tu contraseña no cambiará.
                    """.formatted(code, minutes));
        }
    }

    /** Comprueba el código y, si es correcto, lo elimina para que no se pueda usar dos veces. */
    public void consume(String normalizedEmail, EmailCodePurpose purpose, String code) {
        EmailCode stored = codes.findLatest(normalizedEmail, purpose)
                .orElseThrow(() -> new InvalidEmailCodeException("El código no es válido. Pide uno nuevo."));
        if (stored.expired(clock.instant())) {
            throw new InvalidEmailCodeException("El código venció. Pide uno nuevo.");
        }
        if (stored.attempts() >= properties.maxAttempts()) {
            throw new InvalidEmailCodeException("Demasiados intentos. Pide un código nuevo.");
        }
        if (code == null || !hasher.matches(code.strip(), stored.codeHash())) {
            codes.save(stored.withFailedAttempt());
            throw new InvalidEmailCodeException("El código no es correcto");
        }
        codes.deleteAll(normalizedEmail, purpose);
    }
}
