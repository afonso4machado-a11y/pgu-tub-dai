<script setup>
import { useRoute } from 'vue-router'

const route = useRoute()

const menuItems = [
  { id: 'dashboard', path: '/', label: 'Visão Geral' },
  { id: 'network', path: '/network', label: 'Mapa Operacional' },
  { id: 'ticketing', path: '/ticketing', label: 'Bilhética' },
  { id: 'occupancy', path: '/occupancy', label: 'Lotação' },
  { id: 'correlation', path: '/correlation', label: 'Correlação' },
  { id: 'alerts', path: '/alerts', label: 'Central Alertas' },
  { id: 'fleet', path: '/fleet', label: 'Gestão de Frota' },
  { id: 'historico', path: '/historico', label: 'Histórico' },
]
</script>

<template>
 <aside class="sidebar glass-panel">
 <div class="brand">
 <img src="/tublogo.png" alt="TUB" class="brand-logo" />
 <div class="brand-text">
 <h1 class="brand-title">TUB<span class="dot">.</span><span class="brand-light">PGU</span></h1>
 <p class="brand-subtitle fira-code">Centro de Operações</p>
 </div>
 </div>

 <nav class="nav-menu">
 <router-link 
 v-for="item in menuItems" 
 :key="item.id"
 :to="item.path"
 class="menu-item"
 :class="{ active: route.path === item.path }"
 >
 <span>{{ item.label }}</span>
 </router-link>
  </nav>

  <!-- DEV MODE SHORTCUT -->
  <div style="padding: 1.5rem; border-top: 1px solid var(--border-light); margin-top: auto;">
  <router-link to="/app" class="btn btn-primary" style="width: 100%; text-decoration: none; display: flex; align-items: center; justify-content: center; gap: 0.5rem;">
  Testar App Móvel
  </router-link>
  </div>
  </aside>
</template>

<style scoped>
.sidebar-footer {
 margin-top: auto;
 padding: 1.5rem;
 border-top: 1px solid var(--border-light);
 display: flex;
 flex-direction: column;
 gap: 0.25rem;
}
.group-tag {
 font-family: 'Fira Code', monospace;
 font-size: 0.75rem;
 font-weight: 700;
 color: var(--accent-blue);
 background: rgba(6, 182, 212, 0.1);
 padding: 0.2rem 0.5rem;
 border-radius: 4px;
 width: fit-content;
}
.uni-tag {
 font-size: 0.65rem;
 color: var(--text-muted);
 text-transform: uppercase;
 letter-spacing: 0.05em;
}
.sidebar {
  width: 280px;
  height: 100vh;
  display: flex;
  flex-direction: column;
  border-radius: 0;
  border-right: 1px solid var(--border-light);
  border-top: none;
  border-bottom: none;
  border-left: none;
  background: var(--bg-surface);
  z-index: 10;
  padding: 0;
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1), opacity 0.3s cubic-bezier(0.4, 0, 0.2, 1), border-color 0.3s ease;
  overflow: hidden;
}

.sidebar.collapsed {
  width: 0 !important;
  opacity: 0 !important;
  pointer-events: none !important;
  border-right: none !important;
}

.brand {
 padding: 2rem 1.5rem;
 display: flex;
 align-items: center;
 gap: 1rem;
 border-bottom: 1px solid var(--border-light);
}
.brand-logo {
 width: 48px;
 height: auto;
 filter: drop-shadow(0 0 8px var(--img-shadow));
}
.brand-title {
 font-size: 1.5rem;
 font-weight: 700;
 margin: 0;
}
.brand-light {
 font-weight: 300;
 color: var(--accent-blue);
}
.dot { color: var(--accent-teal); }
.brand-subtitle {
 color: var(--accent-blue);
 font-size: 0.7rem;
 letter-spacing: 0.05em;
 text-transform: uppercase;
}

.nav-menu {
 flex: 1;
 padding: 1.5rem 1rem;
 display: flex;
 flex-direction: column;
 gap: 0.5rem;
}

.menu-item {
 display: flex;
 align-items: center;
 gap: 1rem;
 padding: 0.85rem 1.25rem;
 border-radius: 0.5rem;
 color: var(--text-muted);
 text-decoration: none;
 font-weight: 500;
 font-size: 0.95rem;
 transition: all 0.2s ease;
}
.menu-item:hover {
 background: var(--bg-hover);
 color: var(--text-main);
}
.menu-item.active {
 background: rgba(6, 182, 212, 0.15);
 color: var(--accent-blue);
 border-left: 3px solid var(--accent-blue);
}
.icon { opacity: 0.8; }
.menu-item.active .icon { opacity: 1; filter: drop-shadow(0 0 8px rgba(6, 182, 212, 0.5)); }

.user-profile {
 padding: 1.5rem;
 border-top: 1px solid var(--border-light);
 display: flex;
 align-items: center;
 gap: 1rem;
 background: var(--bg-input);
}
.avatar {
 width: 40px; height: 40px;
 border-radius: 50%;
 background: linear-gradient(135deg, var(--accent-blue), var(--accent-purple));
 color: #fff;
 display: flex;
 align-items: center;
 justify-content: center;
 font-weight: 700;
 font-size: 0.9rem;
 box-shadow: 0 0 10px rgba(99, 102, 241, 0.4);
}
.user-info { display: flex; flex-direction: column; }
.user-name { font-weight: 600; font-size: 0.95rem; }
.user-role { font-size: 0.7rem; color: var(--text-muted); }
</style>
