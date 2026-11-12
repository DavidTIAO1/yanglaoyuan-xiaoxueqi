## 社区药品配送服务 — 架构设计文档

### 一、项目概述

本项目是一个面向社区老人及周边药店的药品配送服务平台。用户（老人或其家属）通过对话式界面描述购药需求，后端借助大模型进行意图识别与药品提取，确认后生成订单并推送给周边商家，商家接单后完成配送。系统支持亲友绑定机制，老人确认后订单可推送给家属完成支付。

### 二、技术选型

| 层级 | 技术 | 说明 |
|------|------|------|
| 前端 | Vue 3 + Vite + Element Plus | SPA 前后端分离 |
| 后端 | Spring Boot 3.x | 核心框架 |
| AI | Spring AI + DeepSeek | 意图识别、药品提取 |
| 持久层 | Spring Data JPA + MySQL 8 | 数据持久化 |
| 缓存 | Spring Data Redis | 会话管理、验证码、订单缓存 |
| 实时通信 | Spring WebSocket | 订单推送、状态通知 |
| 安全 | Spring Security + JWT | 认证与授权 |
| 构建 | Maven | 项目构建管理 |

### 三、分层架构

项目采用经典分层架构，自上而下职责清晰：

```
┌─────────────────────────────────────────────────┐
│              表现层 (Controller)                  │
│   REST API  ·  WebSocket Handler                 │
├─────────────────────────────────────────────────┤
│              业务层 (Service)                     │
│   用户服务 · 商家服务 · 订单服务 · 家庭服务       │
├─────────────────────────────────────────────────┤
│           AI 服务层 (AI Service)                  │
│   意图识别 · 药品提取 · 对话管理                  │
├─────────────────────────────────────────────────┤
│            数据访问层 (Repository)                │
│   Spring Data JPA Repositories                  │
├─────────────────────────────────────────────────┤
│            基础设施层 (Infrastructure)            │
│   MySQL · Redis · DeepSeek API · WebSocket       │
└─────────────────────────────────────────────────┘
```

各层之间的依赖关系遵循单向原则：Controller → Service → Repository，AI 服务层作为独立的横切关注点被 Service 层调用。

### 四、后端包结构

```
auto-medicine-order/
├── pom.xml
├── src/main/java/com/automedicine/
│   │
│   ├── AutoMedicineOrderApplication.java          # 启动类
│   │
│   ├── config/                                    # ── 配置层 ──
│   │   ├── SecurityConfig.java                    #   Spring Security 配置
│   │   ├── JwtConfig.java                         #   JWT 参数配置
│   │   ├── WebSocketConfig.java                   #   WebSocket 端点与跨域
│   │   ├── RedisConfig.java                       #   Redis 序列化与连接
│   │   └── DeepSeekConfig.java                    #   Spring AI ChatClient 配置
│   │
│   ├── controller/                                # ── 表现层 ──
│   │   ├── AuthController.java                    #   登录注册、角色选择
│   │   ├── UserController.java                    #   用户资料管理
│   │   ├── MerchantController.java                #   商家资料与营业管理
│   │   ├── MedicineController.java                #   药品目录查询
│   │   ├── OrderController.java                   #   订单 CRUD
│   │   ├── ChatController.java                    #   对话接口（调用 AI）
│   │   ├── FamilyController.java                  #   亲友绑定与管理
│   │   └── NotificationController.java            #   通知查询
│   │
│   ├── websocket/                                 # ── 实时通信层 ──
│   │   ├── OrderWebSocketHandler.java             #   订单推送处理器
│   │   └── NotificationWebSocketHandler.java      #   通知推送处理器
│   │
│   ├── service/                                   # ── 业务层 ──
│   │   ├── AuthService.java                       #   认证与令牌服务
│   │   ├── UserService.java                       #   用户业务逻辑
│   │   ├── MerchantService.java                   #   商家业务逻辑
│   │   ├── MedicineService.java                   #   药品查询逻辑
│   │   ├── OrderService.java                      #   订单生命周期管理
│   │   ├── ChatService.java                       #   对话编排（调 AI）
│   │   ├── FamilyService.java                     #   亲友关系管理
│   │   └── NotificationService.java               #   通知分发
│   │
│   ├── ai/                                        # ── AI 服务层 ──
│   │   ├── MedicineChatService.java               #   DeepSeek 对话调用
│   │   ├── IntentRecognitionService.java          #   意图解析与分类
│   │   ├── dto/
│   │   │   ├── ChatRequest.java                   #   对话请求 DTO
│   │   │   ├── ChatResponse.java                  #   对话响应 DTO
│   │   │   └── MedicineIntent.java                #   识别出的药品意图
│   │   └── prompt/
│   │       └── SystemPromptTemplate.java          #   系统提示词模板
│   │
│   ├── entity/                                    # ── 实体层 ──
│   │   ├── BaseEntity.java                        #   公共字段（id, createTime, updateTime）
│   │   ├── User.java                              #   用户实体
│   │   ├── Merchant.java                          #   商家实体
│   │   ├── Medicine.java                          #   药品实体
│   │   ├── Order.java                             #   订单实体
│   │   ├── OrderItem.java                         #   订单明细
│   │   ├── FamilyRelation.java                    #   亲友关系
│   │   ├── Notification.java                      #   通知实体
│   │   └── OrderBidRecord.java                    #   抢单记录
│   │
│   ├── repository/                                # ── 数据访问层 ──
│   │   ├── UserRepository.java
│   │   ├── MerchantRepository.java
│   │   ├── MedicineRepository.java
│   │   ├── OrderRepository.java
│   │   ├── OrderItemRepository.java
│   │   ├── FamilyRelationRepository.java
│   │   ├── NotificationRepository.java
│   │   └── OrderBidRecordRepository.java
│   │
│   ├── dto/                                       # ── 数据传输对象 ──
│   │   ├── request/                               #   请求 DTO
│   │   │   ├── LoginRequest.java
│   │   │   ├── RegisterRequest.java
│   │   │   ├── OrderRequest.java
│   │   │   └── ChatMessageRequest.java
│   │   └── response/                              #   响应 DTO
│   │       ├── ApiResponse.java                   #   统一响应包装
│   │       ├── OrderResponse.java
│   │       ├── MedicineResponse.java
│   │       └── UserResponse.java
│   │
│   ├── security/                                  # ── 安全层 ──
│   │   ├── JwtTokenProvider.java                  #   JWT 生成与校验
│   │   ├── JwtAuthenticationFilter.java           #   请求拦截过滤器
│   │   └── UserDetailsServiceImpl.java            #   用户详情加载
│   │
│   ├── enums/                                     # ── 枚举定义 ──
│   │   ├── UserRole.java                          #   USER, MERCHANT
│   │   ├── OrderStatus.java                       #   订单状态机
│   │   └── NotificationType.java                  #   通知类型
│   │
│   └── exception/                                 # ── 异常处理 ──
│       ├── BusinessException.java                 #   业务异常
│       └── GlobalExceptionHandler.java            #   全局异常处理器
│
├── src/main/resources/
│   ├── application.yml                            # 主配置
│   ├── application-dev.yml                        # 开发环境配置
│   └── db/
│       └── schema.sql                             # 建表脚本
│
└── src/test/java/com/automedicine/                # 单元测试
```

### 五、数据库设计

数据库名称：`auto_medicine_order`

共 8 张核心表，覆盖用户、商家、药品、订单、亲友关系、通知与抢单记录：

**users — 用户表**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK AUTO_INCREMENT | 主键 |
| family_id | VARCHAR(10) UNIQUE | 亲友标识（注册时自动生成10位随机数字，不可修改） |
| username | VARCHAR(50) UNIQUE | 登录名 |
| password | VARCHAR(255) | BCrypt 加密密码 |
| real_name | VARCHAR(50) | 真实姓名 |
| phone | VARCHAR(20) UNIQUE | 手机号 |
| role | ENUM('USER','MERCHANT') | 角色 |
| address | VARCHAR(255) | 住址 |
| latitude | DECIMAL(10,6) | 纬度（用于就近匹配） |
| longitude | DECIMAL(10,6) | 经度 |
| status | TINYINT DEFAULT 1 | 1 启用 / 0 禁用 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

**merchants — 商家表**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 关联 users.id |
| shop_name | VARCHAR(100) | 药店名称 |
| license_no | VARCHAR(100) | 营业执照号 |
| address | VARCHAR(255) | 店铺地址 |
| latitude | DECIMAL(10,6) | 纬度 |
| longitude | DECIMAL(10,6) | 经度 |
| phone | VARCHAR(20) | 联系电话 |
| status | ENUM('PENDING','APPROVED','REJECTED') | 审核状态 |
| delivery_radius | INT DEFAULT 3 | 配送半径（公里） |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

**medicines — 药品表**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| merchant_id | BIGINT FK | 所属商家 |
| name | VARCHAR(100) | 药品名称 |
| generic_name | VARCHAR(100) | 通用名 |
| category | VARCHAR(50) | 分类（感冒/心血管/消化…） |
| price | DECIMAL(10,2) | 售价 |
| stock | INT | 库存 |
| description | TEXT | 说明 |
| requires_prescription | BOOLEAN | 是否处方药 |
| status | TINYINT DEFAULT 1 | 上架状态 |

**orders — 订单表**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| order_no | VARCHAR(32) UNIQUE | 订单编号 |
| user_id | BIGINT FK | 下单用户 |
| merchant_id | BIGINT FK | 接单商家 |
| payer_id | BIGINT FK | 支付人（亲友代付时非空） |
| total_amount | DECIMAL(10,2) | 总金额 |
| status | ENUM(...) | 见下方状态机 |
| delivery_address | VARCHAR(255) | 配送地址 |
| remark | TEXT | 备注 |
| paid_at | DATETIME | 支付时间 |
| accepted_at | DATETIME | 商家接单时间 |
| delivered_at | DATETIME | 送达时间 |
| created_at | DATETIME | 订单创建时间 |
| updated_at | DATETIME | 订单更新时间 |

**订单状态机流转**：

```
                          ┌──────────────────────────────────────────┐
                          │                                          │
                          ▼                                          │
PENDING(待接单) ──抢单──→ ACCEPTED(已接单) ──开始配送──→ DELIVERING(配送中) ──→ DELIVERED(已送达)
     │                        │
     │                     放弃接单
     │                        │
     │                        └──→ 回退为 PENDING（重新广播给附近商家）
     │
     └──→ WAITING_PAYMENT(待支付) ──亲友支付──→ PAID(已支付) ──→ 进入配送流程
```

**抢单规则**：

- 订单生成后以发布-订阅模式广播给附近所有合作商家，所有商家均可查看
- 任何商家都可以尝试接单，但只有第一个成功执行的能锁定订单（乐观锁/Redis 分布式锁保证并发安全）
- 接单前任何商家不能拒绝订单，只能选择接单或忽略
- 商家接单后若无法完成配送，可以放弃订单，订单状态回退为 PENDING 并重新广播给附近商家
- **已放弃订单的商家不能再次接同一订单**：系统通过 `order_bid_records` 表校验，若该商家对该订单有 `ABANDONED` 记录则拒绝接单
- 亲友代付场景下，用户确认订单后先流转到 WAITING_PAYMENT，由绑定的亲友完成支付后再推进到配送流程

**order_items — 订单明细表**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| order_id | BIGINT FK | 关联订单 |
| medicine_id | BIGINT FK | 关联药品 |
| medicine_name | VARCHAR(100) | 冗余药品名（防止改名丢失） |
| quantity | INT | 数量 |
| unit_price | DECIMAL(10,2) | 单价快照 |
| subtotal | DECIMAL(10,2) | 小计 |

**family_relations — 亲友关系表**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| requester_id | BIGINT FK | 发起绑定请求的用户 ID |
| target_id | BIGINT FK | 被邀请的用户 ID（通过 family_id 搜索得到） |
| relationship | VARCHAR(20) | 关系描述（子女/配偶/其他） |
| status | ENUM('PENDING','ACCEPTED','REJECTED') | 绑定状态 |
| created_at | DATETIME | 发起时间 |
| updated_at | DATETIME | 处理时间 |

绑定流程：用户 A 输入用户 B 的 `family_id`（10位数字）发起绑定请求 → 系统创建一条 PENDING 状态的记录 → 用户 B 收到通知后可选择接受或拒绝 → 状态变为 ACCEPTED 或 REJECTED。

**notifications — 通知表**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| user_id | BIGINT FK | 接收人 |
| title | VARCHAR(100) | 标题 |
| content | TEXT | 内容 |
| type | ENUM('NEW_ORDER','ORDER_STATUS','PAYMENT','FAMILY') | 类型 |
| related_order_id | BIGINT | 关联订单 |
| is_read | BOOLEAN DEFAULT FALSE | 已读标记 |
| created_at | DATETIME | 创建时间 |

**order_bid_records — 订单抢单记录表**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| order_id | BIGINT FK | 关联订单 |
| merchant_id | BIGINT FK | 尝试接单的商家 |
| action | ENUM('ACCEPTED','ABANDONED') | 操作类型（接单/放弃） |
| created_at | DATETIME | 操作时间 |

**约束逻辑**：商家尝试接单时，系统先查询 `order_bid_records` 表，若该商家对该订单已有 `ABANDONED` 记录，则拒绝接单并提示"您已放弃过此订单，无法再次接单"。每次接单或放弃都会写入一条新记录，形成完整的操作审计链。

### 六、核心 API 设计

**认证模块**

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/auth/register | 注册（含角色选择） |
| POST | /api/auth/login | 登录，返回 JWT |
| POST | /api/auth/refresh | 刷新令牌 |

**用户模块**

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/users/profile | 获取个人资料 |
| PUT | /api/users/profile | 更新资料（含坐标） |

**商家模块**

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/merchants/profile | 商家信息 |
| PUT | /api/merchants/profile | 更新商家信息 |
| GET | /api/merchants/nearby | 按坐标查附近商家 |
| GET | /api/merchants/{id}/medicines | 商家药品列表 |

**药品模块（商家专属）**

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/medicines | 商家新增药品 |
| GET | /api/medicines | 商家查询自己的药品列表 |
| PUT | /api/medicines/{id} | 商家修改药品信息（名称、价格、库存等） |
| DELETE | /api/medicines/{id} | 商家删除/下架药品 |

**对话与 AI 模块**

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/chat/send | 发送消息，返回 AI 响应 |
| GET | /api/chat/history | 获取对话历史 |

**订单模块**

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/orders | 创建订单（从 AI 确认结果生成） |
| GET | /api/orders | 查询我的订单列表 |
| GET | /api/orders/{id} | 订单详情 |
| PUT | /api/orders/{id}/status | 更新订单状态 |
| POST | /api/orders/{id}/pay | 支付（亲友代付入口） |

**亲友模块**

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/users/search?familyId=xxx | 通过 family_id 搜索用户 |
| POST | /api/family/bind | 发起亲友绑定（传入对方 family_id 和关系描述） |
| POST | /api/family/{id}/accept | 接受绑定邀请 |
| POST | /api/family/{id}/reject | 拒绝绑定邀请 |
| GET | /api/family/members | 我的亲友列表 |

**WebSocket 端点**

| 路径 | 方向 | 说明 |
|------|------|------|
| /ws/orders | 服务端→商家 | 新订单推送 |
| /ws/orders | 服务端→用户 | 订单状态变更通知 |
| /ws/notifications | 服务端→客户端 | 通用通知推送 |

### 七、前端页面结构

```
src/
├── App.vue
├── router/index.js                    # 路由配置（含角色守卫）
├── store/                             # Pinia 状态管理
│   ├── auth.js                        #   登录态与用户信息
│   ├── chat.js                        #   对话上下文
│   └── order.js                       #   订单状态
├── api/                               # Axios 请求封装
│   ├── auth.js
│   ├── chat.js
│   ├── order.js
│   ├── merchant.js
│   └── family.js
│
├── views/
│   ├── RoleSelect.vue                 # 角色选择页（商家 / 用户）
│   ├── Login.vue                      # 登录页
│   ├── Register.vue                   # 注册页
│   │
│   ├── user/                          # ── 用户端页面 ──
│   │   ├── Chat.vue                   #   对话主页面（核心）
│   │   ├── OrderConfirm.vue           #   药品确认与下单
│   │   ├── OrderList.vue              #   我的订单
│   │   ├── OrderDetail.vue            #   订单详情
│   │   └── FamilyManage.vue           #   亲友管理
│   │
│   └── merchant/                      # ── 商家端页面 ──
│       ├── Dashboard.vue              #   商家首页/工作台
│       ├── OrderManage.vue            #   订单管理（接单/拒单）
│       ├── MedicineManage.vue         #   药品上下架
│       └── ShopSettings.vue           #   店铺设置
│
└── components/
    ├── ChatBubble.vue                 # 对话气泡组件
    ├── MedicineCard.vue               # 药品卡片
    ├── OrderStatusTag.vue             # 订单状态标签
    ├── ConfirmDialog.vue              # 药品确认弹窗
    └── LayoutHeader.vue               # 顶部导航栏（含 family_id 展示）
```

**family_id 前端展示规则**：

- **生成时机**：用户注册成功后，后端自动生成 10 位随机数字并存入 `users.family_id`，注册成功响应中一并返回给前端。
- **展示位置**：顶部导航栏 `LayoutHeader.vue` 中，用户名正下方。
- **视觉样式**：字号比用户名小两档（如用户名 16px，family_id 12px），颜色使用更淡的灰色（如 `#999` 或 `rgba(0,0,0,0.45)`），与用户名形成主次层级。
- **交互**：点击 family_id 触发一键复制（调用 `navigator.clipboard.writeText`），复制成功后以 Element Plus 的 `ElMessage.success` 提示"已复制亲友码"。
- **全局可见**：无论用户端还是商家端，登录后顶部导航栏始终展示该 family_id，方便用户随时分享给亲友进行绑定。

### 八、核心业务流程

**流程一：对话式购药**

```
用户输入 "我最近头疼，想买点布洛芬"
        │
        ▼
  ChatController 接收消息
        │
        ▼
  ChatService 组装上下文（历史对话 + 当前消息）
        │
        ▼
  MedicineChatService 调用 DeepSeek API
  （系统提示词要求返回结构化 JSON）
        │
        ▼
  IntentRecognitionService 解析 AI 返回
  提取药品列表：[{name: "布洛芬", quantity: 1}]
        │
        ▼
  系统内部查询药品库，匹配附近商家有货的药品
  （用户无需关心具体商家，系统自动匹配）
        │
        ▼
  返回给用户确认：
  "为您找到：布洛芬缓释胶囊 × 1（约¥15.80），确认下单吗？"
        │
        ▼
  用户确认 → 生成订单（状态 PENDING）
        │
        ▼
  WebSocket 推送给附近商家
```

**流程二：亲友代付**

```
老人确认订单
     │
     ▼
系统检查是否有已绑定的亲友
     │
     ├── 有 → 订单状态变为 WAITING_PAYMENT
     │         WebSocket 推送给家属
     │         家属在"我的订单"中看到待支付订单
     │         家属点击支付 → 订单继续流转
     │
     └── 无 → 提示绑定亲友，或走普通支付流程
```

### 九、AI 提示词设计思路

系统提示词核心要求：DeepSeek 扮演社区药房助手角色，从用户的自然语言描述中提取药品信息，返回固定 JSON 格式。提示词中内置常见药品名称和分类作为参考，要求模型在无法明确判断时追问用户，避免误识别。

返回格式约定：

```json
{
  "medicines": [
    { "name": "布洛芬缓释胶囊", "quantity": 1, "confidence": 0.95 }
  ],
  "needsClarification": false,
  "reply": "已为您找到布洛芬缓释胶囊，请问还需要其他药品吗？"
}
```

### 十、项目启动检查清单

- [ ] MySQL 8.x 运行中，创建数据库 `auto_medicine_order`
- [ ] Redis 运行中，默认端口 6379
- [ ] DeepSeek API Key 获取并配置到 application-dev.yml
- [ ] Maven 3.8+ 已安装
- [ ] Node.js 18+ 已安装（前端构建）
- [ ] 后端启动：`mvn spring-boot:run`（端口 8080）
- [ ] 前端启动：`npm run dev`（端口 5173，代理到 8080）
