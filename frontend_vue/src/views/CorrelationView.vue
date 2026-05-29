<script setup>
import { ref, onMounted, computed, watch } from 'vue'

import { apiFetch, demoModeRef } from '../services/api.js'
const loading = ref(false)
const err = ref('')
const correlacao = ref(null)

const dataInicio = ref('')
const dataFim = ref('')

// Inicializar com últimos 30 dias
onMounted(() => {
 const hoje = new Date()
 const inicio = new Date(hoje)
 inicio.setDate(inicio.getDate() - 30)
 dataFim.value = hoje.toISOString().split('T')[0]
 dataInicio.value = inicio.toISOString().split('T')[0]
 carregarCorrelacao()
})

watch(demoModeRef, () => carregarCorrelacao())

async function carregarCorrelacao() {
 loading.value = true
 err.value = ''
 try {
 const { data } = await apiFetch(`/correlacao?dataInicio=${dataInicio.value}&dataFim=${dataFim.value}`)
 if (data.status === 'sucesso') {
 correlacao.value = data.correlacao
 } else {
 err.value = data.mensagem || 'Erro desconhecido.'
 }
 } catch (e) {
 err.value = 'Falha na ligação ao servidor. Verifique que o backend está ativo.'
 } finally {
 loading.value = false
 }
}

// Computed para o gráfico horizontal de barras (CSS puro)
const maxEntradas = computed(() => {
 if (!correlacao.value?.procuraPorLinha?.length) return 1
 return Math.max(...correlacao.value.procuraPorLinha.map(l => l.totalEntradas), 1)
})

// Cor do ratio
function ratioColor(ratio) {
 if (ratio < 5) return 'var(--text-muted)'
 if (ratio < 15) return 'var(--accent-teal)'
 if (ratio < 30) return 'var(--warning)'
 return 'var(--danger)'
}

// Formato hora
function fmtHora(h) {
 return `${String(h).padStart(2, '0')}:00`
}

// Max hora para barra
const maxHoraEntradas = computed(() => {
 if (!correlacao.value?.procuraPorHora?.length) return 1
 return Math.max(...correlacao.value.procuraPorHora.map(h => h.entradas), 1)
})

function exportCSV() {
 if (!correlacao.value) return
 const lines = ['Linha;Entradas;Saídas;Dias com Dados;Total Leituras']
 correlacao.value.procuraPorLinha.forEach(l => {
 lines.push(`${l.linhaId};${l.totalEntradas};${l.totalSaidas};${l.diasComDados};${l.totalLeituras}`)
 })
 lines.push('')
 lines.push('Hora;Entradas;Saídas')
 correlacao.value.procuraPorHora.forEach(h => {
 lines.push(`${h.hora}:00;${h.entradas};${h.saidas}`)
 })
 lines.push('')
 lines.push('Perfil Bilhética;Validações')
 Object.entries(correlacao.value.bilheticaSimulada).forEach(([k, v]) => {
 lines.push(`${k};${v}`)
 })
 const csv = lines.join('\n')
 const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' })
 const url = URL.createObjectURL(blob)
 const a = document.createElement('a')
 a.href = url
 a.download = `correlacao_pgu_${dataInicio.value}_${dataFim.value}.csv`
 a.click()
 URL.revokeObjectURL(url)
}
</script>

<template>
 <div class="correlation-view fade-in">
 <!-- Header -->
 <div class="glass-panel main-header">
 <div class="head-left">
 <h3 class="panel-title">Motor de Correlação — Procura vs Oferta</h3>
 <p class="panel-desc">UC 4.3: Cruzamento de dados de contagem com GTFS e bilhética</p>
 </div>
 <div class="header-controls">
 <div class="date-picker">
 <label>De</label>
 <input type="date" v-model="dataInicio" class="date-input fira-code" />
 </div>
 <div class="date-picker">
 <label>Até</label>
 <input type="date" v-model="dataFim" class="date-input fira-code" />
 </div>
 <button class="btn btn-primary" @click="carregarCorrelacao" :disabled="loading">
 Correlacionar
 </button>
 <button v-if="correlacao" class="btn btn-secondary" @click="exportCSV">
 CSV
 </button>
 </div>
 </div>

 <!-- Error -->
 <div v-if="err" class="error-banner fade-in">
 {{ err }}
 </div>

 <!-- Métricas KPI -->
 <div v-if="correlacao" class="kpi-grid mt-4">
 <div class="glass-panel kpi-card">
 <div class="kpi-info">
 <span class="kpi-value fira-code">{{ correlacao.metricas.totalPassageirosContados }}</span>
 <span class="kpi-label">Passageiros Contados</span>
 </div>
 </div>
 <div class="glass-panel kpi-card">
 <div class="kpi-info">
 <span class="kpi-value fira-code">{{ correlacao.metricas.totalViagensProgramadas }}</span>
 <span class="kpi-label">Viagens Programadas (GTFS)</span>
 </div>
 </div>
 <div class="glass-panel kpi-card highlight">
 <div class="kpi-info">
 <span class="kpi-value fira-code" :style="{color: ratioColor(correlacao.metricas.ratioProcuraOferta)}">
 {{ correlacao.metricas.ratioProcuraOferta }}
 </span>
 <span class="kpi-label">Ratio Passageiros/Viagem</span>
 </div>
 </div>
 <div class="glass-panel kpi-card">
 <div class="kpi-info">
 <span class="kpi-value fira-code text-cyan">{{ correlacao.metricas.periodoInicio }}</span>
 <span class="kpi-label">→ {{ correlacao.metricas.periodoFim }}</span>
 </div>
 </div>
 </div>

 <!-- Content Grid -->
 <div v-if="correlacao" class="content-grid mt-4">

 <!-- Procura por Linha -->
 <div class="glass-panel section">
 <h4>Procura Real por Linha</h4>
 <p class="section-desc">Volume de passageiros contados por sensores/QR em cada linha</p>
 <div v-if="correlacao.procuraPorLinha.length === 0" class="empty-state">
 Sem dados de contagem no período seleccionado.
 </div>
 <div class="bar-list">
 <div v-for="l in correlacao.procuraPorLinha" :key="l.linhaId" class="bar-row">
 <span class="bar-label fira-code">{{ l.linhaId }}</span>
 <div class="bar-track">
 <div class="bar-fill" :style="{width: (l.totalEntradas / maxEntradas * 100) + '%'}"></div>
 </div>
 <span class="bar-value fira-code">{{ l.totalEntradas }}</span>
 <span class="bar-detail dim">{{ l.diasComDados }}d · {{ l.totalLeituras }} leit.</span>
 </div>
 </div>
 </div>

 <!-- Oferta GTFS -->
 <div class="glass-panel section">
 <h4>Oferta Planeada (GTFS)</h4>
 <p class="section-desc">Viagens programadas extraídas dos horários importados</p>
 <div v-if="correlacao.ofertaPorLinha.length === 0" class="empty-state">
 Sem dados GTFS. Execute <code>import_horarios.py</code> primeiro.
 </div>
 <div class="oferta-grid">
 <div v-for="o in correlacao.ofertaPorLinha" :key="o.linhaId + o.tipoDia" class="oferta-card">
 <span class="oferta-linha fira-code text-cyan">{{ o.linhaId }}</span>
 <span class="oferta-tipo">{{ o.tipoDia === 'UTIL' ? 'Dias Úteis' : 'Fim Semana' }}</span>
 <span class="oferta-count fira-code">{{ o.viagensProgramadas }} viagens</span>
 </div>
 </div>
 </div>

 <!-- Distribuição Horária -->
 <div class="glass-panel section">
 <h4>Distribuição Horária da Procura</h4>
 <p class="section-desc">Picos de hora de ponta identificados no período</p>
 <div v-if="correlacao.procuraPorHora.length === 0" class="empty-state">
 Sem dados horários no período.
 </div>
 <div class="hora-chart">
 <div v-for="h in correlacao.procuraPorHora" :key="h.hora" class="hora-bar-wrapper">
 <div class="hora-bar" :style="{height: (h.entradas / maxHoraEntradas * 100) + '%'}"
 :class="{'hora-peak': h.entradas === maxHoraEntradas}">
 </div>
 <span class="hora-label fira-code">{{ fmtHora(h.hora) }}</span>
 </div>
 </div>
 </div>

 <!-- Bilhética Simulada -->
 <div class="glass-panel section">
 <h4>Bilhética Simulada (Perfis)</h4>
 <p class="section-desc">Distribuição estimada por tipo de título de transporte</p>
 <div class="perfil-list">
 <div v-for="(count, perfil) in correlacao.bilheticaSimulada" :key="perfil" class="perfil-row">
 <span class="perfil-name">{{ perfil }}</span>
 <span class="perfil-count fira-code" :class="{'text-cyan': count > 0}">{{ count }}</span>
 </div>
 </div>
 <p class="simulated-note">
 Dados simulados proporcionalmente às entradas reais. A integração com a API de bilhética (vertical 3.3) substituirá estes valores.
 </p>
 </div>
 </div>
 </div>
</template>

<style scoped>
.main-header { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 1rem; }
.panel-title { display: flex; align-items: center; gap: 0.5rem; margin-bottom: 0.25rem; }
.panel-desc { color: var(--text-muted); font-size: 0.85rem; margin: 0; }
.icon-inline { color: var(--accent-blue); }

.header-controls { display: flex; align-items: flex-end; gap: 1rem; flex-wrap: wrap; }
.date-picker { display: flex; flex-direction: column; gap: 0.25rem; }
.date-picker label { font-size: 0.75rem; color: var(--text-muted); text-transform: uppercase; font-weight: 600; }
.date-input {
 background: var(--bg-primary);
 border: 1px solid var(--border-light);
 color: var(--text-main);
 padding: 0.5rem 0.75rem;
 border-radius: 0.4rem;
 font-size: 0.85rem;
}
.date-input:focus { border-color: var(--accent-blue); outline: none; }

.spin { animation: spin 1s linear infinite; }
@keyframes spin { 100% { transform: rotate(360deg); } }

.error-banner {
 background: rgba(239, 68, 68, 0.1);
 color: var(--danger);
 border: 1px solid var(--danger);
 padding: 1rem;
 border-radius: 0.5rem;
 display: flex;
 align-items: center;
 gap: 0.75rem;
 margin-top: 1.5rem;
}

.mt-4 { margin-top: 1.5rem; }
.text-cyan { color: var(--accent-blue); }
.dim { color: var(--text-muted); font-size: 0.8rem; }

/* KPI Grid */
.kpi-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 1.5rem; }
.kpi-card { display: flex; align-items: center; gap: 1.25rem; padding: 1.25rem; }
.kpi-card.highlight { border-left: 4px solid var(--accent-purple); }
.kpi-icon { background: rgba(6, 182, 212, 0.1); color: var(--accent-blue); padding: 0.85rem; border-radius: 0.75rem; display: flex; }
.kpi-info { display: flex; flex-direction: column; }
.kpi-value { font-size: 1.4rem; font-weight: 700; color: var(--text-main); }
.kpi-label { font-size: 0.8rem; color: var(--text-muted); font-weight: 500; text-transform: uppercase; letter-spacing: 0.04em; }

/* Content Grid */
.content-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 1.5rem; }
@media (max-width: 900px) { .content-grid { grid-template-columns: 1fr; } }

.section h4 { display: flex; align-items: center; gap: 0.5rem; margin-bottom: 0.25rem; font-size: 1rem; }
.section-desc { color: var(--text-muted); font-size: 0.8rem; margin-bottom: 1.25rem; }

.empty-state { text-align: center; color: var(--text-muted); font-style: italic; padding: 2rem; }

/* Bar List */
.bar-list { display: flex; flex-direction: column; gap: 0.75rem; }
.bar-row { display: flex; align-items: center; gap: 0.75rem; }
.bar-label { width: 60px; font-weight: 600; font-size: 0.85rem; color: var(--accent-blue); }
.bar-track { flex: 1; height: 8px; background: var(--bg-primary); border-radius: 4px; overflow: hidden; }
.bar-fill { height: 100%; background: linear-gradient(90deg, var(--accent-teal), var(--accent-blue)); border-radius: 4px; transition: width 0.5s ease; }
.bar-value { width: 50px; text-align: right; font-weight: 600; font-size: 0.9rem; }
.bar-detail { min-width: 80px; text-align: right; }

/* Oferta Grid */
.oferta-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(140px, 1fr)); gap: 0.75rem; }
.oferta-card { background: rgba(0,0,0,0.2); border: 1px solid var(--border-light); border-radius: 0.5rem; padding: 0.85rem; display: flex; flex-direction: column; gap: 0.3rem; }
.oferta-linha { font-size: 1rem; font-weight: 700; }
.oferta-tipo { font-size: 0.75rem; color: var(--text-muted); text-transform: uppercase; }
.oferta-count { font-size: 0.9rem; font-weight: 600; }

/* Hora Chart */
.hora-chart { display: flex; align-items: flex-end; gap: 3px; height: 140px; padding-top: 0.5rem; }
.hora-bar-wrapper { flex: 1; display: flex; flex-direction: column; align-items: center; height: 100%; justify-content: flex-end; }
.hora-bar { width: 100%; background: var(--accent-teal); border-radius: 3px 3px 0 0; transition: height 0.4s ease; min-height: 2px; }
.hora-bar.hora-peak { background: var(--accent-blue); box-shadow: 0 0 8px rgba(6, 182, 212, 0.5); }
.hora-label { font-size: 0.6rem; color: var(--text-muted); margin-top: 0.3rem; writing-mode: vertical-rl; transform: rotate(180deg); }

/* Perfil List */
.perfil-list { display: flex; flex-direction: column; gap: 0.75rem; }
.perfil-row { display: flex; justify-content: space-between; align-items: center; padding: 0.6rem 0; border-bottom: 1px solid var(--border-light); }
.perfil-row:last-child { border-bottom: none; }
.perfil-name { font-weight: 500; }
.perfil-count { font-size: 1.1rem; font-weight: 600; }

.simulated-note { margin-top: 1rem; font-size: 0.8rem; color: var(--text-muted); font-style: italic; border-left: 3px solid var(--warning); padding-left: 0.75rem; }
</style>
