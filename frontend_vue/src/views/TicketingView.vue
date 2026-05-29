<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { Bar, Doughnut } from 'vue-chartjs'
import { Chart as ChartJS, Title, Tooltip, Legend, BarElement, CategoryScale, LinearScale, ArcElement } from 'chart.js'
import { Ticket, CreditCard, AlertTriangle, TrendingUp, Download, RefreshCw } from 'lucide-vue-next'

ChartJS.register(Title, Tooltip, Legend, BarElement, CategoryScale, LinearScale, ArcElement)

import { apiFetch, demoModeRef } from '../services/api.js'
const loading = ref(true)
const bilhetica = ref(null)
const metricas = ref(null)

async function fetchBilhetica() {
 loading.value = true
 try {
 const { data } = await apiFetch('/correlacao')
 if (data.status === 'sucesso') {
 bilhetica.value = data.correlacao.bilheticaSimulada || {}
 metricas.value = data.correlacao.metricas || {}
 }
 } catch(e) {
 console.error('Erro ao obter dados de bilhética:', e)
 } finally {
 loading.value = false
 }
}

onMounted(() => fetchBilhetica())
watch(demoModeRef, () => fetchBilhetica())

// Total de validações
const totalValidacoes = computed(() => {
 if (!bilhetica.value) return 0
 return Object.values(bilhetica.value).reduce((s, v) => s + v, 0)
})

// Receita estimada (simulada: média €1.30 por zapping/digital)
const receitaEstimada = computed(() => {
 if (!bilhetica.value) return '0.00'
 const zapping = bilhetica.value['Zapping'] || 0
 return (zapping * 1.30).toFixed(2)
})

// Anomalias (proporção simulada)
const anomalias = computed(() => {
 return Math.round(totalValidacoes.value * 0.002)
})

// Chart Data - Barras
const barChartData = computed(() => {
 if (!bilhetica.value) return { labels: [], datasets: [] }
 return {
 labels: Object.keys(bilhetica.value),
 datasets: [{
 label: 'Validações (Período)',
 backgroundColor: ['#06b6d4', '#8b5cf6', '#14b8a6', '#eab308'],
 borderRadius: 6,
 data: Object.values(bilhetica.value)
 }]
 }
})

// Chart Data - Donut
const donutChartData = computed(() => {
 if (!bilhetica.value) return { labels: [], datasets: [] }
 return {
 labels: Object.keys(bilhetica.value),
 datasets: [{
 backgroundColor: ['#06b6d4', '#8b5cf6', '#14b8a6', '#eab308'],
 borderColor: '#0f172a',
 borderWidth: 3,
 data: Object.values(bilhetica.value),
 hoverOffset: 8,
 }]
 }
})

const barOptions = {
 responsive: true,
 maintainAspectRatio: false,
 plugins: {
 legend: { display: false }
 },
 scales: {
 y: {
 grid: { color: 'rgba(255,255,255,0.04)' },
 ticks: { color: '#94a3b8', font: { family: 'Fira Code' } }
 },
 x: {
 grid: { display: false },
 ticks: { color: '#94a3b8', font: { size: 11 } }
 }
 }
}

const donutOptions = {
  responsive: true,
  maintainAspectRatio: false,
  cutout: '65%',
  plugins: {
    legend: {
      position: 'bottom',
      labels: { color: '#94a3b8', padding: 16, usePointStyle: true, font: { size: 12 } }
    }
  }
}

function exportCSV() {
 if (!bilhetica.value) return
 const lines = ['Perfil;Validações;Percentagem']
 const total = totalValidacoes.value || 1
 Object.entries(bilhetica.value).forEach(([k, v]) => {
 lines.push(`${k};${v};${((v / total) * 100).toFixed(1)}%`)
 })
 lines.push('')
 lines.push(`Total;${total};100%`)
 lines.push(`Receita Estimada (Zapping);€${receitaEstimada.value};`)
 const csv = lines.join('\n')
 const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' })
 const url = URL.createObjectURL(blob)
 const a = document.createElement('a')
 a.href = url
 a.download = `bilhetica_pgu_${new Date().toISOString().split('T')[0]}.csv`
 a.click()
 URL.revokeObjectURL(url)
}
</script>

<template>
 <div class="ticketing-view fade-in">
 <!-- Header -->
 <div class="glass-panel main-header">
 <div class="head-left">
 <h3 class="panel-title">Bilhética Digital — Análise de Validações</h3>
 <p class="panel-desc">Vertical 3.3: Distribuição por perfil de passageiro e receitas</p>
 </div>
 <div class="header-actions">
 <button class="btn btn-secondary btn-sm" @click="fetchBilhetica" :disabled="loading">
 <RefreshCw :size="14" :class="{'spin': loading}" /> Atualizar
 </button>
 <button class="btn btn-secondary btn-sm" @click="exportCSV" :disabled="!bilhetica">
 <Download :size="14" /> CSV
 </button>
 </div>
 </div>

 <!-- KPI Cards -->
 <div class="summary-cards">
 <div class="glass-panel metric-box highlight">
 <div class="metric-icon"><Ticket :size="24" /></div>
 <div class="metric-data">
 <span class="m-val fira-code">{{ totalValidacoes.toLocaleString('pt-PT') }}</span>
 <span class="m-label">Validações Totais</span>
 </div>
 </div>
 <div class="glass-panel metric-box">
 <div class="metric-icon teal"><CreditCard :size="24" /></div>
 <div class="metric-data">
 <span class="m-val fira-code text-teal">€ {{ receitaEstimada }}</span>
 <span class="m-label">Receita Estimada (Zapping)</span>
 </div>
 </div>
 <div class="glass-panel metric-box">
 <div class="metric-icon danger"><AlertTriangle :size="24" /></div>
 <div class="metric-data">
 <span class="m-val fira-code text-danger">{{ anomalias }}</span>
 <span class="m-label">Anomalias / Rejeições</span>
 </div>
 </div>
 <div class="glass-panel metric-box">
 <div class="metric-icon purple"><TrendingUp :size="24" /></div>
 <div class="metric-data">
 <span class="m-val fira-code text-purple">{{ metricas?.ratioProcuraOferta || 0 }}</span>
 <span class="m-label">Ratio Pax/Viagem</span>
 </div>
 </div>
 </div>

 <!-- Charts Grid -->
 <div class="charts-grid mt-4">
 <div class="glass-panel chart-panel">
 <h4 class="chart-title">Distribuição por Perfil de Passageiro</h4>
 <p class="chart-desc">Volume de validações por tipo de título de transporte</p>
 <div class="chart-wrapper">
 <Bar v-if="bilhetica" :data="barChartData" :options="barOptions" />
 <div v-else class="chart-loading">A carregar dados da API...</div>
 </div>
 </div>

 <div class="glass-panel chart-panel">
 <h4 class="chart-title">Proporção de Títulos</h4>
 <p class="chart-desc">Peso relativo de cada tipo de bilhete</p>
 <div class="chart-wrapper donut-wrapper">
 <Doughnut v-if="bilhetica" :data="donutChartData" :options="donutOptions" />
 <div v-else class="chart-loading">A carregar...</div>
 </div>
 </div>
 </div>

 <!-- Detail Table -->
 <div v-if="bilhetica" class="glass-panel detail-table mt-4">
 <h4 class="chart-title">Detalhes por Perfil</h4>
 <table class="data-table">
 <thead>
 <tr>
 <th>Perfil</th>
 <th>Validações</th>
 <th>% do Total</th>
 <th>Indicador</th>
 </tr>
 </thead>
 <tbody>
 <tr v-for="(count, perfil) in bilhetica" :key="perfil">
 <td class="td-perfil">{{ perfil }}</td>
 <td class="fira-code"><strong>{{ count.toLocaleString('pt-PT') }}</strong></td>
 <td class="fira-code dim">{{ totalValidacoes > 0 ? ((count / totalValidacoes) * 100).toFixed(1) : 0 }}%</td>
 <td>
 <div class="td-bar-track">
 <div class="td-bar-fill" :style="{width: (totalValidacoes > 0 ? (count / totalValidacoes) * 100 : 0) + '%'}"></div>
 </div>
 </td>
 </tr>
 </tbody>
 </table>
 <p class="simulated-note">
 <div class="lucide-warning-wrap"><AlertTriangle :size="16" /></div> Dados gerados proporcionalmente às contagens reais dos sensores. A integração com a API de bilhética SCB dos TUB (vertical 3.3) substituirá estas estimativas por dados de validação reais.
 </p>
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

/* KPI Cards */
.summary-cards {
 display: grid;
 grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
 gap: 1.25rem;
 margin-top: 1.25rem;
}
.metric-box {
 display: flex;
 align-items: center;
 gap: 1.25rem;
 padding: 1.25rem;
}
.highlight { border-left: 4px solid var(--accent-purple); }
.metric-icon {
 background: rgba(6, 182, 212, 0.1);
 color: var(--accent-blue);
 padding: 0.85rem;
 border-radius: 0.75rem;
 display: flex;
}
.metric-icon.teal { background: rgba(20, 184, 166, 0.1); color: var(--accent-teal); }
.metric-icon.danger { background: rgba(239, 68, 68, 0.1); color: var(--danger); }
.metric-icon.purple { background: rgba(139, 92, 246, 0.1); color: var(--accent-purple); }
.metric-data { display: flex; flex-direction: column; }
.m-val { font-size: 1.5rem; font-weight: 700; color: var(--text-main); }
.m-label { font-size: 0.78rem; color: var(--text-muted); text-transform: uppercase; font-weight: 500; letter-spacing: 0.04em; }
.text-teal { color: var(--accent-teal); text-shadow: 0 0 12px rgba(20, 184, 166, 0.4); }
.text-danger { color: var(--danger); text-shadow: 0 0 12px rgba(239, 68, 68, 0.4); }
.text-purple { color: var(--accent-purple); }

/* Charts */
.mt-4 { margin-top: 1.25rem; }
.charts-grid { display: grid; grid-template-columns: 1.5fr 1fr; gap: 1.25rem; }
@media (max-width: 900px) { .charts-grid { grid-template-columns: 1fr; } }

.chart-panel { padding: 1.5rem; }
.chart-title { margin-bottom: 0.25rem; font-size: 1rem; }
.chart-desc { color: var(--text-muted); font-size: 0.8rem; margin-bottom: 1.5rem; }
.chart-wrapper { height: 320px; width: 100%; }
.donut-wrapper { height: 300px; display: flex; align-items: center; justify-content: center; }
.chart-loading { display: flex; align-items: center; justify-content: center; height: 100%; color: var(--text-muted); font-style: italic; }

/* Table */
.detail-table { padding: 1.5rem; }
.data-table { width: 100%; border-collapse: collapse; margin-top: 1rem; }
.data-table th {
 text-align: left;
 padding: 0.75rem;
 font-size: 0.75rem;
 text-transform: uppercase;
 color: var(--text-muted);
 font-weight: 600;
 letter-spacing: 0.05em;
 border-bottom: 1px solid var(--border-light);
}
.data-table td {
 padding: 0.85rem 0.75rem;
 border-bottom: 1px solid rgba(255,255,255,0.03);
 font-size: 0.95rem;
}
.data-table tr:hover { background: rgba(6, 182, 212, 0.04); }
.td-perfil { font-weight: 600; }
.dim { color: var(--text-muted); }

.td-bar-track { width: 100%; height: 6px; background: rgba(255,255,255,0.05); border-radius: 3px; overflow: hidden; }
.td-bar-fill { height: 100%; background: linear-gradient(90deg, var(--accent-teal), var(--accent-blue)); border-radius: 3px; transition: width 0.5s ease; }

.simulated-note {
 margin-top: 1.25rem;
 font-size: 0.8rem;
 color: var(--text-muted);
 font-style: italic;
 border-left: 3px solid var(--warning);
 padding-left: 0.75rem;
}
</style>
