<template>
  <div class="pending-page">
    <div class="pending-card">
      <div class="pending-icon">⏳</div>
      <h2>注册申请已提交</h2>
      <p class="pending-desc">您的注册信息已提交，请等待管理员审批后即可登录使用系统。</p>
      <div class="pending-info" v-if="name || eid">
        <div class="pi-row" v-if="name"><span class="pi-label">姓名</span><span class="pi-val">{{ name }}</span></div>
        <div class="pi-row" v-if="eid"><span class="pi-label">工号</span><span class="pi-val">{{ eid }}</span></div>
        <div class="pi-row"><span class="pi-label">状态</span><span class="pi-val" style="color:#faad14">待审批</span></div>
      </div>
      <div class="pending-tips">
        <div class="tip-title">📋 审批说明</div>
        <ul>
          <li>管理员将在 1-2 个工作日内完成审批</li>
          <li>审批通过后您可以使用注册的手机号和密码登录</li>
          <li>如有疑问请联系系统管理员</li>
        </ul>
      </div>
      <el-button type="primary" round size="large" @click="$router.push('/login')" style="width:100%;margin-top:20px">
        返回登录
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { useRoute } from 'vue-router'
const route = useRoute()
const name = route.query.name || ''
const eid = route.query.eid || ''
</script>

<style scoped>
.pending-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #0a1628 0%, #0f2540 50%, #080f1e 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}
.pending-card {
  background: rgba(13,27,46,0.95);
  border: 1px solid #1a3a5c;
  border-radius: 16px;
  padding: 48px 40px;
  width: 100%;
  max-width: 440px;
  text-align: center;
  box-shadow: 0 20px 60px rgba(0,0,0,0.3);
}
.pending-icon { font-size: 56px; margin-bottom: 16px; }
h2 { color: #e0f0ff; font-size: 22px; font-weight: 700; margin: 0 0 12px; }
.pending-desc { color: #8bafc9; font-size: 14px; line-height: 1.6; margin-bottom: 20px; }
.pending-info {
  background: #0f2540;
  border: 1px solid #1a3a5c;
  border-radius: 10px;
  padding: 14px 18px;
  margin-bottom: 20px;
  text-align: left;
}
.pi-row {
  display: flex;
  justify-content: space-between;
  padding: 6px 0;
  font-size: 13px;
}
.pi-label { color: #5a7a9a; }
.pi-val { color: #e0f0ff; font-weight: 600; }
.pending-tips {
  text-align: left;
  background: rgba(250,173,20,0.06);
  border: 1px solid rgba(250,173,20,0.2);
  border-radius: 10px;
  padding: 14px 18px;
}
.tip-title { color: #faad14; font-size: 13px; font-weight: 600; margin-bottom: 8px; }
.pending-tips ul { margin: 0; padding-left: 18px; }
.pending-tips li { color: #8bafc9; font-size: 12px; line-height: 1.8; }
</style>
