import { request } from './http'

export function listRequests() {
  return request('/requests')
}

export function createRequest({ athleteId, message, proposedDate }) {
  return request('/requests', { method: 'POST', json: { athleteId, message, proposedDate: proposedDate ?? null } })
}

/** @param {'ACCEPTED'|'REJECTED'} decision */
export function respondToRequest(requestId, decision) {
  return request(`/requests/${requestId}/respond`, { method: 'PATCH', json: { decision } })
}
