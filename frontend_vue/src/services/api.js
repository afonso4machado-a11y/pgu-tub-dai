/**
 * API Service — PGU-TUB
 * Tenta ligar ao backend Java. Se falhar, retorna dados de demonstração
 * realistas para permitir demonstrações completas de todas as funcionalidades.
 */
import { ref } from 'vue'

// ────────────────────── DADOS DE DEMONSTRAÇÃO ──────────────────────

const DEMO_AUTOCARROS = [
	{ id: 'TUB-101', capacidadeMaxima: 80, passageirosAtuais: 34, ocupacao: 42.5, linhaId: 'L7', marca: 'Mercedes', modelo: 'Citaro', matricula: '23-AB-45', ultimaLeitura: new Date().toISOString(), totalPassageirosTransportados: 1240 },
	{ id: 'TUB-102', capacidadeMaxima: 60, passageirosAtuais: 52, ocupacao: 86.7, linhaId: 'L43', marca: 'Volvo', modelo: '7900', matricula: '67-CD-89', ultimaLeitura: new Date().toISOString(), totalPassageirosTransportados: 980 },
	{ id: 'TUB-103', capacidadeMaxima: 80, passageirosAtuais: 15, ocupacao: 18.8, linhaId: 'L7', marca: 'Mercedes', modelo: 'eCitaro', matricula: '10-EF-23', ultimaLeitura: new Date().toISOString(), totalPassageirosTransportados: 560 },
	{ id: 'TUB-104', capacidadeMaxima: 50, passageirosAtuais: 48, ocupacao: 96.0, linhaId: 'L40', marca: 'MAN', modelo: "Lion's City", matricula: '45-GH-67', ultimaLeitura: new Date().toISOString(), totalPassageirosTransportados: 2100 },
	{ id: 'TUB-105', capacidadeMaxima: 70, passageirosAtuais: 28, ocupacao: 40.0, linhaId: 'L43', marca: 'Volvo', modelo: '7900E', matricula: '89-IJ-01', ultimaLeitura: new Date().toISOString(), totalPassageirosTransportados: 870 },
	{ id: 'TUB-106', capacidadeMaxima: 80, passageirosAtuais: 61, ocupacao: 76.3, linhaId: 'L40', marca: 'Mercedes', modelo: 'Citaro G', matricula: '34-KL-56', ultimaLeitura: new Date().toISOString(), totalPassageirosTransportados: 1550 },
	{ id: 'TUB-107', capacidadeMaxima: 65, passageirosAtuais: 37, ocupacao: 56.9, linhaId: 'L2', marca: 'IVECO', modelo: 'Crossway', matricula: '12-MN-34', ultimaLeitura: new Date().toISOString(), totalPassageirosTransportados: 710 },
	{ id: 'TUB-108', capacidadeMaxima: 60, passageirosAtuais: 26, ocupacao: 43.3, linhaId: 'L3', marca: 'Volvo', modelo: '7900H', matricula: '56-OP-78', ultimaLeitura: new Date().toISOString(), totalPassageirosTransportados: 630 },
	{ id: 'TUB-109', capacidadeMaxima: 75, passageirosAtuais: 49, ocupacao: 65.3, linhaId: 'L12', marca: 'Mercedes', modelo: 'Citaro', matricula: '90-QR-12', ultimaLeitura: new Date().toISOString(), totalPassageirosTransportados: 1120 },
	{ id: 'TUB-110', capacidadeMaxima: 70, passageirosAtuais: 33, ocupacao: 47.1, linhaId: 'L19', marca: 'MAN', modelo: "Lion's City", matricula: '23-ST-45', ultimaLeitura: new Date().toISOString(), totalPassageirosTransportados: 860 },
	// Novos autocarros demo para linhas adicionadas
	{ id: 'TUB-111', capacidadeMaxima: 55, passageirosAtuais: 24, ocupacao: 43.6, linhaId: 'L5', marca: 'Mercedes', modelo: 'Citaro', matricula: '11-UV-22', ultimaLeitura: new Date().toISOString(), totalPassageirosTransportados: 410 },
	{ id: 'TUB-112', capacidadeMaxima: 60, passageirosAtuais: 36, ocupacao: 60.0, linhaId: 'L6', marca: 'MAN', modelo: "Lion's City", matricula: '33-WX-44', ultimaLeitura: new Date().toISOString(), totalPassageirosTransportados: 720 },
	{ id: 'TUB-113', capacidadeMaxima: 50, passageirosAtuais: 12, ocupacao: 24.0, linhaId: 'L8', marca: 'IVECO', modelo: 'Urbanway', matricula: '55-YZ-66', ultimaLeitura: new Date().toISOString(), totalPassageirosTransportados: 190 },
	{ id: 'TUB-114', capacidadeMaxima: 65, passageirosAtuais: 28, ocupacao: 43.1, linhaId: 'L9', marca: 'Volvo', modelo: '7900', matricula: '77-AB-88', ultimaLeitura: new Date().toISOString(), totalPassageirosTransportados: 640 },
	{ id: 'TUB-115', capacidadeMaxima: 80, passageirosAtuais: 50, ocupacao: 62.5, linhaId: 'L13', marca: 'Mercedes', modelo: 'Citaro G', matricula: '99-CD-00', ultimaLeitura: new Date().toISOString(), totalPassageirosTransportados: 980 },
	{ id: 'TUB-116', capacidadeMaxima: 70, passageirosAtuais: 30, ocupacao: 42.9, linhaId: 'L14', marca: 'MAN', modelo: "Lion's City", matricula: '21-EF-33', ultimaLeitura: new Date().toISOString(), totalPassageirosTransportados: 540 },
	{ id: 'TUB-117', capacidadeMaxima: 60, passageirosAtuais: 18, ocupacao: 30.0, linhaId: 'L18', marca: 'IVECO', modelo: 'Crossway', matricula: '44-GH-55', ultimaLeitura: new Date().toISOString(), totalPassageirosTransportados: 310 },
	{ id: 'TUB-118', capacidadeMaxima: 72, passageirosAtuais: 40, ocupacao: 55.6, linhaId: 'L20', marca: 'Volvo', modelo: '7900H', matricula: '66-IJ-77', ultimaLeitura: new Date().toISOString(), totalPassageirosTransportados: 810 },
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
 volumePorHora: [
 { hora: 6, passageiros: 45 },
 { hora: 7, passageiros: 280 },
 { hora: 8, passageiros: 520 },
 { hora: 9, passageiros: 390 },
 { hora: 10, passageiros: 210 },
 { hora: 11, passageiros: 180 },
 { hora: 12, passageiros: 320 },
 { hora: 13, passageiros: 285 },
 { hora: 14, passageiros: 180 },
 { hora: 15, passageiros: 170 },
 { hora: 16, passageiros: 245 },
 { hora: 17, passageiros: 480 },
 { hora: 18, passageiros: 390 },
 { hora: 19, passageiros: 190 },
 { hora: 20, passageiros: 75 },
 { hora: 21, passageiros: 35 },
 ],
}

const DEMO_CORRELACAO = {
 metricas: {
 totalPassageirosContados: 3995, // soma de procuraPorHora.entradas
 totalViagensProgramadas: 142,   // soma de ofertaPorLinha.viagensProgramadas
 ratioProcuraOferta: 28.13,      // 3995 / 142 = 28.13
 periodoInicio: '2026-03-21',
 periodoFim: '2026-04-20',
 },
 procuraPorLinha: [
    { linhaId: 'L7', totalEntradas: 925, totalSaidas: 762, diasComDados: 28, totalLeituras: 340 },
    { linhaId: 'L43', totalEntradas: 810, totalSaidas: 672, diasComDados: 25, totalLeituras: 280 },
    { linhaId: 'L40', totalEntradas: 405, totalSaidas: 332, diasComDados: 20, totalLeituras: 150 },
    { linhaId: 'L2', totalEntradas: 215, totalSaidas: 170, diasComDados: 18, totalLeituras: 90 },
    { linhaId: 'L3', totalEntradas: 260, totalSaidas: 209, diasComDados: 18, totalLeituras: 100 },
    { linhaId: 'L12', totalEntradas: 315, totalSaidas: 255, diasComDados: 22, totalLeituras: 120 },
    { linhaId: 'L19', totalEntradas: 190, totalSaidas: 149, diasComDados: 16, totalLeituras: 75 },
    { linhaId: 'L5', totalEntradas: 105, totalSaidas: 85, diasComDados: 14, totalLeituras: 45 },
    { linhaId: 'L6', totalEntradas: 170, totalSaidas: 136, diasComDados: 16, totalLeituras: 70 },
    { linhaId: 'L8', totalEntradas: 75, totalSaidas: 60, diasComDados: 12, totalLeituras: 30 },
    { linhaId: 'L9', totalEntradas: 130, totalSaidas: 106, diasComDados: 15, totalLeituras: 58 },
    { linhaId: 'L13', totalEntradas: 200, totalSaidas: 162, diasComDados: 18, totalLeituras: 80 },
    { linhaId: 'L14', totalEntradas: 60, totalSaidas: 47, diasComDados: 10, totalLeituras: 20 },
    { linhaId: 'L18', totalEntradas: 48, totalSaidas: 38, diasComDados: 8, totalLeituras: 15 },
    { linhaId: 'L20', totalEntradas: 87, totalSaidas: 72, diasComDados: 12, totalLeituras: 36 },
 ],
 ofertaPorLinha: [
		{ linhaId: 'L7', tipoDia: 'UTIL', viagensProgramadas: 42 },
		{ linhaId: 'L7', tipoDia: 'FDS', viagensProgramadas: 18 },
		{ linhaId: 'L43', tipoDia: 'UTIL', viagensProgramadas: 38 },
		{ linhaId: 'L43', tipoDia: 'FDS', viagensProgramadas: 14 },
		{ linhaId: 'L40', tipoDia: 'UTIL', viagensProgramadas: 30 },
		{ linhaId: 'L2', tipoDia: 'UTIL', viagensProgramadas: 24 },
		{ linhaId: 'L3', tipoDia: 'UTIL', viagensProgramadas: 26 },
		{ linhaId: 'L12', tipoDia: 'UTIL', viagensProgramadas: 22 },
		{ linhaId: 'L19', tipoDia: 'UTIL', viagensProgramadas: 18 },
		{ linhaId: 'L5', tipoDia: 'UTIL', viagensProgramadas: 12 },
		{ linhaId: 'L6', tipoDia: 'UTIL', viagensProgramadas: 20 },
		{ linhaId: 'L8', tipoDia: 'UTIL', viagensProgramadas: 10 },
		{ linhaId: 'L9', tipoDia: 'UTIL', viagensProgramadas: 16 },
		{ linhaId: 'L13', tipoDia: 'UTIL', viagensProgramadas: 24 },
		{ linhaId: 'L14', tipoDia: 'UTIL', viagensProgramadas: 8 },
		{ linhaId: 'L18', tipoDia: 'UTIL', viagensProgramadas: 6 },
		{ linhaId: 'L20', tipoDia: 'UTIL', viagensProgramadas: 14 },
 ],
 procuraPorHora: [
 // Manha: muitas entradas, poucas saidas (passageiros a entrar na cidade)
 { hora: 6, entradas: 45, saidas: 10 },
 { hora: 7, entradas: 280, saidas: 55 },
 { hora: 8, entradas: 520, saidas: 110 },
 { hora: 9, entradas: 390, saidas: 175 },
 { hora: 10, entradas: 210, saidas: 195 },
 { hora: 11, entradas: 180, saidas: 165 },
 // Hora de almoco: equilibrio
 { hora: 12, entradas: 320, saidas: 280 },
 { hora: 13, entradas: 285, saidas: 270 },
 // Tarde: mais saidas (acumulacao da manha a dissipar-se)
 // O total acumulado de entradas ate aqui e ~2230, saidas ~1260,
 // ha ~970 passageiros "no sistema" que agora comecam a sair.
 { hora: 14, entradas: 180, saidas: 230 },
 { hora: 15, entradas: 170, saidas: 195 },
 { hora: 16, entradas: 245, saidas: 240 },
 { hora: 17, entradas: 480, saidas: 340 },
 // Fim do dia: mais saidas que entradas (pessoas a ir para casa)
 { hora: 18, entradas: 390, saidas: 450 },
 { hora: 19, entradas: 190, saidas: 295 },
 { hora: 20, entradas: 75, saidas: 160 },
 { hora: 21, entradas: 35, saidas: 85 },
 // Total entradas: 3995 | Total saidas: 3255
 ],
 bilheticaSimulada: {
 // Base: 3995 passageiros | Estudante=35% Sénior=20% Passe Normal=30% Zapping=resto
 'Estudante': 1398,   // Math.round(3995 * 0.35)
 'Sénior': 799,       // Math.round(3995 * 0.20)
 'Passe Normal': 1199, // Math.round(3995 * 0.30)
 'Zapping': 599,      // 3995 - 1398 - 799 - 1199 (resto: garante soma exacta = 3995)
 },
}

const DEMO_HISTORICO = (() => {
 const h = {}
 const today = new Date()
 for (let d = 0; d < 30; d++) {
 const dt = new Date(today)
 dt.setDate(dt.getDate() - d)
 const key = dt.toISOString().split('T')[0]
 h[key] = {}
 DEMO_AUTOCARROS.forEach(a => {
 // Gerar entradas primeiro; saidas e sempre <= entradas
 // (num dia completo, um autocarro que comeca vazio nao pode
 // ter mais saidas do que entradas — conservacao de passageiros)
  const entradas = Math.floor(Math.random() * 200) + 50
  const saidas = entradas // Tens toda a razão, ninguém fica a morar no autocarro!
  h[key][a.id] = { entradas, saidas }
  })
 }
 return h
})()

const DEMO_PARAGENS = [
 "S. Mamede d' Este", "Avenida da Liberdade", "Igreja S Lázaro", "Celeirós",
 "Rua 25 de Abril", "Parque Infantil", "Hospital", "Rua Egídio Guimarães",
 "Avenida Central", "Rua Mário de Almeida", "Estação C.P.", "U.Minho",
 "Universidade do Minho", "Terminal Intermodal", "São Vítor", "Maximinos",
 "Bom Jesus", "Nogueiró", "Gualtar", "Braga Parque", "Estádio Municipal",
]

// ────────────────────── ESTADO GLOBAL (REATIVO) ──────────────────────

/** Ref reativo — permite que as views façam watch() e recarreguem dados */
export const demoModeRef = ref(localStorage.getItem('pgu-demo-mode') === '1')

/**
 * Verifica se a app está em modo de demonstração.
 * @returns {boolean}
 */
export function isDemoMode() {
 return demoModeRef.value
}

/**
 * Define o modo de demonstração manualmente.
 * @param {boolean} val
 */
export function setDemoMode(val) {
 demoModeRef.value = !!val
 localStorage.setItem('pgu-demo-mode', val ? '1' : '0')
}

/**
 * Alterna entre modo de demonstração e dados reais.
 * @returns {boolean} novo estado
 */
export function toggleDemoMode() {
 setDemoMode(!demoModeRef.value)
 return demoModeRef.value
}

// ────────────────────── FETCH PRINCIPAL ──────────────────────

/**
 * Faz fetch à API real. Se falhar (backend indisponível), retorna dados de demonstração.
 * O servidor Express (server.js) faz proxy de /api → backend Java.
 * 
 * @param {string} endpoint — caminho relativo (ex: '/dashboard')
 * @param {object} options — opções para o fetch (method, headers, body, etc.)
 * @param {AbortSignal} customSignal — sinal opcional para cancelamento (AbortController)
 * @returns {{ live: boolean, data: object }}
 */
export async function apiFetch(endpoint, options = {}, customSignal = null) {
  const baseUrl = '/api'

  // Se o modo demo está forçado pelo utilizador, nem tenta a API
  if (demoModeRef.value) {
    return { live: false, data: getDemoData(endpoint) }
  }

  try {
    const fetchOptions = {
      ...options,
      signal: customSignal || options.signal || AbortSignal.timeout(4000)
    }
    const res = await fetch(`${baseUrl}${endpoint}`, fetchOptions)
    const data = await res.json()
    if (data.status === 'sucesso') {
      return { live: true, data }
    }
    throw new Error('API error')
  } catch (e) {
    if (e.name === 'AbortError') {
      throw e; // Repassa para o useSafeLoading abortar o fluxo visual
    }
    return { live: false, data: getDemoData(endpoint) }
  }
}

// ────────────────────── DADOS DEMO POR ENDPOINT ──────────────────────

function getDemoData(endpoint) {
 // Endpoint de autocarros eliminados — lista vazia em modo demo
 if (endpoint === '/autocarros/eliminados') {
 return { status: 'sucesso', autocarros: [] }
 }

 // Endpoint individual: /autocarros/TUB-101
 if (endpoint.startsWith('/autocarros/') && !endpoint.endsWith('/restaurar')) {
 const id = endpoint.split('/')[2]
 const bus = DEMO_AUTOCARROS.find(a => a.id === id)
 if (bus) return { status: 'sucesso', ...bus, numeroAlertas: bus.ocupacao > 80 ? 2 : 0 }
 return { status: 'erro', mensagem: 'Autocarro não encontrado' }
 }

 const path = endpoint.split('?')[0]

 switch (path) {
 case '/dashboard':
 return { status: 'sucesso', dashboard: DEMO_DASHBOARD }
 case '/autocarros':
 return { status: 'sucesso', autocarros: DEMO_AUTOCARROS }
 case '/correlacao':
 return { status: 'sucesso', correlacao: DEMO_CORRELACAO }
 case '/historico':
 return { status: 'sucesso', historico: DEMO_HISTORICO }
 case '/paragens':
 return { status: 'sucesso', paragens: DEMO_PARAGENS }
 case '/leituras':
 return { status: 'sucesso', mensagem: 'Leitura registada com sucesso (demonstração).', alertas: [] }
 default:
 // POST /autocarros, POST /linhas/Lx/autocarros, etc.
 return { status: 'sucesso', mensagem: 'Operação simulada com sucesso (demonstração).' }
 }
}

export { DEMO_AUTOCARROS, DEMO_ALERTAS, DEMO_DASHBOARD, DEMO_CORRELACAO, DEMO_HISTORICO, DEMO_PARAGENS }
