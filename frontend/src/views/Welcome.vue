<template>
  <div class="welcome-page" ref="pageRef">
    <!-- 粒子背景 canvas -->
    <canvas ref="canvasRef" class="particle-canvas" />

    <!-- 顶部导航栏 -->
    <header class="welcome-nav">
      <div class="nav-brand">
        <span class="nav-logo">⚡</span>
        <span class="nav-title">鼎筑</span>
      </div>
      <div class="nav-right">
        <template v-if="isLoggedIn">
          <span class="nav-user">{{ authStore.user?.nickname }}</span>
          <el-button size="small" type="primary" round @click="enterSystem">进入系统</el-button>
        </template>
        <template v-else>
          <el-button size="small" plain round @click="router.push('/login')">登录</el-button>
          <el-button size="small" type="primary" round @click="router.push('/register')">注册</el-button>
        </template>
      </div>
    </header>

    <!-- Hero 区域 -->
    <section class="hero-section">
      <div class="hero-glow" />
      <h1 class="hero-title">
        <span class="hero-title-main">鼎筑</span>
        <span class="hero-title-sub">建筑能源智能管理系统</span>
      </h1>
      <p class="hero-desc">
        AI 驱动 · 实时监测 · 智能诊断 · 绿色节能
      </p>
      <p class="hero-sub">
        覆盖全校 7 栋建筑，集能耗分析、异常检测、设备监控、AI 智能诊断于一体的综合能源管理平台
      </p>
      <!-- 身份选择入口 -->
      <div class="portal-section">
        <div class="portal-card" v-for="p in portals" :key="p.value" @click="goLogin(p.value)">
          <div class="pc-icon">{{ p.icon }}</div>
          <div class="pc-title">{{ p.label }}</div>
          <div class="pc-desc">{{ p.desc }}</div>
          <div class="pc-features">
            <span v-for="f in p.features" :key="f">{{ f }}</span>
          </div>
          <button class="pc-btn" @click.stop="goLogin(p.value)">{{ isLoggedIn && authStore.user?.role === p.value ? '进入系统' : '登录' }}</button>
        </div>
      </div>

      <div class="hero-btns">
        <button class="hero-btn ghost" @click="scrollToFeatures">
          <span>了解更多</span>
          <span class="btn-arrow">↓</span>
        </button>
      </div>

      <!-- 数据指标浮动卡片 -->
      <div class="hero-metrics">
        <div class="hm-card" v-for="m in metrics" :key="m.label">
          <div class="hm-icon" :style="{color: m.color, background: m.color + '18'}">{{ m.icon }}</div>
          <div class="hm-body">
            <div class="hm-val" :style="{color: m.color}">{{ m.value }}</div>
            <div class="hm-label">{{ m.label }}</div>
          </div>
        </div>
      </div>
    </section>

    <!-- 功能特色区 -->
    <section class="features-section" ref="featuresRef">
      <div class="section-hdr">
        <span class="section-line" />
        <h2>核心功能</h2>
        <span class="section-line" />
      </div>
      <div class="features-grid">
        <div class="feature-card" v-for="f in features" :key="f.title"
             @click="goTo(f.path)">
          <div class="fc-icon" :style="{background: f.color + '15', color: f.color}">{{ f.icon }}</div>
          <div class="fc-title">{{ f.title }}</div>
          <div class="fc-desc">{{ f.desc }}</div>
          <div class="fc-arrow" :style="{color: f.color}">→</div>
        </div>
      </div>
    </section>

    <!-- 技术亮点区 -->
    <section class="tech-section">
      <div class="section-hdr">
        <span class="section-line" />
        <h2>技术架构</h2>
        <span class="section-line" />
      </div>
      <div class="tech-grid">
        <div class="tech-card" v-for="t in techStack" :key="t.name">
          <div class="tc-icon">{{ t.icon }}</div>
          <div class="tc-name">{{ t.name }}</div>
          <div class="tc-desc">{{ t.desc }}</div>
        </div>
      </div>
    </section>

    <!-- 页脚 -->
    <footer class="welcome-footer">
      <div class="footer-brand">⚡ 鼎筑建筑能源智能管理系统 v1.0</div>
      <div class="footer-sub">Vue 3 + Spring Boot + MySQL + ECharts + Coze AI</div>
      <div class="footer-copy">© 2026 鼎筑科技 · 服务外包创新创业大赛</div>
    </footer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth.js'

const router = useRouter()
const authStore = useAuthStore()

const pageRef = ref(null)
const canvasRef = ref(null)
const featuresRef = ref(null)
let animId = null

const isLoggedIn = computed(() => !!authStore.token)

const portals = [
  {
    value: 'user', icon: '👤', label: '个人用户',
    desc: '导入自有能耗数据，查看分析、设备状态、源芯问答',
    features: ['数据导入', '系统总览', '能耗分析', '设备状态', '源芯', '个人设置']
  },
  {
    value: 'enterprise', icon: '🏢', label: '企业用户',
    desc: '全功能业务管理，多模态监控、管理决策、报告生成',
    features: ['系统总览', '能耗分析', '多模态监控', 'AI 分析', '数据管理', '管理决策']
  },
  {
    value: 'admin', icon: '🛡️', label: '系统管理员',
    desc: '用户与内容管理、系统配置、运维保障',
    features: ['用户管理', '企业管理', '系统配置', '运维保障']
  }
]

const metrics = ref([
  { icon: '🏢', label: '监测建筑', value: '7 栋', color: '#00d4ff' },
  { icon: '⚡', label: '日均电耗', value: '2.4万 kWh', color: '#52c41a' },
  { icon: '🌿', label: '碳减排', value: '18.6%', color: '#fadb14' },
  { icon: '🤖', label: 'AI 诊断', value: '实时在线', color: '#8b5cf6' }
])

const features = [
  { icon: '📊', title: '系统总览', desc: '实时掌握全校能耗动态，KPI 指标一目了然', color: '#00d4ff', path: '/dashboard' },
  { icon: '🔍', title: '能耗分析', desc: '多维度深度分析，异常检测与智能诊断', color: '#ff6b6b', path: '/energy-analysis' },
  { icon: '📹', title: '多模态监控', desc: '融合视频与传感的智能安防与能耗联动', color: '#52c41a', path: '/multimodal' },
  { icon: '🤖', title: 'AI 智能分析', desc: 'Coze AI 驱动对话式能耗咨询与决策建议', color: '#8b5cf6', path: '/ai-analysis' },
  { icon: '📱', title: '设备监控', desc: '实时设备状态监测，故障预警与维护提醒', color: '#faad14', path: '/device-status' },
  { icon: '📋', title: '数据管理', desc: '批量导入导出，指标对比与数据面板', color: '#36cfc9', path: '/data-management' },
  { icon: '📄', title: '报告生成', desc: '自动生成建筑能效分析报告与导出', color: '#e6a23c', path: '/report' },
  { icon: '⚙️', title: '系统设置', desc: '个人偏好与系统参数配置', color: '#909399', path: '/settings' },
]

const techStack = [
  { icon: '🟢', name: 'Vue 3', desc: 'Composition API + Pinia 状态管理' },
  { icon: '☕', name: 'Spring Boot', desc: 'RESTful API + JPA 数据持久层' },
  { icon: '🗄️', name: 'MySQL', desc: '结构化能耗数据存储与查询优化' },
  { icon: '📈', name: 'ECharts', desc: '丰富的数据可视化图表与交互' },
  { icon: '🤖', name: 'Coze AI', desc: '大语言模型驱动智能分析对话' },
  { icon: '🎨', name: 'Element Plus', desc: '企业级 UI 组件库与暗色主题' }
]

function goLogin(portal) {
  if (isLoggedIn.value && authStore.user?.role === portal) {
    const landing = { admin: '/admin', enterprise: '/dashboard', user: '/dashboard' }
    return router.push(landing[portal] || '/dashboard')
  }
  router.push({ path: '/login', query: { portal } })
}
function enterSystem() {
  const role = authStore.user?.role || 'user'
  const landing = { admin: '/admin', enterprise: '/dashboard', user: '/dashboard' }
  router.push(landing[role] || '/dashboard')
}
function goTo(path) {
  if (isLoggedIn.value) {
    router.push(path)
  } else {
    router.push('/login')
  }
}
function scrollToFeatures() {
  featuresRef.value?.scrollIntoView({ behavior: 'smooth' })
}

// ── 粒子动画 ──
function initParticles() {
  const canvas = canvasRef.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  let w, h
  const particles = []
  const PARTICLE_COUNT = 80

  function resize() {
    w = canvas.width = canvas.offsetWidth
    h = canvas.height = canvas.offsetHeight
  }
  resize()
  window.addEventListener('resize', resize)

  for (let i = 0; i < PARTICLE_COUNT; i++) {
    particles.push({
      x: Math.random() * w,
      y: Math.random() * h,
      r: Math.random() * 2.5 + 0.8,
      vx: (Math.random() - 0.5) * 0.4,
      vy: (Math.random() - 0.5) * 0.4,
      opacity: Math.random() * 0.5 + 0.2
    })
  }

  function draw() {
    ctx.clearRect(0, 0, w, h)
    // 连线
    for (let i = 0; i < particles.length; i++) {
      for (let j = i + 1; j < particles.length; j++) {
        const dx = particles[i].x - particles[j].x
        const dy = particles[i].y - particles[j].y
        const dist = Math.sqrt(dx * dx + dy * dy)
        if (dist < 120) {
          ctx.beginPath()
          ctx.moveTo(particles[i].x, particles[i].y)
          ctx.lineTo(particles[j].x, particles[j].y)
          ctx.strokeStyle = `rgba(0, 212, 255, ${0.08 * (1 - dist / 120)})`
          ctx.lineWidth = 0.5
          ctx.stroke()
        }
      }
    }
    // 粒子
    for (const p of particles) {
      p.x += p.vx
      p.y += p.vy
      if (p.x < 0 || p.x > w) p.vx *= -1
      if (p.y < 0 || p.y > h) p.vy *= -1
      ctx.beginPath()
      ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2)
      ctx.fillStyle = `rgba(0, 212, 255, ${p.opacity})`
      ctx.fill()
    }
    animId = requestAnimationFrame(draw)
  }
  draw()
}

onMounted(() => {
  initParticles()
})
onBeforeUnmount(() => {
  if (animId) cancelAnimationFrame(animId)
})
</script>

<style scoped>
.welcome-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #060e1a 0%, #0a1628 30%, #0d1b2e 60%, #0a1628 100%);
  color: #e0f0ff;
  overflow-x: hidden;
  overflow-y: auto;
  position: relative;
}

/* ── 粒子画布 ── */
.particle-canvas {
  position: fixed;
  top: 0; left: 0;
  width: 100%; height: 100%;
  pointer-events: none;
  z-index: 0;
}

/* ── 导航栏 ── */
.welcome-nav {
  position: fixed;
  top: 0; left: 0; right: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 40px;
  background: rgba(6, 14, 26, 0.8);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid rgba(26, 58, 92, 0.5);
}
.nav-brand {
  display: flex;
  align-items: center;
  gap: 10px;
}
.nav-logo {
  font-size: 24px;
}
.nav-title {
  font-size: 18px;
  font-weight: 700;
  background: linear-gradient(135deg, #00d4ff, #8b5cf6);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}
.nav-right {
  display: flex;
  align-items: center;
  gap: 14px;
}
.nav-user {
  font-size: 13px;
  color: #8bafc9;
}

/* ── Hero ── */
.hero-section {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  padding: 120px 40px 60px;
  text-align: center;
}
.hero-glow {
  position: absolute;
  top: 20%;
  left: 50%;
  transform: translateX(-50%);
  width: 600px;
  height: 600px;
  background: radial-gradient(circle, rgba(0, 212, 255, 0.08) 0%, rgba(139, 92, 246, 0.04) 40%, transparent 70%);
  pointer-events: none;
  border-radius: 50%;
}
.hero-title {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 20px;
}
.hero-title-main {
  font-size: 64px;
  font-weight: 800;
  letter-spacing: 8px;
  background: linear-gradient(135deg, #00d4ff 0%, #8b5cf6 50%, #00d4ff 100%);
  background-size: 200% 200%;
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  animation: gradientShift 4s ease-in-out infinite;
}
.hero-title-sub {
  font-size: 28px;
  font-weight: 600;
  color: #c0d8f0;
  letter-spacing: 6px;
}
.hero-desc {
  font-size: 16px;
  color: #00d4ff;
  letter-spacing: 12px;
  margin-bottom: 12px;
  font-weight: 500;
}
.hero-sub {
  font-size: 14px;
  color: #5a7a9a;
  max-width: 600px;
  line-height: 1.8;
  margin-bottom: 40px;
}

/* ── 身份选择卡片 ── */
.portal-section {
  display: flex;
  gap: 24px;
  margin-bottom: 40px;
  flex-wrap: wrap;
  justify-content: center;
}
.portal-card {
  width: 240px;
  background: rgba(13, 27, 46, 0.75);
  border: 1.5px solid #1a3a5c;
  border-radius: 18px;
  padding: 28px 22px 22px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
  backdrop-filter: blur(10px);
  display: flex;
  flex-direction: column;
  align-items: center;
}
.portal-card:hover {
  border-color: #00d4ff;
  transform: translateY(-6px);
  box-shadow: 0 16px 48px rgba(0, 212, 255, 0.15);
}
.pc-icon {
  font-size: 40px;
  margin-bottom: 12px;
}
.pc-title {
  font-size: 18px;
  font-weight: 700;
  color: #e0f0ff;
  margin-bottom: 8px;
}
.pc-desc {
  font-size: 12px;
  color: #5a7a9a;
  line-height: 1.6;
  margin-bottom: 14px;
}
.pc-features {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  justify-content: center;
  margin-bottom: 18px;
}
.pc-features span {
  font-size: 10px;
  color: #8bafc9;
  background: rgba(0,212,255,0.08);
  border: 1px solid rgba(0,212,255,0.15);
  border-radius: 20px;
  padding: 2px 10px;
}
.pc-btn {
  width: 100%;
  padding: 10px 0;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  border: none;
  background: linear-gradient(135deg, #00d4ff, #0099cc);
  color: #0a1628;
  transition: all 0.2s;
  letter-spacing: 2px;
}
.pc-btn:hover {
  opacity: 0.9;
  box-shadow: 0 4px 16px rgba(0,212,255,0.3);
}

@keyframes gradientShift {
  0%, 100% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
}

/* Hero 按钮 */
.hero-btns {
  display: flex;
  gap: 20px;
  margin-bottom: 60px;
}
.hero-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 14px 36px;
  border-radius: 50px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
  border: none;
  letter-spacing: 2px;
}
.hero-btn.primary {
  background: linear-gradient(135deg, #00d4ff, #0099cc);
  color: #0a1628;
  box-shadow: 0 4px 24px rgba(0, 212, 255, 0.3);
}
.hero-btn.primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 32px rgba(0, 212, 255, 0.45);
}
.hero-btn.ghost {
  background: transparent;
  color: #8bafc9;
  border: 1px solid #1a3a5c;
}
.hero-btn.ghost:hover {
  border-color: #00d4ff;
  color: #00d4ff;
  transform: translateY(-2px);
}
.btn-arrow {
  font-size: 16px;
  transition: transform 0.2s;
}
.hero-btn:hover .btn-arrow {
  transform: translateX(3px);
}

/* Hero 指标卡 */
.hero-metrics {
  display: flex;
  gap: 24px;
  flex-wrap: wrap;
  justify-content: center;
}
.hm-card {
  display: flex;
  align-items: center;
  gap: 12px;
  background: rgba(13, 27, 46, 0.7);
  border: 1px solid #1a3a5c;
  border-radius: 14px;
  padding: 14px 22px;
  backdrop-filter: blur(8px);
  transition: border-color 0.2s, transform 0.2s;
}
.hm-card:hover {
  border-color: #00d4ff44;
  transform: translateY(-3px);
}
.hm-icon {
  width: 44px; height: 44px;
  border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  font-size: 20px;
}
.hm-val {
  font-size: 18px;
  font-weight: 700;
}
.hm-label {
  font-size: 11px;
  color: #5a7a9a;
}

/* ── 功能特色 ── */
.features-section, .tech-section {
  position: relative;
  z-index: 1;
  padding: 80px 60px;
}
.section-hdr {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-bottom: 48px;
}
.section-hdr h2 {
  font-size: 24px;
  font-weight: 700;
  color: #e0f0ff;
  letter-spacing: 4px;
  margin: 0;
}
.section-line {
  display: inline-block;
  width: 60px;
  height: 1px;
  background: linear-gradient(90deg, transparent, #1a3a5c, transparent);
}
.features-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
  max-width: 1100px;
  margin: 0 auto;
}
.feature-card {
  background: rgba(13, 27, 46, 0.6);
  border: 1px solid #1a3a5c;
  border-radius: 16px;
  padding: 28px 24px;
  cursor: pointer;
  transition: all 0.3s;
  position: relative;
  overflow: hidden;
}
.feature-card:hover {
  border-color: #00d4ff55;
  transform: translateY(-4px);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.3);
}
.feature-card::after {
  content: '';
  position: absolute;
  top: 0; left: 0;
  width: 100%; height: 3px;
  background: linear-gradient(90deg, transparent, var(--fc-color, #00d4ff), transparent);
  opacity: 0;
  transition: opacity 0.3s;
}
.feature-card:hover::after { opacity: 1; }
.fc-icon {
  width: 52px; height: 52px;
  border-radius: 14px;
  display: flex; align-items: center; justify-content: center;
  font-size: 24px;
  margin-bottom: 16px;
}
.fc-title {
  font-size: 17px;
  font-weight: 700;
  color: #e0f0ff;
  margin-bottom: 8px;
}
.fc-desc {
  font-size: 13px;
  color: #5a7a9a;
  line-height: 1.6;
  margin-bottom: 12px;
}
.fc-arrow {
  font-size: 18px;
  font-weight: 700;
  opacity: 0;
  transition: opacity 0.2s, transform 0.2s;
  transform: translateX(-8px);
}
.feature-card:hover .fc-arrow {
  opacity: 1;
  transform: translateX(0);
}

/* ── 技术架构 ── */
.tech-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 16px;
  max-width: 1100px;
  margin: 0 auto;
}
.tech-card {
  text-align: center;
  background: rgba(13, 27, 46, 0.5);
  border: 1px solid #1a3a5c;
  border-radius: 14px;
  padding: 24px 12px;
  transition: border-color 0.2s, transform 0.2s;
}
.tech-card:hover {
  border-color: #00d4ff44;
  transform: translateY(-3px);
}
.tc-icon {
  font-size: 28px;
  margin-bottom: 10px;
}
.tc-name {
  font-size: 14px;
  font-weight: 700;
  color: #e0f0ff;
  margin-bottom: 6px;
}
.tc-desc {
  font-size: 11px;
  color: #5a7a9a;
  line-height: 1.5;
}

/* ── 页脚 ── */
.welcome-footer {
  position: relative;
  z-index: 1;
  text-align: center;
  padding: 40px 20px;
  border-top: 1px solid #1a3a5c22;
}
.footer-brand {
  font-size: 14px;
  font-weight: 600;
  color: #8bafc9;
  margin-bottom: 6px;
}
.footer-sub {
  font-size: 12px;
  color: #5a7a9a;
  margin-bottom: 4px;
}
.footer-copy {
  font-size: 11px;
  color: #3a5a7a;
}

/* ── 响应式 ── */
@media (max-width: 900px) {
  .features-grid { grid-template-columns: repeat(2, 1fr); }
  .tech-grid { grid-template-columns: repeat(3, 1fr); }
  .hero-title-main { font-size: 42px; }
}
@media (max-width: 600px) {
  .features-grid { grid-template-columns: 1fr; }
  .tech-grid { grid-template-columns: repeat(2, 1fr); }
  .hero-metrics { flex-direction: column; align-items: center; }
}
</style>
