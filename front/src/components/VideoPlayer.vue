<script setup>
import { onBeforeUnmount, ref } from 'vue'
import { apiUrl } from '../api/http'
import { downloadVideo } from '../api/videos'

const props = defineProps({
  /** The `url` of a VideoResponse, e.g. /videos/{id}/content. */
  contentPath: { type: String, required: true },
  /**
   * Approved videos are public and stream straight into <video>. Pending and
   * rejected ones need the Bearer token, which a <video src> cannot send, so they
   * are downloaded on demand and played from a blob URL.
   */
  isPublic: { type: Boolean, default: false },
})

const blobUrl = ref(null)
const loading = ref(false)
const error = ref('')

async function loadPrivate() {
  loading.value = true
  error.value = ''
  try {
    blobUrl.value = URL.createObjectURL(await downloadVideo(props.contentPath))
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

onBeforeUnmount(() => {
  if (blobUrl.value) URL.revokeObjectURL(blobUrl.value)
})
</script>

<template>
  <div class="player">
    <video v-if="isPublic" :src="apiUrl(contentPath)" controls preload="metadata" playsinline></video>
    <video v-else-if="blobUrl" :src="blobUrl" controls autoplay playsinline></video>
    <div v-else>
      <p v-if="error" class="player-note">{{ error }}</p>
      <button type="button" class="btn btn-ghost btn-small" style="color: #fff" :disabled="loading" @click="loadPrivate">
        {{ loading ? 'Cargando…' : 'Ver vista previa' }}
      </button>
    </div>
  </div>
</template>
