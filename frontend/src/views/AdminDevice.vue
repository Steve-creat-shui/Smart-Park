<template>
  <div class="admin-device">
    <div class="page-title-bar">📟 设备资产管理</div>

    <el-tabs v-model="activeTab" class="dark-tabs">
      <!-- ====== Tab 1: 设备台账 ====== -->
      <el-tab-pane label="设备台账" name="registry">
        <div class="tab-toolbar">
          <el-input v-model="regSearch" placeholder="搜索设备名称/编号/位置" size="small" style="width:240px" clearable />
          <el-select v-model="regTypeFilter" placeholder="设备类型" size="small" style="width:130px" clearable>
            <el-option label="全部" value="" />
            <el-option v-for="t in deviceTypes" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
          <el-select v-model="regBuildingFilter" placeholder="所在楼宇" size="small" style="width:130px" clearable>
            <el-option label="全部" value="" />
            <el-option v-for="b in buildings" :key="b" :label="b" :value="b" />
          </el-select>
          <el-button type="primary" size="small" @click="showAddDevice">+ 新增设备</el-button>
          <el-button size="small" @click="showImportDialog = true">📥 批量导入</el-button>
          <el-button size="small" @click="exportDevices">📤 导出</el-button>
        </div>

        <el-table :data="filteredDevices" size="small" :header-cell-style="headerStyle" :cell-style="cellStyle" stripe style="margin-top:12px" max-height="520">
          <el-table-column prop="deviceCode" label="设备编号" width="120" />
          <el-table-column prop="name" label="设备名称" width="160" />
          <el-table-column prop="type" label="类型" width="100">
            <template #default="{ row }">
              <el-tag size="small" :type="typeTagColor(row.type)">{{ typeLabel(row.type) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="model" label="型号" width="130" />
          <el-table-column prop="building" label="楼宇" width="100" />
          <el-table-column prop="floor" label="楼层" width="70" />
          <el-table-column prop="room" label="房间" width="90" />
          <el-table-column prop="manufacturer" label="厂商" width="120" show-overflow-tooltip />
          <el-table-column prop="status" label="状态" width="90">
            <template #default="{ row }">
              <span class="status-dot" :class="row.status"></span>
              {{ statusLabel(row.status) }}
            </template>
          </el-table-column>
          <el-table-column prop="maintainCycle" label="维保周期" width="90">
            <template #default="{ row }">{{ row.maintainCycle }}天</template>
          </el-table-column>
          <el-table-column prop="lastMaintain" label="上次维保" width="110" />
          <el-table-column label="操作" width="160" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" size="small" text @click="editDevice(row)">编辑</el-button>
              <el-button type="warning" size="small" text @click="viewDevice(row)">详情</el-button>
              <el-button type="danger" size="small" text @click="deleteDevice(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="table-footer">
          共 {{ filteredDevices.length }} 台设备
        </div>
      </el-tab-pane>

      <!-- ====== Tab 2: 设备状态监控 ====== -->
      <el-tab-pane label="状态监控" name="monitor">
        <!-- 状态统计卡片 -->
        <div class="status-summary">
          <div class="ss-card online">
            <div class="ss-num">{{ statusCount('online') }}</div>
            <div class="ss-label">在线</div>
          </div>
          <div class="ss-card offline">
            <div class="ss-num">{{ statusCount('offline') }}</div>
            <div class="ss-label">离线</div>
          </div>
          <div class="ss-card fault">
            <div class="ss-num">{{ statusCount('fault') }}</div>
            <div class="ss-label">故障</div>
          </div>
          <div class="ss-card maintain">
            <div class="ss-num">{{ statusCount('maintain') }}</div>
            <div class="ss-label">维保中</div>
          </div>
          <div class="ss-card total">
            <div class="ss-num">{{ devices.length }}</div>
            <div class="ss-label">总设备</div>
          </div>
        </div>

        <!-- 楼宇设备地图 -->
        <div class="monitor-grid">
          <div v-for="b in buildings" :key="b" class="building-card">
            <div class="bc-header">
              <span class="bc-name">🏢 {{ b }}</span>
              <span class="bc-count">{{ buildingDeviceCount(b) }} 台</span>
            </div>
            <div class="bc-devices">
              <div v-for="d in buildingDevices(b)" :key="d.deviceCode" class="bc-device-item" :class="d.status" @click="viewDevice(d)">
                <span class="bdi-icon">{{ typeIcon(d.type) }}</span>
                <span class="bdi-name">{{ d.name }}</span>
                <span class="bdi-status"><span class="status-dot" :class="d.status"></span></span>
              </div>
            </div>
          </div>
        </div>

        <!-- 实时告警 -->
        <div class="ops-card" style="margin-top:16px">
          <div class="card-title">⚠️ 实时告警</div>
          <el-table :data="deviceAlerts" size="small" :header-cell-style="headerStyle" :cell-style="cellStyle" max-height="260">
            <el-table-column prop="time" label="时间" width="170" />
            <el-table-column prop="deviceName" label="设备" width="160" />
            <el-table-column prop="building" label="位置" width="120" />
            <el-table-column prop="level" label="级别" width="80">
              <template #default="{ row }">
                <el-tag :type="row.level === 'critical' ? 'danger' : row.level === 'warning' ? 'warning' : 'info'" size="small">
                  {{ { critical: '严重', warning: '警告', info: '提示' }[row.level] }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="message" label="告警内容" min-width="200" show-overflow-tooltip />
            <el-table-column label="操作" width="140">
              <template #default="{ row }">
                <el-button type="primary" size="small" text @click="handleAlert(row)">处理</el-button>
                <el-button type="warning" size="small" text @click="remoteRestart(row)">远程重启</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <!-- ====== Tab 3: 维保管理 ====== -->
      <el-tab-pane name="maintain">
        <template #label>
          <span>维保提醒 <el-badge v-if="overdueCount > 0" :value="overdueCount" :max="99" style="margin-left:4px" /></span>
        </template>

        <!-- 维保统计 -->
        <div class="status-summary">
          <div class="ss-card" style="border-left-color:#ff4d4f">
            <div class="ss-num" style="color:#ff4d4f">{{ overdueCount }}</div>
            <div class="ss-label">已超期</div>
          </div>
          <div class="ss-card" style="border-left-color:#faad14">
            <div class="ss-num" style="color:#faad14">{{ dueSoonCount }}</div>
            <div class="ss-label">即将到期(7天内)</div>
          </div>
          <div class="ss-card" style="border-left-color:#52c41a">
            <div class="ss-num" style="color:#52c41a">{{ normalCount }}</div>
            <div class="ss-label">维保正常</div>
          </div>
        </div>

        <!-- 维保任务列表 -->
        <div class="ops-card" style="margin-top:16px">
          <div class="tab-toolbar" style="margin-bottom:12px">
            <el-radio-group v-model="maintainFilter" size="small">
              <el-radio-button value="all">全部</el-radio-button>
              <el-radio-button value="overdue">已超期</el-radio-button>
              <el-radio-button value="soon">即将到期</el-radio-button>
              <el-radio-button value="normal">正常</el-radio-button>
            </el-radio-group>
            <el-button type="primary" size="small" @click="generateWorkOrders">🔧 批量生成工单</el-button>
          </div>

          <el-table :data="filteredMaintainList" size="small" :header-cell-style="headerStyle" :cell-style="cellStyle" stripe max-height="400">
            <el-table-column prop="deviceCode" label="设备编号" width="120" />
            <el-table-column prop="name" label="设备名称" width="160" />
            <el-table-column prop="building" label="位置" width="120">
              <template #default="{ row }">{{ row.building }} {{ row.floor }}F</template>
            </el-table-column>
            <el-table-column prop="maintainCycle" label="周期(天)" width="90" />
            <el-table-column prop="lastMaintain" label="上次维保" width="110" />
            <el-table-column prop="nextMaintain" label="下次维保" width="110" />
            <el-table-column label="剩余天数" width="100">
              <template #default="{ row }">
                <span :style="{ color: daysColor(row.daysLeft), fontWeight: 'bold' }">
                  {{ row.daysLeft > 0 ? row.daysLeft + '天' : '超期' + Math.abs(row.daysLeft) + '天' }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.daysLeft < 0 ? 'danger' : row.daysLeft <= 7 ? 'warning' : 'success'" size="small">
                  {{ row.daysLeft < 0 ? '已超期' : row.daysLeft <= 7 ? '即将到期' : '正常' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="140">
              <template #default="{ row }">
                <el-button type="primary" size="small" text @click="createWorkOrder(row)">生成工单</el-button>
                <el-button type="success" size="small" text @click="markMaintained(row)">标记完成</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <!-- 维保历史 -->
        <div class="ops-card" style="margin-top:16px">
          <div class="card-title">📋 维保历史记录</div>
          <el-table :data="maintainHistory" size="small" :header-cell-style="headerStyle" :cell-style="cellStyle" max-height="220">
            <el-table-column prop="time" label="维保时间" width="170" />
            <el-table-column prop="deviceName" label="设备" width="160" />
            <el-table-column prop="building" label="位置" width="120" />
            <el-table-column prop="worker" label="维保人员" width="100" />
            <el-table-column prop="content" label="维保内容" min-width="200" show-overflow-tooltip />
            <el-table-column prop="result" label="结果" width="80">
              <template #default="{ row }">
                <el-tag :type="row.result === 'ok' ? 'success' : 'warning'" size="small">{{ row.result === 'ok' ? '正常' : '需跟进' }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 新增/编辑设备弹窗 -->
    <el-dialog v-model="deviceDialogVisible" :title="editingDevice ? '编辑设备' : '新增设备'" width="600px" class="dark-dialog">
      <el-form :model="deviceForm" label-width="100px" class="dark-form">
        <el-form-item label="设备编号">
          <el-input v-model="deviceForm.deviceCode" placeholder="如: DEV-AC-001" />
        </el-form-item>
        <el-form-item label="设备名称">
          <el-input v-model="deviceForm.name" placeholder="如: 行政楼1F中央空调" />
        </el-form-item>
        <el-form-item label="设备类型">
          <el-select v-model="deviceForm.type" style="width:100%">
            <el-option v-for="t in deviceTypes" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="型号">
          <el-input v-model="deviceForm.model" placeholder="设备型号" />
        </el-form-item>
        <el-form-item label="厂商">
          <el-input v-model="deviceForm.manufacturer" placeholder="生产厂商" />
        </el-form-item>
        <div style="display:flex;gap:12px">
          <el-form-item label="所在楼宇" style="flex:1">
            <el-select v-model="deviceForm.building" style="width:100%">
              <el-option v-for="b in buildings" :key="b" :label="b" :value="b" />
            </el-select>
          </el-form-item>
          <el-form-item label="楼层" style="width:120px">
            <el-input-number v-model="deviceForm.floor" :min="1" :max="30" style="width:100%" />
          </el-form-item>
          <el-form-item label="房间" style="width:140px">
            <el-input v-model="deviceForm.room" placeholder="101" />
          </el-form-item>
        </div>
        <el-form-item label="维保周期">
          <el-input-number v-model="deviceForm.maintainCycle" :min="7" :max="730" :step="30" />
          <span class="hint">天</span>
        </el-form-item>
        <el-form-item label="安装日期">
          <el-date-picker v-model="deviceForm.installDate" type="date" style="width:100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="deviceDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveDevice">{{ editingDevice ? '保存' : '新增' }}</el-button>
      </template>
    </el-dialog>

    <!-- 设备详情弹窗 -->
    <el-dialog v-model="detailDialogVisible" title="设备详情" width="650px" class="dark-dialog">
      <div v-if="detailDevice" class="device-detail">
        <div class="dd-header">
          <span class="dd-icon">{{ typeIcon(detailDevice.type) }}</span>
          <div>
            <div class="dd-name">{{ detailDevice.name }}</div>
            <div class="dd-code">{{ detailDevice.deviceCode }}</div>
          </div>
          <span class="dd-status" :class="detailDevice.status">
            <span class="status-dot" :class="detailDevice.status"></span>
            {{ statusLabel(detailDevice.status) }}
          </span>
        </div>
        <div class="dd-grid">
          <div class="dd-item"><span class="dd-label">类型</span><span>{{ typeLabel(detailDevice.type) }}</span></div>
          <div class="dd-item"><span class="dd-label">型号</span><span>{{ detailDevice.model }}</span></div>
          <div class="dd-item"><span class="dd-label">厂商</span><span>{{ detailDevice.manufacturer }}</span></div>
          <div class="dd-item"><span class="dd-label">位置</span><span>{{ detailDevice.building }} {{ detailDevice.floor }}F {{ detailDevice.room }}</span></div>
          <div class="dd-item"><span class="dd-label">维保周期</span><span>{{ detailDevice.maintainCycle }} 天</span></div>
          <div class="dd-item"><span class="dd-label">上次维保</span><span>{{ detailDevice.lastMaintain }}</span></div>
          <div class="dd-item"><span class="dd-label">安装日期</span><span>{{ detailDevice.installDate }}</span></div>
          <div class="dd-item"><span class="dd-label">运行时长</span><span>{{ detailDevice.runHours || '—' }} 小时</span></div>
        </div>
        <div class="dd-section">
          <div class="card-title">📊 实时参数</div>
          <div class="dd-params">
            <div v-for="p in detailDevice.params || defaultParams(detailDevice.type)" :key="p.label" class="dd-param">
              <div class="ddp-val" :style="{color: p.color}">{{ p.value }}</div>
              <div class="ddp-label">{{ p.label }}</div>
            </div>
          </div>
        </div>
        <div class="dd-actions">
          <el-button type="warning" size="small" @click="remoteRestart(detailDevice)">🔄 远程重启</el-button>
          <el-button type="primary" size="small" @click="sendParams(detailDevice)">📤 参数下发</el-button>
          <el-button type="success" size="small" @click="markMaintained(detailDevice)">✅ 标记维保完成</el-button>
        </div>
      </div>
    </el-dialog>

    <!-- 批量导入弹窗 -->
    <el-dialog v-model="showImportDialog" title="批量导入设备" width="480px" class="dark-dialog">
      <div style="color:#8bafc9;margin-bottom:16px">支持 Excel (.xlsx) 格式，请按模板格式填写设备信息</div>
      <el-upload drag action="#" :auto-upload="false" :on-change="handleImportFile" accept=".xlsx,.xls,.csv">
        <div style="padding:20px;color:#5a7a9a">
          <div style="font-size:32px;margin-bottom:8px">📁</div>
          <div>拖拽文件到此处，或 <span style="color:#00d4ff">点击上传</span></div>
        </div>
      </el-upload>
      <template #footer>
        <el-button size="small" @click="downloadTemplate">📋 下载模板</el-button>
        <el-button @click="showImportDialog = false">取消</el-button>
        <el-button type="primary" @click="importDevices">确认导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const activeTab = ref('registry')
const headerStyle = { background: '#0f2540', color: '#8bafc9', borderColor: '#1a3a5c' }
const cellStyle = { background: '#0d1b2e', color: '#c0d8f0', borderColor: '#1a3a5c' }

const deviceTypes = [
  { value: 'meter', label: '能源计量' },
  { value: 'ac', label: '空调' },
  { value: 'light', label: '照明' },
  { value: 'elevator', label: '电梯' },
  { value: 'sensor', label: '传感器' },
  { value: 'camera', label: '监控' },
  { value: 'fire', label: '消防' },
  { value: 'other', label: '其他' }
]
const buildings = ['行政楼', '图书馆', '实验楼', '教学楼', '宿舍楼', '餐厅', '体育馆']

// 设备台账数据
const devices = ref([
  { deviceCode: 'MTR-001', name: '行政楼总电表', type: 'meter', model: 'DTS686-L', building: '行政楼', floor: 1, room: '配电室', manufacturer: '正泰电器', status: 'online', maintainCycle: 180, lastMaintain: '2026-03-15', installDate: '2024-01-10', runHours: 18720 },
  { deviceCode: 'MTR-002', name: '图书馆总电表', type: 'meter', model: 'DTS686-L', building: '图书馆', floor: 1, room: '配电室', manufacturer: '正泰电器', status: 'online', maintainCycle: 180, lastMaintain: '2026-03-10', installDate: '2024-01-10', runHours: 18720 },
  { deviceCode: 'AC-001', name: '行政楼1F中央空调', type: 'ac', model: 'GMV-785WM/X', building: '行政楼', floor: 1, room: '大厅', manufacturer: '格力电器', status: 'online', maintainCycle: 90, lastMaintain: '2026-03-20', installDate: '2024-06-15', runHours: 12800 },
  { deviceCode: 'AC-002', name: '实验楼3F空调机组', type: 'ac', model: 'MDV-560W', building: '实验楼', floor: 3, room: '301', manufacturer: '美的集团', status: 'fault', maintainCycle: 90, lastMaintain: '2026-02-10', installDate: '2024-06-20', runHours: 11200 },
  { deviceCode: 'LT-001', name: '图书馆智能照明系统', type: 'light', model: 'LD-Smart-200', building: '图书馆', floor: 2, room: '阅览室', manufacturer: '欧普照明', status: 'online', maintainCycle: 365, lastMaintain: '2025-12-01', installDate: '2024-03-01', runHours: 15600 },
  { deviceCode: 'LT-002', name: '教学楼走廊灯控', type: 'light', model: 'LD-Ctrl-100', building: '教学楼', floor: 1, room: '走廊', manufacturer: '欧普照明', status: 'online', maintainCycle: 365, lastMaintain: '2026-01-15', installDate: '2024-03-05', runHours: 15400 },
  { deviceCode: 'ELV-001', name: '行政楼客梯#1', type: 'elevator', model: 'OTIS-GeN2', building: '行政楼', floor: 1, room: '电梯间', manufacturer: 'OTIS奥的斯', status: 'online', maintainCycle: 30, lastMaintain: '2026-04-01', installDate: '2023-12-01', runHours: 22000 },
  { deviceCode: 'ELV-002', name: '图书馆客梯#1', type: 'elevator', model: 'ThyssenK-TE', building: '图书馆', floor: 1, room: '电梯间', manufacturer: '蒂森克虏伯', status: 'maintain', maintainCycle: 30, lastMaintain: '2026-04-10', installDate: '2023-12-01', runHours: 21500 },
  { deviceCode: 'SEN-001', name: '实验楼温湿度传感器', type: 'sensor', model: 'TH-200Pro', building: '实验楼', floor: 2, room: '实验室A', manufacturer: '海康威视', status: 'online', maintainCycle: 365, lastMaintain: '2025-11-20', installDate: '2024-05-10', runHours: 16000 },
  { deviceCode: 'SEN-002', name: '餐厅烟感探测器', type: 'sensor', model: 'SD-100', building: '餐厅', floor: 1, room: '厨房', manufacturer: '海湾安全', status: 'online', maintainCycle: 180, lastMaintain: '2026-02-28', installDate: '2024-01-20', runHours: 18500 },
  { deviceCode: 'CAM-001', name: '行政楼大门监控', type: 'camera', model: 'DS-2CD2T47', building: '行政楼', floor: 1, room: '大门', manufacturer: '海康威视', status: 'online', maintainCycle: 180, lastMaintain: '2026-03-05', installDate: '2024-02-01', runHours: 18400 },
  { deviceCode: 'CAM-002', name: '宿舍楼出入口监控', type: 'camera', model: 'DS-2CD2T87', building: '宿舍楼', floor: 1, room: '出入口', manufacturer: '海康威视', status: 'offline', maintainCycle: 180, lastMaintain: '2026-01-20', installDate: '2024-02-01', runHours: 18200 },
  { deviceCode: 'FIR-001', name: '体育馆消防主机', type: 'fire', model: 'JB-QBL-QM300', building: '体育馆', floor: 1, room: '消防控制室', manufacturer: '青鸟消防', status: 'online', maintainCycle: 90, lastMaintain: '2026-03-25', installDate: '2023-11-15', runHours: 20000 },
  { deviceCode: 'MTR-003', name: '宿舍楼水表', type: 'meter', model: 'LXSG-20E', building: '宿舍楼', floor: 1, room: '水泵房', manufacturer: '宁波水表', status: 'online', maintainCycle: 365, lastMaintain: '2025-10-01', installDate: '2024-01-10', runHours: 18720 },
  { deviceCode: 'AC-003', name: '餐厅新风系统', type: 'ac', model: 'ERV-500', building: '餐厅', floor: 1, room: '主厅', manufacturer: '大金空调', status: 'online', maintainCycle: 90, lastMaintain: '2026-04-05', installDate: '2024-07-01', runHours: 10500 },
])

// 搜索/筛选
const regSearch = ref('')
const regTypeFilter = ref('')
const regBuildingFilter = ref('')

const filteredDevices = computed(() => {
  return devices.value.filter(d => {
    const matchSearch = !regSearch.value || d.name.includes(regSearch.value) || d.deviceCode.includes(regSearch.value) || d.building.includes(regSearch.value)
    const matchType = !regTypeFilter.value || d.type === regTypeFilter.value
    const matchBuilding = !regBuildingFilter.value || d.building === regBuildingFilter.value
    return matchSearch && matchType && matchBuilding
  })
})

// 设备告警
const deviceAlerts = ref([
  { time: '2026-04-13 10:28:15', deviceName: '实验楼3F空调机组', building: '实验楼 3F', level: 'critical', message: '压缩机过载保护触发，设备已停机' },
  { time: '2026-04-13 10:15:32', deviceName: '宿舍楼出入口监控', building: '宿舍楼 1F', level: 'warning', message: '设备离线超过30分钟，疑似网络故障' },
  { time: '2026-04-13 09:45:00', deviceName: '宿舍楼水表', building: '宿舍楼 1F', level: 'info', message: '维保周期即将到期，剩余12天' },
  { time: '2026-04-13 08:30:12', deviceName: '实验楼温湿度传感器', building: '实验楼 2F', level: 'warning', message: '温度读数异常偏高: 38.5°C' },
])

// 维保计算
const today = new Date()
const maintainList = computed(() => {
  return devices.value.map(d => {
    const last = new Date(d.lastMaintain)
    const next = new Date(last.getTime() + d.maintainCycle * 86400000)
    const daysLeft = Math.ceil((next - today) / 86400000)
    return { ...d, nextMaintain: next.toISOString().slice(0, 10), daysLeft }
  }).sort((a, b) => a.daysLeft - b.daysLeft)
})

const maintainFilter = ref('all')
const filteredMaintainList = computed(() => {
  if (maintainFilter.value === 'overdue') return maintainList.value.filter(d => d.daysLeft < 0)
  if (maintainFilter.value === 'soon') return maintainList.value.filter(d => d.daysLeft >= 0 && d.daysLeft <= 7)
  if (maintainFilter.value === 'normal') return maintainList.value.filter(d => d.daysLeft > 7)
  return maintainList.value
})

const overdueCount = computed(() => maintainList.value.filter(d => d.daysLeft < 0).length)
const dueSoonCount = computed(() => maintainList.value.filter(d => d.daysLeft >= 0 && d.daysLeft <= 7).length)
const normalCount = computed(() => maintainList.value.filter(d => d.daysLeft > 7).length)

// 维保历史
const maintainHistory = ref([
  { time: '2026-04-10 14:30:00', deviceName: '图书馆客梯#1', building: '图书馆 1F', worker: '王工', content: '季度保养：曳引机润滑、安全钳检测、门机调整', result: 'ok' },
  { time: '2026-04-05 09:00:00', deviceName: '餐厅新风系统', building: '餐厅 1F', worker: '李工', content: '滤网更换、风机轴承润滑、风量校准', result: 'ok' },
  { time: '2026-04-01 11:00:00', deviceName: '行政楼客梯#1', building: '行政楼 1F', worker: '王工', content: '月度检查：制动器、限速器、缓冲器测试', result: 'ok' },
  { time: '2026-03-25 15:00:00', deviceName: '体育馆消防主机', building: '体育馆 1F', worker: '赵工', content: '季度检测：烟感灵敏度、喷淋压力、报警联动', result: 'ok' },
  { time: '2026-03-20 10:00:00', deviceName: '行政楼1F中央空调', building: '行政楼 1F', worker: '张工', content: '季度保养：冷媒压力检测、滤网清洗、电气检查', result: 'ok' },
])

// 弹窗
const deviceDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const showImportDialog = ref(false)
const editingDevice = ref(false)
const detailDevice = ref(null)
const deviceForm = ref({
  deviceCode: '', name: '', type: 'meter', model: '', manufacturer: '',
  building: '行政楼', floor: 1, room: '', maintainCycle: 180, installDate: ''
})

// 工具函数
function typeLabel(t) { return deviceTypes.find(x => x.value === t)?.label || t }
function typeIcon(t) {
  return { meter: '⚡', ac: '❄️', light: '💡', elevator: '🛗', sensor: '📡', camera: '📹', fire: '🔥', other: '⚙️' }[t] || '⚙️'
}
function typeTagColor(t) {
  return { meter: '', ac: 'success', light: 'warning', elevator: 'info', sensor: '', camera: 'info', fire: 'danger' }[t] || ''
}
function statusLabel(s) { return { online: '在线', offline: '离线', fault: '故障', maintain: '维保中' }[s] || s }
function statusCount(s) { return devices.value.filter(d => d.status === s).length }
function buildingDeviceCount(b) { return devices.value.filter(d => d.building === b).length }
function buildingDevices(b) { return devices.value.filter(d => d.building === b) }
function daysColor(d) { return d < 0 ? '#ff4d4f' : d <= 7 ? '#faad14' : '#52c41a' }

function defaultParams(type) {
  const params = {
    meter: [{ label: '功率', value: '12.5 kW', color: '#00d4ff' }, { label: '电压', value: '380V', color: '#52c41a' }, { label: '电流', value: '32.8A', color: '#faad14' }, { label: '功率因数', value: '0.95', color: '#8b5cf6' }],
    ac: [{ label: '送风温度', value: '22°C', color: '#00d4ff' }, { label: '回风温度', value: '26°C', color: '#faad14' }, { label: '运行模式', value: '制冷', color: '#52c41a' }, { label: '负载率', value: '68%', color: '#8b5cf6' }],
    light: [{ label: '亮度', value: '85%', color: '#faad14' }, { label: '功率', value: '2.1 kW', color: '#00d4ff' }, { label: '色温', value: '4000K', color: '#8b5cf6' }, { label: '灯组数', value: '48', color: '#52c41a' }],
    sensor: [{ label: '温度', value: '24.5°C', color: '#00d4ff' }, { label: '湿度', value: '62%', color: '#52c41a' }, { label: 'PM2.5', value: '35', color: '#faad14' }, { label: 'CO2', value: '420ppm', color: '#8b5cf6' }],
    elevator: [{ label: '当前楼层', value: '3F', color: '#00d4ff' }, { label: '运行状态', value: '上行', color: '#52c41a' }, { label: '今日运行', value: '342次', color: '#faad14' }, { label: '负载', value: '45%', color: '#8b5cf6' }],
    camera: [{ label: '分辨率', value: '4MP', color: '#00d4ff' }, { label: '帧率', value: '25fps', color: '#52c41a' }, { label: '存储', value: '128G', color: '#faad14' }, { label: '可用', value: '67%', color: '#8b5cf6' }],
    fire: [{ label: '烟感', value: '正常', color: '#52c41a' }, { label: '温感', value: '正常', color: '#52c41a' }, { label: '喷淋压力', value: '0.8MPa', color: '#00d4ff' }, { label: '联动状态', value: '就绪', color: '#faad14' }],
  }
  return params[type] || params.meter
}

// 操作
function showAddDevice() {
  editingDevice.value = false
  deviceForm.value = { deviceCode: '', name: '', type: 'meter', model: '', manufacturer: '', building: '行政楼', floor: 1, room: '', maintainCycle: 180, installDate: '' }
  deviceDialogVisible.value = true
}
function editDevice(row) {
  editingDevice.value = true
  deviceForm.value = { ...row }
  deviceDialogVisible.value = true
}
function saveDevice() {
  if (!deviceForm.value.deviceCode || !deviceForm.value.name) { ElMessage.warning('请填写设备编号和名称'); return }
  if (editingDevice.value) {
    const idx = devices.value.findIndex(d => d.deviceCode === deviceForm.value.deviceCode)
    if (idx >= 0) devices.value[idx] = { ...devices.value[idx], ...deviceForm.value }
    ElMessage.success('设备信息已更新')
  } else {
    devices.value.push({ ...deviceForm.value, status: 'online', lastMaintain: new Date().toISOString().slice(0, 10), runHours: 0 })
    ElMessage.success('设备已添加')
  }
  deviceDialogVisible.value = false
}
function deleteDevice(row) {
  ElMessageBox.confirm(`确定删除设备 ${row.name}？`, '确认', { type: 'warning' })
    .then(() => { devices.value = devices.value.filter(d => d.deviceCode !== row.deviceCode); ElMessage.success('已删除') })
    .catch(() => {})
}
function viewDevice(row) {
  detailDevice.value = row
  detailDialogVisible.value = true
}

function remoteRestart(row) {
  ElMessageBox.confirm(`确定远程重启 ${row.deviceName || row.name}？`, '远程重启', { type: 'warning' })
    .then(() => { ElMessage.success('重启指令已下发，预计30秒后恢复') })
    .catch(() => {})
}
function sendParams(row) { ElMessage.info(`参数下发面板开发中 - ${row.name}`) }
function handleAlert(row) { ElMessage.success(`告警 "${row.message}" 已标记为处理中`) }

function createWorkOrder(row) {
  ElMessage.success(`已为 ${row.name} 生成维保工单`)
}
function markMaintained(row) {
  const d = devices.value.find(x => x.deviceCode === (row.deviceCode))
  if (d) {
    d.lastMaintain = new Date().toISOString().slice(0, 10)
    if (d.status === 'maintain') d.status = 'online'
  }
  ElMessage.success(`${row.name} 已标记维保完成`)
}
function generateWorkOrders() {
  const count = maintainList.value.filter(d => d.daysLeft <= 7).length
  ElMessage.success(`已为 ${count} 台设备批量生成维保工单`)
}

function exportDevices() { ElMessage.success('设备台账导出中...') }
function handleImportFile() { ElMessage.info('文件已选择') }
function importDevices() { showImportDialog.value = false; ElMessage.success('设备数据导入成功') }
function downloadTemplate() { ElMessage.info('模板下载中...') }
</script>

<style scoped>
.admin-device { display: flex; flex-direction: column; gap: 16px; }
.page-title-bar { font-size: 18px; font-weight: 700; color: #e0f0ff; }
.ops-card { background: #0d1b2e; border-radius: 10px; padding: 18px; }
.card-title { color: #e0f0ff; font-weight: 600; font-size: 15px; margin-bottom: 12px; }
.hint { color: #5a7a9a; font-size: 12px; margin-left: 8px; }
.tab-toolbar { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
.table-footer { margin-top: 10px; color: #5a7a9a; font-size: 12px; }

.dark-tabs :deep(.el-tabs__item) { color: #5a7a9a; }
.dark-tabs :deep(.el-tabs__item.is-active) { color: #00d4ff; }
.dark-tabs :deep(.el-tabs__active-bar) { background: #00d4ff; }
.dark-tabs :deep(.el-tabs__nav-wrap::after) { background: #1a3a5c; }
.dark-form :deep(.el-form-item__label) { color: #8bafc9; }
.dark-form :deep(.el-input__wrapper) { background: #0a1628 !important; border: 1px solid #1a3a5c !important; box-shadow: none !important; }
.dark-form :deep(.el-input__inner) { color: #e0f0ff !important; }
.dark-form :deep(.el-select .el-input__wrapper) { background: #0a1628 !important; }
.dark-form :deep(.el-input-number) { --el-fill-color-blank: #0a1628; }

.status-dot { display: inline-block; width: 8px; height: 8px; border-radius: 50%; margin-right: 4px; }
.status-dot.online { background: #52c41a; box-shadow: 0 0 6px #52c41a; }
.status-dot.offline { background: #8c8c8c; }
.status-dot.fault { background: #ff4d4f; box-shadow: 0 0 6px #ff4d4f; animation: blink 1s infinite; }
.status-dot.maintain { background: #faad14; }
@keyframes blink { 0%,100% { opacity: 1; } 50% { opacity: 0.3; } }

/* 状态统计 */
.status-summary { display: flex; gap: 14px; flex-wrap: wrap; }
.ss-card { background: #0d1b2e; border-radius: 10px; padding: 16px 24px; border-left: 3px solid #1a3a5c; min-width: 120px; text-align: center; }
.ss-card.online { border-left-color: #52c41a; }
.ss-card.offline { border-left-color: #8c8c8c; }
.ss-card.fault { border-left-color: #ff4d4f; }
.ss-card.maintain { border-left-color: #faad14; }
.ss-card.total { border-left-color: #00d4ff; }
.ss-num { font-size: 28px; font-weight: 700; }
.ss-card.online .ss-num { color: #52c41a; }
.ss-card.offline .ss-num { color: #8c8c8c; }
.ss-card.fault .ss-num { color: #ff4d4f; }
.ss-card.maintain .ss-num { color: #faad14; }
.ss-card.total .ss-num { color: #00d4ff; }
.ss-label { color: #5a7a9a; font-size: 13px; margin-top: 4px; }

/* 楼宇设备网格 */
.monitor-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 14px; margin-top: 16px; }
.building-card { background: #0d1b2e; border-radius: 10px; padding: 14px; }
.bc-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.bc-name { color: #e0f0ff; font-weight: 600; font-size: 14px; }
.bc-count { color: #5a7a9a; font-size: 12px; }
.bc-devices { display: flex; flex-direction: column; gap: 6px; max-height: 180px; overflow-y: auto; }
.bc-device-item { display: flex; align-items: center; gap: 8px; padding: 6px 10px; background: #0a1628; border-radius: 6px; cursor: pointer; transition: all 0.2s; font-size: 13px; color: #c0d8f0; }
.bc-device-item:hover { background: rgba(0,212,255,0.1); }
.bc-device-item.fault { border-left: 2px solid #ff4d4f; }
.bc-device-item.offline { border-left: 2px solid #8c8c8c; opacity: 0.7; }
.bdi-icon { font-size: 16px; }
.bdi-name { flex: 1; }
.bdi-status { flex-shrink: 0; }

/* 设备详情 */
.device-detail { color: #c0d8f0; }
.dd-header { display: flex; align-items: center; gap: 14px; margin-bottom: 20px; padding-bottom: 14px; border-bottom: 1px solid #1a3a5c; }
.dd-icon { font-size: 36px; }
.dd-name { color: #e0f0ff; font-size: 18px; font-weight: 700; }
.dd-code { color: #5a7a9a; font-size: 12px; margin-top: 2px; }
.dd-status { margin-left: auto; padding: 4px 12px; border-radius: 20px; font-size: 13px; background: #0a1628; }
.dd-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; margin-bottom: 16px; }
.dd-item { padding: 8px 12px; background: #0a1628; border-radius: 6px; display: flex; justify-content: space-between; }
.dd-label { color: #5a7a9a; }
.dd-section { margin-top: 16px; }
.dd-params { display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px; }
.dd-param { text-align: center; padding: 12px; background: #0a1628; border-radius: 8px; }
.ddp-val { font-size: 20px; font-weight: 700; }
.ddp-label { color: #5a7a9a; font-size: 12px; margin-top: 4px; }
.dd-actions { display: flex; gap: 10px; margin-top: 18px; padding-top: 14px; border-top: 1px solid #1a3a5c; }
</style>
