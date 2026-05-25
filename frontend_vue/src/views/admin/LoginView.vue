<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { authService } from '../../services/auth'
import { ShieldCheck, Lock, Mail, ArrowRight } from 'lucide-vue-next'

const router = useRouter()
const email = ref('')
const password = ref('')
const error = ref('')
const loading = ref(false)

const lockoutRemaining = ref(0)
const lockoutInterval = ref(null)

const isValidEmail = (e) => {
 if (!e) return false
 const normalized = e.trim().toLowerCase()
 return normalized.endsWith('@uminho.pt') || normalized.endsWith('@um')
}

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
  checkLockout()
})

onUnmounted(() => {
  if (lockoutInterval.value) {
    clearInterval(lockoutInterval.value)
  }
})

async function handleLogin() {
  if (checkLockout()) {
    return
  }

  if (!isValidEmail(email.value)) {
    error.value = 'Por favor, use um email institucional (@uminho.pt ou @um).'
    return
  }
  
  error.value = ''
  loading.value = true
  
  try {
    await authService.loginAdmin(email.value, password.value)
    router.push('/')
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
 <div class="login-page">
 <div class="login-card glass-panel fade-in">
 <div class="login-header">
 <div class="logo-circle">
 <ShieldCheck :size="32" color="#10b981" />
 </div>
 <h1 class="title-glow">TUB Backoffice</h1>
 <p class="subtitle">Acesso restrito a administradores</p>
 </div>

 <form @submit.prevent="handleLogin" class="login-form">
 <div class="input-group">
 <label><Mail :size="16" /> Email Institucional</label>
 <input 
 type="email" 
 v-model="email" 
 placeholder="exemplo@uminho.pt" 
 :class="{ 'invalid-email': email && !isValidEmail(email) }"
 required 
 />
 <span v-if="email && !isValidEmail(email)" class="hint-error">
 Apenas emails @uminho.pt ou @um são permitidos.
 </span>
 </div>

 <div class="input-group">
 <label><Lock :size="16" /> Palavra-passe</label>
 <input 
 type="password" 
 v-model="password" 
 placeholder="••••••••" 
 required 
 />
 </div>

 <div v-if="error" class="error-msg fade-in">
 {{ error }}
 </div>

 <button type="submit" class="btn-login" :disabled="loading || lockoutRemaining > 0">
    <span>{{ lockoutRemaining > 0 ? `Bloqueado (${formatTime(lockoutRemaining)})` : (loading ? 'A verificar...' : 'Entrar no Sistema') }}</span>
    <ArrowRight v-if="!loading && lockoutRemaining === 0" :size="18" />
  </button>
 </form>

 <div class="login-footer">
 <p>&copy; 2026 TUB/UMinho — Gestão de Frota</p>
 </div>
 </div>
 </div>
</template>

<style scoped>
.login-page {
 height: 100vh;
 display: flex;
 align-items: center;
 justify-content: center;
 background: var(--bg-primary);
 padding: 1.5rem;
}

.login-card {
 width: 100%;
 max-width: 420px;
 padding: 3rem 2.5rem;
 border-radius: 1.5rem;
 text-align: center;
}

.logo-circle {
 width: 64px;
 height: 64px;
 background: rgba(16, 185, 129, 0.1);
 border-radius: 50%;
 display: flex;
 align-items: center;
 justify-content: center;
 margin: 0 auto 1.5rem;
 border: 1px solid rgba(16, 185, 129, 0.2);
}

.title-glow {
 font-size: 1.75rem;
 margin-bottom: 0.5rem;
}

.subtitle {
 color: var(--text-muted);
 font-size: 0.9rem;
 margin-bottom: 2.5rem;
}

.login-form {
 display: flex;
 flex-direction: column;
 gap: 1.5rem;
 text-align: left;
}

.input-group label {
 display: flex;
 align-items: center;
 gap: 0.5rem;
 font-size: 0.85rem;
 color: var(--text-secondary);
 margin-bottom: 0.5rem;
 font-family: 'Fira Code', monospace;
}

input {
 width: 100%;
 background: var(--bg-hover);
 border: 1px solid var(--border);
 border-radius: 0.75rem;
 padding: 0.85rem 1rem;
 color: var(--text-main);
 outline: none;
 transition: border-color 0.2s;
}

input:focus {
 border-color: var(--success);
}

.invalid-email {
 border-color: var(--danger) !important;
}

.hint-error {
 font-size: 0.7rem;
 color: var(--danger);
 margin-top: 0.3rem;
 display: block;
}

.error-msg {
 background: rgba(239, 68, 68, 0.1);
 color: var(--danger);
 padding: 0.75rem;
 border-radius: 0.75rem;
 font-size: 0.85rem;
 border: 1px solid rgba(239, 68, 68, 0.2);
}

.btn-login {
 margin-top: 1rem;
 background: linear-gradient(135deg, #10b981, #059669);
 color: #052e16;
 border: none;
 border-radius: 0.75rem;
 padding: 1rem;
 font-weight: 700;
 display: flex;
 align-items: center;
 justify-content: center;
 gap: 0.75rem;
 cursor: pointer;
 transition: transform 0.2s, box-shadow 0.2s;
}

.btn-login:hover {
 transform: translateY(-2px);
 box-shadow: 0 4px 15px rgba(16, 185, 129, 0.3);
}

.btn-login:disabled {
 opacity: 0.6;
 cursor: not-allowed;
}

.login-footer {
 margin-top: 2.5rem;
 color: var(--text-muted);
 font-size: 0.75rem;
}

.expired-banner {
 background: rgba(245, 158, 11, 0.1);
 border: 1px solid rgba(245, 158, 11, 0.3);
 color: #f59e0b;
 padding: 0.85rem 1rem;
 border-radius: 0.75rem;
 font-size: 0.85rem;
 display: flex;
 align-items: center;
 gap: 0.6rem;
 margin-bottom: 1.5rem;
 text-align: left;
}
</style>
