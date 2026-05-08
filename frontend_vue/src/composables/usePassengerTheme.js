import { ref } from 'vue'

const currentPassengerTheme = ref(localStorage.getItem('pgu_passenger_theme') || 'dark')
const hasPassengerPermission = ref(localStorage.getItem('pgu_passenger_theme_permission') === 'true')

export function usePassengerTheme() {
  const applyTheme = (theme) => {
    let effectiveTheme = theme;

    if (theme === 'auto') {
      const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches
      effectiveTheme = prefersDark ? 'dark' : 'light'
    }

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
      if (currentPassengerTheme.value === 'auto') {
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
