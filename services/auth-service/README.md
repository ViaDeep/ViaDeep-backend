# auth-service

Autenticación y usuarios: registro, inicio de sesión, perfiles, roles; emite JWT RS256 y publica JWKS.

- Puerto local: `8081`
- Esquema de base de datos: `auth`
- Ruta en el API Gateway: `/auth`

## Endpoints

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| POST | `/auth/register` | Público | Registra un ciudadano (`email`, `password` de 8 a 72 caracteres, `fullName`). Responde 201, o 409 si el correo ya existe. |
| POST | `/auth/login` | Público | Devuelve `accessToken` (JWT RS256), `tokenType` y `expiresIn` en segundos. Responde 401 si las credenciales son incorrectas. |
| GET | `/auth/me` | Token | Perfil del usuario autenticado. |
| GET | `/auth/.well-known/jwks.json` | Público | Clave pública (JWKS) con la que los demás servicios validan los tokens. |

El token incluye `sub` (id del usuario), `email`, `name`, `role` (`CITIZEN` u `OPERATOR`) e `iss` (`viadeep-auth`). El registro público solo crea ciudadanos; el alta de operadores municipales queda pendiente.

## Clave de firma

En producción, la clave privada RSA (PKCS#8, formato PEM) se entrega en `VIADEEP_JWT_PRIVATE_KEY` desde AWS Secrets Manager. Si la variable está vacía, el servicio genera una clave temporal al arrancar, válida solo para desarrollo: los tokens dejan de ser válidos cada vez que se reinicia.

Para generar una clave propia:

```bash
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out jwt-private.pem
```

La tabla `users` se crea con Flyway (`src/main/resources/db/migration`).
