import { request } from './http'

export function login(email, password) {
  return request('/auth/login', { method: 'POST', json: { email, password } })
}

/** @param {object} payload RegisterRequest as defined by the backend. */
export function register(payload) {
  return request('/auth/register', { method: 'POST', json: payload })
}
