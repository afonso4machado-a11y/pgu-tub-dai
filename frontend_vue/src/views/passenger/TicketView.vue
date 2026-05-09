<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { Ticket, Shield, Clock, RefreshCw, CheckCircle, QrCode } from 'lucide-vue-next'

import { authService } from '../../services/auth'

const localUser = authService.getUser()
const activeTicket = ref({
  tipo: localUser?.passeMensal ? 'Passe Mensal' : 'Bilhete Simples',
  zona: 'Zona Urbana Braga',
  validade: localUser?.passeMensal ? '2026-04-30' : '-',
  titular: localUser?.nome || 'Utilizador',
  nif: localUser?.nif || '--- --- ---',
  estado: localUser?.passeMensal ? 'Ativo' : 'Pendente',
})

const qrData = ref('')
const qrCountdown = ref(30)
const qrExpired = ref(false)
let countdownTimer = null

function generateQRPayload() {
  const ts = Date.now()
  const token = btoa(`TUB|${activeTicket.value.tipo}|${activeTicket.value.titular}|${ts}|${Math.random().toString(36).substring(7)}`)
  return token
}

function refreshQR() {
  qrData.value = generateQRPayload()
  qrCountdown.value = 30
  qrExpired.value = false
}

// Simple QR-like grid pattern generator (visual representation)
function generateQRGrid() {
  const grid = []
  const payload = qrData.value
  for (let r = 0; r < 21; r++) {
    const row = []
    for (let c = 0; c < 21; c++) {
      // Fixed patterns (finder patterns)
      if ((r < 7 && c < 7) || (r < 7 && c > 13) || (r > 13 && c < 7)) {
        if (r === 0 || r === 6 || c === 0 || c === 6 || (r >= 2 && r <= 4 && c >= 2 && c <= 4) ||
            (r === 0 || r === 6) || (c === 0 || c === 6) ||
            (r >= 2 && r <= 4 && c >= 2 && c <= 4)) {
          // Simplified finder pattern
          const isOuter = r === 0 || r === 6 || c === 0 || c === 6
          const isInner = r >= 2 && r <= 4 && c >= 2 && c <= 4
          row.push(isOuter || isInner ? 1 : 0)
        } else {
          row.push(0)
        }
      } else {
        // Data area - pseudo-random based on payload
        const charCode = payload.charCodeAt((r * 21 + c) % payload.length) || 0
        row.push((charCode + r * c) % 3 === 0 ? 1 : 0)
      }
    }
    grid.push(row)
  }
  return grid
}

const qrGrid = computed(() => generateQRGrid())

const validadeFormatted = computed(() => {
  if (activeTicket.value.validade === '-') return 'N/A'
  return new Date(activeTicket.value.validade).toLocaleDateString('pt-PT', {
    day: 'numeric', month: 'long', year: 'numeric'
  })
})

const diasRestantes = computed(() => {
  const diff = new Date(activeTicket.value.validade) - new Date()
  return Math.max(0, Math.ceil(diff / (1000 * 60 * 60 * 24)))
})

onMounted(() => {
  refreshQR()
  countdownTimer = setInterval(() => {
    if (qrCountdown.value > 0) {
      qrCountdown.value--
    } else {
      qrExpired.value = true
      refreshQR()
    }
  }, 1000)
})

onUnmounted(() => { if (countdownTimer) clearInterval(countdownTimer) })
</script>

<template>
  <div class="ticket-page">
    <!-- Ticket Card -->
    <div class="ticket-card">
      <div class="tc-header">
        <div class="tc-brand">
          <Ticket :size="22" />
          <span>TUB Digital</span>
        </div>
        <div class="tc-status">
          <CheckCircle :size="14" />
          <span>{{ activeTicket.estado }}</span>
        </div>
      </div>

      <div class="tc-body">
        <div class="tc-type">{{ activeTicket.tipo }}</div>
        <div class="tc-zone">{{ activeTicket.zona }}</div>
        <div class="tc-holder">{{ activeTicket.titular }}</div>
      </div>

      <div class="tc-footer">
        <div class="tc-detail">
          <span class="tc-label">Válido até</span>
          <span class="tc-value">{{ validadeFormatted }}</span>
        </div>
        <div class="tc-detail">
          <span class="tc-label">Restam</span>
          <span class="tc-value tc-days">{{ diasRestantes }} dias</span>
        </div>
      </div>

      <!-- Tear line -->
      <div class="tear-line">
        <div class="tear-circle left"></div>
        <div class="tear-dashes"></div>
        <div class="tear-circle right"></div>
      </div>

      <!-- QR Section -->
      <div class="qr-section">
        <p class="qr-instruction">Apresente ao validador à entrada</p>

        <div class="qr-container" :class="{'qr-refreshing': qrExpired}">
          <!-- QR Grid -->
          <div class="qr-grid">
            <div v-for="(row, r) in qrGrid" :key="r" class="qr-row">
              <div
                v-for="(cell, c) in row"
                :key="c"
                class="qr-cell"
                :class="{'qr-filled': cell === 1}"
              ></div>
            </div>
          </div>
        </div>

        <div class="qr-meta">
          <div class="qr-timer">
            <Clock :size="14" />
            <span>Renova em <strong>{{ qrCountdown }}s</strong></span>
          </div>
          <button class="qr-refresh-btn" @click="refreshQR">
            <RefreshCw :size="14" /> Renovar
          </button>
        </div>

        <div class="qr-security">
          <Shield :size="12" />
          <span>Token anti-fraude · Renovação automática a cada 30s</span>
        </div>
      </div>
    </div>

    <!-- Actions -->
    <div class="ticket-actions">
      <button class="action-btn primary" @click="$router.push('/app/buy-ticket')">
        <Ticket :size="18" /> Comprar Novo Bilhete
      </button>
      <button class="action-btn secondary">
        <QrCode :size="18" /> Histórico de Viagens
      </button>
    </div>
  </div>
</template>

<style scoped>
.ticket-page { padding: 1.25rem; padding-bottom: 2rem; }

/* Ticket Card */
.ticket-card {
  background: linear-gradient(145deg, #0284c7, #0369a1);
  border-radius: 1.5rem;
  overflow: hidden;
  box-shadow: 0 12px 32px rgba(3, 105, 161, 0.25);
  color: #fff;
}

.tc-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 1.5rem 1.75rem 0;
}
.tc-brand { display: flex; align-items: center; gap: 0.5rem; font-weight: 800; font-size: 1rem; }
.tc-status {
  display: flex; align-items: center; gap: 0.3rem;
  background: rgba(255,255,255,0.2); padding: 0.3rem 0.75rem;
  border-radius: 2rem; font-size: 0.75rem; font-weight: 700;
}

.tc-body { padding: 1.25rem 1.75rem; }
.tc-type { font-size: 1.6rem; font-weight: 800; margin-bottom: 0.25rem; }
.tc-zone { font-size: 0.95rem; opacity: 0.85; margin-bottom: 0.5rem; }
.tc-holder { font-size: 0.9rem; opacity: 0.7; font-weight: 500; }

.tc-footer { display: flex; gap: 2rem; padding: 0 1.75rem 1.5rem; }
.tc-detail { display: flex; flex-direction: column; }
.tc-label { font-size: 0.7rem; opacity: 0.6; text-transform: uppercase; font-weight: 600; letter-spacing: 0.04em; }
.tc-value { font-size: 0.95rem; font-weight: 700; }
.tc-days { color: #7dd3fc; }

/* Tear Line */
.tear-line {
  position: relative; display: flex; align-items: center;
  height: 24px; margin: 0 -2px;
}
.tear-circle {
  width: 24px; height: 24px; border-radius: 50%; background: var(--bg-primary);
  flex-shrink: 0;
}
.tear-circle.left { margin-left: -12px; }
.tear-circle.right { margin-right: -12px; }
.tear-dashes {
  flex: 1; border-top: 2px dashed rgba(255,255,255,0.3);
}

/* QR Section */
.qr-section {
  background: var(--bg-surface); padding: 1.5rem; text-align: center;
}
.qr-instruction { color: var(--text-muted); font-size: 0.8rem; font-weight: 600; margin: 0 0 1rem; text-transform: uppercase; letter-spacing: 0.04em; }

.qr-container {
  display: inline-block; padding: 0.75rem;
  border: 2px solid var(--border-light); border-radius: 0.75rem;
  transition: all 0.3s;
}
.qr-refreshing { opacity: 0.3; }

.qr-grid { display: flex; flex-direction: column; gap: 0; }
.qr-row { display: flex; gap: 0; }
.qr-cell { width: 10px; height: 10px; background: transparent; }
.qr-filled { background: var(--text-main); }

.qr-meta {
  display: flex; justify-content: center; align-items: center; gap: 1rem;
  margin-top: 1rem;
}
.qr-timer {
  display: flex; align-items: center; gap: 0.35rem;
  color: var(--text-muted); font-size: 0.8rem;
}
.qr-refresh-btn {
  display: flex; align-items: center; gap: 0.3rem;
  background: var(--bg-hover); border: 1px solid var(--border-light);
  color: var(--accent-blue); padding: 0.35rem 0.75rem; border-radius: 2rem;
  font-size: 0.75rem; font-weight: 700; cursor: pointer;
  transition: all 0.2s;
}
.qr-refresh-btn:active { background: var(--bg-hover-strong); }

.qr-security {
  display: flex; align-items: center; justify-content: center; gap: 0.35rem;
  margin-top: 0.75rem; color: var(--text-muted); font-size: 0.7rem;
}

/* Actions */
.ticket-actions { display: flex; flex-direction: column; gap: 0.85rem; margin-top: 1.75rem; }
.action-btn {
  display: flex; align-items: center; justify-content: center; gap: 0.5rem;
  padding: 1.15rem; border-radius: 1.25rem; font-weight: 700; font-size: 0.95rem;
  border: none; cursor: pointer; transition: all 0.15s, box-shadow 0.2s;
}
.action-btn:active { transform: scale(0.98); }
.action-btn.primary { background: var(--accent-blue); color: #fff; box-shadow: 0 4px 16px rgba(2,132,199,0.3); }
.action-btn.secondary { background: var(--bg-surface); color: var(--text-main); border: 1px solid var(--border-light); box-shadow: 0 2px 8px rgba(0,0,0,0.05); }
</style>
