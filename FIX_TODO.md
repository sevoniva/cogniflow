# ChatBI 修复清单

## P0 — 安全/功能阻断

- [x] **1.** 加密密钥改为环境变量注入，删除硬编码 `chatbi-encryption-key-2026`
- [x] **2.** AES/ECB 改为 AES/GCM
- [x] **3.** JWT 默认密钥改为 dev-only 标记（生产通过环境变量覆盖）
- [x] **4.** 删除所有 Controller 上的 `@CrossOrigin(origins = "*")`
- [x] **5.** CORS 配置改为从配置文件读取具体域名
- [x] **6.** 前端 HTTP 客户端注入 JWT Authorization header
- [x] **7.** 前端添加路由守卫 + 登录页
- [x] **8.** 删除 `DEFAULT_USER_ID = 1`，从 store 获取真实用户 ID
- [x] **9.** 修复 `ConversationMessageItem` 的 `feedback` emit 声明
- [x] **10.** 修复 SQL 拼接（IN 操作符参数化 + 标识符清洗）
- [x] **11.** 管理员密码不再打印到日志
- [x] **12.** 密码强度校验接入代码
- [x] **13.** 用户接口返回时隐藏密码字段（@JsonIgnore）
- [x] **14.** Token 刷新接口改为 POST body
- [x] **15.** 重置密码/修改密码接口改为 POST body
- [x] **16.** Actuator 端点加认证（仅 health/info 公开）

## P1 — 架构/体验

- [x] **17.** 前端 Dashboard ECharts 实例 dispose（修复内存泄漏）- AgileDashboard/SalesDashboard/OperationDashboard
- [x] **18.** 修复 fire-and-forget 异步调用（history/favorite 页面 await）
- [x] **19.** Dashboard 清空画布添加确认弹窗
- [x] **20.** 统一 API 响应格式，删除 `ApiResponse`，统一用 `Result`
- [x] **21.** 删除重复的 `dto.PageResult`，统一用 `common.PageResult`
- [x] **22.** 提取 QueryController/ConversationController 指标匹配逻辑到 Service
- [x] **23.** 前端 Dashboard 页面添加 Loading 状态
- [ ] **24.** 统一 HTTP 客户端，删除双客户端
- [x] **25.** 导出接口添加大小限制
- [ ] **26.** SSO 回调 URL 从配置读取，token 用 fragment 传递
- [x] **27.** Controller 直接注入 Mapper 改为通过 Service 调用
- [ ] **28.** WebSocket 断线重连
- [x] **29.** SSE 解析支持多行 data

## P2 — 体验/可维护

- [ ] **30.** 提取公共 CRUD composable（消除 2000 行重复）
- [ ] **31.** 提取 ConversationQuery/ConversationMessageItem 共享类型和函数
- [ ] **32.** 统一 ECharts 使用方式（树摇导入）
- [ ] **33.** Geo-Map 页移除或实现真实功能
- [ ] **34.** Dashboard 编辑器返回前检查未保存更改
- [ ] **35.** 修复 `handleDialogClose` 破坏响应式问题
- [ ] **36.** 删除死代码（未使用的 store、重复路由）
- [ ] **37.** 响应式适配（Dashboard/Admin 页面）
- [ ] **38.** 统一 CSS 变量（消除硬编码色值）

## 进度

| 阶段 | 状态 |
|------|------|
| P0 安全/功能阻断 | ✅ 已完成 (16/16) |
| P1 架构/体验 | 🔄 进行中 (10/13) |
| P2 体验/可维护 | 待开始 (0/9) |
| 单元测试 | ✅ 50/50 通过 |
| Docker 部署 | ✅ 已部署运行 |
