import { request } from './http'

/** @param {{sport?, position?, minAge?, maxAge?, region?, page?, size?}} filters */
export function searchAthletes(filters) {
  const params = new URLSearchParams()
  for (const [key, value] of Object.entries(filters)) {
    if (value !== '' && value !== null && value !== undefined) params.set(key, value)
  }
  return request(`/athletes/search?${params}`)
}

export function markInterest(athleteId) {
  return request(`/athletes/${athleteId}/interest`, { method: 'POST' })
}
