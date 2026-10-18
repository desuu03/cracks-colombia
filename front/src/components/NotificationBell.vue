<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { markRead, notifications, unreadCount } from '../stores/notifications'
import { formatRelative } from '../utils/format'

const open = ref(false)
const root = ref(null)

const ICONS = {
  PROFILE_VIEWED: 'M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12z M12 9a3 3 0 1 0 0 6a3 3 0 1 0 0-6',
  REQUEST_CREATED: 'M22 12h-6l-2 3h-4l-2-3H2 M5.45 5.11 2 12v6a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-6l-3.45-6.89A2 2 0 0 0 16.76 4H7.24a2 2 0 0 0-1.79 1.11z',
  REQUEST_ACCEPTED: 'M20 6 9 17l-5-5',
  REQUEST_REJECTED: 'M18 6 6 18M6 6l12 12',
}

function onDocumentClick(event) {
  if (open.value && !root.value?.contains(event.target)) open.value = false
}
function onKeydown(event) {
  if (event.key === 'Escape') open.value = false
}

onMounted(() => {
  document.addEventListener('click', onDocumentClick)
  document.addEventListener('keydown', onKeydown)
})
onBeforeUnmount(() => {
  document.removeEventListener('click', onDocumentClick)
  document.removeEventListener('keydown', onKeydown)
})
</script>

<template>
  <div ref="root" class="bell">
    <button
      type="button"
      class="bell-button"
      :aria-expanded="open"
      aria-controls="notif-panel"
      :aria-label="unreadCount ? `Notificaciones, ${unreadCount} sin leer` : 'Notificaciones'"
      @click="open = !open"
    >
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
        <path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9" />
        <path d="M10.3 21a1.94 1.94 0 0 0 3.4 0" />
      </svg>
      <span v-if="unreadCount" class="bell-count">{{ unreadCount }}</span>
    </button>

    <div v-if="open" id="notif-panel" class="notif-panel" role="dialog" aria-label="Notificaciones">
      <div class="notif-head">
        <h2>Notificaciones</h2>
        <span class="hint">{{ unreadCount ? `${unreadCount} sin leer` : 'Todo al día' }}</span>
      </div>
      <ul v-if="notifications.items.length" class="notif-list">
        <li v-for="item in notifications.items" :key="item.id">
          <button type="button" :class="['notif-item', { unread: !item.read }]" @click="markRead(item)">
            <span class="notif-icon">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <path :d="ICONS[item.type] ?? ICONS.REQUEST_CREATED" />
              </svg>
            </span>
            <span>
              {{ item.message }}<br />
              <span class="hint">{{ formatRelative(item.createdAt) }}</span>
            </span>
            <span v-if="!item.read" class="notif-dot" aria-label="Sin leer"></span>
          </button>
        </li>
      </ul>
      <p v-else class="empty" style="margin: 0">No tienes notificaciones todavía.</p>
      <div class="notif-foot">
        <RouterLink :to="{ name: 'requests' }" @click="open = false">Ir a solicitudes</RouterLink>
      </div>
    </div>
  </div>
</template>
