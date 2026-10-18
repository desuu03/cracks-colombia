<script setup>
defineProps({
  /** Supporting line under the pitch-side headline. */
  text: { type: String, required: true },
})
</script>

<template>
  <section class="auth">
    <div class="auth-pitch">
      <span class="eyebrow">Talento de todas las regiones</span>
      <h1><slot name="headline" /></h1>
      <p>{{ text }}</p>
    </div>
    <div class="auth-form">
      <slot />
    </div>
  </section>
</template>
