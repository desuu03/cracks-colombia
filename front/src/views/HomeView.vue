<script setup>
import { RouterLink } from 'vue-router'
import { session } from '../stores/session'
import { ROLE_LABELS } from '../utils/format'
</script>

<template>
  <section v-if="session.user" class="card empty">
    <h1 style="color: var(--text)">Hola, {{ session.user.fullName }}</h1>
    <p>
      Las herramientas para el rol {{ ROLE_LABELS[session.user.role] ?? session.user.role }} todavía no están
      disponibles en esta versión.
    </p>
  </section>

  <section v-else class="landing">
    <span class="eyebrow">Talento de todas las regiones</span>
    <h1>El talento de las regiones, a un clic de los clubes</h1>
    <p class="lead">
      Cracks Colombia conecta a jóvenes deportistas con ojeadores y entrenadores de todo el país, sin depender de
      torneos presenciales.
    </p>
    <div class="row">
      <RouterLink :to="{ name: 'register' }" class="btn btn-primary">Crear cuenta</RouterLink>
      <RouterLink :to="{ name: 'login' }" class="btn btn-ghost">Ya tengo cuenta</RouterLink>
    </div>
    <div class="features">
      <div class="card">
        <h2>Deportistas</h2>
        <p class="muted">Arma tu perfil, sube videos cortos de tus jugadas y responde a los clubes que te contacten.</p>
      </div>
      <div class="card">
        <h2>Ojeadores y entrenadores</h2>
        <p class="muted">Filtra por deporte, posición, edad y departamento, y solicita contacto o una prueba.</p>
      </div>
    </div>
  </section>
</template>
