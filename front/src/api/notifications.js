import { request } from './http'

export function listNotifications() {
  return request('/notifications')
}

export function markNotificationRead(notificationId) {
  return request(`/notifications/${notificationId}/read`, { method: 'PATCH' })
}
