# notification-service

Notificaciones: avisos para ciudadanos y operadores dentro de la plataforma (RF-10).

- Puerto local: `8084`
- Esquema de base de datos: `notifications`
- Ruta en el API Gateway: `/notifications`

## Endpoints

| Método | Ruta | Quién | Descripción |
|---|---|---|---|
| GET | `/notifications` | Ciudadano u operador (JWT) | Avisos del usuario y de su rol, del más reciente al más antiguo. `unread=true` para ver solo los no leídos; `page` y `size` (hasta 100). Cada aviso trae `read` según el usuario que consulta. |
| GET | `/notifications/unread-count` | Ciudadano u operador (JWT) | `{"count": n}` con los avisos no leídos. |
| PATCH | `/notifications/{id}/read` | Ciudadano u operador (JWT) | Marca un aviso como leído (404 si no es para ese usuario). |
| PATCH | `/notifications/read-all` | Ciudadano u operador (JWT) | Marca todos como leídos; devuelve `{"marked": n}`. |
| POST | `/notifications` | Servicio de incidencias | Crea el aviso de un evento. No se expone en el API Gateway y exige la cabecera `X-Internal-Token` con `NOTIFICATIONS_INTERNAL_TOKEN`. |

## Reglas

- Tipos: `NEW_REPORT` (nuevo reporte, para el rol operador; indica si hay posibles duplicados), `STATUS_CHANGED` (en proceso o resuelta) y `REJECTED` (con el motivo), ambos para el ciudadano que reportó.
- Cada aviso tiene exactamente un destinatario: un usuario (`recipient_user_id`) o un rol (`recipient_role`).
- La lectura se guarda por usuario en `notification_reads`, así cada operador tiene su propio estado de leído en los avisos del rol.
