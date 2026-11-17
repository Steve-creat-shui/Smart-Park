<template>
  <div class="device-status">

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <el-select v-model="selectedBuilding" placeholder="选择建筑" style="width:160px" @change="onBuildingChange">
        <el-option v-for="id in buildingIds" :key="id" :label="nameMap[id] || id" :value="id" />
      </el-select>
      <el-radio-group v-model="statusFilter" size="default">
        <el-radio-button value="all">全部</el-radio-button>
        <el-radio-button value="normal">正常</el-radio-button>
        <el-radio-button value="warning">告警</el-radio-button>
        <el-radio-button value="error">故障</el-radio-button>
      </el-radio-group>
      <el-button type="primary" icon="Refresh" @click="onBuildingChange" :loading="loading">刷新</el-button>
      <div style="flex:1"></div>
      <span class="cur-bldg">当前监测：<b>{{ nameMap[selectedBuilding] || selectedBuilding }}</b></span>
    </div>

    <!-- 概览卡片 -->
    <div class="device-overview">
      <div class="overview-card" v-for="item in overviewItems" :key="item.label">
        <div class="ov-icon" :style="{ color: item.color }">
          <el-icon :size="24"><component :is="item.icon" /></el-icon>
        </div>
        <div class="ov-info">
          <div class="ov-value" :style="{ color: item.color }">{{ item.value }}</div>
          <div class="ov-label">{{ item.label }}</div>
        </div>
      </div>
    </div>

    <!-- 主内容区 -->
    <div class="main-grid">
      <!-- 设备列表 -->
      <div class="panel-card device-list-card">
        <div class="card-header">
          <span class="card-title">空调系统设备</span>
          <el-tag type="info" size="small">共 {{ filteredDevices.length }} 台</el-tag>
        </div>
        <div class="device-grid">
          <div class="device-item" v-for="device in filteredDevices" :key="device.id"
               :class="['status-' + device.status, selectedDevice?.id === device.id ? 'device-selected' : '']" @click="selectDevice(device)">
            <div class="device-icon"><el-icon :size="20"><WindPower /></el-icon></div>
            <div class="device-name">{{ device.name }}</div>
            <div class="device-floor">{{ device.floor }}</div>
            <div class="device-power">{{ device.power }} kW</div>
            <div class="device-status-badge" :class="'badge-' + device.status">{{ statusLabel(device.status) }}</div>
          </div>
        </div>
      </div>

      <!-- 右侧列 -->
      <div class="right-col">
        <!-- 功率图 -->
        <div class="panel-card">
          <div class="card-header"><span class="card-title">设备运行功率</span></div>
          <v-chart :option="devicePowerOption" style="height:200px" autoresize />
        </div>
        <!-- 设备详情（始终显示） -->
        <div class="panel-card detail-card">
          <div class="card-header">
            <span class="card-title">设备详情</span>
            <el-tag v-if="selectedDevice"
              :type="selectedDevice.status==='normal'?'success':selectedDevice.status==='warning'?'warning':'danger'"
              effect="dark" size="small">{{ statusLabel(selectedDevice.status) }}</el-tag>
          </div>
          <template v-if="selectedDevice">
            <div class="det-name-row">
              <el-icon :size="22" style="color:#00d4ff;flex-shrink:0"><WindPower /></el-icon>
              <div>
                <div class="det-name">{{ selectedDevice.name }}</div>
                <div class="det-floor">{{ selectedDevice.floor }}</div>
              </div>
            </div>
            <div class="det-metrics">
              <div class="dm" v-for="m in detailMetrics" :key="m.label">
                <div class="dm-val" :style="{color:m.color}">{{ m.value }}</div>
                <div class="dm-lbl">{{ m.label }}</div>
              </div>
            </div>
            <div class="det-actions">
              <el-button size="small" type="success" round @click="handleStart">启动</el-button>
              <el-button size="small" type="warning" round @click="handleAdjustTemp">调温</el-button>
              <el-button size="small" type="danger" round @click="handleShutdown">关机</el-button>
              <el-button size="small" type="info" round @click="handleMaintain">维护</el-button>
            </div>
          </template>
          <div v-else class="det-placeholder">
            <el-icon :size="40" style="color:#1a3a5c"><WindPower /></el-icon>
            <p>点击左侧设备卡片<br>查看详细运行信息</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部三列 -->
    <div class="bottom-grid">
      <!-- 楼层温湿度 -->
      <div class="panel-card temp-card">
        <div class="card-header">
          <span class="card-title">楼层温湿度</span>
          <el-tag type="info" size="small" effect="dark">实时</el-tag>
        </div>
        <div class="floor-temp-grid">
          <div class="ftc" v-for="f in floorTempData" :key="f.floor">
            <div class="ftc-floor">{{ f.floor }}</div>
            <div class="ftc-row">
              <span class="ftc-icon">🌡️</span>
              <div class="ftc-bar-wrap"><div class="ftc-bar" :style="{width:Math.min(100,(f.temp-15)/25*100)+'%',background:f.temp>28?'#ff6b6b':f.temp>26?'#faad14':'#52c41a'}"></div></div>
              <span class="ftc-val" :style="{color:f.temp>28?'#ff6b6b':f.temp>26?'#faad14':'#52c41a'}">{{ f.temp }}°C</span>
            </div>
            <div class="ftc-row">
              <span class="ftc-icon">💧</span>
              <div class="ftc-bar-wrap"><div class="ftc-bar" :style="{width:f.humidity+'%',background:f.humidity>70?'#faad14':'#36cfc9'}"></div></div>
              <span class="ftc-val" style="color:#36cfc9">{{ f.humidity }}%</span>
            </div>
            <el-tag :type="f.temp>28?'danger':f.temp>26?'warning':'success'" size="small" effect="dark" style="margin-top:4px">
              {{ f.temp>28?'超温':f.temp>26?'偏高':'正常' }}
            </el-tag>
          </div>
        </div>
      </div>

      <!-- 能耗趋势 -->
      <div class="panel-card trend-card">
        <div class="card-header"><span class="card-title">多设备24h能耗趋势</span></div>
        <div class="chart-fill">
          <v-chart :option="energyTrendOption" style="height:100%;width:100%" autoresize />
        </div>
      </div>

      <!-- 健康评分 + 告警 -->
      <div class="right-bottom-col">
        <div class="panel-card">
          <div class="card-header">
            <span class="card-title">设备健康评分</span>
            <el-tag type="success" size="small">{{ healthScores.filter(h=>h.score>=80).length }} 台优良</el-tag>
          </div>
          <div class="health-grid">
            <div class="health-item" v-for="h in healthScores" :key="h.id">
              <div style="position:relative;width:68px;height:68px">
                <v-chart :option="h.ringOption" style="height:68px;width:68px" autoresize />
                <div class="ring-center" :style="{color:h.color}">{{ h.score }}</div>
              </div>
              <div class="health-name">{{ h.shortName }}</div>
            </div>
          </div>
        </div>
        <div class="panel-card" style="flex:1">
          <div class="card-header">
            <span class="card-title">告警历史</span>
            <el-tag type="danger" size="small">{{ alertHistory.length }} 条</el-tag>
          </div>
          <el-table :data="alertHistory" size="small" :header-cell-style="headerStyle" :cell-style="cellStyle">
            <el-table-column prop="time" label="时间" width="100">
              <template #default="{row}"><span style="font-size:11px">{{ row.time.slice(5) }}</span></template>
            </el-table-column>
            <el-table-column prop="device" label="设备" />
            <el-table-column label="状态" width="72">
              <template #default="{row}">
                <el-tag :type="row.status==='已处理'?'success':row.status==='处理中'?'warning':'danger'" size="small">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { BarChart, LineChart, PieChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { buildingApi } from '../api/index.js'
import { loadBuildingNames, buildingNameMap } from '../utils/buildingName.js'

use([BarChart, LineChart, PieChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer])

const buildingIds = ref([])
const nameMap = reactive(buildingNameMap)
const selectedBuilding = ref('')
const statusFilter = ref('all')
const loading = ref(false)
const selectedDevice = ref(null)

const bldgCfg = {
  B001: { floors: 8,  ac: 6,  label: '行政楼' },
  B002: { floors: 5,  ac: 5,  label: '图书馆' },
  B003: { floors: 6,  ac: 7,  label: '实验楼' },
  B004: { floors: 10, ac: 9,  label: '教学楼' },
  B005: { floors: 12, ac: 10, label: '宿舍楼' },
  B006: { floors: 3,  ac: 4,  label: '餐厅'   },
  B007: { floors: 2,  ac: 5,  label: '体育馆' },
}

function seed(bid, i, lo, hi) {
  const s = bid.charCodeAt(3)
  const v = ((s * 31 + i * 17 + 7) % 97) / 97
  return parseFloat((lo + v * (hi - lo)).toFixed(1))
}

function mkDevices(bid) {
  const cfg = bldgCfg[bid] || { floors: 5, ac: 5, label: bid }
  const statusPat = ['normal','normal','normal','warning','normal','normal','error','normal','normal','warning']
  const devices = []
  for (let i = 0; i < cfg.ac; i++) {
    const fl = Math.max(1, Math.ceil((i + 1) * cfg.floors / cfg.ac))
    const st = statusPat[i % statusPat.length]
    const pw = st === 'error' ? 0 : seed(bid, i * 3, 12, 56)
    const cp = st === 'error' ? 0 : seed(bid, i * 5, 2.2, 3.9)
    const rt = seed(bid, i * 7, 24.0, 31.0)
    devices.push({
      id: i + 1,
      name: `${i < cfg.ac - 2 ? '中央空调' : '新风机组'}-${fl}F${i % 2 ? 'B' : 'A'}`,
      floor: `${fl}楼`,
      power: pw,
      status: st,
      setTemp: st === 'error' ? null : 26,
      roomTemp: st === 'error' ? null : rt,
      runtime: st === 'error' ? 0 : seed(bid, i * 11, 1.0, 10.0),
      todayEnergy: st === 'error' ? 0 : parseFloat((pw * seed(bid, i * 9, 6, 10)).toFixed(1)),
      cop: cp
    })
  }
  return devices
}

function mkFloors(bid) {
  const cfg = bldgCfg[bid] || { floors: 5, ac: 5, label: bid }
  const n = Math.min(cfg.floors, 6)
  return Array.from({ length: n }, (_, i) => ({
    floor: `${i + 1}楼`,
    temp: seed(bid, i * 31, 24.0, 31.0),
    humidity: Math.round(seed(bid, i * 37, 55, 78))
  }))
}

const allDevices = ref([])

const filteredDevices = computed(() => {
  if (statusFilter.value === 'all') return allDevices.value
  return allDevices.value.filter(d => d.status === statusFilter.value)
})

const overviewItems = computed(() => [
  { label: '设备总数', value: allDevices.value.length, icon: 'Setting', color: '#00d4ff' },
  { label: '运行中', value: allDevices.value.filter(d => d.status === 'normal').length, icon: 'CircleCheck', color: '#52c41a' },
  { label: '告警设备', value: allDevices.value.filter(d => d.status === 'warning').length, icon: 'WarningFilled', color: '#fadb14' },
  { label: '故障设备', value: allDevices.value.filter(d => d.status === 'error').length, icon: 'CircleClose', color: '#ff6b6b' },
  { label: '总运行功率', value: allDevices.value.reduce((s, d) => s + d.power, 0).toFixed(1) + ' kW', icon: 'Lightning', color: '#8b5cf6' }
])

const devicePowerOption = computed(() => ({
  backgroundColor: 'transparent',
  tooltip: { trigger: 'axis', backgroundColor: '#0d1b2e', borderColor: '#1a3a5c', textStyle: { color: '#e0f0ff' } },
  grid: { top: 10, bottom: 40, left: 90, right: 20 },
  xAxis: { type: 'value', axisLabel: { color: '#8bafc9' }, splitLine: { lineStyle: { color: '#1a3a5c' } } },
  yAxis: {
    type: 'category',
    data: allDevices.value.map(d => d.name),
    axisLabel: { color: '#8bafc9', fontSize: 10 }
  },
  series: [{
    type: 'bar',
    data: allDevices.value.map(d => ({
      value: d.power,
      itemStyle: { color: d.status === 'error' ? '#ff6b6b' : d.status === 'warning' ? '#fadb14' : '#00d4ff', borderRadius: [0, 4, 4, 0] }
    })),
    barMaxWidth: 16
  }]
}))

const floorTempData = ref([])

const trendColors = ['#00d4ff','#52c41a','#fadb14','#ff7c3c']
const seeded = (base, i) => parseFloat((base * (i < 6 ? 0.2 : i < 9 ? 0.7 : i < 18 ? 0.95 : i < 22 ? 0.55 : 0.2) + (Math.sin(i * 1.3 + base) * base * 0.08)).toFixed(1))
const trendDevices = computed(() => allDevices.value.slice(0, 4).map((d, idx) => ({ name: d.name, color: trendColors[idx], base: Math.max(8, d.power) })))

const energyTrendOption = computed(() => ({
  backgroundColor: 'transparent',
  tooltip: { trigger: 'axis', backgroundColor: '#0d1b2e', borderColor: '#1a3a5c', textStyle: { color: '#e0f0ff' } },
  legend: { top: 0, right: 0, textStyle: { color: '#8bafc9', fontSize: 10 } },
  grid: { top: 30, bottom: 30, left: 45, right: 10 },
  xAxis: { type: 'category', data: Array.from({length: 24}, (_, i) => `${i}:00`), axisLabel: { color: '#8bafc9', fontSize: 9, interval: 3 }, axisLine: { lineStyle: { color: '#1a3a5c' } } },
  yAxis: { type: 'value', name: 'kW', nameTextStyle: { color: '#8bafc9', fontSize: 10 }, axisLabel: { color: '#8bafc9' }, splitLine: { lineStyle: { color: '#1a3a5c' } } },
  series: trendDevices.value.map(d => ({
    name: d.name, type: 'line', smooth: true, symbol: 'none',
    data: Array.from({length: 24}, (_, i) => seeded(d.base, i)),
    itemStyle: { color: d.color },
    areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: d.color + '44' }, { offset: 1, color: d.color + '00' }] } }
  }))
}))

const healthScores = computed(() => allDevices.value.map((d, i) => {
  let score = d.status === 'error' ? 12 : d.status === 'warning' ? 55 : Math.round(Math.max(70, 98 - (d.cop && d.cop < 3 ? (3 - d.cop) * 15 : 0) - (i % 3 === 0 ? 5 : 0)))
  const color = score >= 80 ? '#52c41a' : score >= 60 ? '#fadb14' : '#ff6b6b'
  return {
    id: d.id,
    shortName: d.name.slice(-4),
    score,
    color,
    ringOption: {
      backgroundColor: 'transparent',
      series: [{ type: 'pie', radius: ['55%', '82%'], center: ['50%', '50%'],
        data: [{ value: score, itemStyle: { color } }, { value: 100 - score, itemStyle: { color: '#1a3a5c' } }],
        label: { show: false }, labelLine: { show: false } }]
    }
  }
}))

const alertHistory = ref([
  { time: '2026-03-30 09:45', device: '分体空调-4FB', type: '设备故障',  value: '0 kW',    status: '处理中' },
  { time: '2026-03-30 08:23', device: '中央空调-3F',  type: '高功率告警', value: '52.1 kW',  status: '已处理' },
  { time: '2026-03-30 07:12', device: '新风机组-3F',  type: '温度过高',  value: '27.5°C',  status: '未处理' },
  { time: '2026-03-29 22:40', device: '中央空调-3F',  type: '效率低下',  value: 'COP 2.4', status: '已处理' },
  { time: '2026-03-29 15:30', device: '分体空调-4FB', type: '断电告警',  value: '0 kW',    status: '已处理' },
  { time: '2026-03-29 11:20', device: '新风机组-3F',  type: '滤网堵塞',  value: '9.2 kW',  status: '未处理' },
  { time: '2026-03-28 16:45', device: '中央空调-1F',  type: '功率波动',  value: '45.2 kW', status: '已处理' },
])

const headerStyle = () => ({ background: '#0f2540', color: '#8bafc9', borderBottom: '1px solid #1a3a5c' })
const cellStyle   = () => ({ background: '#0d1b2e', color: '#c0d8f0', borderBottom: '1px solid #1a3a5c' })

const detailMetrics = computed(() => {
  const d = selectedDevice.value
  if (!d) return []
  return [
    { label: '当前功率',   value: d.power + ' kW',        color: '#00d4ff' },
    { label: '设定温度',   value: d.setTemp ? d.setTemp + '°C' : '—', color: '#e0f0ff' },
    { label: '室内温度',   value: d.roomTemp ? d.roomTemp + '°C' : '—', color: d.roomTemp > 28 ? '#ff6b6b' : '#52c41a' },
    { label: '运行时长',   value: d.runtime + ' h',       color: '#8b5cf6' },
    { label: '今日能耗',   value: d.todayEnergy + ' kWh', color: '#52c41a' },
    { label: '能效比COP',  value: d.cop || '—',           color: d.cop >= 3 ? '#52c41a' : '#ff8c00' }
  ]
})

function statusLabel(s) {
  return { normal: '正常', warning: '告警', error: '故障' }[s] || s
}

function selectDevice(device) {
  selectedDevice.value = device
}

async function handleStart() {
  if (!selectedDevice.value) return
  if (selectedDevice.value.status === 'normal') {
    ElMessage({ type: 'info', message: `${selectedDevice.value.name} 已处于运行状态` })
    return
  }
  ElMessageBox.confirm(`确认启动 ${selectedDevice.value.name}？`, '启动确认', {
    confirmButtonText: '确认启动', cancelButtonText: '取消', type: 'success'
  }).then(() => {
    selectedDevice.value.status = 'normal'
    selectedDevice.value.power = parseFloat((Math.random() * 30 + 15).toFixed(1))
    allDevices.value = allDevices.value.map(d => d.id === selectedDevice.value.id ? { ...selectedDevice.value } : d)
    ElMessage({ type: 'success', message: `${selectedDevice.value.name} 已成功启动` })
  }).catch(() => {})
}

function handleAdjustTemp() {
  if (!selectedDevice.value) return
  ElMessageBox.prompt('请输入目标温度（18~30℃）', '调节温度', {
    confirmButtonText: '确认', cancelButtonText: '取消',
    inputValue: String(selectedDevice.value.setTemp ?? 26),
    inputPattern: /^(1[89]|2[0-9]|30)$/,
    inputErrorMessage: '请输入 18~30 之间的整数'
  }).then(({ value }) => {
    selectedDevice.value.setTemp = Number(value)
    allDevices.value = allDevices.value.map(d => d.id === selectedDevice.value.id ? { ...selectedDevice.value } : d)
    ElMessage({ type: 'success', message: `${selectedDevice.value.name} 设定温度已调整为 ${value}℃` })
  }).catch(() => {})
}

function handleShutdown() {
  if (!selectedDevice.value) return
  if (selectedDevice.value.status === 'error') {
    ElMessage({ type: 'info', message: `${selectedDevice.value.name} 已处于停机状态` })
    return
  }
  ElMessageBox.confirm(`确认关闭 ${selectedDevice.value.name}？此操作将停止设备运行。`, '关机确认', {
    confirmButtonText: '确认关机', cancelButtonText: '取消', type: 'warning'
  }).then(() => {
    selectedDevice.value.status = 'error'
    selectedDevice.value.power = 0
    selectedDevice.value.runtime = 0
    allDevices.value = allDevices.value.map(d => d.id === selectedDevice.value.id ? { ...selectedDevice.value } : d)
    ElMessage({ type: 'warning', message: `${selectedDevice.value.name} 已关机` })
  }).catch(() => {})
}

function handleMaintain() {
  if (!selectedDevice.value) return
  ElMessageBox.confirm(`将 ${selectedDevice.value.name} 切换至维护模式？`, '维护模式', {
    confirmButtonText: '确认', cancelButtonText: '取消', type: 'info'
  }).then(() => {
    selectedDevice.value.status = 'warning'
    allDevices.value = allDevices.value.map(d => d.id === selectedDevice.value.id ? { ...selectedDevice.value } : d)
    ElMessage({ type: 'info', message: `${selectedDevice.value.name} 已进入维护模式` })
  }).catch(() => {})
}

function onBuildingChange() {
  loading.value = true
  allDevices.value = mkDevices(selectedBuilding.value)
  floorTempData.value = mkFloors(selectedBuilding.value)
  selectedDevice.value = allDevices.value[0]
  setTimeout(() => { loading.value = false }, 350)
}

onMounted(async () => {
  try {
    await loadBuildingNames()
    const res = await buildingApi.getAllBuildingIds()
    if (res.code === 200) buildingIds.value = res.data
  } catch {
    buildingIds.value = []
  }
  if (buildingIds.value.length) selectedBuilding.value = buildingIds.value[0]
  onBuildingChange()
})
</script>

<style scoped>
.device-status { display: flex; flex-direction: column; gap: 14px; }

/* ── 筛选栏 ── */
.filter-bar {
  background: #0d1b2e;
  border: 1px solid #1a3a5c;
  border-radius: 12px;
  padding: 12px 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.2);
}
.cur-bldg { font-size: 13px; color: #8bafc9; white-space: nowrap; }
.cur-bldg b { color: #00d4ff; }

/* ── 概览卡片 ── */
.device-overview {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 12px;
}
.overview-card {
  background: #0d1b2e;
  border: 1px solid #1a3a5c;
  border-radius: 12px;
  padding: 14px 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  box-shadow: 0 2px 8px rgba(26,188,156,0.06);
}
.ov-icon { flex-shrink: 0; }
.ov-value { font-size: 20px; font-weight: 700; }
.ov-label { font-size: 12px; color: #8bafc9; margin-top: 2px; }

/* ── 主内容网格：设备列表 + 右侧列 ── */
.main-grid {
  display: grid;
  grid-template-columns: 1fr 340px;
  gap: 14px;
}
.right-col { display: flex; flex-direction: column; gap: 12px; height: 100%; }

/* ── 通用卡片 ── */
.panel-card {
  background: #0d1b2e;
  border: 1px solid #1a3a5c;
  border-radius: 12px;
  padding: 14px 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.2);
}
.card-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.card-title { font-size: 14px; font-weight: 600; color: #e0f0ff; }

/* ── 设备列表卡片 ── */
.device-list-card { display: flex; flex-direction: column; }
.device-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  grid-auto-rows: 1fr;
  gap: 10px;
  flex: 1;
  min-height: 0;
}
.device-item {
  background: #0f2540;
  border: 1px solid #1a3a5c;
  border-radius: 10px;
  padding: 12px 10px;
  cursor: pointer;
  transition: all 0.2s;
  text-align: center;
  position: relative;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  height: 100%;
  box-sizing: border-box;
}
.device-item::before { content: ''; position: absolute; top: 0; left: 0; right: 0; height: 3px; background: #00d4ff; }
.device-item:hover { border-color: #00d4ff55; transform: translateY(-2px); box-shadow: 0 4px 16px rgba(0,212,255,0.2); }
.device-item.device-selected { border-color: #00d4ff88; background: #0f2540; }
.device-item.status-warning { border-color: #fadb1455; }
.device-item.status-warning::before { background: #fadb14; }
.device-item.status-error { border-color: #ff6b6b55; }
.device-item.status-error::before { background: #ff6b6b; }
.device-icon { color: #00d4ff; margin-bottom: 6px; }
.device-name { font-size: 12px; color: #e0f0ff; font-weight: 600; margin-bottom: 2px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.device-floor { font-size: 10px; color: #5a7a9a; margin-bottom: 4px; }
.device-power { font-size: 15px; font-weight: 700; color: #00d4ff; margin: 4px 0; }
.device-status-badge { font-size: 10px; padding: 2px 8px; border-radius: 10px; display: inline-block; }
.badge-normal { background: #52c41a22; color: #52c41a; }
.badge-warning { background: #fadb1422; color: #fadb14; }
.badge-error { background: #ff6b6b22; color: #ff6b6b; }

/* ── 设备详情卡片 ── */
.detail-card { display: flex; flex-direction: column; flex: 1; min-height: 0; }
.det-name-row { display: flex; align-items: center; gap: 10px; padding: 10px 0 12px; border-bottom: 1px solid #1a3a5c; margin-bottom: 12px; }
.det-name { font-size: 15px; font-weight: 700; color: #e0f0ff; }
.det-floor { font-size: 11px; color: #8bafc9; margin-top: 2px; }
.det-metrics {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin-bottom: 14px;
}
.dm { background: #0f2540; border-radius: 8px; padding: 10px 8px; text-align: center; }
.dm-val { font-size: 16px; font-weight: 700; margin-bottom: 3px; }
.dm-lbl { font-size: 10px; color: #8bafc9; }
.det-actions { display: flex; gap: 8px; flex-wrap: wrap; margin-top: auto; }
.det-placeholder { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 10px; color: #5a7a9a; font-size: 13px; text-align: center; padding: 20px 0; }

/* ── 底部三列 ── */
.bottom-grid {
  display: grid;
  grid-template-columns: 1fr 1.6fr 1fr;
  gap: 14px;
}
.right-bottom-col { display: flex; flex-direction: column; gap: 12px; height: 100%; }

/* ── 温湿度 ── */
.temp-card { display: flex; flex-direction: column; }
.floor-temp-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  grid-auto-rows: 1fr;
  gap: 8px;
  flex: 1;
  min-height: 0;
}
.ftc {
  background: #0f2540;
  border: 1px solid #1a3a5c;
  border-radius: 8px;
  padding: 10px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}
.ftc-floor { font-size: 11px; font-weight: 600; color: #e0f0ff; margin-bottom: 6px; }
.ftc-row { display: flex; align-items: center; gap: 5px; margin-bottom: 5px; }
.ftc-icon { font-size: 11px; flex-shrink: 0; }
.ftc-bar-wrap { flex: 1; height: 4px; background: #1a3a5c; border-radius: 2px; overflow: hidden; }
.ftc-bar { height: 100%; border-radius: 2px; transition: width 0.5s; }
.ftc-val { font-size: 10px; font-weight: 600; min-width: 34px; text-align: right; }

/* ── 能耗趋势 ── */
.trend-card { display: flex; flex-direction: column; }
.chart-fill { flex: 1; min-height: 200px; }

/* ── 健康评分 ── */
.health-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px; }
.health-item { display: flex; flex-direction: column; align-items: center; gap: 3px; }
.ring-center { position: absolute; top: 50%; left: 50%; transform: translate(-50%,-50%); font-size: 12px; font-weight: 700; }
.health-name { font-size: 9px; color: #8bafc9; text-align: center; }
</style>
