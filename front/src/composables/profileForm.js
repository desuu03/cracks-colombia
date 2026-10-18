import { computed, reactive, watch } from 'vue'
import { POSITIONS } from '../constants/catalog'

const FIELDS = ['sport', 'position', 'region', 'heightCm', 'weightKg', 'trajectory']
export const MAX_TRAJECTORY = 1000

/** Fields each create-profile step validates before moving on. */
export const STEP_FIELDS = {
  1: ['sport', 'position', 'region'],
  2: ['heightCm', 'weightKg'],
  3: ['trajectory'],
}

function checkRange(value, min, max, label, unit) {
  if (value === '' || value === null) return null
  const n = Number(value)
  if (!Number.isFinite(n) || n < min || n > max) return `${label} debe estar entre ${min} y ${max} ${unit}.`
  return null
}

/** Form state and validation for the athlete's UserProfileUpdate fields. */
export function useProfileForm() {
  const form = reactive(Object.fromEntries(FIELDS.map((f) => [f, ''])))
  const positions = computed(() => POSITIONS[form.sport] ?? [])

  // Keep the position valid when the sport changes.
  watch(() => form.sport, () => {
    if (!positions.value.includes(form.position)) form.position = ''
  })

  const errors = computed(() => {
    const e = {}
    if (!form.sport) e.sport = 'Elige tu deporte.'
    if (!form.position) e.position = 'Elige tu posición o prueba.'
    if (!form.region) e.region = 'Elige tu departamento.'
    const height = checkRange(form.heightCm, 100, 230, 'La estatura', 'cm')
    if (height) e.heightCm = height
    const weight = checkRange(form.weightKg, 30, 150, 'El peso', 'kg')
    if (weight) e.weightKg = weight
    if (form.trajectory.length > MAX_TRAJECTORY) e.trajectory = `La trayectoria no puede superar ${MAX_TRAJECTORY} caracteres.`
    return e
  })

  function fill(profile) {
    for (const f of FIELDS) form[f] = profile?.[f] ?? ''
  }

  function hasErrors(fields = FIELDS) {
    return fields.some((f) => errors.value[f])
  }

  function payload() {
    const toNumber = (v) => (v === '' || v === null ? null : Number(v))
    return {
      sport: form.sport,
      position: form.position,
      region: form.region,
      heightCm: toNumber(form.heightCm),
      weightKg: toNumber(form.weightKg),
      trajectory: form.trajectory.trim() || null,
    }
  }

  return { form, positions, errors, fill, hasErrors, payload }
}
