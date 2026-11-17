<template>
  <div class="admin-ops">
    <div class="page-title-bar">🛡️ 运维保障</div>

    <el-tabs v-model="activeTab" class="dark-tabs">
      <!-- 操作日志 -->
      <el-tab-pane label="操作日志" name="logs">
        <div class="ops-card">
          <div class="filter-row">
            <el-select v-model="logFilter.type" placeholder="操作类型" size="small" style="width:140px" clearable>
              <el-option label="全部" value="" />
              <el-option label="用户登录" value="login" />
              <el-option label="数据导入" value="import" />
              <el-option label="数据导出" value="export" />
              <el-option label="配置变更" value="config" />
              <el-option label="用户审批" value="approve" />
            </el-select>
            <el-date-picker v-model="logFilter.dateRange" type="daterange" size="small"
              start-placeholder="开始" end-placeholder="结束" style="width:260px"
              :shortcuts="dateShortcuts" />
            <el-button type="primary" size="small" @click="loadLogs">查询</el-button>
          </div>
          <el-table :data="logData" size="small" :header-cell-style="headerStyle" :cell-style="cellStyle" style="margin-top:12px">
            <el-table-column prop="time" label="时间" width="170" />
            <el-table-column prop="user" label="操作人" width="120" />
            <el-table-column prop="type" label="类型" width="100">
              <template #default="{ row }">
                <el-tag size="small" :type="logTagType(row.type)">{{ row.typeLabel }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="detail" label="详情" min-width="300" show-overflow-tooltip />
            <el-table-column prop="ip" label="IP" width="130" />
          </el-table>
        </div>
      </el-tab-pane>

      <!-- 系统监控 -->
      <el-tab-pane label="系统监控" name="monitor">
        <div class="monitor-grid">
          <div v-for="svc in services" :key="svc.name" class="svc-card" :class="{ healthy: svc.status === 'ok', warning: svc.status === 'warn', error: svc.status === 'error' }">
            <div class="svc-icon">{{ svc.icon }}</div>
            <div class="svc-name">{{ svc.name }}</div>
            <div class="svc-status">
              <span class="svc-dot"></span>
              {{ svc.statusLabel }}
            </div>
            <div class="svc-detail">{{ svc.detail }}</div>
          </div>
        </div>
        <div class="ops-card" style="margin-top:16px">
          <div class="card-title">实时指标</div>
          <div class="metrics-grid">
            <div v-for="m in metrics" :key="m.label" class="metric-item">
              <div class="metric-val" :style="{ color: m.color }">{{ m.value }}</div>
              <div class="metric-label">{{ m.label }}</div>
            </div>
          </div>
        </div>
      </el-tab-pane>

      <!-- 数据备份 -->
      <el-tab-pane label="数据备份" name="backup">
        <div class="ops-card">
          <div class="backup-actions">
            <el-button type="primary" :loading="backingUp" @click="doBackup">
              <el-icon><FolderAdd /></el-icon> 立即备份
            </el-button>
            <el-button type="warning" @click="showRestore = true">
              <el-icon><RefreshRight /></el-icon> 从备份恢复
            </el-button>
          </div>
          <div class="card-title" style="margin-top:20px">备份记录</div>
          <el-table :data="backupHistory" size="small" :header-cell-style="headerStyle" :cell-style="cellStyle">
            <el-table-column prop="time" label="备份时间" width="180" />
            <el-table-column prop="type" label="类型" width="100">
              <template #default="{ row }">
                <el-tag size="small" :type="row.type === 'auto' ? 'info' : 'success'">{{ row.type === 'auto' ? '自动' : '手动' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="size" label="大小" width="100" />
            <el-table-column prop="tables" label="包含表" min-width="200" />
            <el-table-column prop="status" label="状态" width="90">
              <template #default="{ row }">
                <el-tag size="small" :type="row.status === 'ok' ? 'success' : 'danger'">{{ row.status === 'ok' ? '成功' : '失败' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button type="warning" size="small" text @click="restoreFrom(row)">恢复</el-button>
                <el-button type="danger" size="small" text @click="deleteBackup(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <!-- 定时任务 -->
      <el-tab-pane label="定时任务" name="scheduler">
        <div class="ops-card">
          <div class="tab-toolbar" style="margin-bottom:14px">
            <div class="card-title" style="margin:0">任务调度管理</div>
            <el-button type="primary" size="small" @click="showAddTask = true">+ 新增任务</el-button>
          </div>
          <el-table :data="scheduledTasks" size="small" :header-cell-style="headerStyle" :cell-style="cellStyle" stripe>
            <el-table-column prop="name" label="任务名称" width="200" />
            <el-table-column prop="cron" label="执行计划" width="140">
              <template #default="{ row }">
                <code style="color:#00d4ff;font-size:12px">{{ row.cron }}</code>
              </template>
            </el-table-column>
            <el-table-column prop="cronDesc" label="说明" width="140" />
            <el-table-column prop="status" label="状态" width="90">
              <template #default="{ row }">
                <el-switch v-model="row.enabled" size="small" @change="toggleTask(row)" />
              </template>
            </el-table-column>
            <el-table-column prop="lastRun" label="上次执行" width="170" />
            <el-table-column prop="lastResult" label="结果" width="80">
              <template #default="{ row }">
                <el-tag :type="row.lastResult === 'ok' ? 'success' : row.lastResult === 'fail' ? 'danger' : 'info'" size="small">
                  {{ { ok: '成功', fail: '失败', pending: '待执行' }[row.lastResult] }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="nextRun" label="下次执行" width="170" />
            <el-table-column label="操作" width="180" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" size="small" text @click="runTaskNow(row)">立即执行</el-button>
                <el-button size="small" text @click="viewTaskLog(row)">日志</el-button>
                <el-button type="danger" size="small" text @click="deleteTask(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <!-- 任务执行历史 -->
        <div class="ops-card" style="margin-top:16px">
          <div class="card-title">📊 最近执行记录</div>
          <el-table :data="taskHistory" size="small" :header-cell-style="headerStyle" :cell-style="cellStyle" max-height="220">
            <el-table-column prop="time" label="执行时间" width="170" />
            <el-table-column prop="taskName" label="任务" width="200" />
            <el-table-column prop="duration" label="耗时" width="80" />
            <el-table-column prop="result" label="结果" width="80">
              <template #default="{ row }">
                <el-tag :type="row.result === 'ok' ? 'success' : 'danger'" size="small">{{ row.result === 'ok' ? '成功' : '失败' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="detail" label="详情" min-width="250" show-overflow-tooltip />
          </el-table>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { FolderAdd, RefreshRight } from '@element-plus/icons-vue'

const activeTab = ref('logs')
const headerStyle = { background: '#0f2540', color: '#8bafc9', borderColor: '#1a3a5c' }
const cellStyle = { background: '#0d1b2e', color: '#c0d8f0', borderColor: '#1a3a5c' }

const dateShortcuts = [
  { text: '今天', value: () => { const d = new Date(); return [d, d] } },
  { text: '最近7天', value: () => { const d = new Date(); return [new Date(d.getTime() - 7 * 86400000), d] } },
  { text: '最近30天', value: () => { const d = new Date(); return [new Date(d.getTime() - 30 * 86400000), d] } },
]

// 操作日志
const logFilter = reactive({ type: '', dateRange: null })
const logData = ref([
  { time: '2026-04-11 11:58:32', user: '管理员', type: 'login', typeLabel: '用户登录', detail: '管理员登录系统', ip: '192.168.1.100' },
  { time: '2026-04-11 11:45:10', user: '中中公司', type: 'import', typeLabel: '数据导入', detail: '导入能耗数据 research_office.xlsx (120条)', ip: '192.168.1.55' },
  { time: '2026-04-11 10:30:05', user: '管理员', type: 'approve', typeLabel: '用户审批', detail: '审批通过用户 张三 的企业用户申请', ip: '192.168.1.100' },
  { time: '2026-04-10 23:00:00', user: '系统', type: 'config', typeLabel: '配置变更', detail: '自动备份任务执行完成', ip: '127.0.0.1' },
  { time: '2026-04-10 15:20:18', user: '个人用户A', type: 'export', typeLabel: '数据导出', detail: '导出能耗分析Excel报表', ip: '192.168.1.88' },
])

function logTagType(type) {
  return { login: '', import: 'success', export: 'warning', config: 'info', approve: 'danger' }[type] || ''
}
function loadLogs() { ElMessage.info('日志已刷新') }

// 系统监控
const services = ref([
  { name: 'Spring Boot', icon: '🟢', status: 'ok', statusLabel: '运行中', detail: 'PID 29544 · 内存 512MB · 运行 3h' },
  { name: 'MySQL 数据库', icon: '🗄️', status: 'ok', statusLabel: '已连接', detail: '连接池 5/20 · 查询 1.2ms avg' },
  { name: 'Coze AI 服务', icon: '🤖', status: 'ok', statusLabel: '可用', detail: '延迟 380ms · 今日调用 45次' },
  { name: 'Vue 前端', icon: '🌐', status: 'ok', statusLabel: '正常', detail: 'Vite dev · Port 5173' },
  { name: '文件存储', icon: '📁', status: 'ok', statusLabel: '正常', detail: '已用 2.1GB / 50GB' },
  { name: '消息队列', icon: '📨', status: 'warn', statusLabel: '未配置', detail: '暂未接入消息队列服务' },
])

const metrics = ref([
  { label: 'API 请求/分', value: '42', color: '#00d4ff' },
  { label: '平均响应', value: '18ms', color: '#52c41a' },
  { label: 'CPU 使用', value: '23%', color: '#faad14' },
  { label: '内存使用', value: '512MB', color: '#9254de' },
  { label: '数据库连接', value: '5/20', color: '#00d4ff' },
  { label: '今日错误', value: '0', color: '#52c41a' },
])

// 数据备份
const backingUp = ref(false)
const showRestore = ref(false)
const backupHistory = ref([
  { time: '2026-04-11 00:00:00', type: 'auto', size: '15.2 MB', tables: 'energy_consumption, buildings, users', status: 'ok' },
  { time: '2026-04-10 00:00:00', type: 'auto', size: '14.8 MB', tables: 'energy_consumption, buildings, users', status: 'ok' },
  { time: '2026-04-09 14:30:00', type: 'manual', size: '14.5 MB', tables: '全量备份', status: 'ok' },
  { time: '2026-04-08 00:00:00', type: 'auto', size: '14.1 MB', tables: 'energy_consumption, buildings, users', status: 'ok' },
])

async function doBackup() {
  backingUp.value = true
  setTimeout(() => {
    backingUp.value = false
    backupHistory.value.unshift({
      time: new Date().toISOString().slice(0, 19).replace('T', ' '),
      type: 'manual', size: '15.5 MB', tables: '全量备份', status: 'ok'
    })
    ElMessage.success('数据备份完成')
  }, 2000)
}

async function restoreFrom(row) {
  await ElMessageBox.confirm(`确定从 ${row.time} 的备份恢复？此操作不可撤销。`, '确认恢复', { type: 'warning' }).catch(() => null)
  ElMessage.success('数据恢复完成')
}

function deleteBackup(row) {
  ElMessageBox.confirm(`确定删除 ${row.time} 的备份？`, '确认删除', { type: 'warning' })
    .then(() => {
      backupHistory.value = backupHistory.value.filter(b => b !== row)
      ElMessage.success('备份已删除')
    }).catch(() => {})
}

// 定时任务
const showAddTask = ref(false)
const scheduledTasks = ref([
  { name: '每日能耗数据汇总', cron: '0 0 0 * * ?', cronDesc: '每天 00:00', enabled: true, lastRun: '2026-04-13 00:00:02', lastResult: 'ok', nextRun: '2026-04-14 00:00:00' },
  { name: '每周能耗周报生成', cron: '0 0 8 ? * MON', cronDesc: '每周一 08:00', enabled: true, lastRun: '2026-04-07 08:00:05', lastResult: 'ok', nextRun: '2026-04-14 08:00:00' },
  { name: '每月缓存清理', cron: '0 0 3 1 * ?', cronDesc: '每月初一 03:00', enabled: true, lastRun: '2026-04-01 03:00:01', lastResult: 'ok', nextRun: '2026-05-01 03:00:00' },
  { name: '数据库自动备份', cron: '0 0 2 * * ?', cronDesc: '每天 02:00', enabled: true, lastRun: '2026-04-13 02:00:03', lastResult: 'ok', nextRun: '2026-04-14 02:00:00' },
  { name: '设备维保到期检查', cron: '0 0 9 * * ?', cronDesc: '每天 09:00', enabled: true, lastRun: '2026-04-13 09:00:01', lastResult: 'ok', nextRun: '2026-04-14 09:00:00' },
  { name: '异常数据扫描告警', cron: '0 */15 * * * ?', cronDesc: '每15分钟', enabled: true, lastRun: '2026-04-13 10:30:00', lastResult: 'ok', nextRun: '2026-04-13 10:45:00' },
  { name: '月度报表邮件发送', cron: '0 0 9 1 * ?', cronDesc: '每月初一 09:00', enabled: false, lastRun: '2026-04-01 09:00:10', lastResult: 'fail', nextRun: '—' },
])

const taskHistory = ref([
  { time: '2026-04-13 10:30:00', taskName: '异常数据扫描告警', duration: '1.2s', result: 'ok', detail: '扫描完成，发现 0 条异常' },
  { time: '2026-04-13 09:00:01', taskName: '设备维保到期检查', duration: '0.8s', result: 'ok', detail: '检查完成，3台设备即将到期，已发送提醒' },
  { time: '2026-04-13 02:00:03', taskName: '数据库自动备份', duration: '12.5s', result: 'ok', detail: '备份完成，文件大小 15.8MB' },
  { time: '2026-04-13 00:00:02', taskName: '每日能耗数据汇总', duration: '3.4s', result: 'ok', detail: '汇总 7栋楼宇 2026-04-12 能耗数据，共 168 条记录' },
  { time: '2026-04-01 09:00:10', taskName: '月度报表邮件发送', duration: '5.2s', result: 'fail', detail: 'SMTP连接失败：Connection refused (smtp.example.com:465)' },
])

function toggleTask(task) {
  ElMessage.success(`任务「${task.name}」已${task.enabled ? '启用' : '停用'}`)
}
function runTaskNow(task) {
  ElMessageBox.confirm(`确定立即执行「${task.name}」？`, '确认', { type: 'info' })
    .then(() => {
      ElMessage.success(`任务「${task.name}」已触发执行`)
      task.lastRun = new Date().toISOString().slice(0, 19).replace('T', ' ')
      task.lastResult = 'ok'
    }).catch(() => {})
}
function viewTaskLog(task) { ElMessage.info(`查看任务「${task.name}」的执行日志`) }
function deleteTask(task) {
  ElMessageBox.confirm(`确定删除任务「${task.name}」？`, '确认删除', { type: 'warning' })
    .then(() => { scheduledTasks.value = scheduledTasks.value.filter(t => t !== task); ElMessage.success('已删除') })
    .catch(() => {})
}
</script>

<style scoped>
.admin-ops { display: flex; flex-direction: column; gap: 16px; }
.page-title-bar { font-size: 18px; font-weight: 700; color: #e0f0ff; }
.ops-card { background: #0d1b2e; border-radius: 10px; padding: 18px; }
.card-title { color: #e0f0ff; font-weight: 600; font-size: 15px; margin-bottom: 12px; }
.filter-row { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }

.dark-tabs :deep(.el-tabs__item) { color: #5a7a9a; }
.dark-tabs :deep(.el-tabs__item.is-active) { color: #00d4ff; }
.dark-tabs :deep(.el-tabs__active-bar) { background: #00d4ff; }
.dark-tabs :deep(.el-tabs__nav-wrap::after) { background: #1a3a5c; }

.monitor-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 14px; }
.svc-card {
  background: #0d1b2e; border-radius: 10px; padding: 16px; border-left: 3px solid #1a3a5c;
}
.svc-card.healthy { border-left-color: #52c41a; }
.svc-card.warning { border-left-color: #faad14; }
.svc-card.error { border-left-color: #ff4d4f; }
.svc-icon { font-size: 24px; margin-bottom: 8px; }
.svc-name { color: #e0f0ff; font-weight: 600; font-size: 14px; }
.svc-status { display: flex; align-items: center; gap: 6px; margin-top: 6px; font-size: 13px; color: #8bafc9; }
.svc-dot { width: 8px; height: 8px; border-radius: 50%; }
.svc-card.healthy .svc-dot { background: #52c41a; }
.svc-card.warning .svc-dot { background: #faad14; }
.svc-card.error .svc-dot { background: #ff4d4f; }
.svc-detail { color: #5a7a9a; font-size: 12px; margin-top: 6px; }

.metrics-grid { display: grid; grid-template-columns: repeat(6, 1fr); gap: 14px; }
.metric-item { text-align: center; padding: 12px; background: #0a1628; border-radius: 8px; }
.metric-val { font-size: 22px; font-weight: 700; }
.metric-label { color: #5a7a9a; font-size: 12px; margin-top: 4px; }

.backup-actions { display: flex; gap: 12px; }
.tab-toolbar { display: flex; justify-content: space-between; align-items: center; }
</style>
