# Cracks Colombia — frontend

Frontend de Cracks Colombia (Vue 3 + Vite). Consume la API del backend que vive en la raíz de este repositorio.

## Requisitos

- Node 20 o superior
- El backend ejecutándose (por defecto en `http://localhost:8080`)

## Desarrollo

```bash
cd front
npm install
cp .env.example .env   # opcional
npm run dev            # http://localhost:5173
```

### Backend local sin PostgreSQL y usuarios de prueba

```bash
./scripts/dev-backend.sh        # backend con H2 en archivo (.dev-data/), los datos persisten
./scripts/seed-dev-users.sh     # en otra terminal, con el backend arriba; se puede repetir
```

| Correo | Rol |
| --- | --- |
| `deportista@cracks.test` | DEPORTISTA (mayor de edad) |
| `deportista.menor@cracks.test` | DEPORTISTA (menor, con acudiente) |
| `entrenador@cracks.test` | OJEADOR_ENTRENADOR |
| `moderador@cracks.test` | MODERADOR |
| `admin@cracks.test` | ADMIN |
| `kevin@`, `daniela@`, `samuel@`, `brayan@cracks.test` | DEPORTISTA, con perfil para la búsqueda |

Contraseña de todos: `Cracks2026!` (cambiable con `SEED_PASSWORD`). El registro público no permite
MODERADOR ni ADMIN, así que el script los registra por la API y luego cambia el rol en la base de
datos. También llena por la API los perfiles de los deportistas y el club del entrenador. Con PostgreSQL usa `DB_KIND=postgres ./scripts/seed-dev-users.sh`. Para empezar de cero,
detén el backend y borra `.dev-data/`.

### Proxy

El backend no tiene CORS configurado, así que el navegador llama a `/api/*` en el servidor de Vite y
este reenvía la petición al backend quitando el prefijo `/api`. El destino se cambia con `API_PROXY_TARGET`.

Para producción hay que habilitar CORS en el backend (o servir ambos detrás del mismo dominio) y
definir `VITE_API_BASE_URL` antes de `npm run build`.

## Pantallas

Diseño aprobado por el equipo en el prototipo de revisión (8 pantallas).

| Ruta | Quién | Pantalla |
| --- | --- | --- |
| `/registro` | Público | Registro con rol, fecha de nacimiento, autorización del acudiente y consentimiento (HU-01) |
| `/login` | Público | Inicio de sesión (HU-01). El deportista sin perfil va a `/perfil/crear` |
| `/perfil/crear` | `DEPORTISTA` | Crear perfil en tres pasos: deporte y posición, datos físicos, trayectoria |
| `/perfil` | `DEPORTISTA` | Editar perfil, con vista previa de cómo lo ven los ojeadores |
| `/deportistas/:athleteId` | Público | Vista pública del perfil; el ojeador marca interés y solicita contacto |
| `/buscar` | `OJEADOR_ENTRENADOR` | Búsqueda con filtros (deporte, posición, edad, departamento) y resultados paginados |
| `/solicitudes` | Ambos | Bandeja: el deportista acepta o rechaza; el ojeador sigue el estado de las enviadas |
| Campana (barra superior) | Ambos | Panel de notificaciones; se consulta cada 60 s |
| `/mis-videos` | `DEPORTISTA` | Carga y galería de videos (HU-03) |

## Backend

Todas las pantallas usan el backend real. Los endpoints de perfil, perfil público, interés,
solicitudes y notificaciones están en los paquetes `user`, `match` y `notification` del backend, con
sus pruebas en `UserProfileIntegrationTest` y `MatchRequestIntegrationTest`. Decisiones tomadas:

- Un perfil de deportista completo (deporte, posición y departamento) queda `APROBADO` al guardarlo,
  para que aparezca en la búsqueda. Con `app.profile.auto-approve=false` queda `PENDIENTE`.
- `PUT /users/me` no cambia la fecha de nacimiento: define si hace falta autorización del acudiente.
- El perfil público devuelve la edad, no la fecha de nacimiento ni el correo.
- Al aceptar una solicitud, el ojeador ve el correo del deportista o, si es menor, el del acudiente.
  El deportista ve el club, el teléfono y el correo del ojeador.
- Un ojeador genera como máximo una notificación «vio tu perfil» por deportista y día.

## Notas de implementación

- El token JWT se guarda en `localStorage` y la sesión se cierra sola cuando expira (`exp`) o cuando
  la API responde 401.
- Antes de subir un video, el navegador valida extensión/tipo, tamaño (50 000 000 bytes) y duración
  (60 s). El backend sigue siendo quien valida el contenedor, la pista de video y la duración real.
- Los videos aprobados se reproducen directamente desde `GET /videos/{id}/content`. Los pendientes o
  rechazados requieren el token, que un `<video src>` no puede enviar, así que se descargan con
  `fetch` y se reproducen desde una URL `blob:` cuando el deportista pide la vista previa.
- La mayoría de edad se calcula en el cliente solo para mostrar la sección del acudiente; el backend
  la recalcula y tiene la última palabra.
