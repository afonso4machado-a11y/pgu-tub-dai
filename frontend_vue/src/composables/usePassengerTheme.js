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

 const setTheme = async (theme) => {
 currentPassengerTheme.value = theme
 localStorage.setItem('pgu_passenger_theme', theme)
 applyTheme(theme)

 const user = JSON.parse(localStorage.getItem('pgu_user') || 'null')
 if (user && user.id && !user.id.startsWith('demo-')) {
   try {
     const res = await fetch(`/api/auth/profile/${user.id}`, {
       method: 'PUT',
       headers: { 'Content-Type': 'application/json' },
       body: JSON.stringify({ tema: theme })
     })
     const data = await res.json()
     if (data.status === 'sucesso') {
       localStorage.setItem('pgu_user', JSON.stringify(data.user))
     }
   } catch (e) {
     console.error('Erro ao guardar tema no servidor:', e)
   }
 }
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
