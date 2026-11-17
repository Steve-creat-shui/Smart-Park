<template>
  <div class="admin-page">
    <!-- 顶部横幅 -->
    <div class="admin-banner">
      <div class="ab-left">
        <div class="ab-title">管理面板</div>
        <div class="ab-sub">用户审批 · 企业审批 · 用户管理 · 企业管理 · 数据统计</div>
      </div>
      <div class="ab-right">
        <div class="ab-stat" v-if="stats.pendingCount > 0">
          <span class="ab-badge">{{ stats.pendingCount }}</span> 待审批用户
        </div>
        <div class="ab-stat" v-if="pendingApps.length > 0">
          <span class="ab-badge">{{ pendingApps.length }}</span> 待审批企业
        </div>
        <div class="ab-stat">共 {{ stats.totalUsers || 0 }} 用户 · {{ enterpriseList.length }} 企业</div>
      </div>
    </div>

    <el-tabs v-model="activeTab" class="admin-tabs">
      <!-- ====== Tab 1: 用户审批 ====== -->
      <el-tab-pane label="用户审批" name="pending">
        <div class="tab-toolbar">
          <span class="tb-count">{{ pendingUsers.length }} 条待审批</span>
          <el-button type="primary" size="small" @click="loadPending" :loading="loadingPending">刷新</el-button>
        </div>
        <el-table :data="pendingUsers" size="small" :header-cell-style="headerStyle" :cell-style="cellStyle"
          empty-text="暂无待审批用户" stripe>
          <el-table-column prop="employeeId" label="工号" width="110" />
          <el-table-column prop="realName" label="姓名" width="90" />
          <el-table-column prop="department" label="部门" width="90" />
          <el-table-column prop="position" label="职位" width="100" />
          <el-table-column prop="phone" label="手机号" width="120" />
          <el-table-column prop="role" label="申请身份" width="100">
            <template #default="{ row }">
              <el-tag :type="row.role === 'enterprise' ? 'warning' : 'info'" size="small" effect="dark">
                {{ row.role === 'enterprise' ? '企业用户' : '个人用户' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="申请时间" width="150">
            <template #default="{ row }">{{ row.createdAt?.slice(0, 16) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="160" fixed="right">
            <template #default="{ row }">
              <el-button type="success" size="small" @click="approve(row.id)">通过</el-button>
              <el-button type="danger" size="small" @click="showReject(row)">拒绝</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- ====== Tab 2: 企业审批 ====== -->
      <el-tab-pane name="app-review">
        <template #label>
          <span>企业审批 <el-badge v-if="pendingApps.length" :value="pendingApps.length" :max="99" style="margin-left:4px" /></span>
        </template>
        <div class="tab-toolbar">
          <span class="tb-count">{{ pendingApps.length }} 条待审核</span>
          <el-select v-model="appFilter" size="small" style="width:110px" @change="loadApps">
            <el-option label="待审核" value="pending" />
            <el-option label="已通过" value="approved" />
            <el-option label="已驳回" value="rejected" />
            <el-option label="全部" value="" />
          </el-select>
          <el-button type="primary" size="small" @click="loadApps" :loading="loadingApps">刷新</el-button>
        </div>
        <el-table :data="appList" size="small" :header-cell-style="headerStyle" :cell-style="cellStyle" empty-text="暂无申请" stripe>
          <el-table-column prop="enterpriseName" label="企业名称" min-width="160" />
          <el-table-column prop="industry" label="行业" width="100" />
          <el-table-column prop="applicantName" label="申请人" width="100" />
          <el-table-column prop="applicantPhone" label="手机号" width="130" />
          <el-table-column prop="contactName" label="联系人" width="100" />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="{pending:'warning',approved:'success',rejected:'danger'}[row.status]" size="small" effect="dark">
                {{ {pending:'待审核',approved:'已通过',rejected:'已驳回'}[row.status] }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="申请时间" width="160">
            <template #default="{ row }">{{ row.createdAt?.slice(0, 16) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <template v-if="row.status === 'pending'">
                <el-button type="success" size="small" @click="approveApp(row.id)">通过</el-button>
                <el-button type="danger" size="small" @click="showRejectApp(row)">驳回</el-button>
              </template>
              <span v-else-if="row.status === 'rejected'" style="color:#5a7a9a;font-size:12px">{{ row.rejectReason }}</span>
              <span v-else style="color:#52c41a;font-size:12px">已创建企业</span>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- ====== Tab 3: 用户管理 ====== -->
      <el-tab-pane label="用户管理" name="users">
        <div class="tab-toolbar">
          <el-input v-model="searchKey" placeholder="搜索姓名/工号/手机" size="small"
            style="width:200px" clearable @clear="loadUsers" @keyup.enter="loadUsers" />
          <el-select v-model="filterStatus" placeholder="状态" size="small" style="width:110px" clearable @change="loadUsers">
            <el-option label="全部" value="" />
            <el-option label="激活" value="active" />
            <el-option label="待审批" value="pending" />
            <el-option label="已拒绝" value="rejected" />
            <el-option label="已禁用" value="disabled" />
          </el-select>
          <el-select v-model="filterRole" placeholder="角色" size="small" style="width:110px" clearable @change="loadUsers">
            <el-option label="全部" value="" />
            <el-option label="管理员" value="admin" />
            <el-option label="企业用户" value="enterprise" />
            <el-option label="个人用户" value="user" />
          </el-select>
          <el-button type="primary" size="small" @click="loadUsers" :loading="loadingUsers">查询</el-button>
        </div>
        <el-table :data="allUsers" size="small" :header-cell-style="headerStyle" :cell-style="cellStyle" stripe>
          <el-table-column prop="employeeId" label="工号" width="110" />
          <el-table-column prop="realName" label="姓名" width="90" />
          <el-table-column prop="department" label="部门" width="90" />
          <el-table-column prop="phone" label="手机号" width="120" />
          <el-table-column prop="role" label="角色" width="90">
            <template #default="{ row }">
              <el-tag :type="{admin:'danger',enterprise:'warning',user:'info'}[row.role]" size="small" effect="dark">
                {{ {admin:'管理员',enterprise:'企业用户',user:'个人用户'}[row.role] }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="lastLoginAt" label="最后登录" width="150">
            <template #default="{ row }">{{ row.lastLoginAt?.slice(0, 16) || '—' }}</template>
          </el-table-column>
          <el-table-column label="操作" width="220" fixed="right">
            <template #default="{ row }">
              <el-button v-if="row.status === 'active'" type="warning" size="small" @click="disable(row.id)">禁用</el-button>
              <el-button v-if="row.status === 'disabled'" type="success" size="small" @click="enable(row.id)">启用</el-button>
              <el-button v-if="row.role === 'user'" type="warning" size="small" plain @click="changeRole(row.id, 'enterprise')">升企业</el-button>
              <el-button v-if="row.role !== 'admin'" type="primary" size="small" plain @click="promoteAdmin(row.id)">升管理员</el-button>
              <el-button v-if="row.role === 'admin' && row.phone !== '13800000000'" size="small" plain @click="demoteUser(row.id)">降为用户</el-button>
              <el-button v-if="row.role === 'enterprise'" size="small" plain @click="changeRole(row.id, 'user')">降为用户</el-button>
              <el-button size="small" @click="resetPwd(row.id)">重置密码</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- ====== Tab 4: 企业管理 ====== -->
      <el-tab-pane label="企业管理" name="enterprise">
        <div class="tab-toolbar">
          <span class="tb-count">共 {{ enterpriseList.length }} 家企业</span>
          <el-button type="primary" size="small" @click="loadEnterpriseList" :loading="loadingEnterprise">刷新</el-button>
        </div>
        <el-table :data="enterpriseList" size="small" :header-cell-style="headerStyle" :cell-style="cellStyle" empty-text="暂无企业" stripe>
          <el-table-column prop="id" label="ID" width="60" />
          <el-table-column prop="enterpriseName" label="企业名称" min-width="180" />
          <el-table-column prop="enterpriseCode" label="信用代码" width="160">
            <template #default="{ row }">{{ row.enterpriseCode || '—' }}</template>
          </el-table-column>
          <el-table-column prop="industry" label="行业" width="100" />
          <el-table-column prop="contactName" label="联系人" width="100" />
          <el-table-column prop="memberCount" label="成员数" width="80" />
          <el-table-column prop="status" label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="row.status === 'active' ? 'success' : 'danger'" size="small" effect="dark">
                {{ row.status === 'active' ? '正常' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="创建时间" width="160">
            <template #default="{ row }">{{ row.createdAt?.slice(0, 16) }}</template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- ====== Tab 4: 数据总览 ====== -->
      <el-tab-pane label="数据总览" name="stats">
        <!-- 用户统计卡片 -->
        <div class="section-title">👥 用户统计</div>
        <div class="stats-cards">
          <div class="sc-card" v-for="s in statCards" :key="s.label">
            <div class="sc-icon" :style="{color: s.color, background: s.color + '15'}">{{ s.icon }}</div>
            <div class="sc-val" :style="{color: s.color}">{{ s.value }}</div>
            <div class="sc-label">{{ s.label }}</div>
          </div>
        </div>

        <!-- 能耗统计卡片 -->
        <div class="section-title" style="margin-top:20px">⚡ 能耗总览（近30天）</div>
        <div class="stats-cards">
          <div class="sc-card" v-for="e in energyCards" :key="e.label">
            <div class="sc-icon" :style="{color: e.color, background: e.color + '15'}">{{ e.icon }}</div>
            <div class="sc-val" :style="{color: e.color}">{{ e.value }}</div>
            <div class="sc-label">{{ e.label }}</div>
          </div>
        </div>

        <!-- 图表行 -->
        <div class="stats-chart-row" style="margin-top:16px">
          <div class="stats-chart-card">
            <div class="scc-title">部门分布</div>
            <div ref="deptChartRef" style="height:280px" />
          </div>
          <div class="stats-chart-card">
            <div class="scc-title">用户状态</div>
            <div ref="statusChartRef" style="height:280px" />
          </div>
        </div>

        <div class="stats-chart-row" style="margin-top:16px">
          <div class="stats-chart-card">
            <div class="scc-title">楼宇能耗排名（近30天 kWh）</div>
            <div ref="rankChartRef" style="height:300px" />
          </div>
          <div class="stats-chart-card">
            <div class="scc-title">多模态异常告警统计</div>
            <div ref="alertChartRef" style="height:300px" />
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 拒绝弹窗 -->
    <el-dialog v-model="rejectDialogVisible" title="拒绝申请" width="400px">
      <p style="color:#8bafc9;margin-bottom:12px">请输入拒绝原因：</p>
      <el-input v-model="rejectReason" type="textarea" :rows="3" placeholder="请说明拒绝原因" />
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="confirmReject">确认拒绝</el-button>
      </template>
    </el-dialog>

    <!-- 驳回企业申请弹窗 -->
    <el-dialog v-model="rejectAppDialogVisible" title="驳回企业申请" width="400px">
      <p style="color:#8bafc9;margin-bottom:12px">请输入驳回原因：</p>
      <el-input v-model="rejectAppReason" type="textarea" :rows="3" placeholder="请说明驳回原因" />
      <template #footer>
        <el-button @click="rejectAppDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="confirmRejectApp">确认驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi, dashboardApi } from '../api/index.js'
import request from '../api/index.js'
import * as echarts from 'echarts'

const route = useRoute()
const activeTab = ref('pending')
const loadingPending = ref(false)
const loadingUsers = ref(false)
const pendingUsers = ref([])
const allUsers = ref([])
const searchKey = ref('')
const filterStatus = ref('')
const filterRole = ref('')
const stats = ref({})
const deptChartRef = ref(null)
const statusChartRef = ref(null)
const rankChartRef = ref(null)
const alertChartRef = ref(null)
const energyOverview = ref({})
const buildingRanking = ref([])
const alertTypeStats = ref({})
const loadingEnterprise = ref(false)
const enterpriseList = ref([])
const rejectDialogVisible = ref(false)
const rejectReason = ref('')
const rejectTargetId = ref(null)

const loadingApps = ref(false)
const appFilter = ref('pending')
const appList = ref([])
const pendingApps = computed(() => appList.value.filter(a => a.status === 'pending'))
const rejectAppDialogVisible = ref(false)
const rejectAppReason = ref('')
const rejectAppTargetId = ref(null)

const headerStyle = { background: '#0f2540', color: '#8bafc9', borderColor: '#1a3a5c' }
const cellStyle = { background: '#0d1b2e', color: '#c0d8f0', borderColor: '#1a3a5c' }

const statCards = computed(() => [
  { icon: '👥', label: '总用户', value: stats.value.totalUsers || 0, color: '#00d4ff' },
  { icon: '⏳', label: '待审批', value: stats.value.pendingCount || 0, color: '#faad14' },
  { icon: '✅', label: '已激活', value: stats.value.activeCount || 0, color: '#52c41a' },
  { icon: '🛡️', label: '管理员', value: stats.value.adminCount || 0, color: '#8b5cf6' },
  { icon: '🏢', label: '企业用户', value: stats.value.enterpriseCount || 0, color: '#e6a23c' },
  { icon: '👤', label: '个人用户', value: stats.value.userCount || 0, color: '#909399' },
])

const energyCards = computed(() => [
  { icon: '⚡', label: '本月用电(kWh)', value: fmt(energyOverview.value.monthlyPowerConsumption), color: '#00d4ff' },
  { icon: '�', label: '本月碳排(kg)', value: fmt(energyOverview.value.monthlyCarbonEmission), color: '#8b5cf6' },
  { icon: '🏢', label: '楼宇数', value: energyOverview.value.buildingCount || 0, color: '#52c41a' },
  { icon: '⚠️', label: '能耗异常', value: energyOverview.value.alertCount || 0, color: '#faad14' },
  { icon: '🚨', label: '多模态告警', value: energyOverview.value.mmAlertCount || 0, color: '#ff4d4f' },
])

function fmt(v) { return v != null ? Number(v).toLocaleString(undefined, {maximumFractionDigits: 1}) : '0' }

function statusTagType(s) {
  return { active: 'success', pending: 'warning', rejected: 'danger', disabled: 'info' }[s] || 'info'
}
function statusLabel(s) {
  return { active: '激活', pending: '待审批', rejected: '已拒绝', disabled: '已禁用' }[s] || s
}

async function loadPending() {
  loadingPending.value = true
  try {
    const res = await adminApi.getPendingUsers()
    if (res.code === 200) pendingUsers.value = res.data || []
  } catch (e) {} finally { loadingPending.value = false }
}

async function loadUsers() {
  loadingUsers.value = true
  try {
    const res = await adminApi.getAllUsers({ status: filterStatus.value, role: filterRole.value, keyword: searchKey.value })
    if (res.code === 200) allUsers.value = res.data || []
  } catch (e) {} finally { loadingUsers.value = false }
}

async function loadStats() {
  try {
    const res = await adminApi.getStats()
    if (res.code === 200) {
      stats.value = res.data || {}
    }
  } catch (e) {}
  // 加载能耗数据
  try {
    const res = await dashboardApi.getStats()
    if (res.code === 200) {
      energyOverview.value = res.data || {}
    }
  } catch (e) {}
  // 加载告警统计
  try {
    const res = await request.get('/multimodal/dashboard/B001')
    if (res.code === 200) {
      alertTypeStats.value = res.data.alertTypeDistribution || {}
      energyOverview.value.mmAlertCount = res.data.todayAlertCount || 0
    }
  } catch (e) {}
  await nextTick()
  renderCharts()
}

async function approve(id) {
  try {
    await adminApi.approveUser(id)
    ElMessage.success('已通过')
    loadPending()
    loadStats()
  } catch (e) {}
}

function showReject(row) {
  rejectTargetId.value = row.id
  rejectReason.value = ''
  rejectDialogVisible.value = true
}

async function confirmReject() {
  if (!rejectReason.value.trim()) { ElMessage.warning('请填写拒绝原因'); return }
  try {
    await adminApi.rejectUser(rejectTargetId.value, rejectReason.value)
    ElMessage.success('已拒绝')
    rejectDialogVisible.value = false
    loadPending()
    loadStats()
  } catch (e) {}
}

async function disable(id) {
  await ElMessageBox.confirm('确定禁用该用户？', '提示', { type: 'warning' }).catch(() => null)
  try { await adminApi.disableUser(id); ElMessage.success('已禁用'); loadUsers(); loadStats() } catch (e) {}
}

async function enable(id) {
  try { await adminApi.enableUser(id); ElMessage.success('已启用'); loadUsers(); loadStats() } catch (e) {}
}

async function promoteAdmin(id) {
  await ElMessageBox.confirm('确定将该用户升级为管理员？', '提示', { type: 'warning' }).catch(() => null)
  try { await adminApi.updateRole(id, 'admin'); ElMessage.success('已升级'); loadUsers() } catch (e) {}
}

async function demoteUser(id) {
  try { await adminApi.updateRole(id, 'user'); ElMessage.success('已降级'); loadUsers() } catch (e) {}
}

async function changeRole(id, newRole) {
  const label = { enterprise: '企业用户', user: '个人用户', admin: '管理员' }[newRole]
  try { await adminApi.updateRole(id, newRole); ElMessage.success(`已变更为${label}`); loadUsers() } catch (e) {}
}

async function resetPwd(id) {
  await ElMessageBox.confirm('确定将密码重置为 123456？', '提示', { type: 'warning' }).catch(() => null)
  try { await adminApi.resetPassword(id); ElMessage.success('密码已重置为 123456') } catch (e) {}
}

function renderCharts() {
  // 部门分布饼图
  if (deptChartRef.value) {
    const chart = echarts.init(deptChartRef.value)
    const dist = stats.value.departmentDistribution || {}
    chart.setOption({
      tooltip: { trigger: 'item' },
      series: [{
        type: 'pie', radius: ['40%', '70%'],
        label: { color: '#8bafc9', fontSize: 12 },
        data: Object.entries(dist).map(([name, value]) => ({ name, value })),
        itemStyle: { borderColor: '#0d1b2e', borderWidth: 2 }
      }]
    })
  }
  // 状态分布饼图
  if (statusChartRef.value) {
    const chart = echarts.init(statusChartRef.value)
    chart.setOption({
      tooltip: { trigger: 'item' },
      color: ['#52c41a', '#faad14', '#ff4d4f', '#8c8c8c'],
      series: [{
        type: 'pie', radius: ['40%', '70%'],
        label: { color: '#8bafc9', fontSize: 12 },
        data: [
          { name: '激活', value: stats.value.activeCount || 0 },
          { name: '待审批', value: stats.value.pendingCount || 0 },
          { name: '已拒绝', value: stats.value.rejectedCount || 0 },
          { name: '已禁用', value: stats.value.disabledCount || 0 }
        ],
        itemStyle: { borderColor: '#0d1b2e', borderWidth: 2 }
      }]
    })
  }
  // 楼宇能耗排名
  if (rankChartRef.value) {
    const chart = echarts.init(rankChartRef.value)
    const eo = energyOverview.value
    const ranking = eo.buildingRanking || []
    const bMap = { B001: '行政楼', B002: '图书馆', B003: '实验楼', B004: '教学楼', B005: '宿舍楼', B006: '餐厅', B007: '体育馆' }
    const names = ranking.map(r => bMap[r.buildingId] || r.buildingId || '')
    const values = ranking.map(r => Math.round(r.totalPower || 0))
    chart.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: 100, right: 30, top: 10, bottom: 30 },
      xAxis: { type: 'value', axisLabel: { color: '#5a7a9a' }, splitLine: { lineStyle: { color: '#1a3a5c33' } } },
      yAxis: { type: 'category', data: names.reverse(), axisLabel: { color: '#8bafc9', fontSize: 11 }, axisLine: { lineStyle: { color: '#1a3a5c' } } },
      series: [{
        type: 'bar', data: values.reverse(),
        itemStyle: { color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
          { offset: 0, color: '#1a3a5c' }, { offset: 1, color: '#00d4ff' }
        ]), borderRadius: [0, 6, 6, 0] },
        label: { show: true, position: 'right', color: '#8bafc9', fontSize: 11 }
      }]
    })
  }
  // 告警类型分布
  if (alertChartRef.value) {
    const chart = echarts.init(alertChartRef.value)
    const dist = alertTypeStats.value || {}
    const colors = { '无人照明': '#faad14', '无人空调': '#00d4ff', '无人设备运行': '#8b5cf6', '夜间异常人员': '#1890ff', '人少能耗高': '#ff6b6b' }
    chart.setOption({
      tooltip: { trigger: 'item' },
      color: Object.keys(dist).map(k => colors[k] || '#52c41a'),
      series: [{
        type: 'pie', radius: ['40%', '70%'],
        label: { color: '#8bafc9', fontSize: 11 },
        data: Object.entries(dist).map(([name, value]) => ({ name, value })),
        itemStyle: { borderColor: '#0d1b2e', borderWidth: 2 }
      }]
    })
  }
}

async function loadEnterpriseList() {
  loadingEnterprise.value = true
  try {
    const res = await adminApi.getEnterprises()
    if (res.code === 200) enterpriseList.value = res.data || []
  } catch (e) {} finally { loadingEnterprise.value = false }
}

async function loadApps() {
  loadingApps.value = true
  try {
    const res = await adminApi.getEnterpriseApplications(appFilter.value)
    if (res.code === 200) appList.value = res.data || []
  } catch (e) {} finally { loadingApps.value = false }
}

async function approveApp(id) {
  await ElMessageBox.confirm('确定通过该企业注册？通过后将自动创建企业并升级用户。', '确认审批', { type: 'warning' }).catch(() => null)
  try {
    await adminApi.approveApplication(id)
    ElMessage.success('已通过，企业已创建')
    loadApps()
    loadEnterpriseList()
    loadStats()
  } catch (e) {}
}

function showRejectApp(row) {
  rejectAppTargetId.value = row.id
  rejectAppReason.value = ''
  rejectAppDialogVisible.value = true
}

async function confirmRejectApp() {
  if (!rejectAppReason.value.trim()) { ElMessage.warning('请填写驳回原因'); return }
  try {
    await adminApi.rejectApplication(rejectAppTargetId.value, rejectAppReason.value)
    ElMessage.success('已驳回')
    rejectAppDialogVisible.value = false
    loadApps()
  } catch (e) {}
}

// 监听 URL query 切换 tab
watch(() => route.query.tab, (tab) => {
  if (tab === 'enterprise') activeTab.value = 'enterprise'
}, { immediate: true })

onMounted(() => {
  if (route.query.tab === 'enterprise') activeTab.value = 'enterprise'
  loadPending()
  loadUsers()
  loadApps()
  loadEnterpriseList()
  loadStats()
})
</script>

<style scoped>
.admin-page { color: #e0f0ff; display: flex; flex-direction: column; gap: 16px; }

.admin-banner {
  background: linear-gradient(135deg, #0f2540, #0d1b2e);
  border: 1px solid #1a3a5c; border-radius: 14px;
  padding: 16px 24px; display: flex; align-items: center; justify-content: space-between;
}
.ab-title { font-size: 22px; font-weight: 700; color: #e0f0ff; }
.ab-sub { font-size: 13px; color: #5a7a9a; margin-top: 4px; }
.ab-right { display: flex; gap: 20px; align-items: center; }
.ab-stat { font-size: 13px; color: #8bafc9; }
.ab-badge {
  display: inline-block; background: #ff4d4f; color: #fff; font-size: 12px;
  font-weight: 700; padding: 2px 8px; border-radius: 10px; margin-right: 4px;
}

.admin-tabs :deep(.el-tabs__item) { color: #5a7a9a; }
.admin-tabs :deep(.el-tabs__item.is-active) { color: #00d4ff; }
.admin-tabs :deep(.el-tabs__active-bar) { background: #00d4ff; }
.admin-tabs :deep(.el-tabs__nav-wrap::after) { background: #1a3a5c; }

.tab-toolbar {
  display: flex; gap: 10px; align-items: center; margin-bottom: 14px; flex-wrap: wrap;
}
.tb-count { font-size: 13px; color: #8bafc9; margin-right: auto; }

/* 统计卡片 */
.stats-cards { display: flex; gap: 16px; flex-wrap: wrap; margin-bottom: 20px; }
.sc-card {
  flex: 1; min-width: 130px; background: #0d1b2e; border: 1px solid #1a3a5c;
  border-radius: 12px; padding: 16px; text-align: center;
}
.sc-icon {
  width: 40px; height: 40px; border-radius: 10px;
  display: inline-flex; align-items: center; justify-content: center;
  font-size: 18px; margin-bottom: 8px;
}
.sc-val { font-size: 24px; font-weight: 700; }
.sc-label { font-size: 12px; color: #5a7a9a; margin-top: 4px; }

.stats-chart-row { display: flex; gap: 16px; }
.stats-chart-card {
  flex: 1; background: #0d1b2e; border: 1px solid #1a3a5c;
  border-radius: 12px; padding: 16px;
}
.scc-title { font-size: 14px; font-weight: 600; color: #8bafc9; margin-bottom: 8px; }
.section-title { font-size: 15px; font-weight: 700; color: #e0f0ff; margin-bottom: 12px; }
</style>
