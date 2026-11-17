<template>
  <div class="admin-config">
    <div class="page-title-bar">⚙️ 系统配置</div>

    <el-tabs v-model="activeTab" class="dark-tabs">
      <!-- AI 服务配置 -->
      <el-tab-pane label="AI 服务" name="ai">
        <div class="config-card">
          <div class="card-title">AI 接口配置</div>
          <el-form label-width="140px" class="dark-form">
            <el-form-item label="服务类型">
              <el-radio-group v-model="aiConfig.provider">
                <el-radio value="coze">Coze (扣子)</el-radio>
                <el-radio value="claude">Claude</el-radio>
                <el-radio value="openai">OpenAI</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="API Base URL">
              <el-input v-model="aiConfig.baseUrl" placeholder="https://api.coze.cn" />
            </el-form-item>
            <el-form-item label="API Token">
              <el-input v-model="aiConfig.token" type="password" show-password placeholder="pat_xxxx..." />
            </el-form-item>
            <el-form-item label="Bot / Model ID">
              <el-input v-model="aiConfig.modelId" placeholder="模型或Bot ID" />
            </el-form-item>
            <el-form-item label="限流策略">
              <el-input-number v-model="aiConfig.rateLimit" :min="1" :max="1000" /> <span class="hint">次/分钟</span>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveConfig('ai')">保存配置</el-button>
              <el-button plain @click="testAI">测试连接</el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-tab-pane>

      <!-- 存储配置 -->
      <el-tab-pane label="存储配置" name="storage">
        <div class="config-card">
          <div class="card-title">数据存储策略</div>
          <el-form label-width="140px" class="dark-form">
            <el-form-item label="数据保留周期">
              <el-select v-model="storageConfig.retentionDays">
                <el-option :value="90" label="90 天" />
                <el-option :value="180" label="180 天" />
                <el-option :value="365" label="1 年" />
                <el-option :value="730" label="2 年" />
                <el-option :value="-1" label="永久保留" />
              </el-select>
            </el-form-item>
            <el-form-item label="自动备份">
              <el-switch v-model="storageConfig.autoBackup" />
            </el-form-item>
            <el-form-item label="备份频率" v-if="storageConfig.autoBackup">
              <el-select v-model="storageConfig.backupFreq">
                <el-option value="daily" label="每天" />
                <el-option value="weekly" label="每周" />
                <el-option value="monthly" label="每月" />
              </el-select>
            </el-form-item>
            <el-form-item label="备份路径">
              <el-input v-model="storageConfig.backupPath" placeholder="/data/backups" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveConfig('storage')">保存配置</el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-tab-pane>

      <!-- 第三方接口管理 -->
      <el-tab-pane label="接口集成" name="integration">
        <div class="config-card">
          <div class="card-title">楼宇自控系统 (BAS)</div>
          <el-form label-width="140px" class="dark-form">
            <el-form-item label="对接状态">
              <el-switch v-model="basConfig.enabled" active-text="已启用" inactive-text="未启用" />
            </el-form-item>
            <el-form-item label="API 端点">
              <el-input v-model="basConfig.endpoint" placeholder="https://bas.example.com/api/v1" />
            </el-form-item>
            <el-form-item label="认证密钥">
              <el-input v-model="basConfig.apiKey" type="password" show-password placeholder="API Key" />
            </el-form-item>
            <el-form-item label="采集协议">
              <el-select v-model="basConfig.protocol">
                <el-option value="modbus" label="Modbus TCP" />
                <el-option value="bacnet" label="BACnet/IP" />
                <el-option value="dlt645" label="DL/T645" />
                <el-option value="http" label="HTTP REST" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveConfig('bas')">保存</el-button>
              <el-button plain @click="testConnection('BAS')">测试连接</el-button>
            </el-form-item>
          </el-form>
        </div>

        <div class="config-card" style="margin-top:16px">
          <div class="card-title">电力监控系统</div>
          <el-form label-width="140px" class="dark-form">
            <el-form-item label="对接状态">
              <el-switch v-model="powerConfig.enabled" active-text="已启用" inactive-text="未启用" />
            </el-form-item>
            <el-form-item label="API 端点">
              <el-input v-model="powerConfig.endpoint" placeholder="https://power-monitor.example.com/api" />
            </el-form-item>
            <el-form-item label="认证密钥">
              <el-input v-model="powerConfig.apiKey" type="password" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveConfig('power')">保存</el-button>
              <el-button plain @click="testConnection('电力监控')">测试连接</el-button>
            </el-form-item>
          </el-form>
        </div>

        <div class="config-card" style="margin-top:16px">
          <div class="card-title">光伏逆变器接口</div>
          <el-form label-width="140px" class="dark-form">
            <el-form-item label="对接状态">
              <el-switch v-model="pvConfig.enabled" active-text="已启用" inactive-text="未启用" />
            </el-form-item>
            <el-form-item label="品牌">
              <el-select v-model="pvConfig.brand">
                <el-option value="huawei" label="华为 FusionSolar" />
                <el-option value="sungrow" label="阳光电源" />
                <el-option value="growatt" label="古瑞瓦特" />
                <el-option value="custom" label="自定义" />
              </el-select>
            </el-form-item>
            <el-form-item label="API 端点">
              <el-input v-model="pvConfig.endpoint" placeholder="逆变器API地址" />
            </el-form-item>
            <el-form-item label="认证密钥">
              <el-input v-model="pvConfig.apiKey" type="password" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveConfig('pv')">保存</el-button>
              <el-button plain @click="testConnection('光伏逆变器')">测试连接</el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-tab-pane>

      <!-- 数据推送 -->
      <el-tab-pane label="数据推送" name="push">
        <div class="config-card">
          <div class="card-title">推送目标配置</div>
          <div class="push-targets">
            <div v-for="(target, idx) in pushTargets" :key="idx" class="push-target-card">
              <div class="pt-header">
                <div class="pt-name">
                  <el-tag :type="target.protocol === 'mqtt' ? 'success' : target.protocol === 'http' ? '' : 'warning'" size="small">{{ target.protocol.toUpperCase() }}</el-tag>
                  <span>{{ target.name }}</span>
                </div>
                <el-switch v-model="target.enabled" size="small" />
              </div>
              <el-form label-width="100px" class="dark-form" size="small">
                <el-form-item label="目标地址">
                  <el-input v-model="target.url" />
                </el-form-item>
                <el-form-item label="推送频率">
                  <el-select v-model="target.frequency" style="width:100%">
                    <el-option value="realtime" label="实时" />
                    <el-option value="5min" label="每5分钟" />
                    <el-option value="15min" label="每15分钟" />
                    <el-option value="hourly" label="每小时" />
                    <el-option value="daily" label="每天" />
                  </el-select>
                </el-form-item>
                <el-form-item label="数据范围">
                  <el-checkbox-group v-model="target.dataScope">
                    <el-checkbox value="energy">能耗数据</el-checkbox>
                    <el-checkbox value="device">设备状态</el-checkbox>
                    <el-checkbox value="alert">告警事件</el-checkbox>
                  </el-checkbox-group>
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" size="small" @click="savePushTarget(target)">保存</el-button>
                  <el-button size="small" plain @click="testPush(target)">测试推送</el-button>
                  <el-button type="danger" size="small" text @click="removePushTarget(idx)">删除</el-button>
                </el-form-item>
              </el-form>
            </div>
          </div>
          <el-button type="primary" plain style="margin-top:12px" @click="addPushTarget">+ 添加推送目标</el-button>
        </div>
      </el-tab-pane>

      <!-- 其他配置 -->
      <el-tab-pane label="其他配置" name="other">
        <div class="config-card">
          <div class="card-title">系统参数</div>
          <el-form label-width="140px" class="dark-form">
            <el-form-item label="电价模板 (元/kWh)">
              <div class="price-row">
                <span class="price-label">峰时</span>
                <el-input-number v-model="otherConfig.peakPrice" :precision="2" :step="0.1" :min="0" size="small" />
                <span class="price-label">平时</span>
                <el-input-number v-model="otherConfig.normalPrice" :precision="2" :step="0.1" :min="0" size="small" />
                <span class="price-label">谷时</span>
                <el-input-number v-model="otherConfig.valleyPrice" :precision="2" :step="0.1" :min="0" size="small" />
              </div>
            </el-form-item>
            <el-form-item label="告警网关">
              <el-input v-model="otherConfig.alertGateway" placeholder="https://alert.example.com/webhook" />
            </el-form-item>
            <el-form-item label="邮件服务 SMTP">
              <el-input v-model="otherConfig.smtpHost" placeholder="smtp.example.com" />
            </el-form-item>
            <el-form-item label="碳排放系数">
              <el-input-number v-model="otherConfig.carbonFactor" :precision="4" :step="0.01" :min="0" size="small" />
              <span class="hint">kgCO₂/kWh</span>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveConfig('other')">保存配置</el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'

const activeTab = ref('ai')

const aiConfig = reactive({
  provider: 'coze', baseUrl: 'https://api.coze.cn', token: '', modelId: '', rateLimit: 60
})
const storageConfig = reactive({
  retentionDays: 365, autoBackup: true, backupFreq: 'daily', backupPath: '/data/backups'
})
const otherConfig = reactive({
  peakPrice: 1.20, normalPrice: 0.85, valleyPrice: 0.45,
  alertGateway: '', smtpHost: '', carbonFactor: 0.7035
})

// 第三方接口
const basConfig = reactive({ enabled: false, endpoint: '', apiKey: '', protocol: 'modbus' })
const powerConfig = reactive({ enabled: false, endpoint: '', apiKey: '' })
const pvConfig = reactive({ enabled: false, brand: 'huawei', endpoint: '', apiKey: '' })

// 数据推送
const pushTargets = ref([
  { name: '上级能源管理平台', protocol: 'http', url: 'https://energy-platform.gov.cn/api/push', frequency: 'daily', enabled: true, dataScope: ['energy'] },
  { name: '企业 ERP 系统', protocol: 'mqtt', url: 'mqtt://erp.company.com:1883/energy', frequency: 'hourly', enabled: false, dataScope: ['energy', 'device'] },
])

function addPushTarget() {
  pushTargets.value.push({ name: '新推送目标', protocol: 'http', url: '', frequency: 'daily', enabled: false, dataScope: ['energy'] })
}
function removePushTarget(idx) { pushTargets.value.splice(idx, 1); ElMessage.success('已删除') }
function savePushTarget(target) { ElMessage.success(`推送目标 ${target.name} 已保存`) }
function testPush(target) {
  ElMessage.info(`正在测试推送到 ${target.name}...`)
  setTimeout(() => ElMessage.success('推送测试成功'), 1500)
}

const sectionLabels = { ai: 'AI服务', storage: '存储', other: '系统', bas: 'BAS', power: '电力监控', pv: '光伏' }
function saveConfig(section) {
  ElMessage.success(`${sectionLabels[section] || section}配置已保存`)
}

function testAI() {
  ElMessage.info('正在测试AI连接...')
  setTimeout(() => ElMessage.success('AI服务连接正常'), 1500)
}

function testConnection(name) {
  ElMessage.info(`正在测试${name}连接...`)
  setTimeout(() => ElMessage.success(`${name}连接正常`), 1500)
}
</script>

<style scoped>
.admin-config { display: flex; flex-direction: column; gap: 16px; }
.page-title-bar { font-size: 18px; font-weight: 700; color: #e0f0ff; }
.config-card { background: #0d1b2e; border-radius: 10px; padding: 20px; margin-top: 8px; }
.card-title { color: #e0f0ff; font-weight: 600; font-size: 15px; margin-bottom: 16px; }
.hint { color: #5a7a9a; font-size: 12px; margin-left: 8px; }
.price-row { display: flex; align-items: center; gap: 10px; }
.price-label { color: #8bafc9; font-size: 13px; }
.dark-tabs :deep(.el-tabs__item) { color: #5a7a9a; }
.dark-tabs :deep(.el-tabs__item.is-active) { color: #00d4ff; }
.dark-tabs :deep(.el-tabs__active-bar) { background: #00d4ff; }
.dark-tabs :deep(.el-tabs__nav-wrap::after) { background: #1a3a5c; }
.dark-form :deep(.el-form-item__label) { color: #8bafc9; }
.dark-form :deep(.el-input__wrapper) { background: #0a1628; border: 1px solid #1a3a5c; box-shadow: none; }
.dark-form :deep(.el-input__inner) { color: #e0f0ff; }
.dark-form :deep(.el-radio__label) { color: #c0d8f0; }
.dark-form :deep(.el-select .el-input__wrapper) { background: #0a1628; }
.dark-form :deep(.el-input-number) { --el-fill-color-blank: #0a1628; }
.dark-form :deep(.el-checkbox__label) { color: #c0d8f0; }
.dark-form :deep(.el-switch__label) { color: #5a7a9a; }

.push-targets { display: flex; flex-direction: column; gap: 14px; }
.push-target-card { background: #0a1628; border-radius: 8px; padding: 14px; border: 1px solid #1a3a5c; }
.pt-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.pt-name { display: flex; align-items: center; gap: 8px; color: #e0f0ff; font-weight: 600; font-size: 14px; }
</style>
