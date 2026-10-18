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

Contraseña de todos: `Cracks2026!` (cambiable con `SEED_PASSWORD`). El registro público no permite
MODERADOR ni ADMIN, así que el script los registra por la API y luego cambia el rol en la base de
datos. Con PostgreSQL usa `DB_KIND=postgres ./scripts/seed-dev-users.sh`. Para empezar de cero,
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

## API simulada

El backend aún no implementa perfil, búsqueda, interés, solicitudes ni notificaciones. Con
`VITE_USE_MOCKS=true` (activo por defecto en `npm run dev`, ver `.env.development`) esos endpoints
se responden en el navegador desde `src/mocks/server.js`, siguiendo el contrato OpenAPI. Registro,
login y videos siempre usan el backend real.

- Los datos simulados se guardan en `localStorage`. Si entras como ojeador y luego como deportista
  en el mismo navegador, cada uno ve las solicitudes y notificaciones del otro.
- Incluye 8 deportistas de ejemplo para la búsqueda.
- Para empezar de cero, borra la clave `cracks.mock.v1` del `localStorage` del navegador.
- Cuando el backend implemente un endpoint, quita su ruta de `src/mocks/server.js`. Cuando estén
  todos, pon `VITE_USE_MOCKS=false` y borra la carpeta `src/mocks/`.

### Lo que el backend debe agregar al contrato

La interfaz necesita estos campos, que el contrato actual no tiene (la simulación ya los devuelve):

- `MatchRequest`: `athleteName`, `athleteSummary`, `scoutName`, `scoutClub` y `contact`. Sin ellos la
  bandeja solo tiene identificadores y no hay endpoint para consultar el perfil de un ojeador.
- `AthletePublicProfile`: `interestedByMe` y `pendingRequestByMe`. Sin ellos, al recargar la página el
  ojeador no sabe si ya marcó interés o ya envió una solicitud.
- `PATCH /requests/{id}/respond` recibe `ACCEPTED`/`REJECTED` pero el estado es `ACEPTADA`/`RECHAZADA`.
  Conviene unificarlo.
- `PUT /users/me` acepta `birthDate`. El diseño aprobado la bloquea porque define si hace falta
  autorización del acudiente; el backend debería ignorarla o rechazarla.

## Notas de implementación

- El token JWT se guarda en `localStorage` y la sesión se cierra sola cuando expira (`exp`) o cuando
  la API responde 401.
- Antes de subir un video, el navegador valida extensión/tipo, tamaño (50 000 000 bytes) y duración
  (60 s). El backend sigue siendo quien valida el contenedor, el códec H.264 y la duración real.
- Los videos aprobados se reproducen directamente desde `GET /videos/{id}/content`. Los pendientes o
  rechazados requieren el token, que un `<video src>` no puede enviar, así que se descargan con
  `fetch` y se reproducen desde una URL `blob:` cuando el deportista pide la vista previa.
- La mayoría de edad se calcula en el cliente solo para mostrar la sección del acudiente; el backend
  la recalcula y tiene la última palabra.
