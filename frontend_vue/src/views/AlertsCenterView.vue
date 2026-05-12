<script setup>
import { ref, onMounted, onUnmounted, computed, watch } from 'vue'
import {
  Bell, AlertTriangle, AlertOctagon, CheckCircle, Clock,
  Filter, Download, Bus, RefreshCw, ShieldAlert, Activity
} from 'lucide-vue-next'

import { apiFetch, demoModeRef } from '../services/api.js'
const alerts = ref([])
const loading = ref(true)
const filterType = ref('')
const filterStatus = ref('')
let timer = null

// Estado local de acknowledge (persistiria via API em produção)
const acknowledgedIds = ref(new Set())
const acknowledgeNotes = ref({})

async function fetchAlerts() {
  try {
    const { data } = await apiFetch('/dashboard')
    if (data.status === 'sucesso') {
      const raw = data.dashboard?.avisosRecentes || []
      alerts.value = raw.map((a, i) => ({
        ...a,
        uid: `${a.autocarroId}-${a.timestamp}-${i}`,
        severity: getSeverity(a.tipo),
        severityLabel: getSeverityLabel(a.tipo),
      }))
    }
  } catch(e) {
    console.error('Erro alertas:', e)
  } finally {
    loading.value = false
  }
}

function getSeverity(tipo) {
  if (!tipo) return 'info'
  const t = tipo.toUpperCase()
  if (t.includes('OCUPACAO') || t.includes('LOTACAO')) return 'critical'
  if (t.includes('ANOMALA') || t.includes('INCONSISTENTE')) return 'warning'
  if (t.includes('AUSENCIA')) return 'warning'
  return 'info'
}

function getSeverityLabel(tipo) {
  if (!tipo) return 'Info'
  const t = tipo.toUpperCase()
  if (t.includes('OCUPACAO') || t.includes('LOTACAO')) return 'Crítico'
  if (t.includes('ANOMALA') || t.includes('INCONSISTENTE')) return 'Anomalia'
  if (t.includes('AUSENCIA')) return 'Aviso'
  return 'Info'
}

function isAcknowledged(uid) {
  return acknowledgedIds.value.has(uid)
}

function acknowledgeAlert(uid) {
  acknowledgedIds.value.add(uid)
  if (!acknowledgeNotes.value[uid]) {
    acknowledgeNotes.value[uid] = `Reconhecido às ${new Date().toLocaleTimeString('pt-PT')}`
  }
}

function setNote(uid, note) {
  acknowledgeNotes.value[uid] = note
}

const filteredAlerts = computed(() => {
  let list = alerts.value
  if (filterType.value) list = list.filter(a => a.severity === filterType.value)
  if (filterStatus.value === 'pending') list = list.filter(a => !isAcknowledged(a.uid))
  if (filterStatus.value === 'ack') list = list.filter(a => isAcknowledged(a.uid))
  return list
})

const stats = computed(() => {
  const total = alerts.value.length
  const critical = alerts.value.filter(a => a.severity === 'critical').length
  const pending = alerts.value.filter(a => !isAcknowledged(a.uid)).length
  const ack = alerts.value.filter(a => isAcknowledged(a.uid)).length
  return { total, critical, pending, ack }
})

function exportCSV() {
  if (filteredAlerts.value.length === 0) return
  const headers = ['Timestamp', 'Veículo', 'Tipo', 'Severidade', 'Mensagem', 'Estado', 'Nota']
  const rows = filteredAlerts.value.map(a => [
    a.timestamp,
    a.autocarroId,
    a.tipo,
    a.severityLabel,
    `"${(a.mensagem || '').replace(/"/g, '""')}"`,
    isAcknowledged(a.uid) ? 'Reconhecido' : 'Pendente',
    `"${(acknowledgeNotes.value[a.uid] || '').replace(/"/g, '""')}"`
  ])
  const csv = [headers.join(';'), ...rows.map(r => r.join(';'))].join('\n')
  const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `alertas_pgu_${new Date().toISOString().split('T')[0]}.csv`
  a.click()
  URL.revokeObjectURL(url)
}

function formatDate(ds) {
  if (!ds) return ''
  return new Date(ds).toLocaleString('pt-PT')
}

onMounted(() => {
  fetchAlerts()
  timer = setInterval(fetchAlerts, 8000)
})
onUnmounted(() => { if (timer) clearInterval(timer) })

watch(demoModeRef, () => fetchAlerts())
</script>

<template>
  <div class="alerts-view fade-in">
    <!-- Header -->
    <div class="glass-panel main-header">
      <div class="head-left">
        <h3 class="panel-title"><ShieldAlert class="icon-inline" /> Central de Alertas Operacionais</h3>
        <p class="panel-desc">UC 4.5: Monitorização, triagem e reconhecimento de eventos críticos</p>
      </div>
      <div class="header-actions">
        <button class="btn btn-secondary btn-sm" @click="fetchAlerts" :disabled="loading">
          <RefreshCw :size="14" :class="{'spin': loading}" /> Atualizar
        </button>
        <button class="btn btn-secondary btn-sm" @click="exportCSV">
          <Download :size="14" /> Exportar CSV
        </button>
      </div>
    </div>

    <!-- KPI Stats -->
    <div class="stats-row">
      <div class="glass-panel stat-chip">
        <Bell :size="18" />
        <span class="sc-val fira-code">{{ stats.total }}</span>
        <span class="sc-label">Total</span>
      </div>
      <div class="glass-panel stat-chip chip-critical">
        <AlertOctagon :size="18" />
        <span class="sc-val fira-code text-danger">{{ stats.critical }}</span>
        <span class="sc-label">Críticos</span>
      </div>
      <div class="glass-panel stat-chip chip-pending">
        <Clock :size="18" />
        <span class="sc-val fira-code text-warning">{{ stats.pending }}</span>
        <span class="sc-label">Pendentes</span>
      </div>
      <div class="glass-panel stat-chip chip-ack">
        <CheckCircle :size="18" />
        <span class="sc-val fira-code text-teal">{{ stats.ack }}</span>
        <span class="sc-label">Reconhecidos</span>
      </div>
    </div>

    <!-- Filters -->
    <div class="glass-panel filter-bar">
      <Filter :size="16" class="dim" />
      <select v-model="filterType" class="filter-select">
        <option value="">Todas as Severidades</option>
        <option value="critical">Crítico</option>
        <option value="warning">Aviso / Anomalia</option>
        <option value="info">Informação</option>
      </select>
      <select v-model="filterStatus" class="filter-select">
        <option value="">Todos os Estados</option>
        <option value="pending">Pendentes</option>
        <option value="ack">Reconhecidos</option>
      </select>
    </div>

    <!-- Alert List -->
    <div class="alerts-list">
      <div
        v-for="alert in filteredAlerts"
        :key="alert.uid"
        class="glass-panel alert-card"
        :class="{
          'severity-critical': alert.severity === 'critical',
          'severity-warning': alert.severity === 'warning',
          'alert-ack': isAcknowledged(alert.uid)
        }"
      >
        <div class="ac-indicator">
          <AlertOctagon v-if="alert.severity === 'critical'" class="text-danger" :size="22" />
          <AlertTriangle v-else-if="alert.severity === 'warning'" class="text-warning" :size="22" />
          <Activity v-else class="text-info" :size="22" />
        </div>

        <div class="ac-content">
          <div class="ac-top">
            <span class="ac-severity-badge" :class="'badge-' + alert.severity">
              {{ alert.severityLabel }}
            </span>
            <span class="ac-bus fira-code"><Bus :size="14" /> {{ alert.autocarroId }}</span>
            <span class="ac-time dim"><Clock :size="12" /> {{ formatDate(alert.timestamp) }}</span>
          </div>
          <p class="ac-message">{{ alert.mensagem }}</p>

          <!-- Ação Corretiva -->
          <div v-if="isAcknowledged(alert.uid)" class="ac-ack-info fade-in">
            <CheckCircle :size="14" class="text-teal" />
            <input
              type="text"
              :value="acknowledgeNotes[alert.uid]"
              @input="setNote(alert.uid, $event.target.value)"
              class="ack-note-input fira-code"
              placeholder="Nota da ação corretiva..."
            />
          </div>
        </div>

        <div class="ac-actions">
          <button
            v-if="!isAcknowledged(alert.uid)"
            class="btn btn-ack"
            @click="acknowledgeAlert(alert.uid)"
          >
            <CheckCircle :size="14" /> Acknowledge
          </button>
          <span v-else class="ack-badge"><CheckCircle :size="14" /> ACK</span>
        </div>
      </div>

      <div v-if="filteredAlerts.length === 0" class="empty-state glass-panel">
        <CheckCircle :size="40" class="text-teal" />
        <p>Nenhum alerta encontrado com os filtros atuais.</p>
        <p class="dim">A operação decorre dentro dos parâmetros normais.</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.main-header { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 1rem; }
.panel-title { display: flex; align-items: center; gap: 0.5rem; margin-bottom: 0.25rem; }
.panel-desc { color: var(--text-muted); font-size: 0.85rem; margin: 0; }
.icon-inline { color: var(--accent-blue); }
.header-actions { display: flex; gap: 0.75rem; }
.btn-sm { padding: 0.45rem 0.75rem; font-size: 0.85rem; }
.spin { animation: spin 1s linear infinite; }
@keyframes spin { 100% { transform: rotate(360deg); } }

/* Stats */
.stats-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 1rem; margin-top: 1.25rem; }
.stat-chip { display: flex; align-items: center; gap: 0.75rem; padding: 1rem 1.25rem; }
.chip-critical { border-left: 3px solid var(--danger); }
.chip-pending { border-left: 3px solid var(--warning); }
.chip-ack { border-left: 3px solid var(--accent-teal); }
.sc-val { font-size: 1.4rem; font-weight: 700; }
.sc-label { font-size: 0.75rem; color: var(--text-muted); text-transform: uppercase; font-weight: 600; }

/* Filters */
.filter-bar { display: flex; align-items: center; gap: 1rem; margin-top: 1.25rem; padding: 0.75rem 1.25rem; }
.filter-select {
  background: var(--bg-primary);
  border: 1px solid var(--border-light);
  color: var(--text-main);
  border-radius: 0.4rem;
  padding: 0.4rem 0.75rem;
  font-size: 0.85rem;
}

/* Alert List */
.alerts-list { display: flex; flex-direction: column; gap: 0.75rem; margin-top: 1.25rem; }

.alert-card {
  display: flex;
  align-items: flex-start;
  gap: 1rem;
  padding: 1.25rem;
  transition: all 0.2s ease;
  border-left: 3px solid transparent;
}
.severity-critical { border-left-color: var(--danger); background: linear-gradient(90deg, rgba(239,68,68,0.06) 0%, transparent 30%); }
.severity-warning { border-left-color: var(--warning); background: linear-gradient(90deg, rgba(234,179,8,0.04) 0%, transparent 30%); }
.alert-ack { opacity: 0.7; }
.alert-card:hover { transform: translateX(2px); }

.ac-indicator { padding-top: 0.15rem; }
.ac-content { flex: 1; }
.ac-top { display: flex; align-items: center; gap: 0.75rem; margin-bottom: 0.5rem; flex-wrap: wrap; }
.ac-severity-badge {
  padding: 0.15rem 0.6rem;
  border-radius: 1rem;
  font-size: 0.72rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}
.badge-critical { background: rgba(239,68,68,0.15); color: var(--danger); }
.badge-warning { background: rgba(234,179,8,0.15); color: var(--warning); }
.badge-info { background: rgba(6,182,212,0.15); color: var(--accent-blue); }

.ac-bus { display: flex; align-items: center; gap: 0.3rem; font-weight: 600; font-size: 0.85rem; color: var(--accent-blue); }
.ac-time { display: flex; align-items: center; gap: 0.3rem; font-size: 0.8rem; }
.ac-message { margin: 0; line-height: 1.5; font-size: 0.95rem; }

/* Acknowledge */
.ac-ack-info {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-top: 0.75rem;
  padding: 0.5rem 0.75rem;
  background: rgba(20,184,166,0.08);
  border-radius: 0.4rem;
  border: 1px solid rgba(20,184,166,0.2);
}
.ack-note-input {
  flex: 1;
  background: transparent;
  border: none;
  color: var(--text-main);
  font-size: 0.85rem;
  outline: none;
}
.ack-note-input::placeholder { color: var(--text-muted); }

.ac-actions { display: flex; flex-direction: column; align-items: flex-end; gap: 0.5rem; }
.btn-ack {
  background: rgba(20,184,166,0.12);
  color: var(--accent-teal);
  border: 1px solid rgba(20,184,166,0.3);
  border-radius: 0.4rem;
  padding: 0.4rem 0.85rem;
  cursor: pointer;
  font-size: 0.8rem;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 0.4rem;
  transition: all 0.2s;
  white-space: nowrap;
}
.btn-ack:hover { background: rgba(20,184,166,0.25); }
.ack-badge {
  display: flex;
  align-items: center;
  gap: 0.3rem;
  color: var(--accent-teal);
  font-size: 0.8rem;
  font-weight: 600;
}

.empty-state { display: flex; flex-direction: column; align-items: center; gap: 0.75rem; padding: 3rem; text-align: center; }

.text-danger { color: var(--danger); }
.text-warning { color: var(--warning); }
.text-teal { color: var(--accent-teal); }
.text-info { color: var(--accent-blue); }
.dim { color: var(--text-muted); }

@media (max-width: 768px) {
  .stats-row { grid-template-columns: repeat(2, 1fr); }
  .alert-card { flex-direction: column; }
}
</style>
