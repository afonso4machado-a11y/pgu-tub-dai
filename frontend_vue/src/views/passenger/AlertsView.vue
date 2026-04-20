<script setup>
import { ref, onMounted, computed } from 'vue'
import { Bell, AlertTriangle, Info, Clock, Star, Settings, ChevronRight } from 'lucide-vue-next'

const apiUrl = '/api'
const alerts = ref([])
const linhaFavorita = ref('L43')

const sampleAlerts = [
  { id: 1, tipo: 'PERTURBAÇÃO', linha: 'L43', msg: 'Desvio temporário na Av. da Liberdade devido a obras. Paragem "Tribunal" suspensa até 30/04.', tempo: '12 min', lido: false },
  { id: 2, tipo: 'ATRASO', linha: 'L7', msg: 'Atraso estimado de 8 minutos na linha L7 sentido Celeirós.', tempo: '45 min', lido: false },
  { id: 3, tipo: 'INFO', linha: 'Todas', msg: 'Novos horários de verão entram em vigor a 1 de maio. Consulte o site dos TUB.', tempo: '2 h', lido: true },
  { id: 4, tipo: 'LOTAÇÃO', linha: 'L43', msg: 'Autocarro das 08:15 na L43 com lotação elevada (87%). Considere o próximo às 08:30.', tempo: '3 h', lido: true },
]

onMounted(async () => {
  // Merge real alerts from API with sample passenger-facing alerts
  try {
    const res = await fetch(`${apiUrl}/dashboard`)
    const data = await res.json()
    if (data.status === 'sucesso') {
      const apiAlerts = (data.dashboard?.avisosRecentes || []).map((a, i) => ({
        id: 100 + i,
        tipo: 'SISTEMA',
        linha: a.autocarroId || '',
        msg: a.mensagem,
        tempo: 'agora',
        lido: false,
      }))
      alerts.value = [...apiAlerts, ...sampleAlerts]
    } else {
      alerts.value = sampleAlerts
    }
  } catch(e) {
    alerts.value = sampleAlerts
  }
})

function tipoIcon(tipo) {
  if (tipo === 'PERTURBAÇÃO' || tipo === 'ATRASO') return AlertTriangle
  if (tipo === 'LOTAÇÃO') return Bell
  return Info
}

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
        <Bell :size="14" /> Todos
      </button>
      <button class="filter-chip">
        <Star :size="14" /> Linha {{ linhaFavorita }}
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
        <div class="ac-icon" :style="{color: tipoColor(alert.tipo)}">
          <component :is="tipoIcon(alert.tipo)" :size="20" />
        </div>
        <div class="ac-content">
          <div class="ac-top">
            <span class="ac-tipo" :style="{color: tipoColor(alert.tipo)}">{{ alert.tipo }}</span>
            <span v-if="alert.linha" class="ac-linha">{{ alert.linha }}</span>
          </div>
          <p class="ac-msg">{{ alert.msg }}</p>
          <span class="ac-time"><Clock :size="12" /> {{ alert.tempo }} atrás</span>
        </div>
        <div v-if="!alert.lido" class="ac-dot"></div>
      </div>

      <div v-if="alerts.length === 0" class="empty">
        <Bell :size="40" class="empty-icon" />
        <p>Sem alertas de momento</p>
        <p class="empty-sub">Receberá notificações sobre as suas linhas favoritas</p>
      </div>
    </div>

    <!-- Settings hint -->
    <div class="settings-hint">
      <Settings :size="16" />
      <span>Gerir notificações da linha favorita</span>
      <ChevronRight :size="16" />
    </div>
  </div>
</template>

<style scoped>
.alerts-page { padding: 1.25rem; padding-bottom: 2rem; }

.alerts-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem; }
.page-title { font-size: 1.5rem; font-weight: 800; color: #0f172a; margin: 0; }
.unread-badge {
  background: #ef4444; color: #fff; padding: 0.25rem 0.75rem;
  border-radius: 2rem; font-size: 0.75rem; font-weight: 700;
}

/* Filters */
.fav-filter { display: flex; gap: 0.5rem; margin-bottom: 1.25rem; }
.filter-chip {
  display: flex; align-items: center; gap: 0.35rem;
  padding: 0.5rem 1rem; border-radius: 2rem;
  border: 1px solid #e2e8f0; background: #fff;
  font-size: 0.8rem; font-weight: 600; color: #64748b;
  cursor: pointer; transition: all 0.2s;
}
.filter-chip.active {
  background: #0284c7; color: #fff; border-color: #0284c7;
}

/* Alert List */
.alerts-list { display: flex; flex-direction: column; gap: 0.6rem; }

.alert-card {
  display: flex; align-items: flex-start; gap: 0.85rem;
  background: #fff; padding: 1rem; border-radius: 0.85rem;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
  transition: all 0.2s; position: relative;
}
.alert-card:active { transform: scale(0.99); }
.alert-unread { background: #f0f9ff; border-left: 3px solid #0284c7; }

.ac-icon {
  width: 36px; height: 36px; border-radius: 50%;
  background: #f8fafc; display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.ac-content { flex: 1; min-width: 0; }
.ac-top { display: flex; align-items: center; gap: 0.5rem; margin-bottom: 0.35rem; }
.ac-tipo { font-size: 0.72rem; font-weight: 800; text-transform: uppercase; letter-spacing: 0.04em; }
.ac-linha {
  background: #f1f5f9; padding: 0.15rem 0.5rem; border-radius: 0.3rem;
  font-size: 0.7rem; font-weight: 700; color: #334155;
}
.ac-msg { margin: 0; font-size: 0.88rem; color: #334155; line-height: 1.45; }
.ac-time {
  display: flex; align-items: center; gap: 0.3rem;
  font-size: 0.72rem; color: #94a3b8; margin-top: 0.4rem;
}
.ac-dot {
  width: 8px; height: 8px; border-radius: 50%; background: #0284c7;
  flex-shrink: 0; margin-top: 0.5rem;
}

.empty { text-align: center; padding: 3rem 1rem; color: #94a3b8; }
.empty-icon { opacity: 0.3; margin-bottom: 0.5rem; }
.empty-sub { font-size: 0.8rem; }

.settings-hint {
  display: flex; align-items: center; gap: 0.5rem;
  margin-top: 1.5rem; padding: 1rem;
  background: #fff; border-radius: 0.85rem;
  color: #64748b; font-size: 0.85rem; font-weight: 500;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
}
.settings-hint span { flex: 1; }
</style>
