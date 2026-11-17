<template>
  <div class="page-container">
    <!-- 首次使用引导 -->
    <div v-if="showOnboarding" class="onboarding-banner">
      <div class="onboarding-icon">{{ authStore.isEnterprise ? '🏢' : '👤' }}</div>
      <div class="onboarding-body">
        <h2>欢迎使用{{ authStore.isEnterprise ? (authStore.user?.enterpriseName || '企业') : '' }}能源管理平台</h2>
        <p>请先导入您的建筑能耗数据，导入完成后即可使用系统全部功能</p>
        <div class="onboarding-steps">
          <div class="ob-step"><span class="ob-num">1</span>准备 Excel/CSV 能耗数据文件</div>
          <div class="ob-step"><span class="ob-num">2</span>点击下方上传区域导入数据</div>
          <div class="ob-step"><span class="ob-num">3</span>导入成功后进入系统总览</div>
        </div>
      </div>
    </div>

    <el-tabs v-model="activeTab" class="data-tabs">

      <!-- Tab 1: 批量导入 -->
      <el-tab-pane label="批量导入" name="import">
        <div class="tab-content">
          <el-upload
            class="upload-zone"
            drag
            action="#"
            :auto-upload="false"
            :on-change="handleFileChange"
            :limit="1"
            :on-exceed="() => ElMessage.warning('每次只能上传一个文件')"
            accept=".csv,.xlsx,.xls"
          >
            <el-icon class="upload-icon"><UploadFilled /></el-icon>
            <div class="upload-text">拖拽文件到此处，或 <em>点击上传</em></div>
            <div class="upload-hint">支持 .csv / .xlsx / .xls，列顺序：建筑名称, 时间, 电力(kWh), 水耗(m³), 碳排放(kg), 空调(kWh), 照明(kWh), 其他(kWh), 状态</div>
          </el-upload>

          <div class="upload-actions" v-if="selectedFile">
            <span class="file-name"><el-icon><Document /></el-icon> {{ selectedFile.name }}</span>
            <el-button type="info" plain size="small" :loading="previewing" @click="doPreview">预览前10行</el-button>
            <el-button type="primary" :loading="importing" @click="doImport">确认导入</el-button>
            <el-button text @click="clearFile">取消</el-button>
          </div>

          <div v-if="previewRows.length > 0" class="preview-table">
            <div class="section-title">数据预览（前 {{ previewRows.length }} 行）</div>
            <el-table :data="previewRows" size="small" class="dark-table" :row-class-name="tableRowClass">
              <el-table-column prop="buildingId" label="建筑名称" min-width="100" />
              <el-table-column prop="timestamp" label="时间戳" min-width="150" />
              <el-table-column prop="powerConsumptionKwh" label="电力(kWh)" width="100" />
              <el-table-column prop="waterConsumptionM3" label="水(m³)" width="80" />
              <el-table-column prop="carbonEmissionKg" label="碳排(kg)" width="90" />
              <el-table-column prop="acConsumptionKwh" label="空调(kWh)" width="90" />
              <el-table-column prop="status" label="状态" width="80">
                <template #default="{ row }">
                  <el-tag :type="row.status === 'normal' ? 'success' : 'warning'" size="small">{{ row.status }}</el-tag>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>
      </el-tab-pane>

      <!-- Tab 2: 数据面板 -->
      <el-tab-pane label="数据面板" name="panel">
        <div class="tab-content">
          <div class="filter-row">
            <el-select v-model="panelBuilding" placeholder="选择建筑" style="width:140px" @change="loadPanelData(1)">
              <el-option v-for="id in buildingIds" :key="id" :label="nameMap[id] || id" :value="id" />
            </el-select>
            <el-date-picker
              v-model="panelDateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              value-format="YYYY-MM-DD"
              style="width:280px"
              @change="loadPanelData(1)"
            />
            <el-button type="primary" plain @click="loadPanelData(1)">查询</el-button>
            <el-button type="success" plain :disabled="!panelBuilding" @click="exportExcel">
              <el-icon><Download /></el-icon> 导出 Excel
            </el-button>
          </div>

          <el-table :data="panelRows" v-loading="panelLoading" size="small" class="dark-table" :row-class-name="tableRowClass" style="margin-top:12px">
            <el-table-column prop="timestamp" label="时间戳" min-width="160" />
            <el-table-column prop="powerConsumptionKwh" label="电力(kWh)" width="110" />
            <el-table-column prop="waterConsumptionM3" label="水(m³)" width="90" />
            <el-table-column prop="carbonEmissionKg" label="碳排(kg)" width="100" />
            <el-table-column prop="acConsumptionKwh" label="空调(kWh)" width="100" />
            <el-table-column prop="status" label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="row.status === 'normal' ? 'success' : 'warning'" size="small">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
          </el-table>

          <el-pagination
            v-if="panelTotal > 0"
            style="margin-top:12px; justify-content:flex-end; display:flex"
            layout="total, prev, pager, next"
            :total="panelTotal"
            :page-size="panelPageSize"
            :current-page="panelPage"
            @current-change="loadPanelData"
          />
        </div>
      </el-tab-pane>

      <!-- Tab 3: 导入历史 -->
      <el-tab-pane label="导入历史" name="history">
        <div class="tab-content">
          <div class="filter-row">
            <span style="color:#8bafc9;font-size:13px">共 {{ importHistoryList.length }} 条记录</span>
            <el-button type="primary" size="small" @click="loadHistory" :loading="historyLoading">刷新</el-button>
          </div>
          <el-table :data="importHistoryList" v-loading="historyLoading" size="small" class="dark-table" :row-class-name="tableRowClass" style="margin-top:12px" empty-text="暂无导入记录">
            <el-table-column prop="id" label="ID" width="60" />
            <el-table-column prop="fileName" label="文件名" min-width="200" show-overflow-tooltip />
            <el-table-column label="文件大小" width="100">
              <template #default="{ row }">
                {{ row.fileSize ? (row.fileSize / 1024).toFixed(1) + ' KB' : '—' }}
              </template>
            </el-table-column>
            <el-table-column prop="recordCount" label="导入行数" width="90" />
            <el-table-column prop="userName" label="操作人" width="100">
              <template #default="{ row }">{{ row.userName || '—' }}</template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="{completed:'success',processing:'warning',failed:'danger'}[row.status]" size="small" effect="dark">
                  {{ {completed:'成功',processing:'处理中',failed:'失败'}[row.status] || row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="导入时间" width="160">
              <template #default="{ row }">{{ row.createdAt?.slice(0, 19) }}</template>
            </el-table-column>
            <el-table-column prop="errorMessage" label="错误信息" min-width="200" show-overflow-tooltip>
              <template #default="{ row }">
                <span style="color:#ff6b6b;font-size:12px">{{ row.errorMessage || '' }}</span>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <!-- Tab 4: 指标对比 -->
      <el-tab-pane label="指标对比" name="normalize">
        <div class="tab-content">
          <div class="filter-row">
            <el-select v-model="normBuilding" placeholder="选择建筑" style="width:140px">
              <el-option v-for="id in buildingIds" :key="id" :label="nameMap[id] || id" :value="id" />
            </el-select>
            <el-date-picker
              v-model="normDateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              value-format="YYYY-MM-DD"
              style="width:280px"
            />
            <el-button type="primary" :loading="normLoading" @click="loadNormData">分析</el-button>
          </div>

          <!-- KPI Summary Cards -->
          <div class="norm-kpi-row" v-if="normChartData.length > 0">
            <div class="norm-kpi-card" v-for="m in normChartData" :key="m.name">
              <div class="norm-kpi-icon" :style="{background: m.color + '22', color: m.color}">{{ m.icon }}</div>
              <div class="norm-kpi-body">
                <div class="norm-kpi-label">{{ m.name }}</div>
                <div class="norm-kpi-value" :style="{color: m.color}">{{ m.building.toFixed(1) }}</div>
                <div class="norm-kpi-diff" :class="m.diffPct > 0 ? 'diff-up' : 'diff-down'">
                  {{ m.diffPct > 0 ? '▲' : '▼' }} {{ Math.abs(m.diffPct).toFixed(1) }}% vs 全局
                </div>
              </div>
            </div>
          </div>

          <!-- Charts Row -->
          <div class="norm-charts-row" v-show="normChartData.length > 0">
            <div ref="normChartRef" class="norm-chart-main" />
            <div ref="normRadarRef" class="norm-chart-radar" />
          </div>

          <el-empty v-if="normChartData.length === 0 && !normLoading" description="请选择建筑和时间范围后点击分析" class="empty-state" />
        </div>
      </el-tab-pane>

    </el-tabs>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled, Document, Download } from '@element-plus/icons-vue'
import * as echarts from 'echarts/core'
import { BarChart, RadarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent, RadarComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { buildingApi, energyApi, reportApi, dataApi } from '../api/index.js'
import { loadBuildingNames, buildingNameMap } from '../utils/buildingName.js'
import { useAuthStore } from '../stores/auth.js'
import { useRouter } from 'vue-router'
import axios from 'axios'

echarts.use([BarChart, RadarChart, GridComponent, TooltipComponent, LegendComponent, RadarComponent, CanvasRenderer])

const authStore = useAuthStore()
const router = useRouter()
const showOnboarding = computed(() => authStore.user?.hasData === false)
const activeTab = ref('import')
const buildingIds = ref([])
const nameMap = reactive(buildingNameMap)

function tableRowClass({ rowIndex }) {
  return rowIndex % 2 === 1 ? 'dark-stripe' : ''
}

// --- Tab 1 ---
const selectedFile = ref(null)
const previewRows = ref([])
const previewing = ref(false)
const importing = ref(false)

function handleFileChange(file) {
  selectedFile.value = file.raw
  previewRows.value = []
}

function clearFile() {
  selectedFile.value = null
  previewRows.value = []
}

async function doPreview() {
  if (!selectedFile.value) return
  previewing.value = true
  try {
    const fd = new FormData()
    fd.append('file', selectedFile.value)
    const res = await dataApi.previewFile(fd)
    if (res.code === 200) {
      previewRows.value = res.data
      ElMessage.success('预览成功')
    } else {
      ElMessage.error(res.message)
    }
  } catch (e) {
    // handled by interceptor
  } finally {
    previewing.value = false
  }
}

async function doImport() {
  if (!selectedFile.value) return
  importing.value = true
  try {
    const fd = new FormData()
    fd.append('file', selectedFile.value)
    const res = await dataApi.importFile(fd)
    if (res.code === 200) {
      ElMessage.success(res.data.message)
      clearFile()
      loadHistory()
      // 首次导入后更新状态并跳转
      if (showOnboarding.value) {
        authStore.user.hasData = true
        localStorage.setItem('user', JSON.stringify(authStore.user))
        ElMessage.success('数据导入完成！正在跳转到系统总览...')
        setTimeout(() => router.push('/dashboard'), 1500)
      }
    } else {
      ElMessage.error(res.message)
    }
  } catch (e) {
    // handled by interceptor
  } finally {
    importing.value = false
  }
}

// --- Tab 1.5: Import History ---
const importHistoryList = ref([])
const historyLoading = ref(false)

async function loadHistory() {
  historyLoading.value = true
  try {
    const res = await dataApi.getImportHistory()
    if (res.code === 200) importHistoryList.value = res.data || []
  } catch (e) {} finally { historyLoading.value = false }
}

// --- Tab 2 ---
const panelBuilding = ref('')
const panelDateRange = ref(null)
const panelRows = ref([])
const panelTotal = ref(0)
const panelPage = ref(1)
const panelPageSize = 20
const panelLoading = ref(false)

async function loadPanelData(page = 1) {
  if (!panelBuilding.value) return
  panelLoading.value = true
  panelPage.value = page
  try {
    const start = panelDateRange.value?.[0] || ''
    const end = panelDateRange.value?.[1] || ''
    const res = await energyApi.getEnergyData(panelBuilding.value, {
      startDate: start, endDate: end, page: page - 1, size: panelPageSize
    })
    if (res.code === 200) {
      const data = res.data
      panelRows.value = Array.isArray(data) ? data : (data.content || data.list || [])
      panelTotal.value = data.totalElements || panelRows.value.length
    }
  } catch (e) {
    // handled by interceptor
  } finally {
    panelLoading.value = false
  }
}

async function exportExcel() {
  if (!panelBuilding.value) return
  try {
    const response = await reportApi.exportReport(panelBuilding.value)
    const url = window.URL.createObjectURL(new Blob([response.data]))
    const a = document.createElement('a')
    a.href = url
    a.download = `${panelBuilding.value}_energy_report.xlsx`
    a.click()
    window.URL.revokeObjectURL(url)
  } catch (e) {
    ElMessage.error('导出失败')
  }
}

// --- Tab 3 ---
const normBuilding = ref('')
const normDateRange = ref(null)
const normChartData = ref([])
const normLoading = ref(false)
const normChartRef = ref(null)
const normRadarRef = ref(null)
let normChart = null
let normRadar = null

const METRIC_META = [
  { key: 'powerConsumptionKwh', name: '电力(kWh)', icon: '⚡', color: '#00d4ff' },
  { key: 'waterConsumptionM3',  name: '水(m³)',    icon: '💧', color: '#36cfc9' },
  { key: 'carbonEmissionKg',    name: '碳排(kg)',  icon: '🌿', color: '#faad14' },
  { key: 'acConsumptionKwh',    name: '空调(kWh)', icon: '❄️', color: '#ff6b6b' }
]

async function loadNormData() {
  if (!normBuilding.value) { ElMessage.warning('请选择建筑'); return }
  normLoading.value = true
  normChartData.value = []
  try {
    const dateParams = {
      startDate: normDateRange.value?.[0] || '',
      endDate: normDateRange.value?.[1] || ''
    }
    const res = await energyApi.getEnergyData(normBuilding.value, dateParams)
    if (res.code !== 200) return
    const rows = Array.isArray(res.data) ? res.data : (res.data.content || res.data.list || [])
    if (!rows.length) { ElMessage.warning('该时间范围内无数据'); return }

    const avg = (arr, key) => arr.reduce((s, r) => s + (r[key] || 0), 0) / arr.length

    const metrics = METRIC_META.map(m => ({
      ...m, building: avg(rows, m.key), global: 0, diffPct: 0
    }))

    const allRes = await buildingApi.getAllBuildingIds()
    if (allRes.code === 200 && allRes.data?.length > 1) {
      const others = allRes.data.filter(id => id !== normBuilding.value)
      const allAvgs = METRIC_META.map(() => [])
      for (const id of others.slice(0, 6)) {
        const r = await energyApi.getEnergyData(id, dateParams)
        const rr = r.code === 200 ? (Array.isArray(r.data) ? r.data : r.data.content || []) : []
        if (rr.length) {
          METRIC_META.forEach((m, i) => allAvgs[i].push(avg(rr, m.key)))
        }
      }
      metrics.forEach((m, i) => {
        if (allAvgs[i].length) {
          m.global = allAvgs[i].reduce((a, b) => a + b, 0) / allAvgs[i].length
          m.diffPct = m.global > 0 ? ((m.building - m.global) / m.global) * 100 : 0
        }
      })
    }

    normChartData.value = metrics
    await nextTick()
    renderNormChart(metrics)
    renderNormRadar(metrics)
  } catch (e) {
    ElMessage.error('数据加载失败')
  } finally {
    normLoading.value = false
  }
}

function renderNormChart(metrics) {
  if (!normChartRef.value) return
  if (normChart) { normChart.dispose(); normChart = null }
  normChart = echarts.init(normChartRef.value)

  const barColors = ['#00d4ff', '#36cfc9', '#faad14', '#ff6b6b']
  const globalColors = ['#005f80', '#1a7a77', '#8a6a0a', '#803535']

  normChart.setOption({
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(13,27,46,0.95)', borderColor: '#1a3a5c',
      textStyle: { color: '#e0f0ff', fontSize: 12 },
      formatter(params) {
        let s = `<b>${params[0].name}</b><br/>`
        params.forEach(p => {
          s += `<span style="display:inline-block;width:10px;height:10px;border-radius:50%;background:${p.color};margin-right:6px"></span>${p.seriesName}: <b>${p.value}</b><br/>`
        })
        return s
      }
    },
    legend: {
      data: ['当前建筑', '全局均值'],
      textStyle: { color: '#8bafc9', fontSize: 12 }, top: 6, right: 20,
      icon: 'roundRect'
    },
    grid: { top: 50, bottom: 40, left: 65, right: 20 },
    xAxis: {
      type: 'category',
      data: metrics.map(m => m.name),
      axisLabel: { color: '#8bafc9', fontSize: 12 },
      axisLine: { lineStyle: { color: '#1a3a5c' } },
      axisTick: { show: false }
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: '#5a7a9a', fontSize: 11 },
      splitLine: { lineStyle: { color: 'rgba(26,58,92,0.5)', type: 'dashed' } },
      axisLine: { show: false }
    },
    series: [
      {
        name: '当前建筑', type: 'bar', barMaxWidth: 36, barGap: '30%',
        data: metrics.map((m, i) => ({
          value: +m.building.toFixed(2),
          itemStyle: {
            borderRadius: [6, 6, 0, 0],
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: barColors[i] },
              { offset: 1, color: barColors[i] + '44' }
            ])
          }
        })),
        label: { show: true, position: 'top', color: '#e0f0ff', fontSize: 11, formatter: '{c}' }
      },
      {
        name: '全局均值', type: 'bar', barMaxWidth: 36,
        data: metrics.map((m, i) => ({
          value: +m.global.toFixed(2),
          itemStyle: {
            borderRadius: [6, 6, 0, 0],
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: globalColors[i] },
              { offset: 1, color: globalColors[i] + '44' }
            ])
          }
        })),
        label: { show: true, position: 'top', color: '#8bafc9', fontSize: 11, formatter: '{c}' }
      }
    ]
  })
}

function renderNormRadar(metrics) {
  if (!normRadarRef.value) return
  if (normRadar) { normRadar.dispose(); normRadar = null }
  normRadar = echarts.init(normRadarRef.value)

  const maxVals = metrics.map(m => Math.max(m.building, m.global) * 1.3 || 100)

  normRadar.setOption({
    backgroundColor: 'transparent',
    tooltip: { backgroundColor: 'rgba(13,27,46,0.95)', borderColor: '#1a3a5c', textStyle: { color: '#e0f0ff' } },
    legend: {
      data: ['当前建筑', '全局均值'],
      textStyle: { color: '#8bafc9', fontSize: 12 }, bottom: 0,
      icon: 'roundRect'
    },
    radar: {
      indicator: metrics.map((m, i) => ({ name: m.name, max: +maxVals[i].toFixed(0) })),
      shape: 'polygon',
      splitNumber: 4,
      axisName: { color: '#8bafc9', fontSize: 11 },
      splitLine: { lineStyle: { color: 'rgba(26,58,92,0.6)' } },
      splitArea: { areaStyle: { color: ['rgba(0,212,255,0.02)', 'rgba(0,212,255,0.05)'] } },
      axisLine: { lineStyle: { color: 'rgba(26,58,92,0.6)' } }
    },
    series: [{
      type: 'radar',
      data: [
        {
          value: metrics.map(m => +m.building.toFixed(2)),
          name: '当前建筑',
          lineStyle: { color: '#00d4ff', width: 2 },
          areaStyle: { color: 'rgba(0,212,255,0.15)' },
          itemStyle: { color: '#00d4ff' }
        },
        {
          value: metrics.map(m => +m.global.toFixed(2)),
          name: '全局均值',
          lineStyle: { color: '#52c41a', width: 2, type: 'dashed' },
          areaStyle: { color: 'rgba(82,196,26,0.10)' },
          itemStyle: { color: '#52c41a' }
        }
      ]
    }]
  })
}

onMounted(async () => {
  try {
    await loadBuildingNames()
    const res = await buildingApi.getAllBuildingIds()
    if (res.code === 200) {
      buildingIds.value = res.data || []
      if (buildingIds.value.length) {
        panelBuilding.value = buildingIds.value[0]
        normBuilding.value = buildingIds.value[0]
      }
    }
  } catch (e) {}
  loadHistory()
})
</script>

<style scoped>
.page-container {
  padding: 0;
  color: #e0f0ff;
}

.data-tabs :deep(.el-tabs__header) {
  background: transparent;
  border-bottom: 1px solid #1a3a5c;
  margin-bottom: 0;
}

.data-tabs :deep(.el-tabs__item) {
  color: #8bafc9;
  font-size: 14px;
}

.data-tabs :deep(.el-tabs__item.is-active) {
  color: #00d4ff;
}

.data-tabs :deep(.el-tabs__active-bar) {
  background-color: #00d4ff;
}

.tab-content {
  padding: 20px 4px;
}

.upload-zone {
  width: 100%;
}

.upload-zone :deep(.el-upload-dragger) {
  background: #0f2540;
  border: 1px dashed rgba(0, 212, 255, 0.4);
  border-radius: 12px;
  padding: 32px;
}

.upload-zone :deep(.el-upload-dragger:hover) {
  border-color: #00d4ff;
}

.upload-icon {
  font-size: 48px;
  color: #00d4ff;
  margin-bottom: 8px;
}

.upload-text {
  color: #4a6460;
  font-size: 14px;
}

.upload-text em {
  color: #00d4ff;
  font-style: normal;
}

.upload-hint {
  color: #5a7a9a;
  font-size: 12px;
  margin-top: 6px;
}

.upload-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 16px;
  padding: 12px 16px;
  background: rgba(26, 188, 156, 0.06);
  border-radius: 8px;
  border: 1px solid rgba(26, 188, 156, 0.15);
}

.file-name {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #4a6460;
  font-size: 13px;
  flex: 1;
}

.preview-table {
  margin-top: 20px;
}

.section-title {
  font-size: 14px;
  color: #8bafc9;
  margin-bottom: 10px;
  padding-left: 4px;
  border-left: 3px solid #00d4ff;
  padding-left: 8px;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.dark-table :deep(.el-table),
.dark-table :deep(.el-table__inner-wrapper) {
  background: #0a1628 !important;
}

.dark-table :deep(.el-table th.el-table__cell) {
  background: #0f2540 !important;
  color: #7eaaca !important;
  border-bottom: 1px solid #1a3a5c !important;
}

.dark-table :deep(.el-table tr) {
  background: #0b1d33 !important;
}

.dark-table :deep(.el-table tr.dark-stripe) {
  background: #0e2440 !important;
}

.dark-table :deep(.el-table td.el-table__cell),
.dark-table :deep(.el-table tr td),
.dark-table :deep(.el-table .cell) {
  color: #d0e8ff !important;
  border-bottom: 1px solid #162d4a !important;
}

.dark-table :deep(.el-table tr.dark-stripe td.el-table__cell),
.dark-table :deep(.el-table tr.dark-stripe td),
.dark-table :deep(.el-table tr.dark-stripe .cell) {
  color: #d0e8ff !important;
}

.dark-table :deep(.el-table__body tr:hover > td) {
  background: #163050 !important;
  color: #ffffff !important;
}

.dark-table :deep(.el-table__empty-block) {
  background: #0a1628 !important;
}

.dark-table :deep(.el-table__empty-text) {
  color: #5a7a9a !important;
}

.norm-kpi-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin-top: 18px;
}

.norm-kpi-card {
  display: flex;
  align-items: center;
  gap: 12px;
  background: rgba(13,27,46,0.8);
  border: 1px solid #1a3a5c;
  border-radius: 10px;
  padding: 14px 16px;
  transition: border-color 0.2s;
}
.norm-kpi-card:hover { border-color: #00d4ff; }

.norm-kpi-icon {
  width: 42px; height: 42px;
  border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  font-size: 20px; flex-shrink: 0;
}

.norm-kpi-body { flex: 1; min-width: 0; }
.norm-kpi-label { font-size: 12px; color: #5a7a9a; margin-bottom: 2px; }
.norm-kpi-value { font-size: 22px; font-weight: 700; line-height: 1.2; }
.norm-kpi-diff { font-size: 11px; margin-top: 2px; }
.norm-kpi-diff.diff-up { color: #ff6b6b; }
.norm-kpi-diff.diff-down { color: #52c41a; }

.norm-charts-row {
  display: flex;
  gap: 16px;
  margin-top: 18px;
}

.norm-chart-main {
  flex: 3;
  height: 380px;
  background: rgba(13,27,46,0.5);
  border: 1px solid #1a3a5c;
  border-radius: 10px;
}

.norm-chart-radar {
  flex: 2;
  height: 380px;
  background: rgba(13,27,46,0.5);
  border: 1px solid #1a3a5c;
  border-radius: 10px;
}

.empty-state {
  margin-top: 60px;
}

.empty-state :deep(.el-empty__description) {
  color: #5a7a9a;
}

/* 企业引导横幅 */
.onboarding-banner {
  display: flex;
  gap: 20px;
  align-items: flex-start;
  background: linear-gradient(135deg, rgba(0,212,255,0.08), rgba(54,207,201,0.06));
  border: 1px solid rgba(0,212,255,0.25);
  border-radius: 14px;
  padding: 24px 28px;
  margin-bottom: 20px;
}
.onboarding-icon {
  font-size: 48px;
  flex-shrink: 0;
  line-height: 1;
}
.onboarding-body h2 {
  font-size: 20px;
  color: #00d4ff;
  margin: 0 0 6px;
  font-weight: 600;
}
.onboarding-body p {
  color: #8bafc9;
  font-size: 14px;
  margin: 0 0 14px;
}
.onboarding-steps {
  display: flex;
  gap: 24px;
}
.ob-step {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #e0f0ff;
  font-size: 13px;
}
.ob-num {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: rgba(0,212,255,0.2);
  color: #00d4ff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 12px;
  flex-shrink: 0;
}
</style>
