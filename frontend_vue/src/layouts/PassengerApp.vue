<script setup>
import { onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { MapPin, Ticket, Bell, User, Home } from 'lucide-vue-next'
import { usePassengerTheme } from '../composables/usePassengerTheme'

const route = useRoute()
usePassengerTheme() // Initialize theme listeners on app load

const tabs = [
 { id: 'home', path: '/app', label: 'Início', icon: Home },
 { id: 'map', path: '/app/map', label: 'Mapa', icon: MapPin },
 { id: 'ticket', path: '/app/ticket', label: 'Bilhete', icon: Ticket },
 { id: 'alerts', path: '/app/alerts', label: 'Alertas', icon: Bell },
 { id: 'profile', path: '/app/profile', label: 'Perfil', icon: User },
]

onMounted(() => {
 const userAgent = navigator.userAgent || navigator.vendor || window.opera
 if (/iPad|iPhone|iPod/.test(userAgent) && !window.MSStream) {
 document.body.classList.add('is-ios')
 } else if (/android/i.test(userAgent)) {
 document.body.classList.add('is-android')
 }
})
</script>

<template>
 <div class="pwa-shell">
 <!-- App Header -->
 <header class="app-header">
 <div class="header-brand">
 <img src="/tublogo.png" alt="TUB" class="header-logo" />
 <span class="header-title">TUB<span class="header-accent">.</span>Go</span>
 </div>
 </header>

 <!-- Page Content -->
 <main class="app-content">
 <router-view v-slot="{ Component }">
 <transition name="slide" mode="out-in">
 <component :is="Component" />
 </transition>
 </router-view>
 </main>

 <!-- Bottom Tab Bar -->
 <nav class="tab-bar">
 <router-link
 v-for="tab in tabs"
 :key="tab.id"
 :to="tab.path"
 class="tab-item"
 :class="{ active: route.path === tab.path }"
 >
 <component :is="tab.icon" :size="22" />
 <span class="tab-label">{{ tab.label }}</span>
 </router-link>
 </nav>
 </div>
</template>

<style scoped>
.pwa-shell {
 display: flex;
 flex-direction: column;
 height: 100vh;
 height: 100dvh;
 width: 100%;
 max-width: 430px;
 margin: 0 auto;
 background: var(--bg-primary);
 position: relative;
 overflow: hidden;
 font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
}

/* Header */
.app-header {
 background: linear-gradient(135deg, #0369a1, #0284c7);
 padding: calc(0.85rem + env(safe-area-inset-top)) 1.25rem 0.85rem;
 display: flex;
 align-items: center;
 justify-content: space-between;
 box-shadow: 0 2px 12px rgba(3, 105, 161, 0.3);
}

:global(.is-android) .app-header {
 padding-top: 1.2rem;
}

.header-brand { display: flex; align-items: center; gap: 0.75rem; }

.header-logo { width: 32px; height: auto; filter: brightness(10); }
.header-title {
 font-size: 1.35rem;
 font-weight: 800;
 color: #fff;
 letter-spacing: -0.02em;
}
.header-accent { color: #38bdf8; }

/* Content */
.app-content {
 flex: 1;
 overflow-y: auto;
 overflow-x: hidden;
 -webkit-overflow-scrolling: touch;
}

/* Tab Bar */
.tab-bar {
 display: flex;
 justify-content: space-around;
 align-items: center;
 background: var(--bg-surface);
 border-top: 1px solid var(--border-light);
 padding: 0.5rem 0 calc(0.65rem + env(safe-area-inset-bottom));
 box-shadow: 0 -4px 20px rgba(0,0,0,0.06);
}

:global(.is-android) .tab-bar {
 padding-bottom: 1.25rem;
}
.tab-item {
 display: flex;
 flex-direction: column;
 align-items: center;
 gap: 0.2rem;
 text-decoration: none;
 color: #94a3b8;
 transition: all 0.2s ease;
 padding: 0.2rem 0.75rem;
 border-radius: 0.5rem;
}
.tab-item.active {
 color: #0284c7;
}
.tab-item.active svg {
 filter: drop-shadow(0 0 4px rgba(2, 132, 199, 0.4));
}
.tab-label {
 font-size: 0.65rem;
 font-weight: 600;
 text-transform: uppercase;
 letter-spacing: 0.03em;
}

/* Transitions */
.slide-enter-active, .slide-leave-active {
 transition: all 0.2s ease;
}
.slide-enter-from {
 opacity: 0;
 transform: translateX(20px);
}
.slide-leave-to {
 opacity: 0;
 transform: translateX(-20px);
}
</style>
