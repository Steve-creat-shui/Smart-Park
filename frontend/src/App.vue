<template>
  <router-view v-if="$route.meta.public || $route.meta.hideSidebar || $route.meta.fullscreen" />
  <el-container v-else class="app-container">
    <el-aside v-if="!anyOnboarding" width="220px" class="sidebar">
      <div class="logo-area">
        <div class="logo-icon">⚡</div>
        <div class="logo-text">
          <div class="logo-title">鼎筑</div>
          <div class="logo-sub">建筑能源智能管理系统</div>
        </div>
      </div>
      <el-menu
        :default-active="$route.path"
        background-color="#0d1b2e"
        text-color="#8bafc9"
        active-text-color="#00d4ff"
        class="nav-menu"
      >
        <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.index || item.path" @click="navTo(item.path)">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </el-menu-item>
      </el-menu>
      <div class="sidebar-footer">
        <div class="user-info" v-if="authStore.user">
          <span class="user-name">{{ authStore.user.nickname }}</span>
          <el-tag :type="roleTagType" size="small" effect="dark">
            {{ roleLabel }}
          </el-tag>
        </div>
        <div v-if="authStore.isEnterprise && authStore.user?.enterpriseName" class="enterprise-info">
          <el-icon><OfficeBuilding /></el-icon>
          <span class="ent-name">{{ authStore.user.enterpriseName }}</span>
        </div>
        <div class="system-status">
          <span class="status-dot"></span>
          <span>系统运行正常</span>
        </div>
      </div>
    </el-aside>
    <el-container>
      <el-header v-if="anyOnboarding" class="top-header" style="display:flex;align-items:center;justify-content:space-between">
        <div style="display:flex;align-items:center;gap:10px">
          <span style="font-size:18px">⚡</span>
          <span style="color:#00d4ff;font-weight:600">鼎筑</span>
          <span style="color:#5a7a9a;font-size:13px;margin-left:4px">{{ enterpriseOnboarding ? '企业数据初始化' : '个人数据导入' }}</span>
        </div>
        <el-dropdown @command="handleCommand" trigger="click">
          <el-avatar size="small" style="background:#00d4ff; color:#fff; cursor:pointer">
            {{ authStore.user?.nickname?.charAt(0) || '用' }}
          </el-avatar>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item disabled>{{ authStore.user?.nickname }}（{{ authStore.user?.phone }}）</el-dropdown-item>
              <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>
      <el-header v-else class="top-header">
        <div class="header-left">
          <span class="page-title">{{ $route.meta.title }}</span>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ $route.meta.title }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <!-- 企业切换 -->
          <el-dropdown v-if="authStore.isEnterprise && enterprises.length > 1" @command="onSwitchEnterprise" trigger="click">
            <el-button size="small" type="primary" plain>
              <el-icon><OfficeBuilding /></el-icon> 切换企业
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-for="e in enterprises" :key="e.id" :command="e.id" :disabled="e.id === authStore.currentEnterpriseId">
                  {{ e.enterpriseName }} {{ e.id === authStore.currentEnterpriseId ? '✓' : '' }}
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <!-- 通知 -->
          <el-badge :value="unreadCount" :hidden="!unreadCount" :max="99">
            <el-icon class="notify-bell" @click="navTo('/settings')"><Bell /></el-icon>
          </el-badge>
          <span class="time-display">{{ currentTime }}</span>
          <el-dropdown @command="handleCommand" trigger="click">
            <el-avatar size="small" style="background:#00d4ff; color:#ffffff; cursor:pointer">
              {{ authStore.user?.nickname?.charAt(0) || '用' }}
            </el-avatar>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled>{{ authStore.user?.nickname }}（{{ authStore.user?.phone }}）</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="main-content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { DataBoard, TrendCharts, Monitor, MagicStick, Document, Setting, House, UserFilled, VideoCameraFilled, OfficeBuilding, Bell, Tickets, Upload, Histogram, Cpu, SetUp, View } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { useAuthStore } from './stores/auth.js'
import { enterpriseApi } from './api/index.js'

const router = useRouter()
const authStore = useAuthStore()
const currentTime = ref(dayjs().format('YYYY-MM-DD HH:mm:ss'))
const enterprises = ref([])
const unreadCount = ref(0)
let timer = null

const userRole = computed(() => authStore.user?.role || 'user')
const enterpriseOnboarding = computed(() => authStore.isEnterprise && authStore.user?.hasData === false)
const personalOnboarding = computed(() => userRole.value === 'user' && authStore.user?.hasData === false)
const anyOnboarding = computed(() => enterpriseOnboarding.value || personalOnboarding.value)

const roleLabel = computed(() => {
  const ut = authStore.userType
  if (ut === 'platform_admin') return '管理运维'
  if (ut === 'enterprise') return '企业用户'
  return '个人用户'
})
const roleTagType = computed(() => {
  const ut = authStore.userType
  if (ut === 'platform_admin') return 'danger'
  if (ut === 'enterprise') return 'warning'
  return 'info'
})

const menuItems = computed(() => {
  const role = userRole.value

  if (role === 'user') {
    // 个人用户端: 6个模块，自有数据
    return [
      { path: '/data-management', icon: Upload, label: '导入' },
      { path: '/dashboard', icon: DataBoard, label: '系统总览' },
      { path: '/energy-analysis', icon: TrendCharts, label: '能耗分析' },
      { path: '/device-status', icon: Monitor, label: '设备状态' },
      { path: '/ai-analysis', icon: MagicStick, label: '源芯智控平台' },
      { path: '/settings', icon: Setting, label: '个人设置' },
    ]
  }

  if (role === 'enterprise') {
    // 企业用户端: 全部模块 + 决策
    return [
      { path: '/home', icon: House, label: '首页' },
      { path: '/dashboard', icon: DataBoard, label: '系统总览' },
      { path: '/energy-analysis', icon: TrendCharts, label: '能耗分析' },
      { path: '/device-status', icon: Monitor, label: '设备状态' },
      { path: '/multimodal', icon: VideoCameraFilled, label: '多模态监控' },
      { path: '/alert-management', icon: Bell, label: '告警管理' },
      { path: '/work-order', icon: Tickets, label: '工单管理' },
      { path: '/ai-analysis', icon: MagicStick, label: '源芯智控平台' },
      { path: '/data-management', icon: Document, label: '数据管理' },
      { path: '/enterprise-decision', icon: TrendCharts, label: '管理决策' },
      { path: '/settings', icon: Setting, label: '系统设置' },
    ]
  }

  if (role === 'admin') {
    return [
      { path: '/admin', icon: UserFilled, label: '用户与内容管理' },
      { path: '/admin?tab=enterprise', index: '/admin-enterprise', icon: OfficeBuilding, label: '企业管理' },
      { path: '/admin-device', icon: SetUp, label: '设备资产管理' },
      { path: '/admin-audit', icon: View, label: '审计与日志' },
      { path: '/ai-analysis', icon: MagicStick, label: '源芯智控平台' },
      { path: '/admin-config', icon: Setting, label: '系统配置' },
      { path: '/admin-ops', icon: Cpu, label: '运维保障' },
    ]
  }

  return [{ path: '/dashboard', icon: DataBoard, label: '系统总览' }]
})

async function loadEnterprises() {
  if (!authStore.isLoggedIn) return
  if (authStore.isEnterprise) {
    try {
      const res = await enterpriseApi.myEnterprises()
      enterprises.value = res.data || []
    } catch (e) { /* ignore */ }
  }
}

async function loadUnread() {
  if (!authStore.isLoggedIn) return
  try {
    const res = await enterpriseApi.getUnreadCount()
    unreadCount.value = res.data || 0
  } catch (e) { /* ignore */ }
}

async function onSwitchEnterprise(enterpriseId) {
  try {
    const res = await enterpriseApi.switchEnterprise(enterpriseId)
    const d = res.data
    authStore.updateEnterprise(d.token, d.currentEnterpriseId, d.enterpriseName)
    ElMessage.success('已切换到 ' + d.enterpriseName)
    router.go(0) // reload to refresh all data
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '切换失败')
  }
}

onMounted(() => {
  timer = setInterval(() => {
    currentTime.value = dayjs().format('YYYY-MM-DD HH:mm:ss')
  }, 1000)
  loadEnterprises()
  loadUnread()
  // poll notifications every 60s
  setInterval(loadUnread, 60000)
})
onUnmounted(() => clearInterval(timer))

function navTo(path) {
  router.push(path).catch(err => {
    if (err.name !== 'NavigationDuplicated') console.warn('nav error:', err)
  })
}

async function handleCommand(cmd) {
  if (cmd === 'logout') {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '退出',
      cancelButtonText: '取消',
      type: 'warning'
    }).catch(() => {})
    authStore.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  }
}
</script>

<style scoped>
.app-container { height: 100vh; background: #0a1628; }

.sidebar {
  background: #0d1b2e;
  border-right: 1px solid #1a3a5c;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: 2px 0 8px rgba(0,0,0,0.1);
}

.logo-area {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 20px 16px;
  border-bottom: 1px solid #1a3a5c;
  background: linear-gradient(135deg, #0f2540, #0d1b2e);
}

.logo-icon { font-size: 28px; }

.logo-title {
  font-size: 14px;
  font-weight: 700;
  color: #00d4ff;
  letter-spacing: 1px;
}

.logo-sub {
  font-size: 11px;
  color: #5a7a9a;
  margin-top: 2px;
}

.nav-menu {
  flex: 1;
  border-right: none !important;
  padding: 8px 0;
}

.nav-menu :deep(.el-menu-item) {
  height: 48px;
  margin: 2px 8px;
  border-radius: 8px;
  transition: all 0.2s;
  color: #8bafc9;
}

.nav-menu :deep(.el-menu-item:hover) {
  background: rgba(0,212,255,0.1) !important;
  color: #00d4ff !important;
}

.nav-menu :deep(.el-menu-item.is-active) {
  background: linear-gradient(135deg, #00d4ff22, #00d4ff11) !important;
  border-left: 3px solid #00d4ff;
  color: #00d4ff !important;
  font-weight: 600;
}

.sidebar-footer {
  padding: 16px;
  border-top: 1px solid #1a3a5c;
}

.user-info {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.user-name {
  font-size: 12px;
  color: #8bafc9;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 110px;
}

.system-status {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #00d4ff;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #00d4ff;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}

.top-header {
  background: #0d1b2e;
  border-bottom: 1px solid #1a3a5c;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  height: 60px !important;
  box-shadow: 0 2px 6px rgba(0,0,0,0.2);
}

.header-left { display: flex; flex-direction: column; gap: 2px; }
.page-title { font-size: 18px; font-weight: 600; color: #e0f0ff; }
.header-left :deep(.el-breadcrumb) { font-size: 12px; }
.header-left :deep(.el-breadcrumb__item) { color: #8bafc9; }

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.time-display {
  font-size: 13px;
  color: #8bafc9;
  font-family: monospace;
}

.main-content {
  background: #0a1628;
  padding: 20px;
  overflow-y: auto;
}

.enterprise-info {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  color: #00d4ff;
  margin-bottom: 8px;
  padding: 6px 8px;
  background: rgba(0,212,255,0.08);
  border-radius: 6px;
  border: 1px solid rgba(0,212,255,0.2);
}
.ent-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 140px;
}

.demo-banner {
  background: linear-gradient(90deg, rgba(255,170,0,0.15), rgba(255,170,0,0.05));
  border-bottom: 1px solid rgba(255,170,0,0.3);
  padding: 8px 24px;
  font-size: 13px;
  color: #ffaa00;
  display: flex;
  align-items: center;
}

.demo-tag {
  animation: pulse-tag 2s infinite;
}
@keyframes pulse-tag {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.6; }
}

.notify-bell {
  font-size: 18px;
  color: #8bafc9;
  cursor: pointer;
  transition: color 0.2s;
}
.notify-bell:hover { color: #00d4ff; }

.fade-enter-active, .fade-leave-active { transition: opacity 0.2s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>

<style>
:root {
  --el-color-primary: #00d4ff;
  --el-color-primary-light-3: #33e0ff;
  --el-color-primary-light-5: #66e8ff;
  --el-color-primary-light-7: #99f0ff;
  --el-color-primary-light-8: #ccf5ff;
  --el-color-primary-light-9: #e6faff;
  --el-color-primary-dark-2: #00a8cc;
  --el-bg-color: #0d1b2e;
  --el-border-color: #1a3a5c;
  --el-fill-color-blank: #0d1b2e;
  --el-fill-color: rgba(0,212,255,0.1);
  --el-fill-color-light: rgba(0,212,255,0.05);
  --el-bg-color-overlay: #0f2540;
  --el-text-color-primary: #e0f0ff;
  --el-text-color-regular: #c0d8f0;
  --el-text-color-secondary: #8bafc9;
  --el-text-color-placeholder: #5a7a9a;
  --el-text-color-disabled: #4a6a7a;
  --el-border-color-light: #1a3a5c;
  --el-border-color-lighter: #2a4a6a;
  --el-disabled-bg-color: rgba(0,212,255,0.05);
  --el-disabled-text-color: #5a7a9a;
  --el-mask-color: rgba(0,0,0,0.4);
  --el-input-bg-color: #0d1b2e;
  --el-input-text-color: #e0f0ff;
  --el-input-placeholder-color: #5a7a9a;
  --el-select-option-hover-background: rgba(0,212,255,0.1);
  --el-select-input-color: #e0f0ff;
  --el-table-bg-color: #0d1b2e;
  --el-table-tr-bg-color: #0d1b2e;
  --el-table-header-bg-color: #0f2540;
  --el-table-row-hover-bg-color: rgba(0,212,255,0.1);
  --el-table-text-color: #c0d8f0;
  --el-table-header-text-color: #8bafc9;
  --el-table-border-color: #1a3a5c;
  --el-pagination-bg-color: transparent;
  --el-pagination-text-color: #8bafc9;
  --el-pagination-button-bg-color: rgba(0,212,255,0.05);
  --el-tag-info-color: #8bafc9;
  --el-descriptions-item-bordered-label-background: rgba(0,212,255,0.1);
  --el-box-shadow-light: 0 2px 12px rgba(0,0,0,0.2);
}
body { background: #0a1628; }
::-webkit-scrollbar { width: 6px; height: 6px; }
::-webkit-scrollbar-track { background: #0a1628; }
::-webkit-scrollbar-thumb { background: #1a3a5c; border-radius: 3px; }
::-webkit-scrollbar-thumb:hover { background: #00d4ff; }
</style>
