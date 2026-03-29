<template>
  <div class="device-status">
    <div class="filter-bar">
      <el-select v-model="selectedBuilding" placeholder="选择建筑" style="width:160px" @change="loadDeviceData">
        <el-option v-for="id in buildingIds" :key="id" :label="id" :value="id" />
      </el-select>
      <el-radio-group v-model="statusFilter" size="default">
        <el-radio-button value="all">全部</el-radio-button>
        <el-radio-button value="normal">正常</el-radio-button>
        <el-radio-button value="warning">告警</el-radio-button>
        <el-radio-button value="error">故障</el-radio-button>
      </el-radio-group>
      <el-button type="primary" icon="Refresh" @click="loadDeviceData" :loading="loading">刷新</el-button>
    </div>

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

    <div class="content-grid">
      <div class="device-list-card">
        <div class="card-header">
          <span class="card-title">空调系统状态</span>
          <el-tag type="info" size="small">共 {{ filteredDevices.length }} 台</el-tag>
        </div>
        <div class="device-grid">
          <div class="device-item" v-for="device in filteredDevices" :key="device.id"
               :class="'status-' + device.status" @click="selectDevice(device)">
            <div class="device-icon">
              <el-icon :size="20"><WindPower /></el-icon>
            </div>
            <div class="device-name">{{ device.name }}</div>
            <div class="device-floor">{{ device.floor }}</div>
            <div class="device-power">{{ device.power }} kW</div>
            <div class="device-status-badge" :class="'badge-' + device.status">
              {{ statusLabel(device.status) }}
            </div>
          </div>
        </div>
      </div>

      <div class="right-panel">
        <div class="chart-card">
          <div class="card-header"><span class="card-title">设备运行功率</span></div>
          <v-chart :option="devicePowerOption" style="height:240px" autoresize />
        </div>

        <div class="detail-card" v-if="selectedDevice">
          <div class="card-header">
            <span class="card-title">设备详情 - {{ selectedDevice.name }}</span>
            <el-tag :type="selectedDevice.status === 'normal' ? 'success' : selectedDevice.status === 'warning' ? 'warning' : 'danger'" size="small">
              {{ statusLabel(selectedDevice.status) }}
            </el-tag>
          </div>
          <div class="detail-grid">
            <div class="detail-item">
              <span class="detail-label">当前功率</span>
              <span class="detail-value" style="color:#00d4ff">{{ selectedDevice.power }} kW</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">设定温度</span>
              <span class="detail-value">{{ selectedDevice.setTemp }}°C</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">室内温度</span>
              <span class="detail-value">{{ selectedDevice.roomTemp }}°C</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">运行时长</span>
              <span class="detail-value">{{ selectedDevice.runtime }}h</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">今日能耗</span>
              <span class="detail-value" style="color:#52c41a">{{ selectedDevice.todayEnergy }} kWh</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">能效比(COP)</span>
              <span class="detail-value" :style="{ color: selectedDevice.cop >= 3 ? '#52c41a' : '#ff8c00' }">{{ selectedDevice.cop }}</span>
            </div>
          </div>
          <div class="device-actions">
            <el-button size="small" type="success" plain>启动</el-button>
            <el-button size="small" type="warning" plain>调温</el-button>
            <el-button size="small" type="danger" plain>关机</el-button>
            <el-button size="small" type="info" plain>维护</el-button>
          </div>
        </div>

        <div class="chart-card">
          <div class="card-header"><span class="card-title">今日能耗分时段</span></div>
          <v-chart :option="hourlyOption" style="height:200px" autoresize />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { BarChart, LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { buildingApi } from '../api/index.js'

use([BarChart, LineChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer])

const buildingIds = ref([])
const selectedBuilding = ref('B001')
const statusFilter = ref('all')
const loading = ref(false)
const selectedDevice = ref(null)

const allDevices = ref([
  { id: 1, name: '中央空调-1F', floor: '1楼', power: 45.2, status: 'normal', setTemp: 26, roomTemp: 25.8, runtime: 8.5, todayEnergy: 384.2, cop: 3.2 },
  { id: 2, name: '中央空调-2F', floor: '2楼', power: 38.6, status: 'normal', setTemp: 26, roomTemp: 26.2, runtime: 7.8, todayEnergy: 301.1, cop: 3.1 },
  { id: 3, name: '中央空调-3F', floor: '3楼', power: 52.1, status: 'warning', setTemp: 24, roomTemp: 27.5, runtime: 9.2, todayEnergy: 479.3, cop: 2.4 },
  { id: 4, name: '分体空调-4FA', floor: '4楼A区', power: 12.3, status: 'normal', setTemp: 25, roomTemp: 25.1, runtime: 6.0, todayEnergy: 73.8, cop: 3.5 },
  { id: 5, name: '分体空调-4FB', floor: '4楼B区', power: 0, status: 'error', setTemp: 26, roomTemp: 30.2, runtime: 0, todayEnergy: 0, cop: 0 },
  { id: 6, name: '中央空调-5F', floor: '5楼', power: 41.8, status: 'normal', setTemp: 26, roomTemp: 26.0, runtime: 8.0, todayEnergy: 334.4, cop: 3.3 },
  { id: 7, name: '新风机组-1F', floor: '1楼', power: 8.5, status: 'normal', setTemp: null, roomTemp: null, runtime: 9.0, todayEnergy: 76.5, cop: null },
  { id: 8, name: '新风机组-3F', floor: '3楼', power: 9.2, status: 'warning', setTemp: null, roomTemp: null, runtime: 9.0, todayEnergy: 82.8, cop: null }
])

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

const hourlyOption = computed(() => {
  const hours = Array.from({ length: 24 }, (_, i) => `${i}:00`)
  const values = hours.map((_, i) => {
    if (i < 6) return (Math.random() * 20 + 10).toFixed(1)
    if (i < 9) return (Math.random() * 40 + 60).toFixed(1)
    if (i < 18) return (Math.random() * 30 + 120).toFixed(1)
    if (i < 22) return (Math.random() * 30 + 80).toFixed(1)
    return (Math.random() * 20 + 20).toFixed(1)
  })
  return {
    backgroundColor: 'transparent',
    tooltip: { trigger: 'axis', backgroundColor: '#0d1b2e', borderColor: '#1a3a5c', textStyle: { color: '#e0f0ff' } },
    grid: { top: 10, bottom: 30, left: 45, right: 10 },
    xAxis: { type: 'category', data: hours, axisLabel: { color: '#8bafc9', fontSize: 9, interval: 3 }, axisLine: { lineStyle: { color: '#1a3a5c' } } },
    yAxis: { type: 'value', axisLabel: { color: '#8bafc9' }, splitLine: { lineStyle: { color: '#1a3a5c' } } },
    series: [{
      type: 'bar', data: values, barMaxWidth: 10,
      itemStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: '#00d4ff' }, { offset: 1, color: '#0066cc' }] }, borderRadius: [2, 2, 0, 0] }
    }]
  }
})

function statusLabel(s) {
  return { normal: '正常', warning: '告警', error: '故障' }[s] || s
}

function selectDevice(device) {
  selectedDevice.value = device
}

async function loadDeviceData() {
  loading.value = true
  setTimeout(() => { loading.value = false }, 500)
}

onMounted(async () => {
  try {
    const res = await buildingApi.getAllBuildingIds()
    if (res.code === 200) buildingIds.value = res.data
  } catch {
    buildingIds.value = ['B001', 'B002', 'B003', 'B004']
  }
  selectedDevice.value = allDevices.value[0]
})
</script>

<style scoped>
.device-status { display: flex; flex-direction: column; gap: 16px; }

.filter-bar {
  background: #0d1b2e;
  border: 1px solid #1a3a5c;
  border-radius: 12px;
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.device-overview {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 12px;
}

.overview-card {
  background: #0d1b2e;
  border: 1px solid #1a3a5c;
  border-radius: 12px;
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.ov-icon { flex-shrink: 0; }
.ov-value { font-size: 20px; font-weight: 700; }
.ov-label { font-size: 12px; color: #8bafc9; margin-top: 2px; }

.content-grid { display: grid; grid-template-columns: 1fr 360px; gap: 16px; }

.device-list-card, .chart-card, .detail-card {
  background: #0d1b2e;
  border: 1px solid #1a3a5c;
  border-radius: 12px;
  padding: 16px;
}

.right-panel { display: flex; flex-direction: column; gap: 12px; }

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.card-title { font-size: 14px; font-weight: 600; color: #e0f0ff; }

.device-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
}

.device-item {
  background: #0a1628;
  border: 1px solid #1a3a5c;
  border-radius: 10px;
  padding: 12px;
  cursor: pointer;
  transition: all 0.2s;
  text-align: center;
}
.device-item:hover { border-color: #00d4ff44; transform: translateY(-2px); }
.device-item.status-warning { border-color: #fadb1444; }
.device-item.status-error { border-color: #ff6b6b44; }

.device-icon { color: #00d4ff; margin-bottom: 6px; }
.device-name { font-size: 12px; color: #e0f0ff; font-weight: 600; margin-bottom: 2px; }
.device-floor { font-size: 11px; color: #8bafc9; }
.device-power { font-size: 14px; font-weight: 700; color: #00d4ff; margin: 4px 0; }

.device-status-badge {
  font-size: 10px;
  padding: 2px 8px;
  border-radius: 10px;
  display: inline-block;
}
.badge-normal { background: #52c41a22; color: #52c41a; }
.badge-warning { background: #fadb1422; color: #fadb14; }
.badge-error { background: #ff6b6b22; color: #ff6b6b; }

.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  margin-bottom: 12px;
}

.detail-item {
  background: #0a1628;
  border-radius: 8px;
  padding: 8px 10px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.detail-label { font-size: 11px; color: #8bafc9; }
.detail-value { font-size: 13px; font-weight: 600; color: #e0f0ff; }

.device-actions { display: flex; gap: 8px; flex-wrap: wrap; }
</style>
