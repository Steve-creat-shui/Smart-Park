<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="auth-header">
        <div class="logo">
          <el-icon size="40" color="#00d4ff"><Lightning /></el-icon>
        </div>
        <h2>鼎筑——建筑能源智能管理系统</h2>
        <p>请选择登录身份</p>
      </div>

      <!-- 身份选择 -->
      <div class="portal-tabs">
        <div v-for="p in portals" :key="p.value" class="portal-tab" :class="{ active: portal === p.value }" @click="portal = p.value">
          <span class="pt-icon">{{ p.icon }}</span>
          <span class="pt-label">{{ p.label }}</span>
          <span class="pt-desc">{{ p.desc }}</span>
        </div>
      </div>

      <el-form :model="form" :rules="rules" ref="formRef" size="large">
        <el-form-item prop="phone">
          <el-input
            v-model="form.phone"
            placeholder="请输入手机号"
            prefix-icon="Phone"
            maxlength="11"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            prefix-icon="Lock"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            class="submit-btn"
            :loading="loading"
            @click="handleLogin"
          >
            登 录
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 状态提示 -->
      <div v-if="statusAlert" class="status-alert" :class="statusAlert.type">
        <span class="sa-icon">{{ statusAlert.icon }}</span>
        <div class="sa-body">
          <div class="sa-title">{{ statusAlert.title }}</div>
          <div class="sa-msg">{{ statusAlert.msg }}</div>
        </div>
      </div>

      <div class="auth-footer">
        还没有账号？
        <router-link to="/register">立即注册</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Lightning } from '@element-plus/icons-vue'
import { authApi } from '../api/index.js'
import { useAuthStore } from '../stores/auth.js'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const formRef = ref(null)
const loading = ref(false)
const statusAlert = ref(null)

const portal = ref(route.query.portal || 'enterprise')
const portals = [
  { value: 'user', icon: '👤', label: '个人用户', desc: '查看个人能耗数据' },
  { value: 'enterprise', icon: '🏢', label: '企业用户', desc: '全功能业务管理' },
  { value: 'admin', icon: '🛡️', label: '系统管理员', desc: '用户与系统管理' }
]
const form = reactive({ phone: '', password: '' })

const rules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ]
}

async function handleLogin() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  statusAlert.value = null
  try {
    const res = await authApi.login({ phone: form.phone, password: form.password })
    if (res.code === 200) {
      authStore.setAuth(res.data)
      ElMessage.success(`欢迎回来，${res.data.nickname}`)
      // 角色不匹配时提示
      if (res.data.role !== portal.value) {
        ElMessage.warning(`您的账号身份为「${{admin:'管理员',enterprise:'企业用户',user:'个人用户'}[res.data.role]}」，已自动切换`)
      }
      // 登录后跳转到角色对应的落地页
      const role = res.data.role
      // 无数据用户 → 引导导入
      if ((role === 'enterprise' || role === 'user') && res.data.hasData === false) {
        router.push('/data-management')
      } else {
        const landing = { admin: '/admin', enterprise: '/home', user: '/dashboard' }
        const defaultPath = landing[role] || '/dashboard'
        const redirect = route.query.redirect || defaultPath
        router.push(redirect)
      }
    } else {
      handleLoginError(res.message || '登录失败')
    }
  } catch (e) {
    const msg = e.response?.data?.message || ''
    if (msg) handleLoginError(msg)
  } finally {
    loading.value = false
  }
}

function handleLoginError(msg) {
  if (msg.startsWith('PENDING:')) {
    statusAlert.value = { type: 'pending', icon: '⏳', title: '账号审核中', msg: msg.replace('PENDING:', '') }
  } else if (msg.startsWith('REJECTED:')) {
    statusAlert.value = { type: 'rejected', icon: '❌', title: '申请被拒绝', msg: msg.replace('REJECTED:', '') }
  } else if (msg.startsWith('DISABLED:')) {
    statusAlert.value = { type: 'disabled', icon: '🚫', title: '账号已禁用', msg: msg.replace('DISABLED:', '') }
  } else {
    ElMessage.error(msg)
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #0a1628 0%, #0f2540 50%, #080f1e 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.auth-card {
  background: rgba(13,27,46,0.95);
  backdrop-filter: blur(10px);
  border: 1px solid #1a3a5c;
  border-radius: 16px;
  padding: 48px 40px;
  width: 100%;
  max-width: 520px;
  box-shadow: 0 20px 60px rgba(0,0,0,0.3);
}

/* 身份选择卡片 */
.portal-tabs {
  display: flex;
  gap: 10px;
  margin-bottom: 28px;
}
.portal-tab {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 14px 8px;
  border-radius: 12px;
  border: 1.5px solid #1a3a5c;
  background: rgba(26,58,92,0.15);
  cursor: pointer;
  transition: all 0.25s;
  text-align: center;
}
.portal-tab:hover {
  border-color: #00d4ff55;
  background: rgba(0,212,255,0.05);
}
.portal-tab.active {
  border-color: #00d4ff;
  background: rgba(0,212,255,0.1);
  box-shadow: 0 0 20px rgba(0,212,255,0.15);
}
.pt-icon { font-size: 24px; }
.pt-label {
  font-size: 13px;
  font-weight: 700;
  color: #8bafc9;
  transition: color 0.2s;
}
.portal-tab.active .pt-label { color: #00d4ff; }
.pt-desc {
  font-size: 10px;
  color: #5a7a9a;
  line-height: 1.3;
}

.auth-header {
  text-align: center;
  margin-bottom: 36px;
}

.logo {
  margin-bottom: 12px;
}

.auth-header h2 {
  color: #e0f0ff;
  font-size: 20px;
  font-weight: 700;
  margin: 0 0 8px;
}

.auth-header p {
  color: #8bafc9;
  font-size: 14px;
  margin: 0;
}

:deep(.el-input__wrapper) {
  background: rgba(26,58,92,0.3) !important;
  border: 1px solid #1a3a5c !important;
  box-shadow: none !important;
}

:deep(.el-input__wrapper:hover),
:deep(.el-input__wrapper.is-focus) {
  border-color: #00d4ff !important;
  box-shadow: 0 0 0 1px #00d4ff inset !important;
}

:deep(.el-input__inner) {
  color: #e0f0ff !important;
  background: transparent !important;
  -webkit-text-fill-color: #e0f0ff !important;
}

:deep(.el-input__inner:-webkit-autofill),
:deep(.el-input__inner:-webkit-autofill:hover),
:deep(.el-input__inner:-webkit-autofill:focus) {
  -webkit-box-shadow: 0 0 0 1000px rgba(26,58,92,0.6) inset !important;
  -webkit-text-fill-color: #e0f0ff !important;
  transition: background-color 5000s ease-in-out 0s;
}

:deep(.el-input__inner::placeholder) {
  color: #5a7a9a !important;
}

:deep(.el-input__prefix-inner .el-icon),
:deep(.el-input__suffix-inner .el-icon) {
  color: #5a7a9a !important;
}

.submit-btn {
  width: 100%;
  height: 46px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 8px;
  background: linear-gradient(90deg, #00d4ff, #00a8cc);
  border: none;
  color: #ffffff;
}

.submit-btn:hover {
  opacity: 0.9;
}

.auth-footer {
  text-align: center;
  margin-top: 24px;
  color: #8bafc9;
  font-size: 14px;
}

.auth-footer a {
  color: #00d4ff;
  text-decoration: none;
  font-weight: 600;
}

.auth-footer a:hover {
  text-decoration: underline;
}

.status-alert {
  display: flex; align-items: flex-start; gap: 10px;
  padding: 14px 16px; border-radius: 10px; margin-top: 16px;
}
.status-alert.pending { background: rgba(250,173,20,0.08); border: 1px solid rgba(250,173,20,0.3); }
.status-alert.rejected { background: rgba(255,77,79,0.08); border: 1px solid rgba(255,77,79,0.3); }
.status-alert.disabled { background: rgba(140,140,140,0.08); border: 1px solid rgba(140,140,140,0.3); }
.sa-icon { font-size: 22px; flex-shrink: 0; margin-top: 2px; }
.sa-title { font-size: 14px; font-weight: 700; margin-bottom: 4px; }
.sa-msg { font-size: 12px; line-height: 1.6; }
.status-alert.pending .sa-title { color: #faad14; }
.status-alert.pending .sa-msg { color: #d4a017; }
.status-alert.rejected .sa-title { color: #ff4d4f; }
.status-alert.rejected .sa-msg { color: #cf3e3e; }
.status-alert.disabled .sa-title { color: #8c8c8c; }
.status-alert.disabled .sa-msg { color: #666; }
</style>
