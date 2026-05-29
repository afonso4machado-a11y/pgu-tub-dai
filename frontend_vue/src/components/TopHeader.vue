<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { ShieldCheck, LogOut, Sun, Moon, Monitor, ToggleLeft, ToggleRight, Menu } from 'lucide-vue-next'
import { useTheme } from '../composables/useTheme'
import { authService } from '../services/auth'
import { demoModeRef, toggleDemoMode } from '../services/api'
import { useSidebar } from '../composables/useSidebar'

const adminUser = ref(null)
const { currentTheme, setTheme } = useTheme()
const { isSidebarCollapsed, toggleSidebar } = useSidebar()

// Usa diretamente o ref reativo — sem interval, sem polling
const isDemo = demoModeRef

onMounted(() => {
 adminUser.value = authService.getAdminUser()
})

function handleToggleDemo() {
 toggleDemoMode()
}

function handleLogout() {
 authService.logoutAdmin()
}
</script>

<template>
 <header class="top-header glass-panel">
 <div class="header-left">
 <button 
 class="sidebar-toggle-btn" 
 @click="toggleSidebar" 
 :title="isSidebarCollapsed ? 'Mostrar Sidebar (Ctrl + \\)' : 'Minimizar Sidebar (Ctrl + \\)'"
 :class="{ 'sidebar-collapsed': isSidebarCollapsed }"
 aria-label="Alternar Sidebar"
 >
 <Menu :size="20" class="toggle-icon" />
 </button>
 </div>
 
 <div class="header-actions">

 <!-- Mode Toggle -->
 <button class="mode-toggle" :class="isDemo ? 'mode-demo' : 'mode-live'" @click="handleToggleDemo" :title="isDemo ? 'Mudar para Dados Reais' : 'Mudar para Simulação'">
 <component :is="isDemo ? ToggleRight : ToggleLeft" :size="20" />
 <span class="mode-label fira-code">{{ isDemo ? 'SIMULAÇÃO' : 'DADOS REAIS' }}</span>
 </button>

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

 <div v-if="adminUser" class="admin-profile">
 <span class="admin-name">{{ adminUser.nome }}</span>
 <span class="admin-email">{{ adminUser.email }}</span>
 </div>

 <div class="status-badge" :class="isDemo ? 'status-demo' : 'status-online'">
 <ShieldCheck :size="16" class="status-icon" />
 <span class="fira-code">{{ isDemo ? 'SIMULAÇÃO ATIVA' : 'SISTEMA ONLINE' }}</span>
 </div>
 
 <button class="icon-btn logout-btn" @click="handleLogout" title="Sair do Sistema" aria-label="Sair do Sistema">
 <LogOut :size="20" />
 </button>
 </div>
 </header>
</template>

<style scoped>
/* Sidebar Toggle Button */
.sidebar-toggle-btn {
  background: transparent;
  border: 1px solid var(--border-light);
  color: var(--text-muted);
  cursor: pointer;
  padding: 0.5rem;
  border-radius: 0.5rem;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.sidebar-toggle-btn:hover {
  color: var(--text-main);
  background: var(--bg-hover);
  border-color: var(--accent-blue);
  box-shadow: 0 0 10px rgba(6, 182, 212, 0.15);
}

.toggle-icon {
  transition: transform 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.sidebar-toggle-btn.sidebar-collapsed .toggle-icon {
  transform: rotate(180deg);
  color: var(--accent-blue);
}

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

/* Mode Toggle Button */
.mode-toggle {
 display: flex;
 align-items: center;
 gap: 0.5rem;
 padding: 0.45rem 1rem;
 border-radius: 2rem;
 border: 1px solid var(--border-light);
 cursor: pointer;
 font-size: 0.75rem;
 font-weight: 700;
 transition: all 0.3s ease;
 white-space: nowrap;
}

.mode-live {
 background: rgba(16, 185, 129, 0.08);
 color: var(--success);
 border-color: rgba(16, 185, 129, 0.3);
}
.mode-live:hover {
 background: rgba(16, 185, 129, 0.15);
 box-shadow: 0 0 12px rgba(16, 185, 129, 0.15);
}

.mode-demo {
 background: rgba(245, 158, 11, 0.08);
 color: var(--warning);
 border-color: rgba(245, 158, 11, 0.3);
}
.mode-demo:hover {
 background: rgba(245, 158, 11, 0.15);
 box-shadow: 0 0 12px rgba(245, 158, 11, 0.15);
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
