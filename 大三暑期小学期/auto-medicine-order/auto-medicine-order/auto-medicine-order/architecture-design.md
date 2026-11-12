# 社区药品配送服务 — 架构设计文档

## 1. 项目概述

社区药品配送服务是一个面向老年用户的智能药品下单平台，支持 AI 语音/文字对话式问诊下单、商家抢单配送、家庭成员代付等功能。系统采用 Spring Boot + Vue 3 前后端分离架构，通过 DeepSeek 大模型实现自然语言药品匹配。

## 2. 技术栈

| 层次 | 技术选型 |
|------|----------|
| 后端框架 | Spring Boot 3.x、Spring Security、Spring Data JPA |
| 数据库 | MySQL 8.x（JPA ddl-auto: update） |
| 缓存/会话 | Redis（Spring Data Redis） |
| AI 对话 | DeepSeek API（deepseek-v4-flash）via Spring AI |
| 实时通信 | WebSocket（Spring WebSocket） |
| 认证 | JWT（自研 TokenProvider + Filter） |
| 前端框架 | Vue 3（Composition API）、Vite 5 |
| UI 组件库 | Element Plus |
| 状态管理 | Pinia |
| HTTP 客户端 | Axios |
| 语音交互 | Web Speech API（SpeechRecognition + SpeechSynthesis） |

## 3. 系统架构

```
┌─────────────────────────────────────────────────────────┐
│                     Browser (Chrome)                      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │   Vue 3 SPA  │  │ Web Speech   │  │  WebSocket   │  │
│  │  Element Plus│  │  API (STT/   │  │  Client      │  │
│  │  Pinia/Axios │  │  TTS)        │  │              │  │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘  │
└─────────┼─────────────────┼─────────────────┼──────────┘
          │ HTTP/REST        │ HTTP/REST       │ WS
          ▼                 ▼                 ▼
┌─────────────────────────────────────────────────────────┐
│                   Spring Boot Application                │
│  ┌────────────────────────────────────────────────────┐ │
│  │  Spring Security (JWT Filter + Role-based Access)  │ │
│  └────────────────────┬───────────────────────────────┘ │
│                       │                                   │
│  ┌────────┐ ┌────────┐ ┌─────────┐ ┌───────────────┐  │
│  │Auth    │ │Order   │ │Merchant │ │ AI Chat       │  │
│  │Control │ │Control │ │Control  │ │ Controller    │  │
│  │ler     │ │ler     │ │ler      │ │               │  │
│  └───┬────┘ └───┬────┘ └────┬────┘ └──────┬────────┘  │
│      │          │           │              │            │
│  ┌───┴──────────┴───────────┴──────────────┴──────────┐ │
│  │                   Service Layer                     │ │
│  │  AuthService │ OrderService │ MerchantService       │ │
│  │  UserService │ MedicineChatService │ NotificationS  │ │
│  └──────────────────────┬─────────────────────────────┘ │
│                         │                                 │
│  ┌──────────┐  ┌───────┴──────┐  ┌──────────────────┐  │
│  │JPA Repos │  │ DeepSeek API │  │ WebSocketHandler │  │
│  │itories   │  │ (Spring AI)  │  │                  │  │
│  └────┬─────┘  └──────────────┘  └──────────────────┘  │
│       │                                                   │
└───────┼──────────────────────────────────────────────────┘
        │
   ┌────┴────┐     ┌───────┐
   │ MySQL   │     │ Redis │
   └─────────┘     └───────┘
```

## 4. 数据库设计

### 4.1 实体关系

```
User (1) ──── (1) Merchant        @MapsId 共享主键
Merchant (1) ── (*) Medicine      商家拥有多个药品
User (1) ───── (*) Order          用户创建多个订单
Merchant (1) ── (*) Order         商家接单（可为空，抢单模式）
User (1) ───── (*) Order (payer)  代付人
Order (1) ──── (*) OrderItem      订单包含多个药品项
Order (1) ──── (*) OrderBidRecord 接单/放弃记录
User (1) ───── (*) FamilyRelation 家庭关系（双向）
User (1) ───── (*) Notification   通知消息
```

### 4.2 核心表结构

**users 表**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键（自增） |
| family_id | VARCHAR(10) UNIQUE | 家庭标识（@PrePersist 自动生成 10 位随机数） |
| username | VARCHAR(50) UNIQUE | 用户名 |
| password | VARCHAR(255) | BCrypt 加密密码 |
| real_name | VARCHAR(50) | 真实姓名 |
| phone | VARCHAR(20) UNIQUE | 手机号 |
| role | ENUM | USER / MERCHANT |
| address | VARCHAR(255) | 配送地址（语音下单自动使用） |
| latitude | DECIMAL(10,6) | 纬度 |
| longitude | DECIMAL(10,6) | 经度 |
| status | INT | 账号状态，默认 1 |

**merchants 表**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 与 users.id 共享（@MapsId） |
| shop_name | VARCHAR(100) | 店铺名称 |
| license_no | VARCHAR(100) | 营业执照号 |
| address | VARCHAR(255) | 店铺地址 |
| latitude | DECIMAL(10,6) | 纬度 |
| longitude | DECIMAL(10,6) | 经度 |
| phone | VARCHAR(20) | 联系电话 |
| status | ENUM | PENDING / APPROVED / REJECTED |
| delivery_radius | INT | 配送半径（公里），默认 3 |

**medicines 表**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| merchant_id | BIGINT FK | 所属商家 |
| name | VARCHAR(100) | 药品名称 |
| generic_name | VARCHAR(100) | 通用名 |
| category | VARCHAR(50) | 分类（感冒药、降压药等） |
| price | DECIMAL(10,2) | 单价 |
| stock | INT | 库存 |
| description | TEXT | 描述 |
| requires_prescription | BOOLEAN | 是否需处方 |
| status | INT | 1=上架 0=下架 |

**orders 表**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| order_no | VARCHAR UNIQUE | 订单编号（时间戳+6位随机数） |
| user_id | BIGINT FK | 下单用户 |
| merchant_id | BIGINT FK | 接单商家（可为空，抢单模式） |
| payer_id | BIGINT FK | 代付人（可为空） |
| total_amount | DECIMAL(10,2) | 订单总金额 |
| status | ENUM | 订单状态（见状态机） |
| delivery_address | VARCHAR(255) | 配送地址 |
| remark | VARCHAR(500) | 备注 |
| paid_at | DATETIME | 支付时间 |
| accepted_at | DATETIME | 接单时间 |
| delivered_at | DATETIME | 送达时间 |

**order_items 表**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| order_id | BIGINT FK | 所属订单 |
| medicine_id | BIGINT FK | 关联药品 |
| medicine_name | VARCHAR | 药品名称快照 |
| quantity | INT | 数量 |
| unit_price | DECIMAL(10,2) | 下单时单价快照 |
| subtotal | DECIMAL(10,2) | 小计 |

**order_bid_records 表**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| order_id | BIGINT FK | 关联订单 |
| merchant_id | BIGINT FK | 操作商家 |
| action | ENUM | ACCEPTED / ABANDONED |

**family_relations 表**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| requester_id | BIGINT FK | 发起方用户 |
| target_id | BIGINT FK | 目标方用户 |
| status | ENUM | PENDING / ACCEPTED / REJECTED |

**notifications 表**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| user_id | BIGINT FK | 接收用户 |
| title | VARCHAR | 通知标题 |
| content | VARCHAR | 通知内容 |
| type | ENUM | ORDER / PAYMENT / FAMILY / SYSTEM |
| is_read | BOOLEAN | 是否已读 |
| related_order_id | BIGINT | 关联订单（可空） |

### 4.3 订单状态机

```
PENDING ──→ WAITING_PAYMENT ──→ PAID ──→ ACCEPTED ──→ DELIVERING ──→ DELIVERED ──→ COMPLETED
    │                            ↑
    └────────────────────────────┘
          （直接支付，无代付）

ACCEPTED ──→ PAID（商家放弃后回退）
```

| 状态 | 含义 | 可见性 |
|------|------|--------|
| PENDING | 待支付（刚创建） | 仅用户可见 |
| WAITING_PAYMENT | 等待亲属代付 | 仅用户可见 |
| PAID | 已支付，等待商家接单 | 商家可见（可抢单） |
| ACCEPTED | 商家已接单 | 商家+用户可见 |
| DELIVERING | 配送中 | 商家+用户可见 |
| DELIVERED | 已送达 | 商家+用户可见 |
| COMPLETED | 已完成 | 商家+用户可见 |

## 5. 后端架构

### 5.1 包结构

```
com.automedicine
├── ai/                          # AI 对话模块
│   ├── dto/                     # ChatResponse, MedicineIntent
│   ├── prompt/                  # SystemPromptTemplate（从外部文件加载）
│   └── MedicineChatService      # DeepSeek 对话服务
├── config/                      # 配置
│   ├── SecurityConfig           # Spring Security 配置
│   ├── WebSocketConfig          # WebSocket 配置
│   ├── RedisConfig              # Redis 配置
│   ├── WebConfig                # CORS 配置
│   ├── ChatClientConfig         # Spring AI ChatClient 配置
│   └── DataInitializer          # 启动时补建缺失的 Merchant 记录
├── controller/                  # REST API 控制器
│   ├── AuthController           # POST /api/auth/login, /register, /refresh
│   ├── UserController           # GET/PUT /api/users/profile
│   ├── OrderController          # CRUD /api/orders
│   ├── MerchantController       # GET/PUT /api/merchants/profile
│   ├── MedicineController       # CRUD /api/medicines
│   ├── ChatController           # POST /api/chat, GET/DELETE /api/chat/history
│   ├── FamilyController         # 家庭关系管理
│   └── NotificationController   # 通知管理
├── dto/                         # 数据传输对象
│   ├── request/                 # LoginRequest, RegisterRequest, OrderRequest...
│   └── response/                # ApiResponse, UserResponse, OrderResponse...
├── entity/                      # JPA 实体
├── enums/                       # UserRole, OrderStatus, NotificationType
├── exception/                   # BusinessException, GlobalExceptionHandler
├── repository/                  # Spring Data JPA Repository
├── security/                    # JWT 认证组件
│   ├── JwtTokenProvider
│   ├── JwtAuthenticationFilter
│   └── UserDetailsServiceImpl
├── service/                     # 业务逻辑层
└── websocket/                   # WebSocket 处理器
    └── OrderWebSocketHandler
```

### 5.2 REST API 端点

#### 认证

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/auth/login | 登录，返回 JWT token |
| POST | /api/auth/register | 注册（用户/商家），商家自动创建 Merchant 记录 |
| POST | /api/auth/refresh | 刷新 token |

#### 用户

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/users/profile | 获取当前用户信息 |
| PUT | /api/users/profile | 更新个人信息（姓名、手机、地址等） |
| GET | /api/users/search?familyId= | 根据家庭 ID 搜索用户 |

#### 订单

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/orders | 创建订单（自动 WebSocket 广播给商家） |
| GET | /api/orders | 用户查自己的订单 / 商家查分配+可抢单订单 |
| GET | /api/orders/proxy-payable | 查询亲属待代付订单 |
| GET | /api/orders/{id} | 订单详情 |
| PUT | /api/orders/{id}/status | 更新订单状态（状态机校验） |
| POST | /api/orders/{id}/pay | 支付（支持亲友代付） |
| POST | /api/orders/{id}/accept | 商家接单（抢单） |
| POST | /api/orders/{id}/abandon | 商家放弃订单 |

**商家订单查询规则**：默认返回 PAID 及之后状态的订单。已分配订单查所有状态，可抢单池（merchant_id=NULL）仅查 PAID 状态。PENDING 和 WAITING_PAYMENT 订单对商家不可见。

#### 商家

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/merchants/profile | 获取商家资料 |
| PUT | /api/merchants/profile | 更新商家资料（首次自动创建 Merchant 记录） |
| GET | /api/merchants/nearby | 按距离查询附近商家 |

#### 药品

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/medicines?merchantId= | 查询商家药品列表 |
| POST | /api/medicines | 添加药品 |
| PUT | /api/medicines/{id} | 更新药品 |
| DELETE | /api/medicines/{id} | 删除药品 |

#### AI 对话

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/chat | 发送消息，AI 返回结构化 JSON（药品匹配 + 回复） |
| GET | /api/chat/history | 获取对话历史（Redis 存储） |
| DELETE | /api/chat/history | 清空对话历史 |

#### 家庭关系

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/family/request | 发送家庭绑定请求（通过 familyId） |
| GET | /api/family/pending | 查询待处理请求 |
| PUT | /api/family/{id}/accept | 接受请求 |
| PUT | /api/family/{id}/reject | 拒绝请求 |
| GET | /api/family/members | 查询家庭成员 |

#### 通知

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/notifications | 查询通知列表 |
| GET | /api/notifications/unread-count | 获取未读数 |
| PUT | /api/notifications/{id}/read | 标记已读 |
| PUT | /api/notifications/read-all | 全部标记已读 |

### 5.3 安全机制

- **JWT 认证**：登录返回 token（有效期 24h），每次请求通过 `JwtAuthenticationFilter` 验证
- **角色权限**：`ROLE_USER` 和 `ROLE_MERCHANT`，路由级别通过 `meta.role` 控制
- **接口权限**：`/api/auth/**` 公开访问，其余需认证；商家接口在 Service 层校验角色
- **密码存储**：BCrypt 加密

### 5.4 AI 对话服务

**系统提示词管理**：提示词模板存储在 `src/main/resources/ai/system-prompt.txt`，由 `SystemPromptTemplate` 通过 `ClassPathResource` 加载，`%s` 占位符在运行时注入药品目录。

**对话流程**：
1. 用户发送消息 → `ChatController` → `MedicineChatService.chat()`
2. 构建消息列表：SystemMessage（提示词 + 药品目录）+ 历史对话（Redis，最近 5 轮）+ 当前消息
3. 调用 DeepSeek API，要求返回结构化 JSON
4. 解析 JSON：`medicines` 数组（药品名、数量、置信度）+ `reply`（自然语言回复）+ `needsClarification`
5. 根据 `medicines` 中的药品名在数据库中精确匹配，构建 `matchedMedicines` 列表
6. 对话历史存储在 Redis（key: `chat:{username}`），最多保留 20 条

**AI 行为约束**（system-prompt.txt）：
- 仅帮助用户完成药品下单，不提供任何医学建议
- 禁止给出诊断、治疗建议、用药指导
- 用户询问医学问题时固定回复"建议咨询专业医生或药师"
- 紧急健康状况提醒拨打 120

### 5.5 WebSocket 通信

端点：`/ws/orders`

| 消息类型 | 方向 | 说明 |
|----------|------|------|
| new_order | Server → Merchant | 新订单广播（创建订单 + 支付后重新广播） |
| order_status | Server → User | 订单状态变更通知 |

### 5.6 商家注册与数据补全

- **注册时**：`AuthService.register()` 检测到 MERCHANT 角色自动创建 `merchants` 表记录（默认 APPROVED 状态）
- **启动时**：`DataInitializer`（CommandLineRunner）扫描所有 MERCHANT 用户，为缺少 Merchant 记录的已有商家补建记录

## 6. 前端架构

### 6.1 目录结构

```
frontend/src/
├── api/                     # API 模块
│   ├── index.js             # Axios 实例、拦截器（统一解包 ApiResponse）
│   └── auth.js              # authApi, userApi, merchantApi, medicineApi,
│                            # orderApi, chatApi, familyApi, notificationApi
├── layouts/                 # 页面布局
│   ├── UserLayout.vue       # 用户端布局（紫色侧栏 + 顶部栏）
│   └── MerchantLayout.vue   # 商家端布局（绿色侧栏 + 顶部栏）
├── router/
│   └── index.js             # 路由配置 + 导航守卫
├── stores/
│   └── auth.js              # Pinia 认证状态管理
├── views/
│   ├── RoleSelect.vue       # 角色选择页（/）
│   ├── Login.vue            # 登录页（/login/:role）
│   ├── Register.vue         # 注册页（/register/:role）
│   ├── user/                # 用户端页面
│   │   ├── VoiceChat.vue    # 语音聊天（STT + TTS + 语音下单）
│   │   ├── Chat.vue         # 文字聊天
│   │   ├── OrderList.vue    # 我的订单
│   │   ├── OrderDetail.vue  # 订单详情
│   │   ├── ProxyPayment.vue # 代付订单
│   │   ├── FamilyAdd.vue    # 添加家人
│   │   ├── FamilyMembers.vue# 家庭成员
│   │   ├── Notifications.vue# 消息通知
│   │   └── Profile.vue      # 个人信息
│   └── merchant/            # 商家端页面
│       ├── Dashboard.vue    # 商家仪表盘
│       ├── OrderManage.vue  # 订单管理（接单/放弃）
│       ├── MedicineManage.vue# 药品管理
│       ├── ShopSettings.vue # 店铺设置
│       └── Notifications.vue# 消息通知
├── App.vue                  # 根组件
└── main.js                  # 入口文件
```

### 6.2 路由设计

```
/                            → RoleSelect（角色选择）
/login/:role                 → Login（登录）
/register/:role              → Register（注册）

/user                        → 重定向到 /user/voice-chat
/user/voice-chat             → 语音聊天（默认首页）
/user/chat                   → 文字聊天
/user/orders                 → 我的订单
/user/orders/:id             → 订单详情
/user/proxy-payment          → 代付订单
/user/family/add             → 添加家人
/user/family/members         → 家庭成员
/user/notifications          → 消息通知
/user/profile                → 个人信息

/merchant                    → 重定向到 /merchant/dashboard
/merchant/dashboard          → 商家仪表盘
/merchant/orders             → 订单管理
/merchant/medicines          → 药品管理
/merchant/shop               → 店铺设置
/merchant/notifications      → 消息通知
```

**导航守卫逻辑**：
- 访问需要认证的页面（`meta.requiresAuth`）时，未登录跳转到 `/`，角色不匹配跳转到 `/`
- 已登录用户访问登录/注册页时自动跳转到对应角色的首页
- 角色选择页（`/`）允许已登录用户访问，点击身份时清除旧登录状态后进入登录页

### 6.3 语音聊天（VoiceChat.vue）

语音聊天是用户端的默认首页，实现了完整的语音优先下单流程：

**STT（语音转文字）**：使用 Web Speech API 的 `SpeechRecognition`
- 点击麦克风按钮开始录音，再次点击结束
- 录音前通过 `getUserMedia` 预检麦克风权限
- `continuous: true` 支持较长录音，`interimResults: true` 实时显示识别文字
- 累积 `isFinal` 结果 + `latestInterim` 兜底，8 秒超时安全重置

**TTS（文字转语音）**：使用 Web Speech API 的 `SpeechSynthesis`
- AI 回复自动语音播报（可关闭）
- 每条 AI 消息旁有重播按钮
- 语速 0.85，优先选择中文语音

**语音下单流程**：
1. 用户语音描述需求 → STT 识别文字 → 发送给 AI
2. AI 从药品目录匹配药品 → 自动构建 `pendingOrder`（药品 + 地址 + 总价）
3. TTS 语音播报订单详情："为您找到以下药品：XX，合计XX元。配送地址是XX。确认下单请说'好的'。"
4. 用户语音说"好的/确认/可以"等确认词 → `autoCreateOrder()` 自动创建订单
5. 用户说"不要/取消"等取消词 → 取消本次待确认订单

**地址自动填充**：从 `auth.user.address`（用户在"个人信息"页设置的配送地址）自动获取，无需每次手动输入。

### 6.4 状态管理（Pinia）

`auth` store 管理认证状态：
- `token`：JWT token（localStorage 持久化）
- `user`：用户信息对象（localStorage 持久化）
- `login()`：登录 → 存 token → 拉取用户信息
- `register()`：注册 → 自动登录
- `fetchProfile()`：拉取最新用户信息并更新 localStorage
- `logout()`：清除状态 → 跳转到 `/`

### 6.5 Axios 拦截器

- 请求拦截器：自动附加 `Authorization: Bearer {token}` 请求头
- 响应拦截器：统一解包 `ApiResponse` 信封，`code === 200` 时直接返回 `body`，否则弹出错误提示

## 7. 部署与构建

### 7.1 前端构建

```bash
cd frontend
npm run build
```

Vite 构建输出到 Spring Boot 的 `src/main/resources/static/` 目录。构建前需清空 `static/assets/` 旧文件避免残留。构建后用户须 Ctrl+Shift+R 强制刷新浏览器，否则旧 `index.html` 缓存可能导致路由/导航异常。

### 7.2 后端启动

```bash
mvn spring-boot:run
```

或通过 IDE 直接运行 `AutoMedicineApplication` 主类。服务端口 8080，前端静态资源由 Spring Boot 直接托管。

### 7.3 环境依赖

- JDK 17+
- MySQL 8.x（数据库 `auto_medicine_order`，密码通过 `MYSQL_PASSWORD` 环境变量注入）
- Redis（localhost:6379）
- DeepSeek API Key（通过 `DEEPSEEKCHAT_API` 环境变量注入）
- Node.js 18+（前端构建）
