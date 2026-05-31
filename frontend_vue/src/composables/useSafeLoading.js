import { ref } from 'vue'

export function useSafeLoading(options = {}) {
  const isLoading = ref(false)
  const errorMsg = ref('')
  let timeoutId = null
  const DEFAULT_TIMEOUT_MS = 5000 // Limiar máximo de segurança

  /**
   * Executa uma chamada à API de forma segura injetando um sinal de cancelamento.
   * @param {Function} apiCallWithSignal - Função que recebe (signal) e executa o fetch/axios
   */
  async function execute(apiCallWithSignal) {
    isLoading.value = true
    errorMsg.value = ''

    // 1. Injeta o mecanismo de abortar o pedido HTTP na rede
    const abortController = new AbortController()
    const { signal } = abortController

    // 2. Cria a promessa de timeout de segurança que aborta fisicamente o pedido
    const timeoutPromise = new Promise((_, reject) => {
      timeoutId = setTimeout(() => {
        abortController.abort() // Cancela a conexão HTTP ativa imediatamente na rede!
        reject(new Error('API_TIMEOUT_GUARD'))
      }, options.timeoutMs || DEFAULT_TIMEOUT_MS)
    })

    try {
      // 3. Corrida entre o pedido de rede (com o signal) e o timeout visual
      const response = await Promise.race([
        apiCallWithSignal(signal),
        timeoutPromise
      ])
      return response
    } catch (err) {
      // 4. Sanitização contra vazamento de stack traces e mensagens do servidor
      if (err.name === 'AbortError' || err.message === 'API_TIMEOUT_GUARD') {
        errorMsg.value = 'O sistema está sob forte carga. Transição cancelada por segurança.'
        console.error('[DevSecOps] Pedido HTTP abortado fisicamente: Excedido o limite de 5s.')
      } else {
        errorMsg.value = 'Falha ao comunicar com os serviços centrais.'
        console.error('[Auditoria Sanitizada]:', err.message || err)
      }
      throw new Error(errorMsg.value)
    } finally {
      // 5. Limpeza de timers e garantia de fecho do estado de esqueleto
      if (timeoutId) {
        clearTimeout(timeoutId)
      }
      isLoading.value = false
    }
  }

  return {
    isLoading,
    errorMsg,
    execute
  }
}
