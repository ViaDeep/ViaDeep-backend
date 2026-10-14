-- El administrador (el encargado que designa la municipalidad) es el único operador que crea, activa y desactiva
-- cuentas. El resto de operadores solo gestiona incidencias. Al iniciar, el servicio marca como administrador al
-- operador inicial configurado con OPERATOR_EMAIL.
ALTER TABLE users ADD COLUMN administrator BOOLEAN NOT NULL DEFAULT FALSE;
