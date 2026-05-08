<script setup>
import { ref, onMounted, onUnmounted, computed, watch, nextTick } from 'vue'
import { MapPin, Route, Settings2, Bus, Users, AlertTriangle, Radio, Locate, Layers } from 'lucide-vue-next'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import LiveMap3D from '../components/LiveMap3D.vue'
import { isWebGLAvailable } from '../utils/webgl'

const apiUrl = '/api'
let map = null
let markersLayer = null
let timer = null

const autocarros = ref([])
const selectedBus = ref(null)
const loading = ref(true)
const filterLine = ref('')
const showOnlyCritical = ref(false)

const webglSupported = ref(false)
const viewMode = ref('2D')

const toggleViewMode = async () => {
  if (viewMode.value === '2D' && webglSupported.value) {
    viewMode.value = '3D'
  } else {
    viewMode.value = '2D'
    await nextTick()
    if (map) map.invalidateSize()
  }
}

const handleBusClick3D = (busId) => {
  const bus = filteredBuses.value.find(b => b.id === busId)
  if (bus) {
    selectedBus.value = bus
  }
}

// Cache de posições estáveis por ID (nunca muda após primeiro cálculo)
const positionsCache = new Map()
// Cache de marcadores Leaflet por ID
const markersCache = new Map()
// Cache da última ocupação conhecida por ID (para evitar setIcon desnecessário)
const lastOccCache = new Map()
// Flag para bloquear updates durante animação de zoom
let isZooming = false
// Fingerprint dos dados para evitar re-renders com dados idênticos
let lastDataFingerprint = ''

// Paragens reais de Braga (exemplos representativos das linhas TUB)
const paragens = [
  // Linha 07H — Celeirós ↔ S. Vítor
  { nome: "S. Mamede d' Este", lat: 41.5680, lng: -8.3920 },
  { nome: 'Avenida da Liberdade', lat: 41.5510, lng: -8.4210 },
  { nome: 'Celeirós', lat: 41.5720, lng: -8.4050 },
  { nome: 'São Vítor', lat: 41.5437, lng: -8.4148 },
  // Linha 40H — Hospital ↔ Real
  { nome: 'Hospital de Braga', lat: 41.5578, lng: -8.3843 },
  { nome: 'Rua Egídio Guimarães', lat: 41.5505, lng: -8.4180 },
  { nome: 'Avenida Central', lat: 41.5492, lng: -8.4260 },
  { nome: 'Rua Mário de Almeida', lat: 41.5470, lng: -8.4300 },
  // Linha 43H — Estação CP ↔ Universidade
  { nome: 'Estação C.P.', lat: 41.5489, lng: -8.4341 },
  { nome: 'Universidade do Minho', lat: 41.5614, lng: -8.3966 },
  // Pontos de referência urbana
  { nome: 'Terminal Intermodal', lat: 41.5503, lng: -8.4227 },
  { nome: 'Maximinos', lat: 41.5461, lng: -8.4378 },
  { nome: 'Bom Jesus', lat: 41.5547, lng: -8.3787 },
  { nome: 'Nogueiró', lat: 41.5519, lng: -8.4486 },
  { nome: 'Gualtar', lat: 41.5590, lng: -8.3980 },
]

const filteredBuses = computed(() => {
  let list = autocarros.value
  if (filterLine.value) list = list.filter(a => a.linhaId === filterLine.value)
  if (showOnlyCritical.value) list = list.filter(a => a.ocupacao > 80)
  return list
})

const availableLines = computed(() => {
  const lines = new Set(autocarros.value.map(a => a.linhaId).filter(Boolean))
  return [...lines].sort()
})

const fleetStats = computed(() => {
  const total = autocarros.value.length
  const critical = autocarros.value.filter(a => a.ocupacao > 80).length
  const online = autocarros.value.filter(a => a.ultimaLeitura && a.ultimaLeitura !== 'N/A').length
  const avgOcc = total > 0
    ? Math.round(autocarros.value.reduce((s, a) => s + (a.ocupacao || 0), 0) / total)
    : 0
  return { total, critical, online, avgOcc }
})

function busColor(occ) {
  if (occ > 90) return '#ef4444'
  if (occ > 70) return '#eab308'
  if (occ > 40) return '#06b6d4'
  return '#14b8a6'
}

function busIcon(bus) {
  const color = busColor(bus.ocupacao || 0)
  return L.divIcon({
    className: 'bus-map-icon',
    html: `<div style="
      background: ${color};
      color: #fff;
      width: 36px; height: 36px;
      display: flex; align-items: center; justify-content: center;
      border-radius: 50%;
      font-weight: 700; font-size: 11px;
      border: 3px solid #fff;
      box-shadow: 0 0 12px ${color}88, 0 2px 8px rgba(0,0,0,0.4);
      font-family: 'Fira Code', monospace;
      pointer-events: auto;
    ">${bus.linhaId || bus.id.substring(0, 4)}</div>`,
    iconSize: [36, 36],
    iconAnchor: [18, 18],
    popupAnchor: [0, -18],
  })
}

function stopIcon() {
  return L.divIcon({
    className: 'stop-map-icon',
    html: `<div style="
      background: rgba(99,102,241,0.9);
      width: 12px; height: 12px;
      border-radius: 50%;
      border: 2px solid #fff;
      box-shadow: 0 0 6px rgba(99,102,241,0.5);
    "></div>`,
    iconSize: [12, 12],
    iconAnchor: [6, 6],
  })
}

function initMap() {
  map = L.map('fleet-map', {
    center: [41.5503, -8.4227],
    zoom: 14,
    zoomControl: false,
    attributionControl: false,
  })

  // Bloquear updates de marcadores durante zoom para evitar saltos
  map.on('zoomstart', () => { isZooming = true })
  map.on('zoomend', () => { isZooming = false })
  map.on('movestart', () => { isZooming = true })
  map.on('moveend', () => { isZooming = false })

  // Standard OpenStreetMap tiles (Free)
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: 19,
    noWrap: true
  }).addTo(map)

  // Fix for map rendering incorrectly or zoomed out when container resizes
  setTimeout(() => {
    if (map) map.invalidateSize()
  }, 400)

  L.control.zoom({ position: 'bottomright' }).addTo(map)
  L.control.attribution({ position: 'bottomleft', prefix: 'OSM' }).addTo(map)

  markersLayer = L.layerGroup().addTo(map)

  // Paragens
  paragens.forEach(p => {
    L.marker([p.lat, p.lng], { icon: stopIcon() })
      .bindTooltip(p.nome, {
        className: 'stop-tooltip',
        direction: 'top',
        offset: [0, -8],
      })
      .addTo(map)
  })
}

/**
 * 🐞 FIX: Posição estável por ID — usa seed determinístico baseado no
 * hash do ID do autocarro, evitando o jitter aleatório que causava os 
 * marcadores a "mudar de sítio" a cada re-render.
 */
function getStablePosition(bus, idx) {
  if (positionsCache.has(bus.id)) {
    return positionsCache.get(bus.id)
  }
  // Hash simples determinístico baseado no ID
  let hash = 0
  for (let i = 0; i < bus.id.length; i++) {
    hash = ((hash << 5) - hash) + bus.id.charCodeAt(i)
    hash |= 0 // Converter para inteiro 32-bit
  }
  const baseLat = 41.5503 + (Math.sin(hash * 1.3) * 0.015)
  const baseLng = -8.4227 + (Math.cos(hash * 0.9) * 0.02)
  // Jitter determinístico (baseado no hash, não em Math.random())
  const jitterLat = (Math.sin(hash * 3.7) * 0.002)
  const jitterLng = (Math.cos(hash * 2.3) * 0.003)
  const pos = [baseLat + jitterLat, baseLng + jitterLng]
  positionsCache.set(bus.id, pos)
  return pos
}

function buildPopupContent(bus) {
  return `
    <div style="font-family:'Inter',sans-serif;min-width:180px;">
      <div style="font-weight:700;font-size:14px;margin-bottom:6px;color:#0ea5e9;">
        ${bus.id}
      </div>
      <div style="font-size:12px;color:#64748b;margin-bottom:8px;">
        Linha: ${bus.linhaId || 'N/A'} · ${bus.marca || ''} ${bus.modelo || ''}
      </div>
      <div style="display:flex;justify-content:space-between;font-size:13px;">
        <span>Ocupação:</span>
        <strong style="color:${busColor(bus.ocupacao)}">${Math.round(bus.ocupacao || 0)}%</strong>
      </div>
      <div style="display:flex;justify-content:space-between;font-size:13px;">
        <span>Passageiros:</span>
        <strong>${bus.passageirosAtuais} / ${bus.capacidadeMaxima}</strong>
      </div>
      <div style="margin-top:6px;font-size:11px;color:#94a3b8;">
        Última leitura: ${bus.ultimaLeitura !== 'N/A' ? new Date(bus.ultimaLeitura).toLocaleTimeString('pt-PT') : '--:--'}
      </div>
    </div>
  `
}

/**
 * Atualização segura de marcadores — só toca no DOM quando:
 * 1. O mapa NÃO está em zoom/pan (isZooming === false)
 * 2. A cor do marcador mudou (banda de ocupação diferente)
 * 3. É um marcador novo (não existe no cache)
 */
function updateMapMarkers() {
  if (!markersLayer || isZooming) return

  const currentIds = new Set(filteredBuses.value.map(b => b.id))

  // Remover marcadores que já não estão nos filtros
  for (const [id, marker] of markersCache) {
    if (!currentIds.has(id)) {
      markersLayer.removeLayer(marker)
      markersCache.delete(id)
      lastOccCache.delete(id)
    }
  }

  filteredBuses.value.forEach((bus, i) => {
    const pos = getStablePosition(bus, i)
    const currentColor = busColor(bus.ocupacao || 0)
    const cachedColor = lastOccCache.get(bus.id)

    if (markersCache.has(bus.id)) {
      // Só atualizar ícone se a COR mudou (mudança de banda de ocupação)
      if (cachedColor !== currentColor) {
        const marker = markersCache.get(bus.id)
        marker.setIcon(busIcon(bus))
        lastOccCache.set(bus.id, currentColor)
      }
      // Popup atualiza-se sempre (texto leve, sem impacto no DOM do ícone)
      markersCache.get(bus.id).setPopupContent(buildPopupContent(bus))
    } else {
      // Novo marcador
      const marker = L.marker(pos, {
        icon: busIcon(bus),
        bubblingMouseEvents: false,
        interactive: true,
      })

      marker.bindPopup(buildPopupContent(bus), {
        className: 'bus-popup',
        autoPan: false,
      })

      marker.on('click', (e) => {
        L.DomEvent.stopPropagation(e)
        selectedBus.value = bus
      })

      marker.addTo(markersLayer)
      markersCache.set(bus.id, marker)
      lastOccCache.set(bus.id, currentColor)
    }
  })
}

// Só dispara updateMapMarkers quando filtros mudam (não no polling)
watch([filterLine, showOnlyCritical], () => {
  updateMapMarkers()
})

async function fetchFleet() {
  try {
    const res = await fetch(`${apiUrl}/autocarros`)
    const data = await res.json()
    if (data.status === 'sucesso') {
      const newBuses = data.autocarros || []
      // Fingerprint: só atualizar se os dados realmente mudaram
      const fp = JSON.stringify(newBuses.map(b => b.id + ':' + Math.round(b.ocupacao || 0)))
      if (fp !== lastDataFingerprint) {
        lastDataFingerprint = fp
        autocarros.value = newBuses
        // Atualizar marcadores apenas se não estiver em zoom
        if (!isZooming) updateMapMarkers()
      }
    }
  } catch(e) {
    console.error('Erro ao obter frota:', e)
  } finally {
    loading.value = false
  }
}

function centerMap() {
  if (map) map.setView([41.5503, -8.4227], 14)
}

onMounted(() => {
  webglSupported.value = isWebGLAvailable()
  initMap()
  fetchFleet()
  timer = setInterval(fetchFleet, 5000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
  markersCache.clear()
  positionsCache.clear()
  if (map) { map.remove(); map = null }
})
</script>

<template>
  <div class="network-view fade-in">
    <!-- Toolbar -->
    <div class="map-toolbar glass-panel">
      <div class="toolbar-left">
        <h3 class="panel-title"><Route class="icon-inline" /> Mapa Operacional — Rede TUB</h3>
        <p class="panel-desc">Monitorização geoespacial em tempo real da frota</p>
      </div>
      <div class="toolbar-controls">
        <button v-if="webglSupported" class="btn btn-secondary btn-sm" @click="toggleViewMode" title="Alternar Vista">
          <Layers :size="16" /> {{ viewMode === '2D' ? 'Vista 3D' : 'Vista 2D' }}
        </button>
        <select v-model="filterLine" class="map-select">
          <option value="">Todas as Linhas</option>
          <option v-for="l in availableLines" :key="l" :value="l">Linha {{ l }}</option>
        </select>
        <label class="toggle-label">
          <input type="checkbox" v-model="showOnlyCritical" />
          <AlertTriangle :size="14" /> Só Críticos
        </label>
        <button class="btn btn-secondary btn-sm" @click="centerMap">
          <Locate :size="16" /> Centrar
        </button>
      </div>
    </div>

    <div class="map-layout">
      <!-- Mapa -->
      <div class="map-wrapper glass-panel">
        <div v-show="viewMode === '2D'" id="fleet-map" class="leaflet-container-dark"></div>

        <LiveMap3D
          v-if="viewMode === '3D'"
          context="admin"
          :autocarros="filteredBuses"
          :paragens="paragens"
          :selectedBusId="selectedBus?.id"
          @bus-click="handleBusClick3D"
        />

        <!-- Overlay Stats -->
        <div class="map-overlay-stats">
          <div class="ov-stat">
            <Bus :size="16" />
            <span class="ov-val fira-code">{{ fleetStats.total }}</span>
            <span class="ov-label">Frota</span>
          </div>
          <div class="ov-stat">
            <Radio :size="16" class="pulse-green" />
            <span class="ov-val fira-code text-teal">{{ fleetStats.online }}</span>
            <span class="ov-label">Online</span>
          </div>
          <div class="ov-stat">
            <AlertTriangle :size="16" class="text-danger" />
            <span class="ov-val fira-code text-danger">{{ fleetStats.critical }}</span>
            <span class="ov-label">Críticos</span>
          </div>
          <div class="ov-stat">
            <Users :size="16" />
            <span class="ov-val fira-code">{{ fleetStats.avgOcc }}%</span>
            <span class="ov-label">Média</span>
          </div>
        </div>
      </div>

      <!-- Sidebar com lista da frota -->
      <div class="fleet-sidebar">
        <div class="glass-panel sidebar-header">
          <h4><Bus class="icon-inline" :size="18" /> Veículos Ativos</h4>
          <span class="badge fira-code">{{ filteredBuses.length }}</span>
        </div>
        <div class="fleet-list">
          <div
            v-for="bus in filteredBuses"
            :key="bus.id"
            class="fleet-card glass-panel"
            :class="{
              'card-critical': bus.ocupacao > 90,
              'card-warning': bus.ocupacao > 70 && bus.ocupacao <= 90,
              'card-selected': selectedBus && selectedBus.id === bus.id
            }"
            @click="selectedBus = bus"
          >
            <div class="fc-header">
              <span class="fc-id fira-code">{{ bus.id }}</span>
              <span class="fc-line" v-if="bus.linhaId">{{ bus.linhaId }}</span>
            </div>
            <div class="fc-bar-container">
              <div
                class="fc-bar"
                :style="{width: Math.min(bus.ocupacao || 0, 100) + '%', background: busColor(bus.ocupacao)}"
              ></div>
            </div>
            <div class="fc-footer">
              <span class="fc-occ fira-code" :style="{color: busColor(bus.ocupacao)}">
                {{ Math.round(bus.ocupacao || 0) }}%
              </span>
              <span class="fc-pax">{{ bus.passageirosAtuais }}/{{ bus.capacidadeMaxima }}</span>
            </div>
          </div>
          <div v-if="filteredBuses.length === 0" class="empty-fleet">
            Nenhum veículo encontrado com os filtros atuais.
          </div>
        </div>
      </div>
    </div>

    <!-- Detail Panel -->
    <div v-if="selectedBus" class="detail-panel glass-panel fade-in">
      <div class="dp-header">
        <h4><Bus class="icon-inline" :size="18" /> Detalhes — {{ selectedBus.id }}</h4>
        <button class="btn-close" @click="selectedBus = null">✕</button>
      </div>
      <div class="dp-grid">
        <div class="dp-item">
          <span class="dp-label">Matrícula</span>
          <span class="dp-val fira-code">{{ selectedBus.matricula || 'N/A' }}</span>
        </div>
        <div class="dp-item">
          <span class="dp-label">Marca / Modelo</span>
          <span class="dp-val">{{ selectedBus.marca || '—' }} {{ selectedBus.modelo || '' }}</span>
        </div>
        <div class="dp-item">
          <span class="dp-label">Linha Atribuída</span>
          <span class="dp-val fira-code text-cyan">{{ selectedBus.linhaId || 'Sem linha' }}</span>
        </div>
        <div class="dp-item">
          <span class="dp-label">Ocupação Atual</span>
          <span class="dp-val fira-code" :style="{color: busColor(selectedBus.ocupacao)}">
            {{ Math.round(selectedBus.ocupacao || 0) }}% ({{ selectedBus.passageirosAtuais }}/{{ selectedBus.capacidadeMaxima }})
          </span>
        </div>
        <div class="dp-item">
          <span class="dp-label">Total Transportados</span>
          <span class="dp-val fira-code">{{ selectedBus.totalPassageirosTransportados || 0 }} pax</span>
        </div>
        <div class="dp-item">
          <span class="dp-label">Última Telemetria</span>
          <span class="dp-val fira-code dim">{{ selectedBus.ultimaLeitura !== 'N/A' ? selectedBus.ultimaLeitura : 'Sem dados' }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.network-view {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  height: calc(100vh - 120px);
}

/* Toolbar */
.map-toolbar { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 1rem; }
.panel-title { display: flex; align-items: center; gap: 0.5rem; margin-bottom: 0.25rem; }
.panel-desc { color: var(--text-muted); font-size: 0.85rem; margin: 0; }
.icon-inline { color: var(--accent-blue); }
.toolbar-controls { display: flex; align-items: center; gap: 1rem; flex-wrap: wrap; }
.map-select {
  background: var(--bg-primary);
  border: 1px solid var(--border-light);
  color: var(--text-main);
  border-radius: 0.4rem;
  padding: 0.45rem 0.75rem;
  font-size: 0.85rem;
}
.toggle-label {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.85rem;
  color: var(--text-muted);
  cursor: pointer;
}
.toggle-label input { accent-color: var(--accent-blue); }
.btn-sm { padding: 0.45rem 0.75rem; font-size: 0.85rem; }

/* Layout */
.map-layout { display: flex; gap: 1rem; flex: 1; min-height: 0; }
.map-wrapper { flex: 1; position: relative; overflow: hidden; padding: 0; border-radius: 1rem; }

#fleet-map { width: 100%; height: 100%; z-index: 1; }

/* Map Stats Overlay */
.map-overlay-stats {
  position: absolute;
  top: 1rem; left: 1rem;
  z-index: 500;
  display: flex;
  gap: 0.75rem;
}
.ov-stat {
  background: rgba(15, 23, 42, 0.85);
  backdrop-filter: blur(12px);
  border: 1px solid var(--border-light);
  border-radius: 0.6rem;
  padding: 0.6rem 0.85rem;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: var(--text-main);
}
.ov-val { font-weight: 700; font-size: 1rem; }
.ov-label { font-size: 0.7rem; color: var(--text-muted); text-transform: uppercase; }

/* Sidebar */
.fleet-sidebar { width: 280px; display: flex; flex-direction: column; gap: 0.5rem; min-height: 0; }
.sidebar-header { display: flex; justify-content: space-between; align-items: center; padding: 0.85rem 1rem; }
.sidebar-header h4 { display: flex; align-items: center; gap: 0.5rem; margin: 0; font-size: 0.95rem; }
.badge { background: var(--accent-blue); color: #fff; padding: 0.15rem 0.6rem; border-radius: 1rem; font-size: 0.75rem; }

.fleet-list { flex: 1; overflow-y: auto; display: flex; flex-direction: column; gap: 0.5rem; padding-right: 0.25rem; }
.fleet-list::-webkit-scrollbar { width: 4px; }
.fleet-list::-webkit-scrollbar-thumb { background: var(--border-light); border-radius: 2px; }

.fleet-card {
  padding: 0.85rem;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid transparent;
}
.fleet-card:hover { border-color: var(--accent-blue); transform: translateX(-2px); }
.card-selected { border-color: var(--accent-blue) !important; box-shadow: 0 0 0 1px var(--accent-blue); }
.card-critical { border-left: 3px solid var(--danger); }
.card-warning { border-left: 3px solid var(--warning); }

.fc-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.5rem; }
.fc-id { font-weight: 700; font-size: 0.9rem; color: var(--text-main); }
.fc-line { background: rgba(6,182,212,0.15); color: var(--accent-blue); padding: 0.1rem 0.5rem; border-radius: 0.3rem; font-size: 0.75rem; font-weight: 600; }
.fc-bar-container { height: 4px; background: rgba(255,255,255,0.05); border-radius: 2px; overflow: hidden; margin-bottom: 0.4rem; }
.fc-bar { height: 100%; border-radius: 2px; transition: width 0.5s ease; }
.fc-footer { display: flex; justify-content: space-between; align-items: center; }
.fc-occ { font-weight: 700; font-size: 0.85rem; }
.fc-pax { font-size: 0.75rem; color: var(--text-muted); }

.empty-fleet { text-align: center; color: var(--text-muted); font-style: italic; padding: 2rem 1rem; }

/* Detail Panel */
.detail-panel { margin-top: 0; }
.dp-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem; }
.dp-header h4 { display: flex; align-items: center; gap: 0.5rem; margin: 0; }
.btn-close { background: transparent; border: 1px solid var(--border-light); color: var(--text-muted); border-radius: 0.3rem; padding: 0.3rem 0.6rem; cursor: pointer; transition: all 0.2s; }
.btn-close:hover { color: var(--danger); border-color: var(--danger); }

.dp-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 1rem; }
.dp-item { display: flex; flex-direction: column; gap: 0.25rem; }
.dp-label { font-size: 0.75rem; text-transform: uppercase; color: var(--text-muted); font-weight: 600; letter-spacing: 0.04em; }
.dp-val { font-size: 1rem; font-weight: 500; color: var(--text-main); }

.text-cyan { color: var(--accent-blue); }
.text-teal { color: var(--accent-teal); }
.text-danger { color: var(--danger); }
.dim { color: var(--text-muted); }

.pulse-green { animation: pulse-g 2s infinite; }
@keyframes pulse-g {
  0%, 100% { opacity: 0.6; }
  50% { opacity: 1; filter: drop-shadow(0 0 4px var(--accent-teal)); }
}

@media (max-width: 900px) {
  .map-layout { flex-direction: column; }
  .fleet-sidebar { width: 100%; max-height: 250px; }
}
</style>

<style>
/* Global Leaflet overrides for dark theme */
.leaflet-container { background: #020617 !important; }
.leaflet-control-zoom a { background: rgba(15,23,42,0.9) !important; color: #e2e8f0 !important; border-color: rgba(255,255,255,0.1) !important; }
.leaflet-control-zoom a:hover { background: rgba(6,182,212,0.2) !important; }
.stop-tooltip { background: rgba(15,23,42,0.95) !important; color: #e2e8f0 !important; border: 1px solid rgba(99,102,241,0.4) !important; font-size: 12px !important; font-family: 'Inter', sans-serif !important; }
.stop-tooltip::before { border-top-color: rgba(15,23,42,0.95) !important; }
.bus-popup .leaflet-popup-content-wrapper { background: rgba(15,23,42,0.95) !important; color: #e2e8f0 !important; border: 1px solid rgba(6,182,212,0.3) !important; border-radius: 0.75rem !important; }
.bus-popup .leaflet-popup-tip { background: rgba(15,23,42,0.95) !important; }
.bus-map-icon, .stop-map-icon { background: none !important; border: none !important; }
</style>
