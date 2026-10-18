import { request, requestBlob, uploadFile } from './http'

export function uploadVideo(athleteId, file, onProgress) {
  return uploadFile(`/athletes/${athleteId}/videos`, 'file', file, onProgress)
}

/** The authenticated athlete's full gallery, every moderation status included. */
export function listMyVideos() {
  return request('/users/me/videos')
}

/** Public gallery: approved videos only. */
export function listApprovedVideos(athleteId) {
  return request(`/athletes/${athleteId}/videos`)
}

export function deleteVideo(videoId) {
  return request(`/videos/${videoId}`, { method: 'DELETE' })
}

/** `contentPath` is the `url` property of a VideoResponse. */
export function downloadVideo(contentPath) {
  return requestBlob(contentPath)
}
