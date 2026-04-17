# 演出门票抢购系统

基于 Spring Boot + JDK 21 的高并发演出门票抢购系统。

## 技术栈

- **后端**: Spring Boot 3.2, JDK 21, MyBatis-Plus, Kafka
- **缓存**: Redis + Caffeine (二级缓存)
- **数据库**: MySQL 8.0
- **消息队列**: Apache Kafka
- **限流**: Redis + AOP + 滑动窗口算法
- **前端**: HTML5 + CSS3 + Vanilla JS
- **部署**: Docker, Nginx

## 核心功能

1. **Redis + Lua 脚本** - 库存校验与购票资格校验，同一用户限购3张
2. **Kafka 异步处理** - 订单生成、库存扣减、支付宝支付
3. **乐观锁** - 并发控制，解决超卖问题
4. **二级缓存** - Caffeine + Redis，逻辑过期解决击穿，缓存空值解决穿透
5. **滑动窗口限流** - 全局、IP、用户三级限流
6. **定时关单** - 15分钟未支付自动释放库存
7. **支付回调** - 支付宝回调处理发票生成

## 项目结构

```
ticket-buy/
├── backend/                    # Spring Boot 后端
│   ├── src/main/java/com/ticketbuy/
│   │   ├── config/            # 配置类
│   │   ├── controller/         # 控制器
│   │   ├── service/           # 服务层
│   │   ├── repository/         # 数据访问层
│   │   ├── entity/            # 实体类
│   │   ├── dto/               # 数据传输对象
│   │   ├── kafka/             # Kafka 生产者/消费者
│   │   ├── cache/             # 缓存配置
│   │   ├── annotation/        # 自定义注解
│   │   ├── aspect/            # AOP切面
│   │   └── exception/          # 异常处理
│   ├── src/main/resources/
│   │   ├── lua/               # Lua 脚本
│   │   └── application.yml    # 应用配置
│   └── Dockerfile
├── frontend/                   # 前端页面
│   ├── index.html
│   ├── detail.html
│   ├── orders.html
│   ├── css/
│   └── js/
├── docs/                       # 文档
│   └── init.sql               # 数据库初始化脚本
├── docker-compose.yml          # Docker 部署配置
└── README.md
```

## 快速启动

### 方式一：Docker 一键部署

```bash
# 克隆项目
git clone <repository-url>
cd ticket-buy

# 启动所有服务
docker-compose up -d

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f backend
```

服务地址：

- 前端页面: <http://localhost>
- API接口: <http://localhost/api>

### 方式二：本地开发

#### 前置条件

- JDK 21
- MySQL 8.0
- Redis 7.0
- Kafka 3.x

#### 启动步骤

1. **初始化数据库**

```bash
mysql -uroot -p < docs/init.sql
```

1. **修改配置**
   编辑 `backend/src/main/resources/application.yml`，配置数据库、Redis、Kafka 连接信息。
2. **启动后端**

```bash
cd backend
mvn spring-boot:run
```

1. **启动前端**
   使用任意 HTTP 服务器托管 `frontend/` 目录，或直接用浏览器打开 `index.html`。

## API 接口

### 用户接口

- `POST /api/user/login` - 用户登录
- `POST /api/user/register` - 用户注册
- `POST /api/user/logout` - 用户登出
- `GET /api/user/info` - 获取用户信息

### 演出接口

- `GET /api/events` - 获取演出列表
- `GET /api/events/{id}` - 获取演出详情
- `GET /api/events/categories` - 获取演出分类
- `POST /api/events/buy` - 购买门票

### 订单接口

- `GET /api/orders` - 获取用户订单
- `GET /api/orders/{orderNo}` - 获取订单详情
- `POST /api/orders/{orderNo}/cancel` - 取消订单

### 支付接口

- `GET /api/pay/create/{orderNo}` - 创建支付
- `POST /api/pay/callback` - 支付回调
- `GET /api/pay/query/{orderNo}` - 查询支付状态

## 测试账号

| 用户名       | 密码     |
| --------- | ------ |
| testuser  | 123456 |
| testuser2 | 123456 |

## 配置说明

### 限流配置

```yaml
ticket:
  rate-limit:
    global-window: 60      # 全局限流窗口（秒）
    global-max: 10000     # 全局最大请求数
    ip-window: 60         # IP限流窗口（秒）
    ip-max: 100           # IP最大请求数
    user-window: 60        # 用户限流窗口（秒）
    user-max: 20          # 用户最大请求数
```

### 缓存配置

```yaml
ticket:
  cache:
    l1-ttl: 300           # Caffeine L1缓存 TTL（秒）
    l2-ttl: 3600          # Redis L2缓存 TTL（秒）
    null-value-ttl: 120    # 空值缓存 TTL（秒）
```

### 订单配置

```yaml
ticket:
  max-buy-per-user: 3     # 每用户单票档限购数量
  order-timeout-minutes: 15  # 订单超时时间（分钟）
```

## 部署架构

```
                    ┌─────────────┐
                    │   Client    │
                    └──────┬──────┘
                           │
                    ┌──────▼──────┐
                    │    Nginx    │
                    │  (反向代理)  │
                    └──────┬──────┘
                           │
              ┌────────────┴────────────┐
              │                         │
       ┌──────▼──────┐            ┌──────▼──────┐
       │   Backend   │            │   Frontend  │
       │  (Spring)   │            │   (静态)    │
       └──────┬──────┘            └─────────────┘
              │
    ┌─────────┼─────────┬─────────┐
    │         │         │         │
┌───▼───┐ ┌───▼───┐ ┌───▼───┐ ┌───▼───┐
│ MySQL │ │ Redis │ │ Kafka │ │ ZK    │
└───────┘ └───────┘ └───────┘ └───────┘
```

## 注意事项

1. **支付宝配置**: 生产环境需要替换 `ALIPAY_APP_ID`、`ALIPAY_PRIVATE_KEY`、`ALIPAY_PUBLIC_KEY` 为真实的支付宝应用配置。
2. **Redis 密码**: 默认密码 `redis123`，生产环境请修改。
3. **MySQL 密码**: 默认密码 `ticket123`，生产环境请修改。
4. **高并发测试**: 可使用 Apache JMeter 或 wrk 进行压力测试。

## License

MIT License
