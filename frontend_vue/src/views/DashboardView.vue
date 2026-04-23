<script setup>
import { ref, onMounted } from 'vue'
import { RefreshCw, Users, Bus, Percent, AlertTriangle, Info, Bell, Activity, FlaskConical, Database } from 'lucide-vue-next'
import { apiFetch } from '../services/api.js'
import { authService } from '../services/auth'

const dashboardStats = ref(null)
const loading = ref(true)
const err = ref('')
const isDemo = ref(false)
const adminUser = ref(null)

onMounted(() => {
  adminUser.value = authService.getAdminUser()
  
  // Sincronizar estado do demo com o localStorage
  isDemo.value = localStorage.getItem('pgu_demo_mode') === 'true'
  carregarDashboard()
})

function toggleDemoMode() {
  const newMode = !isDemo.value
  localStorage.setItem('pgu_demo_mode', newMode)
  window.location.reload()
}

async function carregarDashboard() {
  loading.value = true
  err.value = ''
  try {
    const { live, data } = await apiFetch('/dashboard')
    isDemo.value = !live
    if (data.status === 'sucesso') {
      dashboardStats.value = data.dashboard || {}
    } else {
      err.value = data.mensagem || 'Erro desconhecido ao obter dados.'
    }
  } catch(e) { 
    err.value = "Falha Crítica na ligação ao servidor principal."
  } finally {
    setTimeout(() => { loading.value = false }, 300)
  }
}

function formatDate(ds) {
  if (!ds) return ''
  const d = new Date(ds)
  return d.toLocaleString('pt-PT')
}
</script>

<template>
  <div class="dashboard-view fade-in">
    <div v-if="adminUser" class="welcome-banner">
      <div class="banner-content">
        <h2>Bem-vindo de volta, {{ adminUser.nome }}! 👋</h2>
        <p>Gestor de Frota • {{ adminUser.email }}</p>
      </div>
      <div class="mode-selector" :class="{ 'demo-active': isDemo }">
        <span class="mode-label">{{ isDemo ? 'MODO DEMO (SIMULAÇÃO)' : 'MODO REAL (AZURE DB)' }}</span>
        <button @click="toggleDemoMode" class="btn-toggle-mode">
          <FlaskConical v-if="isDemo" :size="18"/>
          <Database v-else :size="18"/>
          {{ isDemo ? 'Ativar Modo Real' : 'Ativar Modo Demo' }}
        </button>
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
        <div class="stat-icon"><Percent /></div>
        <div class="stat-info">
          <span class="stat-value">{{ Number(dashboardStats.taxaOcupacaoMedia || 0).toFixed(1) }}%</span>
          <span class="stat-label">Taxa Ocupação Média</span>
        </div>
      </div>
      <div class="stat-card glass-panel">
        <div class="stat-icon"><Users /></div>
        <div class="stat-info">
          <span class="stat-value">{{ dashboardStats.volumeTotalPassageiros }}</span>
          <span class="stat-label">Volume Passageiros</span>
        </div>
      </div>
      <div class="stat-card glass-panel">
        <div class="stat-icon"><Bus /></div>
        <div class="stat-info">
          <span class="stat-value">{{ dashboardStats.totalAutocarros }}</span>
          <span class="stat-label">Autocarros Ativos</span>
        </div>
      </div>
      <div class="stat-card glass-panel">
        <div class="stat-icon"><Activity /></div>
        <div class="stat-info">
          <span class="stat-value">OK</span>
          <span class="stat-label">Estado PGU</span>
        </div>
      </div>
    </div>

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
  display: flex;
  justify-content: space-between;
  align-items: center;
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

.mode-selector {
  background: rgba(16, 185, 129, 0.1);
  padding: 1rem;
  border-radius: 0.75rem;
  border: 1px solid rgba(16, 185, 129, 0.2);
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 0.5rem;
}

.mode-selector.demo-active {
  background: rgba(245, 158, 11, 0.1);
  border-color: rgba(245, 158, 11, 0.2);
}

.mode-label {
  font-size: 0.7rem;
  font-weight: 800;
  color: var(--success);
}

.demo-active .mode-label {
  color: var(--warning);
}

.btn-toggle-mode {
  background: var(--bg-primary);
  border: 1px solid var(--border-light);
  color: var(--text-main);
  padding: 0.5rem 1rem;
  border-radius: 0.5rem;
  font-size: 0.8rem;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-toggle-mode:hover {
  background: var(--bg-surface);
  border-color: var(--accent-blue);
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

.quick-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1.5rem;
  margin-bottom: 2rem;
}
.stat-card {
  display: flex;
  align-items: center;
  gap: 1.5rem;
  padding: 1.5rem;
  background: var(--bg-surface);
  border: 1px solid var(--border-light);
  border-radius: 1rem;
}
.stat-icon {
  background: rgba(6, 182, 212, 0.1);
  color: var(--accent-blue);
  padding: 1rem;
  border-radius: 0.75rem;
  display: flex;
}
.stat-info { display: flex; flex-direction: column; }
.stat-value { font-size: 1.5rem; font-weight: 700; color: var(--text-main); }
.stat-label { font-size: 0.85rem; color: var(--text-muted); font-weight: 500; text-transform: uppercase; letter-spacing: 0.05em; }

.dashboard-content {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1.5rem;
}

@media (max-width: 768px) {
  .dashboard-content {
    grid-template-columns: 1fr;
  }
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
.text-danger { color: var(--danger); }
.text-accent { color: var(--accent-blue); }

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
.list-item:last-child {
  border-bottom: none;
}

.item-id { font-weight: bold; color: var(--text-main); }
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
.aviso-detalhes {
  display: flex;
  flex-direction: column;
}
.aviso-msg { font-weight: 500; color: var(--text-main); }
.aviso-meta { font-size: 0.8rem; color: var(--text-muted); margin-top: 0.2rem; }
</style>
