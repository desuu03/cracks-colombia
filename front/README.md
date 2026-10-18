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

| Ruta | Quién | HU |
| --- | --- | --- |
| `/registro` | Público | HU-01: rol, fecha de nacimiento, autorización del acudiente para menores, consentimiento de datos |
| `/login` | Público | HU-01 |
| `/mis-videos` | `DEPORTISTA` | HU-03: carga MP4 con progreso, galería con estados de moderación, eliminación |
| `/deportistas/:athleteId/videos` | Público | HU-03: solo videos aprobados |

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
