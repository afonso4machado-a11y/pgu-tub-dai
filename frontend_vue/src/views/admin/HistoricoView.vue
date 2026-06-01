<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { authService } from '../../services/auth'
import { Search, Calendar, RefreshCw, Filter, User, Clock, Shield, Database, ArrowUpDown, FileDown, AlertTriangle } from 'lucide-vue-next'

const logs = ref([])
const loading = ref(true)
const error = ref(null)

// Filters
const searchEmail = ref('')
const selectedAction = ref('')
const startDate = ref('')
const endDate = ref('')

// Sorting
const sortDesc = ref(true)

// Fetch logs
async function loadLogs() {
  loading.value = true
  error.value = null
  try {
    const admin = authService.getAdminUser()
    const email = admin ? admin.email : ''
    
    const res = await fetch('/api/audit-logs', {
      headers: {
        'X-Admin-Email': email
      }
    })
    
    if (!res.ok) {
      if (res.status === 403) {
        throw new Error('Acesso negado. Apenas administradores autorizados com email institucional.')
      }
      throw new Error('Falha ao obter os registos do servidor.')
    }
    
    const data = await res.json()
    if (data.status === 'sucesso') {
      logs.value = data.logs || []
    } else {
      throw new Error(data.mensagem || 'Erro desconhecido.')
    }
  } catch (err) {
    error.value = err.message
  } finally {
    loading.value = false
  }
}

// Extract unique actions for filter list
const actionTypes = computed(() => {
  const types = new Set(logs.value.map(l => l.acaoTipo))
  return Array.from(types).sort()
})

// Filter and Sort logs
const filteredLogs = computed(() => {
  let result = [...logs.value]

  // Filter by actor email/name
  if (searchEmail.value.trim()) {
    const term = searchEmail.value.toLowerCase().trim()
    result = result.filter(l => 
      l.utilizadorEmail.toLowerCase().includes(term) || 
      l.utilizadorNome.toLowerCase().includes(term)
    )
  }

  // Filter by action type
  if (selectedAction.value) {
    result = result.filter(l => l.acaoTipo === selectedAction.value)
  }

  // Filter by start date
  if (startDate.value) {
    const start = new Date(startDate.value)
    start.setHours(0, 0, 0, 0)
    result = result.filter(l => new Date(l.timestamp) >= start)
  }

  // Filter by end date
  if (endDate.value) {
    const end = new Date(endDate.value)
    end.setHours(23, 59, 59, 999)
    result = result.filter(l => new Date(l.timestamp) <= end)
  }

  // Sort by timestamp
  result.sort((a, b) => {
    const timeA = new Date(a.timestamp).getTime()
    const timeB = new Date(b.timestamp).getTime()
    return sortDesc.value ? timeB - timeA : timeA - timeB
  })

  return result
})

function formatActionType(type) {
  if (!type) return ''
  return type.replace(/_/g, ' ')
}

function getActionBadgeClass(type) {
  if (type === 'ADICIONAR_AUTOCARRO') return 'badge-success'
  if (type === 'ELIMINAR_AUTOCARRO') return 'badge-danger'
  if (type === 'RESTAURAR_AUTOCARRO') return 'badge-info'
  if (type === 'ASSOCIAR_AUTOCARRO_LINHA') return 'badge-warning'
  return 'badge-secondary'
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  
  const dd = String(date.getDate()).padStart(2, '0')
  const mm = String(date.getMonth() + 1).padStart(2, '0')
  const yyyy = date.getFullYear()
  
  const hh = String(date.getHours()).padStart(2, '0')
  const min = String(date.getMinutes()).padStart(2, '0')
  const ss = String(date.getSeconds()).padStart(2, '0')
  
  return `${dd}/${mm}/${yyyy} ${hh}:${min}:${ss}`
}

function toggleSort() {
  sortDesc.value = !sortDesc.value
}

function resetFilters() {
  searchEmail.value = ''
  selectedAction.value = ''
  startDate.value = ''
  endDate.value = ''
}

function exportCSV() {
  if (filteredLogs.value.length === 0) return
  
  let csvContent = "data:text/csv;charset=utf-8,\uFEFF" 
    + ["ID", "Sessao", "Utilizador Email", "Utilizador Nome", "Acao", "Detalhes", "Timestamp"].join(",") + "\n";
    
  filteredLogs.value.forEach(l => {
    const row = [
      l.id,
      `"${l.sessionId}"`,
      `"${l.utilizadorEmail.replace(/"/g, '""')}"`,
      `"${l.utilizadorNome.replace(/"/g, '""')}"`,
      `"${l.acaoTipo}"`,
      `"${l.detalhes.replace(/"/g, '""')}"`,
      `"${l.timestamp}"`
    ];
    csvContent += row.join(",") + "\n";
  });
  
  const encodedUri = encodeURI(csvContent);
  const link = document.createElement("a");
  link.setAttribute("href", encodedUri);
  link.setAttribute("download", `historico_backoffice_${new Date().toISOString().split('T')[0]}.csv`);
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
}

let pollInterval = null

onMounted(() => {
  loadLogs()
  // Poll new logs every 4 seconds for real-time multiplayer backoffice tracking
  pollInterval = setInterval(() => {
    loadLogs()
  }, 4000)
})

onUnmounted(() => {
  if (pollInterval) {
    clearInterval(pollInterval)
  }
})
</script>

<template>
  <div class="historico-view fade-in">
    <!-- Header -->
    <div class="page-header mb-6">
      <div>
        <h2 class="page-title">Histórico de Auditoria</h2>
        <p class="page-subtitle">Visualização e rastreamento completo de ações efetuadas por operadores de backoffice</p>
      </div>
      <div class="header-actions">
        <button class="btn btn-outline" @click="exportCSV" :disabled="filteredLogs.length === 0">
          <FileDown :size="18" />
          Exportar CSV
        </button>
        <button class="btn class-btn-primary" @click="loadLogs" :disabled="loading">
          <RefreshCw :size="18" :class="{ 'spin-icon': loading }" />
          Recarregar
        </button>
      </div>
    </div>
    <!-- Filters Control Bar -->
    <div class="glass-panel filter-bar mb-6">
      <div class="filter-header">
        <div class="filter-header-title">
          <Filter :size="16" class="text-cyan" />
          <span>Filtros Rápidos</span>
        </div>
        <button v-if="searchEmail || selectedAction || startDate || endDate" class="btn-clear-filters" @click="resetFilters">
          Limpar Filtros
        </button>
      </div>
      
      <div class="filter-inputs-grid">
        <div class="input-group">
          <label>Operador / Email</label>
          <div class="input-with-icon">
            <Search :size="16" class="input-icon" />
            <input 
              v-model="searchEmail" 
              type="text" 
              class="input-field" 
              placeholder="Filtrar por nome ou email..." 
            />
          </div>
        </div>

        <div class="input-group">
          <label>Tipo de Ação</label>
          <select v-model="selectedAction" class="input-field">
            <option value="">Todas as Ações</option>
            <option v-for="type in actionTypes" :key="type" :value="type">
              {{ formatActionType(type) }}
            </option>
          </select>
        </div>

        <div class="input-group">
          <label>Data de Início</label>
          <div class="input-with-icon">
            <Calendar :size="16" class="input-icon" />
            <input 
              v-model="startDate" 
              type="date" 
              class="input-field" 
            />
          </div>
        </div>

        <div class="input-group">
          <label>Data Limite</label>
          <div class="input-with-icon">
            <Calendar :size="16" class="input-icon" />
            <input 
              v-model="endDate" 
              type="date" 
              class="input-field" 
            />
          </div>
        </div>
      </div>
    </div>

    <!-- Summary Stats -->
    <div class="logs-summary mb-4">
      <span class="summary-text text-muted">
        A mostrar <strong>{{ filteredLogs.length }}</strong> de <strong>{{ logs.length }}</strong> registos encontrados
      </span>
      <button class="btn-sort" @click="toggleSort" title="Alternar ordenação cronológica">
        <ArrowUpDown :size="16" />
        {{ sortDesc ? 'Mais Recentes Primeiro' : 'Mais Antigos Primeiro' }}
      </button>
    </div>

    <!-- Main List/Table Panel -->
    <div class="glass-panel main-logs-panel">
      <!-- Loading State -->
      <div v-if="loading" class="loading-state py-12">
        <RefreshCw :size="40" class="spin-icon text-cyan mb-4" />
        <p class="text-muted">A ler a base de dados de auditoria...</p>
      </div>

      <!-- Error State -->
      <div v-else-if="error" class="error-state py-12 px-6 text-center">
        <AlertTriangle :size="48" class="text-danger mb-4" />
        <h3 class="text-danger mb-2">Erro de Acesso</h3>
        <p class="text-muted max-w-md mx-auto">{{ error }}</p>
      </div>

      <!-- Empty State -->
      <div v-else-if="filteredLogs.length === 0" class="empty-state py-12">
        <Database :size="40" class="text-muted mb-4" />
        <p class="text-muted mb-2">Nenhum registo de auditoria corresponde aos filtros aplicados.</p>
        <button v-if="searchEmail || selectedAction || startDate || endDate" class="btn btn-outline btn-sm" @click="resetFilters">
          Limpar Filtros
        </button>
      </div>

      <!-- Logs Table -->
      <div v-else class="table-responsive">
        <table class="logs-table">
          <thead>
            <tr>
              <th class="w-date">Data / Hora</th>
              <th class="w-action">Ação</th>
              <th class="w-user">Utilizador</th>
              <th class="w-details">Detalhes do Evento</th>
              <th class="w-session">Sessão</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="log in filteredLogs" :key="log.id" class="log-row">
              <td class="cell-date">
                <div class="date-container">
                  <Clock :size="14" class="text-muted" />
                  <span class="fira-code">{{ formatDate(log.timestamp) }}</span>
                </div>
              </td>
              <td>
                <span :class="['badge', getActionBadgeClass(log.acaoTipo)]">
                  {{ formatActionType(log.acaoTipo) }}
                </span>
              </td>
              <td class="cell-user">
                <div class="user-info-container">
                  <div class="user-avatar-sm">
                    {{ log.utilizadorNome.charAt(0).toUpperCase() }}
                  </div>
                  <div>
                    <div class="user-name">{{ log.utilizadorNome }}</div>
                    <div class="user-email fira-code text-muted">{{ log.utilizadorEmail }}</div>
                  </div>
                </div>
              </td>
              <td class="cell-details">
                <div class="details-text" :title="log.detalhes">{{ log.detalhes }}</div>
              </td>
              <td class="cell-session">
                <span class="fira-code session-badge" :title="log.sessionId">
                  {{ log.sessionId.slice(0, 12) }}...
                </span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<style scoped>
.mb-6 { margin-bottom: 1.5rem; }
.mb-4 { margin-bottom: 1rem; }
.mb-2 { margin-bottom: 0.5rem; }
.mt-4 { margin-top: 1rem; }
.py-12 { padding-top: 3rem; padding-bottom: 3rem; }
.px-6 { padding-left: 1.5rem; padding-right: 1.5rem; }
.text-center { text-align: center; }

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 1rem;
}

.page-title {
  font-size: 1.75rem;
  font-weight: 700;
  margin: 0 0 0.25rem 0;
  color: var(--text-main);
}

.page-subtitle {
  color: var(--text-muted);
  font-size: 0.9rem;
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 0.75rem;
}

/* Glass Panels */
.glass-panel {
  background: var(--bg-surface);
  border: 1px solid var(--border-light);
  border-radius: 1rem;
  padding: 1.5rem;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
  backdrop-filter: blur(8px);
}

/* Security Banner */
.security-banner {
  display: flex;
  gap: 1rem;
  background: rgba(20, 184, 166, 0.04);
  border-color: rgba(20, 184, 166, 0.15);
}

.banner-icon-container {
  display: flex;
  align-items: flex-start;
  padding-top: 0.25rem;
}

.banner-body {
  flex: 1;
}

.banner-title {
  font-size: 0.95rem;
  font-weight: 700;
  color: var(--accent-teal);
  margin: 0 0 0.35rem 0;
}

.banner-desc {
  font-size: 0.85rem;
  color: var(--text-muted);
  line-height: 1.5;
  margin: 0;
}

/* Filter Bar */
.filter-bar {
  background: rgba(0, 0, 0, 0.2);
}

.filter-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.filter-header-title {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-weight: 700;
  font-size: 0.9rem;
  color: var(--text-main);
}

.btn-clear-filters {
  background: transparent;
  border: none;
  color: var(--danger);
  font-size: 0.8rem;
  font-weight: 600;
  cursor: pointer;
  padding: 0.25rem 0.5rem;
  border-radius: 0.25rem;
  transition: background 0.2s;
}

.btn-clear-filters:hover {
  background: rgba(239, 68, 68, 0.1);
}

.filter-inputs-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1rem;
}

/* Buttons */
.btn {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.65rem 1.25rem;
  border-radius: 0.5rem;
  font-weight: 600;
  font-size: 0.875rem;
  cursor: pointer;
  transition: all 0.2s;
}

.class-btn-primary {
  background: var(--accent-blue);
  color: #fff;
  border: none;
}

.class-btn-primary:hover {
  background: #0284c7;
}

.btn-outline {
  background: transparent;
  color: var(--text-main);
  border: 1px solid var(--border-light);
}

.btn-outline:hover:not(:disabled) {
  border-color: var(--accent-blue);
  color: var(--accent-blue);
}

.btn-outline:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-sm {
  padding: 0.4rem 0.85rem;
  font-size: 0.8rem;
}

/* Summary stats & sorting */
.logs-summary {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.85rem;
}

.btn-sort {
  background: transparent;
  border: 1px solid var(--border-light);
  color: var(--text-muted);
  padding: 0.40rem 0.85rem;
  border-radius: 0.5rem;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-weight: 500;
  transition: all 0.2s;
}

.btn-sort:hover {
  color: var(--text-main);
  border-color: var(--accent-blue);
}

/* Tables & Lists */
.main-logs-panel {
  padding: 0;
  overflow: hidden;
  border-radius: 1rem;
}

.table-responsive {
  width: 100%;
  overflow-x: auto;
}

.logs-table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
}

.logs-table th {
  background: rgba(0, 0, 0, 0.3);
  padding: 1rem 1.5rem;
  font-size: 0.75rem;
  font-weight: 700;
  text-transform: uppercase;
  color: var(--text-muted);
  letter-spacing: 0.05em;
  border-bottom: 1px solid var(--border-light);
}

.logs-table td {
  padding: 1.15rem 1.5rem;
  border-bottom: 1px solid var(--border-light);
  font-size: 0.9rem;
  vertical-align: middle;
}

.log-row {
  transition: background-color 0.15s ease;
}

.log-row:hover {
  background: rgba(255, 255, 255, 0.02);
}

/* Width limits */
.w-date { width: 180px; }
.w-action { width: 220px; }
.w-user { width: 260px; }
.w-details { min-width: 300px; }
.w-session { width: 140px; }

/* Custom Badge styles */
.badge {
  display: inline-block;
  font-size: 0.7rem;
  font-weight: 800;
  padding: 0.25rem 0.65rem;
  border-radius: 0.35rem;
  letter-spacing: 0.04em;
  white-space: nowrap;
}

.badge-success {
  background: rgba(16, 185, 129, 0.12);
  color: #10b981;
  border: 1px solid rgba(16, 185, 129, 0.3);
}

.badge-danger {
  background: rgba(239, 68, 68, 0.12);
  color: #ef4444;
  border: 1px solid rgba(239, 68, 68, 0.3);
}

.badge-info {
  background: rgba(6, 182, 212, 0.12);
  color: #06b6d4;
  border: 1px solid rgba(6, 182, 212, 0.3);
}

.badge-warning {
  background: rgba(245, 158, 11, 0.12);
  color: #f59e0b;
  border: 1px solid rgba(245, 158, 11, 0.3);
}

.badge-secondary {
  background: rgba(156, 163, 175, 0.12);
  color: #9ca3af;
  border: 1px solid rgba(156, 163, 175, 0.3);
}

/* Cell Specific layouts */
.date-container {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  white-space: nowrap;
  font-size: 0.825rem;
}

.user-info-container {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.user-avatar-sm {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--accent-blue), var(--accent-purple));
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 0.8rem;
  box-shadow: 0 0 6px rgba(99, 102, 241, 0.3);
  flex-shrink: 0;
}

.user-name {
  font-weight: 600;
  color: var(--text-main);
  line-height: 1.2;
}

.user-email {
  font-size: 0.725rem;
  line-height: 1.2;
}

.details-text {
  max-width: 450px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: normal;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  color: var(--text-main);
  line-height: 1.4;
}

.session-badge {
  font-size: 0.75rem;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid var(--border-light);
  padding: 0.15rem 0.4rem;
  border-radius: 0.25rem;
  color: var(--text-muted);
}

/* Loading & Empty states */
.loading-state, .empty-state, .error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
}

.spin-icon {
  animation: spin 1.2s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.max-w-md {
  max-width: 28rem;
}
.mx-auto {
  margin-left: auto;
  margin-right: auto;
}

/* Inputs */
.input-group {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.input-group label {
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.input-with-icon {
  position: relative;
  display: flex;
  align-items: center;
}

.input-icon {
  position: absolute;
  left: 0.75rem;
  color: var(--text-muted);
  pointer-events: none;
}

.input-with-icon .input-field {
  padding-left: 2.25rem;
}

.input-field {
  width: 100%;
  background: var(--bg-primary);
  border: 1px solid var(--border-light);
  padding: 0.65rem 0.85rem;
  border-radius: 0.5rem;
  color: var(--text-main);
  font-size: 0.875rem;
  outline: none;
  transition: all 0.2s;
}

.input-field:focus {
  border-color: var(--accent-blue);
  box-shadow: 0 0 0 1px rgba(6, 182, 212, 0.3);
}

select.input-field {
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 24 24' stroke='%239ca3af'%3E%3Cpath stroke-linecap='round' stroke-linejoin='round' stroke-width='2' d='M19 9l-7 7-7-7'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 0.75rem center;
  background-size: 1rem;
  padding-right: 2rem;
}

/* Utility Fira code format */
.fira-code {
  font-family: 'Fira Code', monospace;
}
</style>
