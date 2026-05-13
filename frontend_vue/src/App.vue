<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useTheme } from './composables/useTheme'
import { usePassengerTheme } from './composables/usePassengerTheme'
import { onMounted, watch } from 'vue'
import Sidebar from './components/Sidebar.vue'
import TopHeader from './components/TopHeader.vue'

const route = useRoute()
const isAuthPage = computed(() => ['admin-login', 'pax-login'].includes(route.name))
const isPassengerApp = computed(() => route.path.startsWith('/app') && !isAuthPage.value)

import { authService } from './services/auth'
const showAdminLayout = computed(() => {
  // Re-evaluates when route changes
  return !isAuthPage.value && !isPassengerApp.value && authService.isAdminLoggedIn()
})

const { initTheme: initAdminTheme } = useTheme()
const { initTheme: initPassengerTheme } = usePassengerTheme()

onMounted(() => {
  if (isPassengerApp.value) {
    initPassengerTheme()
  } else {
    initAdminTheme()
  }
})

watch(isPassengerApp, (newVal) => {
  if (newVal) {
    initPassengerTheme()
  } else {
    initAdminTheme()
  }
})
</script>

<template>
  <!-- Auth pages: full-screen, sem sidebar nem header -->
  <div v-if="isAuthPage" class="auth-wrapper">
    <router-view />
  </div>

  <!-- Passenger App: full-screen, no sidebar -->
  <div v-else-if="isPassengerApp" class="pwa-wrapper">
    <router-view />
  </div>

  <!-- Backoffice Dashboard: sidebar + header (só visível se logado) -->
  <div v-else-if="showAdminLayout" class="app-layout">
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

  <!-- Fallback (ex: 404 sem estar logado) -->
  <div v-else class="auth-wrapper">
    <router-view />
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
  height: 100dvh;
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
