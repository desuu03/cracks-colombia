import { session, logout } from '../stores/session'

const API_BASE = (import.meta.env.VITE_API_BASE_URL ?? '/api').replace(/\/$/, '')
const USE_MOCKS = import.meta.env.VITE_USE_MOCKS === 'true'

/** Error carrying the backend's ApiErrorResponse `code` and `message`. */
export class ApiError extends Error {
  constructor(status, code, message) {
    super(message)
    this.status = status
    this.code = code
  }
}

const FALLBACK_MESSAGES = {
  0: 'No se pudo conectar con el servidor. Verifica que el backend esté en ejecución.',
  401: 'Debes iniciar sesión para continuar.',
  403: 'Tu rol no tiene permiso para realizar esta acción.',
  404: 'No se encontró el recurso solicitado.',
  413: 'El archivo supera el tamaño máximo permitido (50 MB).',
}

export function apiUrl(path) {
  return API_BASE + path
}

function authHeaders() {
  return session.token ? { Authorization: `Bearer ${session.token}` } : {}
}

function toApiError(status, body) {
  // An expired or revoked token: drop the session so the UI sends the user to log in.
  if (status === 401 && session.token) logout()
  return new ApiError(
    status,
    body?.code ?? `HTTP_${status}`,
    body?.message ?? FALLBACK_MESSAGES[status] ?? 'Ocurrió un error inesperado. Intenta de nuevo.',
  )
}

async function send(path, init) {
  let response
  try {
    response = await fetch(apiUrl(path), { ...init, headers: { ...authHeaders(), ...init.headers } })
  } catch {
    throw new ApiError(0, 'NETWORK_ERROR', FALLBACK_MESSAGES[0])
  }
  if (!response.ok) {
    const body = await response.json().catch(() => null)
    throw toApiError(response.status, body)
  }
  return response
}

/** JSON request. Returns the parsed body, or null for 204 responses. */
export async function request(path, { method = 'GET', json } = {}) {
  if (USE_MOCKS) {
    // Loaded lazily so production builds without mocks never execute it.
    const { mockRequest } = await import('../mocks/server.js')
    const mocked = await mockRequest(method, path, json)
    if (mocked !== undefined) return mocked
  }
  const init = { method, headers: {} }
  if (json !== undefined) {
    init.headers['Content-Type'] = 'application/json'
    init.body = JSON.stringify(json)
  }
  const response = await send(path, init)
  return response.status === 204 ? null : response.json()
}

/** Authenticated binary download, for content a plain <video src> cannot authorize. */
export async function requestBlob(path) {
  const response = await send(path, { method: 'GET', headers: {} })
  return response.blob()
}

/**
 * Multipart upload through XHR, since fetch exposes no upload progress.
 * `onProgress` receives a fraction between 0 and 1.
 */
export function uploadFile(path, fieldName, file, onProgress) {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest()
    xhr.open('POST', apiUrl(path))
    xhr.responseType = 'json'
    for (const [name, value] of Object.entries(authHeaders())) xhr.setRequestHeader(name, value)
    xhr.upload.onprogress = (event) => {
      if (event.lengthComputable) onProgress?.(event.loaded / event.total)
    }
    xhr.onload = () => {
      if (xhr.status >= 200 && xhr.status < 300) resolve(xhr.response)
      else reject(toApiError(xhr.status, xhr.response))
    }
    xhr.onerror = () => reject(new ApiError(0, 'NETWORK_ERROR', FALLBACK_MESSAGES[0]))
    const form = new FormData()
    form.append(fieldName, file)
    xhr.send(form)
  })
}
