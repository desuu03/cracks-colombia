export const ROLE_LABELS = {
  DEPORTISTA: 'Deportista',
  OJEADOR_ENTRENADOR: 'Ojeador / Entrenador',
  MODERADOR: 'Moderador',
  ADMIN: 'Administrador',
}

export const STATUS_LABELS = {
  PENDIENTE: 'Pendiente de revisión',
  APROBADO: 'Aprobado',
  RECHAZADO: 'Rechazado',
}

// Decimal units, matching the backend limit of 50 000 000 bytes.
export function formatBytes(bytes) {
  if (bytes >= 1_000_000) return `${(bytes / 1_000_000).toFixed(1)} MB`
  if (bytes >= 1_000) return `${Math.round(bytes / 1_000)} KB`
  return `${bytes} B`
}

export function formatDuration(seconds) {
  const total = Math.round(seconds)
  const minutes = Math.floor(total / 60)
  return `${minutes}:${String(total % 60).padStart(2, '0')}`
}

const dateFormat = new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium', timeStyle: 'short' })

export function formatDateTime(isoString) {
  return dateFormat.format(new Date(isoString))
}
