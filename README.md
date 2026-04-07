# 商品库存与秒杀系统

分布式软件原理与技术 · 课程作业

## 项目介绍

基于 Spring Boot + MyBatis + MySQL 的商品库存与秒杀系统，采用 Docker 容器化部署，实现高并发读场景下的负载均衡、动静分离、分布式缓存、MySQL读写分离和ElasticSearch商品搜索。

## 技术栈

| 技术 | 说明 |
|------|------|
| Spring Boot 2.7.18 | 后端框架 |
| MyBatis 2.3.2 | ORM框架 |
| MySQL 8.0 (主从) | 关系型数据库，支持读写分离 |
| Redis 7 | 分布式缓存 |
| ElasticSearch 7.17 | 商品搜索引擎 |
| Nginx | 反向代理 / 负载均衡 / 动静分离 |
| Docker Compose | 容器编排 |
| JWT | 用户认证 |
| Druid | 数据库连接池 |

## 项目结构

```
DistributedSoftware/
├── Dockerfile                          # 后端服务镜像（多阶段构建）
├── docker-compose.yml                  # 容器编排配置
├── pom.xml                             # Maven依赖管理
├── nginx/
│   └── nginx.conf                      # Nginx配置（负载均衡 + 动静分离）
├── mysql/
│   ├── master.cnf                      # MySQL主库配置
│   ├── slave.cnf                       # MySQL从库配置
│   └── init-slave.sh                   # 从库主从复制初始化脚本
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
    │   │   ├── DataSourceConfig.java   # 动态数据源配置
    │   │   ├── DynamicDataSource.java  # 动态数据源路由
    │   │   ├── DynamicDataSourceContextHolder.java
    │   │   ├── DataSourceAspect.java   # AOP切面（自动切换数据源）
    │   │   └── ReadOnly.java           # 读操作注解
    │   ├── controller/
    │   │   ├── UserController.java     # 用户注册/登录
    │   │   ├── ProductController.java  # 商品查询 + 读写分离测试
    │   │   └── SearchController.java   # ES商品搜索
    │   ├── service/impl/
    │   │   ├── UserServiceImpl.java
    │   │   ├── ProductServiceImpl.java # Redis缓存 + @ReadOnly读写分离
    │   │   └── ProductSearchServiceImpl.java  # ES搜索服务
    │   ├── mapper/
    │   │   ├── UserMapper.java
    │   │   ├── ProductMapper.java
    │   │   └── ProductSearchRepository.java   # ES Repository
    │   ├── entity/
    │   │   ├── User.java
    │   │   ├── Product.java
    │   │   └── ProductDocument.java    # ES文档实体
    │   ├── dto/
    │   └── util/
    └── resources/
        ├── application.yml             # 应用配置（主从数据源 + Redis + ES）
        ├── schema.sql                  # 数据库初始化脚本
        └── mapper/*.xml
```

## 功能模块

### 用户服务
- POST `/api/user/register` — 用户注册
- POST `/api/user/login` — 用户登录（返回JWT Token）

### 商品服务
- GET `/api/product/list` — 商品列表（@ReadOnly → 从库读取）
- GET `/api/product/{id}` — 商品详情（Redis缓存 + @ReadOnly）
- GET `/api/product/port` — 查看处理请求的后端端口
- GET `/api/product/rw-test` — 读写分离测试接口

### 搜索服务（ElasticSearch）
- GET `/api/search?keyword=xxx` — 商品搜索
- POST `/api/search/sync` — 同步MySQL商品数据到ES

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
- **验证**：访问 `/api/product/rw-test`，后端日志会打印 `[读写分离] 切换到从库`

### 5. 商品搜索（ElasticSearch）
- 通过 `/api/search/sync` 将MySQL商品数据同步到ES
- 通过 `/api/search?keyword=iPhone` 进行全文搜索
- 支持商品名称和描述的模糊匹配

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

启动后的服务：

| 服务 | 地址 | 说明 |
|------|------|------|
| 前端页面 | http://localhost | Nginx代理 |
| 后端实例1 | http://localhost:8081 | Spring Boot |
| 后端实例2 | http://localhost:8082 | Spring Boot |
| MySQL主库 | localhost:3307 | 写操作 |
| MySQL从库 | localhost:3308 | 读操作 |
| Redis | localhost:6380 | 缓存 |
| ElasticSearch | http://localhost:9200 | 搜索 |

### 启动后操作

```bash
# 1. 同步商品数据到ES
curl -X POST http://localhost/api/search/sync

# 2. 搜索测试
curl http://localhost/api/search?keyword=iPhone

# 3. 读写分离测试（查看后端日志确认主从切换）
curl http://localhost/api/product/rw-test

# 4. 负载均衡测试
for i in $(seq 1 10); do curl -s http://localhost/api/product/port; echo; done
```

## JMeter 压测建议

1. **负载均衡验证**：压测 `GET http://localhost/api/product/port`，检查后端日志验证请求分布
2. **动静分离对比**：分别压测 `GET http://localhost/css/style.css` 和 `GET http://localhost/api/product/list`，对比响应时间
3. **缓存效果**：压测 `GET http://localhost/api/product/1`，对比 Redis 命中和未命中的响应时间
4. **ES搜索性能**：压测 `GET http://localhost/api/search?keyword=iPhone`
