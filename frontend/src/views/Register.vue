<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="auth-header">
        <div class="logo">
          <el-icon size="40" color="#00d4ff"><Lightning /></el-icon>
        </div>
        <h2>鼎筑——建筑能源智能管理系统</h2>
        <p>注册新账号</p>
      </div>

      <el-steps :active="step" finish-status="success" align-center class="reg-steps">
        <el-step title="账号信息" />
        <el-step title="企业凭证" />
      </el-steps>

      <!-- Step 1: 基本信息 -->
      <el-form v-show="step === 0" :model="form" :rules="rules1" ref="formRef1" size="large">
        <el-form-item prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" prefix-icon="Phone" maxlength="11" />
        </el-form-item>
        <el-form-item prop="code">
          <div class="code-row">
            <el-input v-model="form.code" placeholder="请输入6位验证码" maxlength="6" style="flex:1" />
            <el-button type="primary" plain :disabled="countdown > 0 || !form.phone"
              :loading="codeSending" @click="sendCode" style="margin-left:10px; min-width:110px">
              {{ countdown > 0 ? `${countdown}s 后重发` : '获取验证码' }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="请设置密码（6-20位）"
            prefix-icon="Lock" show-password />
        </el-form-item>
        <el-form-item prop="nickname">
          <el-input v-model="form.nickname" placeholder="昵称（选填）" prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="requestedRole" label="注册身份">
          <el-radio-group v-model="form.requestedRole" style="width:100%">
            <el-radio value="user">个人用户<span style="color:#5a7a9a;font-size:12px;margin-left:4px">（个人能耗查看）</span></el-radio>
            <el-radio value="enterprise">企业用户<span style="color:#5a7a9a;font-size:12px;margin-left:4px">（完整业务功能）</span></el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="submit-btn" @click="nextStep">下一步 →</el-button>
        </el-form-item>
      </el-form>

      <!-- Step 2: 企业凭证 -->
      <el-form v-show="step === 1" :model="form" :rules="rules2" ref="formRef2" size="large">
        <el-form-item prop="realName">
          <el-input v-model="form.realName" placeholder="真实姓名" prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="employeeId">
          <el-input v-model="form.employeeId" placeholder="工号（如 EMP20240001）" prefix-icon="Postcard" />
        </el-form-item>
        <el-form-item prop="department">
          <el-select v-model="form.department" placeholder="所属部门" style="width:100%">
            <el-option v-for="d in departments" :key="d" :label="d" :value="d" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.position" placeholder="职位（选填）" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.engineerCert" placeholder="工程师资格证编号（选填）" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.companyEmail" placeholder="企业邮箱（选填）" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="备注说明（选填）" />
        </el-form-item>
        <el-form-item>
          <div style="display:flex;gap:12px;width:100%">
            <el-button style="flex:1" @click="step = 0">← 上一步</el-button>
            <el-button type="primary" style="flex:2" :loading="loading" @click="handleRegister">
              提交注册申请
            </el-button>
          </div>
        </el-form-item>
      </el-form>

      <div class="auth-footer">
        已有账号？
        <router-link to="/login">立即登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Lightning } from '@element-plus/icons-vue'
import { authApi } from '../api/index.js'

const router = useRouter()
const formRef1 = ref(null)
const formRef2 = ref(null)
const loading = ref(false)
const codeSending = ref(false)
const countdown = ref(0)
const step = ref(0)

const departments = ['工程部', '运维部', '管理部', '财务部', '研发部', '后勤部', '其他']

const form = reactive({
  phone: '', code: '', password: '', nickname: '', requestedRole: 'user',
  realName: '', employeeId: '', department: '', position: '',
  engineerCert: '', companyEmail: '', idCardLast4: '', remark: ''
})

const rules1 = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 6, message: '验证码为6位数字', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请设置密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度6-20位', trigger: 'blur' }
  ]
}

const rules2 = {
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  employeeId: [{ required: true, message: '请输入工号', trigger: 'blur' }],
  department: [{ required: true, message: '请选择部门', trigger: 'change' }]
}

async function sendCode() {
  if (!/^1[3-9]\d{9}$/.test(form.phone)) {
    ElMessage.warning('请先输入正确的手机号')
    return
  }
  codeSending.value = true
  try {
    const res = await authApi.sendCode(form.phone)
    if (res.code === 200) {
      ElMessage.success('验证码已发送，请查看后端控制台日志')
      countdown.value = 60
      const timer = setInterval(() => { countdown.value--; if (countdown.value <= 0) clearInterval(timer) }, 1000)
    } else {
      ElMessage.error(res.message)
    }
  } catch (e) {}
  finally { codeSending.value = false }
}

async function nextStep() {
  const valid = await formRef1.value.validate().catch(() => false)
  if (valid) step.value = 1
}

async function handleRegister() {
  const valid = await formRef2.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    const res = await authApi.register({ ...form })
    if (res.code === 200) {
      ElMessage.success('注册申请已提交！')
      router.push({ name: 'RegisterPending', query: { name: form.realName, eid: form.employeeId } })
    } else {
      ElMessage.error(res.message)
    }
  } catch (e) {}
  finally { loading.value = false }
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
  padding: 40px 36px;
  width: 100%;
  max-width: 480px;
  box-shadow: 0 20px 60px rgba(0,0,0,0.3);
}

.auth-header { text-align: center; margin-bottom: 24px; }
.logo { margin-bottom: 12px; }
.auth-header h2 { color: #e0f0ff; font-size: 20px; font-weight: 700; margin: 0 0 8px; }
.auth-header p { color: #8bafc9; font-size: 14px; margin: 0; }

.reg-steps { margin-bottom: 28px; }
.reg-steps :deep(.el-step__title) { color: #5a7a9a; font-size: 13px; }
.reg-steps :deep(.el-step__title.is-process) { color: #00d4ff; font-weight: 600; }
.reg-steps :deep(.el-step__title.is-success) { color: #52c41a; }

.code-row { display: flex; width: 100%; align-items: center; }

:deep(.el-input__wrapper) { background: #0f2540 !important; border: 1px solid #1a3a5c !important; box-shadow: none !important; }
:deep(.el-input__wrapper:hover), :deep(.el-input__wrapper.is-focus) {
  border-color: #00d4ff !important; box-shadow: 0 0 0 1px #00d4ff inset !important;
}
:deep(.el-input__inner) { color: #e0f0ff !important; background: transparent !important; -webkit-text-fill-color: #e0f0ff !important; }
:deep(.el-input__inner:-webkit-autofill),
:deep(.el-input__inner:-webkit-autofill:hover),
:deep(.el-input__inner:-webkit-autofill:focus) {
  -webkit-box-shadow: 0 0 0 1000px #0f2540 inset !important;
  -webkit-text-fill-color: #e0f0ff !important;
  transition: background-color 5000s ease-in-out 0s;
}
:deep(.el-input__inner::placeholder) { color: #5a7a9a; }
:deep(.el-input__prefix-inner .el-icon) { color: #5a7a9a; }
:deep(.el-textarea__inner) { background: #0f2540; border: 1px solid #1a3a5c; color: #e0f0ff; }
:deep(.el-textarea__inner:focus) { border-color: #00d4ff; }
:deep(.el-select .el-input__wrapper) { background: #0f2540; }

.submit-btn {
  width: 100%; height: 46px; font-size: 16px; font-weight: 600;
  border-radius: 8px; background: linear-gradient(90deg, #00d4ff, #00a8cc);
  border: none; color: #ffffff;
}

.auth-footer { text-align: center; margin-top: 24px; color: #8bafc9; font-size: 14px; }
.auth-footer a { color: #00d4ff; text-decoration: none; font-weight: 600; }
.auth-footer a:hover { text-decoration: underline; }
</style>
