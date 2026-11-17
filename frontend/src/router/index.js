import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { title: '登录', public: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Register.vue'),
    meta: { title: '注册', public: true }
  },
  {
    path: '/landing',
    name: 'Landing',
    component: () => import('../views/LandingPage.vue'),
    meta: { title: '鼎筑科技 - 建筑能源智能管理系统', public: true, fullscreen: true }
  },
  {
    path: '/welcome',
    name: 'Welcome',
    component: () => import('../views/Welcome.vue'),
    meta: { title: '欢迎', public: true, fullscreen: true }
  },
  {
    path: '/',
    name: 'Root',
    component: () => import('../views/Home.vue'),
    meta: { title: '首页' }
  },
  {
    path: '/home',
    name: 'Home',
    component: () => import('../views/Home.vue'),
    meta: { title: '首页', roles: ['enterprise'] }
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('../views/Dashboard.vue'),
    meta: { title: '系统总览', roles: ['user', 'enterprise', 'admin'] }
  },
  {
    path: '/energy-analysis',
    name: 'EnergyAnalysis',
    component: () => import('../views/EnergyAnalysis.vue'),
    meta: { title: '能耗分析', roles: ['user', 'enterprise', 'admin'] }
  },
  {
    path: '/device-status',
    name: 'DeviceStatus',
    component: () => import('../views/DeviceStatus.vue'),
    meta: { title: '设备状态', roles: ['user', 'enterprise', 'admin'] }
  },
  {
    path: '/ai-analysis',
    name: 'AIAnalysis',
    component: () => import('../views/AIAnalysis.vue'),
    meta: { title: '源芯智控平台', roles: ['user', 'enterprise', 'admin'] }
  },
  {
    path: '/data-management',
    name: 'DataManagement',
    component: () => import('../views/DataManagement.vue'),
    meta: { title: '数据管理', roles: ['user', 'enterprise', 'admin'] }
  },
  {
    path: '/report',
    name: 'Report',
    component: () => import('../views/Report.vue'),
    meta: { title: '报告生成', roles: ['enterprise'] }
  },
  {
    path: '/enterprise-decision',
    name: 'EnterpriseDecision',
    component: () => import('../views/EnterpriseDecision.vue'),
    meta: { title: '管理决策', roles: ['enterprise'] }
  },
  {
    path: '/settings',
    name: 'Settings',
    component: () => import('../views/SystemSettings.vue'),
    meta: { title: '系统设置', roles: ['user', 'enterprise', 'admin'] }
  },
  {
    path: '/multimodal',
    name: 'MultiModal',
    component: () => import('../views/MultiModalMonitor.vue'),
    meta: { title: '多模态监控', roles: ['enterprise', 'admin'] }
  },
  {
    path: '/alert-management',
    name: 'AlertManagement',
    component: () => import('../views/AlertManagement.vue'),
    meta: { title: '告警管理', roles: ['enterprise', 'admin'] }
  },
  {
    path: '/work-order',
    name: 'WorkOrderManagement',
    component: () => import('../views/WorkOrderManagement.vue'),
    meta: { title: '工单管理', roles: ['enterprise', 'admin'] }
  },
  {
    path: '/enterprise-apply',
    name: 'EnterpriseApply',
    component: () => import('../views/EnterpriseApply.vue'),
    meta: { title: '注册企业', roles: ['enterprise'] }
  },
  {
    path: '/admin-device',
    name: 'AdminDevice',
    component: () => import('../views/AdminDevice.vue'),
    meta: { title: '设备资产管理', roles: ['admin'] }
  },
  {
    path: '/admin-audit',
    name: 'AdminAudit',
    component: () => import('../views/AdminAudit.vue'),
    meta: { title: '审计与日志', roles: ['admin'] }
  },
  {
    path: '/admin-config',
    name: 'AdminConfig',
    component: () => import('../views/AdminConfig.vue'),
    meta: { title: '系统配置', roles: ['admin'] }
  },
  {
    path: '/admin-ops',
    name: 'AdminOps',
    component: () => import('../views/AdminOps.vue'),
    meta: { title: '运维保障', roles: ['admin'] }
  },
  {
    path: '/admin',
    name: 'AdminDashboard',
    component: () => import('../views/AdminDashboard.vue'),
    meta: { title: '管理面板', roles: ['admin'] }
  },
  {
    path: '/register-pending',
    name: 'RegisterPending',
    component: () => import('../views/RegisterPending.vue'),
    meta: { title: '等待审批', public: true }
  },
  {
    path: '/building/:id',
    name: 'BuildingDetail',
    component: () => import('../views/BuildingDetail.vue'),
    meta: { title: '建筑详情', hideSidebar: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 各角色默认落地页
const roleLanding = { admin: '/admin', enterprise: '/home', user: '/dashboard' }

router.beforeEach((to, from, next) => {
  document.title = `${to.meta.title || '管理系统'} - 鼎筑`
  const token = localStorage.getItem('token')
  const user = JSON.parse(localStorage.getItem('user') || 'null')
  const role = user?.role || 'user'

  // 1. 未登录访问受保护页 → 跳到首页
  if (!to.meta.public && !token) {
    return next({ name: 'Landing' })
  }
  // 2. 已登录访问 login/register → 跳到角色落地页
  if (to.meta.public && token && (to.name === 'Login' || to.name === 'Register')) {
    return next(roleLanding[role] || '/dashboard')
  }
  // 3. Root(/) → 未登录去 landing，已登录按角色跳转
  if (to.name === 'Root') {
    if (!token) return next('/landing')
    return next(roleLanding[role] || '/dashboard')
  }
  // 4. 路由有 roles 限制 → 检查权限
  if (to.meta.roles && !to.meta.roles.includes(role)) {
    return next(roleLanding[role] || '/dashboard')
  }
  next()
})

export default router
