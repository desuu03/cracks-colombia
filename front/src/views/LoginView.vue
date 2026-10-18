<script setup>
import { ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { login } from '../api/auth'
import { startSession } from '../stores/session'
import { safeRedirect } from '../router'

const route = useRoute()
const router = useRouter()

const email = ref('')
const password = ref('')
const error = ref('')
const submitting = ref(false)

async function submit() {
  error.value = ''
  submitting.value = true
  try {
    const auth = await login(email.value.trim(), password.value)
    startSession(auth)
    const fallback = auth.user.role === 'DEPORTISTA' ? { name: 'my-videos' } : { name: 'home' }
    router.replace(safeRedirect(route.query.redirect) ?? fallback)
  } catch (e) {
    error.value = e.message
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <section class="narrow">
    <h1>Iniciar sesión</h1>
    <p class="lead">Ingresa con el correo y la contraseña de tu cuenta.</p>

    <form class="card stack" novalidate @submit.prevent="submit">
      <p v-if="route.query.expirada" class="alert alert-info">
        Tu sesión terminó. Inicia sesión de nuevo para continuar.
      </p>
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
      <p class="muted">
        ¿No tienes cuenta? <RouterLink :to="{ name: 'register' }">Regístrate</RouterLink>
      </p>
    </form>
  </section>
</template>
