<template>
  <div class="ai-analysis">
    <div class="ai-header-card">
      <div class="ai-title-area">
        <div class="ai-icon">🤖</div>
        <div>
          <div class="ai-title">Claude AI 智能分析助手</div>
          <div class="ai-subtitle">基于大模型的建筑能效智能诊断与优化建议系统</div>
        </div>
      </div>
      <el-tag :type="apiStatus === 'connected' ? 'success' : 'warning'" effect="dark">
        {{ apiStatus === 'connected' ? 'AI 已连接' : '本地分析模式' }}
      </el-tag>
    </div>

    <div class="main-grid">
      <div class="chat-panel">
        <div class="chat-messages" ref="chatRef">
          <div v-for="(msg, i) in messages" :key="i" class="message" :class="msg.role">
            <div class="msg-avatar">
              {{ msg.role === 'user' ? '👤' : '🤖' }}
            </div>
            <div class="msg-bubble">
              <div class="msg-content" v-html="formatMessage(msg.content)"></div>
              <div class="msg-time">{{ msg.time }}</div>
            </div>
          </div>
          <div v-if="aiTyping" class="message assistant">
            <div class="msg-avatar">🤖</div>
            <div class="msg-bubble">
              <div class="typing-indicator">
                <span></span><span></span><span></span>
              </div>
            </div>
          </div>
        </div>

        <div class="input-area">
          <div class="quick-queries">
            <span class="quick-label">快捷问题：</span>
            <el-button v-for="q in quickQueries" :key="q" size="small" plain @click="sendQuickQuery(q)">
              {{ q }}
            </el-button>
          </div>
          <div class="input-row">
            <el-select v-model="selectedBuilding" placeholder="选择建筑" style="width:140px" size="large">
              <el-option v-for="id in buildingIds" :key="id" :label="id" :value="id" />
            </el-select>
            <el-input v-model="userInput" placeholder="输入问题，例如：查询建筑A的能耗异常原因..."
              size="large" @keyup.enter="sendMessage" :disabled="aiTyping" style="flex:1" />
            <el-button type="primary" size="large" @click="sendMessage" :loading="aiTyping" icon="Promotion">
              发送
            </el-button>
          </div>
        </div>
      </div>

      <div class="side-panel">
        <div class="analysis-card">
          <div class="card-header">
            <span class="card-title">快速诊断</span>
          </div>
          <div class="diagnosis-list">
            <div class="diag-item" v-for="item in diagnosisItems" :key="item.title"
                 @click="runDiagnosis(item)">
              <div class="diag-icon" :style="{ color: item.color }">
                <el-icon :size="18"><component :is="item.icon" /></el-icon>
              </div>
              <div class="diag-info">
                <div class="diag-title">{{ item.title }}</div>
                <div class="diag-desc">{{ item.desc }}</div>
              </div>
              <el-icon style="color:#8bafc9"><ArrowRight /></el-icon>
            </div>
          </div>
        </div>

        <div class="analysis-card">
          <div class="card-header"><span class="card-title">能效评估结果</span></div>
          <div class="eval-chart">
            <v-chart :option="radarOption" style="height:220px" autoresize />
          </div>
        </div>

        <div class="analysis-card">
          <div class="card-header"><span class="card-title">节能建议</span></div>
          <div class="suggestion-list">
            <div class="suggestion-item" v-for="s in suggestions" :key="s.title">
              <div class="sug-badge" :class="'priority-' + s.priority">{{ s.priority === 'high' ? '高' : s.priority === 'medium' ? '中' : '低' }}</div>
              <div class="sug-content">
                <div class="sug-title">{{ s.title }}</div>
                <div class="sug-saving">预计节省 {{ s.saving }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { RadarChart } from 'echarts/charts'
import { RadarComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { buildingApi, analysisApi } from '../api/index.js'
import dayjs from 'dayjs'

use([RadarChart, RadarComponent, TooltipComponent, LegendComponent, CanvasRenderer])

const buildingIds = ref([])
const selectedBuilding = ref('B001')
const userInput = ref('')
const aiTyping = ref(false)
const apiStatus = ref('local')
const chatRef = ref(null)

const messages = ref([
  {
    role: 'assistant',
    content: '您好！我是建筑能源数字孪生系统的AI分析助手。我可以帮您：\n\n1. **分析建筑能耗异常原因**\n2. **提供空调系统优化建议**\n3. **生成能效改进方案**\n4. **预测未来能耗趋势**\n\n请选择建筑并输入您的问题，或点击快捷问题按钮开始分析。',
    time: dayjs().format('HH:mm:ss')
  }
])

const quickQueries = [
  '分析本月能耗趋势',
  '空调系统异常诊断',
  '提供节能优化建议',
  '碳排放超标原因'
]

const diagnosisItems = [
  { title: '能耗异常检测', desc: '自动识别超标用能时段', icon: 'Warning', color: '#ff6b6b' },
  { title: '空调效率分析', desc: '评估COP值与优化空间', icon: 'Wind', color: '#00d4ff' },
  { title: '碳排放预测', desc: '30天碳排放趋势预测', icon: 'Cloudy', color: '#52c41a' },
  { title: '设备故障预测', desc: '基于运行数据的预测维护', icon: 'Tools', color: '#fadb14' }
]

const suggestions = ref([
  { title: '调整空调设定温度至26°C', saving: '15% 电力', priority: 'high' },
  { title: '安装智能楼宇控制系统', saving: '20% 综合能耗', priority: 'high' },
  { title: '更换老化空调机组', saving: '12% 空调能耗', priority: 'medium' },
  { title: '优化照明自动控制策略', saving: '8% 照明能耗', priority: 'medium' },
  { title: '引入光伏发电系统', saving: '25% 电网用电', priority: 'low' }
])

const radarOption = {
  backgroundColor: 'transparent',
  tooltip: { backgroundColor: '#0d1b2e', borderColor: '#1a3a5c', textStyle: { color: '#e0f0ff' } },
  radar: {
    indicator: [
      { name: '电力效率', max: 100 },
      { name: '水资源利用', max: 100 },
      { name: '碳排放控制', max: 100 },
      { name: '设备健康度', max: 100 },
      { name: '智能化水平', max: 100 },
      { name: '可再生能源', max: 100 }
    ],
    axisLine: { lineStyle: { color: '#1a3a5c' } },
    splitLine: { lineStyle: { color: '#1a3a5c' } },
    name: { color: '#8bafc9', fontSize: 11 }
  },
  series: [{
    type: 'radar',
    data: [{
      value: [78, 65, 72, 88, 55, 42],
      name: '当前评分',
      itemStyle: { color: '#00d4ff' },
      areaStyle: { color: '#00d4ff22' },
      lineStyle: { color: '#00d4ff' }
    }]
  }]
}

function formatMessage(content) {
  return content
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/\n/g, '<br>')
    .replace(/^\d+\. /gm, '<span style="color:#00d4ff">▸</span> ')
}

async function sendMessage() {
  if (!userInput.value.trim() || aiTyping.value) return
  const query = userInput.value.trim()
  messages.value.push({ role: 'user', content: query, time: dayjs().format('HH:mm:ss') })
  userInput.value = ''
  aiTyping.value = true
  await scrollToBottom()

  try {
    const res = await analysisApi.aiQuery(selectedBuilding.value, query, '')
    if (res.code === 200) {
      messages.value.push({ role: 'assistant', content: res.data.result, time: dayjs().format('HH:mm:ss') })
    }
  } catch {
    messages.value.push({
      role: 'assistant',
      content: '抱歉，AI服务暂时不可用。请确认后端服务已启动，或检查Claude API密钥配置。\n\n您可以前往 `backend/src/main/resources/application.properties` 配置 `claude.api.key`。',
      time: dayjs().format('HH:mm:ss')
    })
  } finally {
    aiTyping.value = false
    await scrollToBottom()
  }
}

async function sendQuickQuery(query) {
  userInput.value = query
  await sendMessage()
}

async function runDiagnosis(item) {
  userInput.value = `请对建筑${selectedBuilding.value}进行${item.title}，提供详细分析结果和改进建议。`
  await sendMessage()
}

async function scrollToBottom() {
  await nextTick()
  if (chatRef.value) chatRef.value.scrollTop = chatRef.value.scrollHeight
}

onMounted(async () => {
  try {
    const res = await buildingApi.getAllBuildingIds()
    if (res.code === 200) {
      buildingIds.value = res.data
      if (res.data.length > 0) selectedBuilding.value = res.data[0]
    }
  } catch {
    buildingIds.value = ['B001', 'B002', 'B003', 'B004']
  }
})
</script>

<style scoped>
.ai-analysis { display: flex; flex-direction: column; gap: 16px; }

.ai-header-card {
  background: linear-gradient(135deg, #0d1b2e, #0a2a4a);
  border: 1px solid #00d4ff33;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.ai-title-area { display: flex; align-items: center; gap: 16px; }
.ai-icon { font-size: 36px; }
.ai-title { font-size: 18px; font-weight: 700; color: #00d4ff; }
.ai-subtitle { font-size: 13px; color: #8bafc9; margin-top: 4px; }

.main-grid { display: grid; grid-template-columns: 1fr 340px; gap: 16px; min-height: 600px; }

.chat-panel {
  background: #0d1b2e;
  border: 1px solid #1a3a5c;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  min-height: 400px;
  max-height: 500px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.message { display: flex; gap: 10px; align-items: flex-start; }
.message.user { flex-direction: row-reverse; }

.msg-avatar { font-size: 24px; flex-shrink: 0; }

.msg-bubble {
  max-width: 80%;
  background: #0a1628;
  border: 1px solid #1a3a5c;
  border-radius: 12px;
  padding: 12px 14px;
}

.message.user .msg-bubble {
  background: #0066cc22;
  border-color: #0066cc44;
}

.msg-content { color: #e0f0ff; font-size: 13px; line-height: 1.7; }
.msg-content :deep(strong) { color: #00d4ff; }
.msg-time { font-size: 10px; color: #8bafc9; margin-top: 6px; }

.typing-indicator { display: flex; gap: 4px; padding: 4px 0; }
.typing-indicator span {
  width: 8px; height: 8px;
  border-radius: 50%;
  background: #00d4ff;
  animation: bounce 1.4s infinite;
}
.typing-indicator span:nth-child(2) { animation-delay: 0.2s; }
.typing-indicator span:nth-child(3) { animation-delay: 0.4s; }
@keyframes bounce {
  0%, 60%, 100% { transform: translateY(0); }
  30% { transform: translateY(-6px); }
}

.input-area {
  border-top: 1px solid #1a3a5c;
  padding: 12px 16px;
}

.quick-queries {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 10px;
}

.quick-label { font-size: 12px; color: #8bafc9; }

.input-row { display: flex; gap: 8px; }

.side-panel { display: flex; flex-direction: column; gap: 12px; }

.analysis-card {
  background: #0d1b2e;
  border: 1px solid #1a3a5c;
  border-radius: 12px;
  padding: 14px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.card-title { font-size: 14px; font-weight: 600; color: #e0f0ff; }

.diagnosis-list { display: flex; flex-direction: column; gap: 8px; }
.diag-item {
  display: flex; align-items: center; gap: 10px;
  background: #0a1628; border-radius: 8px; padding: 10px;
  cursor: pointer; transition: border-color 0.2s;
  border: 1px solid transparent;
}
.diag-item:hover { border-color: #00d4ff44; }
.diag-icon { flex-shrink: 0; }
.diag-info { flex: 1; }
.diag-title { font-size: 13px; color: #e0f0ff; font-weight: 600; }
.diag-desc { font-size: 11px; color: #8bafc9; margin-top: 2px; }

.suggestion-list { display: flex; flex-direction: column; gap: 8px; }
.suggestion-item {
  display: flex; align-items: center; gap: 10px;
  background: #0a1628; border-radius: 8px; padding: 8px 10px;
}

.sug-badge {
  font-size: 11px; font-weight: 700;
  padding: 2px 6px; border-radius: 4px; flex-shrink: 0;
}
.priority-high { background: #ff6b6b22; color: #ff6b6b; }
.priority-medium { background: #fadb1422; color: #fadb14; }
.priority-low { background: #52c41a22; color: #52c41a; }

.sug-title { font-size: 12px; color: #e0f0ff; }
.sug-saving { font-size: 11px; color: #52c41a; margin-top: 2px; }
</style>
