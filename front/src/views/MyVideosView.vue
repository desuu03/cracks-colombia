<script setup>
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import VideoCard from '../components/VideoCard.vue'
import VideoUploader from '../components/VideoUploader.vue'
import { deleteVideo, listMyVideos } from '../api/videos'
import { session } from '../stores/session'

const videos = ref([])
const loading = ref(true)
const error = ref('')
const deletingId = ref(null)

async function load() {
  loading.value = true
  error.value = ''
  try {
    videos.value = await listMyVideos()
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

function onUploaded(video) {
  videos.value = [video, ...videos.value]
}

async function remove(video) {
  deletingId.value = video.id
  error.value = ''
  try {
    await deleteVideo(video.id)
    videos.value = videos.value.filter((v) => v.id !== video.id)
  } catch (e) {
    error.value = e.message
  } finally {
    deletingId.value = null
  }
}

onMounted(load)
</script>

<template>
  <section>
    <div class="page-header">
      <div>
        <h1>Mis videos</h1>
        <p class="lead" style="margin: 0">Tu galería completa, con el estado de revisión de cada video.</p>
      </div>
      <RouterLink class="btn btn-ghost" :to="{ name: 'athlete-profile', params: { athleteId: session.user.id } }">
        Ver mi perfil público
      </RouterLink>
    </div>

    <div class="stack">
      <VideoUploader :athlete-id="session.user.id" @uploaded="onUploaded" />

      <p v-if="error" class="alert alert-error" role="alert">{{ error }}</p>
      <p v-if="loading" class="muted">Cargando videos…</p>
      <div v-else-if="videos.length === 0 && !error" class="card empty">
        Aún no has subido videos. Sube tu primera jugada para que los ojeadores te conozcan.
      </div>
      <div v-else class="video-grid">
        <VideoCard
          v-for="video in videos"
          :key="video.id"
          :video="video"
          manageable
          :deleting="deletingId === video.id"
          @delete="remove"
        />
      </div>
    </div>
  </section>
</template>
