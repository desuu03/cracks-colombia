<script setup>
import { ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { session } from '../stores/session'

const router = useRouter()
const athleteId = ref('')

// Athlete search is not available in the backend yet; until then scouts open a
// gallery by the athlete's id.
function openGallery() {
  const id = athleteId.value.trim()
  if (id) router.push({ name: 'athlete-videos', params: { athleteId: id } })
}
</script>

<template>
  <section class="hero">
    <template v-if="!session.user">
      <h1>El talento de las regiones, a un clic de los clubes</h1>
      <p class="lead">
        Cracks Colombia conecta a jóvenes deportistas con ojeadores y entrenadores de todo el país, sin
        depender de torneos presenciales.
      </p>
      <div class="row">
        <RouterLink :to="{ name: 'register' }" class="btn btn-primary">Crear cuenta</RouterLink>
        <RouterLink :to="{ name: 'login' }" class="btn btn-ghost">Ya tengo cuenta</RouterLink>
      </div>
      <div class="features">
        <div class="card">
          <h2>Deportistas</h2>
          <p class="muted">Sube videos cortos de tus jugadas y habilidades para que te descubran.</p>
        </div>
        <div class="card">
          <h2>Ojeadores y entrenadores</h2>
          <p class="muted">Revisa el talento juvenil de todo el país desde tu oficina.</p>
        </div>
      </div>
    </template>

    <template v-else-if="session.user.role === 'DEPORTISTA'">
      <h1>Hola, {{ session.user.fullName }}</h1>
      <p class="lead">Gestiona tu galería y revisa el estado de tus videos.</p>
      <div class="row">
        <RouterLink :to="{ name: 'my-videos' }" class="btn btn-primary">Ir a mis videos</RouterLink>
        <RouterLink :to="{ name: 'athlete-videos', params: { athleteId: session.user.id } }" class="btn btn-ghost">
          Ver mi perfil público
        </RouterLink>
      </div>
    </template>

    <template v-else>
      <h1>Hola, {{ session.user.fullName }}</h1>
      <p class="lead">Abre la galería de un deportista con su identificador.</p>
      <form class="card stack narrow" style="margin: 0" @submit.prevent="openGallery">
        <div class="field">
          <label for="athlete-id">Identificador del deportista</label>
          <input id="athlete-id" v-model="athleteId" placeholder="00000000-0000-0000-0000-000000000000" />
          <span class="hint">La búsqueda por deporte, posición y región llegará en una próxima versión.</span>
        </div>
        <button type="submit" class="btn btn-primary" :disabled="!athleteId.trim()">Ver videos</button>
      </form>
    </template>
  </section>
</template>
