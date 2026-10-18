import { formatBytes, formatDuration } from './format'

export const MAX_VIDEO_BYTES = 50_000_000
export const MAX_VIDEO_SECONDS = 60
const METADATA_TIMEOUT_MS = 10_000

/** Duration in seconds read by the browser, or null when it cannot decode the file. */
function readDuration(file) {
  return new Promise((resolve) => {
    const objectUrl = URL.createObjectURL(file)
    const video = document.createElement('video')
    let settled = false
    const finish = (duration) => {
      if (settled) return
      settled = true
      clearTimeout(timer)
      URL.revokeObjectURL(objectUrl)
      video.removeAttribute('src')
      video.load()
      resolve(Number.isFinite(duration) ? duration : null)
    }
    const timer = setTimeout(() => finish(null), METADATA_TIMEOUT_MS)
    video.preload = 'metadata'
    video.onloadedmetadata = () => finish(video.duration)
    video.onerror = () => finish(null)
    video.src = objectUrl
  })
}

/**
 * Pre-checks what the browser can verify before uploading. The backend still
 * validates the container, the H.264 codec and the real duration.
 */
export async function checkVideoFile(file) {
  const errors = []
  const looksLikeMp4 = file.type === 'video/mp4' || /\.mp4$/i.test(file.name)
  if (!looksLikeMp4) errors.push('Solo se aceptan archivos MP4 (video H.264).')
  if (file.size === 0) errors.push('El archivo está vacío.')
  if (file.size > MAX_VIDEO_BYTES) {
    errors.push(`El archivo pesa ${formatBytes(file.size)}; el máximo es ${formatBytes(MAX_VIDEO_BYTES)}.`)
  }

  const duration = looksLikeMp4 ? await readDuration(file) : null
  if (duration !== null && duration > MAX_VIDEO_SECONDS) {
    errors.push(`El video dura ${formatDuration(duration)}; el máximo es ${MAX_VIDEO_SECONDS} segundos.`)
  }
  return { errors, duration }
}
