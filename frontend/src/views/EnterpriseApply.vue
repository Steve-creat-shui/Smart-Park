<template>
  <div class="enterprise-apply-page">
    <div class="page-header">
      <h2>🏢 注册企业</h2>
      <p class="page-desc">注册企业后，您可以接入真实建筑和设备数据，使用平台全部功能</p>
    </div>

    <!-- 已有申请提示 -->
    <el-alert v-if="existingApp" :title="statusTitle" :type="statusType" show-icon :closable="false" class="existing-alert">
      <template #default>
        <div class="alert-detail">
          <p>企业名称：{{ existingApp.enterpriseName }}</p>
          <p>申请时间：{{ existingApp.createdAt }}</p>
          <p v-if="existingApp.rejectReason">驳回原因：{{ existingApp.rejectReason }}</p>
        </div>
      </template>
    </el-alert>

    <!-- 申请表单 -->
    <el-card v-if="!existingApp || existingApp.status === 'rejected'" class="form-card">
      <template #header>
        <span class="card-title">填写企业信息</span>
      </template>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px" size="large">
        <el-form-item label="企业名称" prop="enterpriseName">
          <el-input v-model="form.enterpriseName" placeholder="请输入企业全称" />
        </el-form-item>
        <el-form-item label="统一社会信用代码" prop="enterpriseCode">
          <el-input v-model="form.enterpriseCode" placeholder="选填" />
        </el-form-item>
        <el-form-item label="所属行业" prop="industry">
          <el-select v-model="form.industry" placeholder="请选择行业" style="width:100%">
            <el-option label="教育" value="教育" />
            <el-option label="医疗" value="医疗" />
            <el-option label="制造" value="制造" />
            <el-option label="商业办公" value="商业办公" />
            <el-option label="公共建筑" value="公共建筑" />
            <el-option label="住宅" value="住宅" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="联系人" prop="contactName">
          <el-input v-model="form.contactName" placeholder="请输入联系人姓名" />
        </el-form-item>
        <el-form-item label="联系电话" prop="contactPhone">
          <el-input v-model="form.contactPhone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="联系邮箱">
          <el-input v-model="form.contactEmail" placeholder="选填" />
        </el-form-item>
        <el-form-item label="企业地址">
          <el-input v-model="form.address" placeholder="选填" />
        </el-form-item>
        <el-form-item label="备注说明">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="选填，可描述您的使用场景和需求" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="submitForm" :loading="loading" size="large">
            提交注册申请
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 流程说明 -->
    <el-card class="flow-card">
      <template #header>
        <span class="card-title">审批流程</span>
      </template>
      <el-steps :active="stepActive" align-center>
        <el-step title="填写信息" description="填写企业基本信息" />
        <el-step title="提交申请" description="系统生成审批工单" />
        <el-step title="平台审核" description="管理员审核企业资质" />
        <el-step title="开通成功" description="自动升级为企业用户" />
      </el-steps>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { enterpriseApi } from '../api/index.js'

const formRef = ref(null)
const loading = ref(false)
const existingApp = ref(null)

const form = ref({
  enterpriseName: '',
  enterpriseCode: '',
  industry: '',
  contactName: '',
  contactPhone: '',
  contactEmail: '',
  address: '',
  description: ''
})

const rules = {
  enterpriseName: [{ required: true, message: '请输入企业名称', trigger: 'blur' }],
  industry: [{ required: true, message: '请选择行业', trigger: 'change' }],
  contactName: [{ required: true, message: '请输入联系人', trigger: 'blur' }],
  contactPhone: [{ required: true, message: '请输入联系电话', trigger: 'blur' }]
}

const statusTitle = computed(() => {
  if (!existingApp.value) return ''
  const s = existingApp.value.status
  if (s === 'pending') return '您已提交企业注册申请，正在等待平台审核...'
  if (s === 'approved') return '🎉 您的企业注册已通过！请重新登录进入企业端。'
  if (s === 'rejected') return '您的申请已被驳回，可修改后重新提交。'
  return ''
})

const statusType = computed(() => {
  if (!existingApp.value) return 'info'
  const s = existingApp.value.status
  return { pending: 'warning', approved: 'success', rejected: 'error' }[s] || 'info'
})

const stepActive = computed(() => {
  if (!existingApp.value) return 0
  const s = existingApp.value.status
  if (s === 'pending') return 2
  if (s === 'approved') return 4
  if (s === 'rejected') return 0
  return 0
})

onMounted(async () => {
  try {
    const res = await enterpriseApi.myApplications()
    const apps = res.data || []
    if (apps.length > 0) {
      existingApp.value = apps[0]
    }
  } catch (e) { /* ignore */ }
})

async function submitForm() {
  await formRef.value.validate()
  loading.value = true
  try {
    await enterpriseApi.apply(form.value)
    ElMessage.success('申请已提交，等待平台审核')
    // refresh
    const res = await enterpriseApi.myApplications()
    const apps = res.data || []
    if (apps.length > 0) existingApp.value = apps[0]
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '提交失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.enterprise-apply-page {
  max-width: 800px;
  margin: 0 auto;
}
.page-header {
  margin-bottom: 24px;
}
.page-header h2 {
  font-size: 22px;
  color: #e0f0ff;
  margin-bottom: 6px;
}
.page-desc {
  color: #8bafc9;
  font-size: 14px;
}
.existing-alert {
  margin-bottom: 20px;
}
.alert-detail p {
  margin: 4px 0;
  font-size: 13px;
}
.form-card, .flow-card {
  margin-bottom: 20px;
}
.card-title {
  font-weight: 600;
  color: #e0f0ff;
}
</style>
