\set ON_ERROR_STOP on

-- Este script no establece ni cambia contraseñas. Los cuatro secretos se
-- asignan por separado con 03-set-service-passwords.psql.
BEGIN;


DO $$
DECLARE
    role_name text;
BEGIN
    FOREACH role_name IN ARRAY ARRAY[
        'auth_svc',
        'incidents_svc',
        'evidences_svc',
        'notifications_svc'
    ] LOOP

        IF NOT EXISTS (
            SELECT 1
            FROM pg_roles
            WHERE rolname = role_name
        ) THEN
            EXECUTE format('CREATE ROLE %I', role_name);
        END IF;

        -- No intentar alterar atributos reservados al superusuario.
        -- Si algún rol existente tiene privilegios elevados, detenerse.
        IF EXISTS (
            SELECT 1
            FROM pg_roles
            WHERE rolname = role_name
              AND (rolsuper OR rolreplication OR rolbypassrls)
        ) THEN
            RAISE EXCEPTION
                'El rol % tiene privilegios elevados; requiere revisión administrativa',
                role_name;
        END IF;

        -- Atributos que el administrador de RDS puede gestionar.
        -- No modifica contraseñas.
        EXECUTE format(
            'ALTER ROLE %I WITH LOGIN NOCREATEDB NOCREATEROLE NOINHERIT',
            role_name
        );

        -- Los roles de servicio no pueden pertenecer a otros roles.
        IF EXISTS (
            SELECT 1
            FROM pg_auth_members membership
            JOIN pg_roles member_role
              ON member_role.oid = membership.member
            WHERE member_role.rolname = role_name
        ) THEN
            RAISE EXCEPTION
                'El rol % pertenece a otro rol; requiere revisión',
                role_name;
        END IF;

    END LOOP;
END
$$;


-- La base de ViaDeep es exclusiva: las conexiones se conceden de forma explícita.
REVOKE CONNECT, TEMPORARY ON DATABASE :"DBNAME" FROM PUBLIC;
GRANT CONNECT ON DATABASE :"DBNAME" TO auth_svc, incidents_svc, evidences_svc, notifications_svc;

-- PostgreSQL exige dos condiciones para crear un esquema para otro rol o
-- transferirle su propiedad: poder hacer SET ROLE al nuevo propietario y que
-- ese propietario tenga CREATE sobre la base. En RDS la cuenta administrativa
-- no es un superusuario real, por lo que ambas concesiones deben ser explícitas.
--
-- La membresía se concede con la propia sesión como grantor para poder retirar
-- exactamente esa concesión al final. PostgreSQL 16 separa SET e INHERIT; para
-- versiones anteriores una membresía permite SET ROLE de forma implícita.
DO $$
DECLARE
    role_name text;
    bootstrap_user text := session_user;
BEGIN
    FOREACH role_name IN ARRAY ARRAY['auth_svc', 'incidents_svc', 'evidences_svc', 'notifications_svc'] LOOP
        IF EXISTS (
            SELECT 1
            FROM pg_auth_members membership
            JOIN pg_roles granted_role ON granted_role.oid = membership.roleid
            JOIN pg_roles member_role ON member_role.oid = membership.member
            JOIN pg_roles grantor_role ON grantor_role.oid = membership.grantor
            WHERE granted_role.rolname = role_name
              AND member_role.rolname = bootstrap_user
              AND (
                  current_setting('server_version_num')::integer < 160000
                  OR grantor_role.rolname = bootstrap_user
              )
        ) THEN
            RAISE EXCEPTION
                'La cuenta de bootstrap ya tiene una membresía preexistente incompatible con una revocación segura en %',
                role_name;
        END IF;

        IF current_setting('server_version_num')::integer >= 160000 THEN
            EXECUTE format(
                'GRANT %I TO %I WITH INHERIT FALSE, SET TRUE GRANTED BY CURRENT_USER',
                role_name,
                bootstrap_user
            );
        ELSE
            EXECUTE format('GRANT %I TO %I', role_name, bootstrap_user);
        END IF;
    END LOOP;
END
$$;

GRANT CREATE ON DATABASE :"DBNAME" TO auth_svc, incidents_svc, evidences_svc, notifications_svc;

CREATE SCHEMA IF NOT EXISTS auth AUTHORIZATION auth_svc;
CREATE SCHEMA IF NOT EXISTS incidents AUTHORIZATION incidents_svc;
CREATE SCHEMA IF NOT EXISTS evidences AUTHORIZATION evidences_svc;
CREATE SCHEMA IF NOT EXISTS notifications AUTHORIZATION notifications_svc;

-- Si el esquema ya existía con el propietario esperado no se ejecuta ALTER:
-- una membresía SET sin INHERIT no concede implícitamente los derechos del
-- propietario. Si tiene otro propietario, la cuenta administrativa debe ser ese
-- propietario (o heredar sus derechos) para transferirlo de forma segura.
DO $$
DECLARE
    schema_name text;
    expected_owner text;
    current_owner text;
BEGIN
    FOR schema_name, expected_owner IN
        SELECT * FROM (VALUES
            ('auth', 'auth_svc'),
            ('incidents', 'incidents_svc'),
            ('evidences', 'evidences_svc'),
            ('notifications', 'notifications_svc')
        ) AS expected(schema_name, owner_name)
    LOOP
        SELECT owner.rolname
        INTO current_owner
        FROM pg_namespace namespace
        JOIN pg_roles owner ON owner.oid = namespace.nspowner
        WHERE namespace.nspname = schema_name;

        IF current_owner <> expected_owner THEN
            EXECUTE format('ALTER SCHEMA %I OWNER TO %I', schema_name, expected_owner);
        END IF;
    END LOOP;
END
$$;

REVOKE ALL ON SCHEMA auth, incidents, evidences, notifications FROM PUBLIC;
GRANT USAGE, CREATE ON SCHEMA auth TO auth_svc;
GRANT USAGE, CREATE ON SCHEMA incidents TO incidents_svc;
GRANT USAGE, CREATE ON SCHEMA evidences TO evidences_svc;
GRANT USAGE, CREATE ON SCHEMA notifications TO notifications_svc;

-- PostGIS vive en public. Solo incidencias necesita resolver sus tipos y funciones.
REVOKE ALL ON SCHEMA public FROM PUBLIC;
GRANT USAGE ON SCHEMA public TO incidents_svc;

-- currentSchema en JDBC prevalece; estos valores protegen también conexiones
-- administrativas o manuales que no especifiquen currentSchema.
ALTER ROLE auth_svc SET search_path = auth, pg_catalog;
ALTER ROLE incidents_svc SET search_path = incidents, public, pg_catalog;
ALTER ROLE evidences_svc SET search_path = evidences, pg_catalog;
ALTER ROLE notifications_svc SET search_path = notifications, pg_catalog;

-- Ningún permiso usado para asignar propietarios debe sobrevivir al bootstrap.
-- Si cualquier sentencia anterior o esta limpieza falla, BEGIN/ON_ERROR_STOP
-- revierte la transacción completa, incluidas las concesiones temporales.
REVOKE CREATE, TEMPORARY ON DATABASE :"DBNAME" FROM auth_svc, incidents_svc, evidences_svc, notifications_svc;

DO $$
DECLARE
    role_name text;
    bootstrap_user text := session_user;
BEGIN
    FOREACH role_name IN ARRAY ARRAY['auth_svc', 'incidents_svc', 'evidences_svc', 'notifications_svc'] LOOP
        IF current_setting('server_version_num')::integer >= 160000 THEN
            EXECUTE format(
                'REVOKE %I FROM %I GRANTED BY CURRENT_USER',
                role_name,
                bootstrap_user
            );
        ELSE
            EXECUTE format('REVOKE %I FROM %I', role_name, bootstrap_user);
        END IF;
    END LOOP;
END
$$;

COMMIT;
