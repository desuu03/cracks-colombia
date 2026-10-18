// In-browser stand-in for the endpoints the backend has not implemented yet:
// profile, search, interest, match requests and notifications. Shapes follow
// src/main/resources/static/cracks-colombia-openapi.yaml. Fields marked
// "extension" are not in the contract yet; the UI needs them and the backend
// should add them (see front/README.md).
//
// State lives in localStorage, so a scout and an athlete logging in one after
// the other in the same browser see each other's requests and notifications.
// Auth and videos are never mocked: mockRequest returns undefined for them.
import { ApiError } from '../api/http'
import { session } from '../stores/session'
import { ageOn } from '../utils/age'

const STORAGE_KEY = 'cracks.mock.v1'
const LATENCY_MS = 250

const EXAMPLE_ATHLETES = [
  ['5f0c2a1e-0001-4c1a-9a00-000000000001', 'Valentina Mosquera', 'Atletismo', 'Velocidad 100 m', 'Chocó', '2009-02-11', 168, 56,
    'Campeona departamental de 100 m en 2025 con 12,1 s. Entrena en la pista de Quibdó con la liga del Chocó.'],
  ['5f0c2a1e-0001-4c1a-9a00-000000000002', 'Kevin Hurtado', 'Fútbol', 'Volante', 'Valle del Cauca', '2008-06-02', 172, 66,
    'Volante de marca en la liga vallecaucana Sub-18. Capitán de su colegio en los Juegos Intercolegiados 2025.'],
  ['5f0c2a1e-0001-4c1a-9a00-000000000003', 'Daniela Pantoja', 'Voleibol', 'Armador', 'Nariño', '2007-09-30', 181, 68,
    'Armadora de la selección Nariño en los Juegos Nacionales Juveniles 2025.'],
  ['5f0c2a1e-0001-4c1a-9a00-000000000004', 'Samuel Arrieta', 'Baloncesto', 'Base', 'Bolívar', '2009-04-18', 185, 74,
    'Base titular del torneo departamental de Bolívar. Promedia 14 puntos y 6 asistencias por partido.'],
  ['5f0c2a1e-0001-4c1a-9a00-000000000005', 'Mariana Quintero', 'Fútbol', 'Defensa central', 'Antioquia', '2011-01-23', 170, 60,
    'Defensa central de la escuela de fútbol femenino de Bello. Subcampeona del torneo Sub-15 de Antioquia.'],
  ['5f0c2a1e-0001-4c1a-9a00-000000000006', 'Brayan Mena', 'Fútbol', 'Portero', 'Risaralda', '2006-11-05', 188, 80,
    'Portero de la liga de Risaralda. Valla menos vencida del torneo departamental Sub-20 de 2025.'],
  ['5f0c2a1e-0001-4c1a-9a00-000000000007', 'Laura Sogamoso', 'Atletismo', 'Fondo 5.000 m', 'Boyacá', '2008-08-14', 162, 50,
    'Fondista de Duitama. Segundo lugar en los 5.000 m del Nacional Juvenil 2025.'],
  ['5f0c2a1e-0001-4c1a-9a00-000000000008', 'Juan Camilo Ríos', 'Fútbol', 'Delantero', 'Quindío', '2010-03-14', 178, 70,
    'Goleador de la Liga Quindiana Sub-17 en 2025 con 14 goles. Convocado a la selección departamental.'],
].map(([id, fullName, sport, position, region, birthDate, heightCm, weightKg, trajectory]) => ({
  id, email: `${fullName.split(' ')[0].toLowerCase()}@ejemplo.co`, fullName, role: 'DEPORTISTA',
  sport, position, region, birthDate, heightCm, weightKg, trajectory, example: true,
}))

function initialState() {
  return {
    users: Object.fromEntries(EXAMPLE_ATHLETES.map((a) => [a.id, a])),
    interests: [], // { scoutId, athleteId }
    requests: [],
    notifications: [],
    profileViews: {}, // "scoutId:athleteId" -> YYYY-MM-DD of the last PROFILE_VIEWED
  }
}

function load() {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY)) ?? initialState()
  } catch {
    return initialState()
  }
}

function save(db) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(db))
  } catch {
    // Storage full or blocked: the mock keeps working for this page load only.
  }
}

/** Wipes mock data back to the example athletes. */
export function resetMockData() {
  save(initialState())
}

const fail = (status, code, message) => {
  throw new ApiError(status, code, message)
}

function currentUser(db) {
  const user = session.user
  if (!session.token || !user) fail(401, 'UNAUTHORIZED', 'Debes iniciar sesión para acceder a este recurso.')
  // Register the real user (from the JWT session) so others can see their name.
  db.users[user.id] = { ...db.users[user.id], id: user.id, email: user.email, fullName: user.fullName, role: user.role, birthDate: user.birthDate ?? db.users[user.id]?.birthDate ?? null }
  return db.users[user.id]
}

function requireRole(user, role) {
  if (user.role !== role) fail(403, 'FORBIDDEN', 'Tu rol no tiene permiso para acceder a este recurso.')
}

const isComplete = (u) => Boolean(u.sport && u.position && u.region)
const today = () => new Date().toISOString().slice(0, 10)

function profileOf(u) {
  const { example, ...profile } = u
  return profile
}

function notify(db, userId, type, message) {
  db.notifications.push({ id: crypto.randomUUID(), userId, type, message, read: false, createdAt: new Date().toISOString() })
}

function contactLine(u) {
  return [u.contactPhone, u.email].filter(Boolean).join(' · ')
}

function requestView(db, r, viewer) {
  const athlete = db.users[r.athleteId] ?? {}
  const scout = db.users[r.scoutId] ?? {}
  const accepted = r.status === 'ACEPTADA'
  return {
    ...r,
    athleteName: athlete.fullName ?? 'Deportista', // extension
    athleteSummary: [athlete.sport, athlete.position, athlete.region].filter(Boolean).join(' · '), // extension
    scoutName: scout.fullName ?? 'Ojeador', // extension
    scoutClub: scout.club ?? null, // extension
    // extension: what the other party sees once the request is accepted
    contact: accepted ? contactLine(viewer.id === r.athleteId ? scout : athlete) : null,
  }
}

const routes = [
  ['GET', /^\/users\/me$/, (db) => profileOf(currentUser(db))],

  ['PUT', /^\/users\/me$/, (db, _m, body) => {
    const user = currentUser(db)
    const allowed = user.role === 'DEPORTISTA'
      ? ['sport', 'position', 'region', 'heightCm', 'weightKg', 'trajectory']
      : ['club', 'contactPhone']
    // birthDate is in UserProfileUpdate, but the approved design locks it: it
    // decides whether guardian consent is required.
    for (const key of allowed) if (key in body) user[key] = body[key] === '' ? null : body[key]
    return profileOf(user)
  }],

  ['GET', /^\/athletes\/search$/, (db, _m, _b, query) => {
    requireRole(currentUser(db), 'OJEADOR_ENTRENADOR')
    const minAge = query.get('minAge') ? Number(query.get('minAge')) : null
    const maxAge = query.get('maxAge') ? Number(query.get('maxAge')) : null
    const page = Number(query.get('page') ?? 0)
    const size = Number(query.get('size') ?? 20)
    const matches = Object.values(db.users).filter((u) => {
      if (u.role !== 'DEPORTISTA' || !isComplete(u)) return false
      const age = u.birthDate ? ageOn(u.birthDate) : null
      return (!query.get('sport') || u.sport === query.get('sport'))
        && (!query.get('position') || u.position === query.get('position'))
        && (!query.get('region') || u.region === query.get('region'))
        && (minAge === null || (age !== null && age >= minAge))
        && (maxAge === null || (age !== null && age <= maxAge))
    }).sort((a, b) => a.fullName.localeCompare(b.fullName, 'es'))
    return {
      items: matches.slice(page * size, (page + 1) * size).map((u) => ({ ...profileOf(u), videos: [] })),
      page,
      totalPages: Math.max(1, Math.ceil(matches.length / size)),
      totalElements: matches.length,
    }
  }],

  ['POST', /^\/athletes\/([^/]+)\/interest$/, (db, [, athleteId]) => {
    const scout = currentUser(db)
    requireRole(scout, 'OJEADOR_ENTRENADOR')
    if (db.users[athleteId]?.role !== 'DEPORTISTA') fail(404, 'NOT_FOUND', 'No se encontró el perfil del deportista.')
    if (!db.interests.some((i) => i.scoutId === scout.id && i.athleteId === athleteId)) {
      db.interests.push({ scoutId: scout.id, athleteId })
    }
    return null
  }],

  ['GET', /^\/athletes\/([^/]+)$/, (db, [, athleteId]) => {
    // Register the viewer first, so athletes can open their own page before saving a profile.
    const viewer = session.token ? currentUser(db) : null
    const athlete = db.users[athleteId]
    if (athlete?.role !== 'DEPORTISTA') fail(404, 'NOT_FOUND', 'No se encontró el perfil del deportista.')
    const isScout = viewer?.role === 'OJEADOR_ENTRENADOR'
    if (isScout) {
      const key = `${viewer.id}:${athleteId}`
      if (db.profileViews[key] !== today()) {
        db.profileViews[key] = today()
        notify(db, athleteId, 'PROFILE_VIEWED', `${viewer.club ?? viewer.fullName} vio tu perfil.`)
      }
    }
    return {
      ...profileOf(athlete),
      videos: [],
      // extensions: the API has no way to read these back after a reload
      interestedByMe: isScout && db.interests.some((i) => i.scoutId === viewer.id && i.athleteId === athleteId),
      pendingRequestByMe: isScout && db.requests.some((r) => r.scoutId === viewer.id && r.athleteId === athleteId && r.status === 'PENDIENTE'),
    }
  }],

  ['GET', /^\/requests$/, (db, _m, _b, query) => {
    const user = currentUser(db)
    const status = query.get('status')
    return db.requests
      .filter((r) => (user.role === 'DEPORTISTA' ? r.athleteId : r.scoutId) === user.id)
      .filter((r) => !status || r.status === status)
      .sort((a, b) => b.createdAt.localeCompare(a.createdAt))
      .map((r) => requestView(db, r, user))
  }],

  ['POST', /^\/requests$/, (db, _m, body) => {
    const scout = currentUser(db)
    requireRole(scout, 'OJEADOR_ENTRENADOR')
    const athlete = db.users[body?.athleteId]
    if (athlete?.role !== 'DEPORTISTA') fail(404, 'NOT_FOUND', 'No se encontró el perfil del deportista.')
    if (!body.message?.trim()) fail(422, 'INVALID_REQUEST', 'El mensaje es obligatorio.')
    if (!db.interests.some((i) => i.scoutId === scout.id && i.athleteId === athlete.id)) {
      fail(422, 'INTEREST_REQUIRED', 'Marca al deportista como prospecto de interés antes de enviar la solicitud.')
    }
    if (db.requests.some((r) => r.scoutId === scout.id && r.athleteId === athlete.id && r.status === 'PENDIENTE')) {
      fail(409, 'REQUEST_ALREADY_PENDING', 'Ya tienes una solicitud pendiente con este deportista.')
    }
    const request = {
      id: crypto.randomUUID(), athleteId: athlete.id, scoutId: scout.id, message: body.message.trim(),
      proposedDate: body.proposedDate ?? null, status: 'PENDIENTE', contactSharedAt: null, createdAt: new Date().toISOString(),
    }
    db.requests.push(request)
    notify(db, athlete.id, 'REQUEST_CREATED', `${scout.club ?? scout.fullName} te envió una solicitud de contacto.`)
    return requestView(db, request, scout)
  }],

  ['PATCH', /^\/requests\/([^/]+)\/respond$/, (db, [, requestId], body) => {
    const athlete = currentUser(db)
    requireRole(athlete, 'DEPORTISTA')
    const request = db.requests.find((r) => r.id === requestId)
    if (!request) fail(404, 'NOT_FOUND', 'No se encontró la solicitud.')
    if (request.athleteId !== athlete.id) fail(403, 'FORBIDDEN', 'No eres el destinatario de esta solicitud.')
    if (request.status !== 'PENDIENTE') fail(409, 'REQUEST_ALREADY_ANSWERED', 'Esta solicitud ya fue respondida.')
    // The contract takes ACCEPTED/REJECTED but reports ACEPTADA/RECHAZADA.
    if (body?.decision === 'ACCEPTED') {
      request.status = 'ACEPTADA'
      request.contactSharedAt = new Date().toISOString()
      notify(db, request.scoutId, 'REQUEST_ACCEPTED', `${athlete.fullName} aceptó tu solicitud. Ya puedes ver sus datos de contacto.`)
    } else if (body?.decision === 'REJECTED') {
      request.status = 'RECHAZADA'
      notify(db, request.scoutId, 'REQUEST_REJECTED', `${athlete.fullName} rechazó tu solicitud.`)
    } else {
      fail(422, 'INVALID_REQUEST', 'La decisión debe ser ACCEPTED o REJECTED.')
    }
    return requestView(db, request, athlete)
  }],

  ['GET', /^\/notifications$/, (db, _m, _b, query) => {
    const user = currentUser(db)
    const unreadOnly = query.get('unreadOnly') === 'true'
    return db.notifications
      .filter((n) => n.userId === user.id && (!unreadOnly || !n.read))
      .sort((a, b) => b.createdAt.localeCompare(a.createdAt))
      .map(({ userId, ...n }) => n)
  }],

  ['PATCH', /^\/notifications\/([^/]+)\/read$/, (db, [, notificationId]) => {
    const user = currentUser(db)
    const notification = db.notifications.find((n) => n.id === notificationId && n.userId === user.id)
    if (!notification) fail(404, 'NOT_FOUND', 'No se encontró la notificación.')
    notification.read = true
    return null
  }],
]

/** Resolves the mocked response, or undefined when the real backend handles the path. */
export async function mockRequest(method, path, body) {
  const url = new URL(path, 'http://mock.local')
  for (const [routeMethod, pattern, handler] of routes) {
    const match = method === routeMethod && url.pathname.match(pattern)
    if (!match) continue
    await new Promise((resolve) => setTimeout(resolve, LATENCY_MS))
    const db = load()
    const result = handler(db, match, body ?? {}, url.searchParams)
    save(db)
    // Detach from the stored object, like a real JSON response.
    return result === null ? null : JSON.parse(JSON.stringify(result))
  }
  return undefined
}
