<script setup>
import { SPORTS, REGIONS } from '../constants/catalog'
import { MAX_TRAJECTORY } from '../composables/profileForm'

const props = defineProps({
  /** Reactive form from useProfileForm(); fields are edited in place. */
  form: { type: Object, required: true },
  positions: { type: Array, required: true },
  errors: { type: Object, required: true },
  /** Show validation messages (after the first submit attempt). */
  touched: { type: Boolean, default: false },
  /** Which groups to render: any of 'sport', 'physical', 'trajectory'. */
  sections: { type: Array, default: () => ['sport', 'physical', 'trajectory'] },
  /** Section headings, for the single-page edit form. */
  headings: { type: Boolean, default: false },
})

const show = (field) => (props.touched ? props.errors[field] : '')
</script>

<template>
  <template v-if="sections.includes('sport')">
    <span v-if="headings" class="eyebrow">Deportivo</span>
    <div class="grid-2">
      <div class="field">
        <label for="profile-sport">Deporte</label>
        <select id="profile-sport" v-model="form.sport">
          <option value="" disabled>Elige un deporte</option>
          <option v-for="sport in SPORTS" :key="sport">{{ sport }}</option>
        </select>
        <span v-if="show('sport')" class="field-error">{{ show('sport') }}</span>
      </div>
      <div class="field">
        <label for="profile-position">Posición o prueba</label>
        <select id="profile-position" v-model="form.position" :disabled="!form.sport">
          <option value="" disabled>{{ form.sport ? 'Elige una opción' : 'Primero elige el deporte' }}</option>
          <option v-for="position in positions" :key="position">{{ position }}</option>
        </select>
        <span v-if="show('position')" class="field-error">{{ show('position') }}</span>
      </div>
    </div>
    <div class="field">
      <label for="profile-region">Departamento</label>
      <select id="profile-region" v-model="form.region">
        <option value="" disabled>Elige tu departamento</option>
        <option v-for="region in REGIONS" :key="region">{{ region }}</option>
      </select>
      <span v-if="show('region')" class="field-error">{{ show('region') }}</span>
      <span v-else class="hint">Los ojeadores filtran por departamento.</span>
    </div>
  </template>

  <template v-if="sections.includes('physical')">
    <span v-if="headings" class="eyebrow">Físico</span>
    <div class="grid-2">
      <div class="field">
        <label for="profile-height">Estatura (cm)</label>
        <input id="profile-height" v-model="form.heightCm" type="number" inputmode="numeric" min="100" max="230" />
        <span v-if="show('heightCm')" class="field-error">{{ show('heightCm') }}</span>
      </div>
      <div class="field">
        <label for="profile-weight">Peso (kg)</label>
        <input id="profile-weight" v-model="form.weightKg" type="number" inputmode="numeric" min="30" max="150" />
        <span v-if="show('weightKg')" class="field-error">{{ show('weightKg') }}</span>
      </div>
    </div>
  </template>

  <template v-if="sections.includes('trajectory')">
    <span v-if="headings" class="eyebrow">Trayectoria</span>
    <div class="field">
      <label for="profile-trajectory">Trayectoria</label>
      <textarea id="profile-trajectory" v-model="form.trajectory" :maxlength="MAX_TRAJECTORY"></textarea>
      <span v-if="show('trajectory')" class="field-error">{{ show('trajectory') }}</span>
      <span v-else class="hint">
        Clubes, torneos, logros y convocatorias. {{ form.trajectory.length }} / {{ MAX_TRAJECTORY }} caracteres.
      </span>
    </div>
  </template>
</template>
