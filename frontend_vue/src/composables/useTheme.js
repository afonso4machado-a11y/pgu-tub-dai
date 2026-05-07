import { ref, watch } from 'vue'

const currentTheme = ref(localStorage.getItem('pgu_theme') || 'dark')
const hasPermission = ref(localStorage.getItem('pgu_theme_permission') === 'true')

export function useTheme() {
  const applyTheme = (theme) => {
    let effectiveTheme = theme;

    if (theme === 'auto') {
      if (!hasPermission.value) {
         const userConfirmed = confirm('Para usar o modo automático, precisamos ler a definição de claridade do seu computador. Só leremos esta definição e nada mais. Aceita?');
         if (userConfirmed) {
           hasPermission.value = true;
           localStorage.setItem('pgu_theme_permission', 'true');
         } else {
           // Fallback to dark mode if permission denied
           setTheme('dark');
           return;
         }
      }

      const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches
      effectiveTheme = prefersDark ? 'dark' : 'light'
    }

    document.documentElement.setAttribute('data-theme', effectiveTheme)
  }

  const setTheme = (theme) => {
    currentTheme.value = theme
    localStorage.setItem('pgu_theme', theme)
    applyTheme(theme)
  }

  const initTheme = () => {
    applyTheme(currentTheme.value)

    window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', (e) => {
      if (currentTheme.value === 'auto' && hasPermission.value) {
        applyTheme('auto')
      }
    })
  }

  return {
    currentTheme,
    setTheme,
    initTheme
  }
}
