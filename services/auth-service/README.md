# auth-service

Autenticación y usuarios: registro, inicio de sesión, perfiles, roles; emite JWT RS256 y publica JWKS.

- Puerto local: `8081`
- Esquema de base de datos: `auth`
- Ruta en el API Gateway: `/auth`

## Endpoints

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| POST | `/auth/register` | Público | Registra un ciudadano (`email`, `password` de 8 a 72 caracteres, `fullName`). Responde 201, o 409 si el correo ya existe. |
| POST | `/auth/login` | Público | Devuelve `accessToken` (JWT RS256), `tokenType` y `expiresIn` en segundos. Responde 401 si las credenciales son incorrectas y 403 si la cuenta está desactivada. Registra la fecha del inicio de sesión. |
| POST | `/auth/operators` | Operador | Da de alta a otro operador municipal (mismo cuerpo que el registro). |
| GET | `/auth/me` | Token | Perfil del usuario autenticado, con `enabled`, `createdAt` y `lastLoginAt`. |
| PUT | `/auth/me` | Token | Edita `fullName`, `phone` (celular de 9 dígitos) y `documentNumber` (DNI de 8 dígitos). |
| PUT | `/auth/me/password` | Token | Cambia la contraseña con `currentPassword` y `newPassword`; 422 si la actual no coincide. |
| GET | `/auth/users` | Operador | Lista paginada de usuarios (`page`, `size` hasta 100), de la cuenta más reciente a la más antigua, con filtros `role` (`CITIZEN` u `OPERATOR`), `enabled` y `q` (nombre o correo, sin distinguir mayúsculas). |
| GET | `/auth/users/{id}` | Operador | Datos de un usuario; 404 si no existe. |
| PATCH | `/auth/users/{id}/status` | Operador | Activa o desactiva la cuenta con `{"enabled": true \| false}`. Un operador no puede desactivar su propia cuenta (409). |
| GET | `/auth/.well-known/jwks.json` | Público | Clave pública (JWKS) con la que los demás servicios validan los tokens. |

El token incluye `sub` (id del usuario), `email`, `name`, `role` (`CITIZEN` u `OPERATOR`) e `iss` (`viadeep-auth`). El registro público solo crea ciudadanos; los operadores los crea otro operador o la cuenta inicial (`OPERATOR_*`).

Cada usuario se devuelve como `id`, `email`, `fullName`, `role`, `phone`, `documentNumber`, `enabled`, `createdAt` y `lastLoginAt` (null si nunca inició sesión). Una cuenta desactivada no puede iniciar sesión; los tokens ya emitidos siguen siendo válidos hasta que vencen.

## Clave de firma

En producción, la clave privada RSA (PKCS#8, formato PEM) se entrega en `VIADEEP_JWT_PRIVATE_KEY` desde AWS Secrets Manager. Si la variable está vacía, el servicio genera una clave temporal al arrancar, válida solo para desarrollo: los tokens dejan de ser válidos cada vez que se reinicia.

Para generar una clave propia:

```bash
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out jwt-private.pem
```

La tabla `users` se crea con Flyway (`src/main/resources/db/migration`).
