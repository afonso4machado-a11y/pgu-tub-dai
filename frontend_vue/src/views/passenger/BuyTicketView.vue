<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { loadStripe } from '@stripe/stripe-js'
import { Ticket, ArrowLeft, CreditCard, CheckCircle, Loader2 } from 'lucide-vue-next'
import { authService } from '../../services/auth'

const router = useRouter()
const user = authService.getUser()

// Step control: 1 = Select Ticket, 2 = Payment, 3 = Success
const currentStep = ref(1)

const ticketTypes = [
  { id: 'simples', name: 'Bilhete Simples', price: 1.55, description: 'Válido para 1 viagem em toda a rede TUB.' },
  { id: 'passe', name: 'Passe Mensal', price: 30.00, description: 'Válido para viagens ilimitadas durante 30 dias.' }
]

const selectedTicket = ref(ticketTypes[0].id)
const isProcessing = ref(false)
const errorMessage = ref('')

// Stripe Elements
let stripe = null
let elements = null
const paymentElementContainer = ref(null)

onMounted(async () => {
  // Substituir por uma chave pública real nas Variáveis de Ambiente no futuro
  stripe = await loadStripe('pk_test_TYooMQauvdEDq54NiTphI7jx')
})

const goBack = () => {
  if (currentStep.value === 2) {
    currentStep.value = 1
  } else {
    router.push('/app/ticket')
  }
}

const proceedToPayment = async () => {
  if (!stripe) return
  currentStep.value = 2
  isProcessing.value = true
  errorMessage.value = ''

  try {
    // 1. O ideal seria: await fetch('/api/payments/create-intent', ...)
    // Como a API real não existe (está documentada no BACKEND_INSTRUCTIONS.md), usamos
    // modo elements client-side-only mode de demonstração apenas para inicializar a UI com Stripe Elements Real
    // Em produção, deve ser substituído pelo clientSecret do backend
    elements = stripe.elements({
      mode: 'payment',
      amount: Math.round(ticketTypes.find(t => t.id === selectedTicket.value)?.price * 100),
      currency: 'eur',
      appearance: { theme: 'flat' }
    })

    const paymentElement = elements.create('payment')

    // Aguardamos que o elemento seja montado no DOM e fazemos mount()
    setTimeout(() => {
      if (paymentElementContainer.value) {
        paymentElement.mount(paymentElementContainer.value)
      }
    }, 100)

    isProcessing.value = false

  } catch (error) {
    errorMessage.value = 'Falha ao inicializar Stripe. Tente novamente.'
    isProcessing.value = false
  }
}

const confirmPayment = async () => {
  if (!stripe || !elements) return

  isProcessing.value = true
  errorMessage.value = ''

  try {
    // Opcional: Submeter os elementos se as validações the UI estiverem ativas (Stripe lida com isto)
    const { error: submitError } = await elements.submit()
    if (submitError) {
      errorMessage.value = submitError.message
      isProcessing.value = false
      return
    }

    // Em produção, chamaria stripe.confirmPayment() enviando o secret retornado do backend
    // Como o backend ainda não processa, simulamos o tempo que demoraria
    await new Promise(r => setTimeout(r, 2000))

    currentStep.value = 3

  } catch (error) {
    errorMessage.value = 'Falha na comunicação com o provedor de pagamento.'
  } finally {
    isProcessing.value = false
  }
}

const finishPurchase = () => {
  router.push('/app/ticket')
}
</script>

<template>
  <div class="buy-ticket-page">
    <div class="top-nav">
      <button v-if="currentStep < 3" @click="goBack" class="back-btn" aria-label="Voltar">
        <ArrowLeft :size="24" />
      </button>
      <h1 class="page-title">Comprar Bilhete</h1>
    </div>

    <!-- Step 1: Selection -->
    <div v-if="currentStep === 1" class="step-container">
      <p class="instruction">Selecione o tipo de bilhete pretendido:</p>

      <div class="ticket-options">
        <div
          v-for="ticket in ticketTypes"
          :key="ticket.id"
          class="ticket-option"
          :class="{ 'selected': selectedTicket === ticket.id }"
          @click="selectedTicket = ticket.id"
        >
          <div class="ticket-info">
            <Ticket :size="24" class="ticket-icon" />
            <div class="ticket-text">
              <h3>{{ ticket.name }}</h3>
              <p>{{ ticket.description }}</p>
            </div>
          </div>
          <div class="ticket-price">
            {{ ticket.price.toFixed(2).replace('.', ',') }}€
          </div>
        </div>
      </div>

      <button class="action-btn primary mt-auto" @click="proceedToPayment">
        Avançar para Pagamento
      </button>
    </div>

    <!-- Step 2: Payment -->
    <div v-if="currentStep === 2" class="step-container">
      <div class="summary-card">
        <h3>Resumo da Compra</h3>
        <div class="summary-row">
          <span>{{ ticketTypes.find(t => t.id === selectedTicket)?.name }}</span>
          <strong>{{ ticketTypes.find(t => t.id === selectedTicket)?.price.toFixed(2).replace('.', ',') }}€</strong>
        </div>
      </div>

      <div class="payment-section">
        <h3 class="payment-title"><CreditCard :size="18" /> Dados de Pagamento</h3>

        <div v-if="isProcessing && errorMessage === ''" class="loading-state">
          <Loader2 class="spinner" :size="32" />
          <p>A preparar pagamento seguro...</p>
        </div>

        <div v-show="!isProcessing && errorMessage === ''" class="stripe-payment-form">
          <!-- Stripe Elements Injection Point -->
          <div ref="paymentElementContainer" id="payment-element" class="demo-card-input"></div>
          <p class="stripe-badge">Pagamento Seguro gerido por Stripe</p>
        </div>

        <div v-if="errorMessage" class="error-msg">{{ errorMessage }}</div>
      </div>

      <button
        class="action-btn primary mt-auto"
        :disabled="isProcessing"
        @click="confirmPayment"
      >
        <Loader2 v-if="isProcessing" class="spinner" :size="18" />
        <span v-else>Pagar {{ ticketTypes.find(t => t.id === selectedTicket)?.price.toFixed(2).replace('.', ',') }}€</span>
      </button>
    </div>

    <!-- Step 3: Success -->
    <div v-if="currentStep === 3" class="step-container success-state">
      <div class="success-icon-wrapper">
        <CheckCircle :size="64" class="success-icon" />
      </div>
      <h2>Pagamento Concluído!</h2>
      <p>O seu <strong>{{ ticketTypes.find(t => t.id === selectedTicket)?.name }}</strong> foi adicionado ao seu perfil e está pronto a ser utilizado.</p>

      <button class="action-btn primary mt-auto" @click="finishPurchase">
        Ver o meu Bilhete
      </button>
    </div>
  </div>
</template>

<style scoped>
.buy-ticket-page {
  padding: 1.25rem;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.top-nav {
  display: flex;
  align-items: center;
  margin-bottom: 2rem;
  gap: 1rem;
}

.back-btn {
  background: none;
  border: none;
  color: var(--text-main);
  padding: 0.5rem;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: background 0.2s;
}

.back-btn:active {
  background: var(--bg-hover);
}

.page-title {
  font-size: 1.5rem;
  font-weight: 700;
  margin: 0;
  color: var(--text-main);
}

.step-container {
  display: flex;
  flex-direction: column;
  flex: 1;
  gap: 1.5rem;
}

.instruction {
  color: var(--text-muted);
  font-size: 0.95rem;
  margin: 0;
}

.ticket-options {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.ticket-option {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.25rem;
  background: var(--bg-surface);
  border: 2px solid var(--border-light);
  border-radius: 1rem;
  cursor: pointer;
  transition: all 0.2s;
}

.ticket-option.selected {
  border-color: var(--accent-blue);
  background: rgba(2, 132, 199, 0.05);
}

.ticket-info {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.ticket-icon {
  color: var(--accent-blue);
}

.ticket-text h3 {
  margin: 0 0 0.25rem 0;
  font-size: 1.1rem;
  color: var(--text-main);
}

.ticket-text p {
  margin: 0;
  font-size: 0.8rem;
  color: var(--text-muted);
  max-width: 200px;
}

.ticket-price {
  font-weight: 800;
  font-size: 1.2rem;
  color: var(--text-main);
}

.mt-auto {
  margin-top: auto;
}

.action-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 1.15rem;
  border-radius: 1rem;
  font-weight: 700;
  font-size: 1.05rem;
  border: none;
  cursor: pointer;
  transition: all 0.15s;
  width: 100%;
}

.action-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.action-btn.primary {
  background: var(--accent-blue);
  color: #fff;
  box-shadow: 0 4px 16px rgba(2,132,199,0.3);
}

.action-btn:active:not(:disabled) {
  transform: scale(0.98);
}

/* Payment Step */
.summary-card {
  background: var(--bg-surface);
  padding: 1.25rem;
  border-radius: 1rem;
  border: 1px solid var(--border-light);
}

.summary-card h3 {
  margin: 0 0 1rem 0;
  font-size: 1rem;
  color: var(--text-muted);
}

.summary-row {
  display: flex;
  justify-content: space-between;
  font-size: 1.1rem;
  color: var(--text-main);
}

.payment-section {
  flex: 1;
}

.payment-title {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 1.1rem;
  margin: 0 0 1rem 0;
  color: var(--text-main);
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 2rem 0;
  color: var(--text-muted);
  gap: 1rem;
}

.spinner {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.demo-card-input {
  background: var(--bg-surface);
  padding: 1.5rem;
  border-radius: 1rem;
  border: 1px solid var(--border-light);
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.input-group label {
  display: block;
  font-size: 0.8rem;
  color: var(--text-muted);
  margin-bottom: 0.5rem;
}

.fake-input {
  background: var(--bg-primary);
  padding: 0.875rem 1rem;
  border-radius: 0.5rem;
  font-family: monospace;
  font-size: 1.1rem;
  color: var(--text-main);
  border: 1px solid var(--border-light);
}

.input-row {
  display: flex;
  gap: 1rem;
}

.input-row .input-group {
  flex: 1;
}

.stripe-badge {
  text-align: center;
  font-size: 0.75rem;
  color: var(--text-muted);
  margin-top: 1rem;
}

.error-msg {
  color: #ef4444;
  font-size: 0.9rem;
  text-align: center;
  margin-top: 1rem;
  padding: 0.75rem;
  background: rgba(239, 68, 68, 0.1);
  border-radius: 0.5rem;
}

/* Success Step */
.success-state {
  align-items: center;
  justify-content: center;
  text-align: center;
  padding-top: 2rem;
}

.success-icon-wrapper {
  color: #10b981;
  margin-bottom: 1rem;
  animation: scaleIn 0.5s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

@keyframes scaleIn {
  0% { transform: scale(0); opacity: 0; }
  100% { transform: scale(1); opacity: 1; }
}

.success-state h2 {
  margin: 0 0 1rem 0;
  color: var(--text-main);
}

.success-state p {
  color: var(--text-muted);
  line-height: 1.5;
  margin: 0;
}
</style>
