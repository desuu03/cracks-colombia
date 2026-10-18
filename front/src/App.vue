<script setup>
import { watch } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import { session, logout } from './stores/session'
import { ROLE_LABELS } from './utils/format'

const route = useRoute()
const router = useRouter()

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
      <nav class="nav">
        <template v-if="session.user">
          <RouterLink v-if="session.user.role === 'DEPORTISTA'" :to="{ name: 'my-videos' }">
            Mis videos
          </RouterLink>
          <span class="nav-user">
            {{ session.user.fullName }}
            <small>{{ ROLE_LABELS[session.user.role] ?? session.user.role }}</small>
          </span>
          <button type="button" class="btn btn-ghost" @click="signOut">Cerrar sesión</button>
        </template>
        <template v-else>
          <RouterLink :to="{ name: 'login' }">Iniciar sesión</RouterLink>
          <RouterLink :to="{ name: 'register' }" class="btn btn-primary">Registrarse</RouterLink>
        </template>
      </nav>
    </div>
  </header>
  <main class="page">
    <RouterView />
  </main>
</template>
