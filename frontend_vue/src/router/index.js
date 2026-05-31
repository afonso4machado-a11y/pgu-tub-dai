import { createRouter, createWebHistory } from 'vue-router'

// ── Operations Dashboard (Backoffice) ──
import DashboardView from '../views/DashboardView.vue'
import FleetView from '../views/FleetView.vue'
import PTNetworkView from '../views/PTNetworkView.vue'
import TicketingView from '../views/TicketingView.vue'
import OccupancyView from '../views/OccupancyView.vue'
import CorrelationView from '../views/CorrelationView.vue'
import AlertsCenterView from '../views/AlertsCenterView.vue'
import HistoricoView from '../views/admin/HistoricoView.vue'
import NotFoundView from '../views/NotFoundView.vue'
import ValidadorView from '../views/ValidadorView.vue'

// ── Passenger App (Mobile PWA) ──
import PassengerApp from '../layouts/PassengerApp.vue'
import PaxHome from '../views/passenger/HomeView.vue'
import PaxMap from '../views/passenger/LiveMapView.vue'
import PaxTicket from '../views/passenger/TicketView.vue'
import PaxAlerts from '../views/passenger/AlertsView.vue'
import PaxProfile from '../views/passenger/ProfileView.vue'
import BuyTicketView from '../views/passenger/BuyTicketView.vue'
import HorariosView from '../views/HorariosView.vue'

import AdminLogin from '../views/admin/LoginView.vue'
import PaxLogin from '../views/passenger/LoginView.vue'
import { authService } from '../services/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    // ═══ AUTH ROUTES ═══
    {
      path: '/login',
      name: 'admin-login',
      component: AdminLogin
    },
    {
      path: '/app/login',
      name: 'pax-login',
      component: PaxLogin
    },

    // ═══ BACKOFFICE ROUTES ═══
    {
      path: '/',
      name: 'dashboard',
      component: DashboardView,
      meta: { requiresAdmin: true }
    },
    {
      path: '/fleet',
      name: 'fleet',
      component: FleetView,
      meta: { requiresAdmin: true }
    },
    {
      path: '/network',
      name: 'network',
      component: PTNetworkView,
      meta: { requiresAdmin: true }
    },
    {
      path: '/ticketing',
      name: 'ticketing',
      component: TicketingView,
      meta: { requiresAdmin: true }
    },
    {
      path: '/occupancy',
      name: 'occupancy',
      component: OccupancyView,
      meta: { requiresAdmin: true }
    },
    {
      path: '/correlation',
      name: 'correlation',
      component: CorrelationView,
      meta: { requiresAdmin: true }
    },
    {
      path: '/alerts',
      name: 'alerts',
      component: AlertsCenterView,
      meta: { requiresAdmin: true }
    },
    {
      path: '/validador',
      name: 'validador',
      component: ValidadorView,
      meta: { requiresAdmin: true }
    },
    {
      path: '/historico',
      name: 'historico',
      component: HistoricoView,
      meta: { requiresAdmin: true }
    },

    // ═══ PASSENGER APP ROUTES ═══
    {
      path: '/app',
      component: PassengerApp,
      meta: { requiresUser: true },
      children: [
        { path: '', name: 'pax-home', component: PaxHome },
        { path: 'map', name: 'pax-map', component: PaxMap },
        { path: 'ticket', name: 'pax-ticket', component: PaxTicket },
        { path: 'buy-ticket', name: 'pax-buy-ticket', component: BuyTicketView },
        { path: 'alerts', name: 'pax-alerts', component: PaxAlerts },
        { path: 'horarios', name: 'pax-horarios', component: HorariosView },
        { path: 'profile', name: 'pax-profile', component: PaxProfile },
      ]
    },

    // ═══ 404 CATCH-ALL ═══
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: NotFoundView
    }
  ]
})

router.beforeEach((to, from, next) => {
  // Admin Guard — usa sessionStorage, expira ao fechar o browser/tab
  if (to.meta.requiresAdmin && !authService.isAdminLoggedIn()) {
    next('/login')
    return
  }

  // Passenger Guard — expira após 7 dias
  if (to.meta.requiresUser) {
    if (!authService.getUser()) {
      const hadSession = localStorage.getItem('pgu_user_login_at')
      next({ path: '/app/login', query: hadSession ? { reason: 'expired' } : {} })
      return
    }
  }

  next()
})

export default router
