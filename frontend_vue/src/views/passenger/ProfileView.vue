<script setup>
import { ref, onMounted, computed } from 'vue'
import {
 User, CreditCard, Clock, MapPin, Star, Settings, ChevronRight,
 LogOut, Shield, Bell, Bus, Sun, Moon, Monitor
} from 'lucide-vue-next'

import { authService } from '../../services/auth'
import { usePassengerTheme } from '../../composables/usePassengerTheme'

const { currentPassengerTheme, setTheme } = usePassengerTheme()

const user = ref(authService.getUser() || {
 nome: 'Convidado',
 email: 'login@pgu.pt',
 tipo: 'Utilizador',
 nif: '--- --- ---',
 passeMensal: false,
})

onMounted(async () => {
 const localUser = authService.getUser()
 if (localUser && localUser.id && !localUser.id.startsWith('demo-')) {
 try {
 const res = await fetch(`/api/auth/profile/${localUser.id}`)
 const data = await res.json()
 if (data.status === 'sucesso') {
 user.value = { ...data.user, tipo: 'Utilizador' }
 localStorage.setItem('pgu_user', JSON.stringify(user.value))
 }
 } catch (e) {
 console.error('Erro ao carregar perfil real:', e)
 }
 }
})

function handleLogout() {
 authService.logoutPassenger()
}

const tripHistory = ref([
 { data: '20/04/2026', linha: 'L43', origem: 'Estação CP', destino: 'Universidade', hora: '08:15', duracao: '18 min' },
 { data: '19/04/2026', linha: 'L43', origem: 'Universidade', destino: 'Estação CP', hora: '17:30', duracao: '22 min' },
])

const comprasHistory = computed(() => user.value.compras || [])
const formatDate = (dateStr) => {
  if (!dateStr) return '';
  try {
    const d = new Date(dateStr)
    return d.toLocaleDateString('pt-PT', { day: 'numeric', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' })
  } catch (e) {
    return dateStr
  }
}

const menuItems = [
 { icon: CreditCard, label: 'Métodos de Pagamento', sub: 'Gerir cartões' },
 { icon: Star, label: 'Linhas Favoritas', sub: 'Configurar atalhos' },
 { icon: Bell, label: 'Notificações', sub: 'Ativas' },
 { icon: Shield, label: 'Privacidade e Dados', sub: 'RGPD' },
 { icon: Settings, label: 'Definições', sub: '' },
]
</script>

<template>
 <div class="profile-page">
 <!-- Profile Card -->
 <div class="profile-card">
 <div class="avatar">
 <span class="avatar-initials">
 {{ user.nome.split(' ').map(n => n[0]).join('').substring(0,2).toUpperCase() }}
 </span>
 </div>
 <div class="profile-info">
 <h2 class="profile-name">{{ user.nome }}</h2>
 <span class="profile-type">{{ user.tipo }}</span>
 <span class="profile-email">{{ user.email }}</span>
 <span class="profile-nif">NIF: {{ user.nif }}</span>
 </div>
 <div class="pass-badge" v-if="user.passeMensal">
 <Bus :size="14" />
 Passe Ativo
 </div>
 </div>

 <!-- Travel Stats -->
 <div class="stats-row">
 <div class="stat-card">
 <span class="stat-num">{{ tripHistory.length }}</span>
 <span class="stat-label">Viagens este mês</span>
 </div>
 <div class="stat-card">
 <span class="stat-num">2</span>
 <span class="stat-label">Linhas usadas</span>
 </div>
 <div class="stat-card">
 <span class="stat-num">97'</span>
 <span class="stat-label">Tempo total</span>
 </div>
 </div>

 <!-- Trip History -->
 <div class="section">
 <h3 class="section-title"><Clock :size="18" /> Histórico de Viagens</h3>
 <div class="trip-list">
 <div v-for="trip in tripHistory" :key="trip.data + trip.hora" class="trip-card">
 <div class="trip-left">
 <span class="trip-linha" :style="{background: trip.linha === 'L7' ? '#0284c7' : '#7c3aed'}">
 {{ trip.linha }}
 </span>
 <div class="trip-info">
 <span class="trip-route">{{ trip.origem }} → {{ trip.destino }}</span>
 <span class="trip-meta">{{ trip.data }} · {{ trip.hora }} · {{ trip.duracao }}</span>
 </div>
 </div>
 <ChevronRight :size="16" class="trip-arrow" />
 </div>
 </div>
 </div>

  <!-- Histórico de Pagamentos -->
  <div class="section">
  <h3 class="section-title"><CreditCard :size="18" /> Histórico de Pagamentos</h3>
  <div class="trip-list" v-if="comprasHistory.length > 0">
  <div v-for="compra in comprasHistory" :key="compra.id" class="trip-card">
  <div class="trip-left">
  <span class="trip-linha" :style="{background: compra.tipo === 'passe' ? '#10b981' : '#0284c7'}">
  {{ compra.tipo === 'passe' ? 'PASSE' : 'BILHETE' }}
  </span>
  <div class="trip-info">
  <span class="trip-route">{{ compra.nomeTipo }}</span>
  <span class="trip-meta">Pago: {{ compra.preco.toFixed(2) }}€ · {{ formatDate(compra.dataCompra) }}</span>
  </div>
  </div>
  <span class="status-badge" :class="compra.estado.toLowerCase()">{{ compra.estado }}</span>
  </div>
  </div>
  <div v-else class="empty-history">
  <span class="empty-text">Nenhuma compra registada.</span>
  </div>
  </div>

 <!-- Theme Switcher -->
 <div class="section">
 <h3 class="section-title"><Moon :size="18" /> Aparência</h3>
 <div class="theme-card">
 <button
 class="theme-option"
 :class="{ active: currentPassengerTheme === 'light' }"
 @click="setTheme('light')"
 >
 <Sun :size="20" class="theme-icon" />
 <span>Claro</span>
 </button>
 <button
 class="theme-option"
 :class="{ active: currentPassengerTheme === 'dark' }"
 @click="setTheme('dark')"
 >
 <Moon :size="20" class="theme-icon" />
 <span>Escuro</span>
 </button>
 <button
 class="theme-option"
 :class="{ active: currentPassengerTheme === 'auto' }"
 @click="setTheme('auto')"
 >
 <Monitor :size="20" class="theme-icon" />
 <span>Automático</span>
 </button>
 </div>
 </div>

 <!-- Menu Items -->
 <div class="section">
 <h3 class="section-title"><Settings :size="18" /> Configurações</h3>
 <div class="menu-list">
 <div v-for="item in menuItems" :key="item.label" class="menu-item">
 <component :is="item.icon" :size="20" class="menu-icon" />
 <div class="menu-text">
 <span class="menu-label">{{ item.label }}</span>
 <span v-if="item.sub" class="menu-sub">{{ item.sub }}</span>
 </div>
 <ChevronRight :size="16" class="menu-arrow" />
 </div>
 </div>
 </div>

 <!-- Logout -->
 <button class="logout-btn" @click="handleLogout">
 <LogOut :size="18" /> Terminar Sessão
 </button>
 </div>
</template>

<style scoped>
.profile-page { padding: 1.25rem; padding-bottom: 2rem; }

/* Profile Card */
.profile-card {
 background: linear-gradient(135deg, #0284c7, #7c3aed);
 border-radius: 1.25rem; padding: 1.5rem;
 color: #fff; position: relative;
 box-shadow: 0 8px 32px rgba(3,105,161,0.3);
 margin-bottom: 1.25rem;
}
.avatar {
 width: 56px; height: 56px; border-radius: 50%;
 background: rgba(255,255,255,0.2); border: 3px solid rgba(255,255,255,0.4);
 display: flex; align-items: center; justify-content: center;
 margin-bottom: 0.85rem;
}
.avatar-initials { font-size: 1.25rem; font-weight: 800; }
.profile-info { display: flex; flex-direction: column; }
.profile-name { font-size: 1.3rem; font-weight: 800; margin: 0 0 0.2rem; }
.profile-type { font-size: 0.85rem; opacity: 0.85; font-weight: 600; }
.profile-email { font-size: 0.78rem; opacity: 0.6; margin-top: 0.15rem; }
.profile-nif { font-size: 0.78rem; opacity: 0.6; margin-top: 0.05rem; }
.pass-badge {
 position: absolute; top: 1.25rem; right: 1.25rem;
 display: flex; align-items: center; gap: 0.35rem;
 background: rgba(255,255,255,0.2); padding: 0.35rem 0.85rem;
 border-radius: 2rem; font-size: 0.75rem; font-weight: 700;
}

/* Stats */
.stats-row { display: grid; grid-template-columns: repeat(3, 1fr); gap: 0.85rem; margin-bottom: 1.75rem; }
.stat-card {
 background: var(--bg-surface); border-radius: 1.25rem; padding: 1.25rem 0.75rem;
 text-align: center; box-shadow: 0 4px 20px rgba(0,0,0,0.05);
}
.stat-num { display: block; font-size: 1.35rem; font-weight: 800; color: #0284c7; }
.stat-label { font-size: 0.65rem; color: var(--text-muted); text-transform: uppercase; font-weight: 600; letter-spacing: 0.04em; }

/* Sections */
.section { margin-bottom: 1.5rem; }
.section-title {
 display: flex; align-items: center; gap: 0.5rem;
 font-size: 1rem; font-weight: 700; color: var(--text-main); margin: 0 0 0.85rem;
}

/* Theme Switcher */
.theme-card {
 display: flex; background: var(--bg-surface); padding: 0.5rem; border-radius: 1rem;
 box-shadow: 0 1px 4px rgba(0,0,0,0.04); gap: 0.5rem;
}
.theme-option {
 flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center;
 gap: 0.4rem; padding: 0.85rem 0.5rem; background: transparent; border: none; border-radius: 0.75rem;
 color: var(--text-muted); cursor: pointer; transition: all 0.2s ease;
}
.theme-option span { font-size: 0.75rem; font-weight: 600; }
.theme-option.active { background: var(--bg-primary); color: #0284c7; box-shadow: 0 2px 8px rgba(0,0,0,0.05); }
.theme-option.active .theme-icon { filter: drop-shadow(0 0 4px rgba(2, 132, 199, 0.4)); }

/* Trip List */
.trip-list { display: flex; flex-direction: column; gap: 0.75rem; }
.trip-card {
 display: flex; justify-content: space-between; align-items: center;
 background: var(--bg-surface); padding: 1rem 1.25rem; border-radius: 1.25rem;
 box-shadow: 0 4px 20px rgba(0,0,0,0.05);
}
.trip-left { display: flex; align-items: center; gap: 0.75rem; }
.trip-linha {
 color: #fff; font-weight: 800; font-size: 0.7rem; padding: 0.3rem 0.5rem;
 border-radius: 0.35rem; min-width: 32px; text-align: center;
}
.trip-info { display: flex; flex-direction: column; }
.trip-route { font-weight: 600; font-size: 0.88rem; color: var(--text-main); }
.trip-meta { font-size: 0.72rem; color: var(--text-muted); margin-top: 0.15rem; }
.trip-arrow { color: #cbd5e1; }

/* Menu */
.menu-list { display: flex; flex-direction: column; gap: 0.6rem; }
.menu-item {
 display: flex; align-items: center; gap: 1rem;
 background: var(--bg-surface); padding: 1.1rem 1.25rem; border-radius: 1.25rem;
 box-shadow: 0 4px 20px rgba(0,0,0,0.05);
}
.menu-icon { color: #0284c7; flex-shrink: 0; }
.menu-text { flex: 1; display: flex; flex-direction: column; }
.menu-label { font-weight: 600; font-size: 0.9rem; color: var(--text-main); }
.menu-sub { font-size: 0.75rem; color: var(--text-muted); }
.menu-arrow { color: #cbd5e1; flex-shrink: 0; }

/* Logout */
.logout-btn {
 display: flex; align-items: center; justify-content: center; gap: 0.5rem;
 width: 100%; padding: 1.15rem; border-radius: 1.25rem; margin-top: 2rem;
 background: #fef2f2; color: #ef4444; border: 1px solid #fecaca;
 font-weight: 700; font-size: 0.95rem; cursor: pointer;
 transition: all 0.15s, box-shadow 0.2s; box-shadow: 0 4px 20px rgba(0,0,0,0.02);
}
.logout-btn:active { background: #fee2e2; transform: scale(0.98); }

.status-badge {
  font-size: 0.72rem;
  font-weight: 700;
  padding: 0.25rem 0.6rem;
  border-radius: 2rem;
  text-transform: uppercase;
}
.status-badge.ativo {
  background: #d1fae5;
  color: #065f46;
}
.status-badge.expirado {
  background: #fee2e2;
  color: #991b1b;
}
.status-badge.utilizado {
  background: #f3f4f6;
  color: #374151;
}
.empty-history {
  background: var(--bg-surface);
  padding: 1.25rem;
  border-radius: 1.25rem;
  text-align: center;
  border: 1px dashed var(--border-light);
}
.empty-text {
  font-size: 0.85rem;
  color: var(--text-muted);
  font-weight: 600;
}
</style>
