<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { searchAthletes } from '../api/search'
import { POSITIONS, REGIONS, SPORTS } from '../constants/catalog'
import { apiUrl } from '../api/http'
import { initials } from '../utils/format'

const PAGE_SIZE = 12
const FILTER_KEYS = ['sport', 'position', 'minAge', 'maxAge', 'region']
const AGE_DEBOUNCE_MS = 400

const route = useRoute()
const router = useRouter()

// Filters live in the URL, so "Volver a resultados" and shared links keep them.
const filters = reactive(Object.fromEntries(FILTER_KEYS.map((k) => [k, ''])))
const page = ref(0)
const result = ref({ items: [], page: 0, totalPages: 0, totalElements: 0, message: null })
const loading = ref(true)
const error = ref('')
const positions = computed(() => POSITIONS[filters.sport] ?? [])
const hasFilters = computed(() => FILTER_KEYS.some((k) => filters[k] !== ''))

let requestSeq = 0
async function run() {
  const seq = ++requestSeq
  loading.value = true
  error.value = ''
  try {
    const data = await searchAthletes({ ...filters, page: page.value, size: PAGE_SIZE })
    if (seq === requestSeq) result.value = data
  } catch (e) {
    if (seq === requestSeq) error.value = e.message
  } finally {
    if (seq === requestSeq) loading.value = false
  }
}

watch(
  () => route.query,
  (query) => {
    for (const k of FILTER_KEYS) filters[k] = typeof query[k] === 'string' ? query[k] : ''
    page.value = Number(query.pagina ?? 0) || 0
    run()
  },
  { immediate: true },
)

function pushQuery(nextPage = 0) {
  const query = Object.fromEntries(FILTER_KEYS.filter((k) => filters[k] !== '').map((k) => [k, filters[k]]))
  if (nextPage > 0) query.pagina = String(nextPage)
  router.replace({ query })
}

function onSportChange() {
  if (!positions.value.includes(filters.position)) filters.position = ''
  pushQuery()
}

let ageTimer = null
function onAgeInput() {
  clearTimeout(ageTimer)
  ageTimer = setTimeout(() => pushQuery(), AGE_DEBOUNCE_MS)
}

function clearFilters() {
  for (const k of FILTER_KEYS) filters[k] = ''
  pushQuery()
}
</script>

<template>
  <section>
    <div class="page-header">
      <div>
        <h1>Buscar talentos</h1>
        <p class="lead">Deportistas con perfil completo en todo el país.</p>
      </div>
    </div>

    <div class="search-layout">
      <form class="card stack" aria-label="Filtros" @submit.prevent>
        <span class="eyebrow">Filtros</span>
        <div class="field">
          <label for="filter-sport">Deporte</label>
          <select id="filter-sport" v-model="filters.sport" @change="onSportChange">
            <option value="">Todos</option>
            <option v-for="sport in SPORTS" :key="sport">{{ sport }}</option>
          </select>
        </div>
        <div class="field">
          <label for="filter-position">Posición o prueba</label>
          <select id="filter-position" v-model="filters.position" :disabled="!filters.sport" @change="pushQuery()">
            <option value="">{{ filters.sport ? 'Todas' : 'Primero elige el deporte' }}</option>
            <option v-for="position in positions" :key="position">{{ position }}</option>
          </select>
        </div>
        <fieldset class="field">
          <legend class="legend">Edad</legend>
          <div class="age-range">
            <input id="filter-min-age" v-model="filters.minAge" type="number" inputmode="numeric" min="8" max="30" placeholder="Desde" aria-label="Edad mínima" @input="onAgeInput" />
            <span aria-hidden="true">a</span>
            <input id="filter-max-age" v-model="filters.maxAge" type="number" inputmode="numeric" min="8" max="30" placeholder="Hasta" aria-label="Edad máxima" @input="onAgeInput" />
          </div>
        </fieldset>
        <div class="field">
          <label for="filter-region">Departamento</label>
          <select id="filter-region" v-model="filters.region" @change="pushQuery()">
            <option value="">Todo el país</option>
            <option v-for="region in REGIONS" :key="region">{{ region }}</option>
          </select>
        </div>
        <button type="button" class="btn btn-ghost" :disabled="!hasFilters" @click="clearFilters">Limpiar filtros</button>
      </form>

      <div class="stack" aria-live="polite">
        <div class="pager">
          <span><b style="color: var(--text)">{{ result.totalElements }}</b> deportista{{ result.totalElements === 1 ? '' : 's' }}</span>
          <span v-if="loading">Buscando…</span>
          <span v-else-if="result.totalPages">Página {{ result.page + 1 }} de {{ result.totalPages }}</span>
        </div>

        <p v-if="error" class="alert alert-error" role="alert">{{ error }}</p>
        <div v-else-if="!loading && result.items.length === 0" class="card empty">
          {{ result.message || 'Ningún deportista coincide con estos filtros.' }} Prueba ampliando el rango de edad o quitando el departamento.
        </div>
        <div v-else class="results">
          <RouterLink
            v-for="athlete in result.items"
            :key="athlete.athleteId"
            class="athlete-card"
            :to="{ name: 'athlete-profile', params: { athleteId: athlete.athleteId } }"
          >
            <div class="athlete-head">
              <img v-if="athlete.photoUrl" class="avatar" :src="apiUrl(athlete.photoUrl)" alt="" />
              <span v-else class="avatar" aria-hidden="true">{{ initials(athlete.fullName) }}</span>
              <div>
                <h3>{{ athlete.fullName }}</h3>
                <span class="hint">{{ athlete.sport }} · {{ athlete.position }}</span>
              </div>
            </div>
            <div class="facts">
              <span>{{ athlete.age }} años</span>
              <span>{{ athlete.region }}</span>
            </div>
          </RouterLink>
        </div>

        <div v-if="result.totalPages > 1" class="row" style="justify-content: center">
          <button type="button" class="btn btn-ghost" :disabled="page === 0 || loading" @click="pushQuery(page - 1)">Anterior</button>
          <button type="button" class="btn btn-ghost" :disabled="page + 1 >= result.totalPages || loading" @click="pushQuery(page + 1)">Siguiente</button>
        </div>
      </div>
    </div>
  </section>
</template>
