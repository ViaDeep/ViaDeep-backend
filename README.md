# ViaDeep Backend

Backend de **ViaDeep Platform**: cuatro microservicios en Java 21 + Spring Boot, uno por bounded context, según el documento *OB2 - Diseño de Arquitectura v2.0*.

| Servicio | Carpeta | Puerto local | Esquema BD | Ruta en API Gateway |
|---|---|---|---|---|
| Autenticación y usuarios | `services/auth-service` | 8081 | `auth` | `/auth` |
| Incidencias | `services/incident-service` | 8082 | `incidents` | `/incidents` |
| Evidencias | `services/evidence-service` | 8083 | `evidences` | `/evidences` |
| Notificaciones | `services/notification-service` | 8084 | `notifications` | `/notifications` |

Cada servicio sigue Clean Architecture con cuatro paquetes: `presentation`, `application`, `domain` e `infrastructure`. Las dependencias apuntan hacia adentro: `domain` y `application` no dependen de Spring, de la base de datos ni de otros servicios.

## Requisitos

- Java 21
- Maven 3.9+
- Docker (para la base de datos y el almacenamiento local)

## Levantar en local

```bash
# 1. PostgreSQL + PostGIS (crea los 4 esquemas y sus usuarios) y un emulador local de S3 para las fotos
docker compose up -d

# 2. Compilar y ejecutar las pruebas (las de incidencias usan el PostGIS del paso 1)
mvn verify

# 3. Ejecutar un servicio (ejemplo: incidencias)
mvn -pl services/incident-service spring-boot:run
```

Verifica que el servicio responde en `http://localhost:8082/actuator/health`.

## Variables de entorno

| Variable | Valor por defecto |
|---|---|
| `DB_HOST` / `DB_PORT` / `DB_NAME` | `localhost` / `5432` / `viadeep` |
| `DB_USER` / `DB_PASSWORD` | `<esquema>_svc` (solo para desarrollo local) |
| `S3_ENDPOINT` / `S3_BUCKET` | `http://localhost:9000` / `viadeep-evidences` (emulador S3 local) |
| `EVIDENCE_SERVICE_URL` | `http://localhost:8083` (lo usa incidencias para validar la evidencia) |
| `AUTH_JWKS_URI` | `http://localhost:8081/auth/.well-known/jwks.json` |
| `OPERATOR_EMAIL` / `OPERATOR_PASSWORD` / `OPERATOR_NAME` | vacíos: no se crea operador. Si se definen, autenticación crea al iniciar el administrador (el encargado que designa la municipalidad; contraseña de 8 caracteres o más). Si ese correo ya existe como operador, lo convierte en administrador |
| `OPERATOR_EMAIL_DOMAINS` | vacío: acepta cualquier correo (solo desarrollo). Dominios institucionales permitidos para operadores, separados por comas, por ejemplo `munlima.gob.pe` |
| `NOTIFICATION_SERVICE_URL` | `http://localhost:8084` (lo usa incidencias para pedir los avisos) |
| `NOTIFICATIONS_INTERNAL_TOKEN` | `viadeep-dev-internal-token`: clave compartida entre incidencias y notificaciones para crear avisos. En AWS debe ser un secreto propio, igual en ambos servicios |
| `GEOCODING_ENABLED` | `true`: incidencias obtiene el distrito del GPS con Nominatim (OpenStreetMap) |
| `GEOCODING_BASE_URL` / `GEOCODING_USER_AGENT` | `https://nominatim.openstreetmap.org` / `ViaDeep/0.1 (...)` |

## Operadores municipales

El registro público (`POST /auth/register`) solo crea ciudadanos. Hay dos tipos de cuenta municipal:

- **Administrador.** El encargado que designa la municipalidad. Se crea con las variables `OPERATOR_*` al iniciar el servicio de autenticación. Es el único que da de alta operadores (`POST /auth/operators`) y que lista, activa o desactiva cuentas. También gestiona incidencias como cualquier operador.
  Puede haber varios administradores: un administrador da o quita el permiso a otro operador con `PATCH /auth/users/{id}/administrator` (`{"administrator": true}`). Nadie puede quitarse su propio permiso ni desactivar su propia cuenta, así que siempre queda al menos uno. La cuenta de `OPERATOR_EMAIL` vuelve a ser administrador cada vez que el servicio arranca: es la cuenta de respaldo de la municipalidad. Un cambio de permiso se aplica cuando la persona vuelve a iniciar sesión (el token anterior dura como máximo `VIADEEP_JWT_TTL`, 1 h por defecto).
- **Operador.** Solo gestiona incidencias. No puede crear cuentas (403), así que no puede abrirse cuentas extra a su nombre.

El token lleva `role: OPERATOR` en ambos casos (los otros servicios no cambian) y `admin: true` solo en el del administrador.

`POST /auth/operators` recibe `email`, `password` (temporal), `fullName` y `documentNumber` (DNI de 8 dígitos, obligatorio). Una persona solo puede tener una cuenta de operador: otro operador con el mismo DNI responde 409. El operador tampoco puede cambiar su DNI desde su perfil (422).

Para que solo personal de la municipalidad tenga cuenta de operador:

- **Correo institucional.** Con `OPERATOR_EMAIL_DOMAINS` configurado, el operador inicial y los que se den de alta deben usar un correo de esos dominios; otro correo responde 422. No hace falta una lista de correos: el dominio prueba que la cuenta pertenece a la institución.
- **Contraseña temporal.** La contraseña que pone el administrador al dar de alta al operador es temporal. El login responde `passwordChangeRequired: true` (también `GET /auth/me`) hasta que el nuevo operador la cambie con `PUT /auth/me/password`, y la plataforma web no lo deja continuar antes de hacerlo.

La verificación por un enlace enviado al correo no está en el alcance de OB2 (no hay servicio de correo).

El administrador gestiona las cuentas con `GET /auth/users` (filtros `role`, `enabled` y `q`), `PUT /auth/users/{id}` (corrige nombre y celular) y `PATCH /auth/users/{id}/status`; cualquier operador puede leer `GET /auth/users/{id}` (por ejemplo, para ver quién hizo un reporte) y `GET /auth/operators`, la lista de operadores activos para asignar incidencias. `{"enabled": false}` desactiva una cuenta; nadie puede desactivar la suya. Una cuenta desactivada recibe 403 al iniciar sesión. Ver `services/auth-service/README.md`.

## Notificaciones

Al registrar un reporte o cambiar su estado, incidencias pide el aviso al servicio de notificaciones en segundo plano, después de confirmar la operación (ADR-013): espera como máximo 2 s, reintenta 3 veces y, si el servicio sigue fallando, el circuit breaker deja de llamarlo por 30 s. Si el aviso no se puede enviar, queda en los logs y el reporte no se ve afectado. Para recibir avisos en local, levanta también `mvn -pl services/notification-service spring-boot:run` (puerto 8084).

## Distrito automático

Al registrar un reporte, incidencias consulta el distrito del punto GPS en Nominatim. Si OpenStreetMap no responde (2 s como máximo) el reporte se registra igual, sin distrito. El servidor público de Nominatim permite como máximo una consulta por segundo; con más tráfico conviene una instancia propia (`GEOCODING_BASE_URL`).

En AWS (ECS Fargate + RDS) estos valores vienen de AWS Secrets Manager; las contraseñas locales no deben usarse fuera de desarrollo.

## Estado

Implementados: autenticación (registro, login, JWT RS256 y JWKS), evidencias (subida de fotos a S3 y URLs firmadas), incidencias (registro, estados, severidad, asignación de responsable y área con `PATCH /incidents/{id}/assignment`, duplicados con PostGIS, estadísticas y distrito automático con OpenStreetMap), cuentas de operador, administración de usuarios (`/auth/users`) edición del perfil (`PUT /auth/me` y `PUT /auth/me/password`) y notificaciones (avisos de nuevo reporte para los operadores y de cambio de estado o rechazo para el ciudadano, con estado de lectura por usuario).
