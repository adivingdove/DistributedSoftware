# 商品库存与秒杀系统

分布式软件原理与技术 · 课程作业

## 项目介绍

基于 Spring Boot + MyBatis + MySQL 的商品库存与秒杀系统，采用 Docker 容器化部署，实现高并发读场景下的负载均衡、动静分离和分布式缓存。

## 技术栈

| 技术 | 说明 |
|------|------|
| Spring Boot 2.7.18 | 后端框架 |
| MyBatis 2.3.2 | ORM框架 |
| MySQL 8.0 | 关系型数据库 |
| Redis 7 | 分布式缓存 |
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
    │   ├── controller/
    │   │   ├── UserController.java     # 用户注册/登录
    │   │   └── ProductController.java  # 商品查询
    │   ├── service/impl/
    │   │   ├── UserServiceImpl.java
    │   │   └── ProductServiceImpl.java # Redis缓存（穿透/击穿/雪崩）
    │   ├── mapper/
    │   ├── entity/
    │   ├── dto/
    │   └── util/
    └── resources/
        ├── application.yml             # 应用配置（支持环境变量）
        ├── schema.sql                  # 数据库初始化脚本
        └── mapper/*.xml                # MyBatis映射文件
```

## 功能模块

### 用户服务
- POST `/api/user/register` — 用户注册
- POST `/api/user/login` — 用户登录（返回JWT Token）

### 商品服务
- GET `/api/product/list` — 商品列表
- GET `/api/product/{id}` — 商品详情（Redis缓存）
- GET `/api/product/port` — 查看处理请求的后端端口

## 高并发读方案

```
用户请求 → Nginx(80) ─┬─ 静态文件(CSS/JS) → 直接返回
                       └─ API请求 → App1(8081) / App2(8082) → Redis → MySQL
```

### 负载均衡
Nginx 将 API 请求分发到两个后端实例，支持三种算法：
- **轮询**（默认）：请求依次分配
- **加权轮询**：按权重分配
- **IP Hash**：同一IP固定到同一后端

### 动静分离
- 静态资源（CSS/JS/图片）由 Nginx 直接返回，设置7天缓存
- API 请求转发到后端服务处理

### 分布式缓存（Redis）
商品详情接口使用 Redis 缓存，并处理三种常见问题：

| 问题 | 解决方案 |
|------|---------|
| 缓存穿透 | null值缓存（TTL 5分钟） |
| 缓存击穿 | Redis分布式锁 + 双重检查 |
| 缓存雪崩 | TTL随机偏移（30 + 0~10分钟） |

## 快速启动

### Docker一键启动（推荐）

```bash
docker compose up --build -d
```

启动后的服务：

| 服务 | 地址 |
|------|------|
| 前端页面 | http://localhost |
| 后端实例1 | http://localhost:8081 |
| 后端实例2 | http://localhost:8082 |
| MySQL | localhost:3307 |
| Redis | localhost:6380 |

### 本地开发

1. 安装并启动 MySQL、Redis
2. 执行 `src/main/resources/schema.sql` 初始化数据库
3. 修改 `application.yml` 中的数据库连接信息
4. 运行：
```bash
mvn spring-boot:run
```

## JMeter 压测建议

1. **负载均衡验证**：压测 `GET http://localhost/api/product/port`，检查后端日志验证请求分布
2. **动静分离对比**：分别压测 `GET http://localhost/css/style.css` 和 `GET http://localhost/api/product/list`，对比响应时间
3. **缓存效果**：压测 `GET http://localhost/api/product/1`，对比 Redis 命中和未命中的响应时间
