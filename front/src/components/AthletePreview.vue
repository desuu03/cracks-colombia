<script setup>
import AthleteStats from './AthleteStats.vue'
import { initials } from '../utils/format'

defineProps({
  /** Profile fields as currently typed, so the preview follows the form. */
  profile: { type: Object, required: true },
})
</script>

<template>
  <aside class="card stack" aria-label="Vista previa del perfil">
    <span class="eyebrow">Así te ven los ojeadores</span>
    <div class="athlete-head">
      <span class="avatar">{{ initials(profile.fullName) }}</span>
      <div>
        <b>{{ profile.fullName }}</b>
        <div class="hint">
          {{ [profile.sport, profile.position, profile.region].filter(Boolean).join(' · ') || 'Completa tu deporte, posición y departamento' }}
        </div>
      </div>
    </div>
    <AthleteStats :profile="profile" />
  </aside>
</template>
