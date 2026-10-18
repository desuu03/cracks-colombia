import { reactive } from 'vue'

const STORAGE_KEY = 'cracks.session'
const MAX_TIMEOUT_MS = 2 ** 31 - 1

export const session = reactive({
  token: null,
  /** @type {{id: string, email: string, fullName: string, role: string, birthDate: string|null} | null} */
  user: null,
})

let expiryTimer = null

// Reads `exp` from the JWT payload. The signature is the backend's concern;
// the client only needs to know when to stop sending the token.
function tokenExpiresAt(token) {
  try {
    const payload = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')
    const { exp } = JSON.parse(atob(payload))
    return typeof exp === 'number' ? exp * 1000 : null
  } catch {
    return null
  }
}

function scheduleExpiry(token) {
  clearTimeout(expiryTimer)
  const expiresAt = tokenExpiresAt(token)
  if (expiresAt === null) return
  const delay = expiresAt - Date.now()
  if (delay < MAX_TIMEOUT_MS) expiryTimer = setTimeout(logout, Math.max(delay, 0))
}

function persist() {
  try {
    if (session.token) {
      localStorage.setItem(STORAGE_KEY, JSON.stringify({ token: session.token, user: session.user }))
    } else {
      localStorage.removeItem(STORAGE_KEY)
    }
  } catch {
    // Storage may be unavailable (private mode); the session still lives in memory.
  }
}

/** Starts a session from an AuthResponse returned by /auth/login or /auth/register. */
export function startSession({ accessToken, user }) {
  session.token = accessToken
  session.user = user
  persist()
  scheduleExpiry(accessToken)
}

export function logout() {
  clearTimeout(expiryTimer)
  session.token = null
  session.user = null
  persist()
}

export function restoreSession() {
  let stored = null
  try {
    stored = JSON.parse(localStorage.getItem(STORAGE_KEY) ?? 'null')
  } catch {
    stored = null
  }
  if (!stored?.token || !stored.user) return
  const expiresAt = tokenExpiresAt(stored.token)
  if (expiresAt !== null && expiresAt <= Date.now()) {
    logout()
    return
  }
  startSession({ accessToken: stored.token, user: stored.user })
}

export function hasRole(role) {
  return session.user?.role === role
}
