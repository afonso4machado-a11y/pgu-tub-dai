<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useTheme } from './composables/useTheme'
import { onMounted } from 'vue'
import Sidebar from './components/Sidebar.vue'
import TopHeader from './components/TopHeader.vue'

const route = useRoute()
const isPassengerApp = computed(() => route.path.startsWith('/app'))

const { initTheme } = useTheme()

onMounted(() => {
  initTheme()
})
</script>

<template>
  <!-- Passenger App: full-screen, no sidebar -->
  <div v-if="isPassengerApp" class="pwa-wrapper">
    <router-view />
  </div>

  <!-- Backoffice Dashboard: sidebar + header -->
  <div v-else class="app-layout">
    <Sidebar />
    <main class="main-content">
      <TopHeader />
      <div class="view-container">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </div>
    </main>
  </div>
</template>

<style scoped>
.app-layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
  background: radial-gradient(circle at top right, var(--bg-surface), var(--bg-primary));
}

.pwa-wrapper {
  height: 100vh;
  overflow: hidden;
  background: var(--bg-primary);
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  position: relative;
}

.view-container {
  padding: 2rem;
  max-width: 1600px;
  margin: 0 auto;
  width: 100%;
}

/* Page Transitions */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}
.fade-enter-from {
  opacity: 0;
  transform: translateY(10px);
}
.fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}
</style>
