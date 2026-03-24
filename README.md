# PPM 供应商质量数据分析系统

PPM（Parts Per Million）供应商质量数据分析系统，用于统计与分析供应商产品质量，通过处理可疑物料统计和供应商供货量数据，计算各基地、供应商的 PPM 值，并提供多维度分析与可视化展示。

## 技术栈

| 层次 | 技术选型 |
|------|----------|
| **前端** | Vue 3 + Vite + Element Plus + ECharts + Pinia |
| **后端** | Spring Boot 3.x + Spring Data JPA + EasyExcel |
| **数据库** | MySQL 8.0 |
| **部署** | Docker + Docker Compose |

## 项目结构

```
ppm-system/
├── ppm-backend/          # Spring Boot 后端服务
│   ├── src/main/java/    # Java 源代码
│   └── pom.xml           # Maven 配置
├── ppm-frontend/         # Vue 3 前端应用
│   ├── src/              # 前端源代码
│   └── package.json      # npm 配置
├── inventory-mq/         # MQ 消息服务（可选）
├── docs/                 # 项目文档
├── docker-compose.yml    # Docker 编排配置
└── README.md             # 项目说明
```

## 核心功能

- **数据导入**：支持 Excel 导入可疑物料统计、供应商供货量数据
- **PPM 计算**：自动计算供应商 PPM 和基地品牌 PPM
- **故障分析**：故障类别分析、失效模式分析
- **数据可视化**：饼图、直方图、Top 排名等多维度图表
- **综合查询**：支持多条件筛选查询与数据导出

## 快速开始

### 环境要求

- Docker 20.10+
- Docker Compose 2.0+
- 内存：建议 4GB+

### Docker 部署

```bash
# 1. 克隆项目
git clone <repository-url>
cd ppm-system

# 2. 启动所有服务
docker-compose up -d

# 3. 查看服务状态
docker-compose ps
```

### 访问地址

| 服务 | 地址 | 说明 |
|------|------|------|
| 前端页面 | http://localhost | 默认 80 端口 |
| 后端 API | http://localhost:8080 | REST API |
| API 文档 | http://localhost:8080/swagger-ui.html | Swagger UI |

### 默认账号

- MySQL root 密码：`fuqiuyang030828`
- 数据库名：`ppm_db`

## 开发环境运行

### 后端启动

```bash
cd ppm-backend
mvn spring-boot:run
```

### 前端启动

```bash
cd ppm-frontend
npm install
npm run dev
```

## 主要端口

| 服务 | 端口 | 说明 |
|------|------|------|
| MySQL | 3306 | 数据库服务 |
| ppm-backend | 8080 | 后端服务 |
| ppm-frontend | 80 | 前端服务 |

## 文档

项目详细文档位于 `docs/` 目录：

- `01-PPM系统需求文档.md` - 功能需求说明
- `01-技术栈与架构.md` - 技术架构设计
- `PPM多维度计算设计.md` - PPM 计算逻辑
- `软件测试报告.md` - 测试报告

## 常用命令

```bash
# 查看日志
docker-compose logs -f

# 停止服务
docker-compose down

# 重启服务
docker-compose restart

# 重新构建
docker-compose up -d --build
```

## PPM 计算公式

```
PPM = (不合格数 / 供货量) × 1,000,000
```

计算维度支持：基地 × 供应商 × 月份

## 支持基地

- 河西
- 宝骏
- 青岛
- 重庆
