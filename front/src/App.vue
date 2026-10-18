<script setup>
import { computed, watch } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import NotificationBell from './components/NotificationBell.vue'
import { session, logout } from './stores/session'
import { startNotifications, stopNotifications } from './stores/notifications'
import { ROLE_LABELS, initials } from './utils/format'

const route = useRoute()
const router = useRouter()

const NAV = {
  DEPORTISTA: [
    { to: { name: 'my-profile' }, label: 'Mi perfil' },
    { to: { name: 'my-videos' }, label: 'Mis videos' },
    { to: { name: 'requests' }, label: 'Solicitudes' },
  ],
  OJEADOR_ENTRENADOR: [
    { to: { name: 'search' }, label: 'Buscar talentos' },
    { to: { name: 'requests' }, label: 'Solicitudes' },
  ],
}
const links = computed(() => NAV[session.user?.role] ?? [])
const hasInbox = computed(() => Boolean(NAV[session.user?.role]))

// The session can end without a navigation (token expiry, a 401 from the API);
// leave protected pages when that happens.
watch(
  () => session.token,
  (token) => {
    if (!token && route.meta.requiresAuth) {
      router.replace({ name: 'login', query: { redirect: route.fullPath, expirada: '1' } })
    }
  },
)

watch(
  () => (hasInbox.value ? session.token : null),
  (token) => (token ? startNotifications() : stopNotifications()),
  { immediate: true },
)

function signOut() {
  logout()
  router.push({ name: 'home' })
}
</script>

<template>
  <header class="topbar">
    <div class="topbar-inner">
      <RouterLink to="/" class="brand">
        <span class="brand-mark" aria-hidden="true"></span>
        Cracks Colombia
      </RouterLink>
      <nav v-if="links.length" class="nav" aria-label="Principal">
        <RouterLink v-for="link in links" :key="link.label" :to="link.to">{{ link.label }}</RouterLink>
      </nav>
      <div v-if="session.user" class="nav-actions">
        <NotificationBell v-if="hasInbox" />
        <span class="nav-user">
          <span class="avatar" aria-hidden="true">{{ initials(session.user.fullName) }}</span>
          <span>
            {{ session.user.fullName }}
            <small>{{ ROLE_LABELS[session.user.role] ?? session.user.role }}</small>
          </span>
        </span>
        <button type="button" class="btn btn-ghost btn-small" @click="signOut">Cerrar sesión</button>
      </div>
      <nav v-else class="nav" aria-label="Cuenta">
        <RouterLink :to="{ name: 'login' }">Iniciar sesión</RouterLink>
        <RouterLink :to="{ name: 'register' }" class="btn btn-primary">Registrarse</RouterLink>
      </nav>
    </div>
  </header>
  <RouterView v-slot="{ Component }">
    <component :is="Component" v-if="route.meta.fullBleed" />
    <main v-else class="page">
      <component :is="Component" />
    </main>
  </RouterView>
</template>
