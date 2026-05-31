<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { authService } from '../../services/auth'

const router = useRouter()
const route = useRoute()
const isSignup = ref(true)
const nome = ref('')
const email = ref('')
const password = ref('')
const error = ref('')
const loading = ref(false)
const sessionExpired = ref(false)

const lockoutRemaining = ref(0)
const lockoutInterval = ref(null)

function formatTime(sec) {
  const m = Math.floor(sec / 60)
  const s = sec % 60
  return `${m}:${s < 10 ? '0' : ''}${s}`
}

function checkLockout() {
  const lockoutUntil = localStorage.getItem('pgu_login_lockout_until')
  if (lockoutUntil) {
    const remaining = Math.ceil((parseInt(lockoutUntil) - Date.now()) / 1000)
    if (remaining > 0) {
      lockoutRemaining.value = remaining
      error.value = 'Demasiadas tentativas de acesso. Espere um pouco para tentar novamente.'
      
      if (!lockoutInterval.value) {
        lockoutInterval.value = setInterval(() => {
          const rem = Math.ceil((parseInt(lockoutUntil) - Date.now()) / 1000)
          if (rem <= 0) {
            lockoutRemaining.value = 0
            error.value = ''
            clearInterval(lockoutInterval.value)
            lockoutInterval.value = null
          } else {
            lockoutRemaining.value = rem
          }
        }, 1000)
      }
      return true
    }
  }
  lockoutRemaining.value = 0
  return false
}

onMounted(() => {
  if (route.query.reason === 'expired') {
    sessionExpired.value = true
    isSignup.value = false // Mostrar login em vez de signup
    localStorage.removeItem('pgu_user_login_at')
  }
  checkLockout()
})

onUnmounted(() => {
  if (lockoutInterval.value) {
    clearInterval(lockoutInterval.value)
  }
})

async function handleSubmit() {
  if (checkLockout()) {
    return
  }

  error.value = ''
  sessionExpired.value = false
  loading.value = true
  
  try {
    if (isSignup.value) {
      await authService.signupPassenger(nome.value, email.value, password.value)
    } else {
      await authService.loginPassenger(email.value, password.value)
    }
    router.push('/app')
  } catch (e) {
    error.value = e.message || 'Falha na autenticação. Tente novamente.'
    if (e.message && (e.message.includes('Demasiadas') || e.message.includes('Múltiplas') || e.message.includes('Zero-Trust'))) {
      localStorage.setItem('pgu_login_lockout_until', (Date.now() + 5 * 60 * 1000).toString())
      checkLockout()
    }
  } finally {
    loading.value = false
  }
}
</script>

<template>
 <div class="mobile-login">
 <div class="header">
 <h1>Bem-vindo à PGU</h1>
 <p>{{ isSignup ? 'Cria o teu perfil para uma viagem personalizada' : 'Inicia sessão para continuar' }}</p>
 </div>

 <div class="form-container">
 <!-- Sessão expirada banner -->
 <div v-if="sessionExpired" class="expired-banner">
 <span>A tua sessão expirou (7 dias). Entra novamente para continuar.</span>
 </div>

 <div class="onboarding-header">
 <h2 v-if="isSignup">Cria o teu Perfil</h2>
 <h2 v-else>Entrar na Conta</h2>
 <p v-if="isSignup">Diz-nos como te chamas para personalizarmos a tua experiência.</p>
 </div>

 <form @submit.prevent="handleSubmit">
 <div v-if="isSignup" class="input-field">
 <input type="text" v-model="nome" placeholder="Seu Nome" required />
 </div>

 <div class="input-field">
 <input type="email" v-model="email" placeholder="Email" required />
 </div>

 <div class="input-field">
 <input type="password" v-model="password" placeholder="Palavra-passe" required />
 </div>

 <div v-if="error" class="error-msg">{{ error }}</div>

 <button type="submit" class="submit-btn" :disabled="loading || lockoutRemaining > 0">
 {{ lockoutRemaining > 0 ? `Bloqueado (${formatTime(lockoutRemaining)})` : (loading ? 'A processar...' : (isSignup ? 'Criar Perfil' : 'Entrar')) }}
 </button>

 <div class="switch-mode">
 {{ isSignup ? 'Já tens conta?' : 'Ainda não tens perfil?' }}
 <a href="#" @click.prevent="isSignup = !isSignup">
 {{ isSignup ? 'Entrar aqui' : 'Criar um agora' }}
 </a>
 </div>
 </form>
 </div>
 </div>
</template>

<style scoped>
.mobile-login {
 height: 100vh;
 height: 100dvh;
 width: 100%;
 max-width: 430px;
 margin: 0 auto;
 overflow-y: auto;
 -webkit-overflow-scrolling: touch;
 background: var(--bg-primary);
 display: flex;
 flex-direction: column;
 padding: calc(2rem + env(safe-area-inset-top)) 2rem calc(2rem + env(safe-area-inset-bottom));
 color: var(--text-main);
}

.header {
 text-align: center;
 margin-top: 3rem;
 margin-bottom: 3rem;
}

.logo-icon {
 width: 80px;
 height: 80px;
 background: white;
 border-radius: 20px;
 display: flex;
 align-items: center;
 justify-content: center;
 margin: 0 auto 1.5rem;
 box-shadow: 0 10px 25px -5px rgba(59, 130, 246, 0.1);
}

h1 {
 font-size: 1.75rem;
 font-weight: 800;
 margin-bottom: 0.5rem;
 color: var(--text-main);
}

p {
 color: var(--text-muted);
 font-size: 0.95rem;
}

.form-container {
 background: white;
 border-radius: 24px;
 padding: 1.5rem;
 box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
}

.onboarding-header {
 text-align: center;
 margin-bottom: 2rem;
}

.onboarding-header h2 {
 font-size: 1.5rem;
 font-weight: 800;
 color: var(--text-main);
 margin-bottom: 0.5rem;
}

.onboarding-header p {
 color: var(--text-muted);
 font-size: 0.9rem;
 line-height: 1.4;
}

.switch-mode {
 text-align: center;
 margin-top: 1.5rem;
 font-size: 0.9rem;
 color: var(--text-muted);
}

.switch-mode a {
 color: #3b82f6;
 font-weight: 700;
 text-decoration: none;
 margin-left: 0.25rem;
}

.input-field {
 display: flex;
 align-items: center;
 gap: 1rem;
 background: var(--bg-primary);
 padding: 1rem;
 border-radius: 12px;
 margin-bottom: 1rem;
 border: 1px solid var(--border-light);
}

.input-field svg {
 color: var(--text-muted);
}

input {
 border: none;
 background: transparent;
 width: 100%;
 font-size: 1rem;
 outline: none;
 color: var(--text-main);
}

.error-msg {
 color: #ef4444;
 font-size: 0.85rem;
 margin-bottom: 1rem;
 text-align: center;
}

.submit-btn {
 width: 100%;
 background: #3b82f6;
 color: white;
 border: none;
 border-radius: 12px;
 padding: 1rem;
 font-size: 1rem;
 font-weight: 700;
 display: flex;
 align-items: center;
 justify-content: center;
 gap: 0.75rem;
 margin-top: 1rem;
 box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

.submit-btn:disabled {
 opacity: 0.7;
}

.expired-banner {
 background: #fef3c7;
 border: 1px solid #fde68a;
 color: #92400e;
 padding: 0.85rem 1rem;
 border-radius: 12px;
 font-size: 0.85rem;
 display: flex;
 align-items: center;
 gap: 0.6rem;
 margin-bottom: 1.5rem;
}
</style>
