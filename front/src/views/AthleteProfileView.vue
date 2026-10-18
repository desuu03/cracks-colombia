<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import AthleteStats from '../components/AthleteStats.vue'
import VideoCard from '../components/VideoCard.vue'
import { getAthleteProfile } from '../api/profile'
import { createRequest } from '../api/requests'
import { markInterest } from '../api/search'
import { apiUrl } from '../api/http'
import { listApprovedVideos } from '../api/videos'
import { session } from '../stores/session'
import { initials } from '../utils/format'

const props = defineProps({
  athleteId: { type: String, required: true },
})

const MAX_MESSAGE = 1000

const profile = ref(null)
const videos = ref([])
const loading = ref(true)
const error = ref('')
const interested = ref(false)
const pendingRequest = ref(false)
const markingInterest = ref(false)
const actionError = ref('')
const requestForm = reactive({ open: false, message: '', proposedDate: '', sending: false, error: '' })
const sentNotice = ref('')

const isScout = computed(() => session.user?.role === 'OJEADOR_ENTRENADOR')
const isOwner = computed(() => session.user?.id === props.athleteId)
const isMinor = computed(() => profile.value?.age != null && profile.value.age < 18)
const hasSportProfile = computed(() => Boolean(profile.value?.sport))
// datetime-local min: now, in local time.
const minProposed = computed(() => {
  const now = new Date()
  now.setMinutes(now.getMinutes() - now.getTimezoneOffset())
  return now.toISOString().slice(0, 16)
})

async function load(athleteId) {
  loading.value = true
  error.value = ''
  Object.assign(requestForm, { open: false, message: '', proposedDate: '', error: '' })
  sentNotice.value = ''
  try {
    const [loaded, approved] = await Promise.all([
      getAthleteProfile(athleteId),
      listApprovedVideos(athleteId).catch(() => []),
    ])
    profile.value = loaded
    videos.value = approved
    interested.value = Boolean(loaded.interestedByMe)
    pendingRequest.value = Boolean(loaded.pendingRequestByMe)
  } catch (e) {
    profile.value = null
    error.value = e.message
  } finally {
    loading.value = false
  }
}

watch(() => props.athleteId, load, { immediate: true })

async function addInterest() {
  markingInterest.value = true
  actionError.value = ''
  try {
    await markInterest(props.athleteId)
    interested.value = true
  } catch (e) {
    actionError.value = e.message
  } finally {
    markingInterest.value = false
  }
}

async function sendRequest() {
  requestForm.error = ''
  if (!requestForm.message.trim()) {
    requestForm.error = 'Escribe un mensaje para el deportista.'
    return
  }
  requestForm.sending = true
  try {
    await createRequest({
      athleteId: props.athleteId,
      message: requestForm.message.trim(),
      proposedDate: requestForm.proposedDate ? new Date(requestForm.proposedDate).toISOString() : null,
    })
    pendingRequest.value = true
    requestForm.open = false
    sentNotice.value = 'Solicitud enviada. Te avisaremos cuando el deportista responda.'
  } catch (e) {
    if (e.status === 409) {
      pendingRequest.value = true
      requestForm.open = false
    }
    requestForm.error = e.message
  } finally {
    requestForm.sending = false
  }
}
</script>

<template>
  <div v-if="loading" class="page"><p class="muted">Cargando perfil…</p></div>

  <div v-else-if="!profile" class="page">
    <div class="card empty">
      <h1 style="color: var(--text)">Perfil no disponible</h1>
      <p>{{ error }}</p>
      <RouterLink v-if="isScout" class="btn btn-ghost" :to="{ name: 'search' }">Volver a la búsqueda</RouterLink>
    </div>
  </div>

  <template v-else>
    <section class="hero">
      <div class="hero-inner">
        <RouterLink v-if="isScout" :to="{ name: 'search' }">← Volver a resultados</RouterLink>
        <div class="row" style="gap: 1rem">
          <img v-if="profile.photoUrl" class="avatar avatar-lg" :src="apiUrl(profile.photoUrl)" alt="" />
          <span v-else class="avatar avatar-lg" aria-hidden="true">{{ initials(profile.fullName) }}</span>
          <div style="display: grid; gap: 0.4rem">
            <h1>{{ profile.fullName }}</h1>
            <div class="hero-meta">
              <span v-if="profile.sport" class="badge">{{ profile.sport }}</span>
              <span v-if="profile.position">{{ profile.position }}</span>
              <span v-if="profile.region">{{ profile.region }}</span>
            </div>
          </div>
        </div>

        <div v-if="isScout" class="row">
          <span v-if="pendingRequest" class="chip">Solicitud enviada · pendiente</span>
          <template v-else>
            <button v-if="!interested" type="button" class="btn btn-accent" :disabled="markingInterest" @click="addInterest">
              ☆ Marcar interés
            </button>
            <span v-else class="chip">★ En tu lista de interés</span>
            <button
              type="button"
              :class="['btn', interested ? 'btn-accent' : 'btn-ghost']"
              :disabled="!interested || requestForm.open"
              :title="interested ? undefined : 'Primero marca interés'"
              @click="requestForm.open = true"
            >
              Solicitar contacto
            </button>
          </template>
        </div>
        <div v-else-if="isOwner" class="row">
          <RouterLink class="btn btn-ghost" :to="{ name: 'my-profile' }">Editar perfil</RouterLink>
          <RouterLink class="btn btn-ghost" :to="{ name: 'my-videos' }">Gestionar mis videos</RouterLink>
        </div>
      </div>
    </section>

    <main class="page stack">
      <p v-if="actionError" class="alert alert-error" role="alert">{{ actionError }}</p>
      <p v-if="sentNotice" class="alert alert-success" role="status">{{ sentNotice }}</p>
      <p v-if="requestForm.error && !requestForm.open" class="alert alert-error" role="alert">{{ requestForm.error }}</p>
      <p v-if="isOwner && !hasSportProfile" class="alert alert-warning">
        Tu perfil aún no aparece en las búsquedas. <RouterLink :to="{ name: 'profile-create' }">Complétalo aquí</RouterLink>.
      </p>

      <form v-if="requestForm.open" class="card stack" novalidate @submit.prevent="sendRequest">
        <h2>Solicitud a {{ profile.fullName }}</h2>
        <p v-if="requestForm.error" class="alert alert-error" role="alert">{{ requestForm.error }}</p>
        <div class="field">
          <label for="request-message">Mensaje</label>
          <textarea id="request-message" v-model="requestForm.message" :maxlength="MAX_MESSAGE" placeholder="Cuéntale quién eres, qué viste en sus videos y qué le propones."></textarea>
          <span class="hint">{{ requestForm.message.length }} / {{ MAX_MESSAGE }} caracteres.</span>
        </div>
        <div class="field">
          <label for="request-date">Fecha propuesta para una prueba (opcional)</label>
          <input id="request-date" v-model="requestForm.proposedDate" type="datetime-local" :min="minProposed" />
        </div>
        <p class="hint">
          {{ isMinor ? 'Es menor de edad: si acepta, verás los datos de contacto que registró con su acudiente.' : 'Si acepta, verás sus datos de contacto.' }}
        </p>
        <div class="row">
          <button type="submit" class="btn btn-primary" :disabled="requestForm.sending">
            {{ requestForm.sending ? 'Enviando…' : 'Enviar solicitud' }}
          </button>
          <button type="button" class="btn btn-ghost" :disabled="requestForm.sending" @click="requestForm.open = false">Cancelar</button>
        </div>
      </form>

      <div class="profile-layout wide-aside">
        <div class="stack">
          <section class="card stack">
            <span class="eyebrow">Videos aprobados · {{ videos.length }}</span>
            <div v-if="videos.length" class="video-grid">
              <VideoCard v-for="video in videos" :key="video.id" :video="video" />
            </div>
            <p v-else class="muted" style="margin: 0">
              {{ isOwner ? 'Aún no tienes videos aprobados. Los pendientes aparecen aquí cuando un moderador los aprueba.' : 'Este deportista aún no tiene videos aprobados.' }}
            </p>
          </section>
          <section class="card stack">
            <span class="eyebrow">Trayectoria</span>
            <p style="margin: 0; white-space: pre-line">{{ profile.trajectory || 'Sin trayectoria registrada.' }}</p>
          </section>
        </div>
        <aside class="card stack">
          <span class="eyebrow">Ficha</span>
          <AthleteStats :profile="profile" />
          <p class="hint" style="margin: 0">La edad la calcula el sistema a partir de la fecha de nacimiento.</p>
        </aside>
      </div>
    </main>
  </template>
</template>
