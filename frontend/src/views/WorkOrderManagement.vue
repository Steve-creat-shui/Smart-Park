<template>
  <div class="page-container">
    <!-- 顶部标题行 -->
    <div class="page-header">
      <h2 class="page-title">工单管理</h2>
      <div class="header-actions">
        <el-switch v-model="autoRefresh" active-text="自动刷新" inactive-text="" size="small" @change="toggleAutoRefresh" />
        <el-button size="small" :icon="Refresh" circle @click="refresh" />
      </div>
    </div>

    <!-- 流程示意图 -->
    <div class="flow-card">
      <div class="flow-title">工单生命周期</div>
      <div class="flow-diagram">
        <div class="flow-node fn-blue">
          <div class="fn-icon">🔔</div>
          <div class="fn-label">告警触发</div>
          <div class="fn-sub">自动创建</div>
        </div>
        <div class="flow-arrow">►</div>
        <div class="flow-node fn-red">
          <div class="fn-icon">📋</div>
          <div class="fn-label">待处理</div>
          <div class="fn-sub">AI生成建议</div>
        </div>
        <div class="flow-arrow">►</div>
        <div class="flow-node fn-yellow">
          <div class="fn-icon">🔧</div>
          <div class="fn-label">处理中</div>
          <div class="fn-sub">运维执行</div>
        </div>
        <div class="flow-arrow">►</div>
        <div class="flow-node fn-green">
          <div class="fn-icon">✅</div>
          <div class="fn-label">已完成</div>
          <div class="fn-sub">告警同步解决</div>
        </div>
      </div>
      <div class="flow-mapping">
        <span class="fm-title">优先级映射：</span>
        <el-tag size="small" type="info" effect="plain">信息告警 → 低优先级</el-tag>
        <el-tag size="small" type="warning" effect="plain">警告告警 → 中优先级</el-tag>
        <el-tag size="small" type="danger" effect="plain">错误告警 → 高优先级</el-tag>
        <el-tag size="small" color="#7c0a02" effect="dark" style="border:none">严重告警 → 紧急</el-tag>
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
          <label>工单状态</label>
          <el-select v-model="filterStatus" placeholder="全部" clearable style="width:180px" @change="loadOrders">
            <el-option label="全部" value="" />
            <el-option label="待处理" value="pending" />
            <el-option label="处理中" value="processing" />
            <el-option label="已完成" value="completed" />
            <el-option label="已取消" value="cancelled" />
          </el-select>
        </div>
        <div class="filter-item">
          <label>优先级</label>
          <el-select v-model="filterPriority" placeholder="全部" clearable style="width:180px" @change="loadOrders">
            <el-option label="全部" value="" />
            <el-option label="低" value="low" />
            <el-option label="中" value="medium" />
            <el-option label="高" value="high" />
            <el-option label="紧急" value="critical" />
          </el-select>
        </div>
        <el-button type="primary" :icon="Search" @click="loadOrders">查询</el-button>
      </div>
    </div>

    <!-- 表格 -->
    <el-table :data="pagedOrders" v-loading="loading" stripe @row-click="openDetail"
      :header-cell-style="headerStyle" :cell-style="cellStyle" class="dark-table" row-class-name="clickable-row">
      <el-table-column prop="orderCode" label="工单编号" width="120">
        <template #default="{ row }">
          <span class="code-link">{{ row.orderCode }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="priority" label="优先级" width="90">
        <template #default="{ row }">
          <span class="priority-dot" :class="'pdot-' + row.priority"></span>
          <el-tag :type="priorityTagType(row.priority)" size="small" effect="dark">{{ priorityLabel(row.priority) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="alertCode" label="关联告警" width="120">
        <template #default="{ row }">
          <span v-if="row.alertCode" class="alert-link">{{ row.alertCode }}</span>
          <span v-else style="color:#5a7a9a">-</span>
        </template>
      </el-table-column>
      <el-table-column label="建筑 / 设备" min-width="140">
        <template #default="{ row }">
          <span>{{ row.buildingName || '-' }}</span>
          <span v-if="row.deviceName" style="color:#5a7a9a;font-size:12px"> / {{ row.deviceName }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="faultDescription" label="故障描述" min-width="200" show-overflow-tooltip />
      <el-table-column label="AI建议" min-width="180">
        <template #default="{ row }">
          <div v-if="row.aiSuggestion" class="ai-cell" @click.stop="openDetail(row)">
            <span class="ai-badge">AI</span>
            <span class="ai-text">{{ truncate(row.aiSuggestion, 35) }}</span>
          </div>
          <span v-else style="color:#5a7a9a;font-size:12px">暂无建议</span>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="160">
        <template #default="{ row }">
          <span style="font-size:12px">{{ row.createdAt }}</span>
        </template>
      </el-table-column>
      <el-table-column label="负责人" width="120" align="center">
        <template #default="{ row }">
          <span v-if="row.assignedName" class="assignee-name">{{ row.assignedName }}</span>
          <el-button v-else-if="row.status !== 'completed' && row.status !== 'cancelled'" size="small" type="primary" link @click.stop="openAssign(row)">指派</el-button>
          <span v-else style="color:#5a7a9a">-</span>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)" size="small" round>{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right" align="center">
        <template #default="{ row }">
          <el-button v-if="row.status !== 'completed' && row.status !== 'cancelled' && !row.assignedName" size="small" type="info" plain @click.stop="openAssign(row)">指派</el-button>
          <el-button v-if="row.status === 'pending'" size="small" type="primary" plain @click.stop="doStart(row)">开始处理</el-button>
          <el-button v-if="row.status === 'processing'" size="small" type="success" plain @click.stop="openComplete(row)">完成</el-button>
          <el-button size="small" plain @click.stop="openDetail(row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination-bar" v-if="orders.length > pageSize">
      <el-pagination background layout="total, prev, pager, next, sizes"
        :total="orders.length" v-model:current-page="currentPage" v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50]" />
    </div>

    <!-- 详情抽屉 -->
    <el-drawer v-model="showDetail" title="工单详情" size="500px" :with-header="true" class="dark-drawer">
      <template v-if="detailRow">
        <!-- 头部 -->
        <div class="detail-section">
          <div class="detail-header">
            <el-tag :type="priorityTagType(detailRow.priority)" effect="dark" size="large">{{ priorityLabel(detailRow.priority) }}</el-tag>
            <el-tag :type="statusTagType(detailRow.status)" size="large" round>{{ statusLabel(detailRow.status) }}</el-tag>
          </div>
          <div class="detail-code">{{ detailRow.orderCode }}</div>
          <div v-if="detailRow.alertCode" class="detail-alert-link">
            关联告警：<span class="alert-link">{{ detailRow.alertCode }}</span>
          </div>
        </div>

        <!-- 基本信息 -->
        <div class="detail-grid">
          <div class="dg-item">
            <div class="dg-label">建筑</div>
            <div class="dg-value">{{ detailRow.buildingName || '-' }}</div>
          </div>
          <div class="dg-item">
            <div class="dg-label">设备</div>
            <div class="dg-value">{{ detailRow.deviceName || '-' }}</div>
          </div>
          <div class="dg-item">
            <div class="dg-label">优先级</div>
            <div class="dg-value">{{ priorityLabel(detailRow.priority) }}</div>
          </div>
          <div class="dg-item">
            <div class="dg-label">创建时间</div>
            <div class="dg-value">{{ detailRow.createdAt }}</div>
          </div>
          <div class="dg-item" style="grid-column: span 2">
            <div class="dg-label">负责人</div>
            <div class="dg-value">
              <span v-if="detailRow.assignedName">{{ detailRow.assignedName }}</span>
              <el-button v-else-if="detailRow.status !== 'completed'" size="small" type="primary" link @click="openAssign(detailRow); showDetail = false">点击指派</el-button>
              <span v-else style="color:#5a7a9a">未指派</span>
            </div>
          </div>
        </div>

        <!-- 故障描述 -->
        <div class="detail-section">
          <div class="detail-label">故障描述</div>
          <div class="detail-value msg-box">{{ detailRow.faultDescription || '-' }}</div>
        </div>

        <!-- AI建议 -->
        <div class="detail-section" v-if="detailRow.aiSuggestion">
          <div class="detail-label">AI 运维建议</div>
          <div class="ai-suggestion-box">
            <div class="ai-suggestion-header">
              <span class="ai-badge-lg">AI</span>
              <span class="ai-suggestion-title">智能分析建议</span>
            </div>
            <div class="ai-suggestion-content">{{ detailRow.aiSuggestion }}</div>
          </div>
        </div>

        <!-- 处理结果 -->
        <div class="detail-section" v-if="detailRow.resolutionNotes">
          <div class="detail-label">处理结果</div>
          <div class="detail-value msg-box" style="border-color:#52c41a44">{{ detailRow.resolutionNotes }}</div>
        </div>

        <!-- 时间线 -->
        <div class="detail-section">
          <div class="detail-label">状态时间线</div>
          <div class="timeline">
            <div class="tl-item tl-done">
              <div class="tl-dot"></div>
              <div class="tl-content">
                <div class="tl-title">创建工单</div>
                <div class="tl-desc">由告警自动生成，AI分析运维建议</div>
                <div class="tl-time">{{ detailRow.createdAt }}</div>
              </div>
            </div>
            <div class="tl-item" :class="{ 'tl-done': detailRow.status === 'processing' || detailRow.status === 'completed' }">
              <div class="tl-dot"></div>
              <div class="tl-content">
                <div class="tl-title">开始处理</div>
                <div class="tl-desc">运维人员接单，关联告警变为已确认</div>
                <div class="tl-time" v-if="detailRow.startedAt">{{ detailRow.startedAt }}</div>
                <div class="tl-time tl-pending" v-else>等待处理</div>
              </div>
            </div>
            <div class="tl-item" :class="{ 'tl-done': detailRow.status === 'completed' }">
              <div class="tl-dot"></div>
              <div class="tl-content">
                <div class="tl-title">完成工单</div>
                <div class="tl-desc">填写处理结果，关联告警同步解决</div>
                <div class="tl-time" v-if="detailRow.completedAt">{{ detailRow.completedAt }}</div>
                <div class="tl-time tl-pending" v-else>待完成</div>
              </div>
            </div>
          </div>
        </div>

        <!-- 操作按钮 -->
        <div class="detail-actions">
          <el-button v-if="detailRow.status === 'pending'" type="primary" @click="doStart(detailRow); showDetail = false">开始处理</el-button>
          <el-button v-if="detailRow.status === 'processing'" type="success" @click="openComplete(detailRow); showDetail = false">完成工单</el-button>
          <el-button @click="showDetail = false">关闭</el-button>
        </div>
      </template>
    </el-drawer>

    <!-- 指派人员弹窗 -->
    <el-dialog v-model="showAssign" title="指派运维人员" width="560px" class="dark-dialog" destroy-on-close>
      <div class="assign-order-info">
        工单：<strong style="color:#e0f0ff">{{ assignOrder?.orderCode }}</strong>
        <el-tag :type="priorityTagType(assignOrder?.priority)" size="small" effect="dark" style="margin-left:8px">{{ priorityLabel(assignOrder?.priority) }}</el-tag>
      </div>
      <div class="assign-search">
        <el-input v-model="userSearch" placeholder="搜索姓名/部门/手机号" clearable prefix-icon="Search" />
      </div>
      <div class="user-list" v-loading="loadingUsers">
        <div class="user-item" v-for="u in filteredUsers" :key="u.id"
          :class="{ 'user-selected': selectedUserId === u.id }" @click="selectedUserId = u.id">
          <div class="user-avatar">{{ (u.realName || u.nickname || '?')[0] }}</div>
          <div class="user-info">
            <div class="user-name">{{ u.realName || u.nickname || '未命名' }}</div>
            <div class="user-meta">
              <span v-if="u.department">{{ u.department }}</span>
              <span v-if="u.position"> · {{ u.position }}</span>
              <span v-if="u.phone"> · {{ u.phone }}</span>
            </div>
          </div>
          <div class="user-check" v-if="selectedUserId === u.id">✓</div>
        </div>
        <div v-if="filteredUsers.length === 0" style="text-align:center;color:#5a7a9a;padding:30px 0">
          没有找到可指派人员
        </div>
      </div>
      <template #footer>
        <el-button @click="showAssign = false">取消</el-button>
        <el-button type="primary" :disabled="!selectedUserId" :loading="assigning" @click="doAssign">确认指派</el-button>
      </template>
    </el-dialog>

    <!-- 完成工单弹窗 -->
    <el-dialog v-model="showComplete" title="完成工单" width="520px" class="dark-dialog" destroy-on-close>
      <div class="complete-header">
        <div class="complete-info">
          <span style="color:#8bafc9">工单：</span>
          <strong style="color:#e0f0ff">{{ currentOrder?.orderCode }}</strong>
          <el-tag :type="priorityTagType(currentOrder?.priority)" size="small" effect="dark" style="margin-left:8px">{{ priorityLabel(currentOrder?.priority) }}</el-tag>
        </div>
        <div v-if="currentOrder?.faultDescription" class="complete-desc">
          {{ currentOrder.faultDescription }}
        </div>
      </div>
      <div v-if="currentOrder?.aiSuggestion" class="ai-suggestion-box" style="margin-bottom:16px">
        <div class="ai-suggestion-header">
          <span class="ai-badge-lg">AI</span>
          <span class="ai-suggestion-title">参考AI建议填写处理结果</span>
        </div>
        <div class="ai-suggestion-content">{{ currentOrder.aiSuggestion }}</div>
      </div>
      <el-input v-model="resolutionNotes" type="textarea" :rows="4" placeholder="请详细描述处理过程和结果..." maxlength="1000" show-word-limit />
      <template #footer>
        <el-button @click="showComplete = false">取消</el-button>
        <el-button type="success" :loading="completing" @click="doComplete">
          确认完成（告警将同步解决）
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'
import { workOrderApi } from '../api/index.js'

const showAssign = ref(false)
const assignOrder = ref(null)
const userSearch = ref('')
const selectedUserId = ref(null)
const assigning = ref(false)
const loadingUsers = ref(false)
const assignableUsers = ref([])

const orders = ref([])
const loading = ref(false)
const filterStatus = ref('')
const filterPriority = ref('')
const stats = ref({})
const showComplete = ref(false)
const completing = ref(false)
const currentOrder = ref(null)
const resolutionNotes = ref('')
const showDetail = ref(false)
const detailRow = ref(null)
const autoRefresh = ref(false)
let refreshTimer = null

const currentPage = ref(1)
const pageSize = ref(10)
const pagedOrders = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return orders.value.slice(start, start + pageSize.value)
})

const statCards = [
  { key: 'total', label: '总工单', icon: '📋', color: '#00d4ff', bg: '#00d4ff18', filterVal: '' },
  { key: 'pending', label: '待处理', icon: '⏳', color: '#ff4d4f', bg: '#ff4d4f18', filterVal: 'pending' },
  { key: 'processing', label: '处理中', icon: '🔧', color: '#faad14', bg: '#faad1418', filterVal: 'processing' },
  { key: 'completed', label: '已完成', icon: '✅', color: '#52c41a', bg: '#52c41a18', filterVal: 'completed' }
]

const headerStyle = () => ({ background: '#0f2540', color: '#8bafc9', borderBottom: '1px solid #1a3a5c' })
const cellStyle = () => ({ background: '#0d1b2e', color: '#c0d8f0', borderBottom: '1px solid #1a3a5c' })

function priorityLabel(p) {
  return { low: '低', medium: '中', high: '高', critical: '紧急' }[p] || p
}
function priorityTagType(p) {
  return { low: 'info', medium: 'warning', high: 'danger', critical: 'danger' }[p] || 'info'
}
function statusLabel(s) {
  return { pending: '待处理', processing: '处理中', completed: '已完成', cancelled: '已取消' }[s] || s
}
function statusTagType(s) {
  return { pending: 'danger', processing: 'warning', completed: 'success', cancelled: 'info' }[s] || 'info'
}
function truncate(str, len) {
  return str && str.length > len ? str.slice(0, len) + '...' : str
}

function quickFilter(val) {
  filterStatus.value = val
  currentPage.value = 1
  loadOrders()
}

function openDetail(row) {
  detailRow.value = row
  showDetail.value = true
}

async function loadAssignableUsers() {
  loadingUsers.value = true
  try {
    const res = await workOrderApi.assignableUsers()
    if (res.code === 200) assignableUsers.value = res.data || []
  } catch {} finally { loadingUsers.value = false }
}

const filteredUsers = computed(() => {
  if (!userSearch.value) return assignableUsers.value
  const kw = userSearch.value.toLowerCase()
  return assignableUsers.value.filter(u =>
    (u.realName && u.realName.toLowerCase().includes(kw)) ||
    (u.nickname && u.nickname.toLowerCase().includes(kw)) ||
    (u.department && u.department.toLowerCase().includes(kw)) ||
    (u.phone && u.phone.includes(kw))
  )
})

function openAssign(row) {
  assignOrder.value = row
  selectedUserId.value = row.assignedTo || null
  userSearch.value = ''
  showAssign.value = true
  if (assignableUsers.value.length === 0) loadAssignableUsers()
}

async function doAssign() {
  if (!selectedUserId.value) return
  assigning.value = true
  try {
    await workOrderApi.assign(assignOrder.value.id, selectedUserId.value)
    ElMessage.success('工单已指派')
    showAssign.value = false
    refresh()
  } catch { ElMessage.error('指派失败') } finally { assigning.value = false }
}

async function loadOrders() {
  loading.value = true
  try {
    const params = {}
    if (filterStatus.value) params.status = filterStatus.value
    if (filterPriority.value) params.priority = filterPriority.value
    const res = await workOrderApi.list(params)
    if (res.code === 200) orders.value = res.data || []
  } catch { /* interceptor */ } finally { loading.value = false }
}

async function loadStats() {
  try {
    const res = await workOrderApi.stats()
    if (res.code === 200) stats.value = res.data
  } catch {}
}

function refresh() {
  loadOrders(); loadStats()
}

function toggleAutoRefresh(val) {
  clearInterval(refreshTimer)
  if (val) {
    refreshTimer = setInterval(refresh, 15000)
    ElMessage.success('已开启自动刷新（15秒）')
  }
}

async function doStart(row) {
  try {
    await ElMessageBox.confirm(
      '开始处理此工单？关联告警将变为"已确认"状态。',
      '开始处理',
      { type: 'info', confirmButtonText: '确认接单', cancelButtonText: '取消' }
    )
    await workOrderApi.start(row.id)
    ElMessage.success('工单已开始处理，关联告警已确认')
    refresh()
  } catch {}
}

function openComplete(row) {
  currentOrder.value = row
  resolutionNotes.value = ''
  showComplete.value = true
}

async function doComplete() {
  if (!resolutionNotes.value.trim()) { ElMessage.warning('请填写处理结果'); return }
  completing.value = true
  try {
    await workOrderApi.complete(currentOrder.value.id, resolutionNotes.value)
    ElMessage.success('工单已完成，关联告警已同步解决')
    showComplete.value = false
    refresh()
  } catch { ElMessage.error('操作失败') } finally { completing.value = false }
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

.code-link { color: #00d4ff; cursor: pointer; font-size: 12px; font-family: monospace; }
.code-link:hover { text-decoration: underline; }
.alert-link { color: #faad14; cursor: pointer; font-size: 12px; font-family: monospace; }
.alert-link:hover { text-decoration: underline; }
.clickable-row { cursor: pointer; }

.priority-dot { display: inline-block; width: 8px; height: 8px; border-radius: 50%; margin-right: 6px; vertical-align: middle; }
.pdot-low { background: #909399; }
.pdot-medium { background: #e6a23c; }
.pdot-high { background: #f56c6c; animation: pulse 1.5s infinite; }
.pdot-critical { background: #ff4d4f; animation: pulse 1s infinite; }
@keyframes pulse { 0%,100% { opacity: 1; } 50% { opacity: 0.4; } }

.ai-cell { display: flex; align-items: center; gap: 6px; cursor: pointer; }
.ai-badge {
  display: inline-flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, #8b5cf6, #6d28d9); color: #fff;
  font-size: 9px; font-weight: 700; padding: 2px 5px; border-radius: 4px; flex-shrink: 0;
}
.ai-text { color: #a78bfa; font-size: 12px; }
.ai-text:hover { text-decoration: underline; }

/* ── 分页 ── */
.pagination-bar { display: flex; justify-content: flex-end; margin-top: 16px; }

/* ── 详情抽屉 ── */
.detail-section { margin-bottom: 20px; }
.detail-header { display: flex; gap: 8px; align-items: center; margin-bottom: 8px; }
.detail-code { font-size: 22px; font-weight: 700; color: #e0f0ff; font-family: monospace; }
.detail-alert-link { font-size: 12px; color: #8bafc9; margin-top: 4px; }
.detail-label { font-size: 12px; color: #5a7a9a; margin-bottom: 4px; }
.detail-value { font-size: 14px; color: #c0d8f0; }
.msg-box {
  background: rgba(0,212,255,0.06); border: 1px solid #1a3a5c;
  border-radius: 8px; padding: 10px 12px; line-height: 1.7; font-size: 13px;
}

.detail-grid {
  display: grid; grid-template-columns: 1fr 1fr; gap: 12px;
  margin-bottom: 20px; padding: 14px;
  background: rgba(0,212,255,0.04); border-radius: 10px; border: 1px solid #1a3a5c;
}
.dg-label { font-size: 11px; color: #5a7a9a; }
.dg-value { font-size: 14px; color: #e0f0ff; font-weight: 500; margin-top: 2px; }

/* ── AI建议 ── */
.ai-suggestion-box {
  background: rgba(139,92,246,0.08); border: 1px solid rgba(139,92,246,0.2);
  border-radius: 10px; padding: 14px; overflow: hidden;
}
.ai-suggestion-header { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.ai-badge-lg {
  display: inline-flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, #8b5cf6, #6d28d9); color: #fff;
  font-size: 11px; font-weight: 700; padding: 3px 8px; border-radius: 6px;
}
.ai-suggestion-title { font-size: 13px; color: #a78bfa; font-weight: 600; }
.ai-suggestion-content { font-size: 13px; color: #c0d8f0; line-height: 1.7; }

/* ── 时间线 ── */
.timeline { padding-left: 12px; }
.tl-item { display: flex; gap: 12px; padding-bottom: 20px; position: relative; }
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
.tl-desc { font-size: 11px; color: #5a7a9a; margin-top: 2px; }
.tl-time { font-size: 11px; color: #5a7a9a; margin-top: 2px; }
.tl-pending { color: #faad14; }

.detail-actions { display: flex; gap: 8px; padding-top: 10px; border-top: 1px solid #1a3a5c; }

/* ── 完成弹窗 ── */
.complete-header { margin-bottom: 14px; }
.complete-info { display: flex; align-items: center; font-size: 13px; }
.complete-desc {
  font-size: 12px; color: #8bafc9; margin-top: 8px; padding: 8px 10px;
  background: rgba(0,212,255,0.04); border-radius: 6px; border: 1px solid #1a3a5c; line-height: 1.6;
}

/* ── 指派人员 ── */
.assignee-name { color: #00d4ff; font-size: 12px; font-weight: 500; }
.assign-order-info {
  font-size: 13px; color: #8bafc9; margin-bottom: 14px;
}
.assign-search { margin-bottom: 12px; }
.user-list {
  max-height: 320px; overflow-y: auto;
  border: 1px solid #1a3a5c; border-radius: 10px;
}
.user-item {
  display: flex; align-items: center; gap: 12px;
  padding: 12px 14px; cursor: pointer;
  border-bottom: 1px solid #1a3a5c22;
  transition: background 0.15s;
}
.user-item:last-child { border-bottom: none; }
.user-item:hover { background: rgba(0,212,255,0.06); }
.user-selected { background: rgba(0,212,255,0.1) !important; border-left: 3px solid #00d4ff; }
.user-avatar {
  width: 36px; height: 36px; border-radius: 50%;
  background: linear-gradient(135deg, #1a3a5c, #0f2540);
  display: flex; align-items: center; justify-content: center;
  font-size: 14px; font-weight: 700; color: #00d4ff; flex-shrink: 0;
}
.user-info { flex: 1; min-width: 0; }
.user-name { font-size: 14px; color: #e0f0ff; font-weight: 500; }
.user-meta { font-size: 11px; color: #5a7a9a; margin-top: 2px; }
.user-check { color: #00d4ff; font-size: 18px; font-weight: 700; }
</style>
