# cracks-colombia
Proyecto académico para Software III. Plataforma de emparejamiento de entrenadores y prospectos y talentos jóvenes colombianos.

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
