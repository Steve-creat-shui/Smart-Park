<template>
  <div class="dashboard">
    <div class="stats-grid">
      <div class="stat-card" v-for="stat in statCards" :key="stat.key">
        <div class="stat-icon" :style="{ background: stat.bgColor }">
          <el-icon :size="22"><component :is="stat.icon" /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stat.value }}</div>
          <div class="stat-label">{{ stat.label }}</div>
        </div>
        <div class="stat-trend" :class="stat.trendUp ? 'up' : 'down'">
          <el-icon><component :is="stat.trendUp ? 'ArrowUp' : 'ArrowDown'" /></el-icon>
          {{ stat.trend }}
        </div>
      </div>
    </div>

    <div class="chart-row">
      <div class="chart-card large">
        <div class="card-header">
          <span class="card-title">月度能耗趋势</span>
          <el-select v-model="selectedBuilding" size="small" style="width:160px" @change="loadMonthlyTrend">
            <el-option v-for="id in buildingIds" :key="id" :label="nameMap[id] || id" :value="id" />
          </el-select>
        </div>
        <v-chart :option="trendOption" style="height:280px" autoresize />
      </div>
      <div class="chart-card">
        <div class="card-header">
          <span class="card-title">能耗构成分析</span>
        </div>
        <v-chart :option="pieOption" style="height:280px" autoresize />
      </div>
    </div>

    <div class="chart-row">
      <div class="chart-card">
        <div class="card-header">
          <span class="card-title">建筑能耗排行</span>
        </div>
        <v-chart :option="rankingOption" style="height:260px" autoresize />
      </div>
      <div class="chart-card large">
        <div class="card-header">
          <span class="card-title">实时能耗监控</span>
          <el-tag size="small" type="success" effect="dark">实时更新</el-tag>
        </div>
        <v-chart :option="realtimeOption" style="height:260px" autoresize />
      </div>
    </div>

    <div class="alert-section">
      <div class="card-header">
        <span class="card-title">异常告警</span>
        <el-badge :value="anomalyList.length" type="danger">
          <el-icon><Warning /></el-icon>
        </el-badge>
      </div>
      <el-table :data="anomalyList.slice(0,5)" size="small" style="background:transparent"
                :header-cell-style="tableHeaderStyle" :cell-style="tableCellStyle">
        <el-table-column prop="buildingId" label="建筑" width="120">
          <template #default="{ row }">{{ nameMap[row.buildingId] || row.buildingId }}</template>
        </el-table-column>
        <el-table-column prop="timestamp" label="告警时间" />
        <el-table-column prop="powerConsumptionKwh" label="能耗(kWh)" width="120">
          <template #default="{ row }">
            <span style="color:#ff6b6b">{{ row.powerConsumptionKwh?.toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'normal' ? 'success' : 'danger'" size="small">
              {{ row.status === 'normal' ? '正常' : '异常' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, computed } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { LineChart, BarChart, PieChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent, TitleComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { dashboardApi, energyApi, buildingApi } from '../api/index.js'
import { loadBuildingNames, getBuildingName, buildingNameMap } from '../utils/buildingName.js'
import { ElMessage } from 'element-plus'

use([LineChart, BarChart, PieChart, GridComponent, TooltipComponent, LegendComponent, TitleComponent, CanvasRenderer])

const stats = ref({ buildingCount: 0, monthlyPowerConsumption: 0, monthlyCarbonEmission: 0, alertCount: 0 })
const buildingIds = ref([])
const selectedBuilding = ref('')
const nameMap = reactive(buildingNameMap)
const anomalyList = ref([])
const monthlyData = ref({ months: [], powerConsumption: [], waterConsumption: [], carbonEmission: [], acConsumption: [] })
const realtimeData = ref([])
let realtimeTimer = null

const statCards = computed(() => [
  { key: 'buildings', label: '监测建筑数', value: '全' + stats.value.buildingCount + '栋', icon: 'OfficeBuilding', bgColor: '#00d4ff22', trend: '较上月', trendUp: true },
  { key: 'power', label: '本月电力消耗', value: stats.value.monthlyPowerConsumption.toFixed(1) + ' kWh', icon: 'Lightning', bgColor: '#52c41a22', trend: '-5.2%', trendUp: false },
  { key: 'carbon', label: '本月碳排放量', value: stats.value.monthlyCarbonEmission.toFixed(1) + ' kg', icon: 'Cloudy', bgColor: '#fadb1422', trend: '-3.1%', trendUp: false },
  { key: 'alerts', label: '异常告警', value: stats.value.alertCount + ' 条', icon: 'Warning', bgColor: '#ff6b6b22', trend: '需处理', trendUp: true }
])

const trendOption = computed(() => ({
  backgroundColor: 'transparent',
  tooltip: { trigger: 'axis', backgroundColor: '#0d1b2e', borderColor: '#1a3a5c', textStyle: { color: '#e0f0ff' } },
  legend: { textStyle: { color: '#8bafc9' }, top: 0 },
  grid: { top: 40, bottom: 30, left: 50, right: 60 },
  xAxis: { type: 'category', data: monthlyData.value.months, axisLine: { lineStyle: { color: '#1a3a5c' } }, axisLabel: { color: '#8bafc9' } },
  yAxis: [
    { type: 'value', name: 'kWh/kg', nameTextStyle: { color: '#8bafc9', fontSize: 10 }, axisLine: { lineStyle: { color: '#1a3a5c' } }, axisLabel: { color: '#8bafc9' }, splitLine: { lineStyle: { color: '#1a3a5c' } } },
    { type: 'value', name: 'm³', nameTextStyle: { color: '#00d4ff', fontSize: 10 }, axisLine: { lineStyle: { color: '#00d4ff' } }, axisLabel: { color: '#00d4ff' }, splitLine: { show: false } }
  ],
  series: [
    { name: '电力消耗(kWh)', yAxisIndex: 0, type: 'line', data: monthlyData.value.powerConsumption, smooth: true, itemStyle: { color: '#00d4ff' }, areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: '#00d4ff33' }, { offset: 1, color: '#00d4ff00' }] } } },
    { name: '碳排放(kg)', yAxisIndex: 0, type: 'line', data: monthlyData.value.carbonEmission, smooth: true, itemStyle: { color: '#52c41a' }, areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: '#52c41a33' }, { offset: 1, color: '#52c41a00' }] } } },
    { name: '空调消耗(kWh)', yAxisIndex: 0, type: 'line', data: monthlyData.value.acConsumption, smooth: true, lineStyle: { type: 'dotted', width: 2 }, itemStyle: { color: '#ff7c3c' }, areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: '#ff7c3c33' }, { offset: 1, color: '#ff7c3c00' }] } } },
    { name: '水消耗(m³)', yAxisIndex: 1, type: 'line', data: monthlyData.value.waterConsumption, smooth: true, lineStyle: { type: 'dashed', width: 2 }, itemStyle: { color: '#9254de' }, areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: '#9254de44' }, { offset: 1, color: '#9254de00' }] } } }
  ]
}))

const pieOption = computed(() => {
  const m = monthlyData.value
  const totalPower = (m.powerConsumption || []).reduce((s, v) => s + v, 0)
  const totalAc = (m.acConsumption || []).reduce((s, v) => s + v, 0)
  const totalWater = (m.waterConsumption || []).reduce((s, v) => s + v, 0)
  const otherPower = Math.max(0, totalPower - totalAc)
  return {
    backgroundColor: 'transparent',
    tooltip: { trigger: 'item', backgroundColor: '#0d1b2e', borderColor: '#1a3a5c', textStyle: { color: '#e0f0ff' } },
    legend: { orient: 'vertical', right: 10, top: 'center', textStyle: { color: '#8bafc9' } },
    series: [{
      type: 'pie', radius: ['40%', '70%'], center: ['40%', '50%'],
      data: [
        { value: totalAc.toFixed(1), name: '空调系统', itemStyle: { color: '#00d4ff' } },
        { value: otherPower.toFixed(1), name: '其他电力', itemStyle: { color: '#52c41a' } },
        { value: totalWater.toFixed(1), name: '水消耗', itemStyle: { color: '#fadb14' } }
      ].filter(d => Number(d.value) > 0),
      label: { color: '#8bafc9' }
    }]
  }
})

const rankingOption = computed(() => {
  const ranking = stats.value.buildingRanking || []
  return {
    backgroundColor: 'transparent',
    tooltip: { trigger: 'axis', backgroundColor: '#0d1b2e', borderColor: '#1a3a5c', textStyle: { color: '#e0f0ff' } },
    grid: { top: 10, bottom: 30, left: 80, right: 20 },
    xAxis: { type: 'value', axisLabel: { color: '#8bafc9' }, splitLine: { lineStyle: { color: '#1a3a5c' } } },
    yAxis: { type: 'category', data: ranking.map(r => getBuildingName(r.buildingId)).reverse(), axisLabel: { color: '#8bafc9' } },
    series: [{
      type: 'bar', data: ranking.map(r => r.totalPower).reverse(),
      itemStyle: { color: { type: 'linear', x: 0, y: 0, x2: 1, y2: 0, colorStops: [{ offset: 0, color: '#00a8cc' }, { offset: 1, color: '#00d4ff' }] }, borderRadius: [0, 4, 4, 0] },
      barMaxWidth: 20
    }]
  }
})

const realtimeOption = computed(() => ({
  backgroundColor: 'transparent',
  tooltip: { trigger: 'axis', backgroundColor: '#0d1b2e', borderColor: '#1a3a5c', textStyle: { color: '#e0f0ff' } },
  grid: { top: 20, bottom: 30, left: 50, right: 20 },
  xAxis: { type: 'category', data: realtimeData.value.map(d => d.time), axisLabel: { color: '#8bafc9', fontSize: 10 }, axisLine: { lineStyle: { color: '#1a3a5c' } } },
  yAxis: { type: 'value', axisLabel: { color: '#8bafc9' }, splitLine: { lineStyle: { color: '#1a3a5c' } } },
  series: [{
    type: 'line', data: realtimeData.value.map(d => d.value), smooth: true,
    symbol: 'none', itemStyle: { color: '#fadb14' },
    lineStyle: { width: 2, color: '#fadb14' },
    areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: '#fadb1433' }, { offset: 1, color: '#fadb1400' }] } }
  }]
}))

const tableHeaderStyle = () => ({ background: '#0f2540', color: '#8bafc9', borderBottom: '1px solid #1a3a5c', padding: '8px 0' })
const tableCellStyle = () => ({ background: '#0d1b2e', color: '#c0d8f0', borderBottom: '1px solid #1a3a5c', padding: '8px 0' })

function generateRealtimePoint() {
  const now = new Date()
  const time = `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}:${now.getSeconds().toString().padStart(2, '0')}`
  return { time, value: (Math.random() * 50 + 180).toFixed(1) }
}

async function loadDashboard() {
  try {
    await loadBuildingNames()
    const res = await dashboardApi.getStats()
    if (res.code === 200) {
      stats.value = res.data
      buildingIds.value = res.data.buildingIds || []
      if (buildingIds.value.length > 0) {
        selectedBuilding.value = buildingIds.value[0]
        loadMonthlyTrend()
        loadAnomalies()
      }
    }
  } catch {
    ElMessage.warning('后端服务未启动，显示演示数据')
    loadDemoData()
  }
}

function loadDemoData() {
  stats.value = { buildingCount: 7, monthlyPowerConsumption: 52360.5, monthlyCarbonEmission: 41103.1, alertCount: 3, buildingRanking: [
    { buildingId: 'B001', totalPower: 12500 }, { buildingId: 'B002', totalPower: 9800 },
    { buildingId: 'B003', totalPower: 8200 }, { buildingId: 'B004', totalPower: 7100 },
    { buildingId: 'B007', totalPower: 5800 }
  ]}
  buildingIds.value = ['B001', 'B002', 'B003', 'B004', 'B005', 'B006', 'B007']
  selectedBuilding.value = 'B001'
  monthlyData.value = {
    months: ['2024-10', '2024-11', '2024-12', '2025-01', '2025-02', '2025-03'],
    powerConsumption: [8200, 7800, 9100, 8600, 7400, 8900],
    waterConsumption: [65.2, 61.8, 72.5, 68.4, 58.6, 70.8],
    carbonEmission: [6437, 6123, 7143, 6751, 5809, 6987],
    acConsumption: [3690, 3510, 4095, 3870, 3330, 4005]
  }
}

async function loadMonthlyTrend() {
  try {
    const res = await energyApi.getMonthlyTrend(selectedBuilding.value)
    if (res.code === 200) monthlyData.value = res.data
  } catch { /* use demo data */ }
}

async function loadAnomalies() {
  try {
    if (!selectedBuilding.value) return
    const res = await energyApi.getAnomalies(selectedBuilding.value)
    if (res.code === 200) anomalyList.value = res.data
  } catch { /* ignore */ }
}

onMounted(() => {
  loadDashboard()
  for (let i = 0; i < 20; i++) realtimeData.value.push(generateRealtimePoint())
  realtimeTimer = setInterval(() => {
    realtimeData.value.push(generateRealtimePoint())
    if (realtimeData.value.length > 30) realtimeData.value.shift()
  }, 3000)
})
onUnmounted(() => clearInterval(realtimeTimer))
</script>

<style scoped>
.dashboard { display: flex; flex-direction: column; gap: 16px; }

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.stat-card {
  background: #0d1b2e;
  border: 1px solid #1a3a5c;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  transition: all 0.2s;
  box-shadow: 0 2px 8px rgba(0,0,0,0.2);
}
.stat-card:hover { border-color: #00d4ff88; box-shadow: 0 4px 16px rgba(0,212,255,0.2); }

.stat-icon {
  width: 52px; height: 52px; border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  color: #00d4ff; flex-shrink: 0;
}

.stat-info { flex: 1; }
.stat-value { font-size: 22px; font-weight: 700; color: #e0f0ff; }
.stat-label { font-size: 12px; color: #8bafc9; margin-top: 4px; }

.stat-trend { font-size: 12px; }
.stat-trend.up { color: #ff6b6b; }
.stat-trend.down { color: #52c41a; }

.chart-row {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 16px;
}

.chart-row .chart-card.large { grid-column: span 1; }

.chart-card {
  background: #0d1b2e;
  border: 1px solid #1a3a5c;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.2);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.card-title {
  font-size: 14px;
  font-weight: 600;
  color: #e0f0ff;
}

.alert-section {
  background: #0d1b2e;
  border: 1px solid #1a3a5c;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.2);
}
</style>
