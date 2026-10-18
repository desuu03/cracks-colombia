<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import AthletePreview from '../components/AthletePreview.vue'
import ProfileFields from '../components/ProfileFields.vue'
import { getMyProfile, isProfileComplete, updateMyProfile } from '../api/profile'
import { useProfileForm } from '../composables/profileForm'
import { session } from '../stores/session'
import { formatBirthDate } from '../utils/format'

const router = useRouter()
const { form, positions, errors, fill, hasErrors, payload } = useProfileForm()

const saved = ref(null)
const touched = ref(false)
const loading = ref(true)
const saving = ref(false)
const error = ref('')
const notice = ref('')
const preview = computed(() => ({ ...form, fullName: session.user.fullName, birthDate: session.user.birthDate }))
const birthDateText = computed(() => formatBirthDate(session.user.birthDate))

onMounted(async () => {
  try {
    const profile = await getMyProfile()
    if (!isProfileComplete(profile)) {
      router.replace({ name: 'profile-create' })
      return
    }
    saved.value = profile
    fill(profile)
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
})

async function save() {
  touched.value = true
  notice.value = ''
  if (hasErrors()) return
  saving.value = true
  error.value = ''
  try {
    saved.value = await updateMyProfile(payload())
    fill(saved.value)
    touched.value = false
    notice.value = 'Cambios guardados. Tu perfil público ya está actualizado.'
  } catch (e) {
    error.value = e.message
  } finally {
    saving.value = false
  }
}

function discard() {
  fill(saved.value)
  touched.value = false
  notice.value = ''
}
</script>

<template>
  <section>
    <div class="page-header">
      <div>
        <h1>Mi perfil</h1>
        <p class="lead">Mantén tus datos al día para aparecer en las búsquedas correctas.</p>
      </div>
      <RouterLink class="btn btn-ghost" :to="{ name: 'athlete-profile', params: { athleteId: session.user.id } }">
        Ver mi perfil público
      </RouterLink>
    </div>

    <p v-if="loading" class="muted">Cargando tu perfil…</p>
    <div v-else class="profile-layout">
      <form class="card stack" novalidate @submit.prevent="save">
        <p v-if="error" class="alert alert-error" role="alert">{{ error }}</p>
        <p v-if="notice" class="alert alert-success" role="status">{{ notice }}</p>
        <ProfileFields :form="form" :positions="positions" :errors="errors" :touched="touched" headings />
        <div class="field">
          <label for="profile-identity">Nombre y fecha de nacimiento</label>
          <input id="profile-identity" :value="[session.user.fullName, birthDateText].filter(Boolean).join(' · ')" disabled />
          <span class="hint">No se pueden cambiar desde aquí porque definen la autorización de tu acudiente.</span>
        </div>
        <div class="row">
          <button type="submit" class="btn btn-primary" :disabled="saving">{{ saving ? 'Guardando…' : 'Guardar cambios' }}</button>
          <button type="button" class="btn btn-ghost" :disabled="saving" @click="discard">Descartar cambios</button>
        </div>
      </form>
      <AthletePreview :profile="preview" />
    </div>
  </section>
</template>
