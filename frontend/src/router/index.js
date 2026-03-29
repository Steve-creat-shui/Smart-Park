import { createRouter, createWebHistory } from 'vue-router'
import Dashboard from '../views/Dashboard.vue'

const routes = [
  {
    path: '/',
    name: 'Dashboard',
    component: Dashboard,
    meta: { title: '系统总览' }
  },
  {
    path: '/energy-analysis',
    name: 'EnergyAnalysis',
    component: () => import('../views/EnergyAnalysis.vue'),
    meta: { title: '能耗分析' }
  },
  {
    path: '/device-status',
    name: 'DeviceStatus',
    component: () => import('../views/DeviceStatus.vue'),
    meta: { title: '设备状态' }
  },
  {
    path: '/ai-analysis',
    name: 'AIAnalysis',
    component: () => import('../views/AIAnalysis.vue'),
    meta: { title: 'AI智能分析' }
  },
  {
    path: '/report',
    name: 'Report',
    component: () => import('../views/Report.vue'),
    meta: { title: '报告生成' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  document.title = `${to.meta.title || '管理系统'} - 建筑能源数字孪生`
})

export default router
