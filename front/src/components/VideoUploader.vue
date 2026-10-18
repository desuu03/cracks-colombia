<script setup>
import { ref } from 'vue'
import { uploadVideo } from '../api/videos'
import { checkVideoFile, MAX_VIDEO_SECONDS } from '../utils/videoFile'
import { formatBytes, formatDuration } from '../utils/format'

const props = defineProps({
  athleteId: { type: String, required: true },
})
const emit = defineEmits(['uploaded'])

const input = ref(null)
const file = ref(null)
const duration = ref(null)
const problems = ref([])
const checking = ref(false)
const progress = ref(null)
const serverError = ref('')

async function onSelect(event) {
  const selected = event.target.files?.[0] ?? null
  file.value = selected
  problems.value = []
  serverError.value = ''
  duration.value = null
  if (!selected) return
  checking.value = true
  const result = await checkVideoFile(selected)
  // Ignore a stale result if the user picked another file meanwhile.
  if (file.value !== selected) return
  problems.value = result.errors
  duration.value = result.duration
  checking.value = false
}

function reset() {
  file.value = null
  duration.value = null
  problems.value = []
  progress.value = null
  if (input.value) input.value.value = ''
}

async function upload() {
  serverError.value = ''
  progress.value = 0
  try {
    const video = await uploadVideo(props.athleteId, file.value, (fraction) => (progress.value = fraction))
    emit('uploaded', video)
    reset()
  } catch (e) {
    serverError.value = e.message
    progress.value = null
  }
}
</script>

<template>
  <section class="card stack">
    <div>
      <h2>Subir un video</h2>
      <p class="hint">
        MP4 con video H.264, máximo 50 MB y {{ MAX_VIDEO_SECONDS }} segundos. Cada video queda pendiente
        hasta que un moderador lo apruebe.
      </p>
    </div>

    <div class="field">
      <label for="video-file">Archivo</label>
      <input
        id="video-file"
        ref="input"
        type="file"
        accept="video/mp4,.mp4"
        :disabled="progress !== null"
        @change="onSelect"
      />
    </div>

    <p v-if="checking" class="muted">Revisando el archivo…</p>
    <template v-else-if="file">
      <p class="video-meta">
        {{ file.name }} · {{ formatBytes(file.size) }}
        <template v-if="duration !== null"> · {{ formatDuration(duration) }}</template>
      </p>
      <div v-if="problems.length" class="alert alert-error" role="alert">
        <ul>
          <li v-for="problem in problems" :key="problem">{{ problem }}</li>
        </ul>
      </div>
      <p v-else-if="duration === null" class="alert alert-warning">
        Tu navegador no pudo leer la duración; el servidor la verificará al subirlo.
      </p>
    </template>

    <p v-if="serverError" class="alert alert-error" role="alert">{{ serverError }}</p>

    <div v-if="progress !== null" class="stack">
      <div class="progress" role="progressbar" :aria-valuenow="Math.round(progress * 100)" aria-valuemin="0" aria-valuemax="100">
        <div :style="{ width: `${progress * 100}%` }"></div>
      </div>
      <p class="hint">
        {{ progress < 1 ? `Subiendo… ${Math.round(progress * 100)} %` : 'Verificando el video en el servidor…' }}
      </p>
    </div>

    <div class="row">
      <button
        type="button"
        class="btn btn-primary"
        :disabled="!file || checking || problems.length > 0 || progress !== null"
        @click="upload"
      >
        Subir video
      </button>
      <button v-if="file && progress === null" type="button" class="btn btn-ghost" @click="reset">Quitar</button>
    </div>
  </section>
</template>
