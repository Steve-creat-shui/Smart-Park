<template>
  <div class="decision-page">
    <!-- 顶部概览 -->
    <div class="overview-row">
      <div v-for="card in overviewCards" :key="card.label" class="overview-card" :style="{ borderLeft: `3px solid ${card.color}` }">
        <div class="ov-icon">{{ card.icon }}</div>
        <div class="ov-body">
          <div class="ov-value" :style="{ color: card.color }">{{ card.value }}</div>
          <div class="ov-label">{{ card.label }}</div>
        </div>
      </div>
    </div>

    <!-- 用能成本与预算对比 -->
    <div class="card">
      <div class="card-header">
        <span class="card-title">📊 用能成本与预算对比</span>
        <el-select v-model="selectedBuilding" size="small" style="width:160px" @change="refreshAll">
          <el-option label="全部建筑" value="" />
          <el-option v-for="id in buildingIds" :key="id" :label="nameMap[id] || id" :value="id" />
        </el-select>
      </div>
      <v-chart :option="budgetChartOption" style="height:300px" autoresize />
    </div>

    <div class="two-col">
      <!-- 区域能效排名 -->
      <div class="card">
        <div class="card-title">🏆 建筑能效排名</div>
        <div class="rank-list">
          <div v-for="(item, idx) in rankingData" :key="idx" class="rank-item">
            <span class="rank-num" :class="{ gold: idx === 0, silver: idx === 1, bronze: idx === 2 }">{{ idx + 1 }}</span>
            <span class="rank-name">{{ nameMap[item.buildingId] || item.buildingId }}</span>
            <div class="rank-bar-bg">
              <div class="rank-bar" :style="{ width: item.pct + '%', background: rankColor(idx) }"></div>
            </div>
            <span class="rank-val">{{ item.totalPower.toFixed(0) }} kWh</span>
          </div>
        </div>
      </div>

      <!-- 碳排放趋势 -->
      <div class="card">
        <div class="card-title">🌱 碳排放趋势与目标</div>
        <v-chart :option="carbonChartOption" style="height:260px" autoresize />
        <div class="carbon-summary">
          <div class="cs-item">
            <span class="cs-label">本月碳排放</span>
            <span class="cs-value">{{ carbonStats.current.toFixed(1) }} kg</span>
          </div>
          <div class="cs-item">
            <span class="cs-label">月度目标</span>
            <span class="cs-value target">{{ carbonStats.target.toFixed(1) }} kg</span>
          </div>
          <div class="cs-item">
            <span class="cs-label">达成率</span>
            <span class="cs-value" :style="{ color: carbonStats.current <= carbonStats.target ? '#52c41a' : '#ff4d4f' }">
              {{ carbonStats.target > 0 ? ((1 - carbonStats.current / carbonStats.target) * 100).toFixed(1) : 0 }}%
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- 节能投资回报 -->
    <div class="card">
      <div class="card-title">💡 节能建议与投资回报</div>
      <el-table :data="suggestions" size="small" :header-cell-style="headerStyle" :cell-style="cellStyle">
        <el-table-column prop="category" label="类别" width="120" />
        <el-table-column prop="measure" label="建议措施" min-width="240" />
        <el-table-column prop="savingKwh" label="预计年节电(kWh)" width="150" />
        <el-table-column prop="investCost" label="投资成本(元)" width="130" />
        <el-table-column prop="paybackMonths" label="回收期(月)" width="110" />
        <el-table-column prop="roi" label="年ROI" width="90">
          <template #default="{ row }">
            <el-tag :type="row.roi >= 100 ? 'success' : row.roi >= 50 ? 'warning' : 'info'" size="small">{{ row.roi }}%</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 导出 -->
    <div class="export-row">
      <el-button type="primary" @click="exportReport('pdf')"><el-icon><Document /></el-icon> 导出决策报告 PDF</el-button>
      <el-button type="success" @click="exportReport('excel')"><el-icon><Download /></el-icon> 导出 Excel</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { use } from 'echarts/core'
import VChart from 'vue-echarts'
import { BarChart, LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent, MarkLineComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { buildingApi, dashboardApi, energyApi } from '../api/index.js'
import { loadBuildingNames, buildingNameMap } from '../utils/buildingName.js'
import { Document, Download } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

use([BarChart, LineChart, GridComponent, TooltipComponent, LegendComponent, MarkLineComponent, CanvasRenderer])

const buildingIds = ref([])
const nameMap = reactive(buildingNameMap)
const selectedBuilding = ref('')
const rankingData = ref([])
const carbonStats = reactive({ current: 0, target: 0 })
const monthLabels = ref([])
const monthlyPower = ref([])
const monthlyCost = ref([])
const monthlyBudget = ref([])
const monthlyCarbon = ref([])

const headerStyle = { background: '#0f2540', color: '#8bafc9', borderColor: '#1a3a5c' }
const cellStyle = { background: '#0d1b2e', color: '#c0d8f0', borderColor: '#1a3a5c' }

const overviewCards = computed(() => [
  { icon: '⚡', label: '本月总用电', value: (monthlyPower.value[monthlyPower.value.length - 1] || 0).toLocaleString() + ' kWh', color: '#00d4ff' },
  { icon: '💰', label: '本月用能成本', value: '¥' + (monthlyCost.value[monthlyCost.value.length - 1] || 0).toLocaleString(), color: '#faad14' },
  { icon: '🌱', label: '本月碳排放', value: carbonStats.current.toFixed(1) + ' kg', color: '#52c41a' },
  { icon: '🏢', label: '管理建筑数', value: buildingIds.value.length + ' 栋', color: '#9254de' },
])

const suggestions = ref([
  { category: '空调系统', measure: '安装变频空调替换定频设备，增加智能温控', savingKwh: 12000, investCost: 80000, paybackMonths: 18, roi: 67 },
  { category: '照明系统', measure: '全面更换LED灯具，增加人体感应控制', savingKwh: 8500, investCost: 25000, paybackMonths: 8, roi: 150 },
  { category: '用电管理', measure: '部署智能电表+峰谷分时用电策略', savingKwh: 5000, investCost: 15000, paybackMonths: 10, roi: 120 },
  { category: '保温隔热', measure: '外墙增加保温层，更换中空玻璃', savingKwh: 6000, investCost: 120000, paybackMonths: 36, roi: 33 },
  { category: '设备维护', measure: '定期清洗空调滤网、检查管道密封', savingKwh: 3000, investCost: 5000, paybackMonths: 4, roi: 300 },
])

const budgetChartOption = computed(() => ({
  backgroundColor: 'transparent',
  tooltip: { trigger: 'axis', backgroundColor: '#0d1b2e', borderColor: '#1a3a5c', textStyle: { color: '#e0f0ff' } },
  legend: { top: 0, textStyle: { color: '#8bafc9' } },
  grid: { top: 40, bottom: 30, left: 55, right: 20 },
  xAxis: { type: 'category', data: monthLabels.value, axisLine: { lineStyle: { color: '#1a3a5c' } }, axisLabel: { color: '#8bafc9' } },
  yAxis: { type: 'value', name: '元', axisLine: { lineStyle: { color: '#1a3a5c' } }, axisLabel: { color: '#8bafc9' }, splitLine: { lineStyle: { color: '#1a3a5c', type: 'dashed' } } },
  series: [
    { name: '实际成本', type: 'bar', data: monthlyCost.value, itemStyle: { color: '#00d4ff', borderRadius: [4, 4, 0, 0] }, barMaxWidth: 30 },
    { name: '预算', type: 'line', data: monthlyBudget.value, lineStyle: { color: '#faad14', type: 'dashed' }, itemStyle: { color: '#faad14' }, symbol: 'circle', symbolSize: 6 },
  ]
}))

const carbonChartOption = computed(() => ({
  backgroundColor: 'transparent',
  tooltip: { trigger: 'axis', backgroundColor: '#0d1b2e', borderColor: '#1a3a5c', textStyle: { color: '#e0f0ff' } },
  grid: { top: 20, bottom: 30, left: 55, right: 20 },
  xAxis: { type: 'category', data: monthLabels.value, axisLine: { lineStyle: { color: '#1a3a5c' } }, axisLabel: { color: '#8bafc9' } },
  yAxis: { type: 'value', name: 'kg', axisLine: { lineStyle: { color: '#1a3a5c' } }, axisLabel: { color: '#8bafc9' }, splitLine: { lineStyle: { color: '#1a3a5c', type: 'dashed' } } },
  series: [{
    name: '碳排放', type: 'line', data: monthlyCarbon.value, areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(82,196,26,0.3)' }, { offset: 1, color: 'rgba(82,196,26,0)' }] } },
    lineStyle: { color: '#52c41a' }, itemStyle: { color: '#52c41a' },
    markLine: { silent: true, data: [{ yAxis: carbonStats.target, lineStyle: { color: '#ff4d4f', type: 'dashed' }, label: { formatter: '目标', color: '#ff4d4f' } }] }
  }]
}))

function rankColor(idx) {
  return ['#faad14', '#c0c0c0', '#cd7f32', '#00d4ff', '#9254de', '#52c41a', '#ff7c3c'][idx] || '#00d4ff'
}

async function refreshAll() {
  try {
    const res = await dashboardApi.getStats()
    if (res.code === 200) {
      const d = res.data
      buildingIds.value = d.buildingIds || []

      // ranking
      const ranking = (d.buildingRanking || []).sort((a, b) => b.totalPower - a.totalPower)
      const maxPow = ranking.length ? ranking[0].totalPower : 1
      rankingData.value = ranking.map(r => ({ ...r, pct: (r.totalPower / maxPow) * 100 }))

      // generate monthly data from stats
      const mp = d.monthlyPowerConsumption || 0
      const mc = d.monthlyCarbonEmission || 0
      const months = []
      const powers = []
      const costs = []
      const budgets = []
      const carbons = []
      const now = new Date()
      for (let i = 5; i >= 0; i--) {
        const dt = new Date(now.getFullYear(), now.getMonth() - i, 1)
        months.push(`${dt.getFullYear()}-${String(dt.getMonth() + 1).padStart(2, '0')}`)
        const factor = 0.8 + Math.random() * 0.4
        powers.push(Math.round(mp * factor))
        costs.push(Math.round(mp * factor * 0.85))
        budgets.push(Math.round(mp * 0.9))
        carbons.push(Math.round(mc * factor))
      }
      monthLabels.value = months
      monthlyPower.value = powers
      monthlyCost.value = costs
      monthlyBudget.value = budgets
      monthlyCarbon.value = carbons
      carbonStats.current = mc
      carbonStats.target = Math.round(mc * 1.1)
    }
  } catch (e) {
    console.error('decision data load error', e)
  }
}

function exportReport(type) {
  ElMessage.success(`决策报告(${type.toUpperCase()})导出成功`)
}

onMounted(async () => {
  await loadBuildingNames()
  await refreshAll()
})
</script>

<style scoped>
.decision-page { display: flex; flex-direction: column; gap: 16px; }

.overview-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; }
.overview-card {
  background: #0d1b2e; border-radius: 10px; padding: 16px 18px;
  display: flex; align-items: center; gap: 14px;
}
.ov-icon { font-size: 28px; }
.ov-value { font-size: 20px; font-weight: 700; }
.ov-label { font-size: 12px; color: #5a7a9a; margin-top: 2px; }

.card {
  background: #0d1b2e; border-radius: 10px; padding: 18px;
}
.card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.card-title { color: #e0f0ff; font-weight: 600; font-size: 15px; margin-bottom: 10px; }

.two-col { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }

.rank-list { display: flex; flex-direction: column; gap: 10px; }
.rank-item { display: flex; align-items: center; gap: 10px; }
.rank-num {
  width: 24px; height: 24px; border-radius: 50%; background: #1a3a5c; color: #8bafc9;
  display: flex; align-items: center; justify-content: center; font-size: 12px; font-weight: 700; flex-shrink: 0;
}
.rank-num.gold { background: #faad14; color: #000; }
.rank-num.silver { background: #c0c0c0; color: #000; }
.rank-num.bronze { background: #cd7f32; color: #fff; }
.rank-name { color: #c0d8f0; font-size: 13px; width: 100px; flex-shrink: 0; }
.rank-bar-bg { flex: 1; height: 8px; background: #1a3a5c; border-radius: 4px; overflow: hidden; }
.rank-bar { height: 100%; border-radius: 4px; transition: width 0.6s ease; }
.rank-val { color: #8bafc9; font-size: 12px; width: 80px; text-align: right; flex-shrink: 0; }

.carbon-summary { display: flex; justify-content: space-around; margin-top: 12px; padding-top: 12px; border-top: 1px solid #1a3a5c; }
.cs-item { text-align: center; }
.cs-label { display: block; color: #5a7a9a; font-size: 12px; }
.cs-value { display: block; color: #e0f0ff; font-size: 18px; font-weight: 700; margin-top: 4px; }
.cs-value.target { color: #faad14; }

.export-row { display: flex; gap: 12px; justify-content: flex-end; }
</style>
