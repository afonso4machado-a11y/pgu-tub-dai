import { createRouter, createWebHistory } from 'vue-router'

// ── Operations Dashboard (Backoffice) ──
import DashboardView from '../views/DashboardView.vue'
import FleetView from '../views/FleetView.vue'
import PTNetworkView from '../views/PTNetworkView.vue'
import TicketingView from '../views/TicketingView.vue'
import OccupancyView from '../views/OccupancyView.vue'
import CorrelationView from '../views/CorrelationView.vue'
import AlertsCenterView from '../views/AlertsCenterView.vue'

// ── Passenger App (Mobile PWA) ──
import PassengerApp from '../layouts/PassengerApp.vue'
import PaxHome from '../views/passenger/HomeView.vue'
import PaxMap from '../views/passenger/LiveMapView.vue'
import PaxTicket from '../views/passenger/TicketView.vue'
import PaxAlerts from '../views/passenger/AlertsView.vue'
import PaxProfile from '../views/passenger/ProfileView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    // ═══ BACKOFFICE ROUTES ═══
    {
      path: '/',
      name: 'dashboard',
      component: DashboardView
    },
    {
      path: '/fleet',
      name: 'fleet',
      component: FleetView
    },
    {
      path: '/network',
      name: 'network',
      component: PTNetworkView
    },
    {
      path: '/ticketing',
      name: 'ticketing',
      component: TicketingView
    },
    {
      path: '/occupancy',
      name: 'occupancy',
      component: OccupancyView
    },
    {
      path: '/correlation',
      name: 'correlation',
      component: CorrelationView
    },
    {
      path: '/alerts',
      name: 'alerts',
      component: AlertsCenterView
    },

    // ═══ PASSENGER APP ROUTES ═══
    {
      path: '/app',
      component: PassengerApp,
      children: [
        { path: '', name: 'pax-home', component: PaxHome },
        { path: 'map', name: 'pax-map', component: PaxMap },
        { path: 'ticket', name: 'pax-ticket', component: PaxTicket },
        { path: 'alerts', name: 'pax-alerts', component: PaxAlerts },
        { path: 'profile', name: 'pax-profile', component: PaxProfile },
      ]
    }
  ]
})

export default router
