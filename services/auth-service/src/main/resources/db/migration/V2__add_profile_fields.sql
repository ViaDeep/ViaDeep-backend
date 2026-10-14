-- Datos opcionales que el ciudadano completa desde "Editar perfil".
ALTER TABLE users ADD COLUMN phone VARCHAR(15);
ALTER TABLE users ADD COLUMN document_number VARCHAR(8);
