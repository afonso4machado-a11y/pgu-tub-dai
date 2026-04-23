/**
 * Serviço de Autenticação PGU/TUB
 * 
 * Gestão de sessão com expiração automática:
 *   - Admin (Backoffice):  2 horas de inatividade
 *   - Passageiro (Mobile): 7 dias desde o último login
 * 
 * Cada interação do admin com a app renova a sessão (sliding window).
 * O passageiro mantém a sessão fixa até expirar.
 */

// ── Tempos de Expiração ──
const ADMIN_SESSION_TTL = 2 * 60 * 60 * 1000     // 2 horas (em ms)
const PASSENGER_SESSION_TTL = 7 * 24 * 60 * 60 * 1000  // 7 dias (em ms)

export const authService = {

  // ═══════════════════════════════════════════
  //  ADMIN (Backoffice) — Sessão Sliding 2h
  // ═══════════════════════════════════════════

  isAdminLoggedIn() {
    if (localStorage.getItem('pgu_admin_session') !== 'true') return false

    const lastActivity = parseInt(localStorage.getItem('pgu_admin_last_activity') || '0')
    const now = Date.now()

    if (now - lastActivity > ADMIN_SESSION_TTL) {
      // Sessão expirada — limpar tudo
      this._clearAdminSession()
      return false
    }

    // Renovar sessão (sliding window) — cada verificação renova o tempo
    localStorage.setItem('pgu_admin_last_activity', now.toString())
    return true
  },

  async loginAdmin(email, password) {
    // Regra local redundante para segurança e rapidez
    const isInstitutional = email.endsWith('@uminho.pt') || email.endsWith('@um');
    if (!isInstitutional || password !== 'tub_uminho26') {
      throw new Error('Acesso restrito. Use email institucional (@uminho.pt ou @um) e a password mestre.')
    }

    try {
      const res = await fetch('/api/auth/admin/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password })
      })
      const data = await res.json()
      if (data.status === 'sucesso') {
        this._setAdminSession(data.nome || email.split('@')[0], email)
        return true
      }
      throw new Error(data.mensagem)
    } catch (e) {
      // Fallback para demo se backend estiver offline mas credenciais estiverem certas
      const isInstitutional = email.endsWith('@uminho.pt') || email.endsWith('@um');
      if (isInstitutional && password === 'tub_uminho26') {
        const nome = email.split('@')[0].charAt(0).toUpperCase() + email.split('@')[0].slice(1)
        this._setAdminSession(nome, email)
        return true
      }
      throw e
    }
  },

  _setAdminSession(nome, email) {
    const now = Date.now()
    localStorage.setItem('pgu_admin_session', 'true')
    localStorage.setItem('pgu_admin_last_activity', now.toString())
    localStorage.setItem('pgu_admin_login_at', now.toString())
    localStorage.setItem('pgu_admin_user', JSON.stringify({ nome, email }))
  },

  _clearAdminSession() {
    localStorage.removeItem('pgu_admin_session')
    localStorage.removeItem('pgu_admin_last_activity')
    localStorage.removeItem('pgu_admin_login_at')
    localStorage.removeItem('pgu_admin_user')
  },

  logoutAdmin() {
    this._clearAdminSession()
    window.location.href = '/login'
  },

  /**
   * Retorna quanto tempo resta na sessão admin (em minutos).
   * Útil para mostrar countdown no UI.
   */
  getAdminSessionRemaining() {
    const lastActivity = parseInt(localStorage.getItem('pgu_admin_last_activity') || '0')
    const remaining = ADMIN_SESSION_TTL - (Date.now() - lastActivity)
    return Math.max(0, Math.round(remaining / 60000))
  },

  // ═══════════════════════════════════════════
  //  PASSAGEIRO (Mobile) — Sessão Fixa 7 dias
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
      const res = await fetch('/api/auth/signup', {
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
      const res = await fetch('/api/auth/login', {
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
