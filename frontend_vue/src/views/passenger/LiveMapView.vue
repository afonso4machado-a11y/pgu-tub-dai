<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { Users, Locate } from 'lucide-vue-next'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'

const apiUrl = '/api'
let map = null
let markersLayer = null
let timer = null
const autocarros = ref([])
const selectedBus = ref(null)

// Cache de posições estáveis por ID (calculada uma vez com hash determinístico)
const positionsCache = new Map()
// Cache de marcadores Leaflet por ID
const markersCache = new Map()
// Cache da última cor por ID — só chama setIcon se a cor mudou
const lastColorCache = new Map()
// Bloquear updates durante zoom/pan para evitar saltos de marcadores
let isZooming = false
// Fingerprint para evitar re-renders com dados idênticos
let lastDataFingerprint = ''

const paragens = [
  { nome: 'Terminal Intermodal', lat: 41.5503, lng: -8.4227 },
  { nome: 'Universidade do Minho', lat: 41.5614, lng: -8.3966 },
  { nome: 'Hospital de Braga', lat: 41.5578, lng: -8.3843 },
  { nome: 'Estádio Municipal', lat: 41.5623, lng: -8.4291 },
  { nome: 'Bom Jesus', lat: 41.5547, lng: -8.3787 },
  { nome: 'Maximinos', lat: 41.5461, lng: -8.4378 },
  { nome: 'São Vítor', lat: 41.5437, lng: -8.4148 },
  { nome: 'Estação CP', lat: 41.5489, lng: -8.4341 },
  { nome: 'Avenida Central', lat: 41.5492, lng: -8.4260 },
  { nome: 'Nogueiró', lat: 41.5519, lng: -8.4486 },
]

function lotColor(occ) {
  if (occ > 80) return '#ef4444'
  if (occ > 60) return '#eab308'
  return '#10b981'
}

function lotEmoji(occ) {
  if (occ > 80) return '🔴'
  if (occ > 60) return '🟡'
  return '🟢'
}

function lotLabel(occ) {
  if (occ > 80) return 'Lotado'
  if (occ > 60) return 'Moderado'
  return 'Lugares livres'
}

function busIcon(bus) {
  const color = lotColor(bus.ocupacao || 0)
  return L.divIcon({
    className: 'pwa-bus-icon',
    html: `<div style="
      background: ${color};
      color: #fff;
      width: 32px; height: 32px;
      display: flex; align-items: center; justify-content: center;
      border-radius: 50%;
      font-weight: 800; font-size: 10px;
      border: 3px solid #fff;
      box-shadow: 0 2px 8px ${color}66;
      font-family: -apple-system, sans-serif;
    ">${bus.linhaId || '?'}</div>`,
    iconSize: [32, 32],
    iconAnchor: [16, 16],
  })
}

function stopIcon() {
  return L.divIcon({
    className: 'pwa-stop-icon',
    html: `<div style="
      background: #0284c7;
      width: 10px; height: 10px;
      border-radius: 50%;
      border: 2px solid #fff;
      box-shadow: 0 1px 4px rgba(0,0,0,0.2);
    "></div>`,
    iconSize: [10, 10],
    iconAnchor: [5, 5],
  })
}

function initMap() {
  map = L.map('passenger-map', {
    center: [41.5503, -8.4227],
    zoom: 14,
    zoomControl: false,
    attributionControl: false,
  })

  // Bloquear updates durante zoom/pan para evitar saltos
  map.on('zoomstart', () => { isZooming = true })
  map.on('zoomend',   () => { isZooming = false })
  map.on('movestart', () => { isZooming = true })
  map.on('moveend',   () => { isZooming = false })

  L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png', {
    maxZoom: 19,
  }).addTo(map)

  markersLayer = L.layerGroup().addTo(map)

  paragens.forEach(p => {
    L.marker([p.lat, p.lng], { icon: stopIcon() })
      .bindTooltip(p.nome, { direction: 'top', offset: [0, -6], className: 'pwa-tooltip' })
      .addTo(map)
  })
}

// Posição determinística por ID (sem Math.random — nunca muda)
function getStablePos(bus) {
  if (positionsCache.has(bus.id)) return positionsCache.get(bus.id)
  let hash = 0
  for (let i = 0; i < bus.id.length; i++) {
    hash = ((hash << 5) - hash) + bus.id.charCodeAt(i)
    hash |= 0
  }
  const pos = [
    41.5503 + Math.sin(hash * 1.3) * 0.015 + Math.sin(hash * 3.7) * 0.002,
    -8.4227 + Math.cos(hash * 0.9) * 0.02  + Math.cos(hash * 2.3) * 0.003,
  ]
  positionsCache.set(bus.id, pos)
  return pos
}

// Atualização incremental — nunca toca no DOM durante zoom
function updateMarkers() {
  if (!markersLayer || isZooming) return

  const currentIds = new Set(autocarros.value.map(b => b.id))

  // Remover marcadores obsoletos
  for (const [id, marker] of markersCache) {
    if (!currentIds.has(id)) {
      markersLayer.removeLayer(marker)
      markersCache.delete(id)
      lastColorCache.delete(id)
    }
  }

  autocarros.value.forEach(bus => {
    const currentColor = lotColor(bus.ocupacao || 0)

    if (markersCache.has(bus.id)) {
      // Só atualizar ícone se a cor (banda de lotação) mudou
      if (lastColorCache.get(bus.id) !== currentColor) {
        markersCache.get(bus.id).setIcon(busIcon(bus))
        lastColorCache.set(bus.id, currentColor)
      }
    } else {
      // Novo marcador
      const pos = getStablePos(bus)
      const marker = L.marker(pos, {
        icon: busIcon(bus),
        bubblingMouseEvents: false,
      })
      marker.on('click', (e) => {
        L.DomEvent.stopPropagation(e)
        selectedBus.value = bus
      })
      marker.addTo(markersLayer)
      markersCache.set(bus.id, marker)
      lastColorCache.set(bus.id, currentColor)
    }
  })
}

async function fetchBuses() {
  try {
    const res = await fetch(`${apiUrl}/autocarros`)
    const data = await res.json()
    if (data.status === 'sucesso') {
      const newBuses = data.autocarros || []
      // Só atualizar se os dados mudaram (evita re-render idle)
      const fp = JSON.stringify(newBuses.map(b => b.id + ':' + Math.round(b.ocupacao || 0)))
      if (fp !== lastDataFingerprint) {
        lastDataFingerprint = fp
        autocarros.value = newBuses
        if (!isZooming) updateMarkers()
      }
    }
  } catch(e) { /* offline */ }
}

function centerMap() {
  if (map) map.setView([41.5503, -8.4227], 14)
}

function dismissCard() { selectedBus.value = null }

onMounted(() => { initMap(); fetchBuses(); timer = setInterval(fetchBuses, 5000) })
onUnmounted(() => {
  if (timer) clearInterval(timer)
  markersCache.clear()
  positionsCache.clear()
  lastColorCache.clear()
  if (map) { map.remove(); map = null }
})
</script>

<template>
  <div class="map-page">
    <div id="passenger-map" class="map-container"></div>

    <!-- Map Controls -->
    <div class="map-controls">
      <button class="map-btn" @click="centerMap"><Locate :size="20" /></button>
    </div>

    <!-- Legend -->
    <div class="map-legend">
      <span class="legend-item">🟢 Livre</span>
      <span class="legend-item">🟡 Moderado</span>
      <span class="legend-item">🔴 Lotado</span>
    </div>

    <!-- Selected Bus Card -->
    <transition name="card-slide">
      <div v-if="selectedBus" class="bus-detail-card" @click="dismissCard">
        <div class="bdc-header">
          <span class="bdc-linha" :style="{background: lotColor(selectedBus.ocupacao)}">
            {{ selectedBus.linhaId || selectedBus.id }}
          </span>
          <span class="bdc-id">{{ selectedBus.id }}</span>
        </div>
        <div class="bdc-body">
          <div class="bdc-metric">
            <Users :size="16" />
            <span>{{ selectedBus.passageirosAtuais }} / {{ selectedBus.capacidadeMaxima }}</span>
          </div>
          <div class="bdc-lot">
            <span class="bdc-lot-text" :style="{color: lotColor(selectedBus.ocupacao)}">
              {{ lotEmoji(selectedBus.ocupacao) }} {{ lotLabel(selectedBus.ocupacao) }} · {{ Math.round(selectedBus.ocupacao) }}%
            </span>
            <div class="bdc-bar-track">
              <div class="bdc-bar" :style="{width: Math.min(selectedBus.ocupacao, 100) + '%', background: lotColor(selectedBus.ocupacao)}"></div>
            </div>
          </div>
        </div>
        <p class="bdc-tip">Toca para fechar</p>
      </div>
    </transition>
  </div>
</template>

<style scoped>
.map-page { position: relative; height: 100%; }
.map-container { width: 100%; height: 100%; }

.map-controls {
  position: absolute; bottom: 5rem; right: 1rem; z-index: 500;
  display: flex; flex-direction: column; gap: 0.5rem;
}
.map-btn {
  width: 44px; height: 44px; border-radius: 50%;
  background: #fff; border: none; color: #0284c7;
  box-shadow: 0 2px 12px rgba(0,0,0,0.15);
  display: flex; align-items: center; justify-content: center;
  cursor: pointer; transition: transform 0.15s;
}
.map-btn:active { transform: scale(0.93); }

.map-legend {
  position: absolute; top: 0.75rem; left: 0.75rem; z-index: 500;
  display: flex; gap: 0.5rem;
  background: rgba(255,255,255,0.95); padding: 0.5rem 0.75rem;
  border-radius: 2rem; box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  font-size: 0.7rem; font-weight: 600; color: #334155;
}

/* Bus Detail Card */
.bus-detail-card {
  position: absolute; bottom: 1rem; left: 1rem; right: 1rem;
  z-index: 600; background: #fff; border-radius: 1rem;
  padding: 1.25rem; box-shadow: 0 8px 30px rgba(0,0,0,0.15);
}
.bdc-header { display: flex; align-items: center; gap: 0.75rem; margin-bottom: 0.75rem; }
.bdc-linha {
  color: #fff; font-weight: 800; font-size: 0.8rem; padding: 0.35rem 0.75rem;
  border-radius: 0.5rem;
}
.bdc-id { font-weight: 700; color: #0f172a; font-size: 1.05rem; }
.bdc-body { display: flex; flex-direction: column; gap: 0.5rem; }
.bdc-metric { display: flex; align-items: center; gap: 0.5rem; color: #64748b; font-size: 0.9rem; }
.bdc-lot-text { font-weight: 700; font-size: 0.85rem; }
.bdc-bar-track { height: 6px; background: #f1f5f9; border-radius: 3px; overflow: hidden; margin-top: 0.35rem; }
.bdc-bar { height: 100%; border-radius: 3px; transition: width 0.5s; }
.bdc-tip { text-align: center; color: #cbd5e1; font-size: 0.7rem; margin: 0.75rem 0 0; }

.card-slide-enter-active, .card-slide-leave-active { transition: all 0.25s ease; }
.card-slide-enter-from, .card-slide-leave-to { opacity: 0; transform: translateY(100%); }
</style>

<style>
.pwa-bus-icon, .pwa-stop-icon { background: none !important; border: none !important; }
.pwa-tooltip {
  background: #fff !important; color: #0f172a !important;
  border: 1px solid #e2e8f0 !important; font-size: 11px !important;
  font-family: -apple-system, sans-serif !important; font-weight: 600 !important;
  border-radius: 0.5rem !important; padding: 4px 8px !important;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1) !important;
}
</style>
