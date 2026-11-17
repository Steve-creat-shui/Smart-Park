<template>
  <div class="admin-audit">
    <div class="page-title-bar">🔍 审计与日志中心</div>

    <el-tabs v-model="activeTab" class="dark-tabs">
      <!-- ====== Tab 1: 操作日志 ====== -->
      <el-tab-pane label="操作日志" name="oplog">
        <div class="ops-card">
          <div class="filter-row">
            <el-select v-model="opFilter.type" placeholder="操作类型" size="small" style="width:140px" clearable>
              <el-option label="全部" value="" />
              <el-option label="用户登录" value="login" />
              <el-option label="数据导入" value="import" />
              <el-option label="数据导出" value="export" />
              <el-option label="配置变更" value="config" />
              <el-option label="用户审批" value="approve" />
              <el-option label="设备操作" value="device" />
              <el-option label="角色变更" value="role" />
              <el-option label="密码重置" value="password" />
            </el-select>
            <el-input v-model="opFilter.keyword" placeholder="搜索操作人/内容" size="small" style="width:200px" clearable />
            <el-date-picker v-model="opFilter.dateRange" type="daterange" size="small" start-placeholder="开始" end-placeholder="结束" style="width:260px" :shortcuts="dateShortcuts" />
            <el-button type="primary" size="small" @click="loadOpLogs">查询</el-button>
            <el-button size="small" @click="exportOpLogs">📤 导出</el-button>
          </div>
          <el-table :data="filteredOpLogs" size="small" :header-cell-style="headerStyle" :cell-style="cellStyle" style="margin-top:12px" max-height="460" stripe>
            <el-table-column prop="time" label="时间" width="170" />
            <el-table-column prop="user" label="操作人" width="110" />
            <el-table-column prop="role" label="角色" width="80">
              <template #default="{ row }">
                <el-tag size="small" :type="{admin:'danger',enterprise:'warning',user:'info',system:''}[row.role]" effect="plain">
                  {{ {admin:'管理员',enterprise:'企业',user:'用户',system:'系统'}[row.role] }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="type" label="类型" width="100">
              <template #default="{ row }">
                <el-tag size="small" :type="opTagType(row.type)">{{ row.typeLabel }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="detail" label="操作详情" min-width="300" show-overflow-tooltip />
            <el-table-column prop="ip" label="IP地址" width="130" />
            <el-table-column prop="result" label="结果" width="80">
              <template #default="{ row }">
                <span :style="{color: row.result === 'success' ? '#52c41a' : '#ff4d4f'}">{{ row.result === 'success' ? '成功' : '失败' }}</span>
              </template>
            </el-table-column>
          </el-table>
          <div class="table-footer">共 {{ filteredOpLogs.length }} 条记录</div>
        </div>
      </el-tab-pane>

      <!-- ====== Tab 2: 登录审计 ====== -->
      <el-tab-pane label="登录审计" name="login">
        <div class="status-summary">
          <div class="ss-card" style="border-left-color:#52c41a">
            <div class="ss-num" style="color:#52c41a">{{ loginStats.success }}</div>
            <div class="ss-label">成功登录</div>
          </div>
          <div class="ss-card" style="border-left-color:#ff4d4f">
            <div class="ss-num" style="color:#ff4d4f">{{ loginStats.failed }}</div>
            <div class="ss-label">失败尝试</div>
          </div>
          <div class="ss-card" style="border-left-color:#faad14">
            <div class="ss-num" style="color:#faad14">{{ loginStats.locked }}</div>
            <div class="ss-label">账号锁定</div>
          </div>
          <div class="ss-card" style="border-left-color:#8b5cf6">
            <div class="ss-num" style="color:#8b5cf6">{{ loginStats.uniqueIPs }}</div>
            <div class="ss-label">不同IP</div>
          </div>
          <div class="ss-card" style="border-left-color:#00d4ff">
            <div class="ss-num" style="color:#00d4ff">{{ loginStats.todayActive }}</div>
            <div class="ss-label">今日活跃用户</div>
          </div>
        </div>

        <div class="ops-card" style="margin-top:16px">
          <div class="filter-row" style="margin-bottom:12px">
            <el-select v-model="loginFilter" size="small" style="width:130px">
              <el-option label="全部" value="" />
              <el-option label="成功" value="success" />
              <el-option label="失败" value="failed" />
            </el-select>
            <el-input v-model="loginSearch" placeholder="搜索用户/IP" size="small" style="width:200px" clearable />
            <el-button type="primary" size="small" @click="loadLoginLogs">查询</el-button>
          </div>
          <el-table :data="filteredLoginLogs" size="small" :header-cell-style="headerStyle" :cell-style="cellStyle" max-height="380" stripe>
            <el-table-column prop="time" label="登录时间" width="170" />
            <el-table-column prop="phone" label="手机号" width="130" />
            <el-table-column prop="nickname" label="用户名" width="110" />
            <el-table-column prop="role" label="角色" width="80">
              <template #default="{ row }">
                <el-tag size="small" :type="{admin:'danger',enterprise:'warning',user:'info'}[row.role]" effect="plain">
                  {{ {admin:'管理员',enterprise:'企业',user:'用户'}[row.role] }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="ip" label="IP地址" width="140" />
            <el-table-column prop="device" label="设备/浏览器" width="180" show-overflow-tooltip />
            <el-table-column prop="location" label="地点" width="120" />
            <el-table-column prop="result" label="结果" width="80">
              <template #default="{ row }">
                <el-tag :type="row.result === 'success' ? 'success' : 'danger'" size="small">{{ row.result === 'success' ? '成功' : '失败' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="failReason" label="失败原因" width="140" show-overflow-tooltip>
              <template #default="{ row }">
                <span style="color:#ff6b6b">{{ row.failReason || '—' }}</span>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <!-- ====== Tab 3: 数据变更追溯 ====== -->
      <el-tab-pane label="数据变更追溯" name="datachange">
        <div class="ops-card">
          <div class="filter-row" style="margin-bottom:12px">
            <el-select v-model="changeFilter.category" placeholder="数据类别" size="small" style="width:140px" clearable>
              <el-option label="全部" value="" />
              <el-option label="能耗数据" value="energy" />
              <el-option label="系数配置" value="config" />
              <el-option label="设备参数" value="device" />
              <el-option label="用户信息" value="user" />
              <el-option label="企业信息" value="enterprise" />
            </el-select>
            <el-input v-model="changeFilter.keyword" placeholder="搜索变更内容" size="small" style="width:200px" clearable />
            <el-button type="primary" size="small" @click="loadChangeLogs">查询</el-button>
          </div>

          <el-table :data="filteredChangeLogs" size="small" :header-cell-style="headerStyle" :cell-style="cellStyle" max-height="400" stripe>
            <el-table-column prop="time" label="变更时间" width="170" />
            <el-table-column prop="operator" label="操作人" width="100" />
            <el-table-column prop="category" label="数据类别" width="100">
              <template #default="{ row }">
                <el-tag size="small" effect="plain">{{ categoryLabel(row.category) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="target" label="变更对象" width="180" show-overflow-tooltip />
            <el-table-column prop="field" label="变更字段" width="120" />
            <el-table-column label="变更前" width="150">
              <template #default="{ row }">
                <span class="val-old">{{ row.oldValue }}</span>
              </template>
            </el-table-column>
            <el-table-column label="变更后" width="150">
              <template #default="{ row }">
                <span class="val-new">{{ row.newValue }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button type="primary" size="small" text @click="viewSnapshot(row)">快照对比</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <!-- 快照对比弹窗 -->
        <el-dialog v-model="snapshotVisible" title="数据快照对比" width="700px" class="dark-dialog">
          <div v-if="snapshotData" class="snapshot-compare">
            <div class="snap-info">
              <span>操作人: <b>{{ snapshotData.operator }}</b></span>
              <span>时间: <b>{{ snapshotData.time }}</b></span>
              <span>对象: <b>{{ snapshotData.target }}</b></span>
            </div>
            <div class="snap-grid">
              <div class="snap-col">
                <div class="snap-col-title" style="color:#ff6b6b">变更前</div>
                <div class="snap-content">
                  <div v-for="f in snapshotData.fields" :key="f.name" class="snap-field" :class="{ changed: f.changed }">
                    <span class="sf-name">{{ f.name }}</span>
                    <span class="sf-val">{{ f.oldVal }}</span>
                  </div>
                </div>
              </div>
              <div class="snap-arrow">→</div>
              <div class="snap-col">
                <div class="snap-col-title" style="color:#52c41a">变更后</div>
                <div class="snap-content">
                  <div v-for="f in snapshotData.fields" :key="f.name" class="snap-field" :class="{ changed: f.changed }">
                    <span class="sf-name">{{ f.name }}</span>
                    <span class="sf-val">{{ f.newVal }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </el-dialog>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, computed, reactive } from 'vue'
import { ElMessage } from 'element-plus'

const activeTab = ref('oplog')
const headerStyle = { background: '#0f2540', color: '#8bafc9', borderColor: '#1a3a5c' }
const cellStyle = { background: '#0d1b2e', color: '#c0d8f0', borderColor: '#1a3a5c' }

const dateShortcuts = [
  { text: '今天', value: () => { const d = new Date(); return [d, d] } },
  { text: '最近7天', value: () => { const d = new Date(); return [new Date(d.getTime() - 7 * 86400000), d] } },
  { text: '最近30天', value: () => { const d = new Date(); return [new Date(d.getTime() - 30 * 86400000), d] } },
]

// ========== 操作日志 ==========
const opFilter = reactive({ type: '', keyword: '', dateRange: null })
const opLogs = ref([
  { time: '2026-04-13 10:30:05', user: '管理员', role: 'admin', type: 'approve', typeLabel: '用户审批', detail: '审批通过用户 王小明 的企业用户申请', ip: '192.168.1.100', result: 'success' },
  { time: '2026-04-13 10:28:12', user: '管理员', role: 'admin', type: 'device', typeLabel: '设备操作', detail: '远程重启设备 实验楼3F空调机组 (AC-002)', ip: '192.168.1.100', result: 'success' },
  { time: '2026-04-13 10:15:00', user: '管理员', role: 'admin', type: 'role', typeLabel: '角色变更', detail: '将用户 李四 从 个人用户 升级为 企业用户', ip: '192.168.1.100', result: 'success' },
  { time: '2026-04-13 09:45:30', user: '管理员', role: 'admin', type: 'config', typeLabel: '配置变更', detail: '修改碳排放系数: 0.7035 → 0.6812 kgCO₂/kWh', ip: '192.168.1.100', result: 'success' },
  { time: '2026-04-13 09:30:00', user: '中中公司', role: 'enterprise', type: 'import', typeLabel: '数据导入', detail: '导入能耗数据 2026Q1_energy.xlsx (356条记录)', ip: '192.168.1.55', result: 'success' },
  { time: '2026-04-13 09:20:00', user: '管理员', role: 'admin', type: 'login', typeLabel: '用户登录', detail: '管理员登录系统', ip: '192.168.1.100', result: 'success' },
  { time: '2026-04-12 17:30:18', user: '张三', role: 'user', type: 'export', typeLabel: '数据导出', detail: '导出能耗分析Excel报表（行政楼2026年3月）', ip: '192.168.1.88', result: 'success' },
  { time: '2026-04-12 16:00:00', user: '管理员', role: 'admin', type: 'password', typeLabel: '密码重置', detail: '重置用户 赵六 的密码', ip: '192.168.1.100', result: 'success' },
  { time: '2026-04-12 14:20:15', user: '管理员', role: 'admin', type: 'approve', typeLabel: '用户审批', detail: '拒绝用户 未知用户 的申请，原因：信息不完整', ip: '192.168.1.100', result: 'success' },
  { time: '2026-04-12 11:00:00', user: '系统', role: 'system', type: 'config', typeLabel: '配置变更', detail: '自动备份任务执行完成，备份大小 15.8MB', ip: '127.0.0.1', result: 'success' },
  { time: '2026-04-11 23:00:00', user: '系统', role: 'system', type: 'config', typeLabel: '配置变更', detail: '定时任务：每日能耗数据汇总完成', ip: '127.0.0.1', result: 'success' },
  { time: '2026-04-11 15:10:00', user: '中中公司', role: 'enterprise', type: 'import', typeLabel: '数据导入', detail: '导入失败: research_office.xlsx 格式错误（第3列数据类型不匹配）', ip: '192.168.1.55', result: 'failed' },
])

const filteredOpLogs = computed(() => {
  return opLogs.value.filter(l => {
    const matchType = !opFilter.type || l.type === opFilter.type
    const matchKw = !opFilter.keyword || l.user.includes(opFilter.keyword) || l.detail.includes(opFilter.keyword)
    return matchType && matchKw
  })
})

function opTagType(type) {
  return { login: '', import: 'success', export: 'warning', config: 'info', approve: 'danger', device: '', role: 'warning', password: 'info' }[type] || ''
}
function loadOpLogs() { ElMessage.info('日志已刷新') }
function exportOpLogs() { ElMessage.success('操作日志导出中...') }

// ========== 登录审计 ==========
const loginFilter = ref('')
const loginSearch = ref('')
const loginStats = ref({ success: 156, failed: 12, locked: 1, uniqueIPs: 23, todayActive: 8 })

const loginLogs = ref([
  { time: '2026-04-13 10:20:00', phone: '13800000000', nickname: '系统管理员', role: 'admin', ip: '192.168.1.100', device: 'Chrome 125 / Windows 11', location: '本地', result: 'success', failReason: '' },
  { time: '2026-04-13 09:45:00', phone: '13811111111', nickname: '中中公司', role: 'enterprise', ip: '192.168.1.55', device: 'Chrome 125 / macOS', location: '本地', result: 'success', failReason: '' },
  { time: '2026-04-13 09:30:00', phone: '13812345678', nickname: '张三', role: 'user', ip: '192.168.1.88', device: 'Safari / iPhone 16', location: '本地', result: 'success', failReason: '' },
  { time: '2026-04-13 09:15:00', phone: '13899999999', nickname: '—', role: 'user', ip: '45.33.12.88', device: 'Chrome 124 / Linux', location: '美国', result: 'failed', failReason: '账号不存在' },
  { time: '2026-04-13 09:10:00', phone: '13811111111', nickname: '中中公司', role: 'enterprise', ip: '103.25.44.12', device: 'Firefox / Windows 10', location: '北京', result: 'failed', failReason: '密码错误(第2次)' },
  { time: '2026-04-13 08:55:00', phone: '13811111111', nickname: '中中公司', role: 'enterprise', ip: '103.25.44.12', device: 'Firefox / Windows 10', location: '北京', result: 'failed', failReason: '密码错误(第1次)' },
  { time: '2026-04-12 18:00:00', phone: '13800000000', nickname: '系统管理员', role: 'admin', ip: '192.168.1.100', device: 'Edge 125 / Windows 11', location: '本地', result: 'success', failReason: '' },
  { time: '2026-04-12 14:20:00', phone: '13866666666', nickname: '李四', role: 'user', ip: '192.168.1.92', device: 'Chrome 125 / Android', location: '本地', result: 'success', failReason: '' },
  { time: '2026-04-12 10:00:00', phone: '13877777777', nickname: '—', role: 'user', ip: '202.96.134.5', device: 'Chrome 123 / Windows', location: '上海', result: 'failed', failReason: '账号已禁用' },
])

const filteredLoginLogs = computed(() => {
  return loginLogs.value.filter(l => {
    const matchResult = !loginFilter.value || l.result === loginFilter.value
    const matchSearch = !loginSearch.value || l.phone.includes(loginSearch.value) || l.nickname.includes(loginSearch.value) || l.ip.includes(loginSearch.value)
    return matchResult && matchSearch
  })
})
function loadLoginLogs() { ElMessage.info('登录记录已刷新') }

// ========== 数据变更追溯 ==========
const changeFilter = reactive({ category: '', keyword: '' })
const snapshotVisible = ref(false)
const snapshotData = ref(null)

const changeLogs = ref([
  { time: '2026-04-13 09:45:30', operator: '管理员', category: 'config', target: '系统配置 - 碳排放系数', field: 'carbonFactor', oldValue: '0.7035', newValue: '0.6812' },
  { time: '2026-04-13 09:30:00', operator: '中中公司', category: 'energy', target: '行政楼 2026-03-15 能耗', field: '用电量', oldValue: '1250 kWh', newValue: '1185 kWh' },
  { time: '2026-04-12 16:00:00', operator: '管理员', category: 'user', target: '用户 赵六', field: '密码', oldValue: '******', newValue: '(已重置)' },
  { time: '2026-04-12 15:30:00', operator: '管理员', category: 'user', target: '用户 李四', field: '角色', oldValue: '个人用户', newValue: '企业用户' },
  { time: '2026-04-12 14:00:00', operator: '管理员', category: 'config', target: '系统配置 - 电价模板', field: '峰时电价', oldValue: '1.20 元/kWh', newValue: '1.35 元/kWh' },
  { time: '2026-04-11 11:00:00', operator: '管理员', category: 'device', target: '设备 AC-002 实验楼3F空调', field: '维保周期', oldValue: '120 天', newValue: '90 天' },
  { time: '2026-04-11 10:30:00', operator: '中中公司', category: 'energy', target: '实验楼 2026-03-20 能耗', field: '用水量', oldValue: '85 m³', newValue: '78 m³' },
  { time: '2026-04-10 09:00:00', operator: '管理员', category: 'enterprise', target: '企业 中中公司', field: '联系人', oldValue: '张经理', newValue: '王总监' },
])

const filteredChangeLogs = computed(() => {
  return changeLogs.value.filter(l => {
    const matchCat = !changeFilter.category || l.category === changeFilter.category
    const matchKw = !changeFilter.keyword || l.target.includes(changeFilter.keyword) || l.field.includes(changeFilter.keyword)
    return matchCat && matchKw
  })
})

function categoryLabel(c) {
  return { energy: '能耗数据', config: '系数配置', device: '设备参数', user: '用户信息', enterprise: '企业信息' }[c] || c
}

function loadChangeLogs() { ElMessage.info('变更记录已刷新') }

function viewSnapshot(row) {
  snapshotData.value = {
    ...row,
    fields: [
      { name: row.field, oldVal: row.oldValue, newVal: row.newValue, changed: true },
      { name: '修改时间', oldVal: '—', newVal: row.time, changed: false },
      { name: '操作人', oldVal: '—', newVal: row.operator, changed: false },
      { name: '备注', oldVal: '—', newVal: '通过管理面板修改', changed: false },
    ]
  }
  snapshotVisible.value = true
}
</script>

<style scoped>
.admin-audit { display: flex; flex-direction: column; gap: 16px; }
.page-title-bar { font-size: 18px; font-weight: 700; color: #e0f0ff; }
.ops-card { background: #0d1b2e; border-radius: 10px; padding: 18px; }
.card-title { color: #e0f0ff; font-weight: 600; font-size: 15px; margin-bottom: 12px; }
.filter-row { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
.table-footer { margin-top: 10px; color: #5a7a9a; font-size: 12px; }

.dark-tabs :deep(.el-tabs__item) { color: #5a7a9a; }
.dark-tabs :deep(.el-tabs__item.is-active) { color: #00d4ff; }
.dark-tabs :deep(.el-tabs__active-bar) { background: #00d4ff; }
.dark-tabs :deep(.el-tabs__nav-wrap::after) { background: #1a3a5c; }

/* 统计卡片 */
.status-summary { display: flex; gap: 14px; flex-wrap: wrap; }
.ss-card { background: #0d1b2e; border-radius: 10px; padding: 16px 24px; border-left: 3px solid #1a3a5c; min-width: 120px; text-align: center; }
.ss-num { font-size: 28px; font-weight: 700; }
.ss-label { color: #5a7a9a; font-size: 13px; margin-top: 4px; }

/* 变更值样式 */
.val-old { color: #ff6b6b; text-decoration: line-through; font-size: 12px; }
.val-new { color: #52c41a; font-weight: 600; font-size: 12px; }

/* 快照对比 */
.snapshot-compare { color: #c0d8f0; }
.snap-info { display: flex; gap: 20px; margin-bottom: 16px; padding-bottom: 12px; border-bottom: 1px solid #1a3a5c; color: #8bafc9; font-size: 13px; }
.snap-info b { color: #e0f0ff; }
.snap-grid { display: flex; gap: 12px; align-items: flex-start; }
.snap-col { flex: 1; }
.snap-col-title { font-weight: 700; font-size: 14px; margin-bottom: 10px; text-align: center; }
.snap-content { background: #0a1628; border-radius: 8px; padding: 12px; }
.snap-field { display: flex; justify-content: space-between; padding: 6px 8px; border-radius: 4px; margin-bottom: 4px; font-size: 13px; }
.snap-field.changed { background: rgba(0,212,255,0.08); border-left: 2px solid #00d4ff; }
.sf-name { color: #5a7a9a; }
.sf-val { color: #e0f0ff; font-weight: 500; }
.snap-arrow { font-size: 24px; color: #5a7a9a; padding-top: 40px; }
</style>
