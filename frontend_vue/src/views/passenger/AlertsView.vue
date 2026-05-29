<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { ChevronRight } from 'lucide-vue-next'

import { authService } from '../../services/auth'

import { apiFetch, isDemoMode } from '../../services/api.js'
const alerts = ref([])

const user = authService.getUser()
const linhaFavorita = ref(user?.linhasFavoritas && user.linhasFavoritas.length > 0 ? user.linhasFavoritas[0] : 'L43')

const sampleAlerts = [
 { id: 1, tipo: 'PERTURBAÇÃO', linha: 'L43', msg: 'Desvio temporário na Av. da Liberdade devido a obras. Paragem "Tribunal" suspensa até 30/04.', tempo: '12 min', lido: false },
 { id: 2, tipo: 'ATRASO', linha: 'L7', msg: 'Atraso estimado de 8 minutos na linha L7 sentido Celeirós.', tempo: '45 min', lido: false },
 { id: 3, tipo: 'INFO', linha: 'Todas', msg: 'Novos horários de verão entram em vigor a 1 de maio. Consulte o site dos TUB.', tempo: '2 h', lido: true },
 { id: 4, tipo: 'LOTAÇÃO', linha: 'L43', msg: 'Autocarro das 08:15 na L43 com lotação elevada (87%). Considere o próximo às 08:30.', tempo: '3 h', lido: true },
]

async function fetchAlerts() {
 try {
  const { data } = await apiFetch('/dashboard')
  if (data.status === 'sucesso') {
   const apiAlerts = (data.dashboard?.avisosRecentes || []).map((a, i) => ({
    id: 100 + i,
    tipo: 'SISTEMA',
    linha: a.autocarroId || '',
    msg: a.mensagem,
    tempo: 'agora',
    lido: false,
   }))
   
   if (isDemoMode()) {
    // Merge: keep read state of existing alerts
    const existingIds = new Set(alerts.value.map(a => a.id))
    const newApiAlerts = apiAlerts.filter(a => !existingIds.has(a.id))
    alerts.value = [...newApiAlerts, ...alerts.value.filter(a => a.id >= 1 && a.id < 100)]
    if (alerts.value.length === 0) alerts.value = sampleAlerts
   } else {
    // Modo Dados Reais: apenas alertas da API, sem misturar samples
    alerts.value = apiAlerts
   }
  } else {
   if (isDemoMode()) {
    if (alerts.value.length === 0) alerts.value = sampleAlerts
   } else {
    alerts.value = []
   }
  }
 } catch(e) {
  if (isDemoMode()) {
   if (alerts.value.length === 0) alerts.value = sampleAlerts
  } else {
   alerts.value = []
  }
 }
}

onMounted(fetchAlerts)

// Polling a cada 5 segundos para alertas em tempo real
let _alertsInterval = null
onMounted(() => {
 _alertsInterval = setInterval(fetchAlerts, 5000)
})
onUnmounted(() => { if (_alertsInterval) clearInterval(_alertsInterval) })

function tipoColor(tipo) {
 if (tipo === 'PERTURBAÇÃO') return '#ef4444'
 if (tipo === 'ATRASO') return '#eab308'
 if (tipo === 'LOTAÇÃO') return '#f97316'
 if (tipo === 'SISTEMA') return '#0284c7'
 return '#64748b'
}

function markRead(alert) {
 alert.lido = true
}

const unreadCount = computed(() => alerts.value.filter(a => !a.lido).length)
</script>

<template>
 <div class="alerts-page">
 <!-- Header -->
 <div class="alerts-header">
 <h2 class="page-title">Alertas</h2>
 <div v-if="unreadCount > 0" class="unread-badge">{{ unreadCount }} novos</div>
 </div>

 <!-- Favorites filter -->
 <div class="fav-filter">
 <button class="filter-chip active">
 Todos
 </button>
 <button class="filter-chip">
 Linha {{ linhaFavorita }}
 </button>
 </div>

 <!-- Alert List -->
 <div class="alerts-list">
 <div
 v-for="alert in alerts"
 :key="alert.id"
 class="alert-card"
 :class="{ 'alert-unread': !alert.lido }"
 @click="markRead(alert)"
 >
 <div class="ac-content">
 <div class="ac-top">
 <span class="ac-tipo" :style="{color: tipoColor(alert.tipo)}">{{ alert.tipo }}</span>
 <span v-if="alert.linha" class="ac-linha">{{ alert.linha }}</span>
 </div>
 <p class="ac-msg">{{ alert.msg }}</p>
 <span class="ac-time">{{ alert.tempo }} atrás</span>
 </div>
 <div v-if="!alert.lido" class="ac-dot"></div>
 </div>

 <div v-if="alerts.length === 0" class="empty">
 <p>Sem alertas de momento</p>
 <p class="empty-sub">Receberá notificações sobre as suas linhas favoritas</p>
 </div>
 </div>

 <!-- Settings hint -->
 <div class="settings-hint">
 <span>Gerir notificações da linha favorita</span>
 <ChevronRight :size="16" />
 </div>
 </div>
</template>

<style scoped>
.alerts-page { padding: 1.25rem; padding-bottom: 2rem; }

.alerts-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem; }
.page-title { font-size: 1.5rem; font-weight: 800; color: var(--text-main); margin: 0; }
.unread-badge {
 background: #ef4444; color: #fff; padding: 0.25rem 0.75rem;
 border-radius: 2rem; font-size: 0.75rem; font-weight: 700;
}

/* Filters */
.fav-filter { display: flex; gap: 0.5rem; margin-bottom: 1.25rem; }
.filter-chip {
 display: flex; align-items: center; gap: 0.35rem;
 padding: 0.5rem 1rem; border-radius: 2rem;
 border: 1px solid var(--border-light); background: var(--bg-surface);
 font-size: 0.8rem; font-weight: 600; color: var(--text-muted);
 cursor: pointer; transition: all 0.2s;
}
.filter-chip.active {
 background: #0284c7; color: #fff; border-color: #0284c7;
}

/* Alert List */
.alerts-list { display: flex; flex-direction: column; gap: 0.85rem; }

.alert-card {
 display: flex; align-items: flex-start; gap: 0.85rem;
 background: var(--bg-surface); padding: 1.25rem; border-radius: 1.25rem;
 box-shadow: 0 4px 20px rgba(0,0,0,0.05);
 transition: all 0.2s, box-shadow 0.2s; position: relative;
}
.alert-card:active { transform: scale(0.98); }
.alert-unread { background: #f0f9ff; border-left: 4px solid #0284c7; }

.ac-icon {
 width: 36px; height: 36px; border-radius: 50%;
 background: var(--bg-primary); display: flex; align-items: center; justify-content: center;
 flex-shrink: 0;
}
.ac-content { flex: 1; min-width: 0; }
.ac-top { display: flex; align-items: center; gap: 0.5rem; margin-bottom: 0.35rem; }
.ac-tipo { font-size: 0.72rem; font-weight: 800; text-transform: uppercase; letter-spacing: 0.04em; }
.ac-linha {
 background: var(--bg-hover); padding: 0.15rem 0.5rem; border-radius: 0.3rem;
 font-size: 0.7rem; font-weight: 700; color: #334155;
}
.ac-msg { margin: 0; font-size: 0.95rem; color: #334155; line-height: 1.5; font-weight: 500; }
.ac-time {
 display: flex; align-items: center; gap: 0.3rem;
 font-size: 0.72rem; color: var(--text-muted); margin-top: 0.4rem;
}
.ac-dot {
 width: 8px; height: 8px; border-radius: 50%; background: #0284c7;
 flex-shrink: 0; margin-top: 0.5rem;
}

.empty { text-align: center; padding: 3rem 1rem; color: var(--text-muted); }
.empty-icon { opacity: 0.3; margin-bottom: 0.5rem; }
.empty-sub { font-size: 0.8rem; }

.settings-hint {
 display: flex; align-items: center; gap: 0.5rem;
 margin-top: 2rem; padding: 1.25rem;
 background: var(--bg-surface); border-radius: 1.25rem;
 color: var(--text-muted); font-size: 0.9rem; font-weight: 600;
 box-shadow: 0 4px 20px rgba(0,0,0,0.05);
}
.settings-hint span { flex: 1; }
</style>
