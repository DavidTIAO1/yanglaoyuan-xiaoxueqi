## 社区药品配送服务 — 实施规划文档

本文档基于 architecture-design.md，将后端实现拆分为 15 个阶段任务，每个任务包含具体实现流程、最小可运行单元、测试数据与验收指标。遵循从底层到上层的原则逐步推进。

---

### 阶段一：枚举与实体层

**实现内容**

创建 3 个枚举类、1 个基础抽象类和 8 个 JPA 实体类，对应数据库 8 张表。

`BaseEntity` 作为抽象基类（`@MappedSuperclass`），提供 `id`、`createdAt`、`updatedAt` 公共字段，所有实体继承此类。

枚举：`UserRole`（USER/MERCHANT）、`OrderStatus`（PENDING/WAITING_PAYMENT/PAID/ACCEPTED/DELIVERING/DELIVERED/COMPLETED）、`NotificationType`（NEW_ORDER/ORDER_STATUS/PAYMENT/FAMILY）。

实体：`User`、`Merchant`、`Medicine`、`Order`、`OrderItem`、`FamilyRelation`、`Notification`、`OrderBidRecord`，分别对应 8 张数据库表。其中 `Merchant` 使用 `@MapsId` 与 `User` 共享主键；`User` 通过 `@PrePersist` 自动生成 10 位 `familyId`；`Order` 关联三个外键（user/merchant/payer）；`Medicines` 和 `OrderBidRecord` 不含 `updatedAt` 字段，其余实体均维护 `createdAt`/`updatedAt` 时间戳。

**最小可运行单元**

项目能正常启动，JPA `ddl-auto: update` 自动在 `auto_medicine_order` 数据库中创建全部 8 张表，表结构与 architecture-design.md 中的字段定义一致。

**测试数据**

无需手动插入，启动项目后由 Hibernate 自动建表。建表后可通过 Navicat 查看表结构验证字段、类型、约束是否正确。

**验收指标**

- `mvn compile` 编译通过，无报错
- 启动应用后，Navicat 中可看到 8 张表已自动创建
- `users` 表包含 `family_id`（UNIQUE）、`latitude`/`longitude`、`role`（ENUM）等关键字段
- `merchants` 表主键无自增（与 users 共享）
- `orders` 表包含 `order_no`（UNIQUE）、`status`（ENUM）、三个外键字段

---

### 阶段二：Repository 层

**实现内容**

创建 8 个 Repository 接口，继承 `JpaRepository`，按需定义自定义查询方法。

接口：`UserRepository`、`MerchantRepository`、`MedicineRepository`、`OrderRepository`、`OrderItemRepository`、`FamilyRelationRepository`、`NotificationRepository`、`OrderBidRecordRepository`。

关键查询：`UserRepository` 的 `findByUsername`/`findByPhone`/`findByFamilyId`；`MerchantRepository` 的 `findNearbyMerchants`（原生 SQL，Haversine 公式计算距离）；`OrderRepository` 的 `findByUserId`/`findByMerchantIdAndStatusIn`/`findByOrderNo`；`FamilyRelationRepository` 的 `findAcceptedRelationsByUserId`（JPQL 查询）；`OrderBidRecordRepository` 的 `existsByOrderIdAndMerchantIdAndAction`（用于抢单校验）。

**最小可运行单元**

项目启动无报错，Spring Data JPA 自动实现所有 Repository 接口。

**测试数据**

无需额外数据，启动验证即可。

**验收指标**

- 所有 Repository 接口编译通过
- 项目启动无 Bean 创建失败的报错
- `MerchantRepository.findNearbyMerchants` 原生 SQL 语法正确（可在 Navicat 中单独执行验证）

---

### 阶段三：DTO 层

**实现内容**

创建请求 DTO 和响应 DTO。

请求 DTO（`dto/request/`）：`RegisterRequest`（username/password/realName/phone/role/address）、`LoginRequest`（username/password）、`ChatMessageRequest`（message）、`OrderRequest`（items/deliveryAddress/remark，内含 `OrderItemRequest` 内部类）。

响应 DTO（`dto/response/`）：`ApiResponse<T>`（统一响应包装，含 code/message/data 和静态工厂方法）、`UserResponse`、`OrderResponse`（含 `OrderItemResponse` 内部类）、`MedicineResponse`。

**最小可运行单元**

编译通过，DTO 类可被后续 Service/Controller 正常引用。

**测试数据**

无需测试数据。

**验收指标**

- 所有 DTO 编译通过
- `ApiResponse` 的 `success()`/`error()` 静态工厂方法签名正确
- `OrderRequest` 能正确嵌套 `OrderItemRequest` 列表

---

### 阶段四：异常处理

**实现内容**

创建 `BusinessException`（自定义业务异常，包含 code 和 message）和 `GlobalExceptionHandler`（`@RestControllerAdvice`，统一捕获 `BusinessException`、`MethodArgumentNotValidException`、`Exception` 等，返回 `ApiResponse` 格式）。

**最小可运行单元**

在任意 Controller 中抛出 `BusinessException`，能被全局处理器捕获并返回标准 JSON 格式。

**测试数据**

无需额外数据。

**验收指标**

- 抛出 `BusinessException("用户不存在", 404)` 时，接口返回 `{"code":404,"message":"用户不存在","data":null}`
- 参数校验失败（`@Valid` 触发）时，返回合理的错误提示
- 未知异常返回 500 及通用错误信息，不暴露堆栈

---

### 阶段五：配置层

**实现内容**

创建 5 个配置类。

`JwtConfig`：`@ConfigurationProperties(prefix="app.jwt")`，读取 `secret` 和 `expirationMs`。

`RedisConfig`：配置 `RedisTemplate<String, Object>`，key 使用 String 序列化，value 使用 JSON 序列化。

`WebSocketConfig`：注册 `OrderWebSocketHandler` 到 `/ws/orders`，`NotificationWebSocketHandler` 到 `/ws/notifications`，允许跨域。

`DeepSeekConfig`：基于 Spring AI 的 `OpenAiChatModel` 创建 `ChatClient` Bean。

`SecurityConfig`：CSRF 关闭、CORS 允许 `localhost:5173`、Session 策略设为 STATELESS、`/api/auth/**` 和 `/ws/**` 放行、`JwtAuthenticationFilter` 插入到 `UsernamePasswordAuthenticationFilter` 之前、注册 `BCryptPasswordEncoder` 和 `AuthenticationManager` Bean。

**最小可运行单元**

项目启动无报错，所有 Bean 正常注入。Security 配置生效：访问 `/api/auth/**` 不需要认证，访问其他 `/api/**` 接口返回 401。

**测试数据**

无需额外数据。

**验收指标**

- `mvn compile` 通过
- 启动无 Bean 创建失败
- 访问 `GET /api/users/profile`（未带 Token）返回 401
- 访问 `POST /api/auth/login` 不被 Security 拦截（返回业务层错误即可）

---

### 阶段六：安全层（JWT）

**实现内容**

创建 3 个安全类。

`JwtTokenProvider`：基于 jjwt 0.12.x API，实现 `generateToken`（从 Authentication 生成 JWT）、`getUsernameFromToken`（解析 token 提取用户名）、`validateToken`（校验签名和过期时间）。密钥从 `JwtConfig` 读取。

`JwtAuthenticationFilter`：继承 `OncePerRequestFilter`，从请求头 `Authorization: Bearer xxx` 提取 token，校验后设置 `SecurityContext`。

`UserDetailsServiceImpl`：实现 `UserDetailsService`，从 `UserRepository` 加载用户，将 `UserRole` 转为 `ROLE_USER`/`ROLE_MERCHANT` 权限。

**最小可运行单元**

注册一个用户 → 登录获取 JWT → 携带 JWT 访问受保护接口能正常返回数据。

**测试数据**

通过注册接口创建一个测试用户：`{"username":"testuser","password":"123456","realName":"测试用户","phone":"13800000001","role":"USER","address":"测试地址"}`。

**验收指标**

- 登录后返回的 JWT 能被 `JwtTokenProvider` 正确解析出用户名
- 携带有效 JWT 访问 `/api/users/profile` 返回 200
- 携带过期/伪造 JWT 访问返回 401
- 密码在数据库中以 BCrypt 哈希存储，非明文

---

### 阶段七：认证模块（Auth）

**实现内容**

`AuthService`：注册逻辑（校验用户名/手机号唯一 → BCrypt 加密密码 → 保存用户 → 返回含 `familyId` 的 `UserResponse`）；登录逻辑（AuthenticationManager 认证 → JwtTokenProvider 生成 token → 返回 token 字符串）；刷新令牌逻辑（校验当前 token 有效 → 生成新 token 返回）。

`AuthController`：`POST /api/auth/register`、`POST /api/auth/login`、`POST /api/auth/refresh`，接收请求 DTO，调用 Service，返回 `ApiResponse` 包装结果。

**最小可运行单元**

完整走通注册 → 登录 → 获取 token → 刷新 token 的流程。

**测试数据**

注册请求：
```json
{"username":"zhangsan","password":"abc123","realName":"张三","phone":"13800000001","role":"USER","address":"幸福小区1号楼"}
```
注册请求（商家）：
```json
{"username":"pharmacy01","password":"abc123","realName":"王五","phone":"13800000002","role":"MERCHANT","address":"健康路88号"}
```
刷新令牌：登录后携带有效 JWT，请求头 `Authorization: Bearer <token>`，调用 `POST /api/auth/refresh`。

**验收指标**

- 注册成功返回 `code:200`，`data` 中包含自动生成的 10 位 `familyId`
- 重复用户名注册返回错误提示
- 登录成功返回 JWT token 字符串
- 错误密码登录返回"用户名或密码错误"
- 数据库中 `family_id` 字段为 10 位数字字符串，且唯一
- `POST /api/auth/refresh` 携带有效 token 返回新 JWT，携带过期 token 返回 401

---

### 阶段八：用户模块（User）

**实现内容**

`UserService`：获取个人资料（根据 SecurityContext 中的用户名查询用户，转为 `UserResponse`）；更新资料（更新 realName/phone/address/latitude/longitude）。

`UserController`：`GET /api/users/profile` 和 `PUT /api/users/profile`。

**最小可运行单元**

登录后能查看和更新自己的个人资料。

**测试数据**

使用阶段七注册的测试用户登录后操作。更新请求：
```json
{"realName":"张三丰","address":"幸福小区2号楼","latitude":39.9042,"longitude":116.4074}
```

**验收指标**

- `GET /api/users/profile` 返回当前登录用户的完整信息
- `PUT /api/users/profile` 更新后再次 GET 能看到变更
- 未登录访问返回 401
- 无法通过此接口修改其他用户的信息

---

### 阶段九：商家模块（Merchant）

**实现内容**

`MerchantService`：获取商家信息；更新商家信息（shopName/licenseNo/address/lat/lng/phone/deliveryRadius）；查询附近商家（根据用户坐标，调用 `MerchantRepository.findNearbyMerchants`）。

`MerchantController`：`GET /api/merchants/profile`、`PUT /api/merchants/profile`、`GET /api/merchants/nearby?lat=&lng=`、`GET /api/merchants/{id}/medicines`。

注意：商家注册后 `merchants` 表尚无记录，需在商家首次更新 profile 时创建（或在注册时同步创建，状态为 PENDING）。

**最小可运行单元**

商家角色用户登录后能完善店铺信息，用户角色能查看附近商家列表。

**测试数据**

商家登录后的更新请求：
```json
{"shopName":"康乐药店","licenseNo":"91110000MA12345X","address":"健康路88号","latitude":39.9100,"longitude":116.4100,"phone":"010-12345678","deliveryRadius":5}
```

**验收指标**

- 商家更新 profile 后，`merchants` 表写入对应记录
- `GET /api/merchants/nearby?lat=39.9&lng=116.4` 返回半径范围内的商家列表
- 用户角色无法调用商家专属的写入接口（权限控制）

---

### 阶段十：药品模块（Medicine）

**实现内容**

`MedicineService`：新增药品（校验商家身份 → 保存）；查询商家自己的药品列表；修改药品信息；删除/下架药品（逻辑删除，status 置 0）。

`MedicineController`：`POST /api/medicines`、`GET /api/medicines`、`PUT /api/medicines/{id}`、`DELETE /api/medicines/{id}`。所有接口仅限 MERCHANT 角色。

**最小可运行单元**

商家能完成药品的增删改查操作。

**测试数据**

新增药品请求：
```json
{"name":"布洛芬缓释胶囊","genericName":"布洛芬","category":"解热镇痛","price":15.80,"stock":200,"description":"用于缓解轻至中度疼痛","requiresPrescription":false}
```
```json
{"name":"阿莫西林胶囊","genericName":"阿莫西林","category":"抗生素","price":12.50,"stock":150,"description":"适用于敏感菌所致的感染","requiresPrescription":true}
```
```json
{"name":"复方甘草片","genericName":"复方甘草","category":"止咳","price":8.00,"stock":100,"description":"镇咳祛药","requiresPrescription":false}
```

**验收指标**

- 新增药品后 `medicines` 表写入记录，`merchant_id` 正确关联
- 查询只返回当前商家自己的药品
- 修改后价格/库存等字段正确更新
- 删除后 `status` 变为 0，查询接口不再返回该药品
- USER 角色调用药品接口返回 403

---

### 阶段十一：订单模块（Order）

**实现内容**

`OrderService`：创建订单（生成唯一 `orderNo` → 保存 Order 和 OrderItem → 计算总金额 → 通过 WebSocket 广播给附近商家）；查询订单列表（用户查自己的，商家查分配给自己的）；查询订单详情；更新订单状态（按状态机流转校验）；支付接口（亲友代付入口，更新 payerId 和 paidAt）；商家接单（乐观锁/Redis 分布式锁抢单 → 写入 `OrderBidRecord`）；商家放弃接单（状态回退 PENDING → 写入 ABANDONED 记录 → 重新广播）。

`OrderController`：`POST /api/orders`、`GET /api/orders`、`GET /api/orders/{id}`、`PUT /api/orders/{id}/status`、`POST /api/orders/{id}/pay`。

**最小可运行单元**

用户创建订单 → 商家看到订单 → 商家接单 → 用户支付 → 商家配送 → 送达 → 完成。完整走通一条订单的全生命周期。

**测试数据**

创建订单请求（假设药品 ID 已知）：
```json
{"items":[{"medicineId":1,"quantity":2},{"medicineId":2,"quantity":1}],"deliveryAddress":"幸福小区1号楼302","remark":"请下午送达"}
```

**验收指标**

- 创建订单后 `orders` 和 `order_items` 表正确写入，`order_no` 唯一且格式合理（如时间戳+随机数）
- `total_amount` 等于各 `OrderItem.subtotal` 之和
- 订单状态严格按照状态机流转，非法状态跳转被拒绝（如 PENDING 不能直接变 DELIVERED）
- 抢单场景：两个商家同时接同一订单，只有一个成功，另一个收到"订单已被接走"提示
- 放弃后订单回退 PENDING，且该商家无法再次接同一订单
- 亲友代付：payerId 正确记录，paidAt 正确设置

---

### 阶段十二：对话与 AI 模块（Chat + AI）

**实现内容**

AI 服务层：`MedicineChatService`（调用 DeepSeek API，传入系统提示词 + 用户消息，获取结构化 JSON 响应）；`IntentRecognitionService`（解析 AI 返回的 JSON，提取药品列表，查询药品库匹配附近商家的库存）；`SystemPromptTemplate`（定义系统提示词，要求 DeepSeek 扮演药房助手，返回固定 JSON 格式）；AI 层 DTO（`ChatRequest`、`ChatResponse`、`MedicineIntent`）。

业务层：`ChatService`（编排对话流程：组装上下文 → 调用 AI → 解析意图 → 匹配药品库 → 返回确认信息）。

表现层：`ChatController`：`POST /api/chat/send`（发送消息，返回 AI 识别结果）、`GET /api/chat/history`（获取对话历史，可存 Redis 或内存）。

**最小可运行单元**

用户发送"我头疼，想买布洛芬" → 系统调用 DeepSeek → 返回识别结果"布洛芬缓释胶囊 x 1，约 15.80 元，确认下单吗？"

**测试数据**

对话消息：
```json
{"message":"我最近总是咳嗽，想买点止咳的药"}
```
```json
{"message":"帮我加一盒阿莫西林，消炎的"}
```
```json
{"message":"好的，就这些了，帮我下单吧"}
```

**验收指标**

- DeepSeek 返回的 JSON 能被正确解析为 `MedicineIntent` 列表
- 系统能根据药品名在药品库中匹配到对应药品（模糊匹配）
- 返回给前端的信息包含药品名、数量、预估价格
- 无法识别的药品名时，AI 会追问用户而非硬猜
- 对话上下文在多轮交互中保持连贯（至少支持 5 轮）
- API Key 无效时返回友好错误提示，不暴露异常堆栈

---

### 阶段十三：亲友与通知模块（Family + Notification）

**实现内容**

`FamilyService`：通过 `familyId` 搜索用户；发起绑定请求（创建 PENDING 状态的 `FamilyRelation` 记录 → 发送通知给对方）；接受/拒绝绑定；查询我的亲友列表。

`NotificationService`：创建通知；查询用户的通知列表；查询未读通知数；标记已读。

`FamilyController`：`GET /api/users/search?familyId=xxx`、`POST /api/family/bind`、`POST /api/family/{id}/accept`、`POST /api/family/{id}/reject`、`GET /api/family/members`。

`NotificationController`：`GET /api/notifications`、`GET /api/notifications/unread-count`、`PUT /api/notifications/{id}/read`。

**最小可运行单元**

用户 A 通过 familyId 搜索用户 B → 发起绑定 → 用户 B 收到通知 → 用户 B 接受绑定 → 双方亲友列表中能看到对方。

**测试数据**

用户 A（familyId: 1234567890）发起绑定：
```json
{"targetFamilyId":"0987654321","relationship":"子女"}
```

**验收指标**

- 通过 familyId 搜索能正确返回对应用户信息（脱敏，不返回密码）
- 绑定请求创建后，对方收到类型为 FAMILY 的通知
- 接受后双方 `family_relations` 记录状态为 ACCEPTED
- 不能重复绑定同一用户
- 不能绑定自己
- 通知列表按时间倒序排列
- 未读数在创建新通知后正确递增

---

### 阶段十四：WebSocket 实时通信

**实现内容**

`OrderWebSocketHandler`：继承 `TextWebSocketHandler`，维护商家连接池（`ConcurrentHashMap`）。订单创建时广播给附近商家（JSON 格式：订单号、药品摘要、配送地址）；订单状态变更时推送给对应用户。

`NotificationWebSocketHandler`：新通知创建时实时推送给目标用户。

与 `WebSocketConfig` 中注册的端点对接，处理连接建立、消息发送、连接关闭等事件。

**最小可运行单元**

商家通过 WebSocket 客户端连接 `/ws/orders` → 用户创建订单 → 商家端实时收到新订单推送。

**测试数据**

使用浏览器控制台或 WebSocket 测试工具连接 `ws://localhost:8080/ws/orders`，携带 JWT 参数进行身份识别。

**验收指标**

- 商家连接 WebSocket 后，新订单创建时 3 秒内收到推送
- 推送消息包含订单号、药品列表摘要、配送地址
- 订单状态变更时，对应用户的 WebSocket 连接收到状态更新
- 连接断开后自动从连接池移除，不造成内存泄漏
- 未认证的 WebSocket 连接被拒绝

---

### 阶段十五：集成验证与全流程测试

**实现内容**

将所有模块串联，走通两条核心业务主线。

主线一（对话式购药）：注册/登录 → 与 AI 对话描述症状 → AI 识别药品 → 确认下单 → 订单广播 → 商家抢单 → 用户支付 → 商家配送 → 送达完成。

主线二（亲友代付）：老人注册 → 与子女通过 familyId 绑定 → 老人对话购药 → 确认订单 → 订单进入 WAITING_PAYMENT → 子女收到通知 → 子女代付 → 订单继续流转。

**测试数据**

准备以下测试账号：
- 用户：`elder01` / 张大爷 / familyId 自动生成
- 家属：`family01` / 张小明（张大爷的儿子）/ familyId 自动生成
- 商家A：`shop_a` / 康乐药店 / 坐标(39.910, 116.410)
- 商家B：`shop_b` / 仁心药房 / 坐标(39.915, 116.420)
- 药品：布洛芬缓释胶囊(15.80元)、阿莫西林胶囊(12.50元)、复方甘草片(8.00元)

**验收指标**

- 主线一全流程无阻断，每个环节数据正确落库
- 主线二中亲友绑定、通知推送、代付流程完整走通
- 并发抢单场景下订单不会被重复接单
- 所有接口返回统一的 `ApiResponse` JSON 格式
- 异常场景（无效 token、无权限、资源不存在）均有合理错误提示
- 应用启动时间不超过 30 秒（本地环境）
