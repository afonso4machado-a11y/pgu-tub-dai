<script setup>
import { ref } from 'vue'
import { Search, Bus, Info, AlertTriangle, Activity } from 'lucide-vue-next'

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

async function handleRegistarAutocarro() {
  try {
    const req = await fetch(apiUrl + '/autocarros', {
      method: 'POST', headers: {'Content-Type': 'application/json'},
      body: JSON.stringify({ 
        id: autoId.value, 
        capacidade: autoCap.value,
        matricula: autoMatricula.value,
        marca: autoMarca.value,
        modelo: autoModelo.value
      })
    })
    const res = await req.json()
    
    if (res.status === 'sucesso') {
      let finalMsg = res.mensagem
      if (autoLinha.value) {
        try {
          const reqLinha = await fetch(`${apiUrl}/linhas/${autoLinha.value}/autocarros`, {
            method: 'POST', headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({ autocarroId: autoId.value })
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
    
    autoId.value = ''; autoCap.value = ''; autoLinha.value = '';
    autoMatricula.value = ''; autoMarca.value = ''; autoModelo.value = '';
  } catch(e) {
    alert("Falha de Comunicação com servidor Java.")
  }
}

async function handleConsulta() {
  try {
    const req = await fetch(`${apiUrl}/autocarros/${consId.value}`)
    const res = await req.json()
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
</script>

<template>
  <div class="fleet-view fade-in">
    
    <div class="dual-grid">
      <!-- Registar -->
      <div class="glass-panel">
        <h3 class="panel-title"><Bus class="icon-inline"/> Nova Viatura na Frota</h3>
        <p class="panel-desc">Atribuição de matrícula interna e definição de lotação</p>
        
        <form @submit.prevent="handleRegistarAutocarro" class="form-stack">
          <div class="input-group">
            <label>Identificador Único</label>
            <input v-model="autoId" class="input-field fira-code" placeholder="Ex: TUB-101" required />
          </div>
          <div style="display: flex; gap: 1rem;">
            <div class="input-group" style="flex: 1;">
              <label>Capacidade Total</label>
              <input type="number" v-model="autoCap" class="input-field" placeholder="Lotação" required />
            </div>
            <div class="input-group" style="flex: 1;">
              <label>Matrícula</label>
              <input v-model="autoMatricula" class="input-field fira-code" placeholder="00-XX-00" />
            </div>
          </div>
          <div style="display: flex; gap: 1rem;">
            <div class="input-group" style="flex: 1;">
              <label>Marca</label>
              <input v-model="autoMarca" class="input-field" placeholder="Ex: Mercedes" />
            </div>
            <div class="input-group" style="flex: 1;">
              <label>Modelo</label>
              <input v-model="autoModelo" class="input-field" placeholder="Ex: Citaro" />
            </div>
          </div>
          <div class="input-group">
            <label>Linha de Serviço (Opcional)</label>
            <input v-model="autoLinha" class="input-field fira-code" placeholder="Ex: L1" />
          </div>
          <button type="submit" class="btn btn-primary mt-4">Registar Viatura</button>
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
</style>
