<template>
  <div class="bd-page" :style="{ '--accent': bld?.color || '#00d4ff' }">

    <!-- 顶部导航 -->
    <div class="bd-nav">
      <button class="bd-back" @click="router.push('/home')">‹ 返回园区地图</button>
      <div class="bd-nav-logo">⚡ 鼎筑 · 建筑能源智能管理</div>
      <div class="bd-nav-actions">
        <button class="bd-btn-ghost" @click="router.push('/energy-analysis?buildingId=' + buildingId)">完整能耗分析 →</button>
        <button class="bd-btn-ghost" @click="router.push('/ai-analysis')">AI 诊断</button>
      </div>
    </div>

    <!-- Hero 区域 -->
    <div class="bd-hero">
      <div class="bd-hero-glow"></div>
      <div class="bd-hero-main">
        <div class="bd-hero-icon">{{ bld?.bgIcon }}</div>
        <div class="bd-hero-text">
          <div class="bd-tag-row">
            <el-tag :style="{background:bld?.color+'22',borderColor:bld?.color+'55',color:bld?.color}" effect="dark">
              {{ bld?.tag }}
            </el-tag>
            <span class="bd-id-badge">{{ buildingId }}</span>
          </div>
          <h1 class="bd-title">{{ bld?.name }}</h1>
          <p class="bd-desc">{{ bld?.desc }}</p>
          <div class="bd-chips">
            <span class="bd-chip">🏗 {{ bld?.area }}</span>
            <span class="bd-chip">🏢 {{ bld?.floors }}</span>
            <span class="bd-chip">📡 {{ bld?.devices }}</span>
          </div>
        </div>
      </div>

      <!-- KPI 指标 -->
      <div class="bd-kpis">
        <div class="bd-kpi" v-for="k in kpis" :key="k.label">
          <div class="bd-kpi-val" :style="{color: bld?.color}">{{ k.value }}</div>
          <div class="bd-kpi-unit">{{ k.unit }}</div>
          <div class="bd-kpi-lbl">{{ k.label }}</div>
        </div>
      </div>
    </div>

    <!-- 图表区 -->
    <div class="bd-charts">
      <div class="bd-card bd-card-lg">
        <div class="bd-card-hdr">
          <span class="bd-card-title">电力消耗趋势</span>
          <el-tag size="small" type="info" effect="plain">最近数据</el-tag>
        </div>
        <div v-if="loading" class="bd-spin">加载中...</div>
        <v-chart v-else :option="powerOpt" style="height:230px" autoresize />
      </div>
      <div class="bd-card">
        <div class="bd-card-hdr">
          <span class="bd-card-title">能耗构成</span>
        </div>
        <div v-if="loading" class="bd-spin">加载中...</div>
        <v-chart v-else :option="pieOpt" style="height:230px" autoresize />
      </div>
      <div class="bd-card">
        <div class="bd-card-hdr">
          <span class="bd-card-title">月度电力对比</span>
        </div>
        <div v-if="monthLoading" class="bd-spin">加载中...</div>
        <v-chart v-else :option="monthlyOpt" style="height:230px" autoresize />
      </div>
    </div>

    <!-- 跳转操作区 -->
    <div class="bd-action-section">
      <div class="bd-action-title">深入分析模块</div>
      <div class="bd-actions">
        <div class="bd-action-card" @click="router.push('/energy-analysis?buildingId=' + buildingId)">
          <div class="bd-ac-icon" style="color:#00d4ff">📊</div>
          <div class="bd-ac-name">能耗分析</div>
          <div class="bd-ac-desc">趋势图表 · 异常检测 · 月度报告</div>
        </div>
        <div class="bd-action-card" @click="router.push('/device-status')">
          <div class="bd-ac-icon" style="color:#52c41a">🖥</div>
          <div class="bd-ac-name">设备状态</div>
          <div class="bd-ac-desc">设备监测 · 故障告警 · 运行状态</div>
        </div>
        <div class="bd-action-card" @click="router.push('/ai-analysis')">
          <div class="bd-ac-icon" style="color:#9254de">🤖</div>
          <div class="bd-ac-name">AI 智能分析</div>
          <div class="bd-ac-desc">智能诊断 · 节能建议 · 碳排预测</div>
        </div>
        <div class="bd-action-card" @click="router.push('/dashboard')">
          <div class="bd-ac-icon" style="color:#fadb14">🗂</div>
          <div class="bd-ac-name">系统总览</div>
          <div class="bd-ac-desc">全园区数据 · 排名 · 告警汇总</div>
        </div>
      </div>
    </div>

    <!-- 页脚 -->
    <div class="bd-footer">
      <span>鼎筑建筑能源智能管理系统 v1.0.0</span>
      <span class="bd-dot">·</span>
      <span>Vue 3 + Spring Boot + MySQL</span>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { LineChart, PieChart, BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { energyApi } from '../api/index.js'
import { buildingMap } from '../utils/buildingsConfig.js'

use([LineChart, PieChart, BarChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer])

const route = useRoute()
const router = useRouter()
const buildingId = route.params.id
const bld = buildingMap[buildingId]

const loading = ref(false)
const monthLoading = ref(false)
const latestData = ref([])
const monthlyData = ref({ months: [], powerConsumption: [] })

const kpis = computed(() => {
  const d = latestData.value
  if (!d.length) return [
    { label: '最新电耗', value: '--', unit: 'kWh' },
    { label: '碳排放', value: '--', unit: 'kg' },
    { label: '水消耗', value: '--', unit: 'm³' },
    { label: '空调占比', value: '--', unit: '%' },
  ]
  const totalPower = d.reduce((s, x) => s + (Number(x.powerConsumptionKwh) || 0), 0)
  const totalCarbon = d.reduce((s, x) => s + (Number(x.carbonEmissionKg) || 0), 0)
  const totalWater = d.reduce((s, x) => s + (Number(x.waterConsumptionM3) || 0), 0)
  const totalAc = d.reduce((s, x) => s + (Number(x.acConsumptionKwh) || 0), 0)
  return [
    { label: '近期总电耗', value: totalPower.toFixed(0), unit: 'kWh' },
    { label: '碳排放', value: totalCarbon.toFixed(0), unit: 'kg' },
    { label: '水消耗', value: totalWater.toFixed(1), unit: 'm³' },
    { label: '空调占比', value: totalPower > 0 ? (totalAc / totalPower * 100).toFixed(1) : '--', unit: '%' },
  ]
})

const powerOpt = computed(() => {
  const d = latestData.value.slice(-48)
  const c = bld?.color || '#00d4ff'
  return {
    backgroundColor: 'transparent',
    tooltip: { trigger: 'axis', backgroundColor: '#0d1b2e', borderColor: '#1a3a5c', textStyle: { color: '#e0f0ff' } },
    grid: { top: 20, bottom: 40, left: 60, right: 20 },
    xAxis: {
      type: 'category',
      data: d.map(x => (x.timestamp || '').slice(5, 16)),
      axisLabel: { color: '#8bafc9', rotate: 30, fontSize: 9 },
      axisLine: { lineStyle: { color: '#1a3a5c' } }
    },
    yAxis: { type: 'value', axisLabel: { color: '#8bafc9' }, splitLine: { lineStyle: { color: '#1a3a5c' } } },
    series: [{
      type: 'line', data: d.map(x => Number(x.powerConsumptionKwh || 0).toFixed(2)),
      smooth: true, symbol: 'none', itemStyle: { color: c },
      areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: c + '44' }, { offset: 1, color: c + '00' }] } }
    }]
  }
})

const pieOpt = computed(() => {
  const d = latestData.value
  const ac = d.reduce((s, x) => s + (Number(x.acConsumptionKwh) || 0), 0)
  const light = d.reduce((s, x) => s + (Number(x.lightingConsumptionKwh) || 0), 0)
  const other = d.reduce((s, x) => s + (Number(x.otherConsumptionKwh) || 0), 0)
  const c = bld?.color || '#00d4ff'
  return {
    backgroundColor: 'transparent',
    tooltip: { trigger: 'item', backgroundColor: '#0d1b2e', borderColor: '#1a3a5c', textStyle: { color: '#e0f0ff' } },
    legend: { orient: 'vertical', right: 5, top: 'center', textStyle: { color: '#8bafc9', fontSize: 11 } },
    series: [{
      type: 'pie', radius: ['40%', '66%'], center: ['38%', '52%'],
      data: [
        { value: ac.toFixed(1), name: '空调', itemStyle: { color: c } },
        { value: light.toFixed(1), name: '照明', itemStyle: { color: '#52c41a' } },
        { value: other.toFixed(1), name: '其他', itemStyle: { color: '#a8d8d0' } },
      ].filter(x => Number(x.value) > 0),
      label: { show: false }
    }]
  }
})

const monthlyOpt = computed(() => {
  const m = monthlyData.value
  const c = bld?.color || '#00d4ff'
  return {
    backgroundColor: 'transparent',
    tooltip: { trigger: 'axis', backgroundColor: '#0d1b2e', borderColor: '#1a3a5c', textStyle: { color: '#e0f0ff' } },
    grid: { top: 20, bottom: 40, left: 60, right: 20 },
    xAxis: { type: 'category', data: m.months || [], axisLabel: { color: '#8bafc9', fontSize: 10 }, axisLine: { lineStyle: { color: '#1a3a5c' } } },
    yAxis: { type: 'value', axisLabel: { color: '#8bafc9' }, splitLine: { lineStyle: { color: '#1a3a5c' } } },
    series: [{
      type: 'bar', data: m.powerConsumption || [],
      itemStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: c }, { offset: 1, color: c + '55' }] }, borderRadius: [4, 4, 0, 0] },
      barMaxWidth: 36
    }]
  }
})

onMounted(async () => {
  loading.value = true
  monthLoading.value = true
  try {
    const res = await energyApi.getLatestEnergyData(buildingId, 72)
    if (res.code === 200) latestData.value = res.data || []
  } catch { /* use empty */ }
  finally { loading.value = false }

  try {
    const res2 = await energyApi.getMonthlyTrend(buildingId)
    if (res2.code === 200) monthlyData.value = res2.data || {}
  } catch { /* use empty */ }
  finally { monthLoading.value = false }
})
</script>

<style scoped>
.bd-page {
  min-height: 100vh;
  background: #0a1628;
  color: #e0f0ff;
  display: flex;
  flex-direction: column;
  font-family: 'Microsoft YaHei', sans-serif;
}

/* ── 顶部导航 ── */
.bd-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 28px;
  height: 56px;
  background: #0d1b2e;
  border-bottom: 1px solid #1a3a5c;
  flex-shrink: 0;
}
.bd-back {
  background: none;
  border: 1px solid #1a3a5c;
  color: #8bafc9;
  padding: 6px 16px;
  border-radius: 20px;
  cursor: pointer;
  font-size: 13px;
  transition: all 0.2s;
}
.bd-back:hover { border-color: var(--accent); color: var(--accent); }
.bd-nav-logo { font-size: 14px; font-weight: 600; color: #00d4ff; letter-spacing: 1px; }
.bd-nav-actions { display: flex; gap: 10px; }
.bd-btn-ghost {
  background: none;
  border: 1px solid #1a3a5c;
  color: #8bafc9;
  padding: 6px 14px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 12px;
  transition: all 0.2s;
}
.bd-btn-ghost:hover { border-color: var(--accent); color: var(--accent); }

/* ── Hero ── */
.bd-hero {
  position: relative;
  padding: 36px 28px 28px;
  border-bottom: 1px solid #1a3a5c;
  overflow: hidden;
}
.bd-hero-glow {
  position: absolute;
  inset: 0;
  background: radial-gradient(ellipse at 20% 50%, color-mix(in srgb, var(--accent) 12%, transparent) 0%, transparent 65%);
  pointer-events: none;
}
.bd-hero-main { display: flex; align-items: flex-start; gap: 24px; margin-bottom: 28px; position: relative; }
.bd-hero-icon { font-size: 56px; line-height: 1; }
.bd-hero-text { flex: 1; }
.bd-tag-row { display: flex; align-items: center; gap: 10px; margin-bottom: 10px; }
.bd-id-badge {
  font-size: 11px;
  color: #5a7a9a;
  background: #0f2540;
  border: 1px solid #1a3a5c;
  padding: 2px 8px;
  border-radius: 10px;
}
.bd-title { font-size: 32px; font-weight: 700; color: #e0f0ff; margin: 0 0 10px; }
.bd-desc { font-size: 13px; color: #8bafc9; line-height: 1.7; margin: 0 0 14px; max-width: 560px; }
.bd-chips { display: flex; gap: 10px; flex-wrap: wrap; }
.bd-chip {
  font-size: 12px;
  color: #8bafc9;
  background: #0f2540;
  border: 1px solid #1a3a5c;
  padding: 4px 12px;
  border-radius: 12px;
}
.bd-kpis {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  position: relative;
}
.bd-kpi {
  background: #0d1b2e;
  border: 1px solid #1a3a5c;
  border-radius: 12px;
  padding: 16px 20px;
  text-align: center;
  box-shadow: 0 2px 8px rgba(0,0,0,0.2);
}
.bd-kpi-val { font-size: 26px; font-weight: 700; line-height: 1.1; }
.bd-kpi-unit { font-size: 11px; color: #5a7a9a; margin-top: 2px; }
.bd-kpi-lbl { font-size: 12px; color: #8bafc9; margin-top: 6px; }

/* ── Charts ── */
.bd-charts {
  display: grid;
  grid-template-columns: 2fr 1fr 1fr;
  gap: 16px;
  padding: 20px 28px;
}
.bd-card {
  background: #0d1b2e;
  border: 1px solid #1a3a5c;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.2);
}
.bd-card-lg { grid-column: span 1; }
.bd-card-hdr { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.bd-card-title { font-size: 13px; font-weight: 600; color: #e0f0ff; }
.bd-spin { height: 230px; display: flex; align-items: center; justify-content: center; color: #5a7a9a; font-size: 13px; }

/* ── Actions ── */
.bd-action-section { padding: 0 28px 20px; }
.bd-action-title { font-size: 13px; color: #5a7a9a; margin-bottom: 14px; text-transform: uppercase; letter-spacing: 1px; }
.bd-actions { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; }
.bd-action-card {
  background: #0d1b2e;
  border: 1px solid #1a3a5c;
  border-radius: 12px;
  padding: 20px 16px;
  cursor: pointer;
  transition: all 0.2s;
  text-align: center;
}
.bd-action-card:hover { border-color: var(--accent); transform: translateY(-2px); background: rgba(0,212,255,0.08); box-shadow: 0 4px 16px rgba(0,212,255,0.15); }
.bd-ac-icon { font-size: 28px; margin-bottom: 8px; }
.bd-ac-name { font-size: 14px; font-weight: 600; color: #e0f0ff; margin-bottom: 6px; }
.bd-ac-desc { font-size: 11px; color: #5a7a9a; line-height: 1.5; }

/* ── Footer ── */
.bd-footer {
  margin-top: auto;
  padding: 16px 28px;
  border-top: 1px solid #1a3a5c;
  font-size: 12px;
  color: #5a7a9a;
  display: flex;
  gap: 8px;
  align-items: center;
}
.bd-dot { color: #1a3a5c; }
</style>
