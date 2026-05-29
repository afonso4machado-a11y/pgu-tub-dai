<script setup>
import { ref, computed } from 'vue'
import { Search, Bus, Info, AlertTriangle, Activity, ShieldCheck, Trash2, RotateCcw } from 'lucide-vue-next'

import { apiFetch } from '../services/api.js'
const apiUrl = '/api'
const autoId = ref('')
const autoCap = ref('')
const autoMatricula = ref('')
const autoMarca = ref('')
const autoModelo = ref('')
const autoLinha = ref('')
const consId = ref('')
const consRes = ref(null)

// Leituras Manuais
const leitId = ref('')
const leitIn = ref('')
const leitOut = ref('')
const autoAlerts = ref([])

// Eliminação
const deleteId = ref('')
const deletedBuses = ref([])
const showConfirmDelete = ref(false)
const deleteTargetId = ref('')
const deleteFeedback = ref(null)

// Estado de validação
const formTouched = ref(false)
const formErrors = ref({})
const submitting = ref(false)

// ─── Regras de Validação Estritas (Frontend Zero-Trust) ───

/** Padrão matrícula portuguesa: XX-XX-XX onde X pode ser letra ou dígito */
const MATRICULA_REGEX = /^[A-Z0-9]{2}-[A-Z0-9]{2}-[A-Z0-9]{2}$/

/** Identificador: TUB-NNN ou formato alfanumérico (3-20 chars) */
const ID_REGEX = /^[A-Za-z0-9\-]{3,20}$/

/** Marca/Modelo: apenas letras, espaços, apóstrofes, hífenes (2-30 chars) */
const NOME_REGEX = /^[A-Za-zÀ-ú\s'\-]{2,30}$/

/** Linha: L seguido de 1-3 dígitos */
const LINHA_REGEX = /^L\d{1,3}$/

function validateForm() {
 const errors = {}

 // ID — obrigatório, formato válido
 if (!autoId.value.trim()) {
 errors.id = 'O identificador é obrigatório.'
 } else if (!ID_REGEX.test(autoId.value.trim())) {
 errors.id = 'Formato inválido. Use 3-20 caracteres alfanuméricos (ex: TUB-101).'
 }

 // Capacidade — obrigatório, inteiro > 0 e <= 200
 const cap = parseInt(autoCap.value)
 if (!autoCap.value && autoCap.value !== 0) {
 errors.capacidade = 'A lotação é obrigatória.'
 } else if (isNaN(cap) || !Number.isInteger(cap)) {
 errors.capacidade = 'A lotação deve ser um número inteiro.'
 } else if (cap <= 0) {
 errors.capacidade = 'A lotação deve ser superior a 0.'
 } else if (cap > 200) {
 errors.capacidade = 'A lotação máxima permitida é 200.'
 }

 // Matrícula — obrigatória, formato português
 if (!autoMatricula.value.trim()) {
 errors.matricula = 'A matrícula é obrigatória.'
 } else if (!MATRICULA_REGEX.test(autoMatricula.value.trim().toUpperCase())) {
 errors.matricula = 'Formato inválido. Use XX-XX-XX (ex: 23-AB-45).'
 }

 // Marca — obrigatória
 if (!autoMarca.value.trim()) {
 errors.marca = 'A marca é obrigatória.'
 } else if (!NOME_REGEX.test(autoMarca.value.trim())) {
 errors.marca = 'A marca contém caracteres inválidos (2-30 letras).'
 }

 // Modelo — obrigatório
 if (!autoModelo.value.trim()) {
 errors.modelo = 'O modelo é obrigatório.'
 } else if (!NOME_REGEX.test(autoModelo.value.trim())) {
 errors.modelo = 'O modelo contém caracteres inválidos (2-30 letras).'
 }

 // Linha — opcional, mas se preenchido, deve respeitar formato
 if (autoLinha.value.trim() && !LINHA_REGEX.test(autoLinha.value.trim())) {
 errors.linha = 'Formato inválido. Use Lx ou Lxx (ex: L7, L43).'
 }

 formErrors.value = errors
 return Object.keys(errors).length === 0
}

/** Computed: formulário é válido? (para desativar botão) */
const isFormValid = computed(() => {
 if (!formTouched.value) return false
 // Validação rápida sem side-effects
 const id = autoId.value.trim()
 const cap = parseInt(autoCap.value)
 const mat = autoMatricula.value.trim().toUpperCase()
 const marca = autoMarca.value.trim()
 const modelo = autoModelo.value.trim()
 const linha = autoLinha.value.trim()

 if (!id || !ID_REGEX.test(id)) return false
 if (isNaN(cap) || !Number.isInteger(cap) || cap <= 0 || cap > 200) return false
 if (!mat || !MATRICULA_REGEX.test(mat)) return false
 if (!marca || !NOME_REGEX.test(marca)) return false
 if (!modelo || !NOME_REGEX.test(modelo)) return false
 if (linha && !LINHA_REGEX.test(linha)) return false

 return true
})

function markTouched() {
 formTouched.value = true
}

async function handleRegistarAutocarro() {
 formTouched.value = true
 if (!validateForm()) return

 submitting.value = true
 try {
 const req = await fetch(apiUrl + '/autocarros', {
 method: 'POST', headers: {'Content-Type': 'application/json'},
 body: JSON.stringify({ 
 id: autoId.value.trim(), 
 capacidade: parseInt(autoCap.value),
 matricula: autoMatricula.value.trim().toUpperCase(),
 marca: autoMarca.value.trim(),
 modelo: autoModelo.value.trim()
 })
 })
 const res = await req.json()
 
 if (res.status === 'sucesso') {
 let finalMsg = res.mensagem
 if (autoLinha.value.trim()) {
 try {
 const reqLinha = await fetch(`${apiUrl}/linhas/${autoLinha.value.trim()}/autocarros`, {
 method: 'POST', headers: {'Content-Type': 'application/json'},
 body: JSON.stringify({ autocarroId: autoId.value.trim() })
 })
 const resLinha = await reqLinha.json()
 if (resLinha.status === 'sucesso') {
 finalMsg += ` E associado à linha ${autoLinha.value} com sucesso!`
 } else {
 finalMsg += ` Porém, falhou a associar à linha: ${resLinha.mensagem}`
 }
 } catch(e) {
 finalMsg += ` Porém, falhou a comunicação para associar à linha.`
 }
 }
 alert(finalMsg)
 } else {
 alert(res.mensagem)
 }
 
 // Limpar formulário
 autoId.value = ''; autoCap.value = ''; autoLinha.value = '';
 autoMatricula.value = ''; autoMarca.value = ''; autoModelo.value = '';
 formTouched.value = false
 formErrors.value = {}
 } catch(e) {
 alert("Falha de Comunicação com servidor Java.")
 } finally {
 submitting.value = false
 }
}

async function handleConsulta() {
 try {
 const { data: res } = await apiFetch(`/autocarros/${consId.value}`)
 if(res.status === 'sucesso') {
 consRes.value = res
 } else alert(res.mensagem)
 } catch(e) {
 alert("Servidor inativo. Não foi possível consultar o veículo.")
 }
}

async function handleRegLeitura() {
 try {
 const req = await fetch(`${apiUrl}/leituras`, {
 method: 'POST', headers: {'Content-Type': 'application/json'},
 body: JSON.stringify({id: leitId.value, entradas: leitIn.value, saidas: leitOut.value})
 })
 const res = await req.json()
 if(res.status === 'sucesso') {
 alert("Leitura simulada/gravada com sucesso!")
 autoAlerts.value = res.alertas || []
 leitIn.value = ''; leitOut.value = '';
 } else alert(res.mensagem)
 } catch(e) {
 alert("Erro ao reportar leitura ao servidor.")
 }
}

function confirmDelete(id) {
 deleteTargetId.value = id
 showConfirmDelete.value = true
}

async function handleEliminarAutocarro() {
 showConfirmDelete.value = false
 const id = deleteTargetId.value || deleteId.value.trim()
 if (!id) return
 try {
 const req = await fetch(`${apiUrl}/autocarros/${encodeURIComponent(id)}`, { method: 'DELETE' })
 const res = await req.json()
 deleteFeedback.value = { ok: res.status === 'sucesso', msg: res.mensagem }
 if (res.status === 'sucesso') {
 deleteId.value = ''
 await loadEliminados()
 }
 } catch(e) {
 deleteFeedback.value = { ok: false, msg: 'Erro de comunicação com o servidor.' }
 }
}

async function handleRestaurarAutocarro(id) {
 try {
 const req = await fetch(`${apiUrl}/autocarros/${encodeURIComponent(id)}/restaurar`, { method: 'POST' })
 const res = await req.json()
 if (res.status === 'sucesso') {
 await loadEliminados()
 deleteFeedback.value = { ok: true, msg: `Autocarro ${id} restaurado com sucesso.` }
 } else {
 deleteFeedback.value = { ok: false, msg: res.mensagem }
 }
 } catch(e) {
 deleteFeedback.value = { ok: false, msg: 'Erro de comunicação com o servidor.' }
 }
}

async function loadEliminados() {
 try {
 const { data } = await apiFetch('/autocarros/eliminados')
 if (data.status === 'sucesso') deletedBuses.value = data.autocarros || []
 } catch(e) { /* silently ignore */ }
}
</script>

<template>
 <div class="fleet-view fade-in">
 
 <div class="dual-grid">
 <!-- Registar -->
 <div class="glass-panel">
 <h3 class="panel-title"><Bus class="icon-inline"/> Nova Viatura na Frota</h3>
 <p class="panel-desc">Atribuição de matrícula interna e definição de lotação</p>
 
 <form @submit.prevent="handleRegistarAutocarro" class="form-stack" novalidate>
 <div class="input-group" :class="{'has-error': formTouched && formErrors.id}">
 <label>Identificador Único <span class="required">*</span></label>
 <input v-model="autoId" @input="markTouched" class="input-field fira-code" placeholder="Ex: TUB-101" />
 <span v-if="formTouched && formErrors.id" class="field-error">{{ formErrors.id }}</span>
 </div>
 <div style="display: flex; gap: 1rem;">
 <div class="input-group" style="flex: 1;" :class="{'has-error': formTouched && formErrors.capacidade}">
 <label>Capacidade Total <span class="required">*</span></label>
 <input type="number" v-model="autoCap" @input="markTouched" class="input-field" placeholder="Lotação (1-200)" min="1" max="200" step="1" />
 <span v-if="formTouched && formErrors.capacidade" class="field-error">{{ formErrors.capacidade }}</span>
 </div>
 <div class="input-group" style="flex: 1;" :class="{'has-error': formTouched && formErrors.matricula}">
 <label>Matrícula <span class="required">*</span></label>
 <input v-model="autoMatricula" @input="markTouched" class="input-field fira-code" placeholder="XX-XX-XX" maxlength="8" />
 <span v-if="formTouched && formErrors.matricula" class="field-error">{{ formErrors.matricula }}</span>
 </div>
 </div>
 <div style="display: flex; gap: 1rem;">
 <div class="input-group" style="flex: 1;" :class="{'has-error': formTouched && formErrors.marca}">
 <label>Marca <span class="required">*</span></label>
 <input v-model="autoMarca" @input="markTouched" class="input-field" placeholder="Ex: Mercedes" />
 <span v-if="formTouched && formErrors.marca" class="field-error">{{ formErrors.marca }}</span>
 </div>
 <div class="input-group" style="flex: 1;" :class="{'has-error': formTouched && formErrors.modelo}">
 <label>Modelo <span class="required">*</span></label>
 <input v-model="autoModelo" @input="markTouched" class="input-field" placeholder="Ex: Citaro" />
 <span v-if="formTouched && formErrors.modelo" class="field-error">{{ formErrors.modelo }}</span>
 </div>
 </div>
 <div class="input-group" :class="{'has-error': formTouched && formErrors.linha}">
 <label>Linha de Serviço (Opcional)</label>
 <input v-model="autoLinha" @input="markTouched" class="input-field fira-code" placeholder="Ex: L1, L43" />
 <span v-if="formTouched && formErrors.linha" class="field-error">{{ formErrors.linha }}</span>
 </div>

 <!-- Indicador de validação -->
 <div class="validation-status" :class="{'valid': formTouched && isFormValid, 'invalid': formTouched && !isFormValid}">
 <ShieldCheck :size="16" />
 <span v-if="!formTouched">Preencha todos os campos obrigatórios.</span>
 <span v-else-if="isFormValid">Formulário válido — pronto para submissão.</span>
 <span v-else>Corrija os erros assinalados acima.</span>
 </div>

 <button type="submit" class="btn btn-primary mt-4" :disabled="!isFormValid || submitting">
 {{ submitting ? 'A registar...' : 'Registar Viatura' }}
 </button>
 </form>
 </div>
 
 <!-- Simulação Manual de Sensores (Registo Leituras) -->
 <div class="glass-panel">
 <h3 class="panel-title"><Activity class="icon-inline"/> Registo Manual de Tráfego</h3>
 <p class="panel-desc">Injeção mecânica das métricas para simular sensores da porta</p>
 
 <form @submit.prevent="handleRegLeitura" class="form-stack">
 <div class="input-group">
 <label>ID da Viatura</label>
 <input v-model="leitId" class="input-field fira-code" placeholder="A qual viatura aplicar" required />
 </div>
 
 <div style="display: flex; gap: 1rem;">
 <div class="input-group" style="flex: 1;">
 <label>Entradas</label>
 <input type="number" v-model="leitIn" class="input-field" required />
 </div>
 <div class="input-group" style="flex: 1;">
 <label>Saídas</label>
 <input type="number" v-model="leitOut" class="input-field" required />
 </div>
 </div>
 <button type="submit" class="btn btn-secondary mt-4">Disparar Carga para DB</button>
 </form>

 <!-- Manifestação de Alertas da Injeção -->
 <div v-if="autoAlerts.length" class="alert-manifest fade-in mt-4">
 <div class="alert-header">
 <AlertTriangle :size="20" class="text-danger"/>
 <span style="font-weight: 600; color: var(--danger)">Limiares Excedidos</span>
 </div>
 <ul class="alert-list">
 <li v-for="(a, index) in autoAlerts" :key="index" class="alert-item">
 <span class="alert-msg">{{ a.mensagem }}</span>
 </li>
 </ul>
 </div>
 </div>
 
 <!-- Consultar -->
 <div class="glass-panel">
 <h3 class="panel-title"><Search class="icon-inline"/> Consultar Viatura</h3>
 <p class="panel-desc">Telemetria direta da viatura no Data Lake</p>
 
 <form @submit.prevent="handleConsulta" class="search-form">
 <input v-model="consId" class="search-input fira-code" placeholder="ID (ex: TUB-101)" required />
 <button class="btn btn-secondary"><Search :size="18"/></button>
 </form>

 <div v-if="consRes" class="metric-results scale-in">
 <div class="res-item">
 <span class="res-label">ID / Matrícula</span>
 <span class="res-val fira-code text-cyan">{{ consRes.id }} <span class="dim">[{{ consRes.matricula }}]</span></span>
 </div>
 <div class="res-item">
 <span class="res-label">Marca / Modelo</span>
 <span class="res-val">{{ consRes.marca }} <span class="dim">{{ consRes.modelo }}</span></span>
 </div>
 <div class="res-item">
 <span class="res-label">Lotação Atual</span>
 <span class="res-val">{{ consRes.passageirosAtuais }} <span class="dim">/ {{ consRes.capacidadeMaxima }}</span></span>
 </div>
 <div class="res-item">
 <span class="res-label">Ocupação</span>
 <span class="res-val" :class="{'text-danger': consRes.taxaOcupacao > 80}">{{ consRes.taxaOcupacao }}%</span>
 </div>
 <div class="res-item warning-box" v-if="consRes.numeroAlertas > 0">
 <AlertTriangle class="text-warning"/>
 <span>{{ consRes.numeroAlertas }} Infrações (Alertas)</span>
 </div>
 </div>
 <div v-else class="empty-state">
 <Info :size="24" class="dim" />
 <p>Introduza uma matrícula para obter o seu estado atual.</p>
 </div>
 </div>
 
 <!-- Eliminação de Viatura (Soft Delete) -->
 <div class="glass-panel" style="grid-column: 1 / -1;">
 <h3 class="panel-title"><Trash2 class="icon-inline" style="color: #ef4444;"/> Eliminar / Restaurar Viatura</h3>
 <p class="panel-desc">O autocarro fica marcado como eliminado — os dados históricos são preservados. A ação pode ser revertida.</p>

 <div style="display: flex; gap: 1rem; align-items: flex-end; flex-wrap: wrap;">
 <div class="input-group" style="flex: 1; min-width: 200px;">
 <label>ID do Autocarro a Eliminar</label>
 <input v-model="deleteId" class="input-field fira-code" placeholder="Ex: TUB-101" />
 </div>
 <button class="btn btn-danger" @click="deleteId.trim() && confirmDelete(deleteId.trim())" :disabled="!deleteId.trim()">
 <Trash2 :size="16" /> Eliminar
 </button>
 <button class="btn btn-outline" @click="loadEliminados">
 <RotateCcw :size="16" /> Atualizar Lista
 </button>
 </div>

 <div v-if="deleteFeedback" class="delete-feedback" :class="{ 'feedback-ok': deleteFeedback.ok, 'feedback-err': !deleteFeedback.ok }">
 {{ deleteFeedback.msg }}
 </div>

 <!-- Lista de eliminados -->
 <div v-if="deletedBuses.length" class="deleted-list mt-4">
 <h4 style="font-size: 0.85rem; font-weight: 700; color: var(--text-muted); margin-bottom: 0.75rem;">Autocarros Eliminados (Reversível)</h4>
 <div v-for="bus in deletedBuses" :key="bus.id" class="deleted-item">
 <span class="deleted-badge">ELIMINADO</span>
 <span class="fira-code" style="font-weight: 700;">{{ bus.id }}</span>
 <span class="dim">{{ bus.matricula }} — {{ bus.marca }} {{ bus.modelo }}</span>
 <button class="btn-restore" @click="handleRestaurarAutocarro(bus.id)">
 <RotateCcw :size="14" /> Restaurar
 </button>
 </div>
 </div>
 <div v-else class="empty-state" style="margin-top: 1rem;">
 <RotateCcw :size="20" class="dim" />
 <p>Sem viaturas eliminadas. Clique em "Atualizar Lista" para verificar.</p>
 </div>
 </div>

 </div>

 <!-- Diálogo de Confirmação de Eliminação -->
 <div v-if="showConfirmDelete" class="confirm-overlay" @click.self="showConfirmDelete = false">
 <div class="confirm-dialog" role="dialog" aria-modal="true" aria-labelledby="confirm-title">
 <h3 id="confirm-title"><Trash2 :size="20" style="color: #ef4444;"/> Confirmar Eliminação</h3>
 <p>Tem a certeza que pretende eliminar o autocarro <strong class="fira-code">{{ deleteTargetId }}</strong>?</p>
 <p style="font-size: 0.8rem; color: var(--text-muted); margin-top: 0.5rem;">Esta ação é reversível — poderá restaurar o autocarro a qualquer momento.</p>
 <div style="display: flex; gap: 0.75rem; margin-top: 1.25rem; justify-content: flex-end;">
 <button class="btn btn-outline" @click="showConfirmDelete = false">Cancelar</button>
 <button class="btn btn-danger" @click="handleEliminarAutocarro"><Trash2 :size="16" /> Confirmar Eliminação</button>
 </div>
 </div>
 </div>
 </div>
</template>

<style scoped>
.dual-grid {
 display: grid;
 grid-template-columns: repeat(auto-fit, minmax(380px, 1fr));
 gap: 2rem;
}

.panel-title { display: flex; align-items: center; gap: 0.5rem; margin-bottom: 0.5rem; }
.icon-inline { color: var(--accent-blue); }
.panel-desc { color: var(--text-muted); font-size: 0.85rem; margin-bottom: 1.5rem; }
.form-stack { display: flex; flex-direction: column; gap: 1rem; }
.mt-4 { margin-top: 1rem; }

/* Estilos de validação */
.required { color: var(--danger); font-weight: 700; }

.has-error .input-field {
 border-color: var(--danger) !important;
 box-shadow: 0 0 0 1px rgba(239, 68, 68, 0.3);
}

.field-error {
 color: var(--danger);
 font-size: 0.75rem;
 font-weight: 500;
 margin-top: 0.25rem;
 display: flex;
 align-items: center;
 gap: 0.25rem;
}

.validation-status {
 display: flex;
 align-items: center;
 gap: 0.5rem;
 padding: 0.6rem 1rem;
 border-radius: 0.5rem;
 font-size: 0.8rem;
 font-weight: 500;
 border: 1px solid var(--border-light);
 background: rgba(0,0,0,0.15);
 color: var(--text-muted);
 transition: all 0.3s ease;
}

.validation-status.valid {
 background: rgba(20, 184, 166, 0.1);
 border-color: rgba(20, 184, 166, 0.3);
 color: var(--accent-teal);
}

.validation-status.invalid {
 background: rgba(239, 68, 68, 0.08);
 border-color: rgba(239, 68, 68, 0.2);
 color: var(--danger);
}

button:disabled {
 opacity: 0.5;
 cursor: not-allowed;
 pointer-events: none;
}

.search-form { display: flex; gap: 0.5rem; margin-bottom: 1.5rem; }
.search-input { 
 flex: 1; 
 background: var(--bg-primary); 
 border: 1px solid var(--border-light); 
 padding: 0.75rem 1rem; 
 border-radius: 0.5rem;
 color: var(--text-main);
}
.search-input:focus { border-color: var(--accent-blue); outline: none; }

.metric-results {
 background: rgba(0,0,0,0.2);
 border: 1px solid var(--border-light);
 border-radius: 0.5rem;
 padding: 1rem;
 display: flex;
 flex-direction: column;
 gap: 1rem;
}
.res-item { display: flex; justify-content: space-between; align-items: center; }
.res-label { font-size: 0.85rem; color: var(--text-muted); text-transform: uppercase; font-weight: 500; }
.res-val { font-size: 1.25rem; font-weight: 600; }
.text-cyan { color: var(--accent-blue); }
.text-danger { color: var(--danger); }
.text-warning { color: var(--warning); }
.dim { color: var(--text-muted); font-size: 0.9em; }

.warning-box {
 background: rgba(234, 179, 8, 0.1);
 padding: 0.75rem;
 border-radius: 0.5rem;
 justify-content: flex-start;
 gap: 0.5rem;
 color: var(--warning);
 font-weight: 500;
 border: 1px solid rgba(234, 179, 8, 0.2);
}

.alert-manifest {
 background: rgba(239, 68, 68, 0.05);
 border: 1px solid rgba(239, 68, 68, 0.2);
 border-radius: 0.5rem;
 padding: 1rem;
}
.alert-header { display: flex; align-items: center; gap: 0.5rem; margin-bottom: 0.5rem; }
.alert-list { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 0.5rem; }
.alert-item { background: rgba(0,0,0,0.2); padding: 0.75rem; border-radius: 0.25rem; font-size: 0.9rem; border-left: 3px solid var(--danger); box-shadow: inset 0 0 10px rgba(0,0,0,0.1); }

.empty-state {
 display: flex;
 flex-direction: column;
 align-items: center;
 gap: 1rem;
 padding: 2rem;
 text-align: center;
 color: var(--text-muted);
 background: rgba(0,0,0,0.2);
 border-radius: 0.5rem;
 border: 1px dashed var(--border-light);
}

/* Soft Delete UI */
.btn-danger {
 background: #ef4444;
 color: #fff;
 border: none;
 padding: 0.65rem 1.25rem;
 border-radius: 0.5rem;
 font-weight: 700;
 cursor: pointer;
 display: flex;
 align-items: center;
 gap: 0.4rem;
 transition: background 0.2s;
}
.btn-danger:hover:not(:disabled) { background: #dc2626; }
.btn-danger:disabled { opacity: 0.4; cursor: not-allowed; }
.btn-outline {
 background: transparent;
 color: var(--text-muted);
 border: 1px solid var(--border-light);
 padding: 0.65rem 1.25rem;
 border-radius: 0.5rem;
 font-weight: 600;
 cursor: pointer;
 display: flex;
 align-items: center;
 gap: 0.4rem;
 transition: all 0.2s;
}
.btn-outline:hover { border-color: var(--accent-blue); color: var(--accent-blue); }
.delete-feedback {
 margin-top: 0.75rem;
 padding: 0.65rem 1rem;
 border-radius: 0.5rem;
 font-size: 0.85rem;
 font-weight: 600;
}
.feedback-ok { background: rgba(16,185,129,0.12); color: #10b981; border: 1px solid rgba(16,185,129,0.3); }
.feedback-err { background: rgba(239,68,68,0.12); color: #ef4444; border: 1px solid rgba(239,68,68,0.3); }
.deleted-list { display: flex; flex-direction: column; gap: 0.5rem; }
.deleted-item {
 display: flex;
 align-items: center;
 gap: 0.75rem;
 padding: 0.65rem 1rem;
 background: rgba(239,68,68,0.05);
 border: 1px solid rgba(239,68,68,0.2);
 border-radius: 0.5rem;
 flex-wrap: wrap;
}
.deleted-badge {
 font-size: 0.65rem;
 font-weight: 800;
 background: #ef4444;
 color: #fff;
 padding: 0.15rem 0.5rem;
 border-radius: 0.25rem;
 letter-spacing: 0.05em;
}
.btn-restore {
 margin-left: auto;
 display: flex;
 align-items: center;
 gap: 0.35rem;
 padding: 0.4rem 0.85rem;
 background: rgba(16,185,129,0.1);
 color: #10b981;
 border: 1px solid rgba(16,185,129,0.3);
 border-radius: 0.4rem;
 cursor: pointer;
 font-size: 0.8rem;
 font-weight: 700;
 transition: all 0.2s;
}
.btn-restore:hover { background: rgba(16,185,129,0.2); }

/* Confirmation Modal */
.confirm-overlay {
 position: fixed;
 inset: 0;
 background: rgba(0,0,0,0.5);
 z-index: 9999;
 display: flex;
 align-items: center;
 justify-content: center;
 backdrop-filter: blur(4px);
}
.confirm-dialog {
 background: var(--bg-surface);
 border: 1px solid var(--border-light);
 border-radius: 1rem;
 padding: 2rem;
 max-width: 420px;
 width: 90%;
 box-shadow: 0 20px 60px rgba(0,0,0,0.3);
}
.confirm-dialog h3 {
 display: flex;
 align-items: center;
 gap: 0.5rem;
 margin-bottom: 1rem;
 font-size: 1.1rem;
}
</style>
