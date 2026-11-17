<template>
  <div class="mm-page">
    <!-- 顶部横幅 -->
    <div class="mm-banner">
      <div class="mm-banner-left">
        <div class="mm-title">多模态智能监控</div>
        <div class="mm-sub">人员感知 · 能耗交叉分析 · 空耗检测</div>
      </div>
      <div class="mm-banner-right">
        <el-select v-model="buildingId" size="small" style="width:140px" @change="loadAll">
          <el-option v-for="b in buildings" :key="b.buildingId" :label="b.buildingName" :value="b.buildingId" />
        </el-select>
        <el-button type="primary" size="small" @click="triggerAnalysis" :loading="analyzing">立即分析</el-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stat-cards">
      <div class="sc" v-for="s in statCards" :key="s.label">
        <div class="sc-icon" :style="{color: s.color, background: s.color + '15'}">{{ s.icon }}</div>
        <div class="sc-val" :style="{color: s.color}">{{ s.value }}</div>
        <div class="sc-label">{{ s.label }}</div>
      </div>
    </div>

    <!-- 主要内容 -->
    <div class="mm-grid">
      <!-- 左列: 区域状态 -->
      <div class="mm-card zone-card">
        <div class="card-title">区域占用状态</div>
        <div class="zone-list">
          <div class="zone-item" v-for="z in zones" :key="z.zoneId"
            :class="{occupied: z.isOccupied, empty: !z.isOccupied}"
            @click="selectZone(z)">
            <div class="zi-header">
              <span class="zi-dot" :style="{background: z.isOccupied ? '#52c41a' : '#ff4d4f'}"></span>
              <span class="zi-name">{{ z.zoneName }}</span>
            </div>
            <div class="zi-body">
              <span class="zi-count" :style="{color: z.isOccupied ? '#52c41a' : '#ff4d4f'}">
                {{ z.personCount }} 人
              </span>
              <span class="zi-cap">/ {{ z.capacity }}</span>
            </div>
            <div class="zi-bar">
              <div class="zi-bar-fill" :style="{
                width: Math.min(100, z.personCount / z.capacity * 100) + '%',
                background: z.personCount === 0 ? '#ff4d4f33' : z.personCount < z.capacity * 0.5 ? '#52c41a' : '#faad14'
              }"></div>
            </div>
          </div>
        </div>
      </div>

      <!-- 中列: 图表 -->
      <div class="mm-card chart-card">
        <div class="card-title">
          人数-能耗对比曲线
          <span class="card-title-sub" v-if="selectedZone">（{{ selectedZone.zoneName }}）</span>
        </div>
        <div ref="mainChartRef" style="height:320px"></div>
      </div>

      <!-- 右列: 告警流 -->
      <div class="mm-card alert-card">
        <div class="card-title">
          实时告警
          <el-badge :value="activeAlertCount" :hidden="activeAlertCount === 0" class="alert-badge" />
        </div>
        <div class="alert-list">
          <div class="alert-item" v-for="a in alerts.slice(0, 20)" :key="a.id"
            :class="'severity-' + a.severity">
            <div class="ai-header">
              <span class="ai-icon">{{ alertIcon(a.anomalyType) }}</span>
              <span class="ai-type">{{ a.anomalyType }}</span>
              <span class="ai-severity">{{ a.severity }}</span>
            </div>
            <div class="ai-evidence">{{ a.evidenceSummary }}</div>
            <div class="ai-footer">
              <span class="ai-time">{{ a.createdAt?.slice(11, 16) }}</span>
              <span class="ai-waste" v-if="a.estimatedWasteKwh > 0">
                浪费 {{ a.estimatedWasteKwh }}kWh ≈ ¥{{ a.estimatedCost }}
              </span>
              <el-button v-if="a.status === 'active'" size="small" type="success" text @click="resolveAlert(a.id)">
                已处理
              </el-button>
            </div>
          </div>
          <div v-if="alerts.length === 0" class="alert-empty">暂无告警</div>
        </div>
      </div>
    </div>

    <!-- 底部: 告警类型分布 + 节能效果 -->
    <div class="mm-bottom-grid">
      <div class="mm-card">
        <div class="card-title">告警类型分布</div>
        <div ref="pieChartRef" style="height:260px"></div>
      </div>
      <div class="mm-card">
        <div class="card-title">区域占用热力图</div>
        <div ref="heatChartRef" style="height:260px"></div>
      </div>
      <div class="mm-card saving-card">
        <div class="card-title">节能效果（本月）</div>
        <div class="saving-big">
          <div class="saving-num">{{ dashboard.monthWasteKwh || 0 }} <span>kWh</span></div>
          <div class="saving-label">检测到的浪费电量</div>
        </div>
        <div class="saving-big">
          <div class="saving-num cost">¥{{ dashboard.monthWasteCost || 0 }}</div>
          <div class="saving-label">估算浪费金额</div>
        </div>
        <div class="saving-tip">通过多模态检测，及时发现无人空耗</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { buildingApi } from '../api/index.js'
import * as echarts from 'echarts'
import request from '../api/index.js'

const buildingId = ref('')
const buildings = ref([])
const zones = ref([])
const alerts = ref([])
const dashboard = ref({})
const selectedZone = ref(null)
const analyzing = ref(false)
const mainChartRef = ref(null)
const pieChartRef = ref(null)
const heatChartRef = ref(null)

const activeAlertCount = computed(() => alerts.value.filter(a => a.status === 'active').length)

const statCards = computed(() => [
  { icon: '👥', label: '当前总人数', value: dashboard.value.totalPeople || 0, color: '#00d4ff' },
  { icon: '🚨', label: '活跃告警', value: dashboard.value.activeAlerts || 0, color: '#ff4d4f' },
  { icon: '📋', label: '今日告警', value: dashboard.value.todayAlertCount || 0, color: '#faad14' },
  { icon: '⚡', label: '本月浪费(kWh)', value: dashboard.value.monthWasteKwh || 0, color: '#8b5cf6' },
  { icon: '💰', label: '浪费金额(元)', value: '¥' + (dashboard.value.monthWasteCost || 0), color: '#ff6b6b' },
])

function alertIcon(type) {
  const map = { '无人照明': '💡', '无人空调': '❄️', '无人设备运行': '🔌', '夜间异常人员': '🌙', '人少能耗高': '📊' }
  return map[type] || '⚠️'
}

async function loadBuildings() {
  try {
    const res = await buildingApi.getAllBuildings()
    buildings.value = res.code === 200 ? res.data : []
    if (buildings.value.length && !buildingId.value) {
      buildingId.value = buildings.value[0].buildingId
    }
  } catch (e) {}
}

async function loadAll() {
  selectedZone.value = null
  zones.value = []
  alerts.value = []
  dashboard.value = {}
  // 销毁旧图表
  ;[mainChartRef, pieChartRef, heatChartRef].forEach(r => {
    if (r.value) { const c = echarts.getInstanceByDom(r.value); if (c) c.dispose() }
  })
  try {
    const res = await request.get(`/multimodal/dashboard/${buildingId.value}`)
    if (res.code === 200) {
      dashboard.value = res.data
      zones.value = res.data.zones || []
      alerts.value = res.data.todayAlerts || []
      await nextTick()
      renderCharts()
    }
  } catch (e) { console.error(e) }
}

async function triggerAnalysis() {
  analyzing.value = true
  try {
    await request.post(`/multimodal/analyze/${buildingId.value}`)
    ElMessage.success('分析完成')
    loadAll()
  } catch (e) {} finally { analyzing.value = false }
}

async function resolveAlert(id) {
  try {
    await request.post(`/multimodal/alerts/${id}/resolve`)
    loadAll()
  } catch (e) {}
}

async function selectZone(zone) {
  selectedZone.value = zone
  try {
    const res = await request.get(`/multimodal/occupancy-history/${zone.zoneId}`, { params: { hours: 24 } })
    if (res.code === 200) {
      renderMainChart(res.data, zone.zoneName)
    }
  } catch (e) {}
}

function renderCharts() {
  renderPieChart()
  renderHeatChart()
  if (zones.value.length > 0) {
    selectZone(zones.value[0])
  }
}

function renderMainChart(historyData, zoneName) {
  if (!mainChartRef.value) return
  const old = echarts.getInstanceByDom(mainChartRef.value)
  if (old) old.dispose()
  const chart = echarts.init(mainChartRef.value)

  // 采样：保留~48个点（每30分钟一个点），避免X轴拥挤
  let sampled = historyData
  if (historyData.length > 48) {
    const step = Math.ceil(historyData.length / 48)
    sampled = historyData.filter((_, i) => i % step === 0)
  }
  const times = sampled.map(d => {
    const t = d.time || ''
    // 格式: "2026-04-07T07:06:48" → "07:06"
    return t.slice(11, 16)
  })
  const counts = sampled.map(d => d.personCount)
  // 每隔若干个标签显示一次，保证X轴最多显示8~12个标签
  const labelInterval = Math.max(0, Math.floor(times.length / 10) - 1)

  chart.setOption({
    tooltip: { trigger: 'axis', formatter: (p) => {
      const idx = p[0]?.dataIndex ?? 0
      const fullTime = sampled[idx]?.time || ''
      const display = fullTime.slice(5, 16).replace('T', ' ')
      return display + '<br/>人数: ' + p[0]?.data
    }},
    legend: { data: ['人数'], textStyle: { color: '#8bafc9' }, top: 5 },
    grid: { left: 50, right: 20, top: 40, bottom: 65 },
    xAxis: { type: 'category', data: times, boundaryGap: false,
      axisLabel: { color: '#5a7a9a', fontSize: 10, rotate: 45, interval: labelInterval, margin: 10 },
      axisLine: { lineStyle: { color: '#1a3a5c' } } },
    yAxis: { type: 'value', name: '人数', nameTextStyle: { color: '#5a7a9a' },
      axisLabel: { color: '#5a7a9a' }, splitLine: { lineStyle: { color: '#1a3a5c33' } } },
    series: [{
      name: '人数', type: 'line', data: counts, smooth: true, showSymbol: false,
      areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
        { offset: 0, color: 'rgba(0,212,255,0.3)' }, { offset: 1, color: 'rgba(0,212,255,0)' }
      ])},
      lineStyle: { color: '#00d4ff', width: 2 },
      itemStyle: { color: '#00d4ff' }
    }]
  })
}

function renderPieChart() {
  if (!pieChartRef.value) return
  const chart = echarts.init(pieChartRef.value)
  const dist = dashboard.value.alertTypeDistribution || {}
  const colors = { '无人照明': '#faad14', '无人空调': '#00d4ff', '无人设备运行': '#8b5cf6', '夜间异常人员': '#1890ff', '人少能耗高': '#ff6b6b' }
  chart.setOption({
    tooltip: { trigger: 'item' },
    color: Object.keys(dist).map(k => colors[k] || '#52c41a'),
    series: [{
      type: 'pie', radius: ['40%', '70%'],
      label: { color: '#8bafc9', fontSize: 11 },
      data: Object.entries(dist).map(([name, value]) => ({ name, value })),
      itemStyle: { borderColor: '#0d1b2e', borderWidth: 2 },
      emphasis: { label: { fontWeight: 'bold' } }
    }]
  })
}

function renderHeatChart() {
  if (!heatChartRef.value) return
  const chart = echarts.init(heatChartRef.value)
  const zoneNames = zones.value.map(z => z.zoneName.replace(/.*?-/, ''))
  const data = zones.value.map((z, i) => {
    const ratio = z.capacity > 0 ? z.personCount / z.capacity : 0
    return [0, i, Math.round(ratio * 100)]
  })
  chart.setOption({
    tooltip: { formatter: p => `${zoneNames[p.data[1]]}: ${p.data[2]}% 占用` },
    grid: { left: 100, right: 30, top: 10, bottom: 30 },
    xAxis: { type: 'category', data: ['占用率'], axisLabel: { color: '#5a7a9a' }, axisLine: { lineStyle: { color: '#1a3a5c' } } },
    yAxis: { type: 'category', data: zoneNames, axisLabel: { color: '#8bafc9', fontSize: 10 }, axisLine: { lineStyle: { color: '#1a3a5c' } } },
    visualMap: { min: 0, max: 100, show: true, orient: 'horizontal', left: 'center', bottom: 0,
      textStyle: { color: '#5a7a9a' }, inRange: { color: ['#0d1b2e', '#1a3a5c', '#00d4ff', '#52c41a', '#faad14', '#ff4d4f'] } },
    series: [{ type: 'heatmap', data, label: { show: true, color: '#e0f0ff', fontSize: 10, formatter: p => p.data[2] + '%' },
      itemStyle: { borderColor: '#0d1b2e', borderWidth: 2, borderRadius: 4 } }]
  })
}

onMounted(async () => {
  await loadBuildings()
  loadAll()
})
</script>

<style scoped>
.mm-page { display: flex; flex-direction: column; gap: 14px; color: #e0f0ff; }
.mm-banner {
  background: linear-gradient(135deg, #0f2540, #0d1b2e); border: 1px solid #1a3a5c; border-radius: 14px;
  padding: 16px 24px; display: flex; align-items: center; justify-content: space-between;
}
.mm-title { font-size: 22px; font-weight: 700; }
.mm-sub { font-size: 13px; color: #5a7a9a; margin-top: 4px; }
.mm-banner-right { display: flex; gap: 10px; align-items: center; }

.stat-cards { display: flex; gap: 12px; }
.sc {
  flex: 1; background: #0d1b2e; border: 1px solid #1a3a5c; border-radius: 12px;
  padding: 14px; text-align: center;
}
.sc-icon { width: 36px; height: 36px; border-radius: 8px; display: inline-flex; align-items: center; justify-content: center; font-size: 16px; margin-bottom: 6px; }
.sc-val { font-size: 22px; font-weight: 700; }
.sc-label { font-size: 11px; color: #5a7a9a; margin-top: 2px; }

.mm-grid { display: grid; grid-template-columns: 280px 1fr 300px; gap: 14px; }
.mm-card { background: #0d1b2e; border: 1px solid #1a3a5c; border-radius: 12px; padding: 16px; }
.card-title { font-size: 14px; font-weight: 600; color: #8bafc9; margin-bottom: 12px; display: flex; align-items: center; gap: 8px; }
.card-title-sub { font-size: 12px; color: #5a7a9a; }

.zone-list { display: flex; flex-direction: column; gap: 8px; max-height: 380px; overflow-y: auto; }
.zone-item {
  background: #0a1628; border: 1px solid #1a3a5c; border-radius: 8px; padding: 10px 12px; cursor: pointer;
  transition: all 0.2s;
}
.zone-item:hover { border-color: #00d4ff; }
.zone-item.empty { border-left: 3px solid #ff4d4f; }
.zone-item.occupied { border-left: 3px solid #52c41a; }
.zi-header { display: flex; align-items: center; gap: 6px; margin-bottom: 4px; }
.zi-dot { width: 8px; height: 8px; border-radius: 50%; }
.zi-name { font-size: 12px; color: #8bafc9; }
.zi-body { display: flex; align-items: baseline; gap: 4px; }
.zi-count { font-size: 18px; font-weight: 700; }
.zi-cap { font-size: 11px; color: #5a7a9a; }
.zi-bar { height: 3px; background: #1a3a5c; border-radius: 2px; margin-top: 6px; }
.zi-bar-fill { height: 100%; border-radius: 2px; transition: width 0.5s; }

.alert-list { max-height: 380px; overflow-y: auto; display: flex; flex-direction: column; gap: 8px; }
.alert-item { background: #0a1628; border-radius: 8px; padding: 10px 12px; border-left: 3px solid #5a7a9a; }
.alert-item.severity-severe { border-left-color: #ff4d4f; }
.alert-item.severity-warning { border-left-color: #faad14; }
.alert-item.severity-info { border-left-color: #1890ff; }
.ai-header { display: flex; align-items: center; gap: 6px; margin-bottom: 4px; }
.ai-icon { font-size: 14px; }
.ai-type { font-size: 13px; font-weight: 600; color: #e0f0ff; }
.ai-severity { font-size: 10px; color: #5a7a9a; margin-left: auto; text-transform: uppercase; }
.ai-evidence { font-size: 11px; color: #8bafc9; line-height: 1.5; margin-bottom: 4px; }
.ai-footer { display: flex; align-items: center; gap: 8px; font-size: 10px; }
.ai-time { color: #5a7a9a; }
.ai-waste { color: #ff6b6b; font-weight: 600; }
.alert-empty { text-align: center; color: #5a7a9a; padding: 40px 0; font-size: 13px; }

.mm-bottom-grid { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 14px; }
.saving-card { text-align: center; }
.saving-big { margin: 16px 0; }
.saving-num { font-size: 32px; font-weight: 700; color: #faad14; }
.saving-num span { font-size: 14px; font-weight: 400; }
.saving-num.cost { color: #ff6b6b; }
.saving-label { font-size: 12px; color: #5a7a9a; margin-top: 4px; }
.saving-tip { font-size: 11px; color: #5a7a9a; background: #0a1628; border-radius: 8px; padding: 8px; margin-top: 12px; }

.alert-badge { margin-left: 4px; }
</style>
