# ChatBI 项目改造路线图

> 基于代码深度分析 + 开源生态调研，针对"自研过重、扩展受限"问题的系统性改造方案。

---

## 一、问题总览

这个项目的技术债务可以归纳为三类：

| 类型 | 说明 | 典型模块 |
|------|------|---------|
| **重复造轮子** | 已有成熟开源方案，但选择了自研 | HTTP层、动态数据源、LLM适配、缓存 |
| **扩展性陷阱** | 当前能跑，但加一个新功能就要改N处 | 图表系统(两套独立配置)、Dashboard Editor、NL2SQL Prompt |
| **架构天花板** | 技术选型限制了产品能力上限 | 无流式对话、无RAG、无子查询/CTE、Hash脱敏=字符串反转 |

---

## 二、前端改造方案

### 2.1 状态管理：从"URL传参"到 Pinia + TanStack Query

**现状问题**
- 无全局状态管理，跨页面靠 `router.push({ query: { q: ... } })`
- `ConversationQuery.vue` 3129行单体组件，所有状态堆在本地 `ref`
- ECharts 实例用数组 `index` 做 Map key，删除消息后图表错配

**改造方案**
```
Pinia（客户端状态）+ TanStack Query Vue（服务端状态）
```

| 状态类型 | 现状 | 改造后 |
|---------|------|--------|
| 查询上下文 | URL query | Pinia `queryStore`（当前问题、历史问题、偏好设置） |
| 对话状态 | 本地 `messages[]` | Pinia `conversationStore`（消息树、分支、流式缓冲区） |
| 图表实例 | `Map<number, ECharts>` | `Map<messageId, ECharts>`，消息删了实例自动释放 |
| 服务端数据 | 每次请求都发 | TanStack Query 自动缓存、后台刷新、乐观更新 |

**关键改造点**
- `views/chatbi/query/index.vue` → `queryStore.init(question)`
- `views/chatbi/result/index.vue` → `useQuery({ queryKey: ['result', question], queryFn: ... })`
- `views/ConversationQuery.vue` 拆分为：`MessageList.vue` + `MessageChartRenderer.vue` + `DiagnosisPanel.vue`

---

### 2.2 HTTP层：自研 fetch → ofetch

**现状问题**
- `utils/http.ts` 160行，只有基础请求 + 超时，缺拦截器/重试/取消/缓存
- `apiAdapter.ts` 604行上帝对象，所有 API 手写重复代码
- `http.ts` 混入了 `formatDateTime`、`slugifyCode` 等无关工具

**改造方案**
```
ofetch（底层）+ openapi-typescript（类型生成）
```

```typescript
// 改造前：手写 URL、手动类型断言
const data = await request<any>('/api/metrics', { method: 'GET' })

// 改造后：类型安全的 API 客户端
const { data } = await $api('/metrics', { method: 'get' })  // 类型自动推断 Metric[]
```

**实施步骤**
1. 用 `ofetch` 替换 `fetch`，获得拦截器、自动重试、请求取消
2. 后端接入 `springdoc-openapi`，生成 OpenAPI JSON
3. 前端用 `openapi-typescript` 自动生成 TS 类型，彻底消灭 `any`
4. `apiAdapter.ts` 按领域拆分为 `metricApi.ts`、`dashboardApi.ts`、`conversationApi.ts`

---

### 2.3 图表系统：两套独立配置 → 统一后端配置驱动

**现状问题**
- `ChatChart.vue`（602行 switch-case）和 `ConversationQuery.vue` 的 `buildChartOption`（400行）是**两套完全独立的 ECharts 配置逻辑**
- 同一图表参数不一致（饼图半径 `['40%','70%']` vs `['36%','68%']`）
- 16 个包装组件（`ChatBarChart.vue` 等）95% 代码相同
- 119 种图表变体硬编码在前端，新增图表需改代码重部署

**改造方案：BFF 模式——后端配置驱动前端渲染**

```typescript
// 后端返回图表配置（不是数据，是完整的 ECharts option 模板）
interface ChartConfig {
  type: 'bar' | 'line' | 'pie'
  style: 'classic' | 'enterprise' | 'minimal'
  option: EChartsOption        // 后端预生成的 ECharts 配置模板
  dataMapping: DataMappingRule // 数据字段映射规则
}

// 前端只需：ChartRenderer.vue
<template>
  <v-chart :option="mergedOption" />
</template>

<script setup>
const props = defineProps<{ config: ChartConfig, data: any[] }>()
const mergedOption = computed(() => mergeOption(props.config.option, props.data, props.config.dataMapping))
</script>
```

**遗留 ECharts 主题系统处理**
- 用 ECharts 原生 `registerTheme()` 管理 7 种风格变体
- 不要手写两套 option 生成器

**实施步骤**
1. 后端新增 `ChartConfigService`：根据指标类型 + 用户偏好 → 返回标准 ECharts option
2. 前端合并 `ChatChart.vue` + `buildChartOption` → 统一的 `ChartRenderer.vue`
3. 16 个包装组件 → 1 个通用组件 `type="bar"`
4. `chartCatalog.ts` 从后端 `/chart-catalog/config` 动态加载

---

### 2.4 Dashboard Editor：自研绝对定位 → GridStack.js

**现状问题（最严重的前端问题）**
- `dashboard/editor.vue`（1284行）**完全没有用 vue-grid-layout**，而是自己用 `left/top/px` 实现拖拽
- 无网格吸附、无碰撞检测、无响应式、组件可任意重叠
- 序列化直接 `JSON.stringify(components)`，无版本控制

**改造方案**
```
GridStack.js（替换 editor.vue 的自研拖拽系统）
```

GridStack.js 对比现状：

| 能力 | 自研 editor | GridStack.js |
|------|-----------|-------------|
| 网格吸附 | ❌ 任意像素 | ✅ 自动 snap to grid |
| 碰撞检测 | ❌ 可重叠 | ✅ 自动推移/交换 |
| 响应式 | ❌ 固定 px | ✅ 多断点自适应（xs/sm/md/lg） |
| 嵌套布局 | ❌ 不支持 | ✅ 子网格 |
| 序列化版本 | ❌ 直接 JSON | ✅ 带元数据的标准化 layout |
| React/Vue/Angular | - | ✅ 官方 wrapper |

**实施步骤**
1. 移除 editor.vue 中的 `onDrop`/`startResize`/`startDrag` 自研逻辑
2. 引入 `gridstack-vue` wrapper
3. layout 数据格式迁移：`{x, y, w, h, id}`（GridStack 标准）
4. 保留 `vue-grid-layout` 在 DashboardPanel.vue（只读模式），或统一迁移

---

### 2.5 对话系统：普通 POST → SSE 流式输出

**现状问题**
- `sendMessage` 是同步 POST，"白屏等待 → 突然弹出"
- 无多轮上下文分支、无重新生成、无消息编辑
- 3129 行单体组件

**改造方案**
```
SSE (Server-Sent Events) 流式输出 + Pinia 状态拆分
```

```typescript
// conversationStore.ts
interface ConversationState {
  messages: Message[]
  branches: Map<messageId, Message[]>  // 支持分支
  streamingContent: string             // 当前流式缓冲区
  isStreaming: boolean
}

// 流式接收
const eventSource = new EventSource(`/api/conversation/${id}/stream`)
eventSource.onmessage = (e) => {
  conversationStore.appendChunk(JSON.parse(e.data))
}
```

**组件拆分**
```
ConversationQuery.vue (3129行)
├── ConversationLayout.vue      # 布局骨架
├── MessageList.vue             # 消息列表（虚拟滚动）
│   ├── UserMessage.vue
│   └── AssistantMessage.vue
│       ├── MessageText.vue     # Markdown 渲染
│       ├── MessageChart.vue    # ECharts 实例（用 messageId 做 key）
│       └── DiagnosisPanel.vue  # 诊断信息（从 result 和 conversation 复用）
├── InputBox.vue                # 输入框
└── SuggestionBar.vue           # 推荐问题
```

---

## 三、后端改造方案

### 3.1 NL2SQL + LLM适配：自研 OkHttp → LangChain4j

**现状问题**
- `AiModelService.java`（489行）：6 个 provider 各写一套 HTTP 调用（RestTemplate + OkHttp 混用）
- `AiQueryService.java`（419行）：Prompt 用 `StringBuilder` 硬编码，无模板、无 RAG、无 Few-shot 动态加载
- `TextToSqlConverter.java`（624行）：纯正则中文 NL2SQL，强制依赖 `created_at` 字段，JOIN 条件写死 `id = table_id`
- pom.xml 引入了 `openai-gpt3-java` SDK，但**代码里完全没用**

**改造方案**
```
LangChain4j（Java 版 LangChain）+ RAG 增强
```

**核心替换**

| 能力 | 现状 | LangChain4j 改造 |
|------|------|-----------------|
| 多模型统一调用 | 6 个手写方法 | `ChatLanguageModel` 统一接口，自动适配 15+ provider |
| Prompt 模板 | StringBuilder 硬编码 | `PromptTemplate` + `@SystemMessage` + `@UserMessage` |
| RAG 增强 | ❌ 无 | `EmbeddingStoreIngestor` 注入表结构/历史查询/业务术语 |
| 对话记忆 | ❌ 无 | `ChatMemory` 管理多轮上下文 |
| 流式响应 | ❌ 无 | `StreamingChatLanguageModel` → SSE 输出 |
| 结构化输出 | 手写正则提取 SQL | `OutputParser` 自动映射 POJO |
| 工具调用 | ❌ 无 | `@Tool` 注解，AI 可调用"查询执行""指标检索"等工具 |

**RAG 架构设计**
```
用户问题
  ↓
[Embedding Model] → 向量检索
  ↓
知识库（向量存储）
  ├── 表结构 DDL + 字段注释
  ├── 历史成功查询 SQL（正确示例）
  ├── 业务术语映射（"GMV" → "order_amount"）
  └── 维度枚举值（"华东区" → "region='east'"）
  ↓
RetrievalAugmentor → 注入到 Prompt 上下文
  ↓
LLM 生成 SQL（带 Few-shot 示例）
```

**Prompt 模板化示例**
```java
@SystemMessage("""
  你是一个数据分析师助手。根据以下信息生成 SQL：
  {{tableSchema}}
  {{businessTerms}}
  {{historicalQueries}}
  要求：仅返回 SQL，不加解释，使用反引号包裹标识符，默认 LIMIT 100
  """)
@UserMessage("{{userQuestion}}")
String generateSql(@V("tableSchema") String schema,
                   @V("businessTerms") String terms,
                   @V("historicalQueries") String examples,
                   @V("userQuestion") String question);
```

**规则引擎保留但重构**
- `TextToSqlConverter` 保留作为**离线兜底**
- 将正则规则改为基于 **ANTLR 语法解析** 或接入 **JSqlParser 的 Expression 解析**
- 解除对 `created_at` 字段的硬编码依赖

---

### 3.2 SQL 治理：JSQLParser → Apache Calcite（渐进式）

**现状问题**
- `QueryGovernanceService.java`（348行）基于 JSQLParser 4.9
- **明确不支持子查询、CTE、UNION**（代码里直接抛异常）
- 复杂方言（ClickHouse、Hive）支持不完整

**改造方案：渐进式迁移**

**阶段 1（立即）：保留 JSQLParser，解除限制**
- JSQLParser 4.9 其实支持子查询/CTE/UNION，只是代码里写死了 `throw new UnsupportedOperationException`
- 先去掉这些限制，让 JSQLParser 正确解析 AST
- 行级权限注入从手动 AST 拼接改为 **Visitor 模式**

**阶段 2（中期）：引入 Apache Calcite**
- Calcite 提供完整的 SQL 解析、验证、关系代数优化、联邦查询
- 适合 ChatBI 未来的**跨库 JOIN** 场景（MySQL + ClickHouse 联邦查询）

```java
// Calcite 验证 + 改写
SqlParser parser = SqlParser.create(sql, parserConfig);
SqlNode sqlNode = parser.parseQuery();
sqlNode.accept(new RowPermissionVisitor(dataPermissions));  // 注入权限
SqlNode validated = validator.validate(sqlNode);            // 语义验证
```

**阶段 3（长期）：ShardingSphere 集成**
- 如果需要数据分片、透明脱敏、SQL 审计，直接用 ShardingSphere
- ShardingSphere 底层就是 Calcite，能力完整

---

### 3.3 动态数据源：自研 RoutingDataSource → baomidou dynamic-datasource

**现状问题**
- `DynamicDataSourceConfig.java`（74行）+ `DynamicDataSourceRegistry.java`（56行）
- 只注册不热更新，`targetDataSources` 初始化后不可变
- 无健康检查、无负载均衡、无故障转移

**改造方案**
```
baomidou dynamic-datasource-spring-boot-starter
```

| 功能 | 自研 | dynamic-datasource |
|------|------|-------------------|
| 注解切换 | `@DataSourceSwitch` + Aspect | `@DS("slave")` + 自动拦截 |
| 动态添加 | Map 只存不更新 | `DynamicRoutingDataSource.addDataSource()` 实时生效 |
| 健康检查 | ❌ 无 | ✅ 内置 |
| 负载均衡 | ❌ 无 | ✅ 轮询/随机/权重 |
| 故障转移 | ❌ 无 | ✅ 主从切换 |
| 分布式事务 | ❌ 无 | ✅ Seata 集成 |

**实施步骤**
1. 移除 `DynamicDataSourceConfig`、`DynamicDataSourceContextHolder`、`DynamicDataSourceRegistry`
2. 引入 `dynamic-datasource-spring-boot-starter`
3. `DataSourceSwitchAspect` → `@DS` 注解
4. 数据源配置从代码注册改为 YML/数据库配置 + 动态刷新

---

### 3.4 数据权限 + 脱敏：自研 AST 拼接 → MyBatis-Plus 插件 + ShardingSphere

**现状问题**
- `DataPermissionService.java`（240行）：手动用 JSQLParser 构建 `EqualsTo`/`InExpression`
- 只支持单字段单条件，不支持 `OR` 组合、不支持"本部门及下级"树形权限
- `DataMaskingService.java`（250行）：
  - `HASH` = `value.hashCode()`（可逆）
  - `ENCRYPT` = `new StringBuilder(value).reverse()`（字符串反转，根本不是加密）
  - 在 Java 层后处理，数据已在内存中暴露

**改造方案**

**行级权限：MyBatis-Plus 数据权限插件**
```java
// MyBatis-Plus DataPermissionInterceptor
@DataPermission(
  deptId = "dept_id",
  rule = DataPermissionRule.DEPT_AND_CHILD  // 本部门及下级
)
public interface MetricMapper extends BaseMapper<Metric> {}
```

**数据脱敏：ShardingSphere EncryptRule（透明化）**
```yaml
# ShardingSphere 配置
rules:
  - !ENCRYPT
    tables:
      t_user:
        columns:
          phone:
            cipherColumn: phone_cipher
            encryptorName: aes_encryptor
          id_card:
            cipherColumn: id_card_cipher
            encryptorName: aes_encryptor
```

| 维度 | 自研 Java 后处理 | ShardingSphere 透明脱敏 |
|------|----------------|------------------------|
| 安全性 | 数据已在内存中暴露 | SQL 改写，数据库返回的就是脱敏后数据 |
| 算法 | hashCode / 字符串反转 | AES、SM4、国密标准 |
| 应用侵入性 | 每次查询后手动调用 mask | 零侵入，应用无感知 |
| 合规性 | 不符合等保/数据安全法 | 符合企业安全审计 |

---

### 3.5 缓存：手动双写 → Spring Cache + Caffeine

**现状问题**
- `CacheService.java`（190行）手动管理 `ConcurrentHashMap` + Redis
- `ConcurrentHashMap` 无限增长，无驱逐策略
- pom.xml 引入了 Caffeine，但**代码里完全没用**
- 无 Spring Cache 注解

**改造方案**
```
Spring Cache 抽象 + Caffeine（本地）+ Redis（分布式）
```

```java
@Configuration
@EnableCaching
public class CacheConfig {
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        // Caffeine 本地缓存（热数据）
        CaffeineCacheManager local = new CaffeineCacheManager();
        local.setCaffeine(Caffeine.newBuilder().maximumSize(1000).expireAfterWrite(10, TimeUnit.MINUTES));
        
        // Redis 分布式缓存
        RedisCacheManager redis = RedisCacheManager.builder(factory).build();
        
        // 多级缓存
        return new CompositeCacheManager(local, redis);
    }
}

// 使用
@Cacheable(value = "metrics", key = "#tenantId + ':' + #category")
public List<Metric> listMetrics(Long tenantId, String category) { ... }

@CacheEvict(value = "metrics", allEntries = true)
public void updateMetric(Metric metric) { ... }
```

---

### 3.6 告警/订阅：@Scheduled 轮询 → XXL-JOB + MQ

**现状问题**
- `SubscriptionService.java`：Spring `@Scheduled` 每分钟轮询数据库
- 告警规则 `checkFluctuation` 直接 `return false`（空实现）
- 同步推送，失败无重试，无消息队列

**改造方案**
```
XXL-JOB（分布式调度）+ RocketMQ/Kafka（异步推送）
```

**架构改造**
```
告警规则配置 → XXL-JOB 动态任务
                   ↓
            分片扫描（按租户/规则ID分片）
                   ↓
            触发条件 → MQ Topic: alert-triggered
                   ↓
            消费端 → 推送服务（邮件/钉钉/企微）
                   ↓
            失败 → 死信队列 → 人工介入
```

| 维度 | @Scheduled 轮询 | XXL-JOB + MQ |
|------|----------------|-------------|
| 调度精度 | 分钟级 | 秒级/CRON 表达式 |
| 分片执行 | ❌ 单机单实例 | ✅ 多实例分片 |
| 失败重试 | ❌ 无 | ✅ 配置重试策略 |
| 幂等性 | ❌ 可能重复推送 | ✅ MQ 消费幂等 |
| 监控 | ❌ 无 | ✅ XXL-JOB 控制台 |

---

## 四、开源生态推荐组合

### 推荐技术栈（改造后）

```
┌─────────────────────────────────────────────────────────────┐
│                        前端层                                │
├─────────────────────────────────────────────────────────────┤
│  Vue 3 + Vite + TypeScript                                  │
│  Element Plus（UI）                                          │
│  Pinia（客户端状态） + TanStack Query Vue（服务端状态）        │
│  ofetch（HTTP） + openapi-typescript（类型生成）              │
│  ECharts（图表，后端配置驱动） + GridStack.js（Dashboard）    │
│  SSE（流式对话）                                              │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                        后端层                                │
├─────────────────────────────────────────────────────────────┤
│  Spring Boot 3 + Java 17                                    │
│  MyBatis Plus（ORM）                                        │
│  LangChain4j（LLM/RAG/Agent）                               │
│  Apache Calcite（SQL解析/优化/联邦查询）                     │
│  baomidou dynamic-datasource（多数据源）                     │
│  ShardingSphere（数据脱敏 + SQL审计）                        │
│  Spring Cache + Caffeine + Redis（缓存）                    │
│  XXL-JOB + RocketMQ（调度与推送）                            │
│  SpringDoc OpenAPI（文档生成）                               │
└─────────────────────────────────────────────────────────────┘
```

### 引入的开源项目清单

| 开源项目 | 替换的自研模块 | 协议 | 成熟度 |
|---------|--------------|------|--------|
| **LangChain4j** | LLM适配 + NL2SQL Prompt | Apache 2.0 | ⭐⭐⭐⭐⭐ |
| **baomidou dynamic-datasource** | 动态数据源 | Apache 2.0 | ⭐⭐⭐⭐⭐ |
| **Apache Calcite** | SQL解析与优化 | Apache 2.0 | ⭐⭐⭐⭐⭐ |
| **ShardingSphere** | 数据脱敏 + SQL审计 | Apache 2.0 | ⭐⭐⭐⭐⭐ |
| **TanStack Query Vue** | 服务端状态管理 | MIT | ⭐⭐⭐⭐⭐ |
| **Pinia** | 客户端状态管理 | MIT | ⭐⭐⭐⭐⭐ |
| **ofetch** | HTTP封装 | MIT | ⭐⭐⭐⭐ |
| **GridStack.js** | Dashboard拖拽 | MIT | ⭐⭐⭐⭐⭐ |
| **XXL-JOB** | 定时调度 | GPL v3 | ⭐⭐⭐⭐⭐ |
| **RocketMQ** | 消息推送 | Apache 2.0 | ⭐⭐⭐⭐⭐ |

---

## 五、实施路线图

### 阶段 1：止血 + 基础建设（2-3 周）

**目标**：解决最痛的扩展性问题，建立工程规范

| 模块 | 动作 | 预期收益 |
|------|------|---------|
| HTTP层 | 引入 ofetch + openapi-typescript，拆分 apiAdapter | 类型安全，减少 `any`，接口变更自动同步 |
| 状态管理 | 引入 Pinia，提取 `queryStore` + `userStore` | 跨页面状态不再靠 URL，代码可测试 |
| 图表系统 | 合并两套配置逻辑 → 统一 `ChartRenderer.vue` | 新增图表改一处，消除配置不一致 |
| Dashboard Editor | 引入 GridStack.js，重写 editor.vue | 响应式布局、网格吸附、无重叠 |
| 缓存 | 启用 Spring Cache + Caffeine（pom 已有） | 消除无限增长的 ConcurrentHashMap |

### 阶段 2：AI 能力升级（3-4 周）

**目标**：NL2SQL 从"能用"到"好用"

| 模块 | 动作 | 预期收益 |
|------|------|---------|
| LLM 适配 | 引入 LangChain4j，替换 6 个手写 provider | 新增模型改配置即可，支持流式输出 |
| Prompt 工程 | 模板化 + RAG（向量检索表结构/历史查询） | 准确率提升，减少幻觉 |
| 对话流式 | 后端 `StreamingChatLanguageModel` + 前端 SSE | 用户体验从"等待"到"打字机效果" |
| NL2SQL 兜底 | 重构 TextToSqlConverter：解除 created_at 硬编码 | 支持任意时间字段 |

### 阶段 3：数据治理增强（2-3 周）

**目标**：企业级安全 + 扩展性

| 模块 | 动作 | 预期收益 |
|------|------|---------|
| 动态数据源 | 迁移到 dynamic-datasource | 运行时热添加数据源、健康检查 |
| 数据脱敏 | 引入 ShardingSphere EncryptRule | 真正的加密、零侵入、合规 |
| 行级权限 | MyBatis-Plus DataPermissionInterceptor | 支持树形权限、OR组合 |
| SQL 治理 | JSQLParser 去限制 → 支持子查询/CTE/UNION | 用户查询能力上限解除 |

### 阶段 4：平台化能力（2-3 周）

**目标**：从工具到平台

| 模块 | 动作 | 预期收益 |
|------|------|---------|
| 告警订阅 | XXL-JOB + RocketMQ 替换 @Scheduled | 分布式调度、失败重试、监控 |
| SQL 联邦 | 引入 Apache Calcite | 跨库 JOIN、查询优化 |
| 诊断组件 | 提取 `<DiagnosisPanel>` 独立组件 | result 和 conversation 复用 |
| 前端 CRUD | 后端分页 + 通用 CRUD hook | 支持万级数据 |

---

## 六、成本与风险

### 改造投入估算

| 阶段 | 人天 | 主要工作 |
|------|------|---------|
| 阶段 1 | 15-20 | 前端基建重构（Pinia/ofetch/GridStack） |
| 阶段 2 | 20-25 | LangChain4j 集成 + RAG + SSE |
| 阶段 3 | 15-20 | 数据源/脱敏/权限迁移 |
| 阶段 4 | 10-15 | 调度/联邦查询/平台化 |
| **总计** | **60-80 人天** | 约 3-4 个月（2 人并行） |

### 风险与缓解

| 风险 | 缓解措施 |
|------|---------|
| LangChain4j 与现有代码耦合深 | 保留 `AiQueryService` 接口，内部实现替换，不破坏 Controller |
| ShardingSphere 引入后性能下降 | 先脱敏场景试点，压测后再全量；或只用 EncryptRule 不用分片 |
| GridStack.js 迁移后布局数据不兼容 | 写迁移脚本：px 坐标 → grid 坐标（12列映射） |
| SSE 改造后旧浏览器不支持 | 降级方案：fetch ReadableStream polyfill，IE  fallback 普通 POST |

---

## 七、一句话总结

> **这个项目的问题不是"代码写得不好"，而是"在已经有成熟开源方案的地方选择了自研"，导致扩展天花板很低。**
>
> 最优先改三件事：
> 1. **LangChain4j 替换自研 LLM 适配**（新增一个模型要改 6 处 → 改 1 处配置）
> 2. **GridStack.js 替换自研 Dashboard 拖拽**（从不能用的布局系统到企业级）
> 3. **ShardingSphere 替换字符串反转脱敏**（从"假加密"到真合规）
