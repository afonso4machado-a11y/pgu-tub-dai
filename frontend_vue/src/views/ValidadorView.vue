<script setup>
import { ref, onUnmounted, computed } from 'vue'
import jsQR from 'jsqr'
import {
  ScanLine, Bus, LogIn, LogOut, CheckCircle2, XCircle,
  Camera, CameraOff, Trash2, Clock, User, Ticket
} from 'lucide-vue-next'
import { apiFetch, DEMO_AUTOCARROS } from '../services/api.js'

// ─── ESTADO PRINCIPAL ────────────────────────────────────────

const autocarroId = ref('')
const modoAtual = ref('entrada') // 'entrada' | 'saida'

const scanning = ref(false)
const cameraError = ref('')
const lastResult = ref(null) // { ok, tipo, nome, bilhete, ts }
const historico = ref([])    // registo local da sessão

let videoEl = null
let canvasEl = null
let animFrameId = null
let stream = null

// ─── AUTOCARROS DISPONÍVEIS ───────────────────────────────────

const autocarros = ref(DEMO_AUTOCARROS.map(a => ({ id: a.id, linha: a.linhaId })))

// ─── CÂMARA ───────────────────────────────────────────────────

async function iniciarCamera() {
  cameraError.value = ''
  lastResult.value = null

  if (!autocarroId.value) {
    cameraError.value = 'Selecione um autocarro antes de iniciar o scanner.'
    return
  }

  try {
    stream = await navigator.mediaDevices.getUserMedia({
      video: { facingMode: 'environment', width: { ideal: 1280 }, height: { ideal: 720 } }
    })

    // Aguardar DOM
    await new Promise(r => setTimeout(r, 50))
    videoEl = document.getElementById('scanner-video')
    canvasEl = document.getElementById('scanner-canvas')

    if (!videoEl || !canvasEl) {
      cameraError.value = 'Erro interno: elementos de vídeo não encontrados.'
      return
    }

    videoEl.srcObject = stream
    await videoEl.play()
    scanning.value = true
    tick()
  } catch (e) {
    if (e.name === 'NotAllowedError') {
      cameraError.value = 'Permissão de câmara negada. Active nas definições do browser.'
    } else if (e.name === 'NotFoundError') {
      cameraError.value = 'Nenhuma câmara encontrada neste dispositivo.'
    } else {
      cameraError.value = `Erro ao aceder à câmara: ${e.message}`
    }
  }
}

function pararCamera() {
  scanning.value = false
  if (animFrameId) cancelAnimationFrame(animFrameId)
  if (stream) {
    stream.getTracks().forEach(t => t.stop())
    stream = null
  }
}

function tick() {
  if (!scanning.value || !videoEl || !canvasEl) return

  if (videoEl.readyState === videoEl.HAVE_ENOUGH_DATA) {
    const ctx = canvasEl.getContext('2d')
    canvasEl.width  = videoEl.videoWidth
    canvasEl.height = videoEl.videoHeight
    ctx.drawImage(videoEl, 0, 0, canvasEl.width, canvasEl.height)

    const imageData = ctx.getImageData(0, 0, canvasEl.width, canvasEl.height)
    const code = jsQR(imageData.data, imageData.width, imageData.height, {
      inversionAttempts: 'dontInvert'
    })

    if (code) {
      processarQR(code.data)
      return // pausar scan enquanto processa
    }
  }

  animFrameId = requestAnimationFrame(tick)
}

// ─── PROCESSAMENTO DO QR ──────────────────────────────────────

const processando = ref(false)

async function processarQR(raw) {
  if (processando.value) return
  processando.value = true
  pararCamera()

  try {
    const payload = JSON.parse(raw)

    // Validar formato
    if (payload.type !== 'TUB_TICKET') throw new Error('QR inválido')

    // Verificar anti-replay: token não pode ter mais de 35 segundos
    const idade = (Date.now() - payload.ts) / 1000
    if (idade > 35) throw new Error('QR expirado')

    // Registar leitura no backend
    const delta = modoAtual.value === 'entrada'
      ? { entradas: 1, saidas: 0 }
      : { entradas: 0, saidas: 1 }

    const { live } = await apiFetch('/leituras', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ id: autocarroId.value, ...delta })
    })

    const entry = {
      id: Date.now(),
      ok: true,
      tipo: modoAtual.value,
      nome: payload.nome,
      bilhete: payload.bilhete,
      nif: payload.nif,
      hora: new Date().toLocaleTimeString('pt-PT', { hour: '2-digit', minute: '2-digit', second: '2-digit' }),
      autocarro: autocarroId.value,
      live
    }

    lastResult.value = entry
    historico.value.unshift(entry)

  } catch (e) {
    const entry = {
      id: Date.now(),
      ok: false,
      erro: e.message === 'QR expirado' ? 'QR expirado (>35s)' : 'QR inválido ou ilegível',
      hora: new Date().toLocaleTimeString('pt-PT', { hour: '2-digit', minute: '2-digit', second: '2-digit' }),
    }
    lastResult.value = entry
    historico.value.unshift(entry)
  } finally {
    processando.value = false
  }
}

function limparResultado() {
  lastResult.value = null
}

function reiniciarScanner() {
  lastResult.value = null
  iniciarCamera()
}

function limparHistorico() {
  historico.value = []
}

// Estatísticas da sessão
const stats = computed(() => ({
  entradas: historico.value.filter(h => h.ok && h.tipo === 'entrada').length,
  saidas:   historico.value.filter(h => h.ok && h.tipo === 'saida').length,
  erros:    historico.value.filter(h => !h.ok).length,
}))

onUnmounted(pararCamera)
</script>

<template>
  <div class="validador-page">

    <!-- CABEÇALHO ──────────────────────────────────────────── -->
    <div class="page-header">
      <div class="page-title-block">
        <ScanLine :size="28" class="page-icon" />
        <div>
          <h1 class="page-title">Validador QR</h1>
          <p class="page-sub">Registo de entradas e saídas por bilhete digital</p>
        </div>
      </div>

      <!-- Stats da sessão -->
      <div class="session-stats">
        <div class="stat-pill entrada">
          <LogIn :size="14" /> {{ stats.entradas }} entradas
        </div>
        <div class="stat-pill saida">
          <LogOut :size="14" /> {{ stats.saidas }} saídas
        </div>
        <div v-if="stats.erros" class="stat-pill erro">
          <XCircle :size="14" /> {{ stats.erros }} erros
        </div>
      </div>
    </div>

    <div class="validador-layout">

      <!-- COLUNA ESQUERDA: Configuração + Scanner ─────────── -->
      <div class="left-col">

        <!-- Configuração -->
        <div class="card config-card">
          <h3 class="card-title"><Bus :size="16" /> Configuração</h3>

          <label class="field-label">Autocarro</label>
          <select v-model="autocarroId" class="field-select" :disabled="scanning">
            <option value="">— Selecionar autocarro —</option>
            <option v-for="a in autocarros" :key="a.id" :value="a.id">
              {{ a.id }} · Linha {{ a.linha }}
            </option>
          </select>

          <label class="field-label" style="margin-top: 1rem;">Modo de Validação</label>
          <div class="modo-tabs">
            <button
              class="modo-btn"
              :class="{ active: modoAtual === 'entrada', entrada: modoAtual === 'entrada' }"
              :disabled="scanning"
              @click="modoAtual = 'entrada'"
            >
              <LogIn :size="16" /> Entrada
            </button>
            <button
              class="modo-btn"
              :class="{ active: modoAtual === 'saida', saida: modoAtual === 'saida' }"
              :disabled="scanning"
              @click="modoAtual = 'saida'"
            >
              <LogOut :size="16" /> Saída
            </button>
          </div>
        </div>

        <!-- Scanner -->
        <div class="card scanner-card">
          <h3 class="card-title"><Camera :size="16" /> Câmara</h3>

          <!-- Erro de câmara -->
          <div v-if="cameraError" class="camera-error">
            <CameraOff :size="20" />
            <span>{{ cameraError }}</span>
          </div>

          <!-- Resultado da leitura -->
          <div v-else-if="lastResult" class="scan-result" :class="lastResult.ok ? 'result-ok' : 'result-err'">
            <div class="result-icon">
              <CheckCircle2 v-if="lastResult.ok" :size="40" />
              <XCircle v-else :size="40" />
            </div>
            <div class="result-body">
              <div class="result-label">
                {{ lastResult.ok
                  ? (lastResult.tipo === 'entrada' ? '✅ Entrada validada' : '✅ Saída registada')
                  : '❌ Leitura inválida'
                }}
              </div>
              <div v-if="lastResult.ok" class="result-details">
                <span class="result-nome">{{ lastResult.nome }}</span>
                <span class="result-bilhete">{{ lastResult.bilhete }}</span>
                <span class="result-hora">{{ lastResult.hora }}</span>
              </div>
              <div v-else class="result-erro-msg">{{ lastResult.erro }}</div>
            </div>
            <div class="result-actions">
              <button class="btn-scan-again" @click="reiniciarScanner">
                <ScanLine :size="15" /> Ler outro
              </button>
            </div>
          </div>

          <!-- Vídeo da câmara -->
          <div v-else-if="scanning" class="camera-active">
            <div class="video-wrapper">
              <video id="scanner-video" class="camera-video" autoplay muted playsinline></video>
              <canvas id="scanner-canvas" class="camera-canvas"></canvas>
              <!-- Overlay de mira -->
              <div class="scan-overlay">
                <div class="scan-frame" :class="modoAtual">
                  <span class="scan-corner tl"></span>
                  <span class="scan-corner tr"></span>
                  <span class="scan-corner bl"></span>
                  <span class="scan-corner br"></span>
                  <div class="scan-line"></div>
                </div>
                <p class="scan-hint">Aponte para o QR Code do bilhete</p>
              </div>
            </div>
            <button class="btn-stop" @click="pararCamera">
              <CameraOff :size="15" /> Parar câmara
            </button>
          </div>

          <!-- Estado inicial -->
          <div v-else class="camera-idle">
            <div class="idle-icon"><ScanLine :size="40" /></div>
            <p class="idle-text">Câmara inativa</p>
            <button
              class="btn-start"
              :class="modoAtual"
              :disabled="!autocarroId"
              @click="iniciarCamera"
            >
              <Camera :size="16" />
              Iniciar scanner
              <span v-if="!autocarroId" class="btn-hint">(selecione autocarro)</span>
            </button>
          </div>
        </div>
      </div>

      <!-- COLUNA DIREITA: Registo ──────────────────────────── -->
      <div class="right-col">
        <div class="card log-card">
          <div class="log-header">
            <h3 class="card-title"><Clock :size="16" /> Registo da Sessão</h3>
            <button v-if="historico.length" class="btn-clear" @click="limparHistorico">
              <Trash2 :size="14" /> Limpar
            </button>
          </div>

          <div v-if="!historico.length" class="log-empty">
            <ScanLine :size="28" style="opacity:0.3" />
            <p>Nenhuma leitura ainda.<br>Inicie o scanner para começar.</p>
          </div>

          <div v-else class="log-list">
            <div
              v-for="entry in historico"
              :key="entry.id"
              class="log-entry"
              :class="entry.ok ? (entry.tipo === 'entrada' ? 'log-entrada' : 'log-saida') : 'log-erro'"
            >
              <div class="log-icon">
                <LogIn  v-if="entry.ok && entry.tipo === 'entrada'" :size="16" />
                <LogOut v-else-if="entry.ok && entry.tipo === 'saida'" :size="16" />
                <XCircle v-else :size="16" />
              </div>
              <div class="log-info">
                <div v-if="entry.ok" class="log-nome">
                  <User :size="12" /> {{ entry.nome }}
                </div>
                <div v-if="entry.ok" class="log-bilhete">
                  <Ticket :size="12" /> {{ entry.bilhete }} · {{ entry.autocarro }}
                </div>
                <div v-if="!entry.ok" class="log-nome" style="color: #ef4444;">
                  {{ entry.erro }}
                </div>
              </div>
              <div class="log-time">{{ entry.hora }}</div>
            </div>
          </div>
        </div>
      </div>

    </div>
  </div>
</template>

<style scoped>
.validador-page {
  padding: 2rem;
  max-width: 1200px;
  margin: 0 auto;
}

/* ── Cabeçalho ── */
.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 1rem;
  margin-bottom: 2rem;
}
.page-title-block {
  display: flex;
  align-items: center;
  gap: 1rem;
}
.page-icon { color: #0284c7; }
.page-title {
  font-size: 1.75rem;
  font-weight: 800;
  color: var(--text-main);
  margin: 0;
}
.page-sub {
  color: var(--text-muted);
  font-size: 0.9rem;
  margin: 0.2rem 0 0;
}

.session-stats {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
  align-items: center;
}
.stat-pill {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.4rem 0.85rem;
  border-radius: 2rem;
  font-size: 0.8rem;
  font-weight: 700;
}
.stat-pill.entrada { background: #dcfce7; color: #15803d; }
.stat-pill.saida   { background: #eff6ff; color: #1d4ed8; }
.stat-pill.erro    { background: #fef2f2; color: #dc2626; }

/* ── Layout ── */
.validador-layout {
  display: grid;
  grid-template-columns: 380px 1fr;
  gap: 1.5rem;
  align-items: start;
}
@media (max-width: 900px) {
  .validador-layout { grid-template-columns: 1fr; }
}

/* ── Cards ── */
.card {
  background: var(--bg-surface);
  border-radius: 1rem;
  padding: 1.5rem;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
  margin-bottom: 1.25rem;
}
.card-title {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.85rem;
  font-weight: 700;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin: 0 0 1.25rem;
}

/* ── Configuração ── */
.field-label {
  display: block;
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--text-muted);
  margin-bottom: 0.4rem;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}
.field-select {
  width: 100%;
  padding: 0.7rem 0.9rem;
  border: 1.5px solid var(--border-light);
  border-radius: 0.75rem;
  background: var(--bg-primary);
  color: var(--text-main);
  font-size: 0.9rem;
  outline: none;
  cursor: pointer;
}
.field-select:focus { border-color: #0284c7; }
.field-select:disabled { opacity: 0.5; cursor: not-allowed; }

.modo-tabs {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.5rem;
}
.modo-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.4rem;
  padding: 0.65rem;
  border-radius: 0.75rem;
  border: 1.5px solid var(--border-light);
  background: var(--bg-primary);
  color: var(--text-muted);
  font-weight: 700;
  font-size: 0.9rem;
  cursor: pointer;
  transition: all 0.15s;
}
.modo-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.modo-btn.active.entrada { background: #dcfce7; border-color: #16a34a; color: #15803d; }
.modo-btn.active.saida   { background: #eff6ff; border-color: #2563eb; color: #1d4ed8; }

/* ── Scanner ── */
.camera-error {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 0.75rem;
  padding: 1rem;
  color: #dc2626;
  font-size: 0.85rem;
}

.camera-idle {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
  padding: 2rem 1rem;
}
.idle-icon { color: #cbd5e1; }
.idle-text { color: var(--text-muted); font-size: 0.9rem; margin: 0; }

.btn-start {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.8rem 1.75rem;
  border-radius: 0.85rem;
  border: none;
  font-weight: 700;
  font-size: 0.95rem;
  cursor: pointer;
  transition: all 0.15s;
  color: #fff;
}
.btn-start.entrada { background: #16a34a; }
.btn-start.saida   { background: #2563eb; }
.btn-start:disabled { background: #94a3b8; cursor: not-allowed; }
.btn-hint { font-size: 0.75rem; font-weight: 400; opacity: 0.8; }

/* ── Câmara ativa ── */
.camera-active {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}
.video-wrapper {
  position: relative;
  width: 100%;
  border-radius: 0.75rem;
  overflow: hidden;
  background: #000;
  aspect-ratio: 4/3;
}
.camera-video {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.camera-canvas {
  display: none;
}

/* Overlay com mira */
.scan-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 1rem;
}
.scan-frame {
  position: relative;
  width: 200px;
  height: 200px;
}
.scan-corner {
  position: absolute;
  width: 24px;
  height: 24px;
  border-color: #fff;
  border-style: solid;
}
.scan-frame.entrada .scan-corner { border-color: #4ade80; }
.scan-frame.saida   .scan-corner { border-color: #60a5fa; }

.scan-corner.tl { top: 0; left: 0;  border-width: 3px 0 0 3px; }
.scan-corner.tr { top: 0; right: 0; border-width: 3px 3px 0 0; }
.scan-corner.bl { bottom: 0; left: 0;  border-width: 0 0 3px 3px; }
.scan-corner.br { bottom: 0; right: 0; border-width: 0 3px 3px 0; }

.scan-line {
  position: absolute;
  left: 0; right: 0;
  height: 2px;
  background: rgba(255,255,255,0.7);
  animation: scanline 2s ease-in-out infinite;
}
.scan-frame.entrada .scan-line { background: #4ade80; }
.scan-frame.saida   .scan-line { background: #60a5fa; }

@keyframes scanline {
  0%   { top: 0; }
  50%  { top: calc(100% - 2px); }
  100% { top: 0; }
}

.scan-hint {
  color: #fff;
  font-size: 0.8rem;
  font-weight: 600;
  text-shadow: 0 1px 4px rgba(0,0,0,0.6);
  margin: 0;
  background: rgba(0,0,0,0.35);
  padding: 0.3rem 0.8rem;
  border-radius: 2rem;
}

.btn-stop {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  width: 100%;
  padding: 0.7rem;
  border-radius: 0.75rem;
  border: 1.5px solid var(--border-light);
  background: var(--bg-primary);
  color: var(--text-muted);
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
}
.btn-stop:hover { background: #fef2f2; color: #dc2626; border-color: #fca5a5; }

/* ── Resultado ── */
.scan-result {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1.25rem;
  padding: 1.5rem 1rem;
  border-radius: 0.85rem;
  text-align: center;
}
.result-ok  { background: #f0fdf4; border: 2px solid #86efac; }
.result-err { background: #fef2f2; border: 2px solid #fca5a5; }

.result-ok  .result-icon { color: #16a34a; }
.result-err .result-icon { color: #dc2626; }

.result-label {
  font-size: 1.1rem;
  font-weight: 800;
  color: var(--text-main);
}
.result-details {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  margin-top: 0.5rem;
}
.result-nome   { font-size: 1rem; font-weight: 700; color: var(--text-main); }
.result-bilhete { font-size: 0.85rem; color: #0284c7; font-weight: 600; }
.result-hora   { font-size: 0.8rem; color: var(--text-muted); }
.result-erro-msg { font-size: 0.9rem; color: #dc2626; font-weight: 600; }

.result-actions { width: 100%; }
.btn-scan-again {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  width: 100%;
  padding: 0.75rem;
  border-radius: 0.75rem;
  border: none;
  background: #0284c7;
  color: #fff;
  font-weight: 700;
  cursor: pointer;
}

/* ── Registo ── */
.log-card { height: fit-content; }
.log-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1rem;
}
.log-header .card-title { margin: 0; }

.btn-clear {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.35rem 0.75rem;
  border-radius: 2rem;
  border: 1px solid var(--border-light);
  background: transparent;
  color: var(--text-muted);
  font-size: 0.75rem;
  font-weight: 600;
  cursor: pointer;
}
.btn-clear:hover { color: #dc2626; border-color: #fca5a5; }

.log-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.75rem;
  padding: 2.5rem 1rem;
  color: var(--text-muted);
  text-align: center;
  font-size: 0.85rem;
}

.log-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  max-height: 600px;
  overflow-y: auto;
}
.log-entry {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem 1rem;
  border-radius: 0.75rem;
  border: 1px solid transparent;
}
.log-entrada { background: #f0fdf4; border-color: #bbf7d0; }
.log-saida   { background: #eff6ff; border-color: #bfdbfe; }
.log-erro    { background: #fef2f2; border-color: #fecaca; }

.log-entrada .log-icon { color: #16a34a; }
.log-saida   .log-icon { color: #2563eb; }
.log-erro    .log-icon { color: #dc2626; }

.log-info { flex: 1; min-width: 0; }
.log-nome {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--text-main);
}
.log-bilhete {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  font-size: 0.75rem;
  color: var(--text-muted);
  margin-top: 0.15rem;
}
.log-time {
  font-size: 0.75rem;
  color: var(--text-muted);
  font-feature-settings: "tnum";
  white-space: nowrap;
}
</style>
