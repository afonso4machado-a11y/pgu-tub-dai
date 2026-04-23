/**
 * Mock API Service — Modo Demo
 * Fornece dados simulados realistas quando o backend Java não está acessível.
 * Permite que a aplicação funcione em deploy público sem servidor.
 */

const DEMO_AUTOCARROS = [
  { id: 'TUB-101', capacidadeMaxima: 80, passageirosAtuais: 34, ocupacao: 42.5, linhaId: 'L7', marca: 'Mercedes', modelo: 'Citaro', matricula: '23-AB-45', ultimaLeitura: new Date().toISOString(), totalPassageirosTransportados: 1240 },
  { id: 'TUB-102', capacidadeMaxima: 60, passageirosAtuais: 52, ocupacao: 86.7, linhaId: 'L43', marca: 'Volvo', modelo: '7900', matricula: '67-CD-89', ultimaLeitura: new Date().toISOString(), totalPassageirosTransportados: 980 },
  { id: 'TUB-103', capacidadeMaxima: 80, passageirosAtuais: 15, ocupacao: 18.8, linhaId: 'L7', marca: 'Mercedes', modelo: 'eCitaro', matricula: '10-EF-23', ultimaLeitura: new Date().toISOString(), totalPassageirosTransportados: 560 },
  { id: 'TUB-104', capacidadeMaxima: 50, passageirosAtuais: 48, ocupacao: 96.0, linhaId: 'L40', marca: 'MAN', modelo: 'Lion\'s City', matricula: '45-GH-67', ultimaLeitura: new Date().toISOString(), totalPassageirosTransportados: 2100 },
  { id: 'TUB-105', capacidadeMaxima: 70, passageirosAtuais: 28, ocupacao: 40.0, linhaId: 'L43', marca: 'Volvo', modelo: '7900E', matricula: '89-IJ-01', ultimaLeitura: new Date().toISOString(), totalPassageirosTransportados: 870 },
  { id: 'TUB-106', capacidadeMaxima: 80, passageirosAtuais: 61, ocupacao: 76.3, linhaId: 'L40', marca: 'Mercedes', modelo: 'Citaro G', matricula: '34-KL-56', ultimaLeitura: new Date().toISOString(), totalPassageirosTransportados: 1550 },
]

const DEMO_ALERTAS = [
  { tipo: 'LOTACAO_CRITICA', autocarroId: 'TUB-104', mensagem: 'Veículo TUB-104 (L40) atingiu 96% de ocupação — limiar de segurança ultrapassado.', timestamp: new Date(Date.now() - 120000).toISOString() },
  { tipo: 'LEITURA_ANOMALA_ENTRADA', autocarroId: 'TUB-102', mensagem: 'Entrada anómala de 12 passageiros em simultâneo no TUB-102 (L43) — possível evento pontual.', timestamp: new Date(Date.now() - 300000).toISOString() },
  { tipo: 'AUSENCIA_DE_LEITURAS', autocarroId: 'TUB-103', mensagem: 'Sem leituras do sensor do TUB-103 há mais de 15 minutos — verificar hardware.', timestamp: new Date(Date.now() - 900000).toISOString() },
  { tipo: 'LOTACAO_CRITICA', autocarroId: 'TUB-106', mensagem: 'Veículo TUB-106 (L40) a aproximar-se do limiar crítico (76.3%).', timestamp: new Date(Date.now() - 1800000).toISOString() },
]

const DEMO_DASHBOARD = {
  taxaOcupacaoMedia: 60.1,
  volumeTotalPassageiros: 7300,
  totalAutocarros: DEMO_AUTOCARROS.length,
  autocarrosCriticos: DEMO_AUTOCARROS.filter(a => a.ocupacao > 80).map(a => ({ id: a.id, taxaOcupacao: a.ocupacao })),
  avisosRecentes: DEMO_ALERTAS,
}

const DEMO_CORRELACAO = {
  metricas: {
    totalPassageirosContados: 4280,
    totalViagensProgramadas: 312,
    ratioProcuraOferta: 13.7,
    periodoInicio: '2026-03-21',
    periodoFim: '2026-04-20',
  },
  procuraPorLinha: [
    { linhaId: 'L7', totalEntradas: 1850, totalSaidas: 1790, diasComDados: 28, totalLeituras: 340 },
    { linhaId: 'L43', totalEntradas: 1620, totalSaidas: 1580, diasComDados: 25, totalLeituras: 280 },
    { linhaId: 'L40', totalEntradas: 810, totalSaidas: 780, diasComDados: 20, totalLeituras: 150 },
  ],
  ofertaPorLinha: [
    { linhaId: 'L7', tipoDia: 'UTIL', viagensProgramadas: 42 },
    { linhaId: 'L7', tipoDia: 'FDS', viagensProgramadas: 18 },
    { linhaId: 'L43', tipoDia: 'UTIL', viagensProgramadas: 38 },
    { linhaId: 'L43', tipoDia: 'FDS', viagensProgramadas: 14 },
    { linhaId: 'L40', tipoDia: 'UTIL', viagensProgramadas: 30 },
  ],
  procuraPorHora: [
    { hora: 6, entradas: 45, saidas: 10 },
    { hora: 7, entradas: 280, saidas: 60 },
    { hora: 8, entradas: 520, saidas: 120 },
    { hora: 9, entradas: 390, saidas: 180 },
    { hora: 10, entradas: 210, saidas: 200 },
    { hora: 11, entradas: 180, saidas: 170 },
    { hora: 12, entradas: 320, saidas: 280 },
    { hora: 13, entradas: 290, saidas: 310 },
    { hora: 14, entradas: 200, saidas: 220 },
    { hora: 15, entradas: 180, saidas: 190 },
    { hora: 16, entradas: 250, saidas: 240 },
    { hora: 17, entradas: 480, saidas: 350 },
    { hora: 18, entradas: 420, saidas: 450 },
    { hora: 19, entradas: 210, saidas: 300 },
    { hora: 20, entradas: 90, saidas: 180 },
    { hora: 21, entradas: 40, saidas: 100 },
  ],
  bilheticaSimulada: {
    'Estudante': 1712,
    'Sénior': 856,
    'Passe Normal': 1070,
    'Zapping': 642,
  },
}

const DEMO_HISTORICO = (() => {
  const h = {}
  const today = new Date()
  for (let d = 0; d < 15; d++) {
    const dt = new Date(today)
    dt.setDate(dt.getDate() - d)
    const key = dt.toISOString().split('T')[0]
    h[key] = {}
    DEMO_AUTOCARROS.forEach(a => {
      h[key][a.id] = {
        entradas: Math.floor(Math.random() * 200) + 50,
        saidas: Math.floor(Math.random() * 180) + 40,
      }
    })
  }
  return h
})()

/**
 * Tenta fazer fetch à API real. Se falhar ou se o Modo Demo estiver ativo, retorna dados demo.
 */
export async function apiFetch(endpoint, options = {}) {
  const baseUrl = '/api'
  const isDemoForced = localStorage.getItem('pgu_demo_mode') === 'true'

  if (isDemoForced) {
    console.log(`[Demo Mode] Serving mock data for: ${endpoint}`)
    return { live: false, data: getDemoData(endpoint) }
  }

  try {
    const res = await fetch(`${baseUrl}${endpoint}`, { ...options, signal: AbortSignal.timeout(4000) })
    const data = await res.json()
    if (data.status === 'sucesso') return { live: true, data }
    throw new Error('API error')
  } catch (e) {
    // Fallback to demo data only if real fetch fails
    return { live: false, data: getDemoData(endpoint) }
  }
}

function getDemoData(endpoint) {
  if (endpoint.startsWith('/autocarros/')) {
    const id = endpoint.split('/')[2]
    const bus = DEMO_AUTOCARROS.find(a => a.id === id)
    if (bus) return { status: 'sucesso', ...bus, numeroAlertas: bus.ocupacao > 80 ? 2 : 0 }
    return { status: 'erro', mensagem: 'Autocarro não encontrado' }
  }

  switch (endpoint.split('?')[0]) {
    case '/dashboard':
      return { status: 'sucesso', dashboard: DEMO_DASHBOARD }
    case '/autocarros':
      return { status: 'sucesso', autocarros: DEMO_AUTOCARROS }
    case '/correlacao':
      return { status: 'sucesso', correlacao: DEMO_CORRELACAO }
    case '/historico':
      return { status: 'sucesso', historico: DEMO_HISTORICO }
    default:
      return { status: 'sucesso' }
  }
}

export { DEMO_AUTOCARROS, DEMO_ALERTAS, DEMO_DASHBOARD, DEMO_CORRELACAO }
