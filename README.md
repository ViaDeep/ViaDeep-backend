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

# 2. Compilar y ejecutar las pruebas
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
| `AUTH_JWKS_URI` | `http://localhost:8081/auth/.well-known/jwks.json` |

En AWS (ECS Fargate + RDS) estos valores vienen de AWS Secrets Manager; las contraseñas locales no deben usarse fuera de desarrollo.

## Estado

Implementados: autenticación (registro, login, JWT RS256 y JWKS) y evidencias (subida de fotos a S3 y URLs firmadas). Incidencias y notificaciones tienen solo su estructura.
