-- Los operadores dados de alta por otro operador reciben una contraseña temporal y deben cambiarla al primer ingreso.
ALTER TABLE users ADD COLUMN password_change_required BOOLEAN NOT NULL DEFAULT FALSE;
