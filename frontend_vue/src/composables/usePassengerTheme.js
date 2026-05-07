import { ref } from 'vue'

const currentPassengerTheme = ref(localStorage.getItem('pgu_passenger_theme') || 'dark')
const hasPassengerPermission = ref(localStorage.getItem('pgu_passenger_theme_permission') === 'true')

export function usePassengerTheme() {
  const applyTheme = (theme) => {
    let effectiveTheme = theme;

    if (theme === 'auto') {
      if (!hasPassengerPermission.value) {
         const userConfirmed = confirm('Para usar o modo automático, precisamos ler a definição de claridade do seu dispositivo. Só leremos esta definição e nada mais. Aceita?');
         if (userConfirmed) {
           hasPassengerPermission.value = true;
           localStorage.setItem('pgu_passenger_theme_permission', 'true');
         } else {
           setTheme('dark');
           return;
         }
      }

      const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches
      effectiveTheme = prefersDark ? 'dark' : 'light'
    }

    // Usamos um prefixo para não colidir com o tema do backoffice, se for usado o mesmo DOM (o que é improvável em uso real, mas seguro)
    // Na verdade, a app passageiro está na mesma SPA. Portanto, podemos definir no body ou apenas no root.
    // Vamos adicionar um data-passenger-theme no HTML element, ou mudar o data-theme e garantir que as views passageiros a leiam.
    // No entanto, as variáveis CSS estão dependentes de `[data-theme='light']` globalmente.
    // O mais seguro para isolar sem reescrever CSS é definir uma root diferente.
    document.documentElement.setAttribute('data-theme', effectiveTheme)
  }

  const setTheme = (theme) => {
    currentPassengerTheme.value = theme
    localStorage.setItem('pgu_passenger_theme', theme)
    applyTheme(theme)
  }

  const initTheme = () => {
    applyTheme(currentPassengerTheme.value)

    window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', () => {
      if (currentPassengerTheme.value === 'auto' && hasPassengerPermission.value) {
        applyTheme('auto')
      }
    })
  }

  return {
    currentPassengerTheme,
    setTheme,
    initTheme
  }
}
