<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { Bell, ShieldCheck, Database, FlaskConical, LogOut, Clock } from 'lucide-vue-next'
import { authService } from '../services/auth'

const isDemo = ref(false)
const adminUser = ref(null)
const sessionMinutes = ref(0)
let sessionTimer = null

onMounted(() => {
  isDemo.value = localStorage.getItem('pgu_demo_mode') === 'true'
  const userStr = localStorage.getItem('pgu_admin_user')
  if (userStr) adminUser.value = JSON.parse(userStr)

  // Atualizar tempo de sessão restante a cada minuto
  updateSessionTime()
  sessionTimer = setInterval(updateSessionTime, 60000)
})

onUnmounted(() => {
  if (sessionTimer) clearInterval(sessionTimer)
})

function updateSessionTime() {
  sessionMinutes.value = authService.getAdminSessionRemaining()
}

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

      <!-- Temporizador de Sessão -->
      <div class="session-timer" :class="{ 'session-warning': sessionMinutes < 15 }">
        <Clock :size="14" />
        <span>{{ sessionMinutes }}min</span>
      </div>

      <div class="status-badge" :style="{ borderColor: isDemo ? '#f59e0b' : '#10b981', color: isDemo ? '#f59e0b' : '#10b981', background: isDemo ? 'rgba(245,158,11,0.1)' : 'rgba(16,185,129,0.1)' }">
        <ShieldCheck :size="16" class="status-icon" />
        <span class="fira-code">{{ isDemo ? 'SIMULAÇÃO ATIVA' : 'SISTEMA ONLINE' }}</span>
      </div>
      
      <button class="icon-btn">
        <Bell :size="20" />
        <span class="notification-dot"></span>
      </button>

      <button class="icon-btn logout-btn" @click="handleLogout" title="Sair do Sistema">
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

.demo-toggle {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  background: rgba(255, 255, 255, 0.05);
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
  background: rgba(255, 255, 255, 0.1);
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
  background: rgba(255, 255, 255, 0.05);
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
