<script setup>
import { ref, watch, onMounted, onUnmounted, computed, shallowRef } from 'vue'
import { TresCanvas, useLoop } from '@tresjs/core'
import { OrbitControls } from '@tresjs/cientos'
import * as THREE from 'three'
import gsap from 'gsap'

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
const scaleFactor = 3000 // Aumentar escala para espalhar os objetos

// Converte Lng/Lat para X/Z
const geoToWorld = (lat, lng) => {
  const x = (lng - mapCenter.lng) * scaleFactor
  const z = -(lat - mapCenter.lat) * scaleFactor // Inverter lat para o eixo Z (norte = -z em WebGL)
  return new THREE.Vector3(x, 0, z)
}

// Lotação -> Cor
function getBusColor(ocupacao) {
  if (ocupacao > 80) return '#ef4444' // Lotado
  if (ocupacao > 60) return '#eab308' // Moderado
  return '#10b981' // Livre
}

const { onLoop } = useLoop()

onLoop(({ delta, elapsed }) => {
  if (isPassenger.value && controlsRef.value) {
    // Interpolação suave para a câmara (Tracking do Autocarro)
    activeCameraTarget.lerp(cameraTarget, 5 * delta)
    controlsRef.value.value.target.copy(activeCameraTarget)
  }
})

// Observa mudanças no autocarro selecionado (Tracking câmara)
watch(() => props.selectedBusId, (newId) => {
  if (newId) {
    const bus = props.autocarros.find(b => b.id === newId)
    if (bus) {
      const pos = geoToWorld(bus.lat || 41.5503, bus.lng || -8.4227)
      cameraTarget.copy(pos)
    }
  } else {
    // Reset para centro
    cameraTarget.set(0, 0, 0)
  }
})

// Atualiza posições e cores dos autocarros
watch(() => props.autocarros, (newBuses) => {
  newBuses.forEach(bus => {
    const mesh = busMeshes.value.get(bus.id)
    if (mesh) {
      const targetPos = geoToWorld(bus.lat || 41.5503, bus.lng || -8.4227)
      // Animação da posição
      gsap.to(mesh.position, {
        x: targetPos.x,
        z: targetPos.z,
        duration: 1,
        ease: 'power2.out'
      })

      // Atualiza cor se mudou
      const newColor = new THREE.Color(getBusColor(bus.ocupacao || 0))
      gsap.to(mesh.material.color, {
        r: newColor.r,
        g: newColor.g,
        b: newColor.b,
        duration: 0.5
      })

      // Update tracking target if this is the selected bus
      if (props.selectedBusId === bus.id && isPassenger.value) {
        cameraTarget.copy(targetPos)
      }
    }
  })
}, { deep: true })

const onBusPointerDown = (intersection, busId) => {
  emit('bus-click', busId)
}

// Inicialização de Meshes manuais para performance / referência
const onMapReady = () => {
  // Inicialização adicional se necessário
}

// Tema Claro/Escuro (observa classe no DOM)
const isDarkMode = ref(document.documentElement.classList.contains('dark-theme'))
const bgColor = computed(() => isDarkMode.value ? '#0f172a' : '#f8fafc') // Slate 900 vs Slate 50
const groundColor = computed(() => isDarkMode.value ? '#1e293b' : '#e2e8f0') // Slate 800 vs Slate 200

onMounted(() => {
  const observer = new MutationObserver(() => {
    isDarkMode.value = document.documentElement.classList.contains('dark-theme') ||
                       document.body.classList.contains('dark-theme') ||
                       document.documentElement.getAttribute('data-theme') === 'dark'
  })
  observer.observe(document.documentElement, { attributes: true, attributeFilter: ['class', 'data-theme'] })
  observer.observe(document.body, { attributes: true, attributeFilter: ['class'] })

  onUnmounted(() => observer.disconnect())
})
</script>

<template>
  <div class="tres-container">
    <TresCanvas
      clear-color="transparent"
      :pixel-ratio="dpr"
      alpha
      ref="canvasRef"
      @ready="onMapReady"
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
        <!-- Usamos ref de array para manter track -->
        <TresMesh
          v-for="bus in autocarros"
          :key="bus.id"
          :ref="el => { if(el) busMeshes.set(bus.id, el) }"
          :position="geoToWorld(bus.lat || 41.5503, bus.lng || -8.4227).toArray()"
          @pointer-down="(e) => onBusPointerDown(e, bus.id)"
          cast-shadow
        >
          <!-- Box: Autocarro -->
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
:global(.dark-theme) .tres-container {
  background: #0f172a;
}
</style>
