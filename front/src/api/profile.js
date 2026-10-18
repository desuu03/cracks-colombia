import { request } from './http'

export function getMyProfile() {
  return request('/users/me')
}

/** @param {object} changes UserProfileUpdate fields. */
export function updateMyProfile(changes) {
  return request('/users/me', { method: 'PUT', json: changes })
}

/** Public profile of an athlete (AthletePublicProfile). */
export function getAthleteProfile(athleteId) {
  return request(`/athletes/${athleteId}`)
}

/** A profile shows up in search once these are filled in. */
export function isProfileComplete(profile) {
  return Boolean(profile?.sport && profile?.position && profile?.region)
}
