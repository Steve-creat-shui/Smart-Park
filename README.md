# 建筑能源数字孪生管理系统

> 服创赛 A08 赛题 - 基于数字孪生技术的智慧建筑能源管理平台

## 系统架构

```
前端 (Vue.js + ECharts)  ←→  后端 (Spring Boot)  ←→  MySQL 数据库
                                      ↕
                              Claude AI 大模型
```

## 技术栈

| 层级 | 技术 |
|------|------|
| 前端框架 | Vue 3 + Vue Router + Pinia |
| UI 组件库 | Element Plus |
| 数据可视化 | ECharts 5 + vue-echarts |
| HTTP 请求 | Axios |
| 报告导出 | jsPDF + Apache POI (Excel) |
| 后端框架 | Spring Boot 3.2 |
| 数据访问 | Spring Data JPA + MySQL 8.0 |
| AI 集成 | Claude API (Anthropic) |
| 构建工具 | Vite 5 / Maven 3 |
| 容器化 | Docker + Docker Compose |

## 功能模块

- **系统总览 Dashboard** - 实时能耗监控、建筑排行、月度趋势
- **能耗分析** - 多维度能耗统计、异常检测、能效评级
- **设备状态** - 空调系统监控、设备健康度、分时能耗
- **AI 智能分析** - Claude 大模型驱动的自然语言问答与故障诊断
- **报告生成** - 一键生成能效报告，支持 Excel/PDF 导出

## 快速启动

### 方式一：本地开发

#### 1. 数据库初始化

```bash
# 连接 MySQL，执行初始化脚本
mysql -u root -p < sql/init.sql
```

#### 2. 配置后端

编辑 `backend/src/main/resources/application.properties`：

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/energy_management?...
spring.datasource.username=root
spring.datasource.password=你的密码

# 可选：配置 Claude API Key 获得真实 AI 分析
claude.api.key=sk-ant-xxxxx
```

#### 3. 启动后端

```bash
cd backend
mvn spring-boot:run
# 后端运行在 http://localhost:8080
```

#### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
# 前端运行在 http://localhost:5173
```

### 方式二：Docker 一键部署

```bash
# 设置 Claude API Key（可选）
set CLAUDE_API_KEY=sk-ant-xxxxx   # Windows
export CLAUDE_API_KEY=sk-ant-xxxxx # Linux/Mac

# 启动全部服务
docker-compose up -d

# 查看服务状态
docker-compose ps
```

服务地址：
- 前端：http://localhost:5173
- 后端 API：http://localhost:8080
- MySQL：localhost:3306

## API 接口文档

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/energy-consumption/{building_id}` | 查询建筑能耗数据 |
| GET | `/api/energy-consumption/{building_id}/latest` | 获取最新能耗数据 |
| GET | `/api/energy-consumption/{building_id}/anomalies` | 获取异常记录 |
| POST | `/api/energy-consumption` | 新增能耗数据 |
| GET | `/api/energy-consumption/{building_id}/monthly-trend` | 月度趋势 |
| POST | `/api/energy-analysis` | 能效综合分析 |
| POST | `/api/ai-query` | AI 自然语言查询 |
| GET | `/api/energy-analysis/{building_id}/issue` | 能耗异常诊断 |
| GET | `/api/report/{building_id}` | 生成能效报告 |
| GET | `/api/report/{building_id}/export` | 导出 Excel 报告 |
| GET | `/api/buildings` | 获取所有建筑 |
| GET | `/api/dashboard/stats` | 获取 Dashboard 统计 |

## 数据库表结构

### buildings（建筑信息表）

| 字段 | 类型 | 说明 |
|------|------|------|
| building_id | VARCHAR(50) | 建筑编号（主键） |
| building_name | VARCHAR(100) | 建筑名称 |
| building_type | VARCHAR(50) | 建筑类型 |
| floor_count | INT | 楼层数 |
| total_area_m2 | DOUBLE | 总面积(m²) |
| energy_rating | VARCHAR(10) | 能效等级 |
| ac_system_type | VARCHAR(50) | 空调类型 |

### energy_consumption（能耗数据表）

| 字段 | 类型 | 说明 |
|------|------|------|
| building_id | VARCHAR(50) | 建筑编号 |
| timestamp | DATETIME | 监测时间 |
| power_consumption_kwh | DOUBLE | 电力消耗(kWh) |
| water_consumption_m3 | DOUBLE | 水能耗(m³) |
| carbon_emission_kg | DOUBLE | 碳排放量(kg) |
| ac_consumption_kwh | DOUBLE | 空调能耗(kWh) |
| lighting_consumption_kwh | DOUBLE | 照明能耗(kWh) |
| temperature_celsius | DOUBLE | 室内温度(°C) |
| humidity_percent | DOUBLE | 湿度(%) |
| status | VARCHAR(20) | 状态(normal/warning) |

## Claude API 说明

本系统集成了 Anthropic Claude API 用于智能分析。

- **已配置 Key**：系统将调用 Claude API 进行真实的 AI 分析
- **未配置 Key**：系统切换到本地分析模式，基于规则引擎提供基础建议

获取 Claude API Key：https://console.anthropic.com/

## 目录结构

```
2/
├── backend/                    # Spring Boot 后端
│   ├── src/main/java/com/energy/
│   │   ├── config/             # CORS 配置
│   │   ├── controller/         # REST API 控制器
│   │   ├── dto/                # 数据传输对象
│   │   ├── entity/             # JPA 实体类
│   │   ├── repository/         # 数据访问层
│   │   └── service/            # 业务逻辑层（含 Claude 集成）
│   ├── src/main/resources/
│   │   └── application.properties
│   ├── Dockerfile
│   └── pom.xml
├── frontend/                   # Vue.js 前端
│   ├── src/
│   │   ├── api/                # Axios API 封装
│   │   ├── router/             # Vue Router
│   │   └── views/              # 页面组件
│   │       ├── Dashboard.vue   # 系统总览
│   │       ├── EnergyAnalysis.vue  # 能耗分析
│   │       ├── DeviceStatus.vue    # 设备状态
│   │       ├── AIAnalysis.vue      # AI智能分析
│   │       └── Report.vue          # 报告生成
│   ├── package.json
│   └── vite.config.js
├── sql/
│   └── init.sql               # 数据库初始化脚本（含测试数据）
├── docker-compose.yml
└── README.md
```
