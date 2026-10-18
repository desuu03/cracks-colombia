<script setup>
import { computed } from 'vue'
import { ageOn } from '../utils/age'

const props = defineProps({
  profile: { type: Object, required: true },
})

// Public profiles carry `age`; the athlete's own forms only know `birthDate`.
const age = computed(() => props.profile.age ?? (props.profile.birthDate ? ageOn(props.profile.birthDate) : null))
</script>

<template>
  <div class="stats">
    <div>
      <span class="eyebrow">Edad</span>
      <span class="figure">{{ age ?? '—' }}<small v-if="age !== null">años</small></span>
    </div>
    <div>
      <span class="eyebrow">Estatura</span>
      <span class="figure">{{ profile.heightCm ?? '—' }}<small v-if="profile.heightCm">cm</small></span>
    </div>
    <div>
      <span class="eyebrow">Peso</span>
      <span class="figure">{{ profile.weightKg ?? '—' }}<small v-if="profile.weightKg">kg</small></span>
    </div>
  </div>
</template>
