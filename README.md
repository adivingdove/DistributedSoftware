# 商品库存与秒杀系统

分布式软件原理与技术 · 课程作业

## 项目介绍

基于 Spring Boot + MyBatis + MySQL 的商品库存与秒杀系统，采用 Docker 容器化部署，实现高并发读写场景下的负载均衡、动静分离、分布式缓存、MySQL读写分离、ElasticSearch商品搜索、Kafka消息队列异步下单、分布式事务（TCC事务 + 本地消息表最终一致性）。

## 技术栈

| 技术 | 说明 |
|------|------|
| Spring Boot 2.7.18 | 后端框架 |
| MyBatis 2.3.2 | ORM框架 |
| MySQL 8.0 (主从) | 关系型数据库，支持读写分离 |
| Redis 7 | 分布式缓存 + 库存预减 + 幂等校验 |
| Kafka (Confluent 7.5) | 消息队列，异步处理秒杀订单 |
| ElasticSearch 7.17 | 商品搜索引擎 |
| Nginx | 反向代理 / 负载均衡 / 动静分离 |
| ShardingSphere-JDBC 5.2.1 | 分库分表中间件 |
| Docker Compose | 容器编排（11个服务） |
| JWT | 用户认证 |
| 雪花算法 | 分布式订单ID生成 |
| TCC事务 + 本地消息表 | 分布式事务一致性 |

## 项目结构

```
DistributedSoftware/
├── Dockerfile                          # 后端服务镜像（多阶段构建）
├── docker-compose.yml                  # 容器编排（11个服务）
├── pom.xml                             # Maven依赖管理
├── nginx/
│   └── nginx.conf                      # Nginx配置（负载均衡 + 动静分离）
├── mysql/
│   ├── master.cnf                      # MySQL主库配置
│   ├── slave.cnf                       # MySQL从库配置
│   ├── init-slave.sh                   # 从库主从复制初始化脚本
│   ├── init-shard-0.sql                # 分片库0初始化脚本
│   └── init-shard-1.sql                # 分片库1初始化脚本
├── static/                             # 前端静态文件
│   ├── index.html
│   ├── css/style.css
│   └── js/app.js
└── src/main/
    ├── java/com/distributed/inventory/
    │   ├── InventorySeckillApplication.java
    │   ├── common/
    │   │   ├── Result.java             # 统一响应封装
    │   │   └── GlobalExceptionHandler.java
    │   ├── config/
    │   │   ├── DataSourceConfig.java   # 动态数据源配置（主从）
    │   │   ├── DynamicDataSource.java  # 动态数据源路由
    │   │   ├── DynamicDataSourceContextHolder.java
    │   │   ├── DataSourceAspect.java   # AOP切面（自动切换数据源）
    │   │   ├── ReadOnly.java           # 读操作注解
    │   │   ├── SeckillConfig.java      # 秒杀配置（雪花ID + Kafka Topic）
    │   │   └── ShardingDataSourceConfig.java  # ShardingSphere分库分表配置
    │   ├── controller/
    │   │   ├── UserController.java     # 用户注册/登录
    │   │   ├── ProductController.java  # 商品查询 + 读写分离测试
    │   │   ├── SearchController.java   # ES商品搜索
    │   │   ├── SeckillController.java  # 秒杀下单 + 订单查询
    │   │   └── PaymentController.java  # TCC支付接口
    │   ├── service/impl/
    │   │   ├── UserServiceImpl.java
    │   │   ├── ProductServiceImpl.java # Redis缓存（穿透/击穿/雪崩）
    │   │   ├── ProductSearchServiceImpl.java  # ES搜索
    │   │   ├── SeckillServiceImpl.java # 秒杀核心逻辑
    │   │   ├── SeckillOrderConsumer.java  # Kafka消费者
    │   │   ├── TccOrderService.java    # TCC事务接口
    │   │   ├── TccOrderServiceImpl.java # TCC事务实现
    │   │   └── ReliableMessageService.java  # 本地消息表服务
    │   ├── mapper/
    │   │   ├── UserMapper.java
    │   │   ├── ProductMapper.java
    │   │   ├── SeckillOrderMapper.java
    │   │   ├── ProductSearchRepository.java
    │   │   ├── TransactionMessageMapper.java  # 事务消息Mapper
    │   │   ├── TccTransactionMapper.java      # TCC事务Mapper
    │   │   └── order/
    │   │       └── ShardingSeckillOrderMapper.java  # 分片订单Mapper
    │   ├── entity/
    │   │   ├── User.java
    │   │   ├── Product.java
    │   │   ├── SeckillOrder.java       # 秒杀订单实体
    │   │   ├── ProductDocument.java    # ES文档实体
    │   │   ├── TransactionMessage.java # 事务消息实体
    │   │   └── TccTransaction.java     # TCC事务实体
    │   ├── dto/
    │   │   └── SeckillMessage.java     # Kafka消息DTO
    │   ├── util/
    │   │   ├── JwtUtil.java
    │   │   ├── MD5Util.java
    │   │   └── SnowflakeIdGenerator.java  # 雪花算法ID生成器
    │   └── task/
    │       └── TransactionRecoveryTask.java  # 分布式事务恢复定时任务
    └── resources/
        ├── application.yml             # 应用配置（MySQL主从 + Redis + ES + Kafka）
        ├── schema.sql                  # 数据库初始化（用户表 + 商品表 + 订单表）
        ├── mapper/*.xml
        ├── mapper/TransactionMessageMapper.xml  # 事务消息Mapper XML
        ├── mapper/TccTransactionMapper.xml      # TCC事务Mapper XML
        └── mapper/order/
            └── ShardingSeckillOrderMapper.xml  # 分片订单Mapper XML

```

## 功能模块

### 用户服务
- POST `/api/user/register` — 用户注册
- POST `/api/user/login` — 用户登录（返回JWT Token + userId）

### 商品服务
- GET `/api/product/list` — 商品列表（@ReadOnly → 从库读取）
- GET `/api/product/{id}` — 商品详情（Redis缓存 + @ReadOnly）
- GET `/api/product/port` — 查看处理请求的后端端口
- GET `/api/product/rw-test` — 读写分离测试接口

### 搜索服务（ElasticSearch）
- GET `/api/search?keyword=xxx` — 商品搜索
- POST `/api/search/sync` — 同步MySQL商品数据到ES

### 秒杀服务（Kafka + Redis）
- POST `/api/seckill/{productId}?userId=xxx` — 秒杀下单
- GET `/api/seckill/order/{orderId}` — 按订单ID查询
- GET `/api/seckill/orders?userId=xxx` — 按用户ID查询订单列表
- POST `/api/seckill/init-stock` — 重新初始化Redis库存缓存

### 分片订单服务（ShardingSphere）
- GET `/api/seckill/sharding/order/{orderId}` — 按订单ID查询分片订单
- GET `/api/seckill/sharding/orders?userId=xxx` — 按用户ID查询分片订单列表

### 分布式事务服务（TCC + 本地消息表）
- POST `/api/payment/try` — TCC-Try 发起支付
- POST `/api/payment/confirm` — TCC-Confirm 确认支付
- POST `/api/payment/cancel` — TCC-Cancel 取消支付

## 高并发读方案

```
用户请求 → Nginx(80) ─┬─ 静态文件(CSS/JS) → 直接返回（动静分离）
                       └─ API请求 → App1(8081) / App2(8082)（负载均衡）
                                         │
                                    ┌─────┴─────┐
                                    │   Redis    │  ← 分布式缓存
                                    └─────┬─────┘
                                     缓存未命中
                                    ┌─────┴─────┐
                              ┌─────┤ 读写分离  ├─────┐
                              │     └───────────┘     │
                         MySQL主库              MySQL从库
                         (写操作)              (读操作@ReadOnly)
```

### 1. 负载均衡（Nginx）
Nginx 将 API 请求分发到两个后端实例，支持三种算法：
- **轮询**（默认）：请求依次分配
- **加权轮询**：按权重分配
- **IP Hash**：同一IP固定到同一后端

### 2. 动静分离（Nginx）
- 静态资源（CSS/JS/图片）由 Nginx 直接返回，设置7天缓存
- API 请求转发到后端服务处理

### 3. 分布式缓存（Redis）
商品详情接口使用 Redis 缓存，并处理三种常见问题：

| 问题 | 解决方案 |
|------|---------|
| 缓存穿透 | null值缓存（TTL 5分钟） |
| 缓存击穿 | Redis分布式锁 + 双重检查 |
| 缓存雪崩 | TTL随机偏移（30 + 0~10分钟） |

### 4. 读写分离（MySQL主从）
- **主库**（mysql-master:3307）：处理写操作（INSERT/UPDATE/DELETE）
- **从库**（mysql-slave:3308）：处理读操作（SELECT），通过GTID自动同步
- **实现方式**：自定义 `@ReadOnly` 注解 + AOP切面 + AbstractRoutingDataSource动态路由

### 5. 商品搜索（ElasticSearch）
- 通过 `/api/search/sync` 将MySQL商品数据同步到ES
- 通过 `/api/search?keyword=iPhone` 进行全文搜索

## 高并发写方案（秒杀）

```
秒杀请求 → Nginx → App
                    │
        ┌───────────┴───────────┐
        │  1. Redis幂等校验     │  ← SETNX 防止重复下单
        │  2. Redis预减库存     │  ← DECR 原子操作，快速判断库存
        │  3. 发送Kafka消息     │  ← 异步削峰填谷
        └───────────┬───────────┘
                    │
        ┌───────────┴───────────┐
        │  Kafka Consumer       │
        │  1. DB幂等二次校验    │  ← 查询是否已有订单
        │  2. 扣减DB库存        │  ← UPDATE ... WHERE stock > 0
        │  3. 创建秒杀订单      │  ← 雪花算法生成订单ID
        └───────────────────────┘
```

### 核心设计

| 特性 | 实现方式 |
|------|---------|
| **削峰填谷** | Kafka消息队列异步处理，前端立即返回 |
| **库存预减** | Redis DECR 原子操作，毫秒级判断库存是否充足 |
| **幂等性** | Redis SETNX 防重 + DB唯一索引(user_id, product_id)双重校验 |
| **防超卖** | Redis预减 + DB层 `WHERE stock > 0` 乐观锁 |
| **订单ID** | 雪花算法（SnowflakeIdGenerator），支持分布式唯一ID |
| **数据一致性** | Kafka消费者事务内完成扣库存+创建订单 |

### 雪花算法ID结构
```
0 | 41位时间戳 | 5位数据中心ID | 5位机器ID | 12位序列号
```
- 两个后端实例使用不同的 `SNOWFLAKE_WORKER_ID`（1和2），保证ID不冲突

## 分库分表（ShardingSphere-JDBC）

使用 **ShardingSphere-JDBC 5.2.1** 实现订单表的分库分表，将秒杀订单水平拆分到多个数据库和表中，提升写入性能和数据存储容量。

### 分片架构

```
秒杀订单写入 → ShardingSphere-JDBC
                    │
        ┌───────────┴───────────┐
        │    分库规则            │  ← user_id % 2 决定数据库
        │    分表规则            │  ← order_id % 2 决定表
        └───────────┬───────────┘
                    │
        ┌───────────┴───────────┐
   seckill_order_0          seckill_order_1
   (mysql-shard0:3309)      (mysql-shard1:3310)
   ├── seckill_order_0      ├── seckill_order_0
   └── seckill_order_1      └── seckill_order_1
```

### 分片规则

| 维度 | 规则 | 说明 |
|------|------|------|
| **分库** | user_id % 2 | user_id为偶数 → seckill_order_0，奇数 → seckill_order_1 |
| **分表** | order_id % 2 | order_id为偶数 → seckill_order_0，奇数 → seckill_order_1 |

### 分片拓扑

2个数据库 × 2张表 = **4个分片表**：

| 数据库 | 表 | Docker服务 | 端口 |
|--------|-----|-----------|------|
| seckill_order_0 | seckill_order_0 | mysql-shard0 | 3309 |
| seckill_order_0 | seckill_order_1 | mysql-shard0 | 3309 |
| seckill_order_1 | seckill_order_0 | mysql-shard1 | 3310 |
| seckill_order_1 | seckill_order_1 | mysql-shard1 | 3310 |

### API接口

- GET `/api/seckill/sharding/order/{orderId}` — 按订单ID查询分片订单
- GET `/api/seckill/sharding/orders?userId=xxx` — 按用户ID查询分片订单列表

## 分布式事务

### 基于消息的最终一致性（下单+库存扣减）
- 本地消息表（transaction_message）保证消息可靠投递
- Kafka异步消费：扣减DB库存 + 创建订单（同一事务）
- 定时任务重试：每30秒扫描未确认消息，自动重发
- 消费端幂等：DB唯一索引 + 重复检查

### TCC事务（订单支付+状态更新）
- Try阶段：冻结订单（status=3 支付中），记录tcc_transaction
- Confirm阶段：确认支付（status=1 已支付），更新事务状态CONFIRMED
- Cancel阶段：回滚（status=0 待支付），更新事务状态CANCELLED
- 悬挂事务恢复：定时任务每60秒扫描TRYING状态超5分钟的事务，自动Cancel

### API接口

| 接口 | 方法 | 说明 |
|------|------|------|
| /api/payment/try | POST | TCC-Try 发起支付 |
| /api/payment/confirm | POST | TCC-Confirm 确认支付 |
| /api/payment/cancel | POST | TCC-Cancel 取消支付 |

### 新增数据表
- transaction_message：事务消息表（本地消息表模式）
- tcc_transaction：TCC事务记录表

### 新增文件
- PaymentController.java
- TccOrderService.java / TccOrderServiceImpl.java
- ReliableMessageService.java
- TransactionRecoveryTask.java
- TransactionMessage.java / TccTransaction.java
- TransactionMessageMapper.java / TccTransactionMapper.java + XML

## 快速启动

### Docker一键启动

```bash
# 停止旧容器并清理
docker compose down -v

# 构建并启动所有服务
docker compose up --build -d

# 查看启动状态
docker compose ps

# 查看日志
docker compose logs -f
```

启动后的服务（共11个容器）：

| 服务 | 地址 | 说明 |
|------|------|------|
| Nginx | http://localhost | 前端入口 + 反向代理 |
| 后端实例1 | http://localhost:8081 | Spring Boot (worker=1) |
| 后端实例2 | http://localhost:8082 | Spring Boot (worker=2) |
| MySQL主库 | localhost:3307 | 写操作 |
| MySQL从库 | localhost:3308 | 读操作 |
| MySQL分片0 | localhost:3309 | 分库分表（seckill_order_0） |
| MySQL分片1 | localhost:3310 | 分库分表（seckill_order_1） |
| Redis | localhost:6380 | 缓存 + 库存 |
| ElasticSearch | http://localhost:9200 | 搜索引擎 |
| Kafka | localhost:9092 | 消息队列 |
| Zookeeper | localhost:2181 | Kafka协调 |

### 启动后操作

```bash
# 1. 同步商品数据到ES
curl -X POST http://localhost/api/search/sync

# 2. 搜索测试
curl http://localhost/api/search?keyword=iPhone

# 3. 读写分离测试
curl http://localhost/api/product/rw-test

# 4. 秒杀测试（需要先注册登录获取userId）
curl -X POST "http://localhost/api/seckill/1?userId=1"

# 5. 查询订单
curl "http://localhost/api/seckill/orders?userId=1"

# 6. 负载均衡测试
for i in $(seq 1 10); do curl -s http://localhost/api/product/port; echo; done
```

## JMeter 压测建议

1. **秒杀并发测试**：100个线程同时对 `POST /api/seckill/1?userId={threadNum}` 发请求，验证不超卖
2. **负载均衡验证**：压测 `GET /api/product/port`，验证请求均匀分布
3. **动静分离对比**：分别压测 `/css/style.css` 和 `/api/product/list`，对比响应时间
4. **缓存效果**：压测 `GET /api/product/1`，对比 Redis 命中/未命中响应时间
5. **ES搜索性能**：压测 `GET /api/search?keyword=iPhone`
