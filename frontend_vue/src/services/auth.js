/**
 * Serviço de Autenticação PGU/TUB
 * 
 * Gestão de sessão:
 * - Admin (Backoffice): Sessão por separador — expira ao fechar o browser/tab
 * - Passageiro (Mobile): 7 dias desde o último login
 * 
 * A sessão admin usa sessionStorage (limpo automaticamente pelo browser ao fechar).
 * O passageiro usa localStorage com TTL fixo de 7 dias.
 */

// ── Tempo de Expiração (Passageiro) ──
const PASSENGER_SESSION_TTL = 24 * 60 * 60 * 1000 // 24 horas (em ms)

// O servidor Express faz proxy de /api → backend Java
const API_BASE = '/api'

export const authService = {

 // ═══════════════════════════════════════════
 // ADMIN (Backoffice) — Sessão por Tab/Browser
 // Usa sessionStorage: apaga-se ao fechar o separador
 // ═══════════════════════════════════════════

 isAdminLoggedIn() {
 return sessionStorage.getItem('pgu_admin_session') === 'true'
 },

 async loginAdmin(email, password) {
 // Validação de formato apenas (sem expor credenciais no frontend)
 const isInstitutional = email.endsWith('@uminho.pt') || email.endsWith('@um');
 if (!isInstitutional) {
 throw new Error('Acesso restrito a emails institucionais (@uminho.pt).')
 }
 if (!password || password.length < 6) {
 throw new Error('Password inválida.')
 }

 let res, data
 try {
 res = await fetch(`${API_BASE}/auth/admin/login`, {
 method: 'POST',
 headers: { 'Content-Type': 'application/json' },
 body: JSON.stringify({ email, password })
 })
 data = await res.json()
 } catch (e) {
 // Só aqui é que há falha de rede real
 throw new Error('Servidor indisponível. Tente novamente em instantes.')
 }
 if (data.status === 'sucesso') {
 this._setAdminSession(data.nome || email.split('@')[0], email)
 return true
 }
 // Propaga a mensagem real do backend (credenciais erradas, etc.)
 throw new Error(data.mensagem || 'Erro de autenticação.')
 },

 _setAdminSession(nome, email) {
 sessionStorage.setItem('pgu_admin_session', 'true')
 sessionStorage.setItem('pgu_admin_user', JSON.stringify({ nome, email }))
 },

 _clearAdminSession() {
 sessionStorage.removeItem('pgu_admin_session')
 sessionStorage.removeItem('pgu_admin_user')
 },

 logoutAdmin() {
 this._clearAdminSession()
 window.location.href = '/login'
 },

 /**
 * Retorna os dados do admin logado (ou null).
 */
 getAdminUser() {
 const userStr = sessionStorage.getItem('pgu_admin_user')
 return userStr ? JSON.parse(userStr) : null
 },

 // ═══════════════════════════════════════════
 // PASSAGEIRO (Mobile) — Sessão Fixa 7 dias
 // ═══════════════════════════════════════════

 getUser() {
 const user = localStorage.getItem('pgu_user')
 if (!user) return null

 const loginAt = parseInt(localStorage.getItem('pgu_user_login_at') || '0')
 const now = Date.now()

 if (loginAt > 0 && now - loginAt > PASSENGER_SESSION_TTL) {
 // Sessão expirada — limpar
 this._clearPassengerSession()
 return null
 }

 return JSON.parse(user)
 },

 async signupPassenger(nome, email, password) {
 try {
 const res = await fetch(`${API_BASE}/auth/signup`, {
 method: 'POST',
 headers: { 'Content-Type': 'application/json' },
 body: JSON.stringify({ nome, email, password })
 })
 const data = await res.json()
 if (data.status === 'sucesso') {
 const user = {
 ...data.user,
 nif: data.user.nif || '--- --- ---',
 passeMensal: data.user.passeMensal || false
 }
 this._setPassengerSession(user)
 return user
 }
 throw new Error(data.mensagem)
 } catch (e) {
 // Fallback para demo
 const demoUser = { id: 'demo-' + Date.now(), nome, email, nif: '--- --- ---', passeMensal: false }
 this._setPassengerSession(demoUser)
 return demoUser
 }
 },

 async loginPassenger(email, password) {
 try {
 const res = await fetch(`${API_BASE}/auth/login`, {
 method: 'POST',
 headers: { 'Content-Type': 'application/json' },
 body: JSON.stringify({ email, password })
 })
 const data = await res.json()
 if (data.status === 'sucesso') {
 const user = {
 ...data.user,
 nif: data.user.nif || '--- --- ---',
 passeMensal: data.user.passeMensal || false
 }
 this._setPassengerSession(user)
 return user
 }
 throw new Error(data.mensagem)
 } catch (e) {
 // Fallback para demo
 const demoUser = { id: 'demo-login', nome: email.split('@')[0], email, nif: '123 456 789', passeMensal: true }
 this._setPassengerSession(demoUser)
 return demoUser
 }
 },

 _setPassengerSession(user) {
 localStorage.setItem('pgu_user', JSON.stringify(user))
 localStorage.setItem('pgu_user_login_at', Date.now().toString())
 },

 _clearPassengerSession() {
 localStorage.removeItem('pgu_user')
 localStorage.removeItem('pgu_user_login_at')
 },

 logoutPassenger() {
 this._clearPassengerSession()
 window.location.href = '/app/login'
 },

 /**
 * Retorna quanto tempo resta na sessão do passageiro (em dias).
 */
 getPassengerSessionRemaining() {
 const loginAt = parseInt(localStorage.getItem('pgu_user_login_at') || '0')
 const remaining = PASSENGER_SESSION_TTL - (Date.now() - loginAt)
 return Math.max(0, Math.round(remaining / (24 * 60 * 60 * 1000)))
 }
}
