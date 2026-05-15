<script setup>
import { ref, onMounted, watch, computed } from 'vue'
import { RefreshCw, Users, Bus, Percent, AlertTriangle, Info, Bell, Activity, TrendingUp } from 'lucide-vue-next'
import { apiFetch, demoModeRef } from '../services/api.js'
import { authService } from '../services/auth'

// vue-chartjs + chart.js
import { Line } from 'vue-chartjs'
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend,
  Filler,
} from 'chart.js'

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Title, Tooltip, Legend, Filler)

const dashboardStats = ref(null)
const loading = ref(true)
const err = ref('')
const adminUser = ref(null)

onMounted(() => {
  adminUser.value = authService.getAdminUser()
  carregarDashboard()
})

watch(demoModeRef, () => carregarDashboard())

async function carregarDashboard() {
  loading.value = true
  err.value = ''
  try {
    const { data } = await apiFetch('/dashboard')
    if (data.status === 'sucesso') {
      dashboardStats.value = data.dashboard || {}
    } else {
      err.value = data.mensagem || 'Erro desconhecido ao obter dados.'
    }
  } catch(e) {
    err.value = 'Falha Crítica na ligação ao servidor principal.'
  } finally {
    setTimeout(() => { loading.value = false }, 300)
  }
}

function formatDate(ds) {
  if (!ds) return ''
  const d = new Date(ds)
  return d.toLocaleString('pt-PT')
}

// ── Chart.js: dados e opções ──────────────────────────────────────────

const chartData = computed(() => {
  const raw = dashboardStats.value?.volumePorHora ?? []
  const labels = raw.map(r => `${String(r.hora).padStart(2, '0')}h`)
  const values = raw.map(r => r.passageiros)

  return {
    labels,
    datasets: [
      {
        label: 'Passageiros / hora',
        data: values,
        fill: true,
        tension: 0.45,
        borderColor: '#06b6d4',
        borderWidth: 2.5,
        pointBackgroundColor: '#06b6d4',
        pointBorderColor: '#0e1117',
        pointRadius: 4,
        pointHoverRadius: 7,
        backgroundColor: (ctx) => {
          const canvas = ctx.chart.canvas
          const gradient = canvas.getContext('2d').createLinearGradient(0, 0, 0, canvas.height)
          gradient.addColorStop(0, 'rgba(6,182,212,0.35)')
          gradient.addColorStop(1, 'rgba(6,182,212,0.00)')
          return gradient
        },
      },
    ],
  }
})

const chartOptions = {
  responsive: true,
  maintainAspectRatio: false,
  interaction: { mode: 'index', intersect: false },
  plugins: {
    legend: { display: false },
    tooltip: {
      backgroundColor: 'rgba(14,17,23,0.92)',
      borderColor: 'rgba(6,182,212,0.4)',
      borderWidth: 1,
      titleColor: '#e2e8f0',
      bodyColor: '#94a3b8',
      padding: 12,
      callbacks: {
        label: ctx => ` ${ctx.parsed.y} passageiros`,
      },
    },
  },
  scales: {
    x: {
      grid: { color: 'rgba(255,255,255,0.04)' },
      ticks: { color: '#64748b', font: { size: 11 } },
    },
    y: {
      grid: { color: 'rgba(255,255,255,0.04)' },
      ticks: { color: '#64748b', font: { size: 11 } },
      beginAtZero: true,
    },
  },
}
</script>

<template>
  <div class="dashboard-view fade-in">
    <div v-if="adminUser" class="welcome-banner">
      <div class="banner-content">
        <h2>Bem-vindo de volta, {{ adminUser.nome }}</h2>
        <p>Gestor de Frota • {{ adminUser.email }}</p>
      </div>
    </div>

    <div class="action-bar">
      <h3 class="section-title">Painel de Operações</h3>
      <button @click="carregarDashboard" class="btn btn-secondary" :disabled="loading">
        <RefreshCw :class="{'spin': loading}" :size="16"/> Sincronizar
      </button>
    </div>

    <div v-if="err" class="error-banner">
      <AlertTriangle /> {{ err }}
    </div>

    <!-- Quick stats -->
    <div v-if="dashboardStats" class="quick-stats">
      <div class="stat-card glass-panel">
        <div class="stat-content">
          <div class="stat-icon"><Percent /></div>
          <div class="stat-info">
            <span class="stat-value">{{ Number(dashboardStats.taxaOcupacaoMedia || 0).toFixed(1) }}%</span>
            <span class="stat-label">Taxa Ocupação Média</span>
          </div>
        </div>
        <div class="stat-chart">
          <svg viewBox="0 0 80 30" preserveAspectRatio="none" class="sparkline">
            <path d="M0,25 C15,20 25,28 40,15 C55,2 65,12 80,5" fill="none" stroke="var(--accent-blue)" stroke-width="2" stroke-linecap="round"/>
            <path d="M0,25 C15,20 25,28 40,15 C55,2 65,12 80,5 L80,30 L0,30 Z" fill="url(#grad1)"/>
            <defs>
              <linearGradient id="grad1" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stop-color="var(--accent-blue)" stop-opacity="0.3" />
                <stop offset="100%" stop-color="var(--accent-blue)" stop-opacity="0" />
              </linearGradient>
            </defs>
          </svg>
        </div>
      </div>

      <div class="stat-card glass-panel">
        <div class="stat-content">
          <div class="stat-icon"><Users /></div>
          <div class="stat-info">
            <span class="stat-value">{{ dashboardStats.volumeTotalPassageiros }}</span>
            <span class="stat-label">Volume Passageiros</span>
          </div>
        </div>
        <div class="stat-chart bars">
          <div class="bar" style="height: 40%"></div>
          <div class="bar" style="height: 60%"></div>
          <div class="bar" style="height: 45%"></div>
          <div class="bar" style="height: 80%"></div>
          <div class="bar" style="height: 55%"></div>
          <div class="bar" style="height: 90%"></div>
        </div>
      </div>

      <div class="stat-card glass-panel">
        <div class="stat-content">
          <div class="stat-icon"><Bus /></div>
          <div class="stat-info">
            <span class="stat-value">{{ dashboardStats.totalAutocarros }}</span>
            <span class="stat-label">Autocarros Ativos</span>
          </div>
        </div>
        <div class="stat-chart">
          <svg viewBox="0 0 80 30" preserveAspectRatio="none" class="sparkline">
            <path d="M0,15 L15,15 L30,15 L45,15 L60,15 L80,15" fill="none" stroke="var(--accent-blue)" stroke-width="2" stroke-linecap="round" stroke-dasharray="4 4"/>
          </svg>
        </div>
      </div>

      <div class="stat-card glass-panel">
        <div class="stat-content">
          <div class="stat-icon icon-success"><Activity /></div>
          <div class="stat-info">
            <span class="stat-value text-success">OK</span>
            <span class="stat-label">Estado PGU</span>
          </div>
        </div>
        <div class="stat-chart">
          <svg viewBox="0 0 80 30" preserveAspectRatio="none" class="sparkline">
            <path d="M0,15 L20,15 L25,5 L35,25 L40,15 L80,15" fill="none" stroke="#10b981" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </div>
      </div>
    </div>

    <!-- ── Gráfico de Evolução Temporal ─────────────────────────────── -->
    <div v-if="dashboardStats" class="chart-section glass-panel">
      <div class="chart-header">
        <div class="chart-title-group">
          <TrendingUp class="chart-icon" :size="20" />
          <div>
            <h4 class="chart-title">Evolução de Passageiros — Hoje</h4>
            <p class="chart-subtitle">Volume de entradas por hora do dia atual</p>
          </div>
        </div>
        <span class="chart-badge">
          {{ demoModeRef ? 'Simulação' : 'Dados Reais' }}
        </span>
      </div>

      <div class="chart-canvas-wrapper">
        <div v-if="(dashboardStats.volumePorHora ?? []).length === 0" class="chart-empty">
          <TrendingUp :size="40" class="text-muted" />
          <p>Sem leituras registadas hoje.</p>
          <span>Assim que chegarem dados do sensor, o gráfico será preenchido automaticamente.</span>
        </div>
        <Line
          v-else
          :data="chartData"
          :options="chartOptions"
          style="width:100%; height:100%;"
        />
      </div>
    </div>

    <!-- ── Painéis Inferiores ───────────────────────────────────────── -->
    <div v-if="dashboardStats" class="dashboard-content">
      <div class="content-section autocarros-criticos">
        <h4><AlertTriangle class="text-warning"/> Veículos em Lotação Crítica</h4>
        <div class="list-container glass-panel">
          <div v-if="(dashboardStats.autocarrosCriticos || []).length === 0" class="empty-state">
            Nenhum veículo em estado crítico no momento.
          </div>
          <div v-for="c in (dashboardStats.autocarrosCriticos || [])" :key="c.id" class="list-item">
            <span class="item-id">Autocarro {{ c.id }}</span>
            <span class="item-value text-danger">{{ Number(c.taxaOcupacao || 0).toFixed(1) }}% de Ocupação</span>
          </div>
        </div>
      </div>

      <div class="content-section avisos-recentes">
        <h4><Bell class="text-accent"/> Avisos do Sistema</h4>
        <div class="list-container glass-panel">
          <div v-if="(dashboardStats.avisosRecentes || []).length === 0" class="empty-state">
            Nenhum aviso recente.
          </div>
          <div v-for="(a, i) in (dashboardStats.avisosRecentes || [])" :key="i" class="list-item aviso-item">
            <div class="aviso-icon">
              <AlertTriangle v-if="a.tipo === 'LOTACAO_CRITICA'" class="text-danger" size="18"/>
              <Info v-else class="text-warning" size="18" />
            </div>
            <div class="aviso-detalhes">
              <span class="aviso-msg">{{ a.mensagem }}</span>
              <span class="aviso-meta">Veículo {{ a.autocarroId }} • {{ formatDate(a.timestamp) }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.action-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
}
.section-title { margin: 0; }
.spin { animation: spin 1s linear infinite; }
@keyframes spin { 100% { transform: rotate(360deg); } }

.welcome-banner {
  margin-bottom: 2rem;
  background: var(--bg-surface);
  padding: 1.5rem;
  border-radius: 1rem;
  border: 1px solid var(--border-light);
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
}
.welcome-banner h2 {
  font-size: 1.5rem;
  margin: 0;
  color: var(--text-main);
}
.welcome-banner p {
  margin: 0.25rem 0 0;
  color: var(--text-muted);
  font-size: 0.85rem;
}

.error-banner {
  background: rgba(239, 68, 68, 0.1);
  color: var(--danger);
  border: 1px solid var(--danger);
  padding: 1rem;
  border-radius: 0.5rem;
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 1.5rem;
}

/* ── Quick Stats ── */
.quick-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1.5rem;
  margin-bottom: 2rem;
}
.stat-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  padding: 1.5rem;
  background: var(--bg-surface);
  border: 1px solid var(--border-light);
  border-radius: 1rem;
  overflow: hidden;
  position: relative;
}
.stat-content {
  display: flex;
  align-items: center;
  gap: 1.25rem;
  z-index: 2;
}
.stat-chart {
  width: 70px;
  height: 35px;
  display: flex;
  align-items: flex-end;
  gap: 3px;
  z-index: 1;
}
.sparkline {
  width: 100%;
  height: 100%;
  overflow: visible;
}
.stat-chart.bars { justify-content: space-between; }
.stat-chart .bar {
  flex: 1;
  background: var(--accent-blue);
  border-radius: 2px 2px 0 0;
  opacity: 0.5;
}
.icon-success {
  background: rgba(16, 185, 129, 0.1) !important;
  color: #10b981 !important;
}
.text-success { color: #10b981 !important; }
.stat-icon {
  background: rgba(6, 182, 212, 0.1);
  color: var(--accent-blue);
  padding: 1rem;
  border-radius: 0.75rem;
  display: flex;
}
.stat-info { display: flex; flex-direction: column; }
.stat-value { font-size: 1.5rem; font-weight: 700; color: var(--text-main); }
.stat-label {
  font-size: 0.85rem;
  color: var(--text-muted);
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

/* ── Chart Section ── */
.chart-section {
  background: var(--bg-surface);
  border: 1px solid var(--border-light);
  border-radius: 1rem;
  padding: 1.75rem;
  margin-bottom: 2rem;
}

.chart-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 1.5rem;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.chart-title-group {
  display: flex;
  align-items: center;
  gap: 0.875rem;
}

.chart-icon {
  color: var(--accent-blue);
  flex-shrink: 0;
  margin-top: 2px;
}

.chart-title {
  margin: 0;
  font-size: 1.05rem;
  font-weight: 600;
  color: var(--text-main);
}

.chart-subtitle {
  margin: 0.2rem 0 0;
  font-size: 0.8rem;
  color: var(--text-muted);
}

.chart-badge {
  font-size: 0.72rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  padding: 0.3rem 0.75rem;
  border-radius: 999px;
  background: rgba(6, 182, 212, 0.12);
  color: var(--accent-blue);
  border: 1px solid rgba(6, 182, 212, 0.3);
  white-space: nowrap;
}

.chart-canvas-wrapper {
  position: relative;
  height: 260px;
}

.chart-empty {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  color: var(--text-muted);
  text-align: center;
}
.chart-empty p {
  margin: 0;
  font-weight: 500;
  font-size: 1rem;
}
.chart-empty span {
  font-size: 0.82rem;
  opacity: 0.7;
  max-width: 28rem;
}

/* ── Bottom Grid ── */
.dashboard-content {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1.5rem;
}

@media (max-width: 768px) {
  .dashboard-content { grid-template-columns: 1fr; }
}

.content-section h4 {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 1rem;
  font-size: 1.1rem;
  color: var(--text-main);
}

.text-warning { color: var(--warning); }
.text-danger  { color: var(--danger); }
.text-accent  { color: var(--accent-blue); }
.text-muted   { color: var(--text-muted); }

.list-container {
  display: flex;
  flex-direction: column;
  padding: 1rem;
  border-radius: 1rem;
  background: var(--bg-surface);
  border: 1px solid var(--border-light);
  min-height: 200px;
  max-height: 400px;
  overflow-y: auto;
}

.empty-state {
  text-align: center;
  color: var(--text-muted);
  font-style: italic;
  margin: auto;
  padding: 2rem;
}

.list-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem 0;
  border-bottom: 1px solid var(--border-light);
}
.list-item:last-child { border-bottom: none; }

.item-id    { font-weight: bold; color: var(--text-main); }
.item-value { font-weight: 500; }

.aviso-item {
  justify-content: flex-start;
  gap: 1rem;
}

.aviso-icon {
  background: var(--bg-primary);
  padding: 0.5rem;
  border-radius: 50%;
  display: flex;
}
.aviso-detalhes { display: flex; flex-direction: column; }
.aviso-msg  { font-weight: 500; color: var(--text-main); }
.aviso-meta { font-size: 0.8rem; color: var(--text-muted); margin-top: 0.2rem; }
</style>
