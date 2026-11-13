<script setup lang="ts">
import { withBase } from 'vitepress'

// Grid of linked feature cards for the Features overview page.
defineProps<{
  items: {
    title: string
    text: string
    link: string
    icon?: string
  }[]
}>()
</script>

<template>
  <div class="feature-cards">
    <a v-for="item in items" :key="item.link" class="feature-card" :href="withBase(item.link)">
      <span v-if="item.icon" class="feature-card-icon" aria-hidden="true">{{ item.icon }}</span>
      <span class="feature-card-title">{{ item.title }}</span>
      <span class="feature-card-text">{{ item.text }}</span>
      <span class="feature-card-more">Read more →</span>
    </a>
  </div>
</template>

<style scoped>
.feature-cards {
  display: grid;
  grid-template-columns: 1fr;
  gap: 16px;
  margin: 24px 0;
}

@media (min-width: 640px) {
  .feature-cards {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

.vp-doc a.feature-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 20px;
  border-radius: 12px;
  border: 1px solid var(--vp-c-divider);
  background-color: var(--vp-c-bg-soft);
  color: var(--vp-c-text-1);
  text-decoration: none;
  font-weight: 400;
  transition: border-color 0.25s;
}

.vp-doc a.feature-card:hover {
  border-color: var(--vp-c-brand-1);
}

.feature-card-icon {
  font-size: 24px;
  line-height: 1;
}

.feature-card-title {
  font-size: 18px;
  font-weight: 600;
  line-height: 1.4;
}

.feature-card-text {
  flex-grow: 1;
  font-size: 14px;
  line-height: 1.6;
  color: var(--vp-c-text-2);
}

.feature-card-more {
  font-size: 14px;
  font-weight: 500;
  color: var(--vp-c-brand-1);
}
</style>
