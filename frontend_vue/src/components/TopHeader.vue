<script setup>
import { Bell, ShieldCheck } from 'lucide-vue-next'
</script>

<template>
  <header class="top-header glass-panel">
    <div class="header-left">
      <h2 class="page-title title-glow">{{ $route.name ? $route.name.charAt(0).toUpperCase() + $route.name.slice(1) : '' }}</h2>
    </div>
    
    <div class="header-actions">
      <div class="status-badge">
        <ShieldCheck :size="16" class="status-icon" />
        <span class="fira-code">SISTEMA ONLINE</span>
      </div>
      
      <button class="icon-btn">
        <Bell :size="20" />
        <span class="notification-dot"></span>
      </button>
    </div>
  </header>
</template>

<style scoped>
.top-header {
  border-radius: 0;
  border-top: none;
  border-left: none;
  border-right: none;
  padding: 1.25rem 2rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  position: sticky;
  top: 0;
  z-index: 5;
}

.page-title {
  font-size: 1.5rem;
  margin: 0;
  text-transform: capitalize;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 1.5rem;
}

.status-badge {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  background: rgba(16, 185, 129, 0.1);
  color: var(--success);
  padding: 0.4rem 1rem;
  border-radius: 2rem;
  font-size: 0.75rem;
  border: 1px solid rgba(16, 185, 129, 0.2);
  box-shadow: 0 0 10px rgba(16, 185, 129, 0.1);
}

.icon-btn {
  background: transparent;
  border: none;
  color: var(--text-muted);
  cursor: pointer;
  position: relative;
  transition: color 0.2s;
  padding: 0.5rem;
}
.icon-btn:hover {
  color: var(--text-main);
}
.notification-dot {
  position: absolute;
  top: 5px;
  right: 6px;
  width: 8px;
  height: 8px;
  background: var(--danger);
  border-radius: 50%;
  box-shadow: 0 0 8px var(--danger);
}
</style>
