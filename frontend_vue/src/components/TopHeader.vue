<script setup>
import { ref, onMounted } from 'vue'
import { Bell, ShieldCheck, Database, FlaskConical, LogOut, Sun, Moon, Monitor } from 'lucide-vue-next'
import { useTheme } from '../composables/useTheme'
import { authService } from '../services/auth'

const isDemo = ref(false)
const adminUser = ref(null)
const { currentTheme, setTheme } = useTheme()

onMounted(() => {
  isDemo.value = localStorage.getItem('pgu_demo_mode') === 'true'
  adminUser.value = authService.getAdminUser()
})

function toggleDemo() {
  isDemo.value = !isDemo.value
  localStorage.setItem('pgu_demo_mode', isDemo.value)
  window.location.reload()
}

function handleLogout() {
  authService.logoutAdmin()
}
</script>

<template>
  <header class="top-header glass-panel">
    <div class="header-left">
      <h2 class="page-title title-glow">{{ $route.name ? $route.name.charAt(0).toUpperCase() + $route.name.slice(1) : '' }}</h2>
    </div>
    
    <div class="header-actions">

      <!-- Theme Switcher -->
      <div class="theme-switcher">
        <button
          class="theme-btn"
          :class="{ active: currentTheme === 'light' }"
          @click="setTheme('light')"
          title="Modo Claro"
          aria-label="Modo Claro"
        >
          <Sun :size="16" />
        </button>
        <button
          class="theme-btn"
          :class="{ active: currentTheme === 'dark' }"
          @click="setTheme('dark')"
          title="Modo Escuro"
          aria-label="Modo Escuro"
        >
          <Moon :size="16" />
        </button>
        <button
          class="theme-btn"
          :class="{ active: currentTheme === 'auto' }"
          @click="setTheme('auto')"
          title="Modo Automático"
          aria-label="Modo Automático"
        >
          <Monitor :size="16" />
        </button>
      </div>

      <!-- Demo Mode Toggle -->
      <button 
        class="demo-toggle" 
        :class="{ 'is-demo': isDemo }" 
        @click="toggleDemo"
        :title="isDemo ? 'Mudar para Dados Reais' : 'Mudar para Dados Demo'"
      >
        <component :is="isDemo ? FlaskConical : Database" :size="16" />
        <span>{{ isDemo ? 'MODO DEMO' : 'DADOS REAIS' }}</span>
      </button>

      <div v-if="adminUser" class="admin-profile">
        <span class="admin-name">{{ adminUser.nome }}</span>
        <span class="admin-email">{{ adminUser.email }}</span>
      </div>

      <div class="status-badge" :class="isDemo ? 'status-demo' : 'status-online'">
        <ShieldCheck :size="16" class="status-icon" />
        <span class="fira-code">{{ isDemo ? 'SIMULAÇÃO ATIVA' : 'SISTEMA ONLINE' }}</span>
      </div>
      
      <button class="icon-btn" aria-label="Notificações">
        <Bell :size="20" />
        <span class="notification-dot"></span>
      </button>

      <button class="icon-btn logout-btn" @click="handleLogout" title="Sair do Sistema" aria-label="Sair do Sistema">
        <LogOut :size="20" />
      </button>
    </div>
  </header>
</template>

<style scoped>
.top-header {
  border-radius: 0;
  border-top: none;
  border-left: none;
  border-right: none;
  padding: 1.25rem 2rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  position: sticky;
  top: 0;
  z-index: 5;
}

.page-title {
  font-size: 1.5rem;
  margin: 0;
  text-transform: capitalize;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 1.5rem;
}

.status-badge {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.4rem 1rem;
  border-radius: 2rem;
  font-size: 0.75rem;
  border: 1px solid rgba(16, 185, 129, 0.2);
  box-shadow: 0 0 10px rgba(16, 185, 129, 0.1);
  transition: all 0.3s ease;
}


.theme-switcher {
  display: flex;
  background: var(--bg-input);
  border-radius: 0.75rem;
  padding: 0.2rem;
  border: 1px solid var(--border-light);
}

.theme-btn {
  background: transparent;
  border: none;
  color: var(--text-muted);
  padding: 0.4rem;
  border-radius: 0.5rem;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.theme-btn:hover {
  color: var(--text-main);
  background: var(--bg-hover);
}

.theme-btn.active {
  background: var(--bg-surface);
  color: var(--accent-blue);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  border: 1px solid var(--border-light);
}

.status-demo {
  border-color: var(--warning);
  color: var(--warning);
  background: rgba(245, 158, 11, 0.1);
}

.status-online {
  border-color: var(--success);
  color: var(--success);
  background: rgba(16, 185, 129, 0.1);
}

.demo-toggle {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  background: var(--bg-hover);
  border: 1px solid var(--border);
  color: var(--text-muted);
  padding: 0.4rem 0.85rem;
  border-radius: 0.75rem;
  cursor: pointer;
  font-size: 0.7rem;
  font-weight: 700;
  font-family: 'Fira Code', monospace;
  transition: all 0.3s ease;
}

.demo-toggle:hover {
  background: var(--bg-hover-strong);
  border-color: var(--text-muted);
}

.demo-toggle.is-demo {
  background: rgba(245, 158, 11, 0.15);
  border-color: #f59e0b;
  color: #f59e0b;
  box-shadow: 0 0 15px rgba(245, 158, 11, 0.2);
}

.icon-btn {
  background: transparent;
  border: none;
  color: var(--text-muted);
  cursor: pointer;
  position: relative;
  transition: color 0.2s;
  padding: 0.5rem;
}
.icon-btn:hover {
  color: var(--text-main);
}
.logout-btn:hover {
  color: var(--danger);
}
.admin-profile {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  margin-right: 0.5rem;
}
.admin-name {
  font-size: 0.85rem;
  font-weight: 700;
  color: var(--text-main);
}
.admin-email {
  font-size: 0.7rem;
  color: var(--text-muted);
}
.notification-dot {
  position: absolute;
  top: 5px;
  right: 6px;
  width: 8px;
  height: 8px;
  background: var(--danger);
  border-radius: 50%;
  box-shadow: 0 0 8px var(--danger);
}

.session-timer {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.3rem 0.7rem;
  border-radius: 2rem;
  font-size: 0.7rem;
  font-weight: 700;
  font-family: 'Fira Code', monospace;
  color: var(--text-muted);
  background: var(--bg-hover);
  border: 1px solid var(--border-light);
  transition: all 0.3s ease;
}

.session-timer.session-warning {
  color: #f59e0b;
  background: rgba(245, 158, 11, 0.1);
  border-color: rgba(245, 158, 11, 0.3);
  animation: pulse-warning 2s ease-in-out infinite;
}

@keyframes pulse-warning {
  0%, 100% { box-shadow: 0 0 0 0 rgba(245, 158, 11, 0); }
  50% { box-shadow: 0 0 8px 2px rgba(245, 158, 11, 0.2); }
}
</style>
