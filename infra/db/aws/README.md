# Bootstrap de PostgreSQL/PostGIS en Amazon RDS

Este directorio inicializa la base de datos de ViaDeep en RDS sin depender de
`/docker-entrypoint-initdb.d`. No crea la instancia RDS ni la base: ambas deben
existir antes de ejecutar los scripts. El flujo no elimina tablas, esquemas ni datos.

## Requisitos

- Una instancia Amazon RDS for PostgreSQL cuya versión soporte PostGIS.
- Una base exclusiva para ViaDeep; su nombre debe coincidir con `DB_NAME`.
- Una cuenta administrativa de RDS con privilegios para `CREATE EXTENSION postgis`,
  crear o administrar los cuatro roles y administrar la base. Debe ser propietaria
  de la base o poder conceder `CREATE` sobre ella. Si un esquema ya existe con un
  propietario incorrecto, la cuenta también debe ser ese propietario o heredar sus
  derechos. No se entrega esta cuenta a ECS.
- `rds.force_ssl=1` en el parameter group. En PostgreSQL 15 o posterior es el
  valor predeterminado, pero debe verificarse expresamente.
- `psql` y el bundle de CA oficial de RDS en el equipo administrativo.
- Cuatro secretos diferentes para `auth_svc`, `incidents_svc`, `evidences_svc`
  y `notifications_svc`.

Compruebe que PostGIS está disponible para la versión elegida antes de continuar:

```sql
SELECT name, default_version, installed_version
FROM pg_available_extensions
WHERE name = 'postgis';
```

Si la instancia restringe extensiones mediante `rds.allowed_extensions`, incluya
`postgis` en el parameter group antes de ejecutar el paso 1.

## Conexión administrativa segura

Use siempre el endpoint DNS de RDS, no una IP. Configure `psql` con verificación
de cadena y nombre del servidor:

```text
PGSSLMODE=verify-full
PGSSLROOTCERT=/ruta/segura/global-bundle.pem
```

Conéctese sin colocar la contraseña administrativa en la línea de comandos; deje
que `psql` la solicite:

```bash
psql --host <rds-endpoint> --port 5432 --username <rds-admin> --dbname <DB_NAME>
```

## Orden de ejecución

Ejecute desde la raíz del repositorio, conectado a la base indicada por `DB_NAME`:

```bash
psql --host <rds-endpoint> --port 5432 --username <rds-admin> --dbname <DB_NAME> \
  --file infra/db/aws/01-postgis.sql

psql --host <rds-endpoint> --port 5432 --username <rds-admin> --dbname <DB_NAME> \
  --file infra/db/aws/02-service-roles-and-schemas.sql

psql --host <rds-endpoint> --port 5432 --username <rds-admin> --dbname <DB_NAME> \
  --file infra/db/aws/03-set-service-passwords.psql

psql --host <rds-endpoint> --port 5432 --username <rds-admin> --dbname <DB_NAME> \
  --file infra/db/aws/04-verify.sql
```

1. `01-postgis.sql` instala PostGIS en `public`. Requiere la cuenta administrativa.
2. `02-service-roles-and-schemas.sql` crea o reconcilia roles, esquemas y permisos.
   Para cumplir las reglas de propiedad de PostgreSQL concede temporalmente a los
   roles `CREATE` sobre la base y a la sesión administrativa capacidad de `SET ROLE`.
   Revoca ambas concesiones antes del `COMMIT`; un error revierte toda la transacción.
   No conoce ni cambia contraseñas.
3. `03-set-service-passwords.psql` usa `\password`, que solicita las contraseñas
   sin escribirlas en el script ni en el historial SQL. Pegue un secreto distinto
   para cada rol desde Secrets Manager. Ejecútelo nuevamente solo para asignar los
   mismos valores o cuando se haya aprobado una rotación.
4. `04-verify.sql` falla si falta PostGIS, un esquema, un rol o los permisos mínimos.

Los pasos 1, 2 y 4 se pueden repetir. Usan `IF NOT EXISTS`, reconcilian atributos
sin modificar contraseñas y nunca ejecutan `DROP`, `TRUNCATE` ni borrados. El paso
3 está separado para que una repetición del bootstrap no rote credenciales.

## Roles, esquemas y Flyway

| Microservicio | `DB_USER` | Esquema | `currentSchema` |
|---|---|---|---|
| auth-service | `auth_svc` | `auth` | `auth` |
| incident-service | `incidents_svc` | `incidents` | `incidents,public` |
| evidence-service | `evidences_svc` | `evidences` | `evidences` |
| notification-service | `notifications_svc` | `notifications` | `notifications` |

Cada rol tiene `CONNECT` sobre la base y `USAGE, CREATE` solamente sobre su
esquema, del cual es propietario. Esto permite que Flyway cree
`flyway_schema_history`, tablas, índices y futuras alteraciones, sin permisos de
RDS ni acceso de escritura a otros esquemas. `incidents_svc` recibe además `USAGE`
sobre `public` para resolver `GEOGRAPHY` y las funciones PostGIS.
El bootstrap falla si un rol de servicio ya pertenece a otro rol, para impedir
que conserve privilegios heredados o capacidad de asumir un rol administrativo.

Los roles no conservan `CREATE` ni `TEMPORARY` sobre la base. En PostgreSQL 16,
un usuario no superusuario con `CREATEROLE` recibe automáticamente una membresía
administrativa, sin `SET` ni `INHERIT`, sobre los roles que crea. El script no usa
esa membresía para ejecutar como el servicio: crea una concesión separada cuyo
grantor es la sesión y la revoca expresamente. Si detecta una concesión previa con
ese mismo grantor, falla antes de alterarla para no eliminar permisos preexistentes.
En PostgreSQL anterior a 16, donde no existen concesiones independientes de `SET`,
falla ante cualquier membresía previa por la misma razón.

Al arrancar por primera vez cada tarea ECS, Flyway ejecuta las migraciones de su
propio `services/<servicio>/src/main/resources/db/migration`. El bootstrap no
ejecuta ni duplica esas migraciones.

## Secrets Manager y ECS

Guarde cada usuario y contraseña como un secreto independiente o como claves
separadas de un secreto JSON. En la task definition:

- `DB_HOST`: endpoint DNS privado de RDS.
- `DB_PORT`: puerto de RDS.
- `DB_NAME`: base ViaDeep inicializada por estos scripts.
- `DB_USER`: rol de la tabla anterior.
- `DB_PASSWORD`: referencia al valor correspondiente de Secrets Manager.
- `DB_SSL_ROOT_CERT`: `/app/certs/global-bundle.pem`. El `Dockerfile` descarga el
  bundle global público desde el trust store oficial de RDS, verifica su SHA-256 y
  establece esta variable en la imagen. Solo debe sobrescribirse para usar otro
  bundle público oficial de AWS incorporado de forma equivalente.

El bundle CA es público, no es un secreto, y queda legible en modo de solo lectura
por el usuario no privilegiado `viadeep`. Los cuatro perfiles `aws` usan
`sslmode=verify-full`; si falta el bundle, la cadena no es confiable o `DB_HOST` no
coincide con el certificado, la conexión falla sin degradarse a TLS sin validación.

## Comprobaciones finales

Después de `04-verify.sql`:

1. Conéctese una vez con cada secreto mediante `psql` y `sslmode=verify-full`.
2. Ejecute `SHOW search_path;` y confirme el esquema esperado.
3. Con `incidents_svc`, ejecute `SELECT postgis_version();`.
4. Arranque una tarea de cada servicio y confirme que Flyway termina sin errores.
5. Compruebe que cada rol no puede crear objetos en los otros tres esquemas.

No entregue a las aplicaciones la cuenta administrativa ni el rol `rds_superuser`.
