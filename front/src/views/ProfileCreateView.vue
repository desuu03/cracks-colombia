<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import AthletePreview from '../components/AthletePreview.vue'
import ProfileFields from '../components/ProfileFields.vue'
import { getMyProfile, updateMyProfile } from '../api/profile'
import { STEP_FIELDS, useProfileForm } from '../composables/profileForm'
import { session } from '../stores/session'
import { formatBirthDate } from '../utils/format'

const router = useRouter()
const { form, positions, errors, fill, hasErrors, payload } = useProfileForm()

const STEPS = [
  { n: 1, label: 'Deporte y posición', sections: ['sport'] },
  { n: 2, label: 'Datos físicos', sections: ['physical'] },
  { n: 3, label: 'Trayectoria', sections: ['trajectory'] },
]
const step = ref(1)
const touched = ref(false)
const loading = ref(true)
const saving = ref(false)
const error = ref('')
const current = computed(() => STEPS[step.value - 1])
const firstName = computed(() => session.user.fullName.split(' ')[0])
const preview = computed(() => ({ ...form, fullName: session.user.fullName, birthDate: session.user.birthDate }))

onMounted(async () => {
  try {
    fill(await getMyProfile())
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
})

function next() {
  touched.value = true
  if (hasErrors(STEP_FIELDS[step.value])) return
  touched.value = false
  step.value += 1
}

async function publish() {
  touched.value = true
  if (hasErrors()) {
    // Send the athlete back to the first step with a problem.
    step.value = STEPS.find((s) => hasErrors(STEP_FIELDS[s.n])).n
    return
  }
  saving.value = true
  error.value = ''
  try {
    await updateMyProfile(payload())
    router.replace({ name: 'athlete-profile', params: { athleteId: session.user.id } })
  } catch (e) {
    error.value = e.message
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <section class="stack">
    <div>
      <span class="eyebrow">Bienvenido, {{ firstName }}</span>
      <h1>Arma tu perfil deportivo</h1>
      <p class="lead" style="margin: 0">Así te encuentran los ojeadores. Te toma unos 3 minutos.</p>
    </div>

    <ol class="steps" aria-label="Pasos">
      <li v-for="s in STEPS" :key="s.n" :class="{ done: s.n < step, current: s.n === step }" :aria-current="s.n === step ? 'step' : undefined">
        <b>Paso {{ s.n }}</b>{{ s.label }}
      </li>
    </ol>

    <p v-if="loading" class="muted">Cargando tu perfil…</p>
    <div v-else class="profile-layout">
      <form class="card stack" novalidate @submit.prevent="step < 3 ? next() : publish()">
        <h2>{{ current.label }}</h2>
        <p v-if="error" class="alert alert-error" role="alert">{{ error }}</p>
        <ProfileFields :form="form" :positions="positions" :errors="errors" :touched="touched" :sections="current.sections" />
        <div v-if="step === 2" class="field">
          <label for="profile-birth">Fecha de nacimiento</label>
          <input id="profile-birth" :value="formatBirthDate(session.user.birthDate) || '—'" disabled />
          <span class="hint">Viene del registro y no se puede cambiar aquí.</span>
        </div>
        <div class="row" style="justify-content: space-between">
          <button type="button" class="btn btn-ghost" :disabled="step === 1 || saving" @click="step -= 1">Atrás</button>
          <button v-if="step < 3" type="submit" class="btn btn-primary">Continuar</button>
          <button v-else type="submit" class="btn btn-primary" :disabled="saving">
            {{ saving ? 'Publicando…' : 'Publicar perfil' }}
          </button>
        </div>
      </form>
      <AthletePreview :profile="preview" />
    </div>
  </section>
</template>
