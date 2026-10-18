# cracks-colombia
Proyecto académico para Software III. Plataforma de emparejamiento de entrenadores y prospectos y talentos jóvenes colombianos.

El backend (Spring Boot) vive en la raíz del repositorio y el frontend (Vue 3 + Vite) en [`front/`](front/README.md).

## HU-01: registro e inicio de sesión

El backend expone `POST /auth/register` y `POST /auth/login`. El registro público solo permite `DEPORTISTA` y `OJEADOR_ENTRENADOR`. Las contraseñas se almacenan con PBKDF2 y el login devuelve un token JWT con el rol del usuario.

Todos los registros deben enviar `dataProcessingConsent: true`. Para deportistas, `birthDate` es obligatorio. Si el servidor calcula que la persona tiene menos de 18 años, también exige `guardianConsent` con nombre, correo y `accepted: true`. La mayoría de edad se calcula en el servidor a partir de la fecha de nacimiento.

La búsqueda, el marcado de interés y la creación de solicitudes requieren `OJEADOR_ENTRENADOR`. La carga/eliminación de videos y la respuesta a solicitudes requieren `DEPORTISTA`. El token se envía en `Authorization: Bearer <token>`.

### Configuración local

- `DB_URL` (por defecto `jdbc:postgresql://localhost:5432/cracks_colombia`)
- `DB_USERNAME` (por defecto `postgres`)
- `DB_PASSWORD`
- `DB_DDL_AUTO` (por defecto `update`, para el esquema local de desarrollo)
- `JWT_SECRET_BASE64` (obligatorio; secreto aleatorio en Base64 de al menos 32 bytes)
- `JWT_EXPIRATION_MS` (por defecto `3600000`, una hora)

Las pruebas usan una base H2 aislada y un secreto de prueba; no requieren PostgreSQL ni `JWT_SECRET_BASE64`.

## HU-03: videos de habilidades

`POST /athletes/{athleteId}/videos` recibe el archivo en `multipart/form-data` bajo el campo `file`. El backend solo acepta MP4 con video H.264, verifica la estructura del contenedor, las muestras de video, la duracion real (maximo 60 segundos) y el tamano (maximo 50 MB). Los videos quedan guardados en el filesystem del servidor con un nombre aleatorio, nunca con el nombre enviado por el usuario. Antes de aceptar un video, valida que exista el consentimiento de tratamiento de datos y, si el deportista es menor, la autorizacion registrada de su acudiente.

`GET /users/me/videos` devuelve la galeria completa del deportista autenticado, incluidos los estados de moderacion. `GET /athletes/{athleteId}/videos` devuelve solo videos aprobados. La propiedad `url` de cada video apunta a `GET /videos/{videoId}/content`, que transmite el MP4 y permite al navegador reproducirlo; los videos pendientes o rechazados solo son visibles para el propietario y moderadores. El deportista puede eliminar sus archivos con `DELETE /videos/{videoId}`.

### Configuracion de videos

- `VIDEO_STORAGE_DIR` (por defecto `./data/videos`; fuera del classpath y excluido de Git)
- `VIDEO_MAX_FILE_SIZE` (por defecto `50MB`, limite HTTP de multipart)
- `VIDEO_MAX_REQUEST_SIZE` (por defecto `51MB`, incluye el overhead multipart)
- `VIDEO_MAX_FILE_SIZE_BYTES` (por defecto `50000000`)
- `VIDEO_MAX_DURATION_SECONDS` (por defecto `60`)

La carpeta de almacenamiento debe persistir entre reinicios y restringirse al usuario del proceso. Al desplegar en varias instancias, usa un volumen compartido o almacenamiento de objetos privado y adapta el servicio de almacenamiento para que todos los nodos lean los mismos archivos.

### Migracion desde los enlaces de video

Si una base PostgreSQL ya contiene la tabla `skill_videos` de la implementacion anterior, aplica una sola vez el script `src/main/resources/db/manual/rename-legacy-video-table.sql` antes de iniciar esta version. El script conserva la tabla anterior como archivo privado; esos enlaces no se publican ni se convierten en archivos automaticamente. Los deportistas deben volver a cargar los videos como MP4.
