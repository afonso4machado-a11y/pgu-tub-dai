<script setup>
import BaseSkeleton from './BaseSkeleton.vue'
</script>

<template>
  <div class="page-skeleton-wrapper fade-in">
    <!-- Premium Header Title Skeleton -->
    <div class="welcome-banner glass-panel">
      <div class="banner-content">
        <BaseSkeleton width="280px" height="1.85rem" style="margin-bottom: 0.5rem;" />
        <BaseSkeleton width="200px" height="0.95rem" />
      </div>
    </div>

    <!-- Action Bar / Subtitle Skeleton -->
    <div class="action-bar" style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.5rem;">
      <BaseSkeleton width="180px" height="1.4rem" />
      <BaseSkeleton width="100px" height="2.2rem" />
    </div>

    <!-- Grid of 4 KPI metrics cards skeletons -->
    <div class="quick-stats">
      <div v-for="i in 4" :key="'stat-sk-' + i" class="stat-card glass-panel">
        <div class="stat-content" style="display: flex; align-items: center; width: 100%; gap: 1rem;">
          <BaseSkeleton width="48px" height="48px" shape="circle" />
          <div style="display: flex; flex-direction: column; flex: 1; gap: 0.4rem;">
            <BaseSkeleton width="80px" height="1.6rem" />
            <BaseSkeleton width="110px" height="0.8rem" />
          </div>
        </div>
      </div>
    </div>

    <!-- Main Chart / High-Fidelity Central Board Skeleton -->
    <div class="chart-section glass-panel">
      <div class="chart-header" style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.5rem; flex-wrap: wrap; gap: 0.75rem;">
        <div style="display: flex; flex-direction: column; gap: 0.35rem; width: 60%;">
          <BaseSkeleton width="240px" height="1.25rem" />
          <BaseSkeleton width="340px" height="0.8rem" />
        </div>
        <BaseSkeleton width="90px" height="1.6rem" shape="rect" style="border-radius: 999px;" />
      </div>
      <div class="chart-canvas-wrapper">
        <div style="display: flex; flex-direction: column; width: 100%; height: 100%; justify-content: flex-end; gap: 0.5rem; padding: 1rem 0;">
          <div style="display: flex; align-items: flex-end; justify-content: space-between; width: 100%; height: 180px; padding: 0 0.5rem; border-bottom: 1px solid var(--border-light);">
            <!-- High Fidelity Shimmer/Pulse Bars representing active network load -->
            <BaseSkeleton 
              v-for="h in 16" 
              :key="'ch-sk-'+h" 
              width="4%" 
              :height="[45, 60, 30, 75, 55, 90, 40, 65, 80, 50, 70, 85, 35, 60, 50, 75][h-1] + '%'" 
              animation="shimmer" 
              style="opacity: 0.5;" 
            />
          </div>
          <div style="display: flex; justify-content: space-between; width: 100%; padding: 0.25rem 0.25rem 0 0.25rem;">
            <BaseSkeleton v-for="h in 6" :key="'lbl-sk-'+h" width="40px" height="0.8rem" />
          </div>
        </div>
      </div>
    </div>

    <!-- Main Body Grid Container with Left & Right Panels -->
    <div class="dashboard-content">
      <!-- Left Panel: Table/List Skeleton -->
      <div class="content-section">
        <BaseSkeleton width="180px" height="1.3rem" style="margin-bottom: 1rem;" />
        <div class="list-container glass-panel">
          <div style="display: flex; flex-direction: column; gap: 1rem; width: 100%; padding: 0.5rem 0;">
            <div v-for="i in 3" :key="'left-sk-'+i" style="display: flex; justify-content: space-between; align-items: center; width: 100%; border-bottom: 1px solid var(--border-light); padding-bottom: 0.75rem;">
              <div style="display: flex; flex-direction: column; gap: 0.35rem; flex: 1;">
                <BaseSkeleton width="45%" height="1.1rem" />
                <BaseSkeleton width="25%" height="0.75rem" />
              </div>
              <BaseSkeleton width="80px" height="1.1rem" />
            </div>
          </div>
        </div>
      </div>

      <!-- Right Panel: System Notifications/Alerts Skeleton -->
      <div class="content-section">
        <BaseSkeleton width="160px" height="1.3rem" style="margin-bottom: 1rem;" />
        <div class="list-container glass-panel">
          <div style="display: flex; flex-direction: column; gap: 1.25rem; width: 100%; padding: 0.5rem 0;">
            <div v-for="i in 3" :key="'right-sk-'+i" style="display: flex; align-items: flex-start; gap: 1rem; width: 100%;">
              <BaseSkeleton width="32px" height="32px" shape="circle" />
              <div style="display: flex; flex-direction: column; gap: 0.4rem; flex: 1;">
                <BaseSkeleton width="85%" height="1.1rem" />
                <BaseSkeleton width="50%" height="0.8rem" />
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page-skeleton-wrapper {
  width: 100%;
}

.welcome-banner {
  margin-bottom: 2rem;
  background: var(--bg-surface);
  padding: 1.5rem;
  border-radius: 1rem;
  border: 1px solid var(--border-light);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.quick-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1.5rem;
  margin-bottom: 2rem;
}

.stat-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  padding: 1.5rem;
  background: var(--bg-surface);
  border: 1px solid var(--border-light);
  border-radius: 1rem;
  overflow: hidden;
  position: relative;
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 1.25rem;
  z-index: 2;
}

.chart-section {
  background: var(--bg-surface);
  border: 1px solid var(--border-light);
  border-radius: 1rem;
  padding: 1.75rem;
  margin-bottom: 2rem;
}

.chart-canvas-wrapper {
  position: relative;
  height: 260px;
}

.dashboard-content {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1.5rem;
}

@media (max-width: 768px) {
  .dashboard-content {
    grid-template-columns: 1fr;
  }
}

.content-section {
  display: flex;
  flex-direction: column;
}

.list-container {
  display: flex;
  flex-direction: column;
  padding: 1rem;
  border-radius: 1rem;
  background: var(--bg-surface);
  border: 1px solid var(--border-light);
  min-height: 200px;
  max-height: 400px;
  overflow-y: auto;
}
</style>
