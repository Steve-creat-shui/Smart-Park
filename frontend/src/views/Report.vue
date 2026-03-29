<template>
  <div class="report-page">
    <div class="report-config-card">
      <div class="card-header">
        <span class="card-title">报告配置</span>
      </div>
      <div class="config-form">
        <el-select v-model="form.buildingId" placeholder="选择建筑" style="width:160px">
          <el-option v-for="id in buildingIds" :key="id" :label="id" :value="id" />
        </el-select>
        <el-date-picker v-model="form.dateRange" type="monthrange"
          range-separator="至" start-placeholder="开始月份" end-placeholder="结束月份"
          format="YYYY-MM" value-format="YYYY-MM-DD HH:mm:ss" style="width:280px" />
        <el-checkbox-group v-model="form.sections">
          <el-checkbox value="overview">能耗概况</el-checkbox>
          <el-checkbox value="trend">趋势分析</el-checkbox>
          <el-checkbox value="anomaly">异常诊断</el-checkbox>
          <el-checkbox value="ai">AI建议</el-checkbox>
        </el-checkbox-group>
        <el-button type="primary" :loading="reportLoading" @click="generateReport" icon="Document">
          生成报告
        </el-button>
        <el-button type="success" :disabled="!reportData" @click="exportExcel" icon="Download" :loading="exporting">
          导出 Excel
        </el-button>
        <el-button type="warning" :disabled="!reportData" @click="exportPDF" icon="Download">
          导出 PDF
        </el-button>
      </div>
    </div>

    <div v-if="reportData" class="report-preview">
      <div class="report-header-section">
        <div class="report-logo">⚡</div>
        <div class="report-title-block">
          <h1 class="report-main-title">建筑能效分析报告</h1>
          <div class="report-meta">
            <span>建筑编号：{{ reportData.buildingId }}</span>
            <span>生成时间：{{ generateTime }}</span>
            <el-tag :class="'rating-tag-' + reportData.energyRating" effect="dark">
              能效评级：{{ reportData.energyRating }}
            </el-tag>
          </div>
        </div>
      </div>

      <div class="kpi-section" v-if="form.sections.includes('overview')">
        <div class="section-title">一、能耗概况</div>
        <div class="kpi-grid">
          <div class="kpi-box" v-for="kpi in kpiItems" :key="kpi.key">
            <div class="kpi-icon" :style="{ background: kpi.bg }">
              <el-icon :size="20"><component :is="kpi.icon" /></el-icon>
            </div>
            <div class="kpi-data">
              <div class="kpi-v" :style="{ color: kpi.color }">{{ kpi.value }}</div>
              <div class="kpi-l">{{ kpi.label }}</div>
            </div>
          </div>
        </div>
        <div class="score-bar">
          <span class="score-label">综合能效得分</span>
          <el-progress
            :percentage="reportData.energyEfficiencyScore || 0"
            :color="scoreColor"
            :stroke-width="16"
            style="flex:1"
          />
          <span class="score-value">{{ reportData.energyEfficiencyScore?.toFixed(1) }}/100</span>
        </div>
      </div>

      <div class="trend-section" v-if="form.sections.includes('trend')">
        <div class="section-title">二、能耗趋势分析</div>
        <div class="chart-row">
          <div class="chart-box">
            <div class="chart-sub-title">电力消耗趋势 (kWh)</div>
            <v-chart :option="powerTrendOption" style="height:220px" autoresize />
          </div>
          <div class="chart-box">
            <div class="chart-sub-title">碳排放趋势 (kg)</div>
            <v-chart :option="carbonTrendOption" style="height:220px" autoresize />
          </div>
        </div>
      </div>

      <div class="anomaly-section" v-if="form.sections.includes('anomaly') && reportData.anomalies?.length > 0">
        <div class="section-title">三、异常分析 <el-tag type="danger" size="small">{{ reportData.anomalies.length }} 条</el-tag></div>
        <el-table :data="reportData.anomalies.slice(0, 10)" size="small"
          :header-cell-style="headerStyle" :cell-style="cellStyle">
          <el-table-column prop="timestamp" label="发生时间" />
          <el-table-column prop="power" label="能耗 (kWh)" width="120">
            <template #default="{ row }">
              <span style="color:#ff6b6b">{{ Number(row.power).toFixed(2) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="excess" label="超出比例" width="120">
            <template #default="{ row }">
              <el-tag type="danger" size="small">+{{ (row.excess * 100).toFixed(1) }}%</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="处理建议" min-width="200">
            <template #default>
              <span style="color:#fadb14">检查设备运行状态，排查非工作时段用电</span>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="ai-section" v-if="form.sections.includes('ai')">
        <div class="section-title">四、AI 智能分析与建议</div>
        <div class="ai-result-box" v-html="formattedAiResult"></div>
      </div>

      <div class="report-footer">
        <div class="footer-disclaimer">本报告由建筑能源数字孪生管理系统自动生成，仅供参考。</div>
        <div class="footer-time">报告生成时间：{{ generateTime }}</div>
      </div>
    </div>

    <el-empty v-else description="请配置报告参数后点击生成报告" class="empty-state" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { LineChart, BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { buildingApi, reportApi } from '../api/index.js'
import { ElMessage } from 'element-plus'
import jsPDF from 'jspdf'
import dayjs from 'dayjs'

use([LineChart, BarChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer])

const buildingIds = ref([])
const reportLoading = ref(false)
const exporting = ref(false)
const reportData = ref(null)
const generateTime = ref('')

const form = ref({
  buildingId: '',
  dateRange: null,
  sections: ['overview', 'trend', 'anomaly', 'ai']
})

const kpiItems = computed(() => {
  if (!reportData.value) return []
  return [
    { key: 'power', label: '总电力消耗', value: (reportData.value.totalPowerConsumption || 0).toFixed(2) + ' kWh', icon: 'Lightning', color: '#00d4ff', bg: '#00d4ff22' },
    { key: 'water', label: '总水能耗', value: (reportData.value.totalWaterConsumption || 0).toFixed(2) + ' m³', icon: 'WatermelonFilled', color: '#52c41a', bg: '#52c41a22' },
    { key: 'carbon', label: '总碳排放', value: (reportData.value.totalCarbonEmission || 0).toFixed(2) + ' kg', icon: 'Cloudy', color: '#fadb14', bg: '#fadb1422' },
    { key: 'ac', label: '空调能耗', value: (reportData.value.totalAcConsumption || 0).toFixed(2) + ' kWh', icon: 'Wind', color: '#8b5cf6', bg: '#8b5cf622' }
  ]
})

const scoreColor = computed(() => {
  const s = reportData.value?.energyEfficiencyScore || 0
  if (s >= 80) return '#52c41a'
  if (s >= 60) return '#fadb14'
  return '#ff6b6b'
})

const formattedAiResult = computed(() => {
  if (!reportData.value?.aiAnalysisResult) return ''
  return reportData.value.aiAnalysisResult
    .replace(/\*\*(.*?)\*\*/g, '<strong style="color:#00d4ff">$1</strong>')
    .replace(/\n/g, '<br>')
    .replace(/^\d+\. /gm, '<span style="color:#00d4ff">▸</span> ')
})

const powerTrendOption = computed(() => {
  const trend = reportData.value?.trendData || []
  return {
    backgroundColor: 'transparent',
    tooltip: { trigger: 'axis', backgroundColor: '#0d1b2e', borderColor: '#1a3a5c', textStyle: { color: '#e0f0ff' } },
    grid: { top: 20, bottom: 40, left: 60, right: 20 },
    xAxis: { type: 'category', data: trend.map(d => d.timestamp?.slice(0, 10) || ''), axisLabel: { color: '#8bafc9', rotate: 30, fontSize: 10 }, axisLine: { lineStyle: { color: '#1a3a5c' } } },
    yAxis: { type: 'value', axisLabel: { color: '#8bafc9' }, splitLine: { lineStyle: { color: '#1a3a5c' } } },
    series: [{ type: 'line', data: trend.map(d => d.power), smooth: true, symbol: 'none', itemStyle: { color: '#00d4ff' }, areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: '#00d4ff44' }, { offset: 1, color: '#00d4ff00' }] } } }]
  }
})

const carbonTrendOption = computed(() => {
  const trend = reportData.value?.trendData || []
  return {
    backgroundColor: 'transparent',
    tooltip: { trigger: 'axis', backgroundColor: '#0d1b2e', borderColor: '#1a3a5c', textStyle: { color: '#e0f0ff' } },
    grid: { top: 20, bottom: 40, left: 60, right: 20 },
    xAxis: { type: 'category', data: trend.map(d => d.timestamp?.slice(0, 10) || ''), axisLabel: { color: '#8bafc9', rotate: 30, fontSize: 10 }, axisLine: { lineStyle: { color: '#1a3a5c' } } },
    yAxis: { type: 'value', axisLabel: { color: '#8bafc9' }, splitLine: { lineStyle: { color: '#1a3a5c' } } },
    series: [{ type: 'line', data: trend.map(d => d.carbon), smooth: true, symbol: 'none', itemStyle: { color: '#52c41a' }, areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: '#52c41a44' }, { offset: 1, color: '#52c41a00' }] } } }]
  }
})

const headerStyle = () => ({ background: '#0d1b2e', color: '#8bafc9', borderBottom: '1px solid #1a3a5c' })
const cellStyle = () => ({ background: 'transparent', color: '#e0f0ff', borderBottom: '1px solid #1a3a5c11' })

async function generateReport() {
  if (!form.value.buildingId) { ElMessage.warning('请选择建筑'); return }
  reportLoading.value = true
  try {
    const res = await reportApi.getReport(form.value.buildingId)
    if (res.code === 200) {
      reportData.value = res.data
      generateTime.value = dayjs().format('YYYY-MM-DD HH:mm:ss')
      ElMessage.success('报告生成成功')
    }
  } catch {
    ElMessage.error('报告生成失败，请检查后端服务')
  } finally {
    reportLoading.value = false
  }
}

async function exportExcel() {
  if (!form.value.buildingId) return
  exporting.value = true
  try {
    const res = await reportApi.exportReport(form.value.buildingId)
    const url = window.URL.createObjectURL(new Blob([res.data]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', `energy_report_${form.value.buildingId}_${dayjs().format('YYYYMMDD')}.xlsx`)
    document.body.appendChild(link)
    link.click()
    link.remove()
    ElMessage.success('Excel报告已下载')
  } catch {
    ElMessage.error('导出失败，请检查后端服务')
  } finally {
    exporting.value = false
  }
}

function exportPDF() {
  if (!reportData.value) return
  const doc = new jsPDF({ orientation: 'portrait', unit: 'mm', format: 'a4' })
  doc.setFont('helvetica')

  doc.setFontSize(18)
  doc.setTextColor(0, 100, 200)
  doc.text('Building Energy Efficiency Report', 105, 20, { align: 'center' })

  doc.setFontSize(11)
  doc.setTextColor(100)
  doc.text(`Building ID: ${reportData.value.buildingId}`, 20, 35)
  doc.text(`Generated: ${generateTime.value}`, 20, 42)
  doc.text(`Energy Rating: ${reportData.value.energyRating}`, 20, 49)
  doc.text(`Efficiency Score: ${reportData.value.energyEfficiencyScore?.toFixed(1)}/100`, 20, 56)

  doc.setFontSize(13)
  doc.setTextColor(0, 100, 200)
  doc.text('Energy Consumption Summary', 20, 70)

  doc.setFontSize(10)
  doc.setTextColor(50)
  const items = [
    `Total Power Consumption: ${reportData.value.totalPowerConsumption?.toFixed(2)} kWh`,
    `Total Water Consumption: ${reportData.value.totalWaterConsumption?.toFixed(2)} m3`,
    `Total Carbon Emission: ${reportData.value.totalCarbonEmission?.toFixed(2)} kg`,
    `AC System Consumption: ${reportData.value.totalAcConsumption?.toFixed(2)} kWh`,
    `Average Power Consumption: ${reportData.value.avgPowerConsumption?.toFixed(2)} kWh`
  ]
  items.forEach((item, i) => doc.text(item, 25, 80 + i * 8))

  const anomalyCount = reportData.value.anomalies?.length || 0
  doc.setFontSize(13)
  doc.setTextColor(0, 100, 200)
  doc.text('Anomaly Detection', 20, 130)
  doc.setFontSize(10)
  doc.setTextColor(50)
  doc.text(`Detected ${anomalyCount} anomalies exceeding 150% of average consumption.`, 25, 140)

  doc.setFontSize(13)
  doc.setTextColor(0, 100, 200)
  doc.text('AI Analysis Summary', 20, 155)
  doc.setFontSize(9)
  doc.setTextColor(50)
  const aiText = (reportData.value.aiAnalysisResult || '').replace(/\*\*(.*?)\*\*/g, '$1').replace(/\n/g, ' ').slice(0, 500)
  const lines = doc.splitTextToSize(aiText, 170)
  doc.text(lines.slice(0, 12), 25, 165)

  doc.setFontSize(8)
  doc.setTextColor(150)
  doc.text('Generated by Building Energy Digital Twin Management System', 105, 285, { align: 'center' })

  doc.save(`energy_report_${form.value.buildingId}_${dayjs().format('YYYYMMDD')}.pdf`)
  ElMessage.success('PDF报告已下载')
}

onMounted(async () => {
  try {
    const res = await buildingApi.getAllBuildingIds()
    if (res.code === 200) {
      buildingIds.value = res.data
      if (res.data.length > 0) form.value.buildingId = res.data[0]
    }
  } catch {
    buildingIds.value = ['B001', 'B002', 'B003', 'B004']
    form.value.buildingId = 'B001'
  }
})
</script>

<style scoped>
.report-page { display: flex; flex-direction: column; gap: 16px; }

.report-config-card {
  background: #0d1b2e;
  border: 1px solid #1a3a5c;
  border-radius: 12px;
  padding: 16px;
}

.config-form {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 12px;
}

.report-preview {
  background: #0d1b2e;
  border: 1px solid #1a3a5c;
  border-radius: 12px;
  padding: 24px;
}

.report-header-section {
  display: flex;
  align-items: center;
  gap: 20px;
  padding-bottom: 20px;
  border-bottom: 2px solid #00d4ff33;
  margin-bottom: 24px;
}

.report-logo { font-size: 48px; }
.report-main-title { font-size: 24px; font-weight: 800; color: #e0f0ff; margin-bottom: 8px; }
.report-meta { display: flex; align-items: center; gap: 16px; color: #8bafc9; font-size: 13px; }

.section-title {
  font-size: 16px;
  font-weight: 700;
  color: #00d4ff;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.kpi-section, .trend-section, .anomaly-section, .ai-section {
  margin-bottom: 24px;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 16px;
}

.kpi-box {
  background: #0a1628;
  border: 1px solid #1a3a5c;
  border-radius: 10px;
  padding: 14px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.kpi-icon {
  width: 44px; height: 44px; border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  color: currentColor; flex-shrink: 0;
}

.kpi-v { font-size: 18px; font-weight: 700; }
.kpi-l { font-size: 12px; color: #8bafc9; margin-top: 3px; }

.score-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  background: #0a1628;
  border-radius: 10px;
  padding: 14px 16px;
}
.score-label { font-size: 13px; color: #8bafc9; white-space: nowrap; }
.score-value { font-size: 13px; font-weight: 700; color: #e0f0ff; white-space: nowrap; }

.chart-row { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.chart-box {
  background: #0a1628;
  border: 1px solid #1a3a5c;
  border-radius: 10px;
  padding: 14px;
}
.chart-sub-title { font-size: 13px; color: #8bafc9; margin-bottom: 8px; }

.ai-result-box {
  background: #0a1628;
  border: 1px solid #1a3a5c;
  border-radius: 10px;
  padding: 16px;
  color: #e0f0ff;
  font-size: 13px;
  line-height: 1.8;
  white-space: pre-wrap;
}

.report-footer {
  border-top: 1px solid #1a3a5c;
  padding-top: 16px;
  margin-top: 24px;
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #8bafc9;
}

.empty-state { margin: 60px auto; }

.card-header { margin-bottom: 12px; }
.card-title { font-size: 14px; font-weight: 600; color: #e0f0ff; }
</style>
