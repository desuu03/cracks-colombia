<script setup>
import { computed, reactive, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { register } from '../api/auth'
import { startSession } from '../stores/session'
import { isMinor, todayIso } from '../utils/age'

const router = useRouter()

const form = reactive({
  role: 'DEPORTISTA',
  fullName: '',
  email: '',
  password: '',
  birthDate: '',
  guardianFullName: '',
  guardianEmail: '',
  guardianAccepted: false,
  dataProcessingConsent: false,
})
const touched = ref(false)
const serverError = ref('')
const submitting = ref(false)

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const today = todayIso()

const isAthlete = computed(() => form.role === 'DEPORTISTA')
const needsGuardian = computed(() => isAthlete.value && isMinor(form.birthDate))

// Mirrors the backend's bean validation so mistakes show before the round trip.
const errors = computed(() => {
  const e = {}
  if (!form.fullName.trim()) e.fullName = 'El nombre completo es obligatorio.'
  else if (form.fullName.trim().length > 150) e.fullName = 'El nombre no puede superar 150 caracteres.'
  if (!EMAIL_PATTERN.test(form.email.trim())) e.email = 'Ingresa un correo válido.'
  if (form.password.length < 8) e.password = 'La contraseña debe tener al menos 8 caracteres.'
  if (isAthlete.value) {
    if (!form.birthDate) e.birthDate = 'La fecha de nacimiento es obligatoria para deportistas.'
    else if (form.birthDate > today) e.birthDate = 'La fecha de nacimiento no puede estar en el futuro.'
  }
  if (needsGuardian.value) {
    if (!form.guardianFullName.trim()) e.guardianFullName = 'El nombre del acudiente es obligatorio.'
    if (!EMAIL_PATTERN.test(form.guardianEmail.trim())) e.guardianEmail = 'Ingresa un correo válido para el acudiente.'
    if (!form.guardianAccepted) e.guardianAccepted = 'El acudiente debe autorizar el registro.'
  }
  if (!form.dataProcessingConsent) e.dataProcessingConsent = 'Debes aceptar la política de tratamiento de datos.'
  return e
})

const show = (field) => (touched.value ? errors.value[field] : '')

function buildPayload() {
  return {
    role: form.role,
    fullName: form.fullName.trim(),
    email: form.email.trim(),
    password: form.password,
    birthDate: isAthlete.value ? form.birthDate : null,
    guardianConsent: needsGuardian.value
      ? {
          guardianFullName: form.guardianFullName.trim(),
          guardianEmail: form.guardianEmail.trim(),
          accepted: form.guardianAccepted,
        }
      : null,
    dataProcessingConsent: form.dataProcessingConsent,
  }
}

async function submit() {
  touched.value = true
  serverError.value = ''
  if (Object.keys(errors.value).length > 0) return
  submitting.value = true
  try {
    const auth = await register(buildPayload())
    startSession(auth)
    router.replace(auth.user.role === 'DEPORTISTA' ? { name: 'my-videos' } : { name: 'home' })
  } catch (e) {
    serverError.value = e.message
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <section class="narrow">
    <h1>Crear cuenta</h1>
    <p class="lead">Muestra tu talento o encuentra a los próximos cracks del país.</p>

    <form class="card stack" novalidate @submit.prevent="submit">
      <p v-if="serverError" class="alert alert-error" role="alert">{{ serverError }}</p>

      <fieldset>
        <legend class="legend">¿Cómo usarás la plataforma?</legend>
        <div class="choices" style="margin-top: 0.5rem">
          <label class="choice">
            <input v-model="form.role" type="radio" name="role" value="DEPORTISTA" />
            <span>Deportista<small>Sube videos de tus habilidades.</small></span>
          </label>
          <label class="choice">
            <input v-model="form.role" type="radio" name="role" value="OJEADOR_ENTRENADOR" />
            <span>Ojeador / Entrenador<small>Busca y contacta talentos.</small></span>
          </label>
        </div>
      </fieldset>

      <div class="field">
        <label for="reg-name">Nombre completo</label>
        <input id="reg-name" v-model="form.fullName" autocomplete="name" maxlength="150" />
        <span v-if="show('fullName')" class="field-error">{{ show('fullName') }}</span>
      </div>
      <div class="field">
        <label for="reg-email">Correo electrónico</label>
        <input id="reg-email" v-model="form.email" type="email" autocomplete="email" maxlength="254" />
        <span v-if="show('email')" class="field-error">{{ show('email') }}</span>
      </div>
      <div class="field">
        <label for="reg-password">Contraseña</label>
        <input id="reg-password" v-model="form.password" type="password" autocomplete="new-password" />
        <span v-if="show('password')" class="field-error">{{ show('password') }}</span>
        <span v-else class="hint">Mínimo 8 caracteres.</span>
      </div>

      <div v-if="isAthlete" class="field">
        <label for="reg-birth">Fecha de nacimiento</label>
        <input id="reg-birth" v-model="form.birthDate" type="date" :max="today" min="1900-01-01" />
        <span v-if="show('birthDate')" class="field-error">{{ show('birthDate') }}</span>
      </div>

      <fieldset v-if="needsGuardian" class="subsection stack">
        <legend class="legend">Autorización del acudiente</legend>
        <p class="hint">
          Eres menor de edad, así que tu madre, padre o acudiente debe autorizar el registro.
        </p>
        <div class="field">
          <label for="reg-guardian-name">Nombre del acudiente</label>
          <input id="reg-guardian-name" v-model="form.guardianFullName" maxlength="150" />
          <span v-if="show('guardianFullName')" class="field-error">{{ show('guardianFullName') }}</span>
        </div>
        <div class="field">
          <label for="reg-guardian-email">Correo del acudiente</label>
          <input id="reg-guardian-email" v-model="form.guardianEmail" type="email" maxlength="254" />
          <span v-if="show('guardianEmail')" class="field-error">{{ show('guardianEmail') }}</span>
        </div>
        <label class="checkbox">
          <input v-model="form.guardianAccepted" type="checkbox" />
          <span>Mi acudiente autoriza mi registro y el tratamiento de mis datos en la plataforma.</span>
        </label>
        <span v-if="show('guardianAccepted')" class="field-error">{{ show('guardianAccepted') }}</span>
      </fieldset>

      <label class="checkbox">
        <input v-model="form.dataProcessingConsent" type="checkbox" />
        <span>Acepto la política de tratamiento de datos personales.</span>
      </label>
      <span v-if="show('dataProcessingConsent')" class="field-error">{{ show('dataProcessingConsent') }}</span>

      <button class="btn btn-primary btn-block" type="submit" :disabled="submitting">
        {{ submitting ? 'Creando cuenta…' : 'Crear cuenta' }}
      </button>
      <p class="muted">
        ¿Ya tienes cuenta? <RouterLink :to="{ name: 'login' }">Inicia sesión</RouterLink>
      </p>
    </form>
  </section>
</template>
