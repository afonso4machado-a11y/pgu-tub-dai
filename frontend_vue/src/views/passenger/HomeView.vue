<script setup>
import { ref, onMounted, computed } from 'vue'
import { MapPin, Navigation, Clock, Bus, ChevronRight, Zap, Star, TrendingUp, FlaskConical, Database } from 'lucide-vue-next'
import { useRouter } from 'vue-router'
import { authService } from '../../services/auth'

const apiUrl = '/api'
const router = useRouter()
const currentUser = ref(authService.getUser())
const greeting = ref('')
const busCount = ref(0)
const avgOcc = ref(0)

// Configuração para Planeamento de Viagem
const origin = ref('')
const destination = ref('')
const showSuggestions = ref({ origin: false, destination: false })
const planResult = ref(null)

const paragensBraga = ref([])

const normalize = (str) => str.normalize("NFD").replace(/[\u0300-\u036f]/g, "").toLowerCase();

function getFilteredSuggestions(query) {
  if (!query || query.length < 1) return []
  const q = normalize(query)
  
  return paragensBraga.value
    .map(name => {
      const n = normalize(name)
      const words = n.split(' ')
      let score = 0
      
      // 1. Prioridade Máxima: Começa exatamente com a query (Letra a Letra)
      if (n.startsWith(q)) {
        score += 10000 + (q.length * 100)
      }
      
      // 2. Prioridade Alta: Alguma palavra começa com a query
      else if (words.some(w => w.startsWith(q))) {
        score += 5000 + (q.length * 50)
      }
      
      // 3. Prioridade Média: Contém a sequência em qualquer lugar
      else if (n.includes(q)) {
        score += 2000 - n.indexOf(q)
      }
      
      // 4. Fuzzy: Letras aparecem na mesma ordem (Sequência Subsequente)
      else {
        let qIdx = 0
        let lastIdx = -1
        for (let char of q) {
          const foundIdx = n.indexOf(char, lastIdx + 1)
          if (foundIdx !== -1) {
            qIdx++
            lastIdx = foundIdx
          }
        }
        if (qIdx === q.length) {
          score += 1000 - (lastIdx - n.indexOf(q[0]))
        }
      }

      return { name, score }
    })
    .filter(item => item.score > 0)
    .sort((a, b) => b.score - a.score || a.name.localeCompare(b.name))
    .map(item => item.name)
    .slice(0, 6)
}

const suggestions = computed(() => ({
  origin: getFilteredSuggestions(origin.value),
  destination: getFilteredSuggestions(destination.value)
}))

function selectSuggestion(type, value) {
  if (type === 'origin') origin.value = value
  if (type === 'destination') destination.value = value
  showSuggestions.value[type] = false
}

function handlePlan() {
  if (!origin.value || !destination.value) return
  planResult.value = {
    linha: 'L43',
    tempo: '12 min',
    partida: '5 min',
    ocu: 65
  }
}

const linhasFavoritas = ref([
  { id: 'L7', nome: 'Celeirós — S. Vítor', cor: '#0284c7' },
  { id: 'L43', nome: 'Estação — Universidade', cor: '#7c3aed' },
])

const proximosAutocarros = ref([
  { linha: 'L7', destino: 'S. Vítor', minutos: 3, lotacao: 45 },
  { linha: 'L43', destino: 'Universidade', minutos: 8, lotacao: 72 },
  { linha: 'L7', destino: 'Celeirós', minutos: 14, lotacao: 28 },
])

const isDemo = ref(false)

onMounted(async () => {
  isDemo.value = localStorage.getItem('pgu_demo_mode') === 'true'
  
  const hour = new Date().getHours()
  if (hour < 12) greeting.value = 'Bom dia'
  else if (hour < 19) greeting.value = 'Boa tarde'
  else greeting.value = 'Boa noite'

  try {
    const res = await fetch(`${apiUrl}/dashboard`)
    const data = await res.json()
    if (data.status === 'sucesso') {
      busCount.value = data.dashboard?.totalAutocarros || 0
      avgOcc.value = Math.round(data.dashboard?.taxaOcupacaoMedia || 0)
    }

    // Fetch Paragens reais
    const pRes = await fetch(`${apiUrl}/paragens`)
    const pData = await pRes.json()
    if (pData.status === 'sucesso' && pData.paragens.length > 0) {
      paragensBraga.value = pData.paragens
    } else {
      // Fallback: paragens reais das linhas TUB (07H, 40H, 43H)
      paragensBraga.value = [
        // Linha 07H
        "S. Mamede d' Este", "Avenida da Liberdade", "Igreja S Lázaro", "Celeirós",
        "Rua 25 de Abril", "Parque Infantil",
        // Linha 40H
        "Hospital", "Rua Egídio Guimarães", "Avenida Central", "Rua Mário de Almeida",
        // Linha 43H
        "Estação C.P.", "U.Minho", "Universidade do Minho",
        // Referências urbanas comuns
        "Terminal Intermodal", "São Vítor", "Maximinos", "Bom Jesus",
        "Nogueiró", "Gualtar", "Braga Parque", "Estádio Municipal",
      ]
    }
  } catch(e) { /* offline mode */ }
})

function toggleDemo() {
  isDemo.value = !isDemo.value
  localStorage.setItem('pgu_demo_mode', isDemo.value)
  window.location.reload()
}

function lotColor(pct) {
  if (pct > 80) return '#ef4444'
  if (pct > 60) return '#eab308'
  return '#10b981'
}

function lotLabel(pct) {
  if (pct > 80) return 'Lotado'
  if (pct > 60) return 'Moderado'
  return 'Livre'
}
</script>

<template>
  <div class="home-page">
    <!-- Demo Banner -->
    <div v-if="isDemo" class="demo-banner">
      <Zap :size="14" /> MODO SIMULAÇÃO ATIVO
    </div>

    <!-- Greeting -->
    <div class="greeting-section">
      <h2 class="greeting">{{ greeting }}, {{ currentUser?.nome || 'Utilizador' }}</h2>
      <p class="greeting-sub">Pronto para a tua viagem?</p>
    </div>

    <!-- Floating Demo Toggle (discreet) -->
    <button class="fab-demo" @click="toggleDemo" :class="{ active: isDemo }">
      <FlaskConical v-if="isDemo" :size="20" />
      <Database v-else :size="20" />
    </button>

    <!-- Quick Actions -->
    <div class="quick-actions">
      <router-link to="/app/map" class="qa-card qa-map">
        <MapPin :size="28" />
        <span>Ver Mapa</span>
      </router-link>
      <router-link to="/app/ticket" class="qa-card qa-ticket">
        <Zap :size="28" />
        <span>Meu Bilhete</span>
      </router-link>
    </div>

    <!-- Search / Trip Planner -->
    <div class="trip-planner">
      <h3 class="section-title"><Navigation :size="18" /> Para onde vais?</h3>
      <div class="planner-inputs">
        <div class="planner-input-wrapper">
          <div class="planner-input">
            <div class="input-dot origin"></div>
            <input 
              type="text" 
              v-model="origin" 
              placeholder="A minha localização" 
              class="planner-field"
              @focus="showSuggestions.origin = true"
              @blur="setTimeout(() => showSuggestions.origin = false, 200)"
            />
          </div>
          <div v-if="showSuggestions.origin && suggestions.origin.length" class="suggestions-dropdown">
            <div 
              v-for="s in suggestions.origin" 
              :key="s" 
              class="suggestion-item"
              @mousedown="selectSuggestion('origin', s)"
            >
              {{ s }}
            </div>
          </div>
        </div>

        <div class="planner-divider"></div>

        <div class="planner-input-wrapper">
          <div class="planner-input">
            <div class="input-dot destination"></div>
            <input 
              type="text" 
              v-model="destination" 
              placeholder="Destino (ex: Universidade)" 
              class="planner-field"
              @focus="showSuggestions.destination = true"
              @blur="setTimeout(() => showSuggestions.destination = false, 200)"
            />
          </div>
          <div v-if="showSuggestions.destination && suggestions.destination.length" class="suggestions-dropdown">
            <div 
              v-for="s in suggestions.destination" 
              :key="s" 
              class="suggestion-item"
              @mousedown="selectSuggestion('destination', s)"
            >
              {{ s }}
            </div>
          </div>
        </div>
      </div>
      <button class="btn-plan" @click="handlePlan">Planear Viagem</button>

      <!-- Resultado do Planeamento -->
      <div v-if="planResult" class="holiday-plan-result fade-in">
        <div class="result-header">
          <span class="res-linha">{{ planResult.linha }}</span>
          <span class="res-msg">Melhor opção encontrada</span>
        </div>
        <div class="result-body">
          <div class="res-item">
            <Clock :size="14" /> <span>Chegada em <strong>{{ planResult.tempo }}</strong></span>
          </div>
          <div class="res-item">
            <Bus :size="14" /> <span>Partida em <strong>{{ planResult.partida }}</strong></span>
          </div>
        </div>
      </div>
    </div>

    <!-- Próximos Autocarros -->
    <div class="section">
      <h3 class="section-title"><Clock :size="18" /> Próximas Partidas</h3>
      <div class="bus-list">
        <div v-for="bus in proximosAutocarros" :key="bus.linha + bus.destino" class="bus-card">
          <div class="bus-left">
            <span class="bus-linha" :style="{background: bus.linha === 'L7' ? '#0284c7' : '#7c3aed'}">
              {{ bus.linha }}
            </span>
            <div class="bus-info">
              <span class="bus-destino">{{ bus.destino }}</span>
              <span class="bus-lotacao-label" :style="{color: lotColor(bus.lotacao)}">
                {{ lotLabel(bus.lotacao) }} · {{ bus.lotacao }}%
              </span>
            </div>
          </div>
          <div class="bus-right">
            <span class="bus-minutos">{{ bus.minutos }}'</span>
            <div class="lot-indicator">
              <div class="lot-bar" :style="{width: bus.lotacao + '%', background: lotColor(bus.lotacao)}"></div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Linhas Favoritas -->
    <div class="section">
      <h3 class="section-title"><Star :size="18" /> As Tuas Linhas</h3>
      <div class="fav-list">
        <div v-for="l in linhasFavoritas" :key="l.id" class="fav-card">
          <div class="fav-left">
            <span class="fav-badge" :style="{background: l.cor}">{{ l.id }}</span>
            <span class="fav-nome">{{ l.nome }}</span>
          </div>
          <ChevronRight :size="18" class="fav-arrow" />
        </div>
      </div>
    </div>

    <!-- Stats -->
    <div class="section stats-section">
      <div class="mini-stat">
        <Bus :size="20" class="stat-icon" />
        <div>
          <span class="stat-val">{{ busCount }}</span>
          <span class="stat-lbl">Em circulação</span>
        </div>
      </div>
      <div class="mini-stat">
        <TrendingUp :size="20" class="stat-icon" />
        <div>
          <span class="stat-val">{{ avgOcc }}%</span>
          <span class="stat-lbl">Lotação média</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.home-page { padding: 1.25rem; padding-bottom: 2rem; position: relative; }

/* Demo Mode UI */
.demo-banner {
  background: #fef3c7;
  color: #92400e;
  padding: 0.5rem 1rem;
  border-radius: 0.75rem;
  font-size: 0.75rem;
  font-weight: 800;
  margin-bottom: 1rem;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  border: 1px solid #fde68a;
}

.fab-demo {
  position: fixed;
  bottom: 1.5rem;
  right: 1.5rem;
  width: 48px;
  height: 48px;
  border-radius: 50%;
  border: none;
  background: var(--bg-surface);
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  cursor: pointer;
  color: var(--text-muted);
  transition: all 0.3s ease;
}

.fab-demo.active {
  background: #f59e0b;
  color: #fff;
  box-shadow: 0 4px 20px rgba(245, 158, 11, 0.4);
}

/* Greeting */
.greeting-section { margin-bottom: 1.5rem; }
.greeting { font-size: 1.5rem; font-weight: 800; color: var(--text-main); margin: 0; }
.greeting-sub { color: var(--text-muted); font-size: 0.95rem; margin: 0.25rem 0 0; }

/* Quick Actions */
.quick-actions { display: grid; grid-template-columns: 1fr 1fr; gap: 0.75rem; margin-bottom: 1.5rem; }
.qa-card {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  gap: 0.5rem; padding: 1.25rem; border-radius: 1rem; text-decoration: none;
  font-weight: 700; font-size: 0.9rem; transition: transform 0.15s;
  box-shadow: 0 2px 12px rgba(0,0,0,0.08);
}
.qa-card:active { transform: scale(0.97); }
.qa-map { background: linear-gradient(135deg, #dbeafe, #bfdbfe); color: #0369a1; }
.qa-ticket { background: linear-gradient(135deg, #ede9fe, #ddd6fe); color: #6d28d9; }

/* Trip Planner */
.trip-planner {
  background: var(--bg-surface); border-radius: 1rem; padding: 1.25rem;
  box-shadow: 0 2px 16px rgba(0,0,0,0.06); margin-bottom: 1.5rem;
}
.section-title {
  display: flex; align-items: center; gap: 0.5rem;
  font-size: 1rem; font-weight: 700; color: var(--text-main); margin: 0 0 1rem;
}
.planner-inputs { display: flex; flex-direction: column; gap: 0; margin-bottom: 1rem; }
.planner-input { display: flex; align-items: center; gap: 0.75rem; }
.input-dot { width: 12px; height: 12px; border-radius: 50%; flex-shrink: 0; }
.input-dot.origin { background: #10b981; border: 2px solid #a7f3d0; }
.input-dot.destination { background: #ef4444; border: 2px solid #fecaca; }
.planner-field {
  flex: 1; border: none; border-bottom: 1px solid var(--border-light); padding: 0.75rem 0;
  font-size: 0.95rem; color: var(--text-main); background: transparent; outline: none;
}
.planner-field::placeholder { color: var(--text-muted); }
.planner-divider { width: 1px; height: 16px; background: #e2e8f0; margin-left: 5px; }
.btn-plan {
  width: 100%; padding: 0.85rem; border: none; border-radius: 0.75rem;
  background: linear-gradient(135deg, #0284c7, #0369a1); color: #fff;
  font-weight: 700; font-size: 0.95rem; cursor: pointer; transition: opacity 0.2s;
}
.btn-plan:active { opacity: 0.9; }

/* Suggestions & Plan Result */
.planner-input-wrapper { position: relative; width: 100%; }
.suggestions-dropdown {
  position: absolute; top: 100%; left: 0; right: 0;
  background: var(--bg-surface); border-radius: 0.75rem; border: 1px solid var(--border-light);
  box-shadow: 0 10px 25px rgba(0,0,0,0.1);
  z-index: 1000; margin-top: 0.5rem; overflow: hidden;
  max-height: 200px; overflow-y: auto;
}
.suggestion-item {
  padding: 0.75rem 1rem; font-size: 0.9rem; color: #334155;
  cursor: pointer; border-bottom: 1px solid var(--border-light);
}
.suggestion-item:hover { background: var(--bg-primary); color: #0284c7; }

.holiday-plan-result {
  margin-top: 1.25rem; padding: 1rem;
  background: linear-gradient(135deg, #f0f9ff, #e0f2fe);
  border: 1px solid #bae6fd; border-radius: 0.75rem;
}
.result-header { display: flex; align-items: center; gap: 0.75rem; margin-bottom: 0.75rem; }
.res-linha {
  background: #0284c7; color: #fff; font-weight: 800; font-size: 0.75rem;
  padding: 0.25rem 0.6rem; border-radius: 0.4rem;
}
.res-msg { font-size: 0.85rem; font-weight: 700; color: #0369a1; }
.result-body { display: flex; gap: 1rem; }
.res-item { display: flex; align-items: center; gap: 0.4rem; font-size: 0.85rem; color: #475569; }
.res-item strong { color: var(--text-main); }


/* Section */
.section { margin-bottom: 1.5rem; }

/* Bus List */
.bus-list { display: flex; flex-direction: column; gap: 0.6rem; }
.bus-card {
  display: flex; justify-content: space-between; align-items: center;
  background: var(--bg-surface); padding: 0.85rem 1rem; border-radius: 0.85rem;
  box-shadow: 0 1px 6px rgba(0,0,0,0.04);
}
.bus-left { display: flex; align-items: center; gap: 0.75rem; }
.bus-linha {
  color: #fff; font-weight: 800; font-size: 0.75rem; padding: 0.35rem 0.6rem;
  border-radius: 0.4rem; min-width: 36px; text-align: center;
}
.bus-info { display: flex; flex-direction: column; }
.bus-destino { font-weight: 600; font-size: 0.9rem; color: var(--text-main); }
.bus-lotacao-label { font-size: 0.75rem; font-weight: 600; }
.bus-right { display: flex; flex-direction: column; align-items: flex-end; gap: 0.35rem; }
.bus-minutos { font-size: 1.25rem; font-weight: 800; color: #0284c7; }
.lot-indicator { width: 48px; height: 4px; background: var(--bg-hover); border-radius: 2px; overflow: hidden; }
.lot-bar { height: 100%; border-radius: 2px; transition: width 0.4s; }

/* Favorites */
.fav-list { display: flex; flex-direction: column; gap: 0.5rem; }
.fav-card {
  display: flex; justify-content: space-between; align-items: center;
  background: var(--bg-surface); padding: 0.85rem 1rem; border-radius: 0.85rem;
  box-shadow: 0 1px 6px rgba(0,0,0,0.04);
}
.fav-left { display: flex; align-items: center; gap: 0.75rem; }
.fav-badge {
  color: #fff; font-weight: 800; font-size: 0.75rem; padding: 0.3rem 0.6rem;
  border-radius: 0.4rem;
}
.fav-nome { font-weight: 500; color: #334155; font-size: 0.9rem; }
.fav-arrow { color: #cbd5e1; }

/* Stats */
.stats-section { display: grid; grid-template-columns: 1fr 1fr; gap: 0.75rem; }
.mini-stat {
  display: flex; align-items: center; gap: 0.75rem;
  background: var(--bg-surface); padding: 1rem; border-radius: 0.85rem;
  box-shadow: 0 1px 6px rgba(0,0,0,0.04);
}
.stat-icon { color: #0284c7; }
.stat-val { font-size: 1.15rem; font-weight: 800; color: var(--text-main); display: block; }
.stat-lbl { font-size: 0.7rem; color: var(--text-muted); text-transform: uppercase; font-weight: 600; }
</style>
