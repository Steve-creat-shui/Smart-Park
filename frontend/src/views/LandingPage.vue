<template>
  <div class="landing" ref="landingRef">
    <!-- 粒子 + 网格背景 -->
    <canvas ref="cvs" class="bg-canvas" />

    <!-- 导航 -->
    <nav class="top-nav" :class="{ scrolled: scrollY > 60 }">
      <div class="nav-brand"><span class="brand-icon">⚡</span><span class="brand-txt">鼎筑科技</span></div>
      <div class="nav-links">
        <a @click="scrollTo('hero')">首页</a>
        <a @click="scrollTo('features')">功能</a>
        <a @click="scrollTo('arch')">架构</a>
        <a @click="scrollTo('data')">数据</a>
      </div>
      <div class="nav-actions">
        <button class="btn-ghost" @click="$router.push('/login')">登 录</button>
        <button class="btn-primary" @click="$router.push('/register')">注 册</button>
      </div>
    </nav>

    <!-- ═══ HERO ═══ -->
    <section id="hero" class="hero">
      <div class="hero-grid-overlay" />
      <div class="hero-content">
        <div class="hero-badge">🏢 建筑能源智能管理平台</div>
        <h1 class="hero-title">
          <span class="line1" ref="line1Ref"></span>
          <span class="line2">建筑能源<span class="gradient-text">智能管理系统</span></span>
        </h1>
        <p class="hero-desc">
          融合 <span class="hl">AI 深度学习</span> · <span class="hl">多模态感知</span> · <span class="hl">数字孪生</span> 技术<br/>
          实现建筑能耗实时监测、异常诊断、智能优化的全链路闭环管理
        </p>
        <div class="hero-btns">
          <button class="btn-glow" @click="$router.push('/login')">
            <span>进入系统</span>
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M5 12h14M12 5l7 7-7 7"/></svg>
          </button>
          <button class="btn-outline" @click="scrollTo('features')">
            <span>了解更多</span>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 5v14M5 12l7 7 7-7"/></svg>
          </button>
        </div>
        <!-- 浮动指标卡 -->
        <div class="hero-stats">
          <div class="hs-card" v-for="s in heroStats" :key="s.label">
            <div class="hs-num">{{ s.num }}<span class="hs-unit">{{ s.unit }}</span></div>
            <div class="hs-label">{{ s.label }}</div>
          </div>
        </div>
      </div>
      <!-- 右侧 3D 智慧园区实景 -->
      <div class="hero-visual">
        <div class="hv-glow"></div>
        <div class="hv-frame">
          <div class="hv-frame-header">
            <span class="hv-dot red"></span>
            <span class="hv-dot yellow"></span>
            <span class="hv-dot green"></span>
            <span class="hv-frame-title">数字孪生 · 智慧园区</span>
          </div>
          <iframe src="/smart-park-v3.html" class="hv-iframe" frameborder="0" allowfullscreen></iframe>
        </div>
      </div>
    </section>

    <!-- ═══ 核心能力 ═══ -->
    <section id="features" class="features-section">
      <div class="section-header">
        <div class="sh-line"></div>
        <h2>核心能力</h2>
        <div class="sh-line"></div>
      </div>
      <p class="section-sub">六大模块覆盖建筑能源管理全场景</p>
      <div class="features-grid">
        <div class="feat-card" v-for="(f, i) in features" :key="f.title"
             :style="{'--accent': f.color, '--delay': i * 0.08 + 's'}">
          <div class="fc-glow"></div>
          <div class="fc-icon">{{ f.icon }}</div>
          <div class="fc-title">{{ f.title }}</div>
          <div class="fc-desc">{{ f.desc }}</div>
          <div class="fc-tags">
            <span v-for="t in f.tags" :key="t">{{ t }}</span>
          </div>
        </div>
      </div>
    </section>

    <!-- ═══ 技术架构 ═══ -->
    <section id="arch" class="arch-section">
      <div class="section-header">
        <div class="sh-line"></div>
        <h2>技术架构</h2>
        <div class="sh-line"></div>
      </div>
      <p class="section-sub">前沿技术栈驱动智能化建筑能源管理</p>
      <div class="arch-layers">
        <div class="arch-layer" v-for="(layer, li) in archLayers" :key="layer.name"
             :style="{'--li': li}">
          <div class="al-label">{{ layer.name }}</div>
          <div class="al-items">
            <div class="al-item" v-for="item in layer.items" :key="item.name"
                 :style="{borderColor: item.color + '66', background: item.color + '0a'}">
              <span class="ali-icon">{{ item.icon }}</span>
              <span class="ali-name">{{ item.name }}</span>
            </div>
          </div>
        </div>
        <!-- 连线 -->
        <div class="arch-connector" v-for="i in 3" :key="i" :style="{top: (i * 25 - 2) + '%'}">
          <div class="ac-dot"></div>
          <div class="ac-line"></div>
          <div class="ac-dot"></div>
        </div>
      </div>
    </section>

    <!-- ═══ 数据总览 ═══ -->
    <section id="data" class="data-section">
      <div class="section-header light">
        <div class="sh-line"></div>
        <h2>平台数据</h2>
        <div class="sh-line"></div>
      </div>
      <div class="data-cards">
        <div class="dc" v-for="d in dataCards" :key="d.label">
          <div class="dc-icon" :style="{background: d.color + '20', color: d.color}">{{ d.icon }}</div>
          <div class="dc-num" :style="{color: d.color}">
            <span class="dc-counter" :data-target="d.value">0</span>{{ d.suffix }}
          </div>
          <div class="dc-label">{{ d.label }}</div>
          <div class="dc-bar"><div class="dc-bar-fill" :style="{width: d.pct + '%', background: d.color}"></div></div>
        </div>
      </div>
    </section>

    <!-- ═══ CTA ═══ -->
    <section class="cta-section">
      <div class="cta-bg"></div>
      <div class="cta-content">
        <h2>开始使用智能能源管理系统</h2>
        <p>AI 驱动，让每一度电都不浪费</p>
        <div class="cta-btns">
          <button class="btn-glow lg" @click="$router.push('/register')">立即注册</button>
          <button class="btn-outline lg" @click="$router.push('/login')">登录系统</button>
        </div>
      </div>
    </section>

    <!-- 页脚 -->
    <footer class="landing-footer">
      <div class="footer-grid">
        <div class="fg-brand">
          <div class="fg-logo">⚡ 鼎筑科技</div>
          <p>建筑能源智能管理系统 v2.0</p>
          <p class="fg-tech">Vue 3 · Spring Boot · MySQL · ECharts · Coze AI</p>
        </div>
        <div class="fg-col">
          <h4>产品</h4>
          <a>能耗监测</a><a>AI 诊断</a><a>设备管理</a><a>报告生成</a>
        </div>
        <div class="fg-col">
          <h4>技术</h4>
          <a>多模态感知</a><a>数字孪生</a><a>深度学习</a><a>边缘计算</a>
        </div>
        <div class="fg-col">
          <h4>关于</h4>
          <a>服务外包创新创业大赛</a><a>团队介绍</a>
        </div>
      </div>
      <div class="footer-bottom">© 2026 鼎筑科技 · 中国服务外包创新创业大赛参赛作品</div>
    </footer>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'

const landingRef = ref(null)
const cvs = ref(null)
const line1Ref = ref(null)
const scrollY = ref(0)
let animId = null

const heroStats = [
  { num: '7', unit: '栋', label: '监测建筑' },
  { num: '2.4', unit: '万kWh', label: '日均电耗' },
  { num: '18.6', unit: '%', label: '碳减排率' },
  { num: '99.9', unit: '%', label: '系统可用率' }
]

const features = [
  { icon: '📊', title: '实时能耗监测', color: '#00d4ff',
    desc: '全校7栋建筑能耗数据实时采集，分楼栋、分时段可视化展示',
    tags: ['实时数据', '可视化', '多维分析'] },
  { icon: '🤖', title: 'AI 智能诊断', color: '#8b5cf6',
    desc: '基于深度学习的能耗异常检测与智能分析，自动生成运维建议',
    tags: ['异常检测', '智能分析', '自动建议'] },
  { icon: '🔔', title: '告警联动工单', color: '#ff4d4f',
    desc: '异常告警自动触发工单，AI生成处理建议，闭环跟踪全流程',
    tags: ['自动告警', '工单派发', '闭环管理'] },
  { icon: '📡', title: '多模态监控', color: '#52c41a',
    desc: '融合温湿度、人员占用、电力负荷等多源数据的综合态势感知',
    tags: ['多源融合', '态势感知', '边缘计算'] },
  { icon: '📝', title: '智能报告生成', color: '#faad14',
    desc: 'AI 驱动的能耗分析报告，支持自动生成、在线预览与导出',
    tags: ['AI撰写', '自动生成', '一键导出'] },
  { icon: '🏗️', title: '数字孪生', color: '#13c2c2',
    desc: '建筑三维数字孪生模型，直观展示设备分布与运行状态',
    tags: ['3D可视化', '设备定位', '状态联动'] }
]

const archLayers = [
  { name: '展示层', items: [
    { icon: '⚡', name: 'Vue 3', color: '#42b883' },
    { icon: '📊', name: 'ECharts', color: '#e43961' },
    { icon: '🎨', name: 'Element Plus', color: '#409eff' },
    { icon: '🧩', name: 'Pinia', color: '#f7d336' }
  ]},
  { name: '服务层', items: [
    { icon: '☕', name: 'Spring Boot', color: '#6db33f' },
    { icon: '🔐', name: 'JWT 认证', color: '#ff6b6b' },
    { icon: '📦', name: 'JPA/Hibernate', color: '#59666c' },
    { icon: '🔄', name: 'REST API', color: '#00d4ff' }
  ]},
  { name: '智能层', items: [
    { icon: '🤖', name: 'Coze AI', color: '#8b5cf6' },
    { icon: '🧠', name: '异常检测', color: '#ff4d4f' },
    { icon: '📈', name: '能耗预测', color: '#52c41a' },
    { icon: '💡', name: 'AI 建议', color: '#faad14' }
  ]},
  { name: '数据层', items: [
    { icon: '🗄️', name: 'MySQL 8', color: '#00758f' },
    { icon: '📊', name: '时序数据', color: '#13c2c2' },
    { icon: '🐳', name: 'Docker', color: '#2496ed' },
    { icon: '☁️', name: '云部署', color: '#ff9c6e' }
  ]}
]

const dataCards = [
  { icon: '🏢', label: '监测建筑', value: 7, suffix: ' 栋', pct: 100, color: '#00d4ff' },
  { icon: '📡', label: '传感器节点', value: 420, suffix: '+', pct: 84, color: '#52c41a' },
  { icon: '⚡', label: '日处理数据', value: 50, suffix: ' 万条', pct: 72, color: '#faad14' },
  { icon: '🤖', label: 'AI分析任务', value: 1200, suffix: '+', pct: 90, color: '#8b5cf6' },
  { icon: '🔔', label: '处理告警', value: 860, suffix: '+', pct: 65, color: '#ff4d4f' },
  { icon: '🌿', label: '节能减排', value: 18, suffix: '%', pct: 55, color: '#13c2c2' }
]

function scrollTo(id) {
  document.getElementById(id)?.scrollIntoView({ behavior: 'smooth' })
}

function onScroll() {
  scrollY.value = window.scrollY
}

// 打字机效果
function typeWriter(el, text, speed = 80) {
  let i = 0
  function tick() {
    if (!el) return
    if (i <= text.length) {
      el.textContent = text.slice(0, i) + (i < text.length ? '|' : '')
      i++
      setTimeout(tick, speed)
    } else {
      el.textContent = text
    }
  }
  tick()
}

// 数字滚动
function animateCounters() {
  document.querySelectorAll('.dc-counter').forEach(el => {
    const target = parseInt(el.dataset.target)
    const duration = 1600
    const start = performance.now()
    function step(now) {
      const progress = Math.min((now - start) / duration, 1)
      const eased = 1 - Math.pow(1 - progress, 3)
      el.textContent = Math.floor(eased * target)
      if (progress < 1) requestAnimationFrame(step)
    }
    requestAnimationFrame(step)
  })
}

// 粒子背景
function initCanvas() {
  const canvas = cvs.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  let w, h, particles = []

  function resize() {
    w = canvas.width = window.innerWidth
    h = canvas.height = window.innerHeight * 4
  }
  resize()
  window.addEventListener('resize', resize)

  class Particle {
    constructor() { this.reset() }
    reset() {
      this.x = Math.random() * w
      this.y = Math.random() * h
      this.vx = (Math.random() - 0.5) * 0.3
      this.vy = (Math.random() - 0.5) * 0.3
      this.r = Math.random() * 1.5 + 0.5
      this.alpha = Math.random() * 0.5 + 0.1
    }
    update() {
      this.x += this.vx; this.y += this.vy
      if (this.x < 0 || this.x > w) this.vx *= -1
      if (this.y < 0 || this.y > h) this.vy *= -1
    }
    draw() {
      ctx.beginPath()
      ctx.arc(this.x, this.y, this.r, 0, Math.PI * 2)
      ctx.fillStyle = `rgba(0,212,255,${this.alpha})`
      ctx.fill()
    }
  }

  for (let i = 0; i < 120; i++) particles.push(new Particle())

  function frame() {
    ctx.clearRect(0, 0, w, h)
    particles.forEach(p => { p.update(); p.draw() })
    // 连线
    for (let i = 0; i < particles.length; i++) {
      for (let j = i + 1; j < particles.length; j++) {
        const dx = particles[i].x - particles[j].x
        const dy = particles[i].y - particles[j].y
        const dist = Math.sqrt(dx * dx + dy * dy)
        if (dist < 160) {
          ctx.beginPath()
          ctx.moveTo(particles[i].x, particles[i].y)
          ctx.lineTo(particles[j].x, particles[j].y)
          ctx.strokeStyle = `rgba(0,212,255,${0.06 * (1 - dist / 160)})`
          ctx.lineWidth = 0.5
          ctx.stroke()
        }
      }
    }
    animId = requestAnimationFrame(frame)
  }
  frame()
}

// 交叉观察器 — 滚动到数据区时触发计数
let counterTriggered = false
function setupObserver() {
  const el = document.getElementById('data')
  if (!el) return
  const obs = new IntersectionObserver(entries => {
    if (entries[0].isIntersecting && !counterTriggered) {
      counterTriggered = true
      animateCounters()
    }
  }, { threshold: 0.3 })
  obs.observe(el)
}

onMounted(() => {
  window.addEventListener('scroll', onScroll)
  initCanvas()
  setupObserver()
  setTimeout(() => typeWriter(line1Ref.value, '鼎  筑', 200), 400)
})
onBeforeUnmount(() => {
  window.removeEventListener('scroll', onScroll)
  cancelAnimationFrame(animId)
})
</script>

<style scoped>
/* ═══ 基础 ═══ */
.landing {
  background: #050d1a; color: #e0f0ff; font-family: 'PingFang SC','Microsoft YaHei',sans-serif;
  min-height: 100vh; overflow-x: hidden; position: relative;
}
.bg-canvas { position: fixed; top: 0; left: 0; width: 100%; height: 100%; z-index: 0; pointer-events: none; }

/* ═══ 导航 ═══ */
.top-nav {
  position: fixed; top: 0; left: 0; right: 0; z-index: 100;
  display: flex; align-items: center; justify-content: space-between;
  padding: 16px 48px; transition: all 0.35s;
}
.top-nav.scrolled { background: rgba(5,13,26,0.92); backdrop-filter: blur(12px); box-shadow: 0 1px 30px rgba(0,0,0,0.4); padding: 10px 48px; }
.nav-brand { display: flex; align-items: center; gap: 10px; }
.brand-icon { font-size: 26px; }
.brand-txt { font-size: 20px; font-weight: 800; background: linear-gradient(90deg,#00d4ff,#8b5cf6); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }
.nav-links { display: flex; gap: 28px; }
.nav-links a { color: #8bafc9; font-size: 14px; cursor: pointer; transition: color 0.2s; position: relative; }
.nav-links a:hover { color: #00d4ff; }
.nav-links a::after { content:''; position: absolute; bottom:-4px; left:0; width:0; height:2px; background:#00d4ff; transition:width 0.25s; }
.nav-links a:hover::after { width:100%; }
.nav-actions { display: flex; gap: 10px; }

/* 按钮 */
.btn-ghost { padding: 7px 20px; border: 1px solid #3a5a7a; border-radius: 8px; background: transparent; color: #8bafc9; font-size: 13px; cursor: pointer; transition: all 0.2s; }
.btn-ghost:hover { border-color: #00d4ff; color: #00d4ff; }
.btn-primary { padding: 7px 20px; border: none; border-radius: 8px; background: linear-gradient(135deg,#00d4ff,#0088cc); color: #fff; font-size: 13px; font-weight: 600; cursor: pointer; transition: all 0.2s; }
.btn-primary:hover { box-shadow: 0 0 20px #00d4ff44; transform: translateY(-1px); }
.btn-glow {
  display: inline-flex; align-items: center; gap: 8px;
  padding: 12px 32px; border: none; border-radius: 12px;
  background: linear-gradient(135deg,#00d4ff,#0088cc);
  color: #fff; font-size: 15px; font-weight: 700; cursor: pointer;
  box-shadow: 0 0 30px #00d4ff33; transition: all 0.3s;
}
.btn-glow:hover { box-shadow: 0 0 40px #00d4ff66; transform: translateY(-2px); }
.btn-glow.lg { padding: 14px 40px; font-size: 16px; }
.btn-outline {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 12px 28px; border: 1.5px solid #3a5a7a; border-radius: 12px;
  background: transparent; color: #8bafc9; font-size: 15px; cursor: pointer; transition: all 0.3s;
}
.btn-outline:hover { border-color: #00d4ff; color: #00d4ff; }
.btn-outline.lg { padding: 14px 36px; font-size: 16px; }

/* ═══ HERO ═══ */
.hero {
  position: relative; z-index: 1; min-height: 100vh;
  display: flex; align-items: center; padding: 120px 60px 60px;
  gap: 40px;
}
.hero-grid-overlay {
  position: absolute; inset: 0; z-index: -1;
  background:
    linear-gradient(rgba(0,212,255,0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(0,212,255,0.03) 1px, transparent 1px);
  background-size: 60px 60px;
  mask-image: radial-gradient(ellipse 70% 60% at 30% 50%, black 30%, transparent 70%);
}
.hero-content { flex: 1; max-width: 640px; }
.hero-badge {
  display: inline-block; padding: 6px 16px; border-radius: 20px;
  background: rgba(0,212,255,0.08); border: 1px solid #00d4ff33;
  font-size: 13px; color: #00d4ff; margin-bottom: 20px;
}
.hero-title { margin: 0 0 20px; line-height: 1.2; }
.line1 { display: block; font-size: 64px; font-weight: 900; letter-spacing: 8px; color: #e0f0ff; min-height: 76px; }
.line2 { display: block; font-size: 28px; font-weight: 600; color: #8bafc9; margin-top: 8px; }
.gradient-text { background: linear-gradient(90deg,#00d4ff,#8b5cf6); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }
.hero-desc { font-size: 15px; color: #6a8fad; line-height: 1.9; margin-bottom: 30px; }
.hl { color: #00d4ff; font-weight: 500; }
.hero-btns { display: flex; gap: 14px; margin-bottom: 48px; }

/* 指标卡 */
.hero-stats { display: flex; gap: 16px; flex-wrap: wrap; }
.hs-card {
  padding: 14px 20px; border-radius: 12px;
  background: rgba(0,212,255,0.04); border: 1px solid #1a3a5c;
  min-width: 110px; transition: border-color 0.2s;
}
.hs-card:hover { border-color: #00d4ff66; }
.hs-num { font-size: 26px; font-weight: 800; color: #00d4ff; }
.hs-unit { font-size: 13px; font-weight: 400; color: #5a7a9a; margin-left: 2px; }
.hs-label { font-size: 11px; color: #5a7a9a; margin-top: 2px; }

/* 右侧 3D 智慧园区 */
.hero-visual {
  position: relative; flex: 1; min-width: 420px; max-width: 600px; min-height: 400px; flex-shrink: 0;
}
.hv-glow {
  position: absolute; inset: -20px; border-radius: 24px; z-index: -1;
  background: radial-gradient(ellipse at center, rgba(0,212,255,0.12) 0%, transparent 70%);
  filter: blur(20px); animation: glow-pulse 4s ease-in-out infinite alternate;
}
@keyframes glow-pulse { 0% { opacity: 0.6; } 100% { opacity: 1; } }
.hv-frame {
  width: 100%; height: 100%; min-height: 400px;
  border-radius: 16px; overflow: hidden;
  border: 1.5px solid rgba(0,212,255,0.3);
  box-shadow: 0 8px 40px rgba(0,0,0,0.5), 0 0 60px rgba(0,212,255,0.08);
  background: #0a0e1a; display: flex; flex-direction: column;
}
.hv-frame-header {
  display: flex; align-items: center; gap: 6px;
  padding: 8px 14px; background: rgba(0,20,40,0.9);
  border-bottom: 1px solid rgba(0,212,255,0.15);
}
.hv-dot {
  width: 10px; height: 10px; border-radius: 50%;
}
.hv-dot.red { background: #ff5f57; }
.hv-dot.yellow { background: #ffbd2e; }
.hv-dot.green { background: #28c840; }
.hv-frame-title {
  margin-left: 8px; font-size: 11px; color: #5a8aaa; letter-spacing: 1px; font-weight: 500;
}
.hv-iframe {
  flex: 1; width: 100%; border: none; display: block;
  pointer-events: auto;
}

/* ═══ 功能区 ═══ */
.features-section { position: relative; z-index: 1; padding: 100px 60px; }
.section-header { display: flex; align-items: center; justify-content: center; gap: 16px; margin-bottom: 8px; }
.section-header h2 { font-size: 28px; font-weight: 800; color: #e0f0ff; white-space: nowrap; }
.sh-line { width: 60px; height: 2px; background: linear-gradient(90deg,transparent,#00d4ff66,transparent); }
.section-sub { text-align: center; font-size: 14px; color: #5a7a9a; margin-bottom: 48px; }

.features-grid { display: grid; grid-template-columns: repeat(3,1fr); gap: 20px; max-width: 1100px; margin: 0 auto; }
.feat-card {
  position: relative; padding: 28px 24px; border-radius: 16px;
  background: rgba(13,27,46,0.7); border: 1px solid #1a3a5c;
  overflow: hidden; cursor: default; transition: all 0.3s;
  animation: fadeUp 0.6s ease both;
  animation-delay: var(--delay);
}
.feat-card:hover { border-color: var(--accent); transform: translateY(-4px); box-shadow: 0 8px 30px rgba(0,0,0,0.3); }
.fc-glow {
  position: absolute; top: -40px; right: -40px; width: 120px; height: 120px;
  background: var(--accent); opacity: 0.06; border-radius: 50%; filter: blur(40px);
  transition: opacity 0.3s;
}
.feat-card:hover .fc-glow { opacity: 0.12; }
.fc-icon { font-size: 32px; margin-bottom: 12px; }
.fc-title { font-size: 17px; font-weight: 700; color: #e0f0ff; margin-bottom: 8px; }
.fc-desc { font-size: 13px; color: #6a8fad; line-height: 1.7; margin-bottom: 12px; }
.fc-tags { display: flex; flex-wrap: wrap; gap: 6px; }
.fc-tags span {
  padding: 3px 10px; border-radius: 12px; font-size: 11px;
  background: rgba(0,212,255,0.1); color: var(--accent, #00d4ff);
  border: 1px solid rgba(0,212,255,0.15);
}

@keyframes fadeUp {
  from { opacity: 0; transform: translateY(24px); }
  to { opacity: 1; transform: translateY(0); }
}

/* ═══ 架构区 ═══ */
.arch-section { position: relative; z-index: 1; padding: 100px 60px; }
.arch-layers { max-width: 900px; margin: 0 auto; display: flex; flex-direction: column; gap: 24px; position: relative; }
.arch-layer {
  display: flex; align-items: center; gap: 20px;
  animation: fadeUp 0.5s ease both;
  animation-delay: calc(var(--li) * 0.12s);
}
.al-label {
  width: 72px; height: 72px; border-radius: 14px; flex-shrink: 0;
  background: rgba(0,212,255,0.06); border: 1px solid #00d4ff33;
  display: flex; align-items: center; justify-content: center;
  font-size: 13px; font-weight: 700; color: #00d4ff;
}
.al-items { display: flex; flex: 1; gap: 12px; flex-wrap: wrap; }
.al-item {
  display: flex; align-items: center; gap: 8px;
  padding: 10px 16px; border-radius: 10px;
  border: 1px solid #1a3a5c; transition: all 0.25s; cursor: default;
}
.al-item:hover { transform: translateY(-2px); box-shadow: 0 4px 16px rgba(0,0,0,0.2); }
.ali-icon { font-size: 18px; }
.ali-name { font-size: 13px; font-weight: 500; color: #c0d8f0; }
.arch-connector { display: none; }

/* ═══ 数据区 ═══ */
.data-section {
  position: relative; z-index: 1; padding: 100px 60px;
  background: linear-gradient(180deg, transparent, rgba(0,212,255,0.02), transparent);
}
.data-cards { display: grid; grid-template-columns: repeat(6,1fr); gap: 16px; max-width: 1100px; margin: 0 auto; }
.dc {
  padding: 24px 16px; border-radius: 14px; text-align: center;
  background: rgba(13,27,46,0.7); border: 1px solid #1a3a5c;
  transition: all 0.3s;
}
.dc:hover { border-color: #00d4ff66; transform: translateY(-3px); }
.dc-icon { width: 44px; height: 44px; border-radius: 12px; margin: 0 auto 10px; display: flex; align-items: center; justify-content: center; font-size: 20px; }
.dc-num { font-size: 28px; font-weight: 800; margin-bottom: 4px; }
.dc-label { font-size: 12px; color: #5a7a9a; margin-bottom: 10px; }
.dc-bar { width: 100%; height: 4px; border-radius: 2px; background: #1a3a5c; overflow: hidden; }
.dc-bar-fill { height: 100%; border-radius: 2px; transition: width 1.5s ease; }

/* ═══ CTA ═══ */
.cta-section {
  position: relative; z-index: 1; padding: 100px 60px; text-align: center;
}
.cta-bg {
  position: absolute; inset: 0; z-index: -1;
  background: radial-gradient(ellipse 60% 50% at 50% 50%, rgba(0,212,255,0.06), transparent);
}
.cta-content h2 { font-size: 32px; font-weight: 800; margin-bottom: 12px; }
.cta-content p { font-size: 15px; color: #5a7a9a; margin-bottom: 32px; }
.cta-btns { display: flex; gap: 16px; justify-content: center; }

/* ═══ 页脚 ═══ */
.landing-footer { position: relative; z-index: 1; padding: 60px; border-top: 1px solid #1a3a5c; }
.footer-grid { display: grid; grid-template-columns: 2fr 1fr 1fr 1fr; gap: 40px; margin-bottom: 40px; }
.fg-logo { font-size: 18px; font-weight: 800; margin-bottom: 8px; background: linear-gradient(90deg,#00d4ff,#8b5cf6); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }
.fg-brand p { font-size: 13px; color: #5a7a9a; margin: 4px 0; }
.fg-tech { font-size: 11px !important; color: #3a5a7a !important; }
.fg-col h4 { font-size: 13px; font-weight: 700; color: #8bafc9; margin-bottom: 12px; }
.fg-col a { display: block; font-size: 12px; color: #5a7a9a; margin-bottom: 8px; cursor: pointer; transition: color 0.2s; }
.fg-col a:hover { color: #00d4ff; }
.footer-bottom { text-align: center; font-size: 12px; color: #3a5a7a; padding-top: 24px; border-top: 1px solid #1a3a5c11; }

/* ═══ 响应式 ═══ */
@media (max-width: 1024px) {
  .hero { flex-direction: column; padding: 100px 30px 40px; }
  .hero-visual { min-width: unset; max-width: 100%; width: 100%; min-height: 340px; }
  .hv-frame { min-height: 340px; }
  .features-grid { grid-template-columns: repeat(2,1fr); }
  .data-cards { grid-template-columns: repeat(3,1fr); }
  .footer-grid { grid-template-columns: 1fr 1fr; }
}
@media (max-width: 640px) {
  .top-nav { padding: 10px 20px; }
  .nav-links { display: none; }
  .hero-content { text-align: center; }
  .line1 { font-size: 42px; }
  .line2 { font-size: 20px; }
  .hero-btns { justify-content: center; }
  .hero-stats { justify-content: center; }
  .features-grid { grid-template-columns: 1fr; }
  .data-cards { grid-template-columns: repeat(2,1fr); }
  .arch-layer { flex-direction: column; }
  .footer-grid { grid-template-columns: 1fr; }
}
</style>
