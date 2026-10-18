<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { listRequests, respondToRequest } from '../api/requests'
import { session } from '../stores/session'
import { refreshNotifications } from '../stores/notifications'
import { REQUEST_STATUS_LABELS, formatProposedDate, initials } from '../utils/format'

const TABS = ['PENDIENTE', 'ACEPTADA', 'RECHAZADA']

const requests = ref([])
const tab = ref('PENDIENTE')
const loading = ref(true)
const error = ref('')
const busyId = ref(null)
const confirmingReject = ref(null)

const isAthlete = computed(() => session.user.role === 'DEPORTISTA')
const shown = computed(() => requests.value.filter((r) => r.status === tab.value))
const count = (status) => requests.value.filter((r) => r.status === status).length

onMounted(async () => {
  try {
    requests.value = await listRequests()
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
})

async function respond(request, decision) {
  busyId.value = request.id
  error.value = ''
  try {
    const updated = await respondToRequest(request.id, decision)
    requests.value = requests.value.map((r) => (r.id === updated.id ? updated : r))
    confirmingReject.value = null
    tab.value = updated.status
    refreshNotifications()
  } catch (e) {
    error.value = e.message
  } finally {
    busyId.value = null
  }
}

function statusLabel(status) {
  if (!isAthlete.value && status === 'PENDIENTE') return 'Esperando respuesta'
  return REQUEST_STATUS_LABELS[status] ?? status
}
</script>

<template>
  <section>
    <div class="page-header">
      <div>
        <h1>{{ isAthlete ? 'Solicitudes recibidas' : 'Solicitudes enviadas' }}</h1>
        <p class="lead">
          {{ isAthlete ? 'Clubes y ojeadores que quieren contactarte o invitarte a una prueba.' : 'El estado de las solicitudes que enviaste a deportistas.' }}
        </p>
      </div>
      <RouterLink v-if="!isAthlete" class="btn btn-ghost" :to="{ name: 'search' }">Buscar talentos</RouterLink>
    </div>

    <div class="tabs" role="tablist">
      <button
        v-for="status in TABS"
        :key="status"
        type="button"
        role="tab"
        :aria-selected="tab === status"
        @click="tab = status"
      >
        {{ REQUEST_STATUS_LABELS[status] }}s<span class="count">{{ count(status) }}</span>
      </button>
    </div>

    <p v-if="error" class="alert alert-error" role="alert" style="margin-bottom: 1rem">{{ error }}</p>
    <p v-if="loading" class="muted">Cargando solicitudes…</p>
    <div v-else-if="shown.length === 0" class="card empty">
      <template v-if="tab === 'PENDIENTE'">
        {{ isAthlete ? 'No tienes solicitudes pendientes. Mantén tu perfil y tus videos al día para que te encuentren.' : 'No tienes solicitudes esperando respuesta.' }}
      </template>
      <template v-else>No hay solicitudes en esta pestaña.</template>
    </div>

    <div v-else class="stack">
      <article v-for="request in shown" :key="request.id" class="card request-card">
        <div class="request-head">
          <div class="athlete-head">
            <span class="avatar" aria-hidden="true">{{ initials(isAthlete ? request.scoutClub || request.scoutName : request.athleteName) }}</span>
            <div v-if="isAthlete">
              <b>{{ request.scoutClub || request.scoutName }}</b>
              <div class="hint">{{ request.scoutClub ? `${request.scoutName} · ` : '' }}Ojeador / Entrenador</div>
            </div>
            <div v-else>
              <RouterLink :to="{ name: 'athlete-profile', params: { athleteId: request.athleteId } }"><b>{{ request.athleteName }}</b></RouterLink>
              <div class="hint">{{ request.athleteSummary }}</div>
            </div>
          </div>
          <span :class="['badge', `badge-${request.status}`]">{{ statusLabel(request.status) }}</span>
        </div>

        <blockquote>{{ request.message }}</blockquote>
        <p v-if="request.proposedDate" style="margin: 0">
          <b>Prueba propuesta:</b> {{ formatProposedDate(request.proposedDate) }}
        </p>
        <p v-else class="hint" style="margin: 0">Sin fecha propuesta.</p>

        <template v-if="isAthlete && request.status === 'PENDIENTE'">
          <div v-if="confirmingReject === request.id" class="row">
            <span class="muted">¿Rechazar esta solicitud? El club recibirá un aviso.</span>
            <button type="button" class="btn btn-danger btn-small" :disabled="busyId === request.id" @click="respond(request, 'REJECTED')">
              Sí, rechazar
            </button>
            <button type="button" class="btn btn-ghost btn-small" :disabled="busyId === request.id" @click="confirmingReject = null">Cancelar</button>
          </div>
          <div v-else class="row">
            <button type="button" class="btn btn-primary" :disabled="busyId === request.id" @click="respond(request, 'ACCEPTED')">
              {{ busyId === request.id ? 'Guardando…' : 'Aceptar y compartir contacto' }}
            </button>
            <button type="button" class="btn btn-danger-outline" :disabled="busyId === request.id" @click="confirmingReject = request.id">
              Rechazar
            </button>
          </div>
          <p class="hint" style="margin: 0">Si aceptas, el club verá tus datos de contacto y tú verás los suyos.</p>
        </template>

        <div v-if="request.status === 'ACEPTADA'" class="contact-box">
          <b>Contacto compartido</b>
          <span>{{ request.contact || 'Datos de contacto no disponibles.' }}</span>
        </div>
      </article>
    </div>
  </section>
</template>
