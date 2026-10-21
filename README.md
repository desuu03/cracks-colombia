# cracks-colombia-api

API REST de **Cracks Colombia**, proyecto académico de Software III (Universidad del Quindío). Conecta deportistas jóvenes con ojeadores y entrenadores.

Este repositorio contiene **solo el backend**. El frontend vive en un repositorio aparte.

- Java 21
- Spring Boot 4.1.1 (Web, Security, JPA, Validation)
- PostgreSQL 16 (desarrollo local con Docker)
- JWT + contraseñas PBKDF2
- OpenAPI / Swagger

## Requisitos

- Java 21
- Maven Wrapper (`mvnw` / `mvnw.cmd`)
- Docker Desktop (para PostgreSQL local)

## Arranque local

### 1. Base de datos

```powershell
docker compose up -d
```

Credenciales de desarrollo:

| Dato | Valor |
| --- | --- |
| Host | `localhost` |
| Puerto | `5432` |
| Base | `cracks_colombia` |
| Usuario | `postgres` |
| Contraseña | `postgres` |

Hibernate crea y actualiza las tablas al arrancar la API (`ddl-auto=update`).

### 2. API

En PowerShell, desde la raíz del repo:

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/cracks_colombia"
$env:DB_USERNAME = "postgres"
$env:DB_PASSWORD = "postgres"
$env:JWT_SECRET_BASE64 = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
.\mvnw.cmd spring-boot:run
```

La API queda en `http://localhost:8080`.

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI: `http://localhost:8080/v3/api-docs`
- Contrato: `src/main/resources/static/cracks-colombia-openapi.yaml`

`JWT_SECRET_BASE64` es obligatorio (mínimo 32 bytes en Base64). El valor de arriba sirve solo para desarrollo.

### 3. Frontend (otro repositorio)

El navegador llama a esta API desde otro origen. CORS permite por defecto `http://localhost:5173`. Cámbialo con `CORS_ALLOWED_ORIGINS` (orígenes separados por coma).

El token JWT se envía en `Authorization: Bearer <token>`.

## Variables de entorno

| Variable | Por defecto | Uso |
| --- | --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/cracks_colombia` | JDBC |
| `DB_USERNAME` | `postgres` | Usuario de PostgreSQL |
| `DB_PASSWORD` | *(vacío)* | Obligatorio con Docker (`postgres`) |
| `DB_DDL_AUTO` | `update` | Esquema local |
| `JWT_SECRET_BASE64` | — | Obligatorio |
| `JWT_EXPIRATION_MS` | `3600000` | 1 hora |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | Orígenes del frontend |
| `PROFILE_AUTO_APPROVE` | `true` | Un perfil completo queda `APROBADO` al guardarlo |
| `VIDEO_STORAGE_DIR` | `./data/videos` | Archivos MP4 |
| `VIDEO_MAX_FILE_SIZE` | `50MB` | Límite HTTP multipart |
| `VIDEO_MAX_REQUEST_SIZE` | `51MB` | Incluye overhead multipart |
| `VIDEO_MAX_FILE_SIZE_BYTES` | `50000000` | Validación de negocio |
| `VIDEO_MAX_DURATION_SECONDS` | `60` | Duración máxima |
| `PHOTO_STORAGE_DIR` | `./data/photos` | Fotos de perfil |
| `PHOTO_MAX_FILE_SIZE_BYTES` | `5000000` | 5 MB |

Videos y fotos se guardan en disco (no en la base) y están fuera de Git.

## Consultar la base

```powershell
docker exec -it cracks-colombia-postgres psql -U postgres -d cracks_colombia
```

Comandos útiles dentro de `psql`:

```sql
\dt
\d user_accounts
SELECT * FROM user_accounts;
SELECT * FROM athlete_profiles;
SELECT * FROM skill_videos;
```

Salir de `psql`: `\q`

Tablas actuales:

- `user_accounts`
- `athlete_profiles`
- `athlete_profile_details`
- `scout_profiles`
- `skill_videos`
- `scout_interests`
- `match_requests`
- `notifications`
- `profile_views`

## Roles

| Rol | Registro público | Uso |
| --- | --- | --- |
| `DEPORTISTA` | Sí | Perfil, videos, foto, responder solicitudes |
| `OJEADOR_ENTRENADOR` | Sí | Búsqueda, interés, crear solicitudes |
| `MODERADOR` | No | `/moderation/**` (reservado) |
| `ADMIN` | No | Reservado |

## Endpoints

### Auth (HU-01)

- `POST /auth/register` — registro. Solo `DEPORTISTA` y `OJEADOR_ENTRENADOR`. Exige `dataProcessingConsent: true`. El deportista debe enviar `birthDate`. Si es menor de 18 (lo calcula el servidor), exige `guardianConsent`.
- `POST /auth/login` — JWT con el rol.

### Perfiles (HU-02)

- `GET /users/me` — perfil autenticado
- `PUT /users/me` — editar perfil. No cambia `birthDate`. Deportista: `sport`, `position`, `region`, `heightCm`, `weightKg`, `trajectory`. Ojeador: `club`, `contactPhone`.
- `GET /athletes/{athleteId}` — perfil público (edad, no fecha de nacimiento ni correo)

Catálogo cerrado: Fútbol, Baloncesto, Voleibol, Atletismo, con posiciones fijas y departamentos de Colombia.

### Videos (HU-03)

- `POST /athletes/{athleteId}/videos` — multipart campo `file`. MP4, máx. 50 MB y 60 s. Queda `PENDIENTE`.
- `GET /users/me/videos` — galería propia (todos los estados)
- `GET /athletes/{athleteId}/videos` — solo `APROBADO`
- `GET /videos/{videoId}/content` — reproduce el MP4
- `DELETE /videos/{videoId}` — elimina el archivo propio

### Fotos

- `PUT /athletes/{athleteId}/photo` — foto de perfil (propietario)
- `GET /athletes/{athleteId}/photo` — imagen

### Búsqueda (HU-04)

- `GET /athletes/search?sport=&position=&minAge=&maxAge=&region=&page=&size=` — ojeador/entrenador, perfiles aprobados, paginado
- `POST /athletes/{athleteId}/interest` — marca de interés

### Solicitudes / Match (HU-05)

- `GET /requests?status=` — enviadas o recibidas
- `POST /requests` — crear (`athleteId`, `message`, `proposedDate` opcional)
- `PATCH /requests/{requestId}/respond` — `ACCEPTED` o `REJECTED`. Si se acepta, se intercambian datos de contacto.

### Notificaciones (HU-06)

- `GET /notifications?unreadOnly=`
- `PATCH /notifications/{notificationId}/read`

Tipos: `PROFILE_VIEWED`, `REQUEST_CREATED`, `REQUEST_ACCEPTED`, `REQUEST_REJECTED`. Máximo una notificación de vista de perfil por ojeador, deportista y día.

### Moderación (HU-09)

Rutas `/moderation/**` reservadas al rol `MODERADOR`. Aún no hay implementación completa de cola/aprobar/rechazar.

## Pruebas

Usan H2 en memoria. No necesitan PostgreSQL ni `JWT_SECRET_BASE64`.

```powershell
.\mvnw.cmd test
```

- `AuthIntegrationTest`
- `UserProfileIntegrationTest`
- `VideoIntegrationTest`
- `PhotoIntegrationTest`
- `AthleteSearchIntegrationTest`
- `MatchRequestIntegrationTest`

## Estructura

```
src/main/java/.../cracks_colombia_api/
  auth/            registro e inicio de sesión
  user/            cuentas y perfiles
  video/           carga y reproducción de MP4
  photo/           foto de perfil
  search/          filtros y paginación
  match/           interés y solicitudes
  notification/    bandeja de avisos
  shared/          JWT, CORS, errores
```
