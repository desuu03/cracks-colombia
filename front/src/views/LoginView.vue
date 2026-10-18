<script setup>
import { ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import AuthLayout from '../components/AuthLayout.vue'
import { login } from '../api/auth'
import { getMyProfile, isProfileComplete } from '../api/profile'
import { startSession } from '../stores/session'
import { homeFor, safeRedirect } from '../router'

const route = useRoute()
const router = useRouter()

const email = ref('')
const password = ref('')
const error = ref('')
const submitting = ref(false)

async function landingFor(user) {
  if (user.role !== 'DEPORTISTA') return homeFor(user.role)
  // Athletes without a sport profile finish it before anything else.
  try {
    return isProfileComplete(await getMyProfile()) ? homeFor(user.role) : { name: 'profile-create' }
  } catch {
    return homeFor(user.role)
  }
}

async function submit() {
  error.value = ''
  submitting.value = true
  try {
    const auth = await login(email.value.trim(), password.value)
    startSession(auth)
    router.replace(safeRedirect(route.query.redirect) ?? (await landingFor(auth.user)))
  } catch (e) {
    error.value = e.message
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <AuthLayout text="Revisa quién vio tu perfil y responde las solicitudes de los clubes.">
    <template #headline>Bienvenido de <em>vuelta.</em></template>
    <form class="stack" novalidate @submit.prevent="submit">
      <div>
        <h2>Iniciar sesión</h2>
        <p class="muted" style="margin: 0">Ingresa con el correo y la contraseña de tu cuenta.</p>
      </div>
      <p v-if="route.query.expirada" class="alert alert-info">Tu sesión terminó. Inicia sesión de nuevo para continuar.</p>
      <p v-if="error" class="alert alert-error" role="alert">{{ error }}</p>

      <div class="field">
        <label for="login-email">Correo electrónico</label>
        <input id="login-email" v-model="email" type="email" autocomplete="email" maxlength="254" required />
      </div>
      <div class="field">
        <label for="login-password">Contraseña</label>
        <input id="login-password" v-model="password" type="password" autocomplete="current-password" required />
      </div>

      <button class="btn btn-primary btn-block" type="submit" :disabled="submitting || !email || !password">
        {{ submitting ? 'Ingresando…' : 'Ingresar' }}
      </button>
      <p class="muted">¿No tienes cuenta? <RouterLink :to="{ name: 'register' }">Regístrate</RouterLink></p>
    </form>
  </AuthLayout>
</template>
