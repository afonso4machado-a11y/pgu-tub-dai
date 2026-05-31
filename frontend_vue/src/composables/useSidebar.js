import { ref } from 'vue'

const isSidebarCollapsed = ref(localStorage.getItem('pgu_sidebar_collapsed') === 'true')

export function useSidebar() {
  const toggleSidebar = () => {
    isSidebarCollapsed.value = !isSidebarCollapsed.value
    localStorage.setItem('pgu_sidebar_collapsed', isSidebarCollapsed.value ? 'true' : 'false')
  }

  const setSidebarCollapsed = (value) => {
    isSidebarCollapsed.value = value
    localStorage.setItem('pgu_sidebar_collapsed', value ? 'true' : 'false')
  }

  return {
    isSidebarCollapsed,
    toggleSidebar,
    setSidebarCollapsed
  }
}
