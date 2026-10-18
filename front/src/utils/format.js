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

export const REQUEST_STATUS_LABELS = {
  PENDIENTE: 'Pendiente',
  ACEPTADA: 'Aceptada',
  RECHAZADA: 'Rechazada',
}

const proposedDateFormat = new Intl.DateTimeFormat('es-CO', {
  weekday: 'short', day: 'numeric', month: 'short', hour: 'numeric', minute: '2-digit',
})

export function formatProposedDate(isoString) {
  return proposedDateFormat.format(new Date(isoString))
}

const relative = new Intl.RelativeTimeFormat('es-CO', { numeric: 'auto' })

export function formatRelative(isoString) {
  const minutes = Math.round((new Date(isoString).getTime() - Date.now()) / 60_000)
  if (Math.abs(minutes) < 60) return relative.format(minutes, 'minute')
  const hours = Math.round(minutes / 60)
  if (Math.abs(hours) < 24) return relative.format(hours, 'hour')
  return relative.format(Math.round(hours / 24), 'day')
}

export function initials(name) {
  return (name ?? '').split(/\s+/).filter(Boolean).slice(0, 2).map((part) => part[0]).join('').toUpperCase()
}

const birthDateFormat = new Intl.DateTimeFormat('es-CO', { dateStyle: 'long' })

/** YYYY-MM-DD as a long Spanish date, read at noon so no time zone shifts the day. */
export function formatBirthDate(isoDate) {
  return isoDate ? birthDateFormat.format(new Date(`${isoDate}T12:00:00`)) : ''
}
