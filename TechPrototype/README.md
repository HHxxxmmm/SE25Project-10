# Mini12306 票务核心系统

## 项目简介

这是一个高并发的铁路票务核心系统，实现了购票、退票、改签等核心功能。系统采用Redis预减库存、RabbitMQ异步处理、Redisson分布式锁等技术来保证高并发场景下的数据一致性。

## 技术栈

- **后端框架**: Spring Boot 3.4.7
- **数据库**: MySQL 8.0
- **缓存**: Redis
- **消息队列**: RabbitMQ
- **分布式锁**: Redisson
- **ORM**: Spring Data JPA
- **ID生成**: 雪花算法

## 核心功能

### 1. 购票功能
- Redis预减库存（原子操作）
- RabbitMQ异步创建订单
- 分布式锁保证并发安全
- 座位自动分配
- 区间占用逻辑处理
- 优惠价格计算
- 时间冲突检测（防止同一乘客在同一时间段购买多张票）**（当前已暂时屏蔽用于并发测试）**

### 2. 退票功能
- 车票状态验证
- 库存回滚
- 座位释放
- 区间占用回滚

### 3. 改签功能
- 旧票作废
- 新票生成
- 库存转移
- 区间占用处理
- 时间冲突检测（改签时检查新时间段是否与乘客其他票冲突）

### 4. 订单管理
- 订单状态管理
- 支付处理
- 车票生成
- 超时订单自动取消
- 取消待支付订单

### 5. 库存管理
- Redis与MySQL定时同步
- 区间占用逻辑
- 乐观锁防止数据不一致

## 系统架构

```
用户请求 -> TicketController -> TicketService -> RedisService
                                    |
                                    v
                              RabbitMQ -> OrderProcessor -> 数据库
```

## 数据库设计

系统严格按照提供的数据库结构设计，主要包含以下表：
- `carriage_types`: 车厢类型
- `passengers`: 乘客信息
- `stations`: 车站信息
- `trains`: 列车信息
- `train_carriages`: 列车车厢
- `seats`: 座位信息
- `train_stops`: 列车停靠站
- `ticket_inventory`: 票务库存
- `users`: 用户信息
- `orders`: 订单信息
- `tickets`: 车票信息
- `user_passenger_relations`: 用户乘客关系

## 高并发设计

### 1. Redis预减库存
- 使用Lua脚本保证原子性
- 预减库存避免超卖

### 2. 分布式锁
- 使用Redisson实现分布式锁
- 防止并发冲突

### 3. 异步处理
- RabbitMQ异步创建订单
- 提高系统响应速度

### 4. 库存管理
- 区间占用逻辑处理
- 乐观锁防止数据不一致
- 定时同步Redis到MySQL

### 5. 优惠价格体系
- 成人票：全价
- 儿童票：5折优惠
- 学生票：7.5折优惠
- 残疾票：免费
- 军人票：免费

## 安装和运行

### 环境要求
- JDK 21
- MySQL 8.0
- Redis 6.0+
- RabbitMQ 3.8+

### 配置
1. 修改 `application.properties` 中的数据库连接信息
2. 确保Redis和RabbitMQ服务正常运行

### 启动
```bash
mvn spring-boot:run
```

## API接口

### 购票接口
```
POST /api/ticket/book
Content-Type: application/json

{
  "userId": 1,
  "trainId": 1,
  "departureStopId": 1,
  "arrivalStopId": 2,
  "travelDate": "2025-07-01",
  "carriageTypeId": 1,
  "passengers": [
    {
      "passengerId": 1,
      "ticketType": 1
    }
  ]
}
```

### 退票接口
```
POST /api/ticket/refund
Content-Type: application/json

{
  "userId": 1,
  "orderNumber": "订单号",
  "ticketIds": [1, 2],
  "refundReason": "行程变更"
}
```

### 改签接口
```
POST /api/ticket/change
Content-Type: application/json

{
  "userId": 1,
  "orderNumber": "订单号",
  "ticketIds": [1, 2],
  "newTrainId": 2,
  "newDepartureStopId": 1,
  "newArrivalStopId": 3,
  "newTravelDate": "2025-07-02",
  "newCarriageTypeId": 1
}
```

### 支付接口
```
POST /api/payment/pay?orderNumber=订单号&userId=1
```

### 取消订单接口
```
POST /api/order/cancel
Content-Type: application/json

{
  "userId": 1,
  "orderNumber": "订单号",
  "cancelReason": "用户主动取消"
}
```

## 测试接口

系统提供了测试接口用于验证功能：

### 基础测试
- `POST /api/test/book`: 测试购票
- `GET /api/test/stock`: 测试查询库存
- `POST /api/test/pay`: 测试支付
- `POST /api/test/book-and-pay`: 测试完整购票支付流程

### 时间冲突测试
- `POST /api/test/test-time-conflict`: 测试时间冲突检测
- `POST /api/test/test-different-time`: 测试不同时间段购票（应该成功）
- `POST /api/test/test-cross-day-conflict`: 测试跨天时间冲突检测
- `POST /api/test/test-pending-conflict`: 测试待支付状态时间冲突检测

### 高并发测试
- `POST /api/test/concurrent-test?count=1000`: 高并发测试（1000个并发请求，时间冲突检测已屏蔽）

### 其他测试
- `POST /api/test/change-and-pay`: 测试改签完整流程
- `GET /api/test/order-tickets`: 获取订单车票信息
- `GET /api/test/check-order`: 检查订单是否存在
- `GET /api/test/seats`: 测试座位数据
- `GET /api/test/test-train-stops`: 测试车次停靠站数据

## 当前系统状态

### 已屏蔽的功能（用于并发测试）
1. **时间冲突检测**: 购票时不再检查乘客的时间冲突，允许同一乘客购买同一时间段的多张票
2. **用户乘客关系验证**: 购票时不再验证乘客是否属于当前用户

### 高并发测试说明
- 使用 `/api/test/concurrent-test` 端点进行高并发测试
- 测试使用同一个乘客ID（319）进行1000个并发请求
- 使用多个车次（1-20循环）分散库存压力
- 使用二等座（carriageTypeId=3）确保库存充足
- 测试库存扣减的原子性和分布式锁的有效性

## 注意事项

1. **时间冲突检测**: 当前已暂时屏蔽，用于并发测试。在生产环境中应该启用。
2. **用户乘客关系验证**: 当前已暂时屏蔽，用于并发测试。在生产环境中应该启用。
3. **库存管理**: 系统使用Redis作为主要库存源，MySQL作为备份。
4. **订单状态**: 订单创建后需要及时支付，否则会被自动取消。
5. **改签机制**: 改签会创建新订单，原票状态更新为CHANGED。
6. **座位分配**: 座位在支付成功后自动分配。

## 性能优化

1. **Redis预减库存**: 避免超卖，提高响应速度
2. **异步订单创建**: 使用RabbitMQ异步处理，提高并发能力
3. **分布式锁**: 使用Redisson保证并发安全
4. **库存同步**: 定时同步Redis到MySQL，保证数据一致性
5. **区间占用**: 正确处理列车区间占用逻辑 