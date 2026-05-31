<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useTheme } from './composables/useTheme'
import { usePassengerTheme } from './composables/usePassengerTheme'
import { onMounted, watch, onUnmounted } from 'vue'
import Sidebar from './components/Sidebar.vue'
import TopHeader from './components/TopHeader.vue'
import { useSidebar } from './composables/useSidebar'
import { ChevronRight } from 'lucide-vue-next'

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
const { isSidebarCollapsed, toggleSidebar } = useSidebar()

const handleKeyDown = (e) => {
  // Keyboard Shortcut: Ctrl + \ or Cmd + \
  if ((e.ctrlKey || e.metaKey) && e.key === '\\') {
    e.preventDefault()
    toggleSidebar()
  }
}

onMounted(() => {
  window.addEventListener('keydown', handleKeyDown)
  if (isPassengerApp.value) {
    initPassengerTheme()
  } else {
    initAdminTheme()
  }
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeyDown)
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
  <div v-else-if="showAdminLayout" class="app-layout" :class="{ 'sidebar-collapsed': isSidebarCollapsed }">
    <Sidebar :class="{ collapsed: isSidebarCollapsed }" />
    
    <!-- Floating Glass Edge Handle (Slack/Notion style) -->
    <div 
      v-if="isSidebarCollapsed" 
      class="floating-edge-handle" 
      @click="toggleSidebar"
      title="Expandir Sidebar (Ctrl + \)"
    >
      <div class="handle-inner">
        <ChevronRight :size="14" class="handle-chevron" />
      </div>
    </div>

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
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
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

/* Floating Edge Handle - Notion/Slack Style */
.floating-edge-handle {
  position: fixed;
  left: 0;
  top: 0;
  bottom: 0;
  width: 14px;
  z-index: 99;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  transition: width 0.2s ease;
}

.floating-edge-handle:hover {
  width: 24px;
}

.handle-inner {
  height: 56px;
  width: 0;
  opacity: 0;
  background: var(--bg-glass);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  border: 1px solid var(--border-light);
  border-left: none;
  border-radius: 0 8px 8px 0;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 4px 0 16px rgba(0, 0, 0, 0.15);
  color: var(--accent-blue);
}

.floating-edge-handle:hover .handle-inner {
  width: 20px;
  opacity: 1;
  box-shadow: 0 0 14px rgba(6, 182, 212, 0.35);
  border-color: rgba(6, 182, 212, 0.4);
}

.handle-chevron {
  transition: transform 0.2s ease;
}

.floating-edge-handle:hover .handle-chevron {
  transform: translateX(1px);
}
</style>
