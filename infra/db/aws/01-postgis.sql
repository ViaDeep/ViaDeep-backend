\set ON_ERROR_STOP on

-- Debe ejecutarlo una cuenta administrativa de RDS con privilegios para
-- instalar PostGIS. La extensión se mantiene fuera de los esquemas de servicio.
BEGIN;

CREATE EXTENSION IF NOT EXISTS postgis WITH SCHEMA public;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM pg_extension extension
        JOIN pg_namespace namespace ON namespace.oid = extension.extnamespace
        WHERE extension.extname = 'postgis' AND namespace.nspname <> 'public'
    ) THEN
        RAISE EXCEPTION 'PostGIS ya existe fuera del esquema public; revise la instalación antes de continuar';
    END IF;
END
$$;

COMMIT;
