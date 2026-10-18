import { computed, reactive } from 'vue'
import { listNotifications, markNotificationRead } from '../api/notifications'

// The backend has no push channel, so the bell polls while someone is logged in.
const POLL_INTERVAL_MS = 60_000

export const notifications = reactive({ items: [] })
export const unreadCount = computed(() => notifications.items.filter((n) => !n.read).length)

let timer = null

export async function refreshNotifications() {
  try {
    notifications.items = await listNotifications()
  } catch {
    // A failed poll keeps the last list; the next one retries.
  }
}

function onVisible() {
  if (document.visibilityState === 'visible') refreshNotifications()
}

export function startNotifications() {
  stopNotifications()
  refreshNotifications()
  timer = setInterval(refreshNotifications, POLL_INTERVAL_MS)
  document.addEventListener('visibilitychange', onVisible)
}

export function stopNotifications() {
  clearInterval(timer)
  timer = null
  document.removeEventListener('visibilitychange', onVisible)
  notifications.items = []
}

export async function markRead(notification) {
  if (notification.read) return
  notification.read = true
  try {
    await markNotificationRead(notification.id)
  } catch {
    notification.read = false
  }
}
