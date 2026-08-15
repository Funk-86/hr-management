---
theme: default
title: 智汇人事管理系统 可行性研究报告答辩
author: 韩宇坤
class: text-center
highlighter: shiki
lineNumbers: false
drawings:
  persist: false
transition: slide-left
mdc: true
canvasWidth: 1024
---

# 智汇人事管理系统
## 可行性研究报告答辩

<div class="pt-4 text-sm opacity-75">
  <p>所在专业：计算机科学与技术 | 班级：B2301 | 学号：0914423088 | 姓名：韩宇坤</p>
</div>

<div class="mt-8 grid grid-cols-5 gap-2 text-xs opacity-60">
  <div>1. 研究背景<br/>与目的</div>
  <div>2. 文献综述</div>
  <div>3. 可行性分析</div>
  <div>4. 系统架构<br/>与技术栈</div>
  <div>5. 核心功能<br/>模块</div>
  <div>6. 创新点</div>
  <div>7. 实施方案<br/>与技术路线</div>
  <div>8. 预期成果</div>
  <div>9. 推广及<br/>应用价值</div>
  <div>10. 总结与致谢</div>
</div>

---
layout: two-cols
class: text-left
---

# 研究背景与目的

<v-click>

## 📋 研究背景

- **企业痛点**：中小企业人事管理仍依赖 Excel + 纸质审批，数据分散、统计滞后、权限难控
- **技术成熟**：Spring Boot + Vue 前后端分离、JWT 认证等技术日趋成熟
- **市场缺口**：商业 HR SaaS 成本高、功能冗余，中小企业难以承受
- **实践价值**：自主可控、功能适中、可部署交付的人事管理系统具有明确必要性与实践价值

</v-click>

::right::

<div class="mt-16 pl-4">

<v-click>

## 🎯 研究目的

**一个平台 + 四个目标：**

1. **业务整合** — 部门、员工、考勤、请假、薪资统一管理
2. **流程规范** — 打卡、审批状态留痕，替代口头/纸质流转
3. **安全可控** — RBAC 权限 + JWT 认证 + 数据范围隔离
4. **技术实践** — 全栈开发（需求→设计→实现→部署）

</v-click>

</div>

---
layout: default
class: text-left
---

# 文献综述

<div class="grid grid-cols-2 gap-6 mt-4">

<div>

<v-click>

## 🔍 研究范围与策略

- **数据库来源**：CNKI、万方、Web of Science、IEEE Xplore
- **时间范围**：2020—2025 年
- **关键词**：HRM 系统、考勤管理、请假审批、Spring Boot、Vue、RBAC

</v-click>

<v-click>

## 📊 文献分类

| 阶段 | 关键技术 | 特点 |
|------|----------|------|
| 传统 | JSP/Servlet+SSH | 功能单一，档案为主 |
| 框架化 | Spring Boot+MyBatis | 模块化，REST 普及 |
| 智能化 | 微服务+前后端分离 | 移动打卡，数据分析 |

</v-click>

</div>

<div>

<v-click>

## 💡 核心发现

- 人事系统应**模块化、服务化**演进，子系统相对独立又数据互通
- **考勤与请假联动**是核心场景，需保证时间冲突校验与状态一致
- **前后端分离**已成为主流架构
- 中小企业场景下，**轻量级单体架构**性价比更高

</v-click>

<v-click>

## ⚠️ 研究空白（本课题切入点）

1. 多数案例偏理论，缺少**完整可运行的后端实现**
2. 考勤与请假常独立开发，**业务联动考虑不足**
3. 面向中小企业的**精简开源参考较少**

</v-click>

</div>

</div>

---
layout: default
class: text-left
---

# 可行性分析

<div class="grid grid-cols-3 gap-4 mt-6">

<div class="bg-blue-50 rounded-lg p-4 border border-blue-200">

<v-click>

## <mdi-cog class="text-blue-600"/> 技术可行性 <span class="text-blue-600 text-sm">✅ 高</span>

- **Java 17 + Spring Boot 4.1** — 生态成熟，文档完善
- **MyBatis-Plus + MySQL 8.0** — 高效数据操作
- **Spring Security + JWT** — 无状态认证
- **Vue 3 + Vite + Vben Admin** — 现代前端框架
- **Docker Compose** — 一键部署
- 核心业务模块**全部已实现**，并扩展任务协作 / 通知 / AI / 导出 / 操作审计

</v-click>

</div>

<div class="bg-green-50 rounded-lg p-4 border border-green-200">

<v-click>

## <mdi-cash class="text-green-600"/> 经济可行性 <span class="text-green-600 text-sm">✅ 高</span>

- **硬件**：现有 PC 即可，无需采购服务器
- **软件**：JDK、MySQL、IDEA 社区版、Node.js 等**全部免费/开源**
- **部署**：MySQL 社区版免费，Docker 本地运行
- **人力**：学生独立开发，无团队支出

<div class="mt-2 text-sm text-gray-600 italic">
  → 与商业 SaaS 按年按人订阅相比，几乎零直接经济门槛
</div>

</v-click>

</div>

<div class="bg-orange-50 rounded-lg p-4 border border-orange-200">

<v-click>

## <mdi-account-details class="text-orange-600"/> 操作可行性 <span class="text-orange-600 text-sm">✅ 高</span>

- **4 类角色**：超管 / HR / 部门经理 / 员工，分工清晰
- **界面友好**：Ant Design Vue 组件，表格/表单/日历即用
- **接口规范**：RESTful `/api` 统一前缀 + 规范响应格式
- **学习成本低**：员工端仅 3 步操作（登录→打卡→请假）
- **部署简单**：JAR 一键启动 or Docker 一键拉起

</v-click>

</div>

</div>

<div class="mt-6 text-center">
  <div class="text-lg font-bold text-gray-700">结论：技术可行 · 经济可行 · 操作可行 → 项目可启动</div>
</div>

---
layout: default
class: text-left
---

# 系统架构与技术栈

<div class="grid grid-cols-2 gap-4 mt-4">

<div>

```mermaid
graph TD
    A["🖥 浏览器 (Vue 3 + Vben Admin)"] -->|"HTTP /api"| B["🔧 Nginx (反向代理)"]
    B -->|"静态资源"| A
    B -->|"API 请求"| C["☕ Spring Boot 4.1"]
    C -->|"MyBatis-Plus"| D[("🗄 MySQL 8.0")]
    C -->|"Spring Security + JWT"| E["🔐 认证授权"]
    C -->|"SpringDoc OpenAPI"| F["📋 Swagger UI"]

    style A fill:#e1f5fe,stroke:#0288d1
    style C fill:#fff3e0,stroke:#f57c00
    style D fill:#e8f5e9,stroke:#388e3c
    style E fill:#fce4ec,stroke:#c62828
    style F fill:#f3e5f5,stroke:#7b1fa2
```

</div>

<div>

<v-click>

## 技术栈一览

| 层次 | 技术选型 |
|------|----------|
| 后端框架 | Spring Boot 4.1.0 |
| 安全框架 | Spring Security + JWT |
| 持久层 | MyBatis-Plus 3.5.16 |
| 数据库 | MySQL 8.0 |
| 前端框架 | Vue 3 + Vite + Pinia |
| UI 组件 | Ant Design Vue (Vben Admin) |
| 接口文档 | SpringDoc OpenAPI 3.0 |
| 构建工具 | Maven + pnpm |
| 反向代理 | Nginx |
| 容器化 | Docker + Docker Compose |

</v-click>

</div>

</div>

---
layout: default
class: text-left
---

# 核心功能模块

<div class="grid grid-cols-3 gap-4 mt-2">

<div>

```mermaid {scale: 0.65}
graph TD
    EMP["👤 员工"] --> UC1["🔐 登录"]
    EMP --> UC2["📊 工作台"]
    EMP --> UC5["⏰ 考勤打卡"]
    EMP --> UC6["📝 请假申请"]

    style EMP fill:#e3f2fd,stroke:#1976d2,color:#000
    style UC1 fill:#fff,stroke:#333
    style UC2 fill:#fff,stroke:#333
    style UC5 fill:#fff,stroke:#333
    style UC6 fill:#fff,stroke:#333
```

**员工视角** — 个人自助操作

</div>

<div>

```mermaid {scale: 0.65}
graph TD
    MGR["👔 管理人员"] --> M1["🔐 登录"]
    MGR --> M2["📊 工作台"]
    MGR --> M3["🏢 组织管理"]
    MGR --> M4["👥 员工管理"]
    MGR --> M5["⏰ 考勤管理"]
    MGR --> M6["📋 请假审批"]
    MGR --> M7["💰 薪资管理"]

    style MGR fill:#e8f5e9,stroke:#388e3c,color:#000
    style M1 fill:#fff,stroke:#333
    style M2 fill:#fff,stroke:#333
    style M3 fill:#fff,stroke:#333
    style M4 fill:#fff,stroke:#333
    style M5 fill:#fff,stroke:#333
    style M6 fill:#fff,stroke:#333
    style M7 fill:#fff,stroke:#333
```

**HR/经理视角** — 全部功能可操作

</div>

<div>

```mermaid {scale: 0.65}
graph TD
    ADMIN["⚙️ 超管"] --> A1["🔐 登录"]
    ADMIN --> A2["📊 工作台"]
    ADMIN --> A3["🏢 组织管理"]
    ADMIN --> A4["👥 员工管理"]
    ADMIN --> A5["⏰ 考勤管理"]
    ADMIN --> A6["📋 请假管理"]
    ADMIN --> A7["💰 薪资管理"]

    style ADMIN fill:#fce4ec,stroke:#c62828,color:#000
    style A1 fill:#fff,stroke:#333
    style A2 fill:#fff,stroke:#333
    style A3 fill:#fff,stroke:#333
    style A4 fill:#fff,stroke:#333
    style A5 fill:#fff,stroke:#333
    style A6 fill:#fff,stroke:#333
    style A7 fill:#fff,stroke:#333
```

**超级管理员** — 全部功能+代打卡

</div>

</div>

<div class="mt-2 grid grid-cols-4 gap-2 text-xs">
  <div class="bg-blue-50 p-1.5 rounded"><b>公共支撑</b><br/>统一响应格式/异常处理/逻辑删除</div>
  <div class="bg-green-50 p-1.5 rounded"><b>认证授权</b><br/>JWT 无状态 + RBAC 四级角色</div>
  <div class="bg-orange-50 p-1.5 rounded"><b>业务模块</b><br/>组织/员工/考勤/请假/任务/薪资/通知/AI</div>
  <div class="bg-purple-50 p-1.5 rounded"><b>部署方案</b><br/>Docker Compose + Nginx 一键启动</div>
</div>

---
layout: default
class: text-left
---

# 创新点

<div class="grid grid-cols-2 gap-4 mt-6">

<v-click>
<div class="flex gap-3 p-3 bg-blue-50 rounded-lg border-l-4 border-blue-500">
  <div class="text-2xl">🔗</div>
  <div>
    <b>角色化数据权限与 RBAC 融合</b>
    <p class="text-sm opacity-75">同一接口下，员工/经理/HR 获得不同数据视图，兼顾安全与简洁性</p>
  </div>
</div>
</v-click>

<v-click>
<div class="flex gap-3 p-3 bg-green-50 rounded-lg border-l-4 border-green-500">
  <div class="text-2xl">🔄</div>
  <div>
    <b>员工入职 — 账号一体化建档</b>
    <p class="text-sm opacity-75">同一事务中自动创建登录账号 + 加密密码 + 绑定角色</p>
  </div>
</div>
</v-click>

<v-click>
<div class="flex gap-3 p-3 bg-orange-50 rounded-lg border-l-4 border-orange-500">
  <div class="text-2xl">📌</div>
  <div>
    <b>多场景打卡 + 健壮性设计</b>
    <p class="text-sm opacity-75">自助打卡 / 超管代打 / HR 补录，兼容"先补录再更新"防索引冲突</p>
  </div>
</div>
</v-click>

<v-click>
<div class="flex gap-3 p-3 bg-purple-50 rounded-lg border-l-4 border-purple-500">
  <div class="text-2xl">🛡</div>
  <div>
    <b>请假业务规则完备化</b>
    <p class="text-sm opacity-75">时间冲突检测 + 天数校验 + 审批/驳回/撤销状态机 + 角色过滤待办</p>
  </div>
</div>
</v-click>

<v-click>
<div class="flex gap-3 p-3 bg-red-50 rounded-lg border-l-4 border-red-500">
  <div class="text-2xl">🎯</div>
  <div>
    <b>角色化工作台</b>
    <p class="text-sm opacity-75">按角色动态展示考勤月历、统计指标、快捷入口，提升差异化使用体验</p>
  </div>
</div>
</v-click>

<v-click>
<div class="flex gap-3 p-3 bg-teal-50 rounded-lg border-l-4 border-teal-500">
  <div class="text-2xl">📦</div>
  <div>
    <b>轻量级全栈可部署架构</b>
    <p class="text-sm opacity-75">单体 + 前端 + Nginx + Docker Compose，降低学习、演示与二次开发门槛</p>
  </div>
</div>
</v-click>

</div>

---
layout: default
class: text-left
---

# 实施方案与技术路线

<div class="grid grid-cols-2 gap-4 mt-2">

<div>

<v-click>

## 🔬 研究方法（五阶段）

```mermaid {scale: 0.65}
graph LR
    A["📝<br/>需求调研"] --> B["🔍<br/>系统分析"]
    B --> C["📐<br/>系统设计"]
    C --> D["💻<br/>系统实现"]
    D --> E["✅<br/>系统测试"]

    A -.->|"文献+流程梳理"| A
    B -.->|"UML用例/活动/状态图"| B
    C -.->|"类图/时序图/E-R/DDL"| C
    D -.->|"后端→前端→部署联调"| D
    E -.->|"接口/场景/Docker测试"| E

    style A fill:#e3f2fd,stroke:#1976d2
    style B fill:#e1f5fe,stroke:#0288d1
    style C fill:#fff3e0,stroke:#f57c00
    style D fill:#e8f5e9,stroke:#388e3c
    style E fill:#fce4ec,stroke:#c62828
```

</v-click>

<v-click>

## 🛠 开发工具链

| 类别 | 工具 | 用途 |
|------|------|------|
| IDE | IntelliJ IDEA / VS Code | 后端 / 前端开发 |
| 构建 | Maven / pnpm | 依赖管理 |
| 数据库 | MySQL 8.0 + Navicat | 存储与管理 |
| 调试 | Postman / Swagger | 接口测试 |
| 部署 | Docker + Nginx | 容器化一键启动 |

</v-click>

</div>

<div>

<v-click>

## 📅 实施进度甘特图

```mermaid {scale: 0.55}
gantt
    title 项目实施计划 (2026.09 — 2027.04)
    dateFormat  YYYY-MM-DD
    axisFormat  %m-%d

    section 准备阶段
    资料搜集与文献调研           :2026-09-01, 17d
    开题报告撰写与答辩           :2026-09-18, 23d

    section 设计阶段
    需求分析与UML建模            :2026-10-11, 51d
    系统设计与数据库设计          :2026-12-01, 31d

    section 实现阶段
    后端模块开发                 :2027-01-01, 30d
    前端页面开发与联调            :2027-01-20, 40d

    section 收尾阶段
    系统测试与完善               :2027-03-01, 10d
    论文撰写                     :2027-03-11, 35d
    论文修改与答辩准备            :2027-04-16, 15d
```

</v-click>

</div>

</div>

---
layout: default
class: text-left
---

# 预期成果与推广价值

<div class="grid grid-cols-2 gap-4 mt-4">

<div>

<v-click>

## 🎁 预期成果

**软件成果：**
- ✅ Spring Boot 后端 JAR + 完整 REST API
- ✅ Vue Vben Admin 前端 Web 应用
- ✅ MySQL 初始化脚本（schema + seed data）
- ✅ Docker Compose 一键部署方案

**文档成果：**
- ✅ 可行性研究报告、需求分析说明书
- ✅ 数据库设计文档、系统测试报告
- ✅ 项目 README（快速开始 + 部署指南）

**能力成果：**
- ✅ 全栈开发能力（Spring Boot → Vue → Docker）
- ✅ 独立设计实现中小型管理信息系统

</v-click>

</div>

<div>

<v-click>

## 📈 推广及应用价值

**经济效益**
- 降低管理成本，减少人工 Excel 维护
- 免去商业 SaaS 高昂的按年订阅费用
- 提高数据准确性，减少考勤纠纷

**社会效益**
- 促进中小企业人事管理规范化
- 保障敏感人事数据安全可控
- 服务教学实践，培养全栈人才

**推广策略**
- 校内开源共享 → 小范围试点 → 已落地：Excel 导出 / 操作审计 / 任务看板附件 / AI 助手；后续可扩展移动打卡

</v-click>

</div>

</div>

---
layout: center
class: text-center
---

# 感谢聆听

## 恳请各位老师批评指正

<div class="mt-12 grid grid-cols-3 gap-8 text-sm opacity-75">

<div>
  <div class="text-3xl mb-2">☕</div>
  <b>技术栈</b><br/>
  Spring Boot 4.1 + Vue 3<br/>
  MyBatis-Plus + MySQL 8.0<br/>
  Docker + Nginx
</div>

<div>
  <div class="text-3xl mb-2">📦</div>
  <b>已实现模块</b><br/>
  组织/员工/考勤/请假/任务/薪资<br/>
  通知·AI·导出·操作日志<br/>
  Docker + Nginx 可部署
</div>

<div>
  <div class="text-3xl mb-2">🎓</div>
  <b>项目信息</b><br/>
  韩宇坤 · 0914423088<br/>
  计算机科学与技术 B2301<br/>
  指导教师：
</div>

</div>

<div class="mt-8 text-xs opacity-50">
  <p>智汇人事管理系统 — 基于 Spring Boot + Vue 前后端分离架构的中小企业人力资源管理平台</p>
</div>
