# 深蓝色主题修复提示词

## 一、全局颜色替换规则

在 `frontend/src/views/` 下所有 `.vue` 文件中，执行以下颜色替换：

| 浅色（当前） | 深蓝色（目标） | 用途 |
|---|---|---|
| `#ffffff` | `#0d1b2e` | 卡片/面板背景、表格行背景 |
| `#f5faf9` | `#0f2540` | 次级背景（输入框、tile、badge） |
| `#f0f9f7` | `#0f2540` | 表头背景、概览 tile 背景 |
| `#e8f9f5` | `#0f2540` | 高亮背景（聊天气泡、工具气泡） |
| `#e8f7f4` | `#1a3a5c` | 分割线、splitLine |
| `#d4ede8` | `#1a3a5c` | 边框、axisLine、分割线 |
| `#d1f2eb` | `#00d4ff22` | stat-card 图标背景 |
| `#1ABC9C` | `#00d4ff` | 主色调（高亮、强调色） |
| `#1a2e2a` | `#e0f0ff` | 主文字颜色（标题、正文） |
| `#2d4a45` | `#c0d8f0` | 次级文字（表格内容） |
| `#6b8a84` | `#8bafc9` | 辅助文字（标签、描述） |
| `#9ab8b2` | `#5a7a9a` | 弱辅助文字（placeholder、版本号） |
| `#148f77` | `#00a8cc` | 渐变色副色 |

**注意**：以下场景中的 `#ffffff` **不要替换**：
- `color: #ffffff`（按钮文字颜色，保持白色）
- `el-avatar` 的 `color:#ffffff`（头像文字颜色，保持白色）

---

## 二、各文件剩余修改清单

### 1. EnergyAnalysis.vue（42处）

**JS 部分：**
- `<span style="color:#1ABC9C">` → `<span style="color:#00d4ff">` （2处，行117和179的表格内联样式）
- `color: '#1ABC9C'` → `color: '#00d4ff'` （kpiItems、metricMap、monthlyKpi、dailyKpi 中所有出现）
- `colors = { power: '#1ABC9C'` → `colors = { power: '#00d4ff'`
- 所有 `tooltip: { ... backgroundColor: '#ffffff', borderColor: '#d4ede8', textStyle: { color: '#1a2e2a' } }` → `backgroundColor: '#0d1b2e', borderColor: '#1a3a5c', textStyle: { color: '#e0f0ff' }`
- 所有 `axisLabel: { color: '#6b8a84' }` → `color: '#8bafc9'`
- 所有 `axisLine: { lineStyle: { color: '#d4ede8' } }` → `color: '#1a3a5c'`
- 所有 `splitLine: { lineStyle: { color: '#e8f7f4' } }` → `color: '#1a3a5c'`
- 所有 `nameTextStyle: { color: '#6b8a84'` → `color: '#8bafc9'`
- `label: { color: '#6b8a84' }` → `color: '#8bafc9'`
- `legend: { ... textStyle: { color: '#6b8a84' } }` → `color: '#8bafc9'`
- `headerStyle` → `background: '#0f2540', color: '#8bafc9', borderBottom: '1px solid #1a3a5c'`
- `cellStyle` → `background: '#0d1b2e', color: '#c0d8f0', borderBottom: '1px solid #1a3a5c'`

**CSS 部分（`<style scoped>` 内）：**
- 需要检查并替换所有 `#ffffff`→`#0d1b2e`, `#f0f9f7`→`#0f2540`, `#d4ede8`→`#1a3a5c`, `#1a2e2a`→`#e0f0ff`, `#6b8a84`→`#8bafc9`, `#1ABC9C`→`#00d4ff` 等

### 2. AIAnalysis.vue（23处）

**JS 部分：**
- `style="color:#6b8a84"` → `style="color:#8bafc9"` （行63、100的内联样式）
- `color: '#1ABC9C'` → `color: '#00d4ff'` （diagnosisItems、trainStats、radarOption）
- `radarOption` 中 `tooltip: { backgroundColor: '#ffffff', borderColor: '#d4ede8', textStyle: { color: '#1a2e2a' } }` → 深蓝色
- `axisLine: { lineStyle: { color: '#d4ede8' } }` → `#1a3a5c`
- `splitLine: { lineStyle: { color: '#e8f7f4' } }` → `#1a3a5c`
- `name: { color: '#6b8a84' }` → `#8bafc9`
- `areaStyle: { color: '#1ABC9C22' }` → `#00d4ff22`
- `lineStyle: { color: '#1ABC9C' }` → `#00d4ff`
- `formatMessage` 中 `color:#1ABC9C` → `#00d4ff`
- `background:#e8f9f5` → `#0f2540`
- `tableHeader` → `{ background: '#0f2540', color: '#8bafc9', ... borderBottom: '1px solid #1a3a5c' }`
- `tableCell` → `{ background: '#0d1b2e', color: '#c0d8f0', borderBottom: '1px solid #1a3a5c' }`

**CSS 部分：**
- `.ai-header-card { background: linear-gradient(135deg, #e8f9f5, #f0f9f7); border: 1px solid #1ABC9C44; }` → `background: linear-gradient(135deg, #0f2540, #0d1b2e); border: 1px solid #00d4ff44;`
- `.msg-bubble { background: #f5faf9; border: 1px solid #d4ede8; }` → `background: #0f2540; border: 1px solid #1a3a5c;`
- `.message.user .msg-bubble { background: #e8f9f5; border-color: #1ABC9C44; }` → `background: #132d4a; border-color: #00d4ff44;`
- `.msg-content { color: #1a2e2a; }` → `color: #e0f0ff;`
- `.msg-content :deep(strong) { color: #1ABC9C; }` → `color: #00d4ff;`
- `.msg-time { ... color: #6b8a84; }` → `#8bafc9`
- `.tool-call-bubble { background: #e8f9f5; border: 1px solid #1ABC9C33; ... color: #6b8a84; }` → `background: #0f2540; border: 1px solid #00d4ff33; color: #8bafc9;`
- `.tool-name { color: #1ABC9C; }` → `#00d4ff`
- `.typing-dots span { ... background: #1ABC9C; }` → `#00d4ff`
- `.input-area { border-top: 1px solid #d4ede8; }` → `#1a3a5c`
- `.quick-label { ... color: #6b8a84; }` → `#8bafc9`
- `.analysis-card { background: #ffffff; border: 1px solid #d4ede8; }` → `background: #0d1b2e; border: 1px solid #1a3a5c;`
- `.card-title { ... color: #1a2e2a; }` → `#e0f0ff`
- `.tool-item { background: #f0f9f7; }` → `#0f2540`
- `.tool-item-name { color: #1a2e2a; }` → `#e0f0ff`
- `.tool-item-desc { color: #6b8a84; }` → `#8bafc9`
- `.diag-item { background: #f0f9f7; }` → `#0f2540`
- `.diag-item:hover { border-color: #1ABC9C44; }` → `#00d4ff44`
- `.diag-title { color: #1a2e2a; }` → `#e0f0ff`
- `.diag-desc { color: #6b8a84; }` → `#8bafc9`

### 3. Dashboard.vue（9处）

**JS 部分：**
- `statCards` 中 `bgColor: '#d1f2eb'` → `'#00d4ff22'`；`bgColor: '#d5f5e3'` → `'#52c41a22'`；`bgColor: '#fef9e7'` → `'#fadb1422'`；`bgColor: '#fde8e8'` → `'#ff6b6b22'`
- `trendOption` 中所有图表颜色：
  - `tooltip` → 深蓝色
  - `legend textStyle color: '#6b8a84'` → `#8bafc9`
  - `axisLine color: '#d4ede8'` → `#1a3a5c`
  - `axisLabel color: '#6b8a84'` → `#8bafc9`
  - `splitLine color: '#e8f7f4'` → `#1a3a5c`
  - `nameTextStyle color: '#6b8a84'` → `#8bafc9`
  - 第二Y轴 `'#1ABC9C'` → `'#00d4ff'`
  - series `'#1ABC9C'` → `'#00d4ff'` 及其 colorStops
- `pieOption` 中：
  - `tooltip` → 深蓝色
  - `legend textStyle color: '#6b8a84'` → `#8bafc9`
  - `空调系统 color: '#1ABC9C'` → `#00d4ff`
  - `label color: '#6b8a84'` → `#8bafc9`

**CSS 部分：**
- `.stat-icon { color: #1ABC9C; }` → `color: #00d4ff;`

### 4. SystemSettings.vue（21处）

**CSS 部分：**
- `.settings-page { color: #1a2e2a; }` → `#e0f0ff`
- `.settings-nav { background: #f5faf9; border-right: 1px solid #d4ede8; }` → `background: #0d1b2e; border-right: 1px solid #1a3a5c;`
- `.nav-brand { color: #1a2e2a; border-bottom: 1px solid #d4ede8; }` → `color: #e0f0ff; border-bottom: 1px solid #1a3a5c;`
- `.nav-search :deep(.el-input__inner) { color: #1a2e2a; }` → `#e0f0ff`
- `.nav-item { color: #6b8a84; }` → `#8bafc9`
- `.section-header { border-bottom: 1px solid #d4ede8; }` → `#1a3a5c`
- `.section-title { color: #1a2e2a; }` → `#e0f0ff`
- `.section-desc { color: #9ab8b2; }` → `#5a7a9a`
- `.block-title { color: #6b8a84; }` → `#8bafc9`
- `.setting-row { border-bottom: 1px solid #e8f7f4; }` → `#1a3a5c`
- `.sr-name { color: #2d4a45; }` → `#c0d8f0`
- `.sr-desc { color: #9ab8b2; }` → `#5a7a9a`
- `.ov-tile { background: #f5faf9; border: 1px solid #d4ede8; }` → `background: #0f2540; border: 1px solid #1a3a5c;`
- `.ov-label { color: #6b8a84; }` → `#8bafc9`
- `.status-row { border-bottom: 1px solid #e8f7f4; }` → `#1a3a5c`
- `.svc-name { color: #2d4a45; }` → `#c0d8f0`
- `.svc-addr { color: #9ab8b2; }` → `#5a7a9a`
- `.preview-row { border-bottom: 1px solid #e8f7f4; }` → `#1a3a5c`
- `.pr-label { color: #6b8a84; }` → `#8bafc9`
- `.pr-desc { color: #9ab8b2; }` → `#5a7a9a`
- `.about-hero { border-bottom: 1px solid #d4ede8; }` → `#1a3a5c`
- `.about-name { color: #1a2e2a; }` → `#e0f0ff`
- `.about-version { color: #9ab8b2; }` → `#5a7a9a`
- `.about-text { color: #6b8a84; }` → `#8bafc9`
- `.dep-item { background: #f5faf9; }` → `#0f2540`
- `.dep-name { color: #2d4a45; }` → `#c0d8f0`
- `.dep-ver { color: #9ab8b2; }` → `#5a7a9a`
- `.settings-form label { color: #6b8a84; }` → `#8bafc9`
- `.unit-label { color: #9ab8b2; }` → `#5a7a9a`
- 内联 `style="color:#6b8a84"` → `style="color:#8bafc9"` （行194缓存大小）

### 5. BuildingDetail.vue（20处）

**JS 部分：**
- `bld?.color || '#1ABC9C'` → `bld?.color || '#00d4ff'` （powerOpt、pieOpt中）
- `powerOpt` 图表：tooltip、axisLabel、axisLine、yAxis splitLine 全部替换
- `pieOpt` 图表：tooltip、legend textStyle 全部替换
- `'其他电力' itemStyle color: '#1ABC9C'` → `#00d4ff`
- 内联 `style="color:#1ABC9C"` → `style="color:#00d4ff"` （行77）

**CSS 部分：**
- `.bd-kpi { background: #ffffff; border: 1px solid #d4ede8; }` → `background: #0d1b2e; border: 1px solid #1a3a5c;`
- `.bd-kpi-unit { color: #9ab8b2; }` → `#5a7a9a`
- `.bd-kpi-lbl { color: #6b8a84; }` → `#8bafc9`
- `.bd-card { background: #ffffff; border: 1px solid #d4ede8; }` → `background: #0d1b2e; border: 1px solid #1a3a5c;`
- `.bd-card-title { color: #1a2e2a; }` → `#e0f0ff`
- `.bd-spin { color: #9ab8b2; }` → `#5a7a9a`
- `.bd-action-title { color: #9ab8b2; }` → `#5a7a9a`
- `.bd-action-card { background: #ffffff; border: 1px solid #d4ede8; }` → `background: #0d1b2e; border: 1px solid #1a3a5c;`
- `.bd-action-card:hover { ... background: rgba(26,188,156,0.04); box-shadow: ... rgba(26,188,156,0.1); }` → `background: rgba(0,212,255,0.08); box-shadow: ... rgba(0,212,255,0.15);`
- `.bd-ac-name { color: #1a2e2a; }` → `#e0f0ff`
- `.bd-ac-desc { color: #9ab8b2; }` → `#5a7a9a`
- `.bd-footer { border-top: 1px solid #d4ede8; color: #9ab8b2; }` → `border-top: 1px solid #1a3a5c; color: #5a7a9a;`
- `.bd-dot { color: #d4ede8; }` → `#1a3a5c`

### 6. DataManagement.vue（17处）

**CSS 部分：**
- `.data-tabs header { border-bottom: 1px solid #d4ede8; }` → `#1a3a5c`
- `.data-tabs .is-active { color: #1ABC9C; }` → `#00d4ff`
- `.data-tabs __active-bar { background-color: #1ABC9C; }` → `#00d4ff`
- `.upload-zone dragger { background: #f5faf9; border: 1px dashed rgba(26,188,156,0.4); }` → `background: #0f2540; border: 1px dashed rgba(0,212,255,0.4);`
- `.upload-zone dragger:hover { border-color: #1ABC9C; }` → `#00d4ff`
- `.upload-icon { color: #1ABC9C; }` → `#00d4ff`
- `.upload-text em { color: #1ABC9C; }` → `#00d4ff`
- `.upload-hint { color: #9ab8b2; }` → `#5a7a9a`
- `.section-title { border-left: 3px solid #1ABC9C; }` → `#00d4ff`
- `.dark-table .el-table { background: #ffffff; color: #2d4a45; }` → `background: #0d1b2e; color: #c0d8f0;`
- `.dark-table th { background: #f0f9f7; color: #6b8a84; border-bottom: 1px solid #d4ede8; }` → `background: #0f2540; color: #8bafc9; border-bottom: 1px solid #1a3a5c;`
- `.dark-table tr { background: #ffffff; }` → `#0d1b2e`
- `.dark-table td { border-bottom: 1px solid #e8f7f4; color: #2d4a45; }` → `border-bottom: 1px solid #1a3a5c; color: #c0d8f0;`
- `.empty-state description { color: #9ab8b2; }` → `#5a7a9a`

### 7. DeviceStatus.vue（10处）

- 内联 `style="color:#1ABC9C"` → `style="color:#00d4ff"` （行70设备详情图标）
- 内联 `style="color:#d4ede8"` → `style="color:#1a3a5c"` （行90占位符图标）
- `overviewItems` 中 `color: '#1ABC9C'` → `'#00d4ff'`
- `.ftc { background: #f5faf9; border: 1px solid #d4ede8; }` → `background: #0f2540; border: 1px solid #1a3a5c;`

### 8. Report.vue（10处）

**CSS 部分：**
- `.kpi-box { background: #f5faf9; border: 1px solid #d4ede8; }` → `background: #0f2540; border: 1px solid #1a3a5c;`
- `.kpi-l { color: #6b8a84; }` → `#8bafc9`
- `.score-bar { background: #f5faf9; border: 1px solid #d4ede8; }` → `background: #0f2540; border: 1px solid #1a3a5c;`
- `.score-label { color: #6b8a84; }` → `#8bafc9`
- `.score-value { color: #1a2e2a; }` → `#e0f0ff`
- `.chart-box { background: #f5faf9; border: 1px solid #d4ede8; }` → `background: #0f2540; border: 1px solid #1a3a5c;`
- `.chart-sub-title { color: #6b8a84; }` → `#8bafc9`
- `.ai-result-box { background: #f5faf9; border: 1px solid #d4ede8; color: #2d4a45; }` → `background: #0f2540; border: 1px solid #1a3a5c; color: #c0d8f0;`
- `.report-footer { border-top: 1px solid #d4ede8; color: #6b8a84; }` → `border-top: 1px solid #1a3a5c; color: #8bafc9;`
- `.card-title { color: #1a2e2a; }` → `#e0f0ff`

### 9. Register.vue（5处）

- `.auth-header h2 { color: #1a2e2a; }` → `#e0f0ff`
- `.auth-header p { color: #6b8a84; }` → `#8bafc9`
- `:deep(.el-input__wrapper) { background: #f5faf9; border: 1px solid #d4ede8; }` → `background: #0f2540; border: 1px solid #1a3a5c;`
- `:deep(.el-input__inner) { color: #1a2e2a; }` → `#e0f0ff`
- `:deep(::placeholder) { color: #9ab8b2; }` → `#5a7a9a`
- `:deep(.el-icon) { color: #9ab8b2; }` → `#5a7a9a`
- `.auth-footer { color: #6b8a84; }` → `#8bafc9`

---

## 三、后端连接异常排查

### 1. 检查后端是否运行
```bash
# 在 backend 目录执行
mvn spring-boot:run
```

### 2. 检查 application.properties 配置
```properties
# 确认MySQL连接信息正确
spring.datasource.url=jdbc:mysql://localhost:3306/energy_management
spring.datasource.username=root
spring.datasource.password=你的密码

# 确认端口
server.port=8080
```

### 3. 检查前端代理配置
文件：`frontend/vite.config.js`
```js
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true
    }
  }
}
```

### 4. 测试后端连通性
```bash
curl http://localhost:8080/api/dashboard/stats
```

### 5. 检查 CORS 配置
文件：`backend/src/main/java/com/energy/config/CorsConfig.java`
确保允许 `http://localhost:5173` 跨域访问。

---

## 四、快捷批量替换命令（可在 VS Code 中使用正则替换）

在 `frontend/src/views/` 目录下所有 `.vue` 文件中，按以下顺序执行全局替换：

1. `#1ABC9C` → `#00d4ff`（主色调，注意大小写）
2. `#148f77` → `#00a8cc`（渐变副色）
3. `#ffffff` → `#0d1b2e`（白色背景，**跳过 `color: #ffffff` 的情况**）
4. `#f5faf9` → `#0f2540`
5. `#f0f9f7` → `#0f2540`
6. `#e8f9f5` → `#0f2540`
7. `#e8f7f4` → `#1a3a5c`
8. `#d4ede8` → `#1a3a5c`
9. `#d1f2eb` → `#00d4ff22`
10. `#1a2e2a` → `#e0f0ff`（深绿文字→亮蓝白文字）
11. `#2d4a45` → `#c0d8f0`（次级文字）
12. `#6b8a84` → `#8bafc9`（辅助文字）
13. `#9ab8b2` → `#5a7a9a`（弱辅助文字）
14. `rgba(26,188,156` → `rgba(0,212,255`（阴影/透明色）

**⚠️ 替换后需手动检查：**
- 按钮文字 `color: #ffffff` 应保持不变（白色文字在深色按钮上）
- Login.vue 中已经改好的部分不要重复替换
