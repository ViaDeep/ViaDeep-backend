-- Los operadores pueden desactivar cuentas; una cuenta desactivada no puede iniciar sesión.
ALTER TABLE users ADD COLUMN enabled BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE users ADD COLUMN last_login_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX idx_users_created_at ON users (created_at);
