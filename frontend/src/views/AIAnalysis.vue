<template>
  <div class="ai-analysis">
    <!-- Header -->
    <div class="ai-header-card">
      <div class="ai-title-area">
        <div class="ai-icon">�</div>
        <div>
          <div class="ai-title">源芯智控平台</div>
          <div class="ai-subtitle">系统大脑 · 智能对话 · 自动派单 · 自优化引擎 · 知识库增强</div>
        </div>
      </div>
      <div class="header-stats">
        <div class="stat-item"><span class="stat-val">{{ stats.knowledgeCount }}</span><span class="stat-lbl">知识条目</span></div>
        <div class="stat-item"><span class="stat-val" style="color:#52c41a">{{ stats.positiveFeedback }}</span><span class="stat-lbl">学习样本</span></div>
        <div class="stat-item"><span class="stat-val" style="color:#fadb14">{{ stats.totalFeedback }}</span><span class="stat-lbl">训练记录</span></div>
        <el-tag :type="apiStatus === 'connected' ? 'success' : 'warning'" effect="dark" style="margin-left:8px">
          {{ apiStatus === 'connected' ? '🔗 AI已连接' : '🖥 本地模式' }}
        </el-tag>
      </div>
    </div>

    <!-- Tabs -->
    <el-tabs v-model="activeTab" class="main-tabs">

      <!-- ─── Tab 1: 智控中心（左右双栏布局） ─── -->
      <el-tab-pane label="🧠 智控中心" name="chat">
        <div class="brain-layout">
          <!-- ═══ 左侧栏：系统日志大脑 ═══ -->
          <div class="brain-left">
            <div class="brain-left-header">
              <span class="brain-left-title">⚙️ 系统大脑运行日志</span>
            </div>
            <el-tabs v-model="brainTab" class="brain-tabs" stretch>
              <!-- 自动派单日志 -->
              <el-tab-pane label="📋 自动派单" name="dispatch">
                <div class="log-scroll">
                  <div v-for="log in dispatchLogs" :key="log.id" class="log-card" :class="'log-' + log.status">
                    <div class="log-card-head">
                      <span class="log-status-dot" :class="log.status"></span>
                      <span class="log-card-title">{{ log.title }}</span>
                      <span class="log-card-time">{{ log.time }}</span>
                    </div>
                    <div class="log-card-body">{{ log.detail }}</div>
                    <div class="log-card-footer">
                      <el-tag :type="dispatchStatusType(log.status)" size="small">{{ dispatchStatusLabel(log.status) }}</el-tag>
                      <span v-if="log.assignee" class="log-assignee">👤 {{ log.assignee }}</span>
                    </div>
                  </div>
                </div>
              </el-tab-pane>
              <!-- 自优化记录 -->
              <el-tab-pane label="🔄 自优化记录" name="optimize">
                <div class="log-scroll">
                  <div v-for="log in optimizeLogs" :key="log.id" class="log-card" :class="'log-' + log.level">
                    <div class="log-card-head">
                      <span class="log-type-icon">{{ log.icon }}</span>
                      <span class="log-card-title">{{ log.title }}</span>
                      <span class="log-card-time">{{ log.time }}</span>
                    </div>
                    <div class="log-card-body">{{ log.detail }}</div>
                    <div class="log-card-metrics" v-if="log.metrics">
                      <span v-for="(v, k) in log.metrics" :key="k" class="log-metric">
                        <span class="log-metric-label">{{ k }}</span>
                        <span class="log-metric-val">{{ v }}</span>
                      </span>
                    </div>
                  </div>
                </div>
              </el-tab-pane>
              <!-- 设备智控记录 -->
              <el-tab-pane label="🎛 设备控制" name="control">
                <div class="log-scroll">
                  <div v-for="log in controlLogs" :key="log.id" class="log-card" :class="'log-' + log.result">
                    <div class="log-card-head">
                      <span class="log-type-icon">{{ log.icon }}</span>
                      <span class="log-card-title">{{ log.title }}</span>
                      <span class="log-card-time">{{ log.time }}</span>
                    </div>
                    <div class="log-card-body">{{ log.detail }}</div>
                    <div class="log-card-metrics" v-if="log.params">
                      <span v-for="(v, k) in log.params" :key="k" class="log-metric">
                        <span class="log-metric-label">{{ k }}</span>
                        <span class="log-metric-val">{{ v }}</span>
                      </span>
                    </div>
                  </div>
                </div>
              </el-tab-pane>
            </el-tabs>
          </div>

          <!-- ═══ 右侧栏：智能对话窗口 ═══ -->
          <div class="brain-right">
            <div class="chat-panel">
              <div class="chat-messages" ref="chatRef">
                <div v-for="(msg, i) in messages" :key="i">
                  <div v-if="msg.role === 'tool'" class="tool-call-bubble">
                    <span class="tool-icon">🔧</span>
                    <span class="tool-name">{{ toolLabel(msg.tool) }}</span>
                    <el-tag size="small" type="info">{{ msg.input }}</el-tag>
                    <el-icon class="tool-ok"><CircleCheckFilled /></el-icon>
                  </div>
                  <div v-else class="message" :class="msg.role">
                    <div class="msg-avatar">{{ msg.role === 'user' ? '👤' : '�' }}</div>
                    <div class="msg-wrap">
                      <div class="msg-bubble">
                        <div class="msg-content" v-html="formatMessage(msg.content)"></div>
                        <div class="msg-footer">
                          <span class="msg-time">{{ msg.time }}</span>
                          <div v-if="msg.role === 'assistant'" class="msg-feedback">
                            <el-button size="small" text :type="msg.liked ? 'success' : ''" @click="giveFeedback(msg, 1, i)" :disabled="msg.rated">👍</el-button>
                            <el-button size="small" text :type="msg.disliked ? 'danger' : ''" @click="giveFeedback(msg, -1, i)" :disabled="msg.rated">👎</el-button>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
                <div v-if="aiTyping" class="message assistant">
                  <div class="msg-avatar">�</div>
                  <div class="msg-bubble">
                    <div class="typing-dots"><span></span><span></span><span></span></div>
                    <div style="font-size:11px;color:#8bafc9;margin-top:4px">{{ typingStatus }}</div>
                  </div>
                </div>
              </div>
              <div class="input-area">
                <div class="quick-queries">
                  <span class="quick-label">快捷：</span>
                  <el-button v-for="q in quickQueries" :key="q" size="small" plain @click="sendQuickQuery(q)">{{ q }}</el-button>
                  <el-button size="small" type="warning" plain @click="clearChat" style="margin-left:auto">清空对话</el-button>
                </div>
                <div class="input-row">
                  <el-select v-model="selectedBuilding" placeholder="建筑" style="width:130px" size="large">
                    <el-option v-for="id in buildingIds" :key="id" :label="nameMap[id]||id" :value="id" />
                  </el-select>
                  <el-input v-model="userInput" placeholder="向源芯智控平台提问，自动查询数据并推理分析..."
                    size="large" @keyup.enter="sendMessage" :disabled="aiTyping" style="flex:1" />
                  <el-button type="primary" size="large" @click="sendMessage" :loading="aiTyping" icon="Promotion">发送</el-button>
                </div>
              </div>
            </div>

            <!-- 右侧底部工具栏 -->
            <div class="right-tools-row">
              <div class="analysis-card compact">
                <div class="card-header"><span class="card-title">🔧 工具能力</span></div>
                <div class="tool-list-h">
                  <div class="tool-item-h" v-for="t in agentTools" :key="t.name">
                    <span>{{ t.icon }}</span><span class="tool-item-name">{{ t.name }}</span>
                  </div>
                </div>
              </div>
              <div class="analysis-card compact">
                <div class="card-header"><span class="card-title">⚡ 快速诊断</span></div>
                <div class="tool-list-h">
                  <div class="diag-chip" v-for="item in diagnosisItems" :key="item.title" @click="runDiagnosis(item)">
                    <el-icon :size="14" :style="{ color: item.color }"><component :is="item.icon" /></el-icon>
                    <span>{{ item.title }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </el-tab-pane>

      <!-- ─── Tab 2: 知识库管理 ─── -->
      <el-tab-pane label="📚 知识库管理" name="knowledge">
        <div class="kb-toolbar">
          <el-button type="primary" icon="Plus" @click="openKbDialog(null)">新增知识</el-button>
          <el-select v-model="kbCategoryFilter" placeholder="所有分类" clearable style="width:140px">
            <el-option label="规则类" value="rule" />
            <el-option label="模式类" value="pattern" />
            <el-option label="领域知识" value="domain" />
            <el-option label="建议类" value="advice" />
          </el-select>
          <span class="kb-tip">💡 知识库条目将自动注入每次对话的系统提示，引导智能体更精准地回答</span>
        </div>
        <el-table :data="filteredKnowledge" size="small" :header-cell-style="tableHeader" :cell-style="tableCell">
          <el-table-column prop="title" label="标题" width="200" />
          <el-table-column prop="content" label="内容" show-overflow-tooltip />
          <el-table-column prop="category" label="分类" width="90">
            <template #default="{row}">
              <el-tag :type="catColor(row.category)" size="small">{{ catLabel(row.category) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="useCount" label="引用次数" width="90" align="center" />
          <el-table-column prop="createdAt" label="创建时间" width="150">
            <template #default="{row}">{{ row.createdAt?.slice(0,16) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="120">
            <template #default="{row}">
              <el-button size="small" text type="primary" @click="openKbDialog(row)">编辑</el-button>
              <el-button size="small" text type="danger" @click="deleteKb(row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-dialog v-model="kbDialogVisible" :title="kbForm.id ? '编辑知识' : '新增知识'" width="560px">
          <el-form :model="kbForm" label-width="70px">
            <el-form-item label="标题"><el-input v-model="kbForm.title" placeholder="简短描述该条知识" /></el-form-item>
            <el-form-item label="分类">
              <el-select v-model="kbForm.category" style="width:100%">
                <el-option label="规则类" value="rule" />
                <el-option label="模式类" value="pattern" />
                <el-option label="领域知识" value="domain" />
                <el-option label="建议类" value="advice" />
              </el-select>
            </el-form-item>
            <el-form-item label="内容"><el-input v-model="kbForm.content" type="textarea" :rows="5" placeholder="详细描述该知识条目，将自动注入AI系统提示" /></el-form-item>
          </el-form>
          <template #footer>
            <el-button @click="kbDialogVisible = false">取消</el-button>
            <el-button type="primary" @click="saveKb">保存</el-button>
          </template>
        </el-dialog>
      </el-tab-pane>

      <!-- ─── Tab 3: 训练记录 ─── -->
      <el-tab-pane label="🎯 训练记录" name="training">
        <div class="train-header">
          <div class="train-stat" v-for="s in trainStats" :key="s.label">
            <div class="train-val" :style="{color:s.color}">{{ s.val }}</div>
            <div class="train-lbl">{{ s.label }}</div>
          </div>
          <el-radio-group v-model="feedbackFilter" size="small" style="margin-left:auto">
            <el-radio-button value="all">全部</el-radio-button>
            <el-radio-button value="1">👍 好评</el-radio-button>
            <el-radio-button value="-1">👎 差评</el-radio-button>
          </el-radio-group>
        </div>
        <el-table :data="filteredFeedback" size="small" :header-cell-style="tableHeader" :cell-style="tableCell">
          <el-table-column label="评价" width="70" align="center">
            <template #default="{row}">
              <span style="font-size:18px">{{ row.rating === 1 ? '👍' : '👎' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="buildingId" label="建筑" width="80" />
          <el-table-column prop="question" label="问题" width="220" show-overflow-tooltip />
          <el-table-column prop="answer" label="AI回答" show-overflow-tooltip />
          <el-table-column prop="createdAt" label="时间" width="140">
            <template #default="{row}">{{ row.createdAt?.slice(0,16) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="70">
            <template #default="{row}">
              <el-button size="small" text type="danger" @click="deleteFb(row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="train-tip">💡 好评（👍）回答将作为少样本示例自动注入下次对话，帮助智能体持续学习优化。</div>
      </el-tab-pane>

    </el-tabs>
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { buildingApi, agentApi } from '../api/index.js'
import { loadBuildingNames, buildingNameMap } from '../utils/buildingName.js'
import dayjs from 'dayjs'

/* ── State ── */
const buildingIds = ref([])
const nameMap = reactive(buildingNameMap)
const selectedBuilding = ref('')
const userInput = ref('')
const aiTyping = ref(false)
const typingStatus = ref('智能体思考中...')
const apiStatus = ref('local')
const chatRef = ref(null)
const activeTab = ref('chat')
const stats = reactive({ knowledgeCount: 0, positiveFeedback: 0, totalFeedback: 0 })

/* ── Brain Left Panel ── */
const brainTab = ref('dispatch')

// TODO: 后续接入真实 REST API 或 WebSocket
const dispatchLogs = ref([
  { id: 1, title: '空调系统过载告警 → 自动生成工单', time: '2026-04-13 10:45:12', status: 'completed', detail: '检测到行政楼B001中央空调系统功率超标23%，自动创建高优先级工单 WO-20260413-001，并派发给运维组张工。', assignee: '张工' },
  { id: 2, title: '实验楼用电异常 → 触发排查工单', time: '2026-04-13 09:30:05', status: 'processing', detail: '实验楼B003夜间02:00-05:00电力消耗异常偏高（较基准+180%），系统自动生成巡检工单，等待运维确认。', assignee: '李工' },
  { id: 3, title: '水耗突增预警 → 派单至水务组', time: '2026-04-13 08:15:33', status: 'completed', detail: '图书馆B002水耗同比增长45%，可能存在管道泄漏，已自动生成工单并推送至水务维修组。', assignee: '王工' },
  { id: 4, title: '宿舍楼电梯故障 → 紧急派单', time: '2026-04-12 22:10:48', status: 'completed', detail: '宿舍楼B005电梯控制模块通信中断，系统判定为critical级别，自动派单并短信通知值班人员。', assignee: '赵工' },
  { id: 5, title: '餐厅排风系统效率下降', time: '2026-04-12 17:20:00', status: 'pending', detail: '餐厅B006排风系统运行效率下降至65%，低于阈值80%，已生成维保工单等待排期。', assignee: null },
  { id: 6, title: '光伏逆变器离线 → 自动重启失败', time: '2026-04-12 14:05:22', status: 'failed', detail: '体育馆B007屋顶光伏逆变器#3离线，远程重启指令超时，已升级为人工介入工单。', assignee: '钱工' },
])

const optimizeLogs = ref([
  { id: 1, icon: '🧬', title: '能耗预测模型迭代 v2.3.1', time: '2026-04-13 06:00:00', level: 'success', detail: '基于最近30天数据重新训练LSTM预测模型，MAE从12.3kWh降低至9.7kWh，模型已自动热更新部署。', metrics: { 'MAE': '9.7 kWh', 'RMSE': '14.2 kWh', '训练耗时': '3m 22s' } },
  { id: 2, icon: '⚙️', title: '告警阈值自适应调整', time: '2026-04-13 03:00:00', level: 'info', detail: '根据季节变化和近7天能耗模式，自动调整夏季空调用电阈值：行政楼 280→310 kWh/h，教学楼 350→380 kWh/h。', metrics: { '调整项': '5个建筑', '生效时间': '即时' } },
  { id: 3, icon: '📊', title: '异常检测灵敏度优化', time: '2026-04-12 06:00:00', level: 'success', detail: '分析上周异常检测结果：误报率从8.2%降至4.5%，将Z-Score阈值从2.5调整为2.8，减少低置信度告警。', metrics: { '误报率': '4.5%', '召回率': '96.2%', '阈值': '2.8σ' } },
  { id: 4, icon: '🔄', title: '派单策略权重更新', time: '2026-04-11 23:00:00', level: 'info', detail: '根据工单完成时效统计，调整自动派单人员匹配权重：技能匹配+30%、地理距离-10%、当前负载+20%。', metrics: { '平均响应': '15min→11min', '满意度': '94%' } },
  { id: 5, icon: '🧹', title: '数据清洗与插补', time: '2026-04-11 02:00:00', level: 'warning', detail: '检测到宿舍楼B005在04-10日存在3小时数据缺失，已使用线性插值补全，并标记为estimated数据。', metrics: { '缺失段': '3h', '补全方法': '线性插值' } },
])

// TODO: 后续接入真实设备控制 API
const controlLogs = ref([
  { id: 1, icon: '❄️', title: '行政楼中央空调温度自动调节', time: '2026-04-13 10:52:00', result: 'success', detail: '检测到行政楼B001 4F会议中心室温28.3°C，超出舒适区间。源芯大脑自动下发指令将空调设定温度从26°C调至24°C，预计15分钟内降至适宜温度。', params: { '调前温度': '28.3°C', '设定温度': '26→24°C', '目标温度': '25°C', '响应设备': 'AHU-B001-04' } },
  { id: 2, icon: '💡', title: '图书馆无人区域照明关闭', time: '2026-04-13 10:30:00', result: 'success', detail: '图书馆B002 3F阅览室B连续20分钟无人检测，源芯大脑自动关闭该区域照明，保留应急通道灯。预计节省3.2kWh/h。', params: { '节能': '3.2 kWh/h', '触发条件': '无人20min', '保留灯组': '应急通道' } },
  { id: 3, icon: '🌡️', title: '实验楼精密仪器室温控调整', time: '2026-04-13 09:45:00', result: 'success', detail: '实验楼B003精密仪器室湿度达72%（超限值65%），源芯大脑启动除湿模式并微调温度至22°C，保护精密设备运行环境。', params: { '湿度': '72%→65%', '温度': '24→22°C', '模式': '除湿+制冷' } },
  { id: 4, icon: '🔌', title: '教学楼夜间待机设备断电', time: '2026-04-13 00:05:00', result: 'success', detail: '教学楼B004检测到22:00后全楼无人，源芯大脑批量关闭多媒体教室投影仪、电子白板等待机设备共47台，预计节省夜间待机功耗12.8kW。', params: { '关闭设备': '47台', '省电功率': '12.8kW', '恢复时间': '次日07:30' } },
  { id: 5, icon: '❄️', title: '宿舍楼公共区域空调预冷', time: '2026-04-12 17:00:00', result: 'success', detail: '预测18:00-19:00宿舍楼B005门厅将迎来返回高峰（历史数据约150人/h），源芯大脑提前30分钟启动门厅空调预冷至25°C。', params: { '预测人流': '150人/h', '提前量': '30min', '预冷温度': '25°C' } },
  { id: 6, icon: '🌀', title: '餐厅排风系统功率优化', time: '2026-04-12 14:30:00', result: 'success', detail: '餐厅B006午餐高峰结束，源芯大脑检测到后厨油烟浓度降至安全值，自动将排风功率从100%降至40%，降低噪音和能耗。', params: { '排风功率': '100%→40%', '油烟值': '正常', '节能': '4.5kW' } },
  { id: 7, icon: '☀️', title: '体育馆照明亮度自适应', time: '2026-04-12 13:15:00', result: 'success', detail: '体育馆B007检测到自然采光充足（光照度820lux），源芯大脑自动将篮球场人工照明调暗至30%，实现自然光+人工光混合照明。', params: { '自然光': '820lux', '调光比': '100%→30%', '节能': '6.2kW' } },
  { id: 8, icon: '❄️', title: '行政楼空调模式切换失败', time: '2026-04-12 11:20:00', result: 'warning', detail: '行政楼B001 7F数据中心空调切换精密制冷模式时通信超时，已重试2次。系统自动维持当前模式运行并生成运维工单。', params: { '重试次数': '2', '当前模式': '普通制冷', '工单号': 'WO-0412-003' } },
])

function dispatchStatusType(s) {
  return { completed: 'success', processing: 'warning', pending: 'info', failed: 'danger' }[s] || ''
}
function dispatchStatusLabel(s) {
  return { completed: '已完成', processing: '处理中', pending: '待处理', failed: '失败' }[s] || s
}

/* ── Chat ── */
const messages = ref([{
  role: 'assistant',
  content: '您好！我是 **源芯智控平台** 智能体，具备以下能力：\n\n1. **🔧 工具调用** — 自主查询能耗数据、建筑信息、异常记录\n2. **📚 知识库增强** — 结合领域知识进行深度分析\n3. **🎯 反馈学习** — 从您的好评中学习，持续优化回答质量\n4. **💬 多轮记忆** — 记住本次对话上下文，支持追问\n\n请选择建筑并提问，我会自主调用工具获取真实数据后进行分析。',
  time: dayjs().format('HH:mm:ss')
}])
const conversationHistory = ref([])

const quickQueries = ['分析本月能耗趋势', '空调系统异常诊断', '与其他建筑对比能效', '预测未来碳排放']

const agentTools = [
  { icon: '📊', name: '能耗数据查询', desc: '查询总电量/水耗/碳排放' },
  { icon: '⚠️', name: '异常检测', desc: '识别超标用能时段' },
  { icon: '🏢', name: '建筑对比', desc: '多栋建筑能效横向比较' },
  { icon: 'ℹ️', name: '建筑信息', desc: '查询楼层/面积/空调系统' },
]

const diagnosisItems = [
  { title: '能耗异常检测', desc: '自动识别超标用能时段', icon: 'Warning', color: '#ff6b6b' },
  { title: '空调效率分析', desc: '评估COP值与优化空间', icon: 'Wind', color: '#00d4ff' },
  { title: '碳排放预测', desc: '30天碳排放趋势预测', icon: 'Cloudy', color: '#52c41a' },
  { title: '设备故障预测', desc: '基于运行数据的预测维护', icon: 'Tools', color: '#fadb14' },
]

function formatMessage(content) {
  return content
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/\n/g, '<br>')
    .replace(/^\d+\. /gm, '<span style="color:#00d4ff">▸</span> ')
    .replace(/`([^`]+)`/g, '<code style="background:#0f2540;padding:2px 6px;border-radius:4px;font-size:11px;color:#00d4ff">$1</code>')
}

function toolLabel(name) {
  const labels = { query_energy_data: '查询能耗数据', get_anomalies: '检测异常记录',
    compare_buildings: '对比多栋建筑', get_building_info: '获取建筑信息' }
  return labels[name] || name
}

async function sendMessage() {
  if (!userInput.value.trim() || aiTyping.value) return
  const query = userInput.value.trim()
  const userMsg = { role: 'user', content: query, time: dayjs().format('HH:mm:ss') }
  messages.value.push(userMsg)
  conversationHistory.value.push({ role: 'user', content: query })
  userInput.value = ''
  aiTyping.value = true
  typingStatus.value = '智能体思考中...'
  await scrollToBottom()

  try {
    typingStatus.value = '正在调用工具查询数据...'
    const res = await agentApi.query({
      buildingId: selectedBuilding.value,
      query,
      history: conversationHistory.value.slice(-10)
    })
    if (res.code === 200) {
      const d = res.data
      if (d.toolCalls && d.toolCalls.length > 0) {
        for (const tc of d.toolCalls) {
          messages.value.push({ role: 'tool', tool: tc.tool, input: tc.input, time: dayjs().format('HH:mm:ss') })
          await scrollToBottom()
        }
      }
      const aMsg = { role: 'assistant', content: d.result, time: dayjs().format('HH:mm:ss'), rated: false,
        liked: false, disliked: false, _query: query }
      messages.value.push(aMsg)
      conversationHistory.value.push({ role: 'assistant', content: d.result })
      apiStatus.value = 'connected'
    }
  } catch {
    messages.value.push({
      role: 'assistant',
      content: '智能体暂时不可用，请确认后端服务已启动。\n\n提示：配置 Coze API Token 和 Bot ID 后可启用 Coze 智能体推理能力。',
      time: dayjs().format('HH:mm:ss')
    })
  } finally {
    aiTyping.value = false
    await scrollToBottom()
  }
}

async function sendQuickQuery(q) { userInput.value = q; await sendMessage() }
async function runDiagnosis(item) {
  userInput.value = `请对建筑${selectedBuilding.value}进行${item.title}，提供详细分析结果。`
  await sendMessage()
}
function clearChat() {
  messages.value = messages.value.slice(0, 1)
  conversationHistory.value = []
  ElMessage.success('对话已清空')
}

async function giveFeedback(msg, rating, idx) {
  if (msg.rated) return
  try {
    await agentApi.saveFeedback({ question: msg._query || '', answer: msg.content,
      buildingId: selectedBuilding.value, rating })
    msg.rated = true
    msg.liked = rating === 1
    msg.disliked = rating === -1
    ElMessage.success(rating === 1 ? '已记录为优质样本，将用于后续学习 👍' : '感谢反馈，将持续改进 👎')
    await loadStats()
  } catch { ElMessage.error('反馈提交失败') }
}

async function scrollToBottom() {
  await nextTick()
  if (chatRef.value) chatRef.value.scrollTop = chatRef.value.scrollHeight
}

/* ── Knowledge Base ── */
const knowledgeList = ref([])
const kbCategoryFilter = ref('')
const kbDialogVisible = ref(false)
const kbForm = reactive({ id: null, title: '', content: '', category: 'rule' })

const filteredKnowledge = computed(() =>
  kbCategoryFilter.value
    ? knowledgeList.value.filter(k => k.category === kbCategoryFilter.value)
    : knowledgeList.value
)

function catLabel(c) { return { rule: '规则', pattern: '模式', domain: '领域', advice: '建议' }[c] || c }
function catColor(c) { return { rule: '', pattern: 'info', domain: 'success', advice: 'warning' }[c] || '' }

function openKbDialog(row) {
  if (row) { Object.assign(kbForm, { id: row.id, title: row.title, content: row.content, category: row.category }) }
  else { Object.assign(kbForm, { id: null, title: '', content: '', category: 'rule' }) }
  kbDialogVisible.value = true
}
async function saveKb() {
  if (!kbForm.title || !kbForm.content) { ElMessage.warning('请填写标题和内容'); return }
  try {
    if (kbForm.id) await agentApi.updateKnowledge(kbForm.id, { ...kbForm })
    else await agentApi.addKnowledge({ title: kbForm.title, content: kbForm.content, category: kbForm.category })
    kbDialogVisible.value = false
    await loadKnowledge()
    await loadStats()
    ElMessage.success('保存成功')
  } catch { ElMessage.error('保存失败') }
}
async function deleteKb(id) {
  await ElMessageBox.confirm('确认删除该知识条目？', '删除确认', { type: 'warning' })
  await agentApi.deleteKnowledge(id)
  await loadKnowledge()
  await loadStats()
  ElMessage.success('已删除')
}
async function loadKnowledge() {
  try { const r = await agentApi.getKnowledge(); if (r.code === 200) knowledgeList.value = r.data } catch {}
}

/* ── Training records ── */
const feedbackList = ref([])
const feedbackFilter = ref('all')
const filteredFeedback = computed(() =>
  feedbackFilter.value === 'all' ? feedbackList.value
    : feedbackList.value.filter(f => String(f.rating) === feedbackFilter.value)
)
const trainStats = computed(() => [
  { val: stats.totalFeedback, label: '总训练记录', color: '#00d4ff' },
  { val: stats.positiveFeedback, label: '好评样本', color: '#52c41a' },
  { val: stats.totalFeedback - stats.positiveFeedback, label: '差评记录', color: '#fadb14' },
  { val: stats.knowledgeCount, label: '知识条目', color: '#a855f7' },
])
async function deleteFb(id) {
  await ElMessageBox.confirm('删除该训练记录？', '确认', { type: 'warning' })
  await agentApi.deleteFeedback(id)
  await loadFeedback()
  await loadStats()
  ElMessage.success('已删除')
}
async function loadFeedback() {
  try { const r = await agentApi.getFeedbackAll(); if (r.code === 200) feedbackList.value = r.data } catch {}
}
async function loadStats() {
  try { const r = await agentApi.getStats(); if (r.code === 200) Object.assign(stats, r.data) } catch {}
}

/* ── Table styles ── */
const tableHeader = { background: '#0f2540', color: '#8bafc9', fontSize: '12px', borderBottom: '1px solid #1a3a5c' }
const tableCell = { background: '#0d1b2e', color: '#c0d8f0', borderBottom: '1px solid #1a3a5c' }

onMounted(async () => {
  try {
    await loadBuildingNames()
    const r = await buildingApi.getAllBuildingIds()
    if (r.code === 200) { buildingIds.value = r.data; if (r.data.length) selectedBuilding.value = r.data[0] }
  } catch { buildingIds.value = [] }
  await Promise.all([loadKnowledge(), loadFeedback(), loadStats()])
})
</script>

<style scoped>
.ai-analysis { display: flex; flex-direction: column; gap: 14px; }

/* ── Header ── */
.ai-header-card {
  background: linear-gradient(135deg, #0f2540, #0d1b2e);
  border: 1px solid #00d4ff44;
  border-radius: 12px;
  padding: 16px 20px;
  display: flex; align-items: center; justify-content: space-between;
  box-shadow: 0 2px 8px rgba(0,0,0,0.2);
}
.ai-title-area { display: flex; align-items: center; gap: 14px; }
.ai-icon { font-size: 34px; }
.ai-title { font-size: 17px; font-weight: 700; color: #00d4ff; }
.ai-subtitle { font-size: 12px; color: #8bafc9; margin-top: 3px; }
.header-stats { display: flex; align-items: center; gap: 20px; }
.stat-item { text-align: center; }
.stat-val { font-size: 20px; font-weight: 700; color: #00d4ff; display: block; }
.stat-lbl { font-size: 11px; color: #8bafc9; }

/* ── Main Tabs ── */
.main-tabs :deep(.el-tabs__header) { background: #0d1b2e; border-radius: 10px; padding: 4px 8px; border: 1px solid #1a3a5c; margin-bottom: 12px; box-shadow: 0 2px 6px rgba(0,0,0,0.2); }
.main-tabs :deep(.el-tabs__item) { color: #8bafc9; font-size: 13px; }
.main-tabs :deep(.el-tabs__item.is-active) { color: #00d4ff; }
.main-tabs :deep(.el-tabs__active-bar) { background-color: #00d4ff; }
.main-tabs :deep(.el-tabs__nav-wrap::after) { display: none; }
.main-tabs :deep(.el-tabs__content) { overflow: visible; }

/* ══════════════════════════════════════════
   Brain Layout — 左右双栏 30:70
   ══════════════════════════════════════════ */
.brain-layout {
  display: grid;
  grid-template-columns: 30% 1fr;
  gap: 14px;
  min-height: 660px;
}

/* ── 左侧：系统大脑日志 ── */
.brain-left {
  background: #0d1b2e;
  border: 1px solid #1a3a5c;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0,0,0,0.2);
}
.brain-left-header {
  padding: 14px 16px 0;
}
.brain-left-title {
  font-size: 14px;
  font-weight: 700;
  color: #00d4ff;
}

/* Brain inner tabs */
.brain-tabs { flex: 1; display: flex; flex-direction: column; }
.brain-tabs :deep(.el-tabs__header) {
  background: transparent;
  border: none;
  margin: 0;
  padding: 8px 12px 0;
  box-shadow: none;
}
.brain-tabs :deep(.el-tabs__item) { color: #8bafc9; font-size: 12px; padding: 0 10px; height: 32px; line-height: 32px; }
.brain-tabs :deep(.el-tabs__item.is-active) { color: #00d4ff; }
.brain-tabs :deep(.el-tabs__active-bar) { background-color: #00d4ff; }
.brain-tabs :deep(.el-tabs__nav-wrap::after) { display: none; }
.brain-tabs :deep(.el-tabs__content) { flex: 1; overflow: hidden; padding: 0; }
.brain-tabs :deep(.el-tab-pane) { height: 100%; }

/* Log scroll container */
.log-scroll {
  height: 560px;
  overflow-y: auto;
  padding: 10px 14px 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.log-scroll::-webkit-scrollbar { width: 4px; }
.log-scroll::-webkit-scrollbar-thumb { background: #1a3a5c; border-radius: 2px; }

/* Log card */
.log-card {
  background: #0f2540;
  border: 1px solid #1a3a5c;
  border-radius: 10px;
  padding: 12px 14px;
  transition: border-color 0.2s;
}
.log-card:hover { border-color: #00d4ff44; }
.log-card.log-completed { border-left: 3px solid #52c41a; }
.log-card.log-processing { border-left: 3px solid #faad14; }
.log-card.log-pending { border-left: 3px solid #8bafc9; }
.log-card.log-failed { border-left: 3px solid #ff4d4f; }
.log-card.log-success { border-left: 3px solid #52c41a; }
.log-card.log-info { border-left: 3px solid #00d4ff; }
.log-card.log-warning { border-left: 3px solid #faad14; }

.log-card-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}
.log-status-dot {
  width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0;
}
.log-status-dot.completed { background: #52c41a; }
.log-status-dot.processing { background: #faad14; animation: pulse-dot 1.5s infinite; }
.log-status-dot.pending { background: #8bafc9; }
.log-status-dot.failed { background: #ff4d4f; }
@keyframes pulse-dot { 0%,100%{ opacity:1 } 50%{ opacity:0.4 } }

.log-type-icon { font-size: 16px; flex-shrink: 0; }
.log-card-title { font-size: 12px; font-weight: 600; color: #e0f0ff; flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.log-card-time { font-size: 10px; color: #5a7a9a; flex-shrink: 0; white-space: nowrap; }
.log-card-body { font-size: 11px; color: #8bafc9; line-height: 1.65; margin-bottom: 8px; }
.log-card-footer { display: flex; align-items: center; gap: 10px; }
.log-assignee { font-size: 11px; color: #8bafc9; }

/* Metrics row (optimize tab) */
.log-card-metrics {
  display: flex; flex-wrap: wrap; gap: 8px; margin-top: 6px;
  padding-top: 8px; border-top: 1px solid #1a3a5c;
}
.log-metric {
  background: #0d1b2e;
  border: 1px solid #1a3a5c;
  border-radius: 6px;
  padding: 3px 8px;
  display: flex; align-items: center; gap: 4px;
}
.log-metric-label { font-size: 10px; color: #5a7a9a; }
.log-metric-val { font-size: 11px; color: #00d4ff; font-weight: 600; }

/* ── 右侧：智能对话 ── */
.brain-right {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.chat-panel {
  background: #0d1b2e;
  border: 1px solid #1a3a5c;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
  box-shadow: 0 2px 8px rgba(0,0,0,0.2);
}
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  max-height: 460px;
}
.chat-messages::-webkit-scrollbar { width: 4px; }
.chat-messages::-webkit-scrollbar-thumb { background: #1a3a5c; border-radius: 2px; }

.message { display: flex; gap: 10px; align-items: flex-start; }
.message.user { flex-direction: row-reverse; }
.msg-avatar { font-size: 22px; flex-shrink: 0; }
.msg-wrap { max-width: 85%; }
.msg-bubble { background: #0f2540; border: 1px solid #1a3a5c; border-radius: 12px; padding: 11px 14px; }
.message.user .msg-bubble { background: #132d4a; border-color: #00d4ff44; }
.msg-content { color: #e0f0ff; font-size: 13px; line-height: 1.75; }
.msg-content :deep(strong) { color: #00d4ff; }
.msg-content :deep(code) { font-family: monospace; }
.msg-footer { display: flex; align-items: center; justify-content: space-between; margin-top: 6px; }
.msg-time { font-size: 10px; color: #8bafc9; }
.msg-feedback { display: flex; gap: 2px; }

/* Tool call bubble */
.tool-call-bubble {
  display: flex; align-items: center; gap: 8px;
  background: #0f2540; border: 1px solid #00d4ff33; border-radius: 20px;
  padding: 5px 12px; margin: 2px 0; font-size: 12px; color: #8bafc9;
  width: fit-content;
}
.tool-icon { font-size: 14px; }
.tool-name { color: #00d4ff; font-weight: 600; }
.tool-ok { color: #52c41a; }

/* Typing */
.typing-dots { display: flex; gap: 4px; padding: 4px 0; }
.typing-dots span { width: 7px; height: 7px; border-radius: 50%; background: #00d4ff;
  animation: bounce 1.4s infinite; }
.typing-dots span:nth-child(2) { animation-delay: 0.2s; }
.typing-dots span:nth-child(3) { animation-delay: 0.4s; }
@keyframes bounce { 0%,60%,100%{transform:translateY(0)} 30%{transform:translateY(-6px)} }

/* Input area */
.input-area { border-top: 1px solid #1a3a5c; padding: 12px 14px; }
.quick-queries { display: flex; align-items: center; gap: 6px; flex-wrap: wrap; margin-bottom: 10px; }
.quick-label { font-size: 11px; color: #8bafc9; }
.input-row { display: flex; gap: 8px; }

/* Right tools row (horizontal compact) */
.right-tools-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}
.analysis-card { background: #0d1b2e; border: 1px solid #1a3a5c; border-radius: 12px; padding: 12px 14px; box-shadow: 0 2px 8px rgba(0,0,0,0.2); }
.analysis-card.compact { padding: 10px 12px; }
.analysis-card.compact .card-header { margin-bottom: 6px; }
.card-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 10px; }
.card-title { font-size: 13px; font-weight: 600; color: #e0f0ff; }

.tool-list-h { display: flex; flex-wrap: wrap; gap: 6px; }
.tool-item-h {
  display: flex; align-items: center; gap: 4px;
  background: #0f2540; border-radius: 6px; padding: 4px 10px;
  font-size: 12px; color: #e0f0ff;
}
.tool-item-h .tool-item-name { font-size: 11px; color: #e0f0ff; }

.diag-chip {
  display: flex; align-items: center; gap: 4px;
  background: #0f2540; border-radius: 6px; padding: 4px 10px;
  font-size: 11px; color: #e0f0ff; cursor: pointer;
  border: 1px solid transparent; transition: border-color 0.2s;
}
.diag-chip:hover { border-color: #00d4ff44; }

/* Knowledge tab */
.kb-toolbar { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
.kb-tip { font-size: 12px; color: #8bafc9; margin-left: auto; }

/* Training tab */
.train-header { display: flex; align-items: center; gap: 24px; margin-bottom: 14px;
  background: #0d1b2e; border: 1px solid #1a3a5c; border-radius: 10px; padding: 14px 20px; box-shadow: 0 2px 8px rgba(0,0,0,0.2); }
.train-val { font-size: 22px; font-weight: 700; }
.train-lbl { font-size: 11px; color: #8bafc9; margin-top: 2px; }
.train-tip { margin-top: 12px; font-size: 12px; color: #8bafc9; padding: 8px 12px;
  background: #0f2540; border-radius: 8px; border-left: 3px solid #00d4ff; }

/* Table override */
:deep(.el-table) { background: #0d1b2e; }
:deep(.el-table tr) { background: #0d1b2e; }
:deep(.el-table--enable-row-hover .el-table__body tr:hover td) { background: rgba(0,212,255,0.1) !important; }

/* Responsive: small screens stack vertically */
@media (max-width: 1024px) {
  .brain-layout { grid-template-columns: 1fr; }
  .brain-left { max-height: 400px; }
  .log-scroll { height: 300px; }
  .right-tools-row { grid-template-columns: 1fr; }
}
</style>
