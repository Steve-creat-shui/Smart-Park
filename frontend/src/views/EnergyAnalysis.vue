<template>
  <div class="energy-analysis">
    <div class="filter-bar">
      <el-select v-model="form.buildingId" placeholder="选择建筑" style="width:160px" clearable>
        <el-option v-for="id in buildingIds" :key="id" :label="nameMap[id] || id" :value="id" />
      </el-select>
      <el-date-picker v-model="form.startDate" type="date"
        placeholder="开始日期" format="YYYY-MM-DD" value-format="YYYY-MM-DD 00:00:00"
        :disabled-date="disabledDate" :loading="datesLoading"
        style="width:150px" />
      <span style="color:#8bafc9;font-size:13px">至</span>
      <el-date-picker v-model="form.endDate" type="date"
        placeholder="结束日期" format="YYYY-MM-DD" value-format="YYYY-MM-DD 23:59:59"
        :disabled-date="disabledDate" :loading="datesLoading"
        style="width:150px" />
      <el-select v-model="form.analysisType" placeholder="分析类型" style="width:140px">
        <el-option label="综合分析" value="comprehensive" />
        <el-option label="月度报告" value="monthly_report" />
        <el-option label="异常诊断" value="anomaly_detection" />
      </el-select>
      <el-button type="primary" :loading="loading" @click="doAnalysis" icon="Search">开始分析</el-button>
      <el-button @click="resetForm" icon="Refresh">重置</el-button>
      <div v-if="result" class="export-group">
        <el-dropdown trigger="click" @command="handleExport">
          <el-button type="success" icon="Download">导出报告 <el-icon class="el-icon--right"><ArrowDown /></el-icon></el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="pdf" icon="Document">
                <span style="display:flex;align-items:center;gap:6px">📄 导出 PDF 报告<el-tag size="small" type="danger" effect="plain" style="margin-left:4px">可视化</el-tag></span>
              </el-dropdown-item>
              <el-dropdown-item command="excel" icon="Grid">
                <span style="display:flex;align-items:center;gap:6px">📊 导出 Excel 报告<el-tag size="small" type="success" effect="plain" style="margin-left:4px">数据表</el-tag></span>
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <el-tag v-if="exporting" type="warning" size="small" effect="dark" style="margin-left:8px">
          <el-icon class="is-loading"><Loading /></el-icon> 正在生成...
        </el-tag>
      </div>
    </div>

    <!-- ① 综合分析 -->
    <div v-if="result && displayType === 'comprehensive'" class="result-area" ref="resultAreaRef">
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
        <div class="anomaly-type-summary">
          <span class="ats-chip" v-for="(count, type) in anomalyTypeCounts" :key="type"
            :style="{background: anomalyTypeColor(type) + '22', color: anomalyTypeColor(type), border: '1px solid ' + anomalyTypeColor(type) + '44'}">
            {{ anomalyTypeIcon(type) }} {{ type }} <b>{{ count }}</b>
          </span>
        </div>
        <el-table :data="result.anomalies" size="small"
          :header-cell-style="headerStyle" :cell-style="cellStyle">
          <el-table-column prop="timestamp" label="时间" min-width="140">
            <template #default="{ row }">{{ row.timestamp?.slice(0, 16) }}</template>
          </el-table-column>
          <el-table-column prop="type" label="类型" width="120">
            <template #default="{ row }">
              <span class="atype-badge" :style="{background: anomalyTypeColor(row.type) + '22', color: anomalyTypeColor(row.type)}">
                {{ anomalyTypeIcon(row.type) }} {{ row.type }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="power" label="能耗(kWh)" width="110">
            <template #default="{ row }">
              <span style="color:#ff6b6b;font-weight:600">{{ Number(row.power).toFixed(2) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="excess" label="超出比例" width="110">
            <template #default="{ row }">
              <el-tag :type="row.severity === 'severe' ? 'danger' : row.severity === 'warning' ? 'warning' : 'info'" size="small">
                +{{ (row.excess * 100).toFixed(1) }}%
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="detail" label="详情" min-width="200">
            <template #default="{ row }">
              <span style="color:#8bafc9;font-size:12px">{{ row.detail || '-' }}</span>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <!-- ② 月度报告 — 日视图（同月内） -->
    <div v-else-if="result && displayType === 'monthly_report' && displayScope === 'daily'" class="result-area" ref="resultAreaRef">
      <div class="section-banner monthly-banner">
        <el-icon :size="18"><DataLine /></el-icon>
        <span>日能耗报告 · {{ nameMap[result.buildingId] || result.buildingId }}</span>
        <el-tag type="success" size="small" effect="dark">共 {{ dailyRows.length }} 天</el-tag>
        <el-tag type="info" size="small" effect="plain" style="margin-left:4px">同月精细视图</el-tag>
      </div>
      <div class="kpi-row-5">
        <div class="kpi-card" v-for="k in dailyKpis" :key="k.label">
          <div class="kpi-label">{{ k.label }}</div>
          <div class="kpi-value" :style="{color: k.color}">{{ k.value }}</div>
          <div class="kpi-unit">{{ k.unit }}</div>
        </div>
      </div>
      <div class="chart-row">
        <div class="chart-card large">
          <div class="card-header">
            <span class="card-title">逐日能耗对比</span>
            <el-radio-group v-model="dailyMetric" size="small">
              <el-radio-button value="power">电力</el-radio-button>
              <el-radio-button value="water">水耗</el-radio-button>
              <el-radio-button value="carbon">碳排</el-radio-button>
              <el-radio-button value="ac">空调</el-radio-button>
            </el-radio-group>
          </div>
          <v-chart :option="dailyBarOption" style="height:260px" autoresize />
        </div>
        <div class="chart-card">
          <div class="card-header"><span class="card-title">能耗构成分析</span></div>
          <v-chart :option="monthlyPieOption" style="height:260px" autoresize />
        </div>
      </div>
      <div class="chart-card" style="margin-top:0">
        <div class="card-header"><span class="card-title">逐日明细</span></div>
        <el-table :data="dailyRows" size="small" :header-cell-style="headerStyle" :cell-style="cellStyle">
          <el-table-column prop="day" label="日期" width="110" />
          <el-table-column label="电力(kWh)">
            <template #default="{ row }"><span style="color:#00d4ff">{{ row.power.toFixed(1) }}</span></template>
          </el-table-column>
          <el-table-column label="水耗(m³)">
            <template #default="{ row }"><span style="color:#52c41a">{{ row.water.toFixed(2) }}</span></template>
          </el-table-column>
          <el-table-column label="碳排(kg)">
            <template #default="{ row }"><span style="color:#fadb14">{{ row.carbon.toFixed(1) }}</span></template>
          </el-table-column>
          <el-table-column label="空调(kWh)">
            <template #default="{ row }"><span style="color:#8b5cf6">{{ row.ac.toFixed(1) }}</span></template>
          </el-table-column>
          <el-table-column label="记录数" width="80">
            <template #default="{ row }">{{ row.count }}</template>
          </el-table-column>
          <el-table-column label="空调占比" width="100">
            <template #default="{ row }">
              <el-tag :type="(row.ac/row.power)>0.6?'danger':'warning'" size="small">
                {{ row.power > 0 ? (row.ac/row.power*100).toFixed(1) : '0' }}%
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <!-- ② 月度报告 — 月视图（跨月） -->
    <div v-else-if="result && displayType === 'monthly_report' && displayScope === 'monthly'" class="result-area" ref="resultAreaRef">
      <div class="section-banner monthly-banner">
        <el-icon :size="18"><DataLine /></el-icon>
        <span>月度能耗报告 · {{ nameMap[result.buildingId] || result.buildingId }}</span>
        <el-tag type="primary" size="small" effect="dark">共 {{ monthlyRows.length }} 个月</el-tag>
        <el-tag type="info" size="small" effect="plain" style="margin-left:4px">跨月汇总视图</el-tag>
      </div>
      <div class="kpi-row-5">
        <div class="kpi-card" v-for="k in monthlyKpis" :key="k.label">
          <div class="kpi-label">{{ k.label }}</div>
          <div class="kpi-value" :style="{color: k.color}">{{ k.value }}</div>
          <div class="kpi-unit">{{ k.unit }}</div>
        </div>
      </div>
      <div class="chart-row">
        <div class="chart-card large">
          <div class="card-header">
            <span class="card-title">月度能耗对比</span>
            <el-radio-group v-model="monthlyMetric" size="small">
              <el-radio-button value="power">电力</el-radio-button>
              <el-radio-button value="water">水耗</el-radio-button>
              <el-radio-button value="carbon">碳排</el-radio-button>
            </el-radio-group>
          </div>
          <v-chart :option="monthlyBarOption" style="height:260px" autoresize />
        </div>
        <div class="chart-card">
          <div class="card-header"><span class="card-title">能耗构成分析</span></div>
          <v-chart :option="monthlyPieOption" style="height:260px" autoresize />
        </div>
      </div>
      <div class="chart-card" style="margin-top:0">
        <div class="card-header"><span class="card-title">月度明细</span></div>
        <el-table :data="monthlyRows" size="small" :header-cell-style="headerStyle" :cell-style="cellStyle">
          <el-table-column prop="month" label="月份" width="100" />
          <el-table-column label="电力消耗(kWh)">
            <template #default="{ row }"><span style="color:#00d4ff">{{ row.power.toFixed(1) }}</span></template>
          </el-table-column>
          <el-table-column label="水消耗(m³)">
            <template #default="{ row }"><span style="color:#52c41a">{{ row.water.toFixed(2) }}</span></template>
          </el-table-column>
          <el-table-column label="碳排放(kg)">
            <template #default="{ row }"><span style="color:#fadb14">{{ row.carbon.toFixed(1) }}</span></template>
          </el-table-column>
          <el-table-column label="空调能耗(kWh)">
            <template #default="{ row }"><span style="color:#8b5cf6">{{ row.ac.toFixed(1) }}</span></template>
          </el-table-column>
          <el-table-column label="记录数" width="90">
            <template #default="{ row }">{{ row.count }}</template>
          </el-table-column>
          <el-table-column label="日均电力(kWh)" width="120">
            <template #default="{ row }">
              <span style="color:#ff8c00">{{ (row.power / row.count).toFixed(2) }}</span>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <!-- ③ 异常诊断 -->
    <div v-else-if="result && displayType === 'anomaly_detection'" class="result-area" ref="resultAreaRef">
      <div class="section-banner anomaly-banner">
        <el-icon :size="18"><Warning /></el-icon>
        <span>异常诊断报告 · {{ nameMap[result.buildingId] || result.buildingId }}</span>
        <el-tag :type="anomalyStats.count > 0 ? 'danger' : 'success'" size="small" effect="dark">
          {{ anomalyStats.count > 0 ? anomalyStats.count + ' 条高能耗异常' : '无异常' }}
        </el-tag>
      </div>
      <div class="anomaly-stat-row">
        <div class="anstat-card" v-for="s in anomalySummaryCards" :key="s.label">
          <div class="anstat-icon" :style="{background: s.bg}">
            <el-icon :size="20"><component :is="s.icon" /></el-icon>
          </div>
          <div>
            <div class="anstat-val" :style="{color: s.color}">{{ s.value }}</div>
            <div class="anstat-label">{{ s.label }}</div>
          </div>
        </div>
      </div>
      <div class="chart-row">
        <div class="chart-card large">
          <div class="card-header">
            <span class="card-title">能耗分布（红点为异常）</span>
          </div>
          <v-chart :option="anomalyScatterOption" style="height:260px" autoresize />
        </div>
        <div class="chart-card">
          <div class="card-header"><span class="card-title">超标等级分布</span></div>
          <v-chart :option="anomalyLevelPie" style="height:260px" autoresize />
        </div>
      </div>
      <div v-if="result.anomalies && result.anomalies.length > 0" class="anomaly-cards-wrap">
        <div class="card-header" style="margin-bottom:8px">
          <span class="card-title">异常记录详情</span>
          <el-tag type="danger" size="small">{{ result.anomalies.length }} 条</el-tag>
        </div>
        <div class="anomaly-card-grid">
          <div class="anomaly-card" v-for="(a, i) in result.anomalies" :key="i"
               :class="{ 'ac-severe': a.excess > 1, 'ac-warning': a.excess <= 1 }">
            <div class="ac-rank">#{{ i + 1 }}</div>
            <div class="ac-time">{{ a.timestamp?.slice(0, 16) }}</div>
            <div class="ac-power">{{ Number(a.power).toFixed(2) }} <span class="ac-unit">kWh</span></div>
            <el-tag :type="a.excess > 1 ? 'danger' : 'warning'" size="small" effect="dark">
              +{{ (a.excess * 100).toFixed(1) }}%
            </el-tag>
          </div>
        </div>
      </div>
      <el-empty v-else description="当前时段未检测到高能耗异常" style="margin:40px auto" />
    </div>

    <el-empty v-else description="正在加载行政楼数据..." class="empty-state" />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { LineChart, BarChart, PieChart, ScatterChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { buildingApi, analysisApi, energyApi } from '../api/index.js'
import { loadBuildingNames, buildingNameMap } from '../utils/buildingName.js'
import { ElMessage } from 'element-plus'
import { DataLine, Warning, TrendCharts, CircleClose, Lightning, ArrowDown, Loading } from '@element-plus/icons-vue'
import { jsPDF } from 'jspdf'
import 'jspdf-autotable'
import ExcelJS from 'exceljs'

use([LineChart, BarChart, PieChart, ScatterChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer])

const route = useRoute()
const buildingIds = ref([])
const nameMap = reactive(buildingNameMap)
const loading = ref(false)
const result = ref(null)
const chartType = ref('power')
const displayType = ref('comprehensive')
const monthlyMetric = ref('power')
const dailyMetric = ref('power')
const displayScope = ref('monthly')
const availableDatesSet = ref(new Set())
const datesLoading = ref(false)
const exporting = ref(false)
const resultAreaRef = ref(null)

const form = ref({ buildingId: '', startDate: null, endDate: null, analysisType: 'comprehensive' })

function handleExport(cmd) {
  if (cmd === 'pdf') exportPDF()
  else if (cmd === 'excel') exportExcel()
}

const disabledDate = (date) => {
  if (availableDatesSet.value.size === 0) return false
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return !availableDatesSet.value.has(`${y}-${m}-${d}`)
}

async function loadAvailableDates(buildingId) {
  if (!buildingId) return
  datesLoading.value = true
  try {
    const res = await energyApi.getAvailableDates(buildingId)
    if (res.code === 200 && Array.isArray(res.data)) {
      availableDatesSet.value = new Set(res.data)
    }
  } catch (e) {
    availableDatesSet.value = new Set()
  } finally {
    datesLoading.value = false
  }
}

watch(() => form.value.buildingId, (newId) => {
  if (newId) loadAvailableDates(newId)
  form.value.startDate = null
  form.value.endDate = null
})

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
        { value: Math.max(0, other).toFixed(2), name: '其他能耗', itemStyle: { color: '#a8d8d0' } }
      ],
      label: { color: '#8bafc9' },
      emphasis: { itemStyle: { shadowBlur: 10 } }
    }]
  }
})

const headerStyle = () => ({ background: '#0f2540', color: '#8bafc9', borderBottom: '1px solid #1a3a5c' })
const cellStyle = () => ({ background: '#0d1b2e', color: '#c0d8f0', borderBottom: '1px solid #1a3a5c' })

// ── 月度报告 computed ─────────────────────────────────
const monthlyRows = computed(() => {
  if (!result.value?.trendData) return []
  const map = {}
  result.value.trendData.forEach(d => {
    const month = (d.timestamp || '').slice(0, 7)
    if (!month) return
    if (!map[month]) map[month] = { month, power: 0, water: 0, carbon: 0, ac: 0, count: 0 }
    map[month].power  += Number(d.power)  || 0
    map[month].water  += Number(d.water)  || 0
    map[month].carbon += Number(d.carbon) || 0
    map[month].ac     += Number(d.ac)     || 0
    map[month].count++
  })
  return Object.values(map).sort((a, b) => a.month.localeCompare(b.month))
})

const monthlyKpis = computed(() => {
  if (!result.value) return []
  const months = monthlyRows.value.length || 1
  return [
    { label: '时段总电力', value: result.value.totalPowerConsumption?.toFixed(1) || '-', unit: 'kWh',   color: '#00d4ff' },
    { label: '时段总水耗', value: result.value.totalWaterConsumption?.toFixed(2) || '-', unit: 'm³',   color: '#52c41a' },
    { label: '时段总碳排', value: result.value.totalCarbonEmission?.toFixed(1) || '-', unit: 'kg',    color: '#fadb14' },
    { label: '空调能耗',   value: result.value.totalAcConsumption?.toFixed(1) || '-',    unit: 'kWh',   color: '#8b5cf6' },
    { label: '月均电力',   value: ((result.value.totalPowerConsumption || 0) / months).toFixed(1), unit: 'kWh/月', color: '#ff8c00' }
  ]
})

// ── 日视图 computed ─────────────────────────────────
const dailyRows = computed(() => {
  if (!result.value?.trendData) return []
  const map = {}
  result.value.trendData.forEach(d => {
    const day = (d.timestamp || '').slice(0, 10)
    if (!day) return
    if (!map[day]) map[day] = { day, power: 0, water: 0, carbon: 0, ac: 0, count: 0 }
    map[day].power  += Number(d.power)  || 0
    map[day].water  += Number(d.water)  || 0
    map[day].carbon += Number(d.carbon) || 0
    map[day].ac     += Number(d.ac)     || 0
    map[day].count++
  })
  return Object.values(map).sort((a, b) => a.day.localeCompare(b.day))
})

const dailyKpis = computed(() => {
  if (!result.value) return []
  const days = dailyRows.value.length || 1
  return [
    { label: '时段总电力', value: result.value.totalPowerConsumption?.toFixed(1) || '-', unit: 'kWh',   color: '#00d4ff' },
    { label: '时段总水耗', value: result.value.totalWaterConsumption?.toFixed(2) || '-', unit: 'm³',   color: '#52c41a' },
    { label: '时段总碳排', value: result.value.totalCarbonEmission?.toFixed(1) || '-', unit: 'kg',    color: '#fadb14' },
    { label: '空调能耗',   value: result.value.totalAcConsumption?.toFixed(1) || '-',    unit: 'kWh',   color: '#8b5cf6' },
    { label: '日均电力',   value: ((result.value.totalPowerConsumption || 0) / days).toFixed(1), unit: 'kWh/天', color: '#ff8c00' }
  ]
})

const dailyBarOption = computed(() => {
  const rows = dailyRows.value
  const metricMap = { power: { key: 'power', color: '#00d4ff', label: '电力(kWh)' }, water: { key: 'water', color: '#52c41a', label: '水耗(m³)' }, carbon: { key: 'carbon', color: '#fadb14', label: '碳排(kg)' }, ac: { key: 'ac', color: '#8b5cf6', label: '空调(kWh)' } }
  const m = metricMap[dailyMetric.value]
  return {
    backgroundColor: 'transparent',
    tooltip: { trigger: 'axis', backgroundColor: '#0d1b2e', borderColor: '#1a3a5c', textStyle: { color: '#e0f0ff' } },
    grid: { top: 20, bottom: 50, left: 60, right: 20 },
    xAxis: { type: 'category', data: rows.map(r => r.day.slice(5)), axisLabel: { color: '#8bafc9', rotate: 35, fontSize: 10 }, axisLine: { lineStyle: { color: '#1a3a5c' } } },
    yAxis: { type: 'value', name: m.label, nameTextStyle: { color: '#8bafc9', fontSize: 10 }, axisLabel: { color: '#8bafc9' }, splitLine: { lineStyle: { color: '#1a3a5c' } } },
    series: [{ type: 'bar', data: rows.map(r => r[m.key].toFixed(2)), itemStyle: { color: m.color, borderRadius: [4, 4, 0, 0] }, barMaxWidth: 32 }]
  }
})

const monthlyBarOption = computed(() => {
  const rows = monthlyRows.value
  const metricMap = { power: { key: 'power', color: '#00d4ff', label: '电力(kWh)' }, water: { key: 'water', color: '#52c41a', label: '水耗(m³)' }, carbon: { key: 'carbon', color: '#fadb14', label: '碳排(kg)' } }
  const m = metricMap[monthlyMetric.value]
  return {
    backgroundColor: 'transparent',
    tooltip: { trigger: 'axis', backgroundColor: '#0d1b2e', borderColor: '#1a3a5c', textStyle: { color: '#e0f0ff' } },
    grid: { top: 20, bottom: 40, left: 60, right: 20 },
    xAxis: { type: 'category', data: rows.map(r => r.month), axisLabel: { color: '#8bafc9', rotate: 20 }, axisLine: { lineStyle: { color: '#1a3a5c' } } },
    yAxis: { type: 'value', name: m.label, nameTextStyle: { color: '#8bafc9', fontSize: 10 }, axisLabel: { color: '#8bafc9' }, splitLine: { lineStyle: { color: '#1a3a5c' } } },
    series: [{ type: 'bar', data: rows.map(r => r[m.key].toFixed(2)), itemStyle: { color: m.color, borderRadius: [4, 4, 0, 0] }, barMaxWidth: 40 }]
  }
})

const monthlyPieOption = computed(() => {
  const r = result.value
  if (!r) return {}
  const total = r.totalPowerConsumption || 1
  return {
    backgroundColor: 'transparent',
    tooltip: { trigger: 'item', backgroundColor: '#0d1b2e', borderColor: '#1a3a5c', textStyle: { color: '#e0f0ff' } },
    legend: { orient: 'vertical', right: 10, top: 'center', textStyle: { color: '#8bafc9' } },
    series: [{ type: 'pie', radius: ['40%', '68%'], center: ['40%', '55%'],
      data: [
        { value: (r.totalAcConsumption || 0).toFixed(1), name: '空调能耗', itemStyle: { color: '#8b5cf6' } },
        { value: Math.max(0, total - (r.totalAcConsumption || 0)).toFixed(1), name: '其他电力', itemStyle: { color: '#00d4ff' } },
        { value: (r.totalWaterConsumption || 0).toFixed(1), name: '水消耗', itemStyle: { color: '#52c41a' } }
      ], label: { color: '#8bafc9' }
    }]
  }
})

// ── 异常诊断 computed ─────────────────────────────────
const anomalyStats = computed(() => {
  const list = result.value?.anomalies || []
  const excessArr = list.map(a => Number(a.excess) || 0)
  return {
    count: list.length,
    total: result.value?.trendData?.length || 0,
    maxExcess: list.length ? Math.max(...excessArr) : 0,
    avgExcess: list.length ? excessArr.reduce((s, v) => s + v, 0) / list.length : 0
  }
})

const anomalySummaryCards = computed(() => [
  { label: '异常记录数',   value: anomalyStats.value.count, color: '#ff6b6b', bg: '#ff6b6b22', icon: 'CircleClose' },
  { label: '最大超标比例', value: '+' + (anomalyStats.value.maxExcess * 100).toFixed(1) + '%', color: '#faad14', bg: '#faad1422', icon: 'TrendCharts' },
  { label: '平均超标比例', value: '+' + (anomalyStats.value.avgExcess * 100).toFixed(1) + '%', color: '#ff8c00', bg: '#ff8c0022', icon: 'Warning' },
  { label: '记录总量',     value: anomalyStats.value.total, color: '#5a7a9a', bg: '#5a7a9a22', icon: 'DataLine' }
])

const anomalyScatterOption = computed(() => {
  const trend = result.value?.trendData || []
  const anomalyTimes = new Set((result.value?.anomalies || []).map(a => a.timestamp))
  const normal = trend.filter(d => !anomalyTimes.has(d.timestamp)).map((d, i) => [i, Number(d.power) || 0])
  const anomalous = trend.filter(d => anomalyTimes.has(d.timestamp)).map((d, i) => {
    const idx = trend.indexOf(d)
    return [idx, Number(d.power) || 0]
  })
  return {
    backgroundColor: 'transparent',
    tooltip: { trigger: 'item', backgroundColor: '#0d1b2e', borderColor: '#1a3a5c', textStyle: { color: '#e0f0ff' }, formatter: p => `记录 ${p.data[0]}<br/>能耗: ${p.data[1].toFixed(2)} kWh` },
    grid: { top: 20, bottom: 40, left: 60, right: 20 },
    xAxis: { type: 'value', name: '记录序号', axisLabel: { color: '#8bafc9' }, splitLine: { lineStyle: { color: '#1a3a5c' } } },
    yAxis: { type: 'value', name: 'kWh', axisLabel: { color: '#8bafc9' }, splitLine: { lineStyle: { color: '#1a3a5c' } } },
    series: [
      { name: '正常', type: 'scatter', data: normal, symbolSize: 5, itemStyle: { color: '#00d4ff55' } },
      { name: '异常', type: 'scatter', data: anomalous, symbolSize: 9, itemStyle: { color: '#ff6b6b' }, emphasis: { scale: 1.5 } }
    ]
  }
})

const _typeColorMap = { '高能耗异常': '#ff4d4f', '空调过载': '#ff8c00', '用水异常': '#36cfc9', '碳排超标': '#faad14', '夜间异常': '#8b5cf6', '能效偏低': '#1890ff' }
const _typeIconMap  = { '高能耗异常': '⚡', '空调过载': '❄️', '用水异常': '💧', '碳排超标': '🌿', '夜间异常': '🌙', '能效偏低': '📉' }
function anomalyTypeColor(t) { return _typeColorMap[t] || '#ff6b6b' }
function anomalyTypeIcon(t)  { return _typeIconMap[t] || '⚠️' }

const anomalyTypeCounts = computed(() => {
  const map = {}
  for (const a of (result.value?.anomalies || [])) {
    map[a.type] = (map[a.type] || 0) + 1
  }
  return map
})

const anomalyLevelPie = computed(() => {
  const list = result.value?.anomalies || []
  const typeMap = {}
  for (const a of list) {
    typeMap[a.type] = (typeMap[a.type] || 0) + 1
  }
  const data = Object.entries(typeMap).map(([name, value]) => ({
    value, name, itemStyle: { color: anomalyTypeColor(name) }
  }))
  return {
    backgroundColor: 'transparent',
    tooltip: { trigger: 'item', backgroundColor: '#0d1b2e', borderColor: '#1a3a5c', textStyle: { color: '#e0f0ff' } },
    legend: { orient: 'vertical', right: 10, top: 'center', textStyle: { color: '#8bafc9', fontSize: 11 }, icon: 'circle' },
    series: [{ type: 'pie', radius: ['40%', '68%'], center: ['40%', '55%'],
      data, label: { color: '#8bafc9', formatter: '{b}\n{d}%' }
    }]
  }
})

async function doAnalysis() {
  if (!form.value.buildingId) { ElMessage.warning('请选择建筑'); return }
  if (form.value.startDate && form.value.endDate && form.value.endDate <= form.value.startDate) {
    ElMessage.error('结束日期不能早于或等于开始日期')
    return
  }
  loading.value = true
  try {
    const req = {
      buildingId: form.value.buildingId,
      analysisType: form.value.analysisType,
      skipAiAnalysis: true
    }
    if (form.value.startDate) req.startTime = form.value.startDate
    if (form.value.endDate) req.endTime = form.value.endDate
    const res = await analysisApi.analyzeEnergy(req)
    if (res.code === 200) {
      result.value = res.data
      displayType.value = form.value.analysisType
      if (form.value.analysisType === 'monthly_report') {
        const sm = (form.value.startDate || '').slice(0, 7)
        const em = (form.value.endDate   || '').slice(0, 7)
        displayScope.value = (sm && em && sm === em) ? 'daily' : 'monthly'
      }
      ElMessage.success('分析完成')
    }
  } catch {
    ElMessage.error('分析失败，请检查后端服务')
  } finally {
    loading.value = false
  }
}

/* ── 公共工具 ── */
function _dateStr() {
  const n = new Date()
  return `${n.getFullYear()}-${String(n.getMonth()+1).padStart(2,'0')}-${String(n.getDate()).padStart(2,'0')}`
}
const _typeLabels = { comprehensive: '综合分析', monthly_report: '月度报告', anomaly_detection: '异常诊断' }

/* ══════════════════════════════════════════════════════
   PDF 导出 —— 正式排版文档（非截图）
   ══════════════════════════════════════════════════════ */
async function exportPDF() {
  if (!result.value) return
  exporting.value = true
  try {
    const r = result.value
    const bldName = nameMap[r.buildingId] || r.buildingId
    const dateStr = _dateStr()
    const typeLabel = _typeLabels[displayType.value] || '能耗分析'

    const doc = new jsPDF({ orientation: 'portrait', unit: 'mm', format: 'a4' })
    const W = 210, H = 297
    const ML = 20, MR = 20, bodyW = W - ML - MR
    let y = 0

    /* ---------- 辅助函数 ---------- */
    function addPageFooter() {
      const pages = doc.internal.getNumberOfPages()
      for (let i = 1; i <= pages; i++) {
        doc.setPage(i)
        doc.setDrawColor(200, 200, 200)
        doc.line(ML, H - 15, W - MR, H - 15)
        doc.setFontSize(8)
        doc.setTextColor(150, 150, 150)
        doc.text(`Page ${i} / ${pages}`, W - MR - 20, H - 10)
        doc.text('Energy Management System', ML, H - 10)
        doc.text(dateStr, W / 2, H - 10, { align: 'center' })
      }
    }
    function ensureSpace(need) {
      if (y + need > H - 20) { doc.addPage(); y = 25; sectionLine() }
    }
    function sectionLine() {
      doc.setDrawColor(220, 220, 220)
      doc.line(ML, y, W - MR, y)
      y += 2
    }
    function heading(text, fontSize, color) {
      ensureSpace(14)
      doc.setFontSize(fontSize)
      doc.setTextColor(...color)
      doc.setFont('helvetica', 'bold')
      doc.text(text, ML, y + 5)
      y += fontSize * 0.5 + 4
      doc.setFont('helvetica', 'normal')
    }
    function bodyText(text, indent) {
      const x = ML + (indent || 0)
      doc.setFontSize(10)
      doc.setTextColor(60, 60, 60)
      const lines = doc.splitTextToSize(text, bodyW - (indent || 0))
      ensureSpace(lines.length * 5 + 2)
      doc.text(lines, x, y)
      y += lines.length * 5 + 2
    }

    /* ---------- 封面 ---------- */
    doc.setFillColor(24, 50, 82)
    doc.rect(0, 0, W, 100, 'F')
    doc.setFillColor(0, 180, 220)
    doc.rect(0, 98, W, 3, 'F')

    doc.setFontSize(28)
    doc.setTextColor(255, 255, 255)
    doc.setFont('helvetica', 'bold')
    doc.text(`${typeLabel}`, W / 2, 40, { align: 'center' })

    doc.setFontSize(14)
    doc.setTextColor(180, 215, 240)
    doc.text(`${bldName}`, W / 2, 55, { align: 'center' })

    doc.setFontSize(11)
    doc.setTextColor(140, 180, 210)
    doc.text(`Building Energy Management System`, W / 2, 68, { align: 'center' })

    doc.setFontSize(10)
    doc.setTextColor(100, 100, 100)
    doc.text(`Report Date: ${dateStr}`, W / 2, 115, { align: 'center' })
    doc.text(`Building: ${bldName}  |  Type: ${typeLabel}  |  Rating: ${r.energyRating || '-'}`, W / 2, 123, { align: 'center' })

    /* ---------- 目录页 ---------- */
    doc.addPage()
    y = 30
    heading('Contents', 18, [24, 50, 82])
    y += 4
    const tocItems = [
      '1.  Executive Summary',
      '2.  Key Performance Indicators',
      '3.  Energy Trend Analysis',
    ]
    if (displayType.value === 'monthly_report') tocItems.push('4.  Monthly / Daily Breakdown')
    tocItems.push(`${tocItems.length + 1}.  Anomaly Detection`)
    if (r.aiAnalysisResult) tocItems.push(`${tocItems.length + 1}.  AI Analysis`)
    tocItems.forEach(item => {
      doc.setFontSize(11)
      doc.setTextColor(50, 50, 50)
      doc.text(item, ML + 5, y)
      y += 8
    })

    /* ---------- 1. Executive Summary ---------- */
    doc.addPage()
    y = 25
    heading('1. Executive Summary', 16, [24, 50, 82])
    sectionLine()
    y += 4

    const totalPower = r.totalPowerConsumption || 0
    const totalWater = r.totalWaterConsumption || 0
    const totalCarbon = r.totalCarbonEmission || 0
    const totalAc = r.totalAcConsumption || 0
    const avgPower = r.avgPowerConsumption || 0
    const acRatio = totalPower > 0 ? (totalAc / totalPower * 100).toFixed(1) : '0'
    const trendCount = (r.trendData || []).length
    const anomalyCount = (r.anomalies || []).length

    bodyText(`This report provides a ${typeLabel} for ${bldName}. The analysis covers ${trendCount} data records, evaluating power consumption, water usage, carbon emissions, and HVAC performance.`)
    y += 2
    bodyText(`Key findings: Total power consumption is ${totalPower.toFixed(2)} kWh with an average of ${avgPower.toFixed(2)} kWh per record. HVAC accounts for ${acRatio}% of total power. The building received an energy efficiency rating of ${r.energyRating || 'N/A'} (score: ${r.energyEfficiencyScore || '-'}/100). A total of ${anomalyCount} anomalies were detected.`)

    /* ---------- 2. KPI ---------- */
    y += 8
    heading('2. Key Performance Indicators', 16, [24, 50, 82])
    sectionLine()
    y += 2

    doc.autoTable({
      startY: y,
      head: [['Indicator', 'Value', 'Unit', 'Description']],
      body: [
        ['Total Power', totalPower.toFixed(2), 'kWh', 'Sum of all power consumption in the period'],
        ['Total Water', totalWater.toFixed(2), 'm\u00B3', 'Sum of water consumption in the period'],
        ['Carbon Emission', totalCarbon.toFixed(2), 'kg', 'Total CO2 equivalent emissions'],
        ['HVAC Power', totalAc.toFixed(2), 'kWh', 'Air conditioning system consumption'],
        ['Avg Power', avgPower.toFixed(2), 'kWh', 'Average per data record'],
        ['HVAC Ratio', acRatio + '%', '-', 'Percentage of HVAC in total power'],
        ['Efficiency Score', String(r.energyEfficiencyScore || '-'), 'pts', 'Composite efficiency score (0~100)'],
        ['Energy Rating', r.energyRating || '-', '-', 'A+ / A / B / C / D / E'],
        ['Data Records', String(trendCount), '-', 'Total number of data points analyzed'],
        ['Anomalies', String(anomalyCount), '-', 'Records exceeding 1.5x average power']
      ],
      theme: 'grid',
      styles: { fontSize: 9, cellPadding: 3, textColor: [40, 40, 40], lineColor: [200, 200, 200], lineWidth: 0.3 },
      headStyles: { fillColor: [24, 50, 82], textColor: [255, 255, 255], fontStyle: 'bold', halign: 'center' },
      bodyStyles: { halign: 'center' },
      columnStyles: { 0: { halign: 'left', fontStyle: 'bold', cellWidth: 35 }, 3: { halign: 'left', cellWidth: 65 } },
      margin: { left: ML, right: MR }
    })
    y = doc.lastAutoTable.finalY + 8

    /* ---------- 3. Trend Data ---------- */
    ensureSpace(40)
    heading('3. Energy Trend Analysis', 16, [24, 50, 82])
    sectionLine()
    y += 2

    const trend = r.trendData || []
    if (trend.length > 0) {
      const previewRows = trend.slice(0, 30)
      bodyText(`Showing first ${previewRows.length} of ${trend.length} records. Full data available in the Excel export.`)
      y += 2

      doc.autoTable({
        startY: y,
        head: [['Time', 'Power (kWh)', 'Water (m\u00B3)', 'Carbon (kg)', 'HVAC (kWh)', 'Status']],
        body: previewRows.map(d => [
          (d.timestamp || '').slice(0, 16),
          Number(d.power || 0).toFixed(2),
          Number(d.water || 0).toFixed(3),
          Number(d.carbon || 0).toFixed(2),
          Number(d.ac || 0).toFixed(2),
          d.status === 'abnormal' ? 'Abnormal' : 'Normal'
        ]),
        theme: 'striped',
        styles: { fontSize: 7.5, cellPadding: 2, textColor: [40, 40, 40], lineColor: [210, 210, 210], lineWidth: 0.2 },
        headStyles: { fillColor: [24, 50, 82], textColor: [255, 255, 255], fontStyle: 'bold', halign: 'center', fontSize: 8 },
        bodyStyles: { halign: 'center' },
        alternateRowStyles: { fillColor: [240, 246, 252] },
        columnStyles: { 0: { halign: 'left', cellWidth: 32 } },
        margin: { left: ML, right: MR }
      })
      y = doc.lastAutoTable.finalY + 8
    }

    /* ---------- 4. Monthly / Daily Breakdown ---------- */
    if (displayType.value === 'monthly_report') {
      ensureSpace(30)
      heading('4. Period Breakdown', 16, [24, 50, 82])
      sectionLine()
      y += 2

      const rows = displayScope.value === 'daily' ? dailyRows.value : monthlyRows.value
      const label = displayScope.value === 'daily' ? 'Date' : 'Month'
      if (rows.length > 0) {
        doc.autoTable({
          startY: y,
          head: [[label, 'Power (kWh)', 'Water (m\u00B3)', 'Carbon (kg)', 'HVAC (kWh)', 'Records', 'HVAC %']],
          body: rows.map(d => [
            d.day || d.month,
            d.power.toFixed(1),
            d.water.toFixed(2),
            d.carbon.toFixed(1),
            d.ac.toFixed(1),
            d.count,
            d.power > 0 ? (d.ac / d.power * 100).toFixed(1) + '%' : '0%'
          ]),
          theme: 'striped',
          styles: { fontSize: 8, cellPadding: 2.5, textColor: [40, 40, 40], lineColor: [210, 210, 210], lineWidth: 0.2 },
          headStyles: { fillColor: [46, 125, 50], textColor: [255, 255, 255], fontStyle: 'bold', halign: 'center' },
          bodyStyles: { halign: 'center' },
          alternateRowStyles: { fillColor: [232, 245, 233] },
          columnStyles: { 0: { halign: 'left', fontStyle: 'bold' } },
          margin: { left: ML, right: MR }
        })
        y = doc.lastAutoTable.finalY + 8
      }
    }

    /* ---------- 5. Anomalies ---------- */
    const anomalies = r.anomalies || []
    ensureSpace(30)
    const aSectionNum = displayType.value === 'monthly_report' ? 5 : 4
    heading(`${aSectionNum}. Anomaly Detection`, 16, [180, 40, 40])
    sectionLine()
    y += 2

    if (anomalies.length > 0) {
      const maxExcess = Math.max(...anomalies.map(a => a.excess || 0))
      const avgExcess = anomalies.reduce((s, a) => s + (a.excess || 0), 0) / anomalies.length
      bodyText(`Detected ${anomalies.length} anomalous records (power > 1.5x average). Maximum excess: +${(maxExcess * 100).toFixed(1)}%, average excess: +${(avgExcess * 100).toFixed(1)}%. Top 50 anomalies listed below:`)
      y += 2

      doc.autoTable({
        startY: y,
        head: [['#', 'Timestamp', 'Power (kWh)', 'Excess', 'Severity']],
        body: anomalies.slice(0, 50).map((a, i) => [
          i + 1,
          (a.timestamp || '').slice(0, 16),
          Number(a.power).toFixed(2),
          `+${(a.excess * 100).toFixed(1)}%`,
          a.excess > 1 ? 'SEVERE' : a.excess > 0.5 ? 'WARNING' : 'MILD'
        ]),
        theme: 'striped',
        styles: { fontSize: 8, cellPadding: 2.5, textColor: [40, 40, 40], lineColor: [210, 210, 210], lineWidth: 0.2 },
        headStyles: { fillColor: [180, 40, 40], textColor: [255, 255, 255], fontStyle: 'bold', halign: 'center' },
        bodyStyles: { halign: 'center' },
        alternateRowStyles: { fillColor: [255, 240, 240] },
        columnStyles: { 0: { cellWidth: 10 }, 1: { halign: 'left', cellWidth: 35 } },
        didParseCell: (data) => {
          if (data.section === 'body' && data.column.index === 4) {
            const v = data.cell.raw
            if (v === 'SEVERE') data.cell.styles.textColor = [220, 20, 20]
            else if (v === 'WARNING') data.cell.styles.textColor = [200, 120, 0]
            else data.cell.styles.textColor = [80, 160, 80]
          }
        },
        margin: { left: ML, right: MR }
      })
      y = doc.lastAutoTable.finalY + 8
    } else {
      bodyText('No anomalies detected in the selected period. All energy consumption values are within the normal range.')
    }

    /* ---------- 6. AI Analysis ---------- */
    if (r.aiAnalysisResult) {
      ensureSpace(30)
      heading(`${aSectionNum + 1}. AI Insight`, 16, [24, 50, 82])
      sectionLine()
      y += 2
      bodyText(r.aiAnalysisResult)
    }

    /* ---------- Footer ---------- */
    addPageFooter()

    doc.save(`${bldName}_${typeLabel}_${dateStr}.pdf`)
    ElMessage.success('PDF report exported successfully')
  } catch (e) {
    console.error('PDF export error:', e)
    ElMessage.error('PDF export failed: ' + e.message)
  } finally {
    exporting.value = false
  }
}

/* ══════════════════════════════════════════════════════
   Excel 导出 —— ExcelJS 正式排版
   ══════════════════════════════════════════════════════ */
async function exportExcel() {
  if (!result.value) return
  exporting.value = true
  try {
    const r = result.value
    const bldName = nameMap[r.buildingId] || r.buildingId
    const dateStr = _dateStr()
    const typeLabel = _typeLabels[displayType.value] || '能耗分析'

    const wb = new ExcelJS.Workbook()
    wb.creator = 'Energy Management System'
    wb.created = new Date()

    /* ---- 通用样式 ---- */
    const HEADER_FILL = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FF183252' } }
    const HEADER_FONT = { bold: true, color: { argb: 'FFFFFFFF' }, size: 11, name: 'Microsoft YaHei' }
    const BODY_FONT = { size: 10, name: 'Microsoft YaHei' }
    const TITLE_FONT = { bold: true, size: 16, name: 'Microsoft YaHei', color: { argb: 'FF183252' } }
    const SUB_FONT = { size: 10, name: 'Microsoft YaHei', color: { argb: 'FF666666' } }
    const BORDER_THIN = { top: { style: 'thin', color: { argb: 'FFD0D0D0' } }, bottom: { style: 'thin', color: { argb: 'FFD0D0D0' } }, left: { style: 'thin', color: { argb: 'FFD0D0D0' } }, right: { style: 'thin', color: { argb: 'FFD0D0D0' } } }
    const CENTER = { horizontal: 'center', vertical: 'middle', wrapText: false }
    const LEFT = { horizontal: 'left', vertical: 'middle', wrapText: false }

    function styleHeaderRow(ws, rowNum, colCount) {
      const row = ws.getRow(rowNum)
      row.height = 28
      for (let c = 1; c <= colCount; c++) {
        const cell = row.getCell(c)
        cell.fill = HEADER_FILL
        cell.font = HEADER_FONT
        cell.alignment = CENTER
        cell.border = BORDER_THIN
      }
    }
    function styleDataRows(ws, startRow, endRow, colCount) {
      for (let r = startRow; r <= endRow; r++) {
        const row = ws.getRow(r)
        row.height = 22
        const isEven = (r - startRow) % 2 === 0
        for (let c = 1; c <= colCount; c++) {
          const cell = row.getCell(c)
          cell.font = BODY_FONT
          cell.alignment = CENTER
          cell.border = BORDER_THIN
          if (isEven) cell.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FFF5F8FC' } }
        }
      }
    }

    /* ======== Sheet 1: 报告概要 ======== */
    const ws1 = wb.addWorksheet('Report Summary', { properties: { defaultColWidth: 18 } })
    ws1.columns = [
      { width: 22 }, { width: 24 }, { width: 14 }, { width: 42 }
    ]

    ws1.mergeCells('A1:D1')
    const titleCell = ws1.getCell('A1')
    titleCell.value = `${bldName} - ${typeLabel}`
    titleCell.font = TITLE_FONT
    titleCell.alignment = { horizontal: 'center', vertical: 'middle' }
    ws1.getRow(1).height = 40

    ws1.mergeCells('A2:D2')
    const subCell = ws1.getCell('A2')
    subCell.value = `Report Date: ${dateStr}  |  Building: ${bldName}  |  Rating: ${r.energyRating || '-'}`
    subCell.font = SUB_FONT
    subCell.alignment = { horizontal: 'center', vertical: 'middle' }
    ws1.getRow(2).height = 24

    ws1.getRow(3).height = 8

    const kpiHeader = ['Indicator', 'Value', 'Unit', 'Description']
    const kpiRow = ws1.addRow(kpiHeader)
    styleHeaderRow(ws1, kpiRow.number, 4)

    const totalPower = r.totalPowerConsumption || 0
    const totalWater = r.totalWaterConsumption || 0
    const totalCarbon = r.totalCarbonEmission || 0
    const totalAc = r.totalAcConsumption || 0
    const avgPower = r.avgPowerConsumption || 0
    const acRatio = totalPower > 0 ? (totalAc / totalPower * 100) : 0
    const trendCount = (r.trendData || []).length
    const anomalyCount = (r.anomalies || []).length

    const kpiData = [
      ['Total Power', totalPower.toFixed(2), 'kWh', 'Total electricity consumption'],
      ['Total Water', totalWater.toFixed(2), 'm\u00B3', 'Total water consumption'],
      ['Carbon Emission', totalCarbon.toFixed(2), 'kg', 'CO2 equivalent emissions'],
      ['HVAC Power', totalAc.toFixed(2), 'kWh', 'Air conditioning consumption'],
      ['Average Power', avgPower.toFixed(2), 'kWh', 'Average per record'],
      ['HVAC Ratio', acRatio.toFixed(1) + '%', '-', 'HVAC share of total power'],
      ['Efficiency Score', String(r.energyEfficiencyScore || '-'), 'Points', 'Composite score (0-100)'],
      ['Energy Rating', r.energyRating || '-', '-', 'A+/A/B/C/D/E'],
      ['Data Records', String(trendCount), '-', 'Records analyzed'],
      ['Anomalies', String(anomalyCount), '-', 'Over 1.5x average']
    ]
    const kpiStart = kpiRow.number + 1
    kpiData.forEach(row => ws1.addRow(row))
    styleDataRows(ws1, kpiStart, kpiStart + kpiData.length - 1, 4)

    // 没有单位的行：合并 B+C 列
    kpiData.forEach((row, i) => {
      if (row[2] === '-') {
        const rowNum = kpiStart + i
        ws1.mergeCells(rowNum, 2, rowNum, 3)
        ws1.getRow(rowNum).getCell(2).alignment = CENTER
      }
    })

    // 给评级单元格高亮
    const ratingCell = ws1.getCell(kpiStart + 7, 2)
    const ratingColors = { 'A+': 'FF4CAF50', 'A': 'FF66BB6A', 'B': 'FFFFC107', 'C': 'FFFF9800', 'D': 'FFF44336', 'E': 'FFD32F2F' }
    ratingCell.font = { bold: true, size: 13, name: 'Microsoft YaHei', color: { argb: ratingColors[r.energyRating] || 'FF333333' } }

    /* ======== Sheet 2: 趋势明细 ======== */
    const trend = r.trendData || []
    if (trend.length > 0) {
      const ws2 = wb.addWorksheet('Trend Data')
      ws2.columns = [
        { header: '', width: 20 },
        { header: '', width: 14 },
        { header: '', width: 14 },
        { header: '', width: 14 },
        { header: '', width: 14 },
        { header: '', width: 10 }
      ]
      const h2 = ws2.addRow(['Timestamp', 'Power (kWh)', 'Water (m\u00B3)', 'Carbon (kg)', 'HVAC (kWh)', 'Status'])
      styleHeaderRow(ws2, h2.number, 6)

      trend.forEach(d => {
        ws2.addRow([
          (d.timestamp || '').slice(0, 16),
          Number(d.power || 0),
          Number(d.water || 0),
          Number(d.carbon || 0),
          Number(d.ac || 0),
          d.status === 'abnormal' ? 'Abnormal' : 'Normal'
        ])
      })
      styleDataRows(ws2, h2.number + 1, h2.number + trend.length, 6)

      // 数值列右对齐
      for (let ri = h2.number + 1; ri <= h2.number + trend.length; ri++) {
        for (let ci = 2; ci <= 5; ci++) {
          ws2.getRow(ri).getCell(ci).numFmt = '#,##0.00'
        }
        const sCell = ws2.getRow(ri).getCell(6)
        if (sCell.value === 'Abnormal') {
          sCell.font = { ...BODY_FONT, color: { argb: 'FFFF0000' }, bold: true }
        }
      }
    }

    /* ======== Sheet 3: 汇总（月度/日） ======== */
    if (displayType.value === 'monthly_report') {
      const isDaily = displayScope.value === 'daily'
      const rows = isDaily ? dailyRows.value : monthlyRows.value
      if (rows.length > 0) {
        const ws3 = wb.addWorksheet(isDaily ? 'Daily Summary' : 'Monthly Summary')
        ws3.columns = [
          { width: 14 }, { width: 15 }, { width: 13 }, { width: 15 }, { width: 15 }, { width: 10 }, { width: 13 }
        ]
        const label = isDaily ? 'Date' : 'Month'
        const h3 = ws3.addRow([label, 'Power (kWh)', 'Water (m\u00B3)', 'Carbon (kg)', 'HVAC (kWh)', 'Records', 'HVAC %'])
        styleHeaderRow(ws3, h3.number, 7)

        rows.forEach(d => {
          ws3.addRow([
            d.day || d.month,
            Number(d.power.toFixed(1)),
            Number(d.water.toFixed(2)),
            Number(d.carbon.toFixed(1)),
            Number(d.ac.toFixed(1)),
            d.count,
            d.power > 0 ? Number((d.ac / d.power * 100).toFixed(1)) : 0
          ])
        })
        styleDataRows(ws3, h3.number + 1, h3.number + rows.length, 7)

        // 合计行
        const sumRow = ws3.addRow([
          'TOTAL',
          rows.reduce((s, d) => s + d.power, 0),
          rows.reduce((s, d) => s + d.water, 0),
          rows.reduce((s, d) => s + d.carbon, 0),
          rows.reduce((s, d) => s + d.ac, 0),
          rows.reduce((s, d) => s + d.count, 0),
          ''
        ])
        sumRow.height = 26
        for (let c = 1; c <= 7; c++) {
          const cell = sumRow.getCell(c)
          cell.font = { ...BODY_FONT, bold: true }
          cell.alignment = CENTER
          cell.border = BORDER_THIN
          cell.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FFE8EEF4' } }
        }
      }
    }

    /* ======== Sheet 4: 异常记录 ======== */
    const anomalies = r.anomalies || []
    if (anomalies.length > 0) {
      const ws4 = wb.addWorksheet('Anomalies')
      ws4.columns = [
        { width: 8 }, { width: 20 }, { width: 14 }, { width: 14 }, { width: 14 }, { width: 12 }
      ]
      const h4 = ws4.addRow(['#', 'Timestamp', 'Power (kWh)', 'Excess', 'Type', 'Severity'])
      styleHeaderRow(ws4, h4.number, 6)
      ws4.getRow(h4.number).getCell(1).fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FFB42828' } }
      for (let ci = 2; ci <= 6; ci++) ws4.getRow(h4.number).getCell(ci).fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FFB42828' } }

      anomalies.forEach((a, i) => {
        const severity = a.excess > 1 ? 'SEVERE' : a.excess > 0.5 ? 'WARNING' : 'MILD'
        ws4.addRow([
          i + 1,
          (a.timestamp || '').slice(0, 16),
          Number(a.power),
          `+${(a.excess * 100).toFixed(1)}%`,
          a.type || 'High Power',
          severity
        ])
      })
      styleDataRows(ws4, h4.number + 1, h4.number + anomalies.length, 6)

      // 严重等级颜色标注
      for (let ri = h4.number + 1; ri <= h4.number + anomalies.length; ri++) {
        const sev = ws4.getRow(ri).getCell(6).value
        const color = sev === 'SEVERE' ? 'FFFF0000' : sev === 'WARNING' ? 'FFFF8C00' : 'FF52C41A'
        ws4.getRow(ri).getCell(6).font = { ...BODY_FONT, bold: true, color: { argb: color } }
      }
    }

    /* ---- 导出 ---- */
    const buffer = await wb.xlsx.writeBuffer()
    const blob = new Blob([buffer], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${bldName}_${typeLabel}_${dateStr}.xlsx`
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('Excel report exported successfully')
  } catch (e) {
    console.error('Excel export error:', e)
    ElMessage.error('Excel export failed: ' + e.message)
  } finally {
    exporting.value = false
  }
}

function resetForm() {
  form.value = { buildingId: buildingIds.value[0] || '', startDate: null, endDate: null, analysisType: 'comprehensive' }
  result.value = null
  doAnalysis()
}

onMounted(async () => {
  try {
    await loadBuildingNames()
    const res = await buildingApi.getAllBuildingIds()
    if (res.code === 200) buildingIds.value = res.data
  } catch {
    buildingIds.value = []
  }
  if (buildingIds.value.length && !form.value.buildingId) form.value.buildingId = buildingIds.value[0]
  const qId = route.query.buildingId
  if (qId) form.value.buildingId = qId
  await loadAvailableDates(form.value.buildingId)
  doAnalysis()
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
  box-shadow: 0 2px 8px rgba(0,0,0,0.2);
}

.export-group {
  display: flex;
  align-items: center;
  margin-left: auto;
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
  box-shadow: 0 2px 8px rgba(0,0,0,0.2);
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
  box-shadow: 0 2px 8px rgba(0,0,0,0.2);
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
  border: 1px solid #1a3a5c;
  border-radius: 12px;
  padding: 16px;
}

.anomaly-type-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 10px 0 14px;
}

.ats-chip {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 5px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
}
.ats-chip b { font-weight: 700; margin-left: 2px; }

.atype-badge {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 500;
  white-space: nowrap;
}

.empty-state { margin: 60px auto; }

/* ── 板块横幅 ── */
.section-banner {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 18px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  color: #e0f0ff;
}
.monthly-banner  { background: linear-gradient(90deg, #0f2540, #0d1b2e); border: 1px solid #1a3a5c; }
.anomaly-banner  { background: linear-gradient(90deg, #2a1520, #0d1b2e); border: 1px solid #ffb3b3; }

/* ── 月度报告 / 日报告 KPI 5列 ── */
.kpi-row-5 {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 12px;
}

/* ── 异常诊断统计行 ── */
.anomaly-stat-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}
.anstat-card {
  background: #0d1b2e;
  border: 1px solid #1a3a5c;
  border-radius: 12px;
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 14px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.2);
}
.anstat-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: #ffffff;
}
.anstat-val   { font-size: 22px; font-weight: 700; line-height: 1.2; }
.anstat-label { font-size: 12px; color: #8bafc9; margin-top: 2px; }

/* ── 异常卡片网格 ── */
.anomaly-cards-wrap {
  background: #0d1b2e;
  border: 1px solid #ffb3b3;
  border-radius: 12px;
  padding: 16px;
}
.anomaly-card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 10px;
  margin-top: 8px;
}
.anomaly-card {
  border-radius: 10px;
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  border: 1px solid;
}
.ac-severe  { background: #2a1520; border-color: #ffb3b3; }
.ac-warning { background: #2a2515; border-color: #ffd77a; }
.ac-rank    { font-size: 11px; color: #5a7a9a; }
.ac-time    { font-size: 12px; color: #8bafc9; }
.ac-power   { font-size: 18px; font-weight: 700; color: #ff6b6b; }
.ac-unit    { font-size: 11px; color: #8bafc9; }

</style>
