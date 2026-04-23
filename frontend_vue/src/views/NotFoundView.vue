<script setup>
import { useRouter } from 'vue-router'
import { Home, ArrowLeft, MapPin } from 'lucide-vue-next'

const router = useRouter()
</script>

<template>
  <div class="not-found">
    <div class="nf-content">
      <div class="nf-code">404</div>
      <h2 class="nf-title">Rota não encontrada</h2>
      <p class="nf-desc">
        A página que procuras não existe ou foi movida.
        <br />Tal como um autocarro que saiu fora da rota. 🚌
      </p>
      <div class="nf-actions">
        <button class="nf-btn primary" @click="router.push('/')">
          <Home :size="18" /> Voltar ao Dashboard
        </button>
        <button class="nf-btn secondary" @click="router.back()">
          <ArrowLeft :size="18" /> Página Anterior
        </button>
        <button class="nf-btn secondary" @click="router.push('/app')">
          <MapPin :size="18" /> App Passageiro
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.not-found {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: radial-gradient(circle at top right, #0f172a, #020617);
  padding: 2rem;
}

.nf-content {
  text-align: center;
  max-width: 480px;
}

.nf-code {
  font-size: 8rem;
  font-weight: 900;
  background: linear-gradient(135deg, #06b6d4, #6366f1);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  line-height: 1;
  margin-bottom: 1rem;
  text-shadow: 0 0 40px rgba(6, 182, 212, 0.3);
}

.nf-title {
  font-size: 1.75rem;
  color: #f8fafc;
  margin-bottom: 0.75rem;
}

.nf-desc {
  color: #94a3b8;
  font-size: 1rem;
  line-height: 1.6;
  margin-bottom: 2.5rem;
}

.nf-actions {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  align-items: center;
}

.nf-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 0.85rem 1.5rem;
  border-radius: 0.75rem;
  font-weight: 600;
  font-size: 0.9rem;
  cursor: pointer;
  transition: all 0.2s ease;
  border: none;
  width: 100%;
  max-width: 280px;
}

.nf-btn.primary {
  background: linear-gradient(135deg, #06b6d4, #0284c7);
  color: #fff;
  box-shadow: 0 4px 16px rgba(6, 182, 212, 0.3);
}
.nf-btn.primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 24px rgba(6, 182, 212, 0.4);
}

.nf-btn.secondary {
  background: rgba(255, 255, 255, 0.05);
  color: #94a3b8;
  border: 1px solid rgba(255, 255, 255, 0.1);
}
.nf-btn.secondary:hover {
  background: rgba(255, 255, 255, 0.1);
  color: #f8fafc;
}
</style>
