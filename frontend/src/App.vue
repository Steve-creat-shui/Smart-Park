<template>
  <el-container class="app-container">
    <el-aside width="220px" class="sidebar">
      <div class="logo-area">
        <div class="logo-icon">⚡</div>
        <div class="logo-text">
          <div class="logo-title">能源数字孪生</div>
          <div class="logo-sub">智慧建筑管理平台</div>
        </div>
      </div>
      <el-menu
        :default-active="$route.path"
        router
        background-color="#0d1b2e"
        text-color="#8bafc9"
        active-text-color="#00d4ff"
        class="nav-menu"
      >
        <el-menu-item index="/">
          <el-icon><DataBoard /></el-icon>
          <span>系统总览</span>
        </el-menu-item>
        <el-menu-item index="/energy-analysis">
          <el-icon><TrendCharts /></el-icon>
          <span>能耗分析</span>
        </el-menu-item>
        <el-menu-item index="/device-status">
          <el-icon><Monitor /></el-icon>
          <span>设备状态</span>
        </el-menu-item>
        <el-menu-item index="/ai-analysis">
          <el-icon><MagicStick /></el-icon>
          <span>AI 智能分析</span>
        </el-menu-item>
        <el-menu-item index="/report">
          <el-icon><Document /></el-icon>
          <span>报告生成</span>
        </el-menu-item>
      </el-menu>
      <div class="sidebar-footer">
        <div class="system-status">
          <span class="status-dot"></span>
          <span>系统运行正常</span>
        </div>
      </div>
    </el-aside>
    <el-container>
      <el-header class="top-header">
        <div class="header-left">
          <span class="page-title">{{ $route.meta.title }}</span>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ $route.meta.title }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-tag type="success" size="small" effect="dark">数字孪生模式</el-tag>
          <span class="time-display">{{ currentTime }}</span>
          <el-avatar size="small" style="background:#00d4ff; color:#0d1b2e">管理员</el-avatar>
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
import { ref, onMounted, onUnmounted } from 'vue'
import dayjs from 'dayjs'

const currentTime = ref(dayjs().format('YYYY-MM-DD HH:mm:ss'))
let timer = null
onMounted(() => {
  timer = setInterval(() => {
    currentTime.value = dayjs().format('YYYY-MM-DD HH:mm:ss')
  }, 1000)
})
onUnmounted(() => clearInterval(timer))
</script>

<style scoped>
.app-container { height: 100vh; background: #0a1628; }

.sidebar {
  background: #0d1b2e;
  border-right: 1px solid #1a3a5c;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.logo-area {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 20px 16px;
  border-bottom: 1px solid #1a3a5c;
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
  color: #8bafc9;
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
}

.nav-menu :deep(.el-menu-item.is-active) {
  background: linear-gradient(135deg, #0066cc22, #00d4ff22) !important;
  border-left: 3px solid #00d4ff;
}

.sidebar-footer {
  padding: 16px;
  border-top: 1px solid #1a3a5c;
}

.system-status {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #52c41a;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #52c41a;
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

.fade-enter-active, .fade-leave-active { transition: opacity 0.2s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>

<style>
:root {
  --el-color-primary: #00d4ff;
  --el-bg-color: #0d1b2e;
  --el-border-color: #1a3a5c;
}
body { background: #0a1628; }
::-webkit-scrollbar { width: 6px; height: 6px; }
::-webkit-scrollbar-track { background: #0a1628; }
::-webkit-scrollbar-thumb { background: #1a3a5c; border-radius: 3px; }
</style>
