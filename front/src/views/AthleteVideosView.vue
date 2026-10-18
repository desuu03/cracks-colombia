<script setup>
import { computed, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import VideoCard from '../components/VideoCard.vue'
import { listApprovedVideos } from '../api/videos'
import { session } from '../stores/session'

const props = defineProps({
  athleteId: { type: String, required: true },
})

const UUID_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i

const videos = ref([])
const loading = ref(true)
const error = ref('')
const isOwner = computed(() => session.user?.id === props.athleteId)

async function load(athleteId) {
  videos.value = []
  error.value = ''
  if (!UUID_PATTERN.test(athleteId)) {
    error.value = 'El identificador del deportista no es válido.'
    loading.value = false
    return
  }
  loading.value = true
  try {
    videos.value = await listApprovedVideos(athleteId)
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

watch(() => props.athleteId, load, { immediate: true })
</script>

<template>
  <section>
    <div class="page-header">
      <div>
        <h1>Videos del deportista</h1>
        <p class="lead" style="margin: 0">Solo se muestran los videos aprobados por moderación.</p>
      </div>
      <RouterLink v-if="isOwner" class="btn btn-ghost" :to="{ name: 'my-videos' }">Gestionar mis videos</RouterLink>
    </div>

    <p v-if="error" class="alert alert-error" role="alert">{{ error }}</p>
    <p v-else-if="loading" class="muted">Cargando videos…</p>
    <div v-else-if="videos.length === 0" class="card empty">
      {{ isOwner ? 'Todavía no tienes videos aprobados. Los pendientes aparecerán aquí cuando un moderador los apruebe.' : 'Este deportista aún no tiene videos aprobados.' }}
    </div>
    <div v-else class="video-grid">
      <VideoCard v-for="video in videos" :key="video.id" :video="video" />
    </div>
  </section>
</template>
