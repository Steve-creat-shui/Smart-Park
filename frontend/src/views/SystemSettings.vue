<template>
  <div class="win-settings">

    <!-- 左侧导航栏 -->
    <div class="settings-nav">
      <div class="nav-brand">系统设置</div>
      <el-input v-model="searchText" placeholder="搜索设置项" :prefix-icon="Search"
        clearable size="small" class="nav-search" />
      <div class="nav-list">
        <div v-for="item in filteredMenu" :key="item.key"
             class="nav-item" :class="{ active: activeKey === item.key }"
             @click="activeKey = item.key">
          <el-icon :size="17"><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </div>
      </div>
    </div>

    <!-- 右侧内容区 -->
    <div class="settings-body">
      <div class="section-header">
        <el-icon :size="24" :style="{ color: current.color }"><component :is="current.icon" /></el-icon>
        <div>
          <div class="section-title">{{ current.label }}</div>
          <div class="section-desc">{{ current.desc }}</div>
        </div>
      </div>

      <!-- ① 系统概览 -->
      <div v-if="activeKey === 'overview'" class="section-body">
        <div class="overview-grid">
          <div class="ov-tile" v-for="t in overviewTiles" :key="t.label">
            <el-icon :size="28" :style="{ color: t.color }"><component :is="t.icon" /></el-icon>
            <div class="ov-val" :style="{ color: t.color }">{{ t.value }}</div>
            <div class="ov-label">{{ t.label }}</div>
          </div>
        </div>
        <div class="card-block">
          <div class="block-title">服务运行状态</div>
          <div class="status-row" v-for="s in serviceStatus" :key="s.name">
            <span class="svc-name">{{ s.name }}</span>
            <el-tag :type="s.ok ? 'success' : 'danger'" size="small" effect="dark">
              {{ s.ok ? '运行中' : '异常' }}
            </el-tag>
            <span class="svc-addr">{{ s.addr }}</span>
          </div>
        </div>
      </div>

      <!-- ② 告警阈值 -->
      <div v-if="activeKey === 'threshold'" class="section-body">
        <div class="card-block">
          <div class="block-title">能耗告警参数</div>
          <el-form :model="thresholds" label-position="top" class="settings-form">
            <el-form-item label="电力消耗异常倍数阈值">
              <el-input-number v-model="thresholds.powerMultiplier"
                :min="1.1" :max="5" :step="0.1" :precision="1" style="width:200px" />
              <span class="unit-label">× 均值触发告警</span>
            </el-form-item>
            <el-form-item label="水消耗预警上限 (m³/小时)">
              <el-input-number v-model="thresholds.waterLimit"
                :min="0" :max="10000" :step="10" style="width:200px" />
              <span class="unit-label">m³</span>
            </el-form-item>
            <el-form-item label="碳排放预警上限 (kg/小时)">
              <el-input-number v-model="thresholds.carbonLimit"
                :min="0" :max="100000" :step="100" style="width:200px" />
              <span class="unit-label">kg</span>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveThresholds">保存配置</el-button>
              <el-button plain @click="resetThresholds">恢复默认</el-button>
            </el-form-item>
          </el-form>
          <div class="tip-box" v-if="savedAt">
            <el-icon color="#52c41a"><CircleCheck /></el-icon>
            已保存于 {{ savedAt }}（存储于本地）
          </div>
        </div>
        <div class="card-block">
          <div class="block-title">当前阈值预览</div>
          <div class="preview-row" v-for="p in thresholdPreview" :key="p.label">
            <span class="pr-label">{{ p.label }}</span>
            <span class="pr-value" :style="{ color: p.color }">{{ p.value }}</span>
            <span class="pr-desc">{{ p.desc }}</span>
          </div>
        </div>
      </div>

      <!-- ③ 账户与安全 -->
      <div v-if="activeKey === 'account'" class="section-body">
        <div class="card-block">
          <div class="block-title">当前账户</div>
          <div class="account-row">
            <div class="avatar-circle">{{ (authStore.user?.nickname || 'U')[0] }}</div>
            <div class="account-info">
              <div class="acc-name">{{ authStore.user?.nickname || '未登录' }}</div>
              <el-tag :type="authStore.user?.role === 'admin' ? 'danger' : 'info'" size="small" effect="dark">
                {{ authStore.user?.role === 'admin' ? '系统管理员' : authStore.user?.role === 'enterprise' ? '企业用户' : '个人用户' }}
              </el-tag>
            </div>
          </div>
          <el-descriptions :column="2" border class="dark-desc" style="margin-top:16px">
            <el-descriptions-item label="手机号">{{ authStore.user?.phone || '—' }}</el-descriptions-item>
            <el-descriptions-item label="邮箱">{{ authStore.user?.email || '—' }}</el-descriptions-item>
            <el-descriptions-item label="用户名">{{ authStore.user?.username || '—' }}</el-descriptions-item>
            <el-descriptions-item label="最后登录">{{ loginTime }}</el-descriptions-item>
          </el-descriptions>
        </div>
        <div class="card-block">
          <div class="block-title">安全选项</div>
          <div class="setting-row">
            <div class="sr-left">
              <div class="sr-name">自动锁屏</div>
              <div class="sr-desc">无操作 30 分钟后自动退出登录</div>
            </div>
            <el-switch v-model="security.autoLock" @change="saveSecurity" />
          </div>
          <div class="setting-row">
            <div class="sr-left">
              <div class="sr-name">操作日志</div>
              <div class="sr-desc">记录所有用户操作到本地日志</div>
            </div>
            <el-switch v-model="security.opLog" @change="saveSecurity" />
          </div>
        </div>
      </div>

      <!-- ④ 通知偏好 -->
      <div v-if="activeKey === 'notification'" class="section-body">
        <div class="card-block">
          <div class="block-title">告警通知</div>
          <div class="setting-row" v-for="n in notifyItems" :key="n.key">
            <div class="sr-left">
              <div class="sr-name">{{ n.label }}</div>
              <div class="sr-desc">{{ n.desc }}</div>
            </div>
            <el-switch v-model="notify[n.key]" @change="saveNotify" />
          </div>
        </div>
        <div class="card-block">
          <div class="block-title">告警等级过滤</div>
          <el-checkbox-group v-model="notify.levels" @change="saveNotify">
            <el-checkbox label="warning" border size="small" style="margin:4px">⚠ 警告</el-checkbox>
            <el-checkbox label="error" border size="small" style="margin:4px">✕ 故障</el-checkbox>
            <el-checkbox label="info" border size="small" style="margin:4px">ℹ 信息</el-checkbox>
          </el-checkbox-group>
        </div>
      </div>

      <!-- ⑤ 数据管理 -->
      <div v-if="activeKey === 'data'" class="section-body">
        <div class="card-block">
          <div class="block-title">数据保留策略</div>
          <div class="setting-row">
            <div class="sr-left">
              <div class="sr-name">历史数据保留天数</div>
              <div class="sr-desc">超出天数的数据将不在图表中显示</div>
            </div>
            <el-select v-model="dataMgmt.retentionDays" style="width:120px" @change="saveDataMgmt">
              <el-option label="30 天" :value="30" />
              <el-option label="90 天" :value="90" />
              <el-option label="180 天" :value="180" />
              <el-option label="365 天" :value="365" />
              <el-option label="不限制" :value="0" />
            </el-select>
          </div>
          <div class="setting-row">
            <div class="sr-left">
              <div class="sr-name">默认导出格式</div>
              <div class="sr-desc">导出报告时的默认文件格式</div>
            </div>
            <el-radio-group v-model="dataMgmt.exportFormat" @change="saveDataMgmt">
              <el-radio-button label="xlsx">Excel</el-radio-button>
              <el-radio-button label="csv">CSV</el-radio-button>
              <el-radio-button label="pdf">PDF</el-radio-button>
            </el-radio-group>
          </div>
          <div class="setting-row">
            <div class="sr-left">
              <div class="sr-name">自动备份</div>
              <div class="sr-desc">每日凌晨 2:00 自动导出当日数据摘要</div>
            </div>
            <el-switch v-model="dataMgmt.autoBackup" @change="saveDataMgmt" />
          </div>
        </div>
        <div class="card-block">
          <div class="block-title">缓存管理</div>
          <div class="setting-row">
            <div class="sr-left">
              <div class="sr-name">本地缓存大小</div>
              <div class="sr-desc">当前 localStorage 占用</div>
            </div>
            <span style="color:#8bafc9;font-size:13px">{{ cacheSize }}</span>
          </div>
          <el-button size="small" plain style="margin-top:8px" @click="clearCache">清除本地缓存</el-button>
        </div>
      </div>

      <!-- ⑥ 界面设置 -->
      <div v-if="activeKey === 'display'" class="section-body">
        <div class="card-block">
          <div class="block-title">显示偏好</div>
          <div class="setting-row">
            <div class="sr-left">
              <div class="sr-name">侧边栏展开状态</div>
              <div class="sr-desc">默认展开左侧菜单</div>
            </div>
            <el-switch v-model="display.sidebarOpen" @change="saveDisplay" />
          </div>
          <div class="setting-row">
            <div class="sr-left">
              <div class="sr-name">图表动画</div>
              <div class="sr-desc">ECharts 图表加载动画效果</div>
            </div>
            <el-switch v-model="display.chartAnimation" @change="saveDisplay" />
          </div>
          <div class="setting-row">
            <div class="sr-left">
              <div class="sr-name">表格紧凑模式</div>
              <div class="sr-desc">减小表格行高，显示更多数据</div>
            </div>
            <el-switch v-model="display.compactTable" @change="saveDisplay" />
          </div>
          <div class="setting-row">
            <div class="sr-left">
              <div class="sr-name">数字精度</div>
              <div class="sr-desc">能耗数值显示的小数位数</div>
            </div>
            <el-radio-group v-model="display.precision" @change="saveDisplay">
              <el-radio-button :label="1">1 位</el-radio-button>
              <el-radio-button :label="2">2 位</el-radio-button>
              <el-radio-button :label="3">3 位</el-radio-button>
            </el-radio-group>
          </div>
        </div>
        <div class="card-block">
          <div class="block-title">仪表盘刷新</div>
          <div class="setting-row">
            <div class="sr-left">
              <div class="sr-name">自动刷新间隔</div>
              <div class="sr-desc">系统总览页面数据自动刷新周期</div>
            </div>
            <el-select v-model="display.refreshInterval" style="width:120px" @change="saveDisplay">
              <el-option label="关闭" :value="0" />
              <el-option label="30 秒" :value="30" />
              <el-option label="1 分钟" :value="60" />
              <el-option label="5 分钟" :value="300" />
            </el-select>
          </div>
        </div>
      </div>

      <!-- ⑦ 系统信息 -->
      <div v-if="activeKey === 'sysinfo'" class="section-body">
        <div class="card-block">
          <div class="block-title">平台信息</div>
          <el-descriptions :column="1" border class="dark-desc">
            <el-descriptions-item label="系统名称">鼎筑——建筑能源智能管理系统</el-descriptions-item>
            <el-descriptions-item label="版本号">v1.0.0</el-descriptions-item>
            <el-descriptions-item label="后端 API 地址">http://localhost:8080/api</el-descriptions-item>
            <el-descriptions-item label="前端地址">http://localhost:5173</el-descriptions-item>
            <el-descriptions-item label="技术栈">Vue 3 + Element Plus + ECharts + Spring Boot + MySQL</el-descriptions-item>
            <el-descriptions-item label="构建时间">2026-03</el-descriptions-item>
          </el-descriptions>
        </div>
        <div class="card-block">
          <div class="block-title">运行环境</div>
          <el-descriptions :column="2" border class="dark-desc">
            <el-descriptions-item label="浏览器">{{ browser }}</el-descriptions-item>
            <el-descriptions-item label="屏幕分辨率">{{ resolution }}</el-descriptions-item>
            <el-descriptions-item label="语言">{{ navigator.language }}</el-descriptions-item>
            <el-descriptions-item label="时区">{{ timezone }}</el-descriptions-item>
          </el-descriptions>
        </div>
      </div>

      <!-- ⑧ 关于 -->
      <div v-if="activeKey === 'about'" class="section-body">
        <div class="about-hero">
          <div class="about-logo">⚡</div>
          <div class="about-name">鼎筑——建筑能源智能管理系统</div>
          <div class="about-version">版本 v1.0.0 · 2026</div>
        </div>
        <div class="card-block">
          <div class="block-title">项目介绍</div>
          <p class="about-text">
            本系统为面向高校校园的建筑能源智能管理平台，集成能耗监测、异常告警、AI 智能分析、
            数据可视化及报告生成等功能，助力校园绿色节能目标实现。
          </p>
        </div>
        <div class="card-block">
          <div class="block-title">开源组件</div>
          <div class="dep-grid">
            <div class="dep-item" v-for="d in deps" :key="d.name">
              <span class="dep-name">{{ d.name }}</span>
              <span class="dep-ver">{{ d.ver }}</span>
            </div>
          </div>
        </div>
        <div class="card-block">
          <div class="block-title">许可</div>
          <p class="about-text">仅供教学与竞赛展示使用，未经授权不得用于商业目的。</p>
        </div>
      </div>

    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Search, Monitor, Warning, CircleCheck, InfoFilled, DataLine,
  User, Bell, Setting, FolderOpened, QuestionFilled, House
} from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth.js'
import dayjs from 'dayjs'

const authStore = useAuthStore()

// ── 导航 ──────────────────────────────────────────────
const searchText = ref('')
const activeKey = ref('overview')

const menuItems = [
  { key: 'overview',     label: '系统概览',   icon: 'House',          color: '#00d4ff', desc: '服务状态与运行统计' },
  { key: 'threshold',    label: '告警阈值',   icon: 'Warning',        color: '#faad14', desc: '配置能耗异常触发条件' },
  { key: 'account',      label: '账户与安全', icon: 'User',           color: '#52c41a', desc: '当前登录账户及安全选项' },
  { key: 'notification', label: '通知偏好',   icon: 'Bell',           color: '#9254de', desc: '告警通知方式与等级过滤' },
  { key: 'data',         label: '数据管理',   icon: 'FolderOpened',   color: '#36cfc9', desc: '数据保留、导出与缓存' },
  { key: 'display',      label: '界面设置',   icon: 'Monitor',        color: '#597ef7', desc: '图表、刷新与显示偏好' },
  { key: 'sysinfo',      label: '系统信息',   icon: 'InfoFilled',     color: '#409EFF', desc: '平台版本与运行环境' },
  { key: 'about',        label: '关于',       icon: 'QuestionFilled', color: '#ff7875', desc: '开源组件与许可声明' }
]

const filteredMenu = computed(() =>
  searchText.value
    ? menuItems.filter(m => m.label.includes(searchText.value))
    : menuItems
)

const current = computed(() => menuItems.find(m => m.key === activeKey.value) || menuItems[0])

// ── 系统概览 ──────────────────────────────────────────
const overviewTiles = ref([
  { label: '建筑总数',   value: '全7栋', icon: 'House',     color: '#00d4ff' },
  { label: '监测设备',   value: '48 台', icon: 'Setting',   color: '#52c41a' },
  { label: '今日告警',   value: '3 条',  icon: 'Warning',   color: '#faad14' },
  { label: '系统在线率', value: '99.8%', icon: 'Monitor',   color: '#36cfc9' }
])

const serviceStatus = ref([
  { name: '后端 API 服务',   ok: true,  addr: 'http://localhost:8080' },
  { name: '数据库 MySQL',    ok: true,  addr: 'localhost:3306 · energy_management' },
  { name: '前端 Vite Dev',   ok: true,  addr: 'http://localhost:5173' },
  { name: 'AI 分析服务',     ok: false, addr: 'Claude API（需配置密钥）' }
])

// ── 告警阈值 ──────────────────────────────────────────
const DEFAULT_THRESHOLDS = { powerMultiplier: 1.5, waterLimit: 500, carbonLimit: 20000 }
const STORAGE_KEY = 'dingzhu_thresholds'
const thresholds = ref({ ...DEFAULT_THRESHOLDS })
const savedAt = ref('')

function loadThresholds() {
  try {
    const saved = localStorage.getItem(STORAGE_KEY)
    if (saved) {
      const parsed = JSON.parse(saved)
      thresholds.value = { ...DEFAULT_THRESHOLDS, ...parsed.data }
      savedAt.value = parsed.savedAt || ''
    }
  } catch (e) {}
}

function saveThresholds() {
  const now = dayjs().format('YYYY-MM-DD HH:mm:ss')
  localStorage.setItem(STORAGE_KEY, JSON.stringify({ data: thresholds.value, savedAt: now }))
  savedAt.value = now
  ElMessage.success('阈值配置已保存')
}

function resetThresholds() {
  thresholds.value = { ...DEFAULT_THRESHOLDS }
  localStorage.removeItem(STORAGE_KEY)
  savedAt.value = ''
  ElMessage.info('已恢复默认配置')
}

const thresholdPreview = computed(() => [
  { label: '电力异常倍数', value: thresholds.value.powerMultiplier + '×', desc: '超出均值触发告警', color: '#faad14' },
  { label: '水消耗预警',   value: thresholds.value.waterLimit + ' m³',    desc: '单次记录超过触发', color: '#9254de' },
  { label: '碳排放预警',   value: thresholds.value.carbonLimit + ' kg',   desc: '单次记录超过触发', color: '#ff4d4f' }
])

// ── 账户与安全 ────────────────────────────────────────
const loginTime = dayjs().format('YYYY-MM-DD HH:mm')
const security = ref({ autoLock: false, opLog: true })
function saveSecurity() { localStorage.setItem('dingzhu_security', JSON.stringify(security.value)); ElMessage.success('安全设置已保存') }

// ── 通知偏好 ──────────────────────────────────────────
const notifyItems = [
  { key: 'pageAlert',  label: '页面弹窗告警', desc: '检测到异常时弹出 Element Plus 消息框' },
  { key: 'soundAlert', label: '声音提示',      desc: '告警时播放提示音（需浏览器权限）' },
  { key: 'badgeCount', label: '导航角标数量',  desc: '侧边栏菜单显示未读告警数' }
]
const notify = ref({ pageAlert: true, soundAlert: false, badgeCount: true, levels: ['warning', 'error'] })
function saveNotify() { localStorage.setItem('dingzhu_notify', JSON.stringify(notify.value)); ElMessage.success('通知设置已保存') }

// ── 数据管理 ──────────────────────────────────────────
const dataMgmt = ref({ retentionDays: 365, exportFormat: 'xlsx', autoBackup: false })
function saveDataMgmt() { localStorage.setItem('dingzhu_data', JSON.stringify(dataMgmt.value)); ElMessage.success('数据管理设置已保存') }

const cacheSize = computed(() => {
  let total = 0
  for (const k in localStorage) { if (localStorage.hasOwnProperty(k)) total += localStorage.getItem(k).length }
  return (total / 1024).toFixed(1) + ' KB'
})

function clearCache() {
  const keep = ['dingzhu_token', 'dingzhu_user']
  Object.keys(localStorage).forEach(k => { if (!keep.includes(k)) localStorage.removeItem(k) })
  ElMessage.success('本地缓存已清除')
}

// ── 界面设置 ──────────────────────────────────────────
const display = ref({ sidebarOpen: true, chartAnimation: true, compactTable: false, precision: 2, refreshInterval: 60 })
function saveDisplay() { localStorage.setItem('dingzhu_display', JSON.stringify(display.value)); ElMessage.success('界面设置已保存') }

// ── 系统信息 ──────────────────────────────────────────
const browser = navigator.userAgent.match(/(Chrome|Firefox|Safari|Edge)\/[\d.]+/)?.[0] || '未知'
const resolution = `${screen.width} × ${screen.height}`
const timezone = Intl.DateTimeFormat().resolvedOptions().timeZone

// ── 关于 ──────────────────────────────────────────────
const deps = [
  { name: 'Vue',           ver: '3.x' },
  { name: 'Element Plus',  ver: '2.x' },
  { name: 'ECharts',       ver: '5.x' },
  { name: 'Spring Boot',   ver: '3.2' },
  { name: 'MySQL',         ver: '8.0' },
  { name: 'Vite',          ver: '5.x' },
  { name: 'Pinia',         ver: '2.x' },
  { name: 'dayjs',         ver: '1.x' }
]

onMounted(() => {
  loadThresholds()
  try { security.value = { ...security.value, ...JSON.parse(localStorage.getItem('dingzhu_security') || '{}') } } catch (e) {}
  try { notify.value   = { ...notify.value,   ...JSON.parse(localStorage.getItem('dingzhu_notify') || '{}') } } catch (e) {}
  try { dataMgmt.value = { ...dataMgmt.value, ...JSON.parse(localStorage.getItem('dingzhu_data') || '{}') } } catch (e) {}
  try { display.value  = { ...display.value,  ...JSON.parse(localStorage.getItem('dingzhu_display') || '{}') } } catch (e) {}
})
</script>

<style scoped>
.win-settings {
  display: flex;
  height: 100%;
  min-height: 600px;
  gap: 0;
  color: #e0f0ff;
}

/* ── 左侧导航 ── */
.settings-nav {
  width: 220px;
  flex-shrink: 0;
  background: #0d1b2e;
  border-right: 1px solid #1a3a5c;
  display: flex;
  flex-direction: column;
  padding: 20px 0 12px;
}

.nav-brand {
  font-size: 16px;
  font-weight: 700;
  color: #e0f0ff;
  padding: 0 18px 14px;
  border-bottom: 1px solid #1a3a5c;
  margin-bottom: 10px;
}

.nav-search {
  margin: 0 12px 10px;
}
.nav-search :deep(.el-input__wrapper) {
  background: #0d1b2e;
  box-shadow: none;
  border: 1px solid #1a3a5c;
}
.nav-search :deep(.el-input__inner) { color: #e0f0ff; }

.nav-list { flex: 1; overflow-y: auto; padding: 0 8px; }

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 12px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 13px;
  color: #8bafc9;
  transition: all 0.15s;
  margin-bottom: 2px;
}
.nav-item:hover { background: rgba(0,212,255,0.1); color: #e0f0ff; }
.nav-item.active { background: rgba(0,212,255,0.2); color: #00d4ff; font-weight: 600; }

/* ── 右侧内容 ── */
.settings-body {
  flex: 1;
  overflow-y: auto;
  padding: 24px 28px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 14px;
  padding-bottom: 16px;
  border-bottom: 1px solid #1a3a5c;
  margin-bottom: 4px;
}
.section-title { font-size: 18px; font-weight: 700; color: #e0f0ff; }
.section-desc  { font-size: 12px; color: #5a7a9a; margin-top: 2px; }

.section-body { display: flex; flex-direction: column; gap: 16px; }

/* ── 通用 card-block ── */
.card-block {
  background: #0d1b2e;
  border: 1px solid #1a3a5c;
  border-radius: 10px;
  padding: 18px 20px;
  box-shadow: 0 2px 8px rgba(26,188,156,0.06);
}
.block-title {
  font-size: 13px;
  font-weight: 600;
  color: #8bafc9;
  margin-bottom: 14px;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

/* ── 设置行 ── */
.setting-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px solid #1a3a5c;
}
.setting-row:last-child { border-bottom: none; }
.sr-left { flex: 1; margin-right: 16px; }
.sr-name { font-size: 13px; color: #c0d8f0; margin-bottom: 2px; }
.sr-desc { font-size: 11px; color: #5a7a9a; }

/* ── 系统概览 ── */
.overview-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}
.ov-tile {
  background: #0f2540;
  border: 1px solid #1a3a5c;
  border-radius: 10px;
  padding: 18px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}
.ov-val   { font-size: 22px; font-weight: 700; }
.ov-label { font-size: 12px; color: #8bafc9; }

.status-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
  border-bottom: 1px solid #1a3a5c;
}
.status-row:last-child { border-bottom: none; }
.svc-name { width: 140px; font-size: 13px; color: #c0d8f0; }
.svc-addr { font-size: 11px; color: #5a7a9a; }

/* ── 账户 ── */
.account-row { display: flex; align-items: center; gap: 16px; }
.avatar-circle {
  width: 52px; height: 52px; border-radius: 50%;
  background: linear-gradient(135deg, #00d4ff33, #1a3a5c);
  border: 2px solid #00d4ff44;
  display: flex; align-items: center; justify-content: center;
  font-size: 22px; font-weight: 700; color: #00d4ff;
  flex-shrink: 0;
}
.acc-name { font-size: 16px; font-weight: 600; color: #e0f0ff; margin-bottom: 4px; }

/* ── 阈值预览行 ── */
.preview-row {
  display: flex; align-items: center; gap: 16px; padding: 8px 0;
  border-bottom: 1px solid #1a3a5c;
}
.preview-row:last-child { border-bottom: none; }
.pr-label { width: 120px; font-size: 12px; color: #8bafc9; }
.pr-value { font-size: 16px; font-weight: 700; width: 120px; }
.pr-desc  { font-size: 11px; color: #5a7a9a; }

/* ── 关于 ── */
.about-hero {
  text-align: center;
  padding: 28px 0 20px;
  border-bottom: 1px solid #1a3a5c;
  margin-bottom: 4px;
}
.about-logo    { font-size: 48px; margin-bottom: 10px; }
.about-name    { font-size: 17px; font-weight: 700; color: #e0f0ff; margin-bottom: 4px; }
.about-version { font-size: 12px; color: #5a7a9a; }
.about-text    { font-size: 13px; color: #8bafc9; line-height: 1.8; }

.dep-grid {
  display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px;
}
.dep-item {
  display: flex; justify-content: space-between; align-items: center;
  background: #0f2540; border-radius: 6px; padding: 8px 12px;
}
.dep-name { font-size: 12px; color: #c0d8f0; }
.dep-ver  { font-size: 11px; color: #5a7a9a; }

/* ── 表单 ── */
.settings-form :deep(.el-form-item__label) { color: #8bafc9; font-size: 13px; }
.unit-label { margin-left: 10px; color: #5a7a9a; font-size: 12px; }
.tip-box { display: flex; align-items: center; gap: 6px; margin-top: 12px; font-size: 12px; color: #52c41a; }

/* ── descriptions ── */
.dark-desc :deep(.el-descriptions__body)   { background: #0d1b2e; }
.dark-desc :deep(.el-descriptions__label)  { background: #0f2540; color: #8bafc9; font-size: 12px; width: 140px; }
.dark-desc :deep(.el-descriptions__content){ background: #0d1b2e; color: #c0d8f0; font-size: 13px; }
.dark-desc :deep(.el-descriptions__cell)   { border-color: #1a3a5c; }
</style>
