# evidence-service

Evidencias: metadatos de fotografías, almacenamiento en S3 y URLs firmadas.

- Puerto local: `8083`
- Esquema de base de datos: `evidences`
- Ruta en el API Gateway: `/evidences`

## Endpoints

Todos requieren el token JWT del usuario (`Authorization: Bearer <token>`).

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/evidences` | Sube la fotografía (multipart, campo `file`). Solo JPEG o PNG, hasta 10 MB; el formato se verifica por el contenido del archivo, no por lo que declara el cliente. Responde 201 con el `id` de la evidencia. |
| GET | `/evidences/{id}` | Metadatos de la evidencia. Lo usa también el servicio de incidencias para validar el reporte. |
| GET | `/evidences/{id}/url` | URL firmada de descarga directa desde S3, válida 10 minutos. |

Solo el ciudadano que subió la foto o un operador pueden consultarla (403 para los demás).

## Almacenamiento

Las fotos se guardan en el bucket `viadeep-evidences` con la clave `evidences/<usuario>/<id>.<ext>`; la base de datos solo guarda los metadatos. En local se usa un emulador de S3 (S3Mock, incluido en el `docker-compose.yml`) que crea el bucket al arrancar y guarda las fotos en el volumen `viadeep-storage-data`, así que se conservan al reiniciar Docker. El servicio también crea el bucket si no existe. En AWS se dejan vacíos `S3_ENDPOINT`, `S3_ACCESS_KEY` y `S3_SECRET_KEY`, y el servicio usa el rol IAM de la tarea ECS.
