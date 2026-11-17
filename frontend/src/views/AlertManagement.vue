<template>
  <div class="page-container">
    <!-- 顶部标题行 -->
    <div class="page-header">
      <h2 class="page-title">告警管理</h2>
      <div class="header-actions">
        <el-switch v-model="autoRefresh" active-text="自动刷新" inactive-text="" size="small" @change="toggleAutoRefresh" />
        <el-button size="small" :icon="Refresh" circle @click="refresh" />
      </div>
    </div>

    <!-- 流程示意图 -->
    <div class="flow-card">
      <div class="flow-title">告警 → 工单 联动流程</div>
      <div class="flow-diagram">
        <div class="flow-node fn-red">
          <div class="fn-icon">⚡</div>
          <div class="fn-label">能耗异常</div>
        </div>
        <div class="flow-arrow">►</div>
        <div class="flow-node fn-orange">
          <div class="fn-icon">🔔</div>
          <div class="fn-label">产生告警</div>
          <div class="fn-sub">活跃</div>
        </div>
        <div class="flow-arrow">►</div>
        <div class="flow-node fn-blue">
          <div class="fn-icon">📋</div>
          <div class="fn-label">自动工单</div>
          <div class="fn-sub">含AI建议</div>
        </div>
        <div class="flow-arrow">►</div>
        <div class="flow-node fn-yellow">
          <div class="fn-icon">🔧</div>
          <div class="fn-label">运维处理</div>
          <div class="fn-sub">已确认</div>
        </div>
        <div class="flow-arrow">►</div>
        <div class="flow-node fn-green">
          <div class="fn-icon">✅</div>
          <div class="fn-label">完成解决</div>
          <div class="fn-sub">已解决</div>
        </div>
      </div>
      <div class="flow-mapping">
        <span class="fm-title">级别映射：</span>
        <el-tag size="small" type="info" effect="plain">信息 → 低</el-tag>
        <el-tag size="small" type="warning" effect="plain">警告 → 中</el-tag>
        <el-tag size="small" type="danger" effect="plain">错误 → 高</el-tag>
        <el-tag size="small" color="#7c0a02" effect="dark" style="border:none">严重 → 紧急</el-tag>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-card" v-for="s in statCards" :key="s.key" :class="{ 'stat-active': filterStatus === s.filterVal }" @click="quickFilter(s.filterVal)">
        <div class="stat-icon" :style="{ background: s.bg, color: s.color }">{{ s.icon }}</div>
        <div class="stat-body">
          <div class="stat-value" :style="{ color: s.color }">{{ stats[s.key] || 0 }}</div>
          <div class="stat-label">{{ s.label }}</div>
        </div>
      </div>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-card">
      <div class="filter-row">
        <div class="filter-item">
          <label>告警级别</label>
          <el-select v-model="filterLevel" placeholder="全部" clearable style="width:180px" @change="loadAlerts">
            <el-option label="全部" value="" />
            <el-option label="信息" value="info" />
            <el-option label="警告" value="warning" />
            <el-option label="错误" value="error" />
            <el-option label="严重" value="critical" />
          </el-select>
        </div>
        <div class="filter-item">
          <label>状态</label>
          <el-select v-model="filterStatus" placeholder="全部" clearable style="width:180px" @change="loadAlerts">
            <el-option label="全部" value="" />
            <el-option label="活跃" value="active" />
            <el-option label="已确认" value="acknowledged" />
            <el-option label="已解决" value="resolved" />
          </el-select>
        </div>
        <el-button type="primary" :icon="Search" @click="loadAlerts">查询</el-button>
        <el-button type="danger" plain @click="showCreate = true">+ 新增告警</el-button>
        <el-button v-if="selectedRows.length > 0" type="warning" plain @click="batchAcknowledge">批量确认 ({{ selectedRows.length }})</el-button>
        <el-button v-if="selectedRows.length > 0" type="success" plain @click="batchResolve">批量解决 ({{ selectedRows.length }})</el-button>
      </div>
    </div>

    <!-- 表格 -->
    <el-table :data="pagedAlerts" v-loading="loading" stripe
      @selection-change="onSelectionChange" @row-click="openDetail"
      :header-cell-style="headerStyle" :cell-style="cellStyle" class="dark-table" row-class-name="clickable-row">
      <el-table-column type="selection" width="45" />
      <el-table-column prop="alertCode" label="告警编码" width="120">
        <template #default="{ row }">
          <span class="code-link">{{ row.alertCode }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="level" label="级别" width="90">
        <template #default="{ row }">
          <span class="level-dot" :class="'dot-' + row.level"></span>
          <el-tag :type="levelTagType(row.level)" size="small" effect="dark">{{ levelLabel(row.level) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="type" label="类型" width="120" show-overflow-tooltip />
      <el-table-column label="建筑 / 设备" min-width="150">
        <template #default="{ row }">
          <span>{{ row.buildingName || '-' }}</span>
          <span v-if="row.deviceName" style="color:#5a7a9a;font-size:12px"> / {{ row.deviceName }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="message" label="告警信息" min-width="220" show-overflow-tooltip />
      <el-table-column prop="triggeredAt" label="触发时间" width="165">
        <template #default="{ row }">
          <span style="font-size:12px">{{ row.triggeredAt }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)" size="small" round>{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right" align="center">
        <template #default="{ row }">
          <el-button v-if="row.status === 'active'" size="small" type="warning" plain @click.stop="doAcknowledge(row)">确认</el-button>
          <el-button v-if="row.status !== 'resolved'" size="small" type="success" plain @click.stop="doResolve(row)">解决</el-button>
          <el-button size="small" plain @click.stop="openDetail(row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination-bar" v-if="alerts.length > pageSize">
      <el-pagination background layout="total, prev, pager, next, sizes"
        :total="alerts.length" v-model:current-page="currentPage" v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50]" />
    </div>

    <!-- 详情抽屉 -->
    <el-drawer v-model="showDetail" title="告警详情" size="460px" :with-header="true" class="dark-drawer">
      <template v-if="detailRow">
        <div class="detail-section">
          <div class="detail-header">
            <el-tag :type="levelTagType(detailRow.level)" effect="dark" size="large">{{ levelLabel(detailRow.level) }}</el-tag>
            <el-tag :type="statusTagType(detailRow.status)" size="large" round>{{ statusLabel(detailRow.status) }}</el-tag>
          </div>
          <div class="detail-code">{{ detailRow.alertCode }}</div>
        </div>

        <div class="detail-section">
          <div class="detail-label">告警类型</div>
          <div class="detail-value">{{ detailRow.type || '-' }}</div>
        </div>
        <div class="detail-section">
          <div class="detail-label">建筑 / 设备</div>
          <div class="detail-value">{{ detailRow.buildingName || '-' }}{{ detailRow.deviceName ? ' / ' + detailRow.deviceName : '' }}</div>
        </div>
        <div class="detail-section">
          <div class="detail-label">告警信息</div>
          <div class="detail-value msg-box">{{ detailRow.message || '-' }}</div>
        </div>

        <div class="detail-section">
          <div class="detail-label">状态时间线</div>
          <div class="timeline">
            <div class="tl-item tl-done">
              <div class="tl-dot"></div>
              <div class="tl-content">
                <div class="tl-title">触发告警</div>
                <div class="tl-time">{{ detailRow.triggeredAt || detailRow.createdAt }}</div>
              </div>
            </div>
            <div class="tl-item" :class="{ 'tl-done': detailRow.status !== 'active' }">
              <div class="tl-dot"></div>
              <div class="tl-content">
                <div class="tl-title">确认告警 → 工单生成</div>
                <div class="tl-time" v-if="detailRow.status !== 'active'">已确认</div>
                <div class="tl-time tl-pending" v-else>等待确认</div>
              </div>
            </div>
            <div class="tl-item" :class="{ 'tl-done': detailRow.status === 'resolved' }">
              <div class="tl-dot"></div>
              <div class="tl-content">
                <div class="tl-title">解决告警</div>
                <div class="tl-time" v-if="detailRow.resolvedAt">{{ detailRow.resolvedAt }}</div>
                <div class="tl-time tl-pending" v-else>待解决</div>
              </div>
            </div>
          </div>
        </div>

        <div class="detail-actions">
          <el-button v-if="detailRow.status === 'active'" type="warning" @click="doAcknowledge(detailRow); showDetail = false">确认告警</el-button>
          <el-button v-if="detailRow.status !== 'resolved'" type="success" @click="doResolve(detailRow); showDetail = false">解决告警</el-button>
          <el-button @click="showDetail = false">关闭</el-button>
        </div>
      </template>
    </el-drawer>

    <!-- 手动新增告警 -->
    <el-dialog v-model="showCreate" title="新增告警" width="520px" class="dark-dialog" destroy-on-close>
      <div class="create-tip">
        <el-icon><InfoFilled /></el-icon>
        <span>创建告警后将自动生成对应优先级的工单，并调用AI生成运维建议。</span>
      </div>
      <el-form :model="createForm" label-width="80px" :rules="createRules" ref="createFormRef">
        <el-form-item label="级别" prop="level">
          <el-radio-group v-model="createForm.level">
            <el-radio-button value="info">信息</el-radio-button>
            <el-radio-button value="warning">警告</el-radio-button>
            <el-radio-button value="error">错误</el-radio-button>
            <el-radio-button value="critical">严重</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="createForm.type" placeholder="选择或输入类型" filterable allow-create style="width:100%">
            <el-option label="能耗异常" value="能耗异常" />
            <el-option label="设备故障" value="设备故障" />
            <el-option label="温度异常" value="温度异常" />
            <el-option label="电力过载" value="电力过载" />
            <el-option label="漏水检测" value="漏水检测" />
            <el-option label="空调异常" value="空调异常" />
          </el-select>
        </el-form-item>
        <el-form-item label="建筑ID" prop="buildingId">
          <el-input-number v-model="createForm.buildingId" :min="1" :max="99" style="width:100%" />
        </el-form-item>
        <el-form-item label="设备ID">
          <el-input-number v-model="createForm.deviceId" :min="0" placeholder="可选" style="width:100%" />
        </el-form-item>
        <el-form-item label="告警信息" prop="message">
          <el-input v-model="createForm.message" type="textarea" :rows="3" placeholder="详细描述告警内容，越详细AI建议越准确" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreate = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="doCreate">
          创建告警 & 生成工单
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, InfoFilled } from '@element-plus/icons-vue'
import { alertApi } from '../api/index.js'

const alerts = ref([])
const loading = ref(false)
const filterLevel = ref('')
const filterStatus = ref('')
const stats = ref({})
const showCreate = ref(false)
const creating = ref(false)
const showDetail = ref(false)
const detailRow = ref(null)
const selectedRows = ref([])
const autoRefresh = ref(false)
let refreshTimer = null
const createFormRef = ref(null)

const currentPage = ref(1)
const pageSize = ref(10)
const pagedAlerts = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return alerts.value.slice(start, start + pageSize.value)
})

const createForm = reactive({
  level: 'warning',
  type: '',
  buildingId: 1,
  deviceId: null,
  message: ''
})
const createRules = {
  level: [{ required: true, message: '请选择级别', trigger: 'change' }],
  type: [{ required: true, message: '请选择或输入类型', trigger: 'change' }],
  buildingId: [{ required: true, message: '请输入建筑ID', trigger: 'blur' }],
  message: [{ required: true, message: '请填写告警信息', trigger: 'blur' }]
}

const statCards = [
  { key: 'total', label: '总告警', icon: '🔔', color: '#00d4ff', bg: '#00d4ff18', filterVal: '' },
  { key: 'active', label: '活跃', icon: '🔴', color: '#ff4d4f', bg: '#ff4d4f18', filterVal: 'active' },
  { key: 'acknowledged', label: '已确认', icon: '🟡', color: '#faad14', bg: '#faad1418', filterVal: 'acknowledged' },
  { key: 'resolved', label: '已解决', icon: '🟢', color: '#52c41a', bg: '#52c41a18', filterVal: 'resolved' }
]

const headerStyle = () => ({ background: '#0f2540', color: '#8bafc9', borderBottom: '1px solid #1a3a5c' })
const cellStyle = () => ({ background: '#0d1b2e', color: '#c0d8f0', borderBottom: '1px solid #1a3a5c' })

function levelLabel(l) {
  return { info: '信息', warning: '警告', error: '错误', critical: '严重' }[l] || l
}
function levelTagType(l) {
  return { info: 'info', warning: 'warning', error: 'danger', critical: 'danger' }[l] || 'info'
}
function statusLabel(s) {
  return { active: '活跃', acknowledged: '已确认', resolved: '已解决' }[s] || s
}
function statusTagType(s) {
  return { active: 'danger', acknowledged: 'warning', resolved: 'success' }[s] || 'info'
}

function quickFilter(val) {
  filterStatus.value = val
  currentPage.value = 1
  loadAlerts()
}

function onSelectionChange(rows) {
  selectedRows.value = rows
}

function openDetail(row) {
  detailRow.value = row
  showDetail.value = true
}

async function loadAlerts() {
  loading.value = true
  try {
    const params = {}
    if (filterLevel.value) params.level = filterLevel.value
    if (filterStatus.value) params.status = filterStatus.value
    const res = await alertApi.list(params)
    if (res.code === 200) alerts.value = res.data || []
  } catch { /* interceptor */ } finally { loading.value = false }
}

async function loadStats() {
  try {
    const res = await alertApi.stats()
    if (res.code === 200) stats.value = res.data
  } catch {}
}

function refresh() {
  loadAlerts(); loadStats()
}

function toggleAutoRefresh(val) {
  clearInterval(refreshTimer)
  if (val) {
    refreshTimer = setInterval(refresh, 15000)
    ElMessage.success('已开启自动刷新（15秒）')
  }
}

async function doAcknowledge(row) {
  try {
    await alertApi.acknowledge(row.id)
    ElMessage.success('告警已确认')
    refresh()
  } catch { ElMessage.error('操作失败') }
}

async function doResolve(row) {
  try {
    await ElMessageBox.confirm('确认解决此告警？关联工单将同时完成。', '解决告警', { type: 'warning', confirmButtonText: '确认解决', cancelButtonText: '取消' })
    await alertApi.resolve(row.id)
    ElMessage.success('告警已解决，关联工单已完成')
    refresh()
  } catch {}
}

async function batchAcknowledge() {
  const active = selectedRows.value.filter(r => r.status === 'active')
  if (active.length === 0) { ElMessage.warning('选中的告警中没有活跃状态的'); return }
  try {
    await ElMessageBox.confirm(`确认批量确认 ${active.length} 条告警？`, '批量确认', { type: 'warning' })
    for (const row of active) { await alertApi.acknowledge(row.id) }
    ElMessage.success(`已确认 ${active.length} 条告警`)
    refresh()
  } catch {}
}

async function batchResolve() {
  const pending = selectedRows.value.filter(r => r.status !== 'resolved')
  if (pending.length === 0) { ElMessage.warning('选中的告警都已解决'); return }
  try {
    await ElMessageBox.confirm(`确认批量解决 ${pending.length} 条告警？`, '批量解决', { type: 'warning' })
    for (const row of pending) { await alertApi.resolve(row.id) }
    ElMessage.success(`已解决 ${pending.length} 条告警`)
    refresh()
  } catch {}
}

async function doCreate() {
  if (!createFormRef.value) return
  await createFormRef.value.validate(async (valid) => {
    if (!valid) return
    creating.value = true
    try {
      const res = await alertApi.create(createForm)
      if (res.code === 200) {
        ElMessage.success('告警已创建，工单已自动生成（含AI建议）')
        showCreate.value = false
        createForm.type = ''; createForm.message = ''; createForm.deviceId = null
        refresh()
      }
    } catch { ElMessage.error('创建失败') } finally { creating.value = false }
  })
}

onMounted(() => { refresh() })
onUnmounted(() => { clearInterval(refreshTimer) })
</script>

<style scoped>
.page-container { color: #e0f0ff; }

.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.page-title { font-size: 20px; font-weight: 700; color: #e0f0ff; margin: 0; }
.header-actions { display: flex; align-items: center; gap: 12px; }

/* ── 流程图 ── */
.flow-card {
  background: rgba(13,27,46,0.8); border: 1px solid #1a3a5c;
  border-radius: 14px; padding: 20px 24px; margin-bottom: 18px;
}
.flow-title { font-size: 13px; font-weight: 600; color: #8bafc9; margin-bottom: 14px; }
.flow-diagram { display: flex; align-items: center; gap: 6px; justify-content: center; flex-wrap: wrap; margin-bottom: 14px; }
.flow-node {
  text-align: center; padding: 10px 16px; border-radius: 12px; min-width: 90px;
  border: 1.5px solid transparent; transition: transform 0.2s;
}
.flow-node:hover { transform: translateY(-2px); }
.fn-icon { font-size: 22px; margin-bottom: 4px; }
.fn-label { font-size: 13px; font-weight: 600; color: #e0f0ff; }
.fn-sub { font-size: 10px; color: #8bafc9; margin-top: 2px; }
.fn-red { background: #ff4d4f12; border-color: #ff4d4f44; }
.fn-orange { background: #fa8c1612; border-color: #fa8c1644; }
.fn-blue { background: #00d4ff12; border-color: #00d4ff44; }
.fn-yellow { background: #faad1412; border-color: #faad1444; }
.fn-green { background: #52c41a12; border-color: #52c41a44; }
.flow-arrow { color: #3a5a7a; font-size: 16px; margin: 0 2px; }
.flow-mapping { display: flex; align-items: center; gap: 8px; justify-content: center; flex-wrap: wrap; }
.fm-title { font-size: 12px; color: #5a7a9a; }

/* ── 统计卡片 ── */
.stats-row {
  display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; margin-bottom: 18px;
}
.stat-card {
  display: flex; align-items: center; gap: 12px; cursor: pointer;
  background: rgba(13,27,46,0.8); border: 1.5px solid #1a3a5c;
  border-radius: 12px; padding: 16px 18px; transition: all 0.25s;
}
.stat-card:hover { border-color: #00d4ff88; }
.stat-active { border-color: #00d4ff !important; box-shadow: 0 0 12px #00d4ff22; }
.stat-icon {
  width: 44px; height: 44px; border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  font-size: 20px; flex-shrink: 0;
}
.stat-value { font-size: 24px; font-weight: 700; }
.stat-label { font-size: 12px; color: #5a7a9a; margin-top: 2px; }

/* ── 筛选栏 ── */
.filter-card {
  background: rgba(13,27,46,0.8); border: 1px solid #1a3a5c;
  border-radius: 12px; padding: 18px 20px; margin-bottom: 16px;
}
.filter-row { display: flex; align-items: flex-end; gap: 14px; flex-wrap: wrap; }
.filter-item label { display: block; font-size: 12px; color: #8bafc9; margin-bottom: 6px; }

/* ── 表格 ── */
.dark-table :deep(.el-table) { background: #0a1628 !important; color: #e0f0ff !important; }
.dark-table :deep(.el-table th) { background: #0f2540 !important; color: #8bafc9 !important; border-bottom: 1px solid #1a3a5c !important; }
.dark-table :deep(.el-table tr) { background: #0a1628 !important; color: #e0f0ff !important; }
.dark-table :deep(.el-table td) { border-bottom: 1px solid #1a3a5c !important; color: #e0f0ff !important; }
.dark-table :deep(.el-table--striped .el-table__body tr.el-table__row--striped td) { background: #0f1f35 !important; }
.dark-table :deep(.el-table__body tr:hover > td) { background: #142a45 !important; }
.dark-table :deep(.el-table__empty-text) { color: #5a7a9a; }
.dark-table :deep(.el-checkbox__inner) { background: transparent; border-color: #3a5a7a; }

.code-link { color: #00d4ff; cursor: pointer; font-size: 12px; font-family: monospace; }
.code-link:hover { text-decoration: underline; }

.level-dot { display: inline-block; width: 8px; height: 8px; border-radius: 50%; margin-right: 6px; vertical-align: middle; }
.dot-info { background: #909399; }
.dot-warning { background: #e6a23c; animation: pulse 2s infinite; }
.dot-error { background: #f56c6c; animation: pulse 1.5s infinite; }
.dot-critical { background: #ff4d4f; animation: pulse 1s infinite; }
@keyframes pulse { 0%,100% { opacity: 1; } 50% { opacity: 0.4; } }

.clickable-row { cursor: pointer; }

/* ── 分页 ── */
.pagination-bar { display: flex; justify-content: flex-end; margin-top: 16px; }

/* ── 详情抽屉 ── */
.detail-section { margin-bottom: 20px; }
.detail-header { display: flex; gap: 8px; align-items: center; margin-bottom: 8px; }
.detail-code { font-size: 22px; font-weight: 700; color: #e0f0ff; font-family: monospace; }
.detail-label { font-size: 12px; color: #5a7a9a; margin-bottom: 4px; }
.detail-value { font-size: 14px; color: #c0d8f0; }
.msg-box {
  background: rgba(0,212,255,0.06); border: 1px solid #1a3a5c;
  border-radius: 8px; padding: 10px 12px; line-height: 1.7; font-size: 13px;
}

/* ── 时间线 ── */
.timeline { padding-left: 12px; }
.tl-item { display: flex; gap: 12px; padding-bottom: 18px; position: relative; }
.tl-item:not(:last-child)::after {
  content: ''; position: absolute; left: 5px; top: 14px; bottom: 0;
  width: 2px; background: #1a3a5c;
}
.tl-done:not(:last-child)::after { background: #52c41a66; }
.tl-dot {
  width: 12px; height: 12px; border-radius: 50%; flex-shrink: 0; margin-top: 3px;
  border: 2px solid #3a5a7a; background: #0d1b2e;
}
.tl-done .tl-dot { border-color: #52c41a; background: #52c41a33; }
.tl-title { font-size: 13px; color: #e0f0ff; font-weight: 500; }
.tl-time { font-size: 11px; color: #5a7a9a; margin-top: 2px; }
.tl-pending { color: #faad14; }

.detail-actions { display: flex; gap: 8px; padding-top: 10px; border-top: 1px solid #1a3a5c; }

/* ── 新增弹窗 ── */
.create-tip {
  display: flex; align-items: center; gap: 8px;
  background: rgba(0,212,255,0.08); border: 1px solid rgba(0,212,255,0.2);
  border-radius: 8px; padding: 10px 14px; margin-bottom: 18px;
  font-size: 12px; color: #8bafc9;
}
.create-tip .el-icon { color: #00d4ff; font-size: 16px; }
</style>
