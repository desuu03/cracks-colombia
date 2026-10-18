<script setup>
import { ref } from 'vue'
import VideoPlayer from './VideoPlayer.vue'
import { STATUS_LABELS, formatBytes, formatDateTime, formatDuration } from '../utils/format'

defineProps({
  video: { type: Object, required: true },
  /** Owner view: shows the moderation status and the delete action. */
  manageable: { type: Boolean, default: false },
  deleting: { type: Boolean, default: false },
})
const emit = defineEmits(['delete'])

// Two-step delete instead of window.confirm.
const confirming = ref(false)
</script>

<template>
  <article class="card video-card">
    <VideoPlayer :content-path="video.url" :is-public="video.status === 'APROBADO'" />
    <div class="video-card-body">
      <div v-if="manageable" class="row">
        <span :class="['badge', `badge-${video.status}`]">{{ STATUS_LABELS[video.status] ?? video.status }}</span>
      </div>
      <p v-if="manageable && video.status === 'RECHAZADO' && video.rejectionReason" class="alert alert-error">
        Motivo: {{ video.rejectionReason }}
      </p>
      <p class="video-meta">
        {{ formatDuration(video.durationSeconds) }} · {{ formatBytes(video.sizeBytes) }} ·
        {{ formatDateTime(video.uploadedAt) }}
      </p>
      <div v-if="manageable" class="row" style="margin-top: auto">
        <template v-if="confirming">
          <span class="muted">¿Eliminar este video?</span>
          <button type="button" class="btn btn-danger btn-small" :disabled="deleting" @click="emit('delete', video)">
            {{ deleting ? 'Eliminando…' : 'Sí, eliminar' }}
          </button>
          <button type="button" class="btn btn-ghost btn-small" :disabled="deleting" @click="confirming = false">
            Cancelar
          </button>
        </template>
        <button v-else type="button" class="btn btn-ghost btn-small" @click="confirming = true">Eliminar</button>
      </div>
    </div>
  </article>
</template>
