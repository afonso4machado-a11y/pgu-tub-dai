<script setup>
import { ref, watch, onMounted, onUnmounted, computed, shallowRef } from 'vue'
import { TresCanvas } from '@tresjs/core'
import { OrbitControls } from '@tresjs/cientos'
import * as THREE from 'three'

const props = defineProps({
  context: { type: String, required: true }, // 'passenger' | 'admin'
  autocarros: { type: Array, default: () => [] },
  paragens: { type: Array, default: () => [] },
  selectedBusId: { type: String, default: null }
})

const emit = defineEmits(['bus-click'])

// Refs
const canvasRef = shallowRef(null)
const controlsRef = shallowRef(null)
const busMeshes = shallowRef(new Map())
const cameraTarget = new THREE.Vector3(0, 0, 0)
const activeCameraTarget = new THREE.Vector3(0, 0, 0)

// Configuração da câmara por contexto
const isPassenger = computed(() => props.context === 'passenger')
const dpr = computed(() => isPassenger.value ? Math.min(window.devicePixelRatio, 1.5) : window.devicePixelRatio)

// Constantes geográficas (Centro de Braga para normalização)
const mapCenter = { lat: 41.5503, lng: -8.4227 }
const scaleFactor = 3000

// Converte Lng/Lat para X/Z
const geoToWorld = (lat, lng) => {
  const x = (lng - mapCenter.lng) * scaleFactor
  const z = -(lat - mapCenter.lat) * scaleFactor
  return new THREE.Vector3(x, 0, z)
}

// Lotação -> Cor
function getBusColor(ocupacao) {
  if (ocupacao > 80) return '#ef4444'
  if (ocupacao > 60) return '#eab308'
  return '#10b981'
}

// Loop de animação manual (substitui useLoop que causa crash fora do TresCanvas)
let animFrame = null
function startLoop() {
  const tick = () => {
    if (isPassenger.value && controlsRef.value?.value) {
      activeCameraTarget.lerp(cameraTarget, 0.08)
      controlsRef.value.value.target.copy(activeCameraTarget)
    }
    animFrame = requestAnimationFrame(tick)
  }
  animFrame = requestAnimationFrame(tick)
}

// Observa mudanças no autocarro selecionado (Tracking câmara)
watch(() => props.selectedBusId, (newId) => {
  if (newId) {
    const bus = props.autocarros.find(b => b.id === newId)
    if (bus) {
      const pos = geoToWorld(bus.lat || 41.5503, bus.lng || -8.4227)
      cameraTarget.copy(pos)
    }
  } else {
    cameraTarget.set(0, 0, 0)
  }
})

// Atualiza posições e cores dos autocarros
watch(() => props.autocarros, (newBuses) => {
  newBuses.forEach(bus => {
    const mesh = busMeshes.value.get(bus.id)
    if (mesh) {
      const targetPos = geoToWorld(bus.lat || 41.5503, bus.lng || -8.4227)
      // Interpolação suave manual (sem gsap para evitar dependência problemática)
      const lerp = (a, b, t) => a + (b - a) * t
      const doLerp = () => {
        mesh.position.x = lerp(mesh.position.x, targetPos.x, 0.05)
        mesh.position.z = lerp(mesh.position.z, targetPos.z, 0.05)
      }
      // Lerp durante ~1s (60 frames)
      let frames = 0
      const anim = () => {
        doLerp()
        if (++frames < 60) requestAnimationFrame(anim)
      }
      requestAnimationFrame(anim)

      // Atualiza cor
      const newColor = new THREE.Color(getBusColor(bus.ocupacao || 0))
      if (mesh.material) {
        mesh.material.color.copy(newColor)
      }

      if (props.selectedBusId === bus.id && isPassenger.value) {
        cameraTarget.copy(targetPos)
      }
    }
  })
}, { deep: true })

const onBusPointerDown = (intersection, busId) => {
  emit('bus-click', busId)
}

// Tema Claro/Escuro
const isDarkMode = ref(false)
const groundColor = computed(() => isDarkMode.value ? '#1e293b' : '#e2e8f0')

let observer = null
onMounted(() => {
  isDarkMode.value = !document.documentElement.getAttribute('data-theme') ||
                     document.documentElement.getAttribute('data-theme') === 'dark'

  observer = new MutationObserver(() => {
    isDarkMode.value = !document.documentElement.getAttribute('data-theme') ||
                       document.documentElement.getAttribute('data-theme') === 'dark'
  })
  observer.observe(document.documentElement, { attributes: true, attributeFilter: ['class', 'data-theme'] })

  startLoop()
})

onUnmounted(() => {
  if (observer) observer.disconnect()
  if (animFrame) cancelAnimationFrame(animFrame)
})
</script>

<template>
  <div class="tres-container">
    <TresCanvas
      clear-color="transparent"
      :pixel-ratio="dpr"
      alpha
      ref="canvasRef"
    >
      <TresPerspectiveCamera
        v-if="isPassenger"
        :position="[0, 30, 40]"
        :fov="45"
        :near="0.1"
        :far="1000"
      />
      <TresPerspectiveCamera
        v-else
        :position="[0, 150, 0]"
        :fov="60"
        :near="0.1"
        :far="2000"
      />

      <OrbitControls
        ref="controlsRef"
        :enable-damping="true"
        :damping-factor="0.05"
        :enable-pan="!isPassenger"
        :max-polar-angle="isPassenger ? Math.PI / 2.5 : Math.PI / 2"
        :min-distance="10"
        :max-distance="isPassenger ? 80 : 300"
      />

      <!-- Iluminação -->
      <TresAmbientLight :intensity="isDarkMode ? 0.4 : 0.8" />
      <TresDirectionalLight
        :position="[50, 100, 50]"
        :intensity="isDarkMode ? 1.5 : 1"
        cast-shadow
      />

      <!-- Chão (Plano) -->
      <TresMesh :rotation="[-Math.PI / 2, 0, 0]" :position="[0, -0.5, 0]" receive-shadow>
        <TresPlaneGeometry :args="[500, 500]" />
        <TresMeshStandardMaterial :color="groundColor" />
      </TresMesh>

      <!-- Paragens -->
      <TresGroup>
        <TresMesh
          v-for="(p, i) in paragens"
          :key="'p'+i"
          :position="geoToWorld(p.lat, p.lng).toArray()"
        >
          <TresCylinderGeometry :args="[0.5, 0.5, 0.2, 16]" />
          <TresMeshStandardMaterial color="#0284c7" />
        </TresMesh>
      </TresGroup>

      <!-- Autocarros -->
      <TresGroup>
        <TresMesh
          v-for="bus in autocarros"
          :key="bus.id"
          :ref="el => { if(el) busMeshes.set(bus.id, el) }"
          :position="geoToWorld(bus.lat || 41.5503, bus.lng || -8.4227).toArray()"
          @pointer-down="(e) => onBusPointerDown(e, bus.id)"
          cast-shadow
        >
          <TresBoxGeometry :args="[2, 3, 5]" />
          <TresMeshStandardMaterial :color="getBusColor(bus.ocupacao)" />

          <!-- Selected Indicator -->
          <TresMesh v-if="selectedBusId === bus.id" :position="[0, 2.5, 0]">
            <TresConeGeometry :args="[0.8, 1.5, 16]" :rotation="[Math.PI, 0, 0]" />
            <TresMeshBasicMaterial color="#38bdf8" />
          </TresMesh>
        </TresMesh>
      </TresGroup>
    </TresCanvas>
  </div>
</template>

<style scoped>
.tres-container {
  width: 100%;
  height: 100%;
  position: absolute;
  top: 0;
  left: 0;
  background: var(--bg-surface, #f8fafc);
  z-index: 10;
}
</style>
