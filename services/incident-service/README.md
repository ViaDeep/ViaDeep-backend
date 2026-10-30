# incident-service

Incidencias: registro, estados e historial, severidad, asignación de responsables, duplicados (PostGIS), estadísticas, geocodificación inversa.

- Puerto local: `8082`
- Esquema de base de datos: `incidents`
- Ruta en el API Gateway: `/incidents`

## Endpoints

Todos requieren el token JWT (`Authorization: Bearer <token>`).

| Método | Ruta | Rol | Descripción |
|---|---|---|---|
| POST | `/incidents` | Ciudadano | Registra el reporte con el `evidenceId` subido antes al servicio de evidencias. |
| GET | `/incidents` | Ambos | Lista paginada (`page`, `size` hasta 100) con filtros `status`, `type`, `severity`, `district`, `assigned` (`true`: con responsable, `false`: sin responsable) y `from`/`to` (fecha de registro, `AAAA-MM-DD`, hora de Lima, ambas incluidas). `sort`: `createdAt` (por defecto) o `updatedAt`, siempre del más reciente al más antiguo. Cada ítem incluye `updatedAt`, `assignee`, `assignedArea` y `assignedAt`. El ciudadano solo ve sus reportes. |
| GET | `/incidents/{id}` | Ambos | Detalle con resultado de la IA, responsable asignado (`assignee`, `assignedArea`, `assignmentNote`, `assignedAt`), historial de estados y posibles duplicados. Un ciudadano solo ve los suyos. |
| GET | `/incidents/stats` | Operador | Totales por estado, tipo, distrito y severidad (`bySeverity`, sin las que aún no tienen severidad) y cuántas tienen responsable (`assigned`), entre `from` y `to` (fechas `AAAA-MM-DD`, hora de Lima; por defecto, los últimos 30 días). |
| PATCH | `/incidents/{id}/status` | Operador | Cambia el estado (`IN_PROGRESS`, `RESOLVED`, `REJECTED`) con `comment` opcional; el rechazo exige motivo. |
| PATCH | `/incidents/{id}/severity` | Operador | Asigna `LOW`, `MEDIUM` o `HIGH` mientras la incidencia está abierta. |
| PATCH | `/incidents/{id}/assignment` | Operador | Asigna o reasigna el responsable: `assignee` y `area` (obligatorios, hasta 120 caracteres) y `note` opcional (hasta 500). 409 si la incidencia ya está `RESOLVED` o `REJECTED`. |

Ejemplo de registro validado por la IA (para un registro manual: `"aiValidated": false` y sin `aiResult`):

```json
{
  "evidenceId": "…",
  "type": "D40",
  "latitude": -12.0464,
  "longitude": -77.0428,
  "description": "Bache frente al mercado",
  "aiValidated": true,
  "aiResult": { "confidence": 0.88, "bboxX": 0.1, "bboxY": 0.2, "bboxWidth": 0.3, "bboxHeight": 0.2 }
}
```

## Reglas

- Tipos: `D00` grieta longitudinal, `D10` grieta transversal, `D20` piel de cocodrilo, `D40` bache.
- Estados: `PENDING → IN_PROGRESS → RESOLVED` y `PENDING → REJECTED`; cada cambio queda en `status_histories`.
- La evidencia debe existir, ser del ciudadano que reporta y no estar usada en otra incidencia (422 si no). Se verifica con `GET /evidences/{id}` reenviando el token; si el servicio de evidencias no responde, 503.
- Asignación: queda registrado el operador que asignó (`assignedBy`) y cuándo (`assignedAt`); reasignar reemplaza al responsable anterior.
- Posibles duplicados: incidencias abiertas del mismo tipo a menos de `INCIDENT_DUPLICATE_RADIUS_METERS` (30 m por defecto), con `ST_DWithin` de PostGIS.
- Al registrar o cambiar de estado se publican los eventos `IncidentRegistered` e `IncidentStatusChanged`. `IncidentNotificationListener` los recibe después de confirmar la transacción y, en segundo plano, `NotificationClient` pide el aviso con `POST /notifications` (tiempo de espera, 3 reintentos y circuit breaker con Resilience4j). Si falla, el aviso no enviado queda en los logs.

## Pruebas

Las pruebas de integración usan el PostgreSQL + PostGIS del `docker-compose.yml` (`docker compose up -d` antes de `mvn verify`), en un esquema `incidents_test` que se limpia al iniciar.
