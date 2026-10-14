-- Un esquema por microservicio (OB2, ADR-004). Cada servicio usa su propio
-- usuario y solo tiene permisos sobre su esquema.
CREATE EXTENSION IF NOT EXISTS postgis;

DO $$
DECLARE
    s text;
BEGIN
    FOREACH s IN ARRAY ARRAY['auth', 'incidents', 'evidences', 'notifications'] LOOP
        EXECUTE format('CREATE ROLE %I LOGIN PASSWORD %L', s || '_svc', s || '_svc');
        EXECUTE format('CREATE SCHEMA %I AUTHORIZATION %I', s, s || '_svc');
        EXECUTE format('REVOKE ALL ON SCHEMA %I FROM PUBLIC', s);
    END LOOP;
END $$;

-- PostGIS vive en el esquema public; el servicio de incidencias necesita usar sus funciones.
GRANT USAGE ON SCHEMA public TO incidents_svc;
