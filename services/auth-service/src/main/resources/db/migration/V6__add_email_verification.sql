-- Los ciudadanos confirman su correo con un código de 6 dígitos antes de iniciar sesión. Las cuentas que ya existían
-- se consideran verificadas.
ALTER TABLE users ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT TRUE;

-- Códigos enviados por correo para verificar la cuenta o restablecer la contraseña. Solo se guarda su hash.
CREATE TABLE email_codes (
    id          UUID PRIMARY KEY,
    email       VARCHAR(254) NOT NULL,
    purpose     VARCHAR(20)  NOT NULL,
    code_hash   VARCHAR(100) NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    attempts    INTEGER      NOT NULL DEFAULT 0
);

CREATE INDEX email_codes_email_purpose_idx ON email_codes (email, purpose);
