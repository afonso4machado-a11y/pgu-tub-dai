<script setup>
import { ref, onMounted, onUnmounted, computed, watch } from 'vue'
import { Activity, Radio, AlertOctagon, Calendar, ChevronLeft, ChevronRight } from 'lucide-vue-next'

import { apiFetch, demoModeRef } from '../services/api.js'
const telemetries = ref([])
const criticalBus = ref(null)
let timer = null

// Calendário
const historico = ref({}) // { "2026-04-11": { "TUB-101": { entradas: 120, saidas: 115 } } }
const calendarDate = ref(new Date()) // mês/ano atualmente visível
const selectedDay = ref(null) // dia selecionado: "2026-04-11"
const selectedBus = ref(null) // filtro por autocarro

// Computar dias do mês
const calendarDays = computed(() => {
 const year = calendarDate.value.getFullYear()
 const month = calendarDate.value.getMonth()
 const firstDay = new Date(year, month, 1).getDay() // 0=Dom
 const daysInMonth = new Date(year, month + 1, 0).getDate()
 const days = []
 // Preencher dias vazios antes
 for (let i = 0; i < (firstDay === 0 ? 6 : firstDay - 1); i++) days.push(null)
 for (let d = 1; d <= daysInMonth; d++) {
 const key = `${year}-${String(month + 1).padStart(2, '0')}-${String(d).padStart(2, '0')}`
 days.push({ day: d, key, data: historico.value[key] || null })
 }
 return days
})

const calendarTitle = computed(() => {
 return calendarDate.value.toLocaleDateString('pt-PT', { month: 'long', year: 'numeric' })
})

// Dados do dia selecionado (com filtro de autocarro)
const selectedDayData = computed(() => {
 if (!selectedDay.value) return null
 const dayData = historico.value[selectedDay.value]
 if (!dayData) return null
 return selectedBus.value
 ? { [selectedBus.value]: dayData[selectedBus.value] }
 : dayData
})

// Lista de todos os autocarros com dados históricos
const busesWithHistory = computed(() => {
 const ids = new Set()
 Object.values(historico.value).forEach(day => Object.keys(day).forEach(id => ids.add(id)))
 return [...ids].sort()
})

// Intensidade da cor do dia no calendário baseada no volume total
function dayHeat(data) {
 if (!data) return 0
 const total = Object.values(data).reduce((s, v) => s + (v.entradas || 0), 0)
 if (total === 0) return 0
 if (total < 50) return 1
 if (total < 200) return 2
 if (total < 500) return 3
 return 4
}

function prevMonth() {
 const d = new Date(calendarDate.value)
 d.setMonth(d.getMonth() - 1)
 calendarDate.value = d
}

function nextMonth() {
 const d = new Date(calendarDate.value)
 d.setMonth(d.getMonth() + 1)
 calendarDate.value = d
}

// ---- Telemetria em tempo real ----
async function fetchData() {
 try {
 const { data: res } = await apiFetch('/autocarros')
 if (res.status === 'sucesso') {
 telemetries.value = res.autocarros.map(a => ({
 id: a.id,
 route: a.linhaId,
 occ: Math.round(a.ocupacao),
 trend: 'stable',
 time: a.ultimaLeitura !== 'N/A' ? a.ultimaLeitura.split('T')[1].split('.')[0] : '--:--:--'
 }))
 const sorted = [...telemetries.value].sort((a, b) => b.occ - a.occ)
 criticalBus.value = (sorted.length > 0 && sorted[0].occ > 80) ? sorted[0] : null
 }
 } catch (e) { console.error("Erro telemetria:", e) }
}

// ---- Histórico ----
async function fetchHistorico() {
 try {
 const { data: res } = await apiFetch('/historico')
 if (res.status === 'sucesso') historico.value = res.historico
 } catch (e) { console.error("Erro histórico:", e) }
}

onMounted(() => {
 fetchData()
 fetchHistorico()
 timer = setInterval(fetchData, 5000)
})
onUnmounted(() => { if (timer) clearInterval(timer) })

watch(demoModeRef, () => {
 fetchData()
 fetchHistorico()
})
</script>

<template>
 <div class="occupancy-view fade-in">
 <!-- Header -->
 <div class="glass-panel main-header">
 <div class="head-left">
 <h3 class="panel-title"><Radio class="icon-inline pulse"/> Telemetria de Lotação Em Tempo Real</h3>
 <p class="panel-desc">Vertical 3.4: Stream direto dos sensores de portas e contadores stereoscópicos</p>
 </div>
 <button class="btn btn-secondary" @click="fetchHistorico">Atualizar Histórico</button>
 </div>

 <!-- Telemetria em grelha -->
 <div class="feed-grid mt-4">
 <div class="glass-panel feed-panel">
 <h4 class="mb-4">Fluxo de Dados ao Vivo</h4>
 <div v-if="telemetries.length === 0" class="empty-telemetry">Sem viaturas na frota. Registe autocarros no separador Frota.</div>
 <div class="telemetry-list">
 <div v-for="t in telemetries" :key="t.id" class="t-row scale-in">
 <div class="t-time fira-code">{{ t.time }}</div>
 <div class="t-id fira-code text-cyan">{{ t.id }}</div>
 <div class="t-route">{{ t.route }}</div>
 <div class="t-bar-container">
 <div class="t-bar"
 :style="{width: t.occ + '%'}"
 :class="{'bar-critical': t.occ > 90, 'bar-warning': t.occ > 70 && t.occ <= 90}">
 </div>
 </div>
 <div class="t-occ fira-code" :class="{'text-danger': t.occ > 90}">{{ t.occ }}%</div>
 </div>
 </div>
 </div>

 <div class="alerts-panel">
 <div v-if="criticalBus" class="glass-panel critical-bg fade-in">
 <h4 class="alert-title"><AlertOctagon /> Atenção Requerida</h4>
 <p class="alert-text">O veículo <strong>{{ criticalBus.id }}</strong> excedeu a lotação de segurança ({{ criticalBus.occ }}%). Recomenda-se reforço para a Linha {{ criticalBus.route }}.</p>
 <button class="btn btn-secondary mt-2">Avisar Motorista</button>
 </div>
 <div v-else class="glass-panel safe-bg fade-in">
 <h4 class="alert-title text-success"><Activity /> Operação Estável</h4>
 <p class="alert-text">Nenhuma viatura em estado crítico. A frota opera dentro dos parâmetros de segurança.</p>
 </div>
 </div>
 </div>

 <!-- Secção Calendário Histórico -->
 <div class="glass-panel calendar-section mt-4">
 <div class="cal-header">
 <h4 class="cal-title"><Calendar class="icon-inline" /> Histórico de Ocupação por Dia</h4>
 <div class="cal-nav">
 <button class="nav-btn" @click="prevMonth"><ChevronLeft :size="18"/></button>
 <span class="cal-month">{{ calendarTitle }}</span>
 <button class="nav-btn" @click="nextMonth"><ChevronRight :size="18"/></button>
 </div>
 <select v-model="selectedBus" class="bus-select">
 <option :value="null">Todos os Autocarros</option>
 <option v-for="id in busesWithHistory" :key="id" :value="id">{{ id }}</option>
 </select>
 </div>

 <!-- Dias da semana -->
 <div class="cal-grid">
 <div class="cal-weekday" v-for="d in ['Seg','Ter','Qua','Qui','Sex','Sáb','Dom']" :key="d">{{ d }}</div>
 <div
 v-for="(d, i) in calendarDays"
 :key="i"
 class="cal-cell"
 :class="{
 'cal-empty': !d,
 'cal-selected': d && d.key === selectedDay,
 [`heat-${d ? dayHeat(d.data) : 0}`]: true
 }"
 @click="d && (selectedDay = d.key === selectedDay ? null : d.key)"
 >
 <span v-if="d" class="cal-day-num">{{ d.day }}</span>
 <span v-if="d && d.data" class="cal-dot"></span>
 </div>
 </div>

 <!-- Legenda de calor -->
 <div class="heat-legend">
 <span class="legend-label">Sem dados</span>
 <div class="heat-0 heat-sample"></div>
 <div class="heat-1 heat-sample"></div>
 <div class="heat-2 heat-sample"></div>
 <div class="heat-3 heat-sample"></div>
 <div class="heat-4 heat-sample"></div>
 <span class="legend-label">Alto Volume</span>
 </div>

 <!-- Detalhe do dia selecionado -->
 <div v-if="selectedDay && selectedDayData" class="day-detail fade-in">
 <h5 class="day-detail-title">
 Dados de {{ new Date(selectedDay + 'T12:00:00').toLocaleDateString('pt-PT', {weekday: 'long', day: 'numeric', month: 'long'}) }}
 </h5>
 <div class="detail-grid">
 <div v-for="(dados, busId) in selectedDayData" :key="busId" class="detail-card">
 <div class="detail-bus-id fira-code">{{ busId }}</div>
 <div class="detail-row">
 <span class="detail-label">Entradas</span>
 <span class="detail-val text-cyan">{{ dados.entradas }}</span>
 </div>
 <div class="detail-row">
 <span class="detail-label">Saídas</span>
 <span class="detail-val">{{ dados.saidas }}</span>
 </div>
 <div class="detail-row">
 <span class="detail-label">Balanço</span>
 <span class="detail-val" :class="{'text-success': dados.entradas >= dados.saidas, 'text-danger': dados.entradas < dados.saidas}">
 {{ dados.entradas - dados.saidas >= 0 ? '+' : '' }}{{ dados.entradas - dados.saidas }}
 </span>
 </div>
 </div>
 </div>
 <p class="paragens-note">
 <AlertTriangle :size="14" class="icon-inline" /> A divisão por paragens estará disponível quando os horários com paragens forem configurados na base de dados.
 </p>
 </div>
 <div v-else-if="selectedDay" class="no-data-day">
 Sem dados de leituras para este dia.
 </div>
 </div>

 </div>
</template>

<style scoped>
.main-header { display: flex; justify-content: space-between; align-items: center; }
.panel-title { display: flex; align-items: center; gap: 0.5rem; margin-bottom: 0.25rem; }
.panel-desc { color: var(--text-muted); font-size: 0.85rem; margin: 0; }
.icon-inline { color: var(--accent-blue); }

.pulse { animation: pulse-glow 2s infinite; }
@keyframes pulse-glow {
 0% { filter: drop-shadow(0 0 4px rgba(6, 182, 212, 0.2)); }
 50% { filter: drop-shadow(0 0 12px rgba(6, 182, 212, 0.8)); }
 100% { filter: drop-shadow(0 0 4px rgba(6, 182, 212, 0.2)); }
}

.mt-4 { margin-top: 1.5rem; }
.mt-2 { margin-top: 1rem; }
.mb-4 { margin-bottom: 1.5rem; }

.feed-grid { display: grid; grid-template-columns: 2fr 1fr; gap: 1.5rem; }
.telemetry-list { display: flex; flex-direction: column; gap: 0.75rem; }
.empty-telemetry { color: var(--text-muted); text-align: center; padding: 2rem; font-style: italic; }

.t-row { display: flex; align-items: center; gap: 1rem; padding: 1rem; background: rgba(0,0,0,0.2); border: 1px solid var(--border-light); border-radius: 0.5rem; }
.t-time { font-size: 0.8rem; color: var(--text-muted); }
.t-id { font-weight: 600; width: 80px; }
.text-cyan { color: var(--accent-blue); }
.text-danger { color: var(--danger); font-weight: 700; }
.text-success { color: var(--accent-teal) !important; }
.t-route { background: var(--bg-primary); padding: 0.2rem 0.6rem; border-radius: 4px; font-size: 0.8rem; font-weight: 600; }
.t-bar-container { flex: 1; height: 6px; background: var(--bg-primary); border-radius: 3px; overflow: hidden; }
.t-bar { height: 100%; background: var(--accent-teal); transition: width 0.5s ease; }
.bar-warning { background: var(--warning); }
.bar-critical { background: var(--danger); }
.t-occ { width: 50px; text-align: right; }

.critical-bg { border-color: rgba(239,68,68,0.3); background: linear-gradient(180deg, rgba(239,68,68,0.1) 0%, rgba(15,23,42,0.8) 100%); box-shadow: 0 0 20px rgba(239,68,68,0.1); }
.safe-bg { border-color: rgba(20,184,166,0.2); background: linear-gradient(180deg, rgba(20,184,166,0.05) 0%, rgba(15,23,42,0.8) 100%); }
.alert-title { display: flex; align-items: center; gap: 0.5rem; color: var(--danger); margin-bottom: 1rem; }
.alert-text { line-height: 1.5; font-size: 0.95rem; }

/* ---- Calendário ---- */
.calendar-section { padding: 1.5rem; }
.cal-header { display: flex; align-items: center; gap: 1rem; flex-wrap: wrap; margin-bottom: 1.5rem; }
.cal-title { display: flex; align-items: center; gap: 0.5rem; margin: 0; flex: 1; font-size: 1rem; }
.cal-nav { display: flex; align-items: center; gap: 0.5rem; }
.cal-month { font-weight: 600; font-size: 0.95rem; min-width: 140px; text-align: center; text-transform: capitalize; }
.nav-btn { background: var(--bg-primary); border: 1px solid var(--border-light); color: var(--text-main); border-radius: 0.4rem; padding: 0.3rem 0.5rem; cursor: pointer; display: flex; align-items: center; transition: background 0.2s; }
.nav-btn:hover { background: var(--bg-surface); }
.bus-select { background: var(--bg-primary); border: 1px solid var(--border-light); color: var(--text-main); border-radius: 0.4rem; padding: 0.4rem 0.75rem; font-size: 0.9rem; cursor: pointer; }

.cal-grid { display: grid; grid-template-columns: repeat(7, 1fr); gap: 4px; }
.cal-weekday { text-align: center; font-size: 0.75rem; font-weight: 600; color: var(--text-muted); padding: 0.4rem 0; text-transform: uppercase; }
.cal-cell { aspect-ratio: 1; border-radius: 0.4rem; display: flex; flex-direction: column; align-items: center; justify-content: center; cursor: pointer; position: relative; border: 1px solid transparent; transition: border-color 0.2s, transform 0.15s; }
.cal-cell:not(.cal-empty):hover { border-color: var(--accent-blue); transform: scale(1.05); }
.cal-cell.cal-selected { border-color: var(--accent-blue) !important; box-shadow: 0 0 0 2px rgba(6,182,212,0.3); }
.cal-empty { cursor: default; }
.cal-day-num { font-size: 0.85rem; font-weight: 500; }
.cal-dot { width: 5px; height: 5px; border-radius: 50%; background: var(--accent-teal); margin-top: 2px; }

/* Calor */
.heat-0 { background: rgba(0,0,0,0.15); }
.heat-1 { background: rgba(20, 184, 166, 0.12); }
.heat-2 { background: rgba(20, 184, 166, 0.28); }
.heat-3 { background: rgba(234, 179, 8, 0.3); }
.heat-4 { background: rgba(239, 68, 68, 0.3); }

.heat-legend { display: flex; align-items: center; gap: 4px; margin-top: 0.75rem; font-size: 0.75rem; color: var(--text-muted); }
.heat-sample { width: 16px; height: 16px; border-radius: 3px; }
.legend-label { margin: 0 4px; }

/* Detalhe */
.day-detail { margin-top: 1.5rem; border-top: 1px solid var(--border-light); padding-top: 1.5rem; }
.day-detail-title { font-size: 1rem; font-weight: 600; color: var(--accent-blue); margin-bottom: 1rem; text-transform: capitalize; }
.detail-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(160px, 1fr)); gap: 1rem; }
.detail-card { background: rgba(0,0,0,0.2); border: 1px solid var(--border-light); border-radius: 0.5rem; padding: 1rem; }
.detail-bus-id { font-size: 1rem; font-weight: 700; color: var(--accent-blue); margin-bottom: 0.75rem; }
.detail-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.4rem; }
.detail-label { font-size: 0.8rem; color: var(--text-muted); }
.detail-val { font-weight: 600; font-size: 0.95rem; }
.paragens-note { margin-top: 1rem; font-size: 0.8rem; color: var(--text-muted); font-style: italic; border-left: 3px solid var(--border-light); padding-left: 0.75rem; }
.no-data-day { text-align: center; color: var(--text-muted); padding: 1.5rem; font-style: italic; }
</style>
