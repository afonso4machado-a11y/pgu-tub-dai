<script setup>
import { computed } from 'vue'

const props = defineProps({
  width: {
    type: String,
    default: '100%'
  },
  height: {
    type: String,
    default: '1.25rem'
  },
  shape: {
    type: String,
    default: 'rect', // 'circle' | 'rect' | 'line'
    validator: (val) => ['circle', 'rect', 'line'].includes(val)
  },
  animation: {
    type: String,
    default: 'shimmer', // 'shimmer' | 'pulse' | 'none'
    validator: (val) => ['shimmer', 'pulse', 'none'].includes(val)
  },
  customClass: {
    type: String,
    default: ''
  }
})

const skeletonClass = computed(() => {
  return [
    'skeleton-base',
    props.shape === 'circle' ? 'rounded-full' : props.shape === 'line' ? 'rounded' : 'rounded-lg',
    props.animation === 'shimmer' ? 'skeleton-shimmer' : '',
    props.animation === 'pulse' ? 'skeleton-pulse' : '',
    props.customClass
  ].filter(Boolean).join(' ')
})

const skeletonStyle = computed(() => {
  return {
    width: props.shape === 'circle' ? props.height : props.width,
    height: props.height
  }
})
</script>

<template>
  <div
    :class="skeletonClass"
    :style="skeletonStyle"
    role="progressbar"
    aria-busy="true"
    aria-label="A carregar dados do sistema..."
    aria-valuemin="0"
    aria-valuemax="100"
  ></div>
</template>

<style scoped>
.rounded-full { border-radius: 9999px; }
.rounded { border-radius: 0.25rem; }
.rounded-lg { border-radius: 0.5rem; }
</style>
