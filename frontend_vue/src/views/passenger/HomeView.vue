<script setup>
import { ref, onMounted, computed } from 'vue'
import { MapPin, Navigation, Clock, Bus, ChevronRight, Zap, Star, TrendingUp } from 'lucide-vue-next'

const apiUrl = '/api'
const greeting = ref('')
const busCount = ref(0)
const avgOcc = ref(0)

// Configuração para Planeamento de Viagem
const origin = ref('')
const destination = ref('')
const showSuggestions = ref({ origin: false, destination: false })
const planResult = ref(null)

const paragensBraga = [
  'Terminal Intermodal', 'Universidade do Minho', 'Hospital de Braga', 
  'Estádio Municipal', 'Bom Jesus', 'Sameiro', 'Maximinos', 
  'São Vítor', 'Estação CP', 'Avenida Central', 'Tenões', 'Nogueiró'
]

const suggestions = computed(() => ({
  origin: paragensBraga.filter(p => p.toLowerCase().includes(origin.value.toLowerCase()) && origin.value.length > 0),
  destination: paragensBraga.filter(p => p.toLowerCase().includes(destination.value.toLowerCase()) && destination.value.length > 0)
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

onMounted(async () => {
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
  } catch(e) { /* offline mode */ }
})

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
    <!-- Greeting -->
    <div class="greeting-section">
      <h2 class="greeting">{{ greeting }}, Afonso</h2>
      <p class="greeting-sub">Pronto para a tua viagem?</p>
    </div>

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
.home-page { padding: 1.25rem; padding-bottom: 2rem; }

/* Greeting */
.greeting-section { margin-bottom: 1.5rem; }
.greeting { font-size: 1.5rem; font-weight: 800; color: #0f172a; margin: 0; }
.greeting-sub { color: #64748b; font-size: 0.95rem; margin: 0.25rem 0 0; }

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
  background: #fff; border-radius: 1rem; padding: 1.25rem;
  box-shadow: 0 2px 16px rgba(0,0,0,0.06); margin-bottom: 1.5rem;
}
.section-title {
  display: flex; align-items: center; gap: 0.5rem;
  font-size: 1rem; font-weight: 700; color: #0f172a; margin: 0 0 1rem;
}
.planner-inputs { display: flex; flex-direction: column; gap: 0; margin-bottom: 1rem; }
.planner-input { display: flex; align-items: center; gap: 0.75rem; }
.input-dot { width: 12px; height: 12px; border-radius: 50%; flex-shrink: 0; }
.input-dot.origin { background: #10b981; border: 2px solid #a7f3d0; }
.input-dot.destination { background: #ef4444; border: 2px solid #fecaca; }
.planner-field {
  flex: 1; border: none; border-bottom: 1px solid #e2e8f0; padding: 0.75rem 0;
  font-size: 0.95rem; color: #0f172a; background: transparent; outline: none;
}
.planner-field::placeholder { color: #94a3b8; }
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
  background: #fff; border-radius: 0.75rem; border: 1px solid #e2e8f0;
  box-shadow: 0 10px 25px rgba(0,0,0,0.1);
  z-index: 1000; margin-top: 0.5rem; overflow: hidden;
  max-height: 200px; overflow-y: auto;
}
.suggestion-item {
  padding: 0.75rem 1rem; font-size: 0.9rem; color: #334155;
  cursor: pointer; border-bottom: 1px solid #f1f5f9;
}
.suggestion-item:hover { background: #f8fafc; color: #0284c7; }

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
.res-item strong { color: #0f172a; }


/* Section */
.section { margin-bottom: 1.5rem; }

/* Bus List */
.bus-list { display: flex; flex-direction: column; gap: 0.6rem; }
.bus-card {
  display: flex; justify-content: space-between; align-items: center;
  background: #fff; padding: 0.85rem 1rem; border-radius: 0.85rem;
  box-shadow: 0 1px 6px rgba(0,0,0,0.04);
}
.bus-left { display: flex; align-items: center; gap: 0.75rem; }
.bus-linha {
  color: #fff; font-weight: 800; font-size: 0.75rem; padding: 0.35rem 0.6rem;
  border-radius: 0.4rem; min-width: 36px; text-align: center;
}
.bus-info { display: flex; flex-direction: column; }
.bus-destino { font-weight: 600; font-size: 0.9rem; color: #0f172a; }
.bus-lotacao-label { font-size: 0.75rem; font-weight: 600; }
.bus-right { display: flex; flex-direction: column; align-items: flex-end; gap: 0.35rem; }
.bus-minutos { font-size: 1.25rem; font-weight: 800; color: #0284c7; }
.lot-indicator { width: 48px; height: 4px; background: #f1f5f9; border-radius: 2px; overflow: hidden; }
.lot-bar { height: 100%; border-radius: 2px; transition: width 0.4s; }

/* Favorites */
.fav-list { display: flex; flex-direction: column; gap: 0.5rem; }
.fav-card {
  display: flex; justify-content: space-between; align-items: center;
  background: #fff; padding: 0.85rem 1rem; border-radius: 0.85rem;
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
  background: #fff; padding: 1rem; border-radius: 0.85rem;
  box-shadow: 0 1px 6px rgba(0,0,0,0.04);
}
.stat-icon { color: #0284c7; }
.stat-val { font-size: 1.15rem; font-weight: 800; color: #0f172a; display: block; }
.stat-lbl { font-size: 0.7rem; color: #94a3b8; text-transform: uppercase; font-weight: 600; }
</style>
