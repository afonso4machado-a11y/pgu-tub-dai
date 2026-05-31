<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { Locate } from 'lucide-vue-next'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'

import { apiFetch } from '../../services/api.js'
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
 // Linha 02H — Ponte de Prado ↔ Bom Jesus
 { nome: 'Ponte de Prado', lat: 41.5612, lng: -8.4315 },
 { nome: 'Avenida da Liberdade', lat: 41.5510, lng: -8.4210 },
 { nome: 'Terminal Intermodal', lat: 41.5503, lng: -8.4227 },
 { nome: 'Santuário do Bom Jesus', lat: 41.5621, lng: -8.4147 },
 // Linha 03H — Avenida Central ↔ Ruães
 { nome: 'Avenida Central / Hospital', lat: 41.5578, lng: -8.3843 },
 { nome: 'Praça Conde de Agrolongo', lat: 41.5542, lng: -8.4335 },
 { nome: 'Ruães', lat: 41.5476, lng: -8.4395 },
 // Linha 12H — Av. da Liberdade ↔ Lageosa/Pedralva via Gualtar
 { nome: 'Avenida da Liberdade', lat: 41.5510, lng: -8.4210 },
 { nome: 'Gualtar / Universidade', lat: 41.5590, lng: -8.3980 },
 { nome: 'Lageosa', lat: 41.5813, lng: -8.4047 },
 { nome: 'Pedralva', lat: 41.5942, lng: -8.3961 },
 // Linha 19H — Areal ↔ Boavista
 { nome: 'Areal', lat: 41.5514, lng: -8.4094 },
 { nome: 'Jardim da Ponte', lat: 41.5528, lng: -8.4065 },
 { nome: 'Boavista', lat: 41.5607, lng: -8.4160 },
 // Linha 05H — Dume ↔ Quinta da Capela
 { nome: 'Igreja de Dume', lat: 41.5569, lng: -8.4270 },
 { nome: 'Dume / Ruas de Dume', lat: 41.5580, lng: -8.4325 },
 { nome: 'Quinta da Capela', lat: 41.5785, lng: -8.4182 },
 // Linha 06H — Av. Gen. Norton de Matos ↔ Gondizalves/Semelhe
 { nome: 'Av. Gen. Norton de Matos', lat: 41.5600, lng: -8.4200 },
 { nome: 'Semelhe', lat: 41.5828, lng: -8.4182 },
 { nome: 'Gondizalves', lat: 41.5770, lng: -8.3974 },
 // Linha 08H — Rua 25 de Abril ↔ Sete Fontes
 { nome: 'Rua 25 de Abril', lat: 41.5466, lng: -8.4312 },
 { nome: 'Sete Fontes', lat: 41.5397, lng: -8.4471 },
 // Linha 09H — Ruães ↔ Nogueira (Barral)
 { nome: 'Ruães', lat: 41.5476, lng: -8.4395 },
 { nome: 'Nogueira (Barral)', lat: 41.5620, lng: -8.4480 },
 // Linha 13H — Av. Gen. Norton de Matos ↔ Lageosa/Pedralva
 { nome: 'Av. Gen. Norton de Matos', lat: 41.5600, lng: -8.4200 },
 { nome: 'Lageosa', lat: 41.5813, lng: -8.4047 },
 // Linha 14H — Praça Conde de Agrolongo ↔ Priscos
 { nome: 'Praça Conde de Agrolongo', lat: 41.5542, lng: -8.4335 },
 { nome: 'Priscos', lat: 41.5655, lng: -8.4728 },
 // Linha 18H — Rua do Raio ↔ Pinheiro do Bicho via Esporões
 { nome: 'Rua do Raio', lat: 41.5837, lng: -8.3962 },
 { nome: 'Esporões', lat: 41.5766, lng: -8.4816 },
 { nome: 'Pinheiro do Bicho', lat: 41.5772, lng: -8.4954 },
 // Linha 20H — Av. da Liberdade ↔ Escudeiros via Ponte Nova
 { nome: 'Ponte Nova', lat: 41.5598, lng: -8.4022 },
 { nome: 'Escudeiros', lat: 41.5735, lng: -8.4052 },
 // Pontos de referência urbana
 { nome: 'Terminal Intermodal', lat: 41.5503, lng: -8.4227 },
 { nome: 'Maximinos', lat: 41.5461, lng: -8.4378 },
 { nome: 'Bom Jesus', lat: 41.5547, lng: -8.3787 },
 { nome: 'Nogueiró', lat: 41.5519, lng: -8.4486 },
 { nome: 'Gualtar', lat: 41.5590, lng: -8.3980 },
]

function lotColor(occ) {
 if (occ > 80) return '#ef4444'
 if (occ > 60) return '#eab308'
 return '#10b981'
}

function lotEmoji(occ) {
 if (occ > 80) return '-'
 if (occ > 60) return '-'
 return '-'
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
 map.on('zoomend', () => { isZooming = false })
 map.on('movestart', () => { isZooming = true })
 map.on('moveend', () => { isZooming = false })

 L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png', {
 maxZoom: 19
 }).addTo(map)

 // Force recalculation after flexbox/DOM layout settles
 setTimeout(() => {
 if (map) map.invalidateSize()
 }, 400)

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
 -8.4227 + Math.cos(hash * 0.9) * 0.02 + Math.cos(hash * 2.3) * 0.003,
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
 const { data } = await apiFetch('/autocarros')
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

let userMarker = null
let geoWatchId = null

function drawUserMarker(lat, lng, shouldCenter) {
  if (!map) return
  if (shouldCenter) map.setView([lat, lng], 16)
  if (userMarker) {
    userMarker.setLatLng([lat, lng])
  } else {
    const userIcon = L.divIcon({
      className: 'user-gps-marker',
      html: '<div class="pulse-ring"></div><div class="pulse-dot"></div>',
      iconSize: [24, 24],
      iconAnchor: [12, 12]
    })
    userMarker = L.marker([lat, lng], { icon: userIcon }).addTo(map)
  }
}

function locateUser() {
 if (!map) return
 if (navigator.geolocation) {
   if (geoWatchId === null) {
     geoWatchId = navigator.geolocation.watchPosition(
       (pos) => drawUserMarker(pos.coords.latitude, pos.coords.longitude, true),
       (err) => {
         alert("O browser não conseguiu o GPS (" + err.message + "). Vamos simular a localização para a apresentação.")
         drawUserMarker(41.5503, -8.4227, true) // Fallback ativado de emergência se o PC falhar
       },
       { timeout: 10000, maximumAge: 0 } // Sem enableHighAccuracy para não bloquear PCs
     )
   } else {
     navigator.geolocation.getCurrentPosition(
       (pos) => drawUserMarker(pos.coords.latitude, pos.coords.longitude, true),
       (err) => alert("Erro ao centrar GPS: " + err.message)
     )
   }
 } else {
   alert("O seu browser não suporta geolocalização.")
 }
}

function dismissCard() { selectedBus.value = null }

let resizeObserver = null

onMounted(() => {
  initMap()
  fetchBuses()
  timer = setInterval(fetchBuses, 5000)

  // Iniciar geolocalização automaticamente ao abrir a página
  locateUser()

  // Fix for map rendering incorrectly or showing blank areas when container resizes
  const mapEl = document.getElementById('passenger-map')
  if (mapEl && typeof ResizeObserver !== 'undefined') {
    resizeObserver = new ResizeObserver(() => {
      if (map) {
        map.invalidateSize({ debounceMove: false })
        requestAnimationFrame(() => {
          if (map) map.invalidateSize({ debounceMove: false })
        })
      }
    })
    resizeObserver.observe(mapEl)
  }
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
  if (resizeObserver) {
    resizeObserver.disconnect()
    resizeObserver = null
  }
  if (geoWatchId !== null && navigator.geolocation) {
    navigator.geolocation.clearWatch(geoWatchId)
  }
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
 <button class="map-btn" @click="locateUser"><Locate :size="20" /></button>
 </div>

 <!-- Legend -->
 <div class="map-legend">
 <span class="legend-item"><span class="dot" style="background:#10b981"></span> Livre</span>
 <span class="legend-item"><span class="dot" style="background:#eab308"></span> Moderado</span>
 <span class="legend-item"><span class="dot" style="background:#ef4444"></span> Lotado</span>
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
  <span>Passageiros: <strong>{{ selectedBus.passageirosAtuais }} / {{ selectedBus.capacidadeMaxima }}</strong></span>
  </div>
 <div class="bdc-lot">
 <span class="bdc-lot-text" :style="{color: lotColor(selectedBus.ocupacao)}">
 {{ lotLabel(selectedBus.ocupacao) }} · {{ Math.round(selectedBus.ocupacao) }}%
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
 background: var(--bg-surface); border: none; color: #0284c7;
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
 z-index: 600; background: var(--bg-surface); border-radius: 1rem;
 padding: 1.25rem; box-shadow: 0 8px 30px rgba(0,0,0,0.15);
}
.bdc-header { display: flex; align-items: center; gap: 0.75rem; margin-bottom: 0.75rem; }
.bdc-linha {
 color: #fff; font-weight: 800; font-size: 0.8rem; padding: 0.35rem 0.75rem;
 border-radius: 0.5rem;
}
.bdc-id { font-weight: 700; color: var(--text-main); font-size: 1.05rem; }
.bdc-body { display: flex; flex-direction: column; gap: 0.5rem; }
.bdc-metric { display: flex; align-items: center; gap: 0.5rem; color: var(--text-muted); font-size: 0.9rem; }
.bdc-lot-text { font-weight: 700; font-size: 0.85rem; }
.bdc-bar-track { height: 6px; background: var(--bg-hover); border-radius: 3px; overflow: hidden; margin-top: 0.35rem; }
.bdc-bar { height: 100%; border-radius: 3px; transition: width 0.5s; }
.bdc-tip { text-align: center; color: #cbd5e1; font-size: 0.7rem; margin: 0.75rem 0 0; }
.dot { display: inline-block; width: 8px; height: 8px; border-radius: 50%; margin-right: 2px; }

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
 .user-gps-marker { position: relative; }
 .pulse-dot { width: 16px; height: 16px; background: #10b981; border: 2.5px solid white; border-radius: 50%; position: absolute; top: 4px; left: 4px; box-shadow: 0 2px 5px rgba(0,0,0,0.3); z-index: 1000; }
 .pulse-ring { width: 48px; height: 48px; background: rgba(16, 185, 129, 0.4); border-radius: 50%; position: absolute; top: -12px; left: -12px; animation: pulsate 2s ease-out infinite; }
 @keyframes pulsate { 0% { transform: scale(0.1); opacity: 1; } 100% { transform: scale(1); opacity: 0; } }
 </style>
