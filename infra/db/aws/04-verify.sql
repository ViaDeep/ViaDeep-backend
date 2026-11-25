\set ON_ERROR_STOP on

DO $$
DECLARE
    service_role text;
    service_schema text;
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_extension extension
        JOIN pg_namespace namespace ON namespace.oid = extension.extnamespace
        WHERE extension.extname = 'postgis' AND namespace.nspname = 'public'
    ) THEN
        RAISE EXCEPTION 'PostGIS no está instalado en el esquema public';
    END IF;

    FOR service_role, service_schema IN
        SELECT * FROM (VALUES
            ('auth_svc', 'auth'),
            ('incidents_svc', 'incidents'),
            ('evidences_svc', 'evidences'),
            ('notifications_svc', 'notifications')
        ) AS expected(role_name, schema_name)
    LOOP
        IF NOT EXISTS (
            SELECT 1
            FROM pg_roles
            WHERE rolname = service_role
              AND rolcanlogin
              AND NOT rolsuper
              AND NOT rolcreatedb
              AND NOT rolcreaterole
              AND NOT rolinherit
              AND NOT rolreplication
              AND NOT rolbypassrls
        ) THEN
            RAISE EXCEPTION 'El rol % no existe o tiene privilegios administrativos', service_role;
        END IF;

        IF EXISTS (
            SELECT 1
            FROM pg_auth_members membership
            JOIN pg_roles member_role ON member_role.oid = membership.member
            WHERE member_role.rolname = service_role
        ) THEN
            RAISE EXCEPTION 'El rol de servicio % pertenece a otro rol', service_role;
        END IF;

        IF NOT EXISTS (
            SELECT 1
            FROM pg_namespace namespace
            JOIN pg_roles owner ON owner.oid = namespace.nspowner
            WHERE namespace.nspname = service_schema AND owner.rolname = service_role
        ) THEN
            RAISE EXCEPTION 'El esquema % no existe o no pertenece a %', service_schema, service_role;
        END IF;

        IF NOT has_database_privilege(service_role, current_database(), 'CONNECT')
           OR NOT has_schema_privilege(service_role, service_schema, 'USAGE')
           OR NOT has_schema_privilege(service_role, service_schema, 'CREATE') THEN
            RAISE EXCEPTION 'El rol % no tiene los permisos Flyway requeridos en %', service_role, service_schema;
        END IF;

        IF has_database_privilege(service_role, current_database(), 'CREATE')
           OR has_database_privilege(service_role, current_database(), 'TEMPORARY') THEN
            RAISE EXCEPTION 'El rol % conserva CREATE o TEMPORARY sobre la base', service_role;
        END IF;

        IF EXISTS (
            SELECT 1
            FROM pg_auth_members membership
            JOIN pg_roles granted_role ON granted_role.oid = membership.roleid
            JOIN pg_roles member_role ON member_role.oid = membership.member
            JOIN pg_roles grantor_role ON grantor_role.oid = membership.grantor
            WHERE granted_role.rolname = service_role
              AND member_role.rolname = session_user
              AND grantor_role.rolname = session_user
        ) THEN
            RAISE EXCEPTION 'La membresía temporal de la cuenta de bootstrap en % no fue revocada', service_role;
        END IF;
    END LOOP;

    IF NOT has_schema_privilege('incidents_svc', 'public', 'USAGE') THEN
        RAISE EXCEPTION 'incidents_svc no puede usar los tipos y funciones PostGIS de public';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM (VALUES
            ('auth_svc', 'auth'),
            ('incidents_svc', 'incidents'),
            ('evidences_svc', 'evidences'),
            ('notifications_svc', 'notifications')
        ) AS roles(role_name, own_schema)
        CROSS JOIN (VALUES ('auth'), ('incidents'), ('evidences'), ('notifications')) AS schemas(schema_name)
        WHERE roles.own_schema <> schemas.schema_name
          AND has_schema_privilege(roles.role_name, schemas.schema_name, 'CREATE')
    ) THEN
        RAISE EXCEPTION 'Un rol conserva permisos CREATE fuera de su propio esquema';
    END IF;

    IF has_schema_privilege('auth_svc', 'public', 'USAGE')
       OR has_schema_privilege('evidences_svc', 'public', 'USAGE')
       OR has_schema_privilege('notifications_svc', 'public', 'USAGE') THEN
        RAISE EXCEPTION 'Un rol que no usa PostGIS conserva USAGE sobre public';
    END IF;
END
$$;

SELECT current_database() AS database_name,
       current_user AS verified_by,
       postgis_full_version() AS postgis_version;

SELECT namespace.nspname AS schema_name,
       owner.rolname AS owner_name
FROM pg_namespace namespace
JOIN pg_roles owner ON owner.oid = namespace.nspowner
WHERE namespace.nspname IN ('auth', 'incidents', 'evidences', 'notifications', 'public')
ORDER BY namespace.nspname;
