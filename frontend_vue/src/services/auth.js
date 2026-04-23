export const authService = {
  // Backoffice Auth
  isAdminLoggedIn() {
    return localStorage.getItem('pgu_admin_session') === 'true'
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
        localStorage.setItem('pgu_admin_session', 'true')
        localStorage.setItem('pgu_admin_user', JSON.stringify({
          nome: data.nome || email.split('@')[0],
          email: email
        }))
        return true
      }
      throw new Error(data.mensagem)
    } catch (e) {
      // Fallback para demo se backend estiver offline mas credenciais estiverem certas
      const isInstitutional = email.endsWith('@uminho.pt') || email.endsWith('@um');
      if (isInstitutional && password === 'tub_uminho26') {
        localStorage.setItem('pgu_admin_session', 'true')
        localStorage.setItem('pgu_admin_user', JSON.stringify({
          nome: email.split('@')[0].charAt(0).toUpperCase() + email.split('@')[0].slice(1),
          email: email
        }))
        return true
      }
      throw e
    }
  },

  logoutAdmin() {
    localStorage.removeItem('pgu_admin_session')
    window.location.reload()
  },

  // Passenger Auth
  getUser() {
    const user = localStorage.getItem('pgu_user')
    return user ? JSON.parse(user) : null
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
        localStorage.setItem('pgu_user', JSON.stringify(user))
        return user
      }
      throw new Error(data.mensagem)
    } catch (e) {
      // Fallback para demo
      const demoUser = { id: 'demo-' + Date.now(), nome, email, nif: '--- --- ---', passeMensal: false }
      localStorage.setItem('pgu_user', JSON.stringify(demoUser))
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
        localStorage.setItem('pgu_user', JSON.stringify(user))
        return user
      }
      throw new Error(data.mensagem)
    } catch (e) {
      // Fallback para demo
      const demoUser = { id: 'demo-login', nome: email.split('@')[0], email, nif: '123 456 789', passeMensal: true }
      localStorage.setItem('pgu_user', JSON.stringify(demoUser))
      return demoUser
    }
  },

  logoutPassenger() {
    localStorage.removeItem('pgu_user')
    window.location.reload()
  }
}
