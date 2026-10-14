# incident-service

Incidencias: registro, estados e historial, severidad, duplicados (PostGIS), estadísticas, geocodificación inversa.

- Puerto local: `8082`
- Esquema de base de datos: `incidents`
- Ruta en el API Gateway: `/incidents`

## Endpoints

Todos requieren el token JWT (`Authorization: Bearer <token>`).

| Método | Ruta | Rol | Descripción |
|---|---|---|---|
| POST | `/incidents` | Ciudadano | Registra el reporte con el `evidenceId` subido antes al servicio de evidencias. |
| GET | `/incidents` | Ambos | Lista paginada (`page`, `size` hasta 100) con filtros `status`, `type`, `severity` y `district`. El ciudadano solo ve sus reportes. |
| GET | `/incidents/{id}` | Ambos | Detalle con resultado de la IA, historial de estados y posibles duplicados. Un ciudadano solo ve los suyos. |
| GET | `/incidents/stats` | Operador | Totales por estado, tipo y distrito entre `from` y `to` (fechas `AAAA-MM-DD`, hora de Lima; por defecto, los últimos 30 días). |
| PATCH | `/incidents/{id}/status` | Operador | Cambia el estado (`IN_PROGRESS`, `RESOLVED`, `REJECTED`) con `comment` opcional; el rechazo exige motivo. |
| PATCH | `/incidents/{id}/severity` | Operador | Asigna `LOW`, `MEDIUM` o `HIGH` mientras la incidencia está abierta. |

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
- Posibles duplicados: incidencias abiertas del mismo tipo a menos de `INCIDENT_DUPLICATE_RADIUS_METERS` (30 m por defecto), con `ST_DWithin` de PostGIS.
- Al registrar o cambiar de estado se publican los eventos `IncidentRegistered` e `IncidentStatusChanged`, que usará el servicio de notificaciones.

## Pruebas

Las pruebas de integración usan el PostgreSQL + PostGIS del `docker-compose.yml` (`docker compose up -d` antes de `mvn verify`), en un esquema `incidents_test` que se limpia al iniciar.
