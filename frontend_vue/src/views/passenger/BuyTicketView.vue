<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { loadStripe } from '@stripe/stripe-js'
import { ArrowLeft, Shield, CheckCircle, Loader2, CreditCard, Smartphone, Ticket } from 'lucide-vue-next'
import { authService } from '../../services/auth'

const router = useRouter()
const user = authService.getUser()

// Flow: 'select' -> 'checkout' -> 'processing' -> 'success'
const step = ref('select')
const selectedId = ref('simples')
const paymentMethod = ref('card') // 'card' | 'mbway' | 'apple'
const mbwayPhone = ref('')
const isProcessing = ref(false)
const errorMessage = ref('')

let stripe = null
let elements = null
const stripeContainer = ref(null)

const catalog = [
  { id: 'simples', name: 'Bilhete Simples', price: 1.55, desc: 'Valido para 1 viagem em qualquer linha TUB.', badge: 'Viagem unica' },
  { id: 'passe', name: 'Passe Mensal', price: 30.00, desc: 'Viagens ilimitadas durante 30 dias em toda a rede.', badge: 'Mais popular' }
]

const selected = computed(() => catalog.find(t => t.id === selectedId.value))
const formattedPrice = computed(() => selected.value?.price.toFixed(2).replace('.', ',') + ' EUR')
const ctaLabel = computed(() => {
  if (isProcessing.value) return ''
  const price = selected.value?.price.toFixed(2).replace('.', ',')
  if (paymentMethod.value === 'mbway') return `Pagar ${price} EUR com MB WAY`
  if (paymentMethod.value === 'apple') return `Pagar com Apple Pay`
  return `Pagar ${price} EUR`
})

onMounted(async () => {
  const key = import.meta.env.VITE_STRIPE_PUBLIC_KEY || 'pk_test_51TV9cB2Zu827UzcHZ0e3PHKZLWVChT3Vag39Mkv4bsXj6B4C4g6POPhGSps7AovYNYpHZg39uoendvmZBYZKeJCa0079Gpuisx'
  stripe = await loadStripe(key)
})

const goBack = () => {
  if (step.value === 'checkout') { step.value = 'select'; elements = null; errorMessage.value = '' }
  else router.push('/app/ticket')
}

const goToCheckout = async () => {
  step.value = 'checkout'
  errorMessage.value = ''
  await mountStripeElements()
}

const mountStripeElements = async () => {
  if (!stripe) return
  isProcessing.value = true
  errorMessage.value = ''
  try {
    const res = await fetch('/api/payments/create-intent', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ tipoId: selectedId.value, clienteId: user?.id || 'anonimo' })
    })
    if (!res.ok) { const e = await res.json(); throw new Error(e.mensagem || 'Erro ao inicializar.') }
    const { clientSecret } = await res.json()

    const root = document.documentElement
    const isDark = !root.getAttribute('data-theme') || root.getAttribute('data-theme') === 'dark'

    elements = stripe.elements({
      clientSecret,
      appearance: {
        theme: isDark ? 'night' : 'flat',
        variables: {
          colorPrimary: isDark ? '#06b6d4' : '#0284c7',
          colorBackground: isDark ? '#1a1a1a' : '#ffffff',
          colorText: isDark ? '#f8fafc' : '#0f172a',
          colorDanger: '#ef4444',
          fontFamily: 'Inter, sans-serif',
          borderRadius: '12px',
          spacingUnit: '4px'
        },
        rules: {
          '.Input': { border: `1px solid ${isDark ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.1)'}`, padding: '14px 16px', fontSize: '16px' },
          '.Input:focus': { borderColor: isDark ? '#06b6d4' : '#0284c7', boxShadow: `0 0 0 3px ${isDark ? 'rgba(6,182,212,0.25)' : 'rgba(2,132,199,0.25)'}` },
          '.Label': { fontSize: '13px', fontWeight: '500', marginBottom: '6px' }
        }
      }
    })
    const el = elements.create('payment', { layout: { type: 'tabs', defaultCollapsed: false } })
    setTimeout(() => { if (stripeContainer.value) el.mount(stripeContainer.value) }, 80)
  } catch (err) {
    errorMessage.value = err.message || 'Falha ao inicializar pagamento. Verifique a sua ligacao.'
    // Mantém o utilizador no checkout (não volta para select)
  } finally {
    isProcessing.value = false
  }
}

const pay = async () => {
  if (!stripe || !elements) return
  isProcessing.value = true
  errorMessage.value = ''
  step.value = 'processing'

  try {
    const { error: submitErr } = await elements.submit()
    if (submitErr) { errorMessage.value = submitErr.message; step.value = 'checkout'; return }

    const { error } = await stripe.confirmPayment({
      elements,
      confirmParams: { return_url: window.location.origin + '/app/ticket' },
      redirect: 'if_required'
    })
    if (error) { errorMessage.value = error.message || 'Pagamento recusado.'; step.value = 'checkout'; return }
    step.value = 'success'
  } catch (e) {
    errorMessage.value = 'Erro de comunicacao. Verifique a ligacao.'
    step.value = 'checkout'
  } finally {
    isProcessing.value = false
  }
}
</script>

<template>
  <div class="checkout-page">

    <!-- Header -->
    <header class="checkout-header">
      <button v-if="step !== 'success' && step !== 'processing'" @click="goBack" class="header-back" aria-label="Voltar">
        <ArrowLeft :size="20" />
      </button>
      <div class="header-center">
        <h1 v-if="step === 'select'">Comprar Bilhete</h1>
        <h1 v-else-if="step === 'checkout'">Finalizar Compra</h1>
        <h1 v-else-if="step === 'processing'">A processar...</h1>
        <h1 v-else>Pagamento Concluido</h1>
      </div>
      <div class="header-spacer"></div>
    </header>

    <!-- Step: Select Ticket -->
    <div v-if="step === 'select'" class="checkout-body fade-up">
      <p class="section-label">Selecione o seu titulo de transporte</p>

      <div class="ticket-cards">
        <button
          v-for="t in catalog" :key="t.id"
          class="ticket-card"
          :class="{ active: selectedId === t.id }"
          @click="selectedId = t.id"
        >
          <div class="tc-badge" :class="{ popular: t.id === 'passe' }">{{ t.badge }}</div>
          <div class="tc-icon"><Ticket :size="28" /></div>
          <div class="tc-name">{{ t.name }}</div>
          <div class="tc-price">{{ t.price.toFixed(2).replace('.', ',') }}<span>EUR</span></div>
          <div class="tc-desc">{{ t.desc }}</div>
          <div class="tc-radio"><div class="tc-radio-dot"></div></div>
        </button>
      </div>

      <div class="checkout-footer">
        <button class="cta-btn" @click="goToCheckout">
          Continuar <span class="cta-price">{{ formattedPrice }}</span>
        </button>
      </div>
    </div>

    <!-- Step: Checkout -->
    <div v-if="step === 'checkout'" class="checkout-body fade-up">
      <!-- Order Summary Mini -->
      <div class="order-summary">
        <div class="os-left">
          <Ticket :size="20" class="os-icon" />
          <div>
            <div class="os-name">{{ selected?.name }}</div>
            <div class="os-desc">Rede TUB completa</div>
          </div>
        </div>
        <div class="os-price">{{ selected?.price.toFixed(2).replace('.', ',') }} EUR</div>
      </div>

      <!-- Stripe Payment Element -->
      <div class="payment-card">
        <div class="pc-title">
          <CreditCard :size="18" />
          <span>Dados de Pagamento</span>
        </div>

        <div v-if="isProcessing && !errorMessage" class="stripe-loading">
          <Loader2 class="spin" :size="28" />
          <span>A preparar pagamento seguro...</span>
        </div>

        <div v-show="!isProcessing" ref="stripeContainer" class="stripe-mount"></div>

        <div v-if="errorMessage" class="pay-error">
          {{ errorMessage }}
          <button class="retry-btn" @click="mountStripeElements">Tentar novamente</button>
        </div>
      </div>

      <!-- Trust Indicators -->
      <div class="trust-row">
        <Shield :size="14" />
        <span>Pagamento 100% seguro e encriptado. Compativel com PCI-DSS.</span>
      </div>

      <!-- CTA -->
      <div class="checkout-footer">
        <button v-if="!errorMessage" class="cta-btn" :disabled="isProcessing" @click="pay">
          <Loader2 v-if="isProcessing" class="spin" :size="20" />
          <template v-else>
            <Shield :size="16" />
            {{ ctaLabel }}
          </template>
        </button>
      </div>
    </div>

    <!-- Step: Processing -->
    <div v-if="step === 'processing'" class="checkout-body processing-state fade-up">
      <div class="proc-spinner"><Loader2 class="spin" :size="48" /></div>
      <h2>A processar pagamento</h2>
      <p>Nao feche esta pagina...</p>
    </div>

    <!-- Step: Success -->
    <div v-if="step === 'success'" class="checkout-body success-state fade-up">
      <div class="success-ring">
        <CheckCircle :size="56" />
      </div>
      <h2>Pagamento Concluido</h2>
      <p>O seu <strong>{{ selected?.name }}</strong> foi adicionado ao perfil e esta pronto a usar.</p>

      <div class="success-receipt">
        <div class="sr-row"><span>Titulo</span><strong>{{ selected?.name }}</strong></div>
        <div class="sr-row"><span>Valor</span><strong>{{ formattedPrice }}</strong></div>
        <div class="sr-row"><span>Estado</span><strong class="sr-active">Ativo</strong></div>
      </div>

      <div class="checkout-footer">
        <button class="cta-btn" @click="router.push('/app/ticket')">Ver o meu Bilhete</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ── Page Shell ──────────────────────────────────── */
.checkout-page {
  display: flex;
  flex-direction: column;
  min-height: 100dvh;
  background: var(--bg-primary);
  position: relative;
}

/* ── Header ──────────────────────────────────────── */
.checkout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px 12px;
  position: sticky;
  top: 0;
  z-index: 10;
  background: var(--bg-primary);
}
.header-back {
  width: 40px; height: 40px;
  display: flex; align-items: center; justify-content: center;
  background: var(--bg-surface);
  border: 1px solid var(--border-light);
  border-radius: 12px;
  color: var(--text-main);
  cursor: pointer;
  transition: all 0.2s;
}
.header-back:active { transform: scale(0.92); }
.header-center h1 { font-size: 1.125rem; font-weight: 700; margin: 0; }
.header-spacer { width: 40px; }

/* ── Body ────────────────────────────────────────── */
.checkout-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 0 20px 24px;
  gap: 20px;
}

/* ── Animations ──────────────────────────────────── */
.fade-up {
  animation: fadeUp 0.35s cubic-bezier(0.16, 1, 0.3, 1) forwards;
}
@keyframes fadeUp {
  from { opacity: 0; transform: translateY(12px); }
  to   { opacity: 1; transform: translateY(0); }
}
.spin { animation: spin360 0.8s linear infinite; }
@keyframes spin360 { to { transform: rotate(360deg); } }

/* ── Section Label ───────────────────────────────── */
.section-label {
  font-size: 0.8rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--text-muted);
  margin: 4px 0 0;
}

/* ── Ticket Cards ────────────────────────────────── */
.ticket-cards { display: flex; flex-direction: column; gap: 14px; }

.ticket-card {
  position: relative;
  display: grid;
  grid-template-columns: auto 1fr auto;
  grid-template-rows: auto auto;
  gap: 2px 14px;
  padding: 20px;
  background: var(--bg-surface);
  border: 2px solid var(--border-light);
  border-radius: 16px;
  cursor: pointer;
  text-align: left;
  transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
  -webkit-tap-highlight-color: transparent;
}
.ticket-card:active { transform: scale(0.98); }
.ticket-card.active {
  border-color: var(--accent-blue);
  background: var(--bg-surface);
  box-shadow: 0 0 0 3px var(--border-focus), var(--shadow-glow);
}

.tc-badge {
  position: absolute;
  top: -9px; right: 16px;
  padding: 2px 10px;
  font-size: 0.65rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  border-radius: 6px;
  background: var(--bg-hover-strong);
  color: var(--text-muted);
}
.tc-badge.popular {
  background: var(--accent-blue);
  color: #fff;
}
.tc-icon {
  grid-row: 1 / 3;
  display: flex; align-items: center; justify-content: center;
  width: 48px; height: 48px;
  border-radius: 14px;
  background: var(--bg-hover);
  color: var(--accent-blue);
}
.tc-name { font-size: 1rem; font-weight: 700; color: var(--text-main); align-self: end; }
.tc-desc { font-size: 0.78rem; color: var(--text-muted); line-height: 1.35; }
.tc-price {
  grid-row: 1 / 3;
  align-self: center;
  font-size: 1.35rem;
  font-weight: 800;
  color: var(--text-main);
  font-variant-numeric: tabular-nums;
}
.tc-price span {
  font-size: 0.65rem;
  font-weight: 600;
  color: var(--text-muted);
  margin-left: 3px;
  vertical-align: super;
}
.tc-radio { display: none; }

/* ── Order Summary ───────────────────────────────── */
.order-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 18px;
  background: var(--bg-surface);
  border: 1px solid var(--border-light);
  border-radius: 14px;
}
.os-left { display: flex; align-items: center; gap: 12px; }
.os-icon { color: var(--accent-blue); flex-shrink: 0; }
.os-name { font-size: 0.95rem; font-weight: 700; color: var(--text-main); }
.os-desc { font-size: 0.75rem; color: var(--text-muted); margin-top: 1px; }
.os-price { font-size: 1.1rem; font-weight: 800; color: var(--text-main); white-space: nowrap; }

/* ── Payment Card ────────────────────────────────── */
.payment-card {
  background: var(--bg-surface);
  border: 1px solid var(--border-light);
  border-radius: 16px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.pc-title {
  display: flex; align-items: center; gap: 8px;
  font-size: 0.95rem; font-weight: 700;
  color: var(--text-main);
}
.stripe-mount { min-height: 120px; }
.stripe-loading {
  display: flex; flex-direction: column; align-items: center;
  gap: 12px; padding: 32px 0;
  color: var(--text-muted); font-size: 0.85rem;
}
.pay-error {
  padding: 16px;
  border-radius: 10px;
  background: rgba(239, 68, 68, 0.08);
  border: 1px solid rgba(239, 68, 68, 0.2);
  color: var(--danger);
  font-size: 0.85rem;
  text-align: center;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.retry-btn {
  padding: 10px 20px;
  border: 1px solid var(--border-light);
  border-radius: 10px;
  background: var(--bg-surface);
  color: var(--text-main);
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}
.retry-btn:active { transform: scale(0.97); }

/* ── Trust Row ───────────────────────────────────── */
.trust-row {
  display: flex; align-items: center; justify-content: center;
  gap: 6px;
  font-size: 0.72rem;
  color: var(--text-muted);
  opacity: 0.7;
  text-align: center;
}

/* ── CTA Footer ──────────────────────────────────── */
.checkout-footer {
  margin-top: auto;
  padding-top: 8px;
  position: sticky;
  bottom: 0;
  padding-bottom: env(safe-area-inset-bottom, 8px);
  background: linear-gradient(to top, var(--bg-primary) 80%, transparent);
}
.cta-btn {
  width: 100%;
  display: flex; align-items: center; justify-content: center; gap: 8px;
  padding: 16px 24px;
  font-size: 1rem;
  font-weight: 700;
  color: #fff;
  background: var(--accent-blue);
  border: none;
  border-radius: 14px;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
  box-shadow: 0 4px 20px -4px rgba(6, 182, 212, 0.4);
  -webkit-tap-highlight-color: transparent;
  position: relative;
  overflow: hidden;
}
.cta-btn::after {
  content: '';
  position: absolute; inset: 0;
  background: linear-gradient(135deg, rgba(255,255,255,0.12) 0%, transparent 60%);
  pointer-events: none;
}
.cta-btn:active:not(:disabled) { transform: scale(0.97); }
.cta-btn:disabled { opacity: 0.6; cursor: not-allowed; }
.cta-price { opacity: 0.85; font-weight: 500; margin-left: 2px; }

/* ── Processing State ────────────────────────────── */
.processing-state {
  align-items: center; justify-content: center; text-align: center;
  padding-top: 20vh;
}
.proc-spinner { color: var(--accent-blue); margin-bottom: 20px; }
.processing-state h2 { font-size: 1.25rem; margin-bottom: 8px; }
.processing-state p { color: var(--text-muted); font-size: 0.9rem; }

/* ── Success State ───────────────────────────────── */
.success-state {
  align-items: center; text-align: center;
  padding-top: 10vh;
}
.success-ring {
  width: 88px; height: 88px;
  display: flex; align-items: center; justify-content: center;
  border-radius: 50%;
  background: rgba(16, 185, 129, 0.1);
  color: var(--success);
  margin-bottom: 20px;
  animation: popIn 0.5s cubic-bezier(0.175, 0.885, 0.32, 1.275) forwards;
}
@keyframes popIn {
  0%   { transform: scale(0); opacity: 0; }
  100% { transform: scale(1); opacity: 1; }
}
.success-state h2 { font-size: 1.35rem; margin-bottom: 8px; }
.success-state p { color: var(--text-muted); line-height: 1.5; max-width: 300px; margin-bottom: 24px; }

.success-receipt {
  width: 100%;
  max-width: 320px;
  background: var(--bg-surface);
  border: 1px solid var(--border-light);
  border-radius: 14px;
  padding: 16px 20px;
  margin-bottom: 8px;
}
.sr-row {
  display: flex; justify-content: space-between; align-items: center;
  padding: 8px 0;
  font-size: 0.85rem;
  color: var(--text-muted);
  border-bottom: 1px solid var(--border-light);
}
.sr-row:last-child { border-bottom: none; }
.sr-row strong { color: var(--text-main); }
.sr-active { color: var(--success) !important; }
</style>
