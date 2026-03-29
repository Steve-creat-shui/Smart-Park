<template>
  <div class="energy-analysis">
    <div class="filter-bar">
      <el-select v-model="form.buildingId" placeholder="选择建筑" style="width:160px" clearable>
        <el-option v-for="id in buildingIds" :key="id" :label="id" :value="id" />
      </el-select>
      <el-date-picker v-model="form.dateRange" type="datetimerange"
        range-separator="至" start-placeholder="开始时间" end-placeholder="结束时间"
        format="YYYY-MM-DD HH:mm:ss" value-format="YYYY-MM-DD HH:mm:ss"
        style="width:360px" />
      <el-select v-model="form.analysisType" placeholder="分析类型" style="width:140px">
        <el-option label="综合分析" value="comprehensive" />
        <el-option label="月度报告" value="monthly_report" />
        <el-option label="异常诊断" value="anomaly_detection" />
      </el-select>
      <el-button type="primary" :loading="loading" @click="doAnalysis" icon="Search">开始分析</el-button>
      <el-button @click="resetForm" icon="Refresh">重置</el-button>
    </div>

    <div v-if="result" class="result-area">
      <div class="kpi-row">
        <div class="kpi-card" v-for="kpi in kpiItems" :key="kpi.key">
          <div class="kpi-label">{{ kpi.label }}</div>
          <div class="kpi-value" :style="{ color: kpi.color }">{{ kpi.value }}</div>
          <div class="kpi-unit">{{ kpi.unit }}</div>
        </div>
        <div class="kpi-card rating-card">
          <div class="kpi-label">能效评级</div>
          <div class="rating-badge" :class="'rating-' + result.energyRating">{{ result.energyRating }}</div>
          <div class="kpi-unit">综合得分 {{ result.energyEfficiencyScore }}</div>
        </div>
      </div>

      <div class="chart-row">
        <div class="chart-card large">
          <div class="card-header">
            <span class="card-title">能耗趋势图</span>
            <el-radio-group v-model="chartType" size="small">
              <el-radio-button value="power">电力</el-radio-button>
              <el-radio-button value="carbon">碳排放</el-radio-button>
              <el-radio-button value="water">水耗</el-radio-button>
            </el-radio-group>
          </div>
          <v-chart :option="trendChartOption" style="height:280px" autoresize />
        </div>
        <div class="chart-card">
          <div class="card-header"><span class="card-title">空调能耗占比</span></div>
          <v-chart :option="acPieOption" style="height:280px" autoresize />
        </div>
      </div>

      <div class="anomaly-section" v-if="result.anomalies && result.anomalies.length > 0">
        <div class="card-header">
          <span class="card-title">异常检测结果</span>
          <el-tag type="danger" size="small">{{ result.anomalies.length }} 条异常</el-tag>
        </div>
        <el-table :data="result.anomalies" size="small"
          :header-cell-style="headerStyle" :cell-style="cellStyle">
          <el-table-column prop="timestamp" label="时间" />
          <el-table-column prop="power" label="能耗(kWh)" width="120">
            <template #default="{ row }">
              <span style="color:#ff6b6b;font-weight:600">{{ Number(row.power).toFixed(2) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="excess" label="超出比例" width="120">
            <template #default="{ row }">
              <el-tag type="danger" size="small">+{{ (row.excess * 100).toFixed(1) }}%</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="type" label="类型" width="120" />
        </el-table>
      </div>
    </div>

    <el-empty v-else description="请选择建筑和时间范围后点击开始分析" class="empty-state" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { LineChart, BarChart, PieChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { buildingApi, analysisApi } from '../api/index.js'
import { ElMessage } from 'element-plus'

use([LineChart, BarChart, PieChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer])

const buildingIds = ref([])
const loading = ref(false)
const result = ref(null)
const chartType = ref('power')

const form = ref({ buildingId: '', dateRange: null, analysisType: 'comprehensive' })

const kpiItems = computed(() => {
  if (!result.value) return []
  return [
    { key: 'power', label: '总电力消耗', value: result.value.totalPowerConsumption?.toFixed(2) || '-', unit: 'kWh', color: '#00d4ff' },
    { key: 'water', label: '总水能耗', value: result.value.totalWaterConsumption?.toFixed(2) || '-', unit: 'm³', color: '#52c41a' },
    { key: 'carbon', label: '总碳排放', value: result.value.totalCarbonEmission?.toFixed(2) || '-', unit: 'kg', color: '#fadb14' },
    { key: 'ac', label: '空调能耗', value: result.value.totalAcConsumption?.toFixed(2) || '-', unit: 'kWh', color: '#8b5cf6' },
    { key: 'avg', label: '平均能耗', value: result.value.avgPowerConsumption?.toFixed(2) || '-', unit: 'kWh/次', color: '#ff8c00' }
  ]
})

const trendChartOption = computed(() => {
  const trend = result.value?.trendData || []
  const times = trend.map(d => d.timestamp?.slice(0, 16) || '')
  const values = trend.map(d => chartType.value === 'power' ? d.power : chartType.value === 'carbon' ? d.carbon : d.water)
  const colors = { power: '#00d4ff', carbon: '#52c41a', water: '#8b5cf6' }
  const color = colors[chartType.value]
  return {
    backgroundColor: 'transparent',
    tooltip: { trigger: 'axis', backgroundColor: '#0d1b2e', borderColor: '#1a3a5c', textStyle: { color: '#e0f0ff' } },
    grid: { top: 20, bottom: 50, left: 60, right: 20 },
    xAxis: { type: 'category', data: times, axisLabel: { color: '#8bafc9', rotate: 30, fontSize: 10 }, axisLine: { lineStyle: { color: '#1a3a5c' } } },
    yAxis: { type: 'value', axisLabel: { color: '#8bafc9' }, splitLine: { lineStyle: { color: '#1a3a5c' } } },
    series: [{
      type: 'line', data: values, smooth: true, symbol: 'none',
      itemStyle: { color },
      areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: color + '44' }, { offset: 1, color: color + '00' }] } }
    }]
  }
})

const acPieOption = computed(() => {
  const total = result.value?.totalPowerConsumption || 1
  const ac = result.value?.totalAcConsumption || 0
  const other = total - ac
  return {
    backgroundColor: 'transparent',
    tooltip: { trigger: 'item', backgroundColor: '#0d1b2e', borderColor: '#1a3a5c', textStyle: { color: '#e0f0ff' } },
    series: [{
      type: 'pie', radius: ['45%', '72%'], center: ['50%', '55%'],
      data: [
        { value: ac.toFixed(2), name: '空调能耗', itemStyle: { color: '#8b5cf6' } },
        { value: Math.max(0, other).toFixed(2), name: '其他能耗', itemStyle: { color: '#1a3a5c' } }
      ],
      label: { color: '#8bafc9' },
      emphasis: { itemStyle: { shadowBlur: 10 } }
    }]
  }
})

const headerStyle = () => ({ background: '#0d1b2e', color: '#8bafc9', borderBottom: '1px solid #1a3a5c' })
const cellStyle = () => ({ background: 'transparent', color: '#e0f0ff', borderBottom: '1px solid #1a3a5c11' })

async function doAnalysis() {
  if (!form.value.buildingId) { ElMessage.warning('请选择建筑'); return }
  loading.value = true
  try {
    const req = {
      buildingId: form.value.buildingId,
      analysisType: form.value.analysisType
    }
    if (form.value.dateRange) {
      req.startTime = form.value.dateRange[0]
      req.endTime = form.value.dateRange[1]
    }
    const res = await analysisApi.analyzeEnergy(req)
    if (res.code === 200) {
      result.value = res.data
      ElMessage.success('分析完成')
    }
  } catch {
    ElMessage.error('分析失败，请检查后端服务')
  } finally {
    loading.value = false
  }
}

function resetForm() {
  form.value = { buildingId: '', dateRange: null, analysisType: 'comprehensive' }
  result.value = null
}

onMounted(async () => {
  try {
    const res = await buildingApi.getAllBuildingIds()
    if (res.code === 200) buildingIds.value = res.data
  } catch {
    buildingIds.value = ['B001', 'B002', 'B003', 'B004', 'B005', 'B006']
  }
})
</script>

<style scoped>
.energy-analysis { display: flex; flex-direction: column; gap: 16px; }

.filter-bar {
  background: #0d1b2e;
  border: 1px solid #1a3a5c;
  border-radius: 12px;
  padding: 16px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.kpi-row {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 12px;
}

.kpi-card {
  background: #0d1b2e;
  border: 1px solid #1a3a5c;
  border-radius: 12px;
  padding: 16px;
  text-align: center;
}

.kpi-label { font-size: 12px; color: #8bafc9; margin-bottom: 8px; }
.kpi-value { font-size: 22px; font-weight: 700; }
.kpi-unit { font-size: 11px; color: #8bafc9; margin-top: 4px; }

.rating-badge {
  font-size: 36px;
  font-weight: 900;
  margin: 4px 0;
}
.rating-A\+ { color: #52c41a; }
.rating-A { color: #73d13d; }
.rating-B { color: #fadb14; }
.rating-C { color: #ff8c00; }
.rating-D, .rating-E { color: #ff6b6b; }

.chart-row {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 16px;
}

.chart-card {
  background: #0d1b2e;
  border: 1px solid #1a3a5c;
  border-radius: 12px;
  padding: 16px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.card-title { font-size: 14px; font-weight: 600; color: #e0f0ff; }

.anomaly-section {
  background: #0d1b2e;
  border: 1px solid #ff6b6b44;
  border-radius: 12px;
  padding: 16px;
}

.empty-state { margin: 60px auto; }
</style>
