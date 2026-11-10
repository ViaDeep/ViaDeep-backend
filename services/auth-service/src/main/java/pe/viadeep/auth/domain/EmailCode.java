package pe.viadeep.auth.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Código de un solo uso enviado al correo. Se guarda solo su hash, vence a los pocos minutos y deja de aceptarse tras
 * varios intentos fallidos, para que no se pueda adivinar probando combinaciones.
 */
public record EmailCode(UUID id, String email, EmailCodePurpose purpose, String codeHash, Instant createdAt,
                        Instant expiresAt, int attempts) {

    public EmailCode {
        Objects.requireNonNull(id);
        email = User.normalizeEmail(email);
        Objects.requireNonNull(purpose);
        Objects.requireNonNull(codeHash);
        Objects.requireNonNull(createdAt);
        Objects.requireNonNull(expiresAt);
    }

    public static EmailCode issue(String email, EmailCodePurpose purpose, String codeHash, Instant now, Instant expiresAt) {
        return new EmailCode(UUID.randomUUID(), email, purpose, codeHash, now, expiresAt, 0);
    }

    public boolean expired(Instant now) {
        return !now.isBefore(expiresAt);
    }

    public EmailCode withFailedAttempt() {
        return new EmailCode(id, email, purpose, codeHash, createdAt, expiresAt, attempts + 1);
    }
}
