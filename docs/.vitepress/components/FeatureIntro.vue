<script setup lang="ts">
import { withBase } from 'vitepress'

// Intro block for a feature page: copy on the left, a Play Store screenshot
// on the right. Stacks (screenshot below the copy) on narrow screens.
defineProps<{
  image?: string
  alt?: string
}>()
</script>

<template>
  <div class="feature-intro" :class="{ 'has-image': image }">
    <div class="feature-intro-copy">
      <slot />
    </div>
    <figure v-if="image" class="feature-intro-shot">
      <img :src="withBase(image)" :alt="alt" width="720" height="1280" loading="eager" decoding="async" />
    </figure>
  </div>
</template>

<style scoped>
.feature-intro {
  margin: 24px 0 8px;
}

.feature-intro.has-image {
  display: grid;
  grid-template-columns: 1fr;
  gap: 24px;
  align-items: start;
}

@media (min-width: 768px) {
  .feature-intro.has-image {
    grid-template-columns: minmax(0, 1fr) 260px;
    gap: 40px;
  }
}

.feature-intro-copy :deep(> :first-child) {
  margin-top: 0;
}

.feature-intro-shot {
  margin: 0 auto;
  width: min(300px, 80%);
}

@media (min-width: 768px) {
  .feature-intro-shot {
    width: 100%;
  }
}

.feature-intro-shot img {
  display: block;
  width: 100%;
  height: auto;
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.45);
}
</style>
