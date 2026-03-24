# 供应商库存数据接口 - 消息队列示例

独立于 PPM 主系统运行，通过 **RabbitMQ** 接收供应商库存数据，实现解耦与异步处理。

## 模块说明

| 组件 | 说明 |
|------|------|
| **消息模型** | `SupplierInventoryMessage`：供应商编码、物料编码、库存数量、仓库、更新时间、业务流水号 |
| **生产者** | `InventoryProducer`：将消息发送到交换机 `inventory.exchange`，路由键 `inventory.supplier` |
| **消费者** | `InventoryConsumer`：监听队列 `inventory.supplier.queue`，消费后写入 `InventoryStore` |
| **存储** | `InventoryStore`：示例为内存 Map；可替换为 MySQL 或调用 PPM 主系统 API |

## 环境要求

- JDK 17+
- Maven 3.8+
- RabbitMQ 3.x（本地默认 `localhost:5672`，guest/guest）

## 启动方式

1. 启动 RabbitMQ（Docker 示例）：
   ```bash
   docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3-management
   ```

2. 启动本应用：
   ```bash
   cd inventory-mq
   mvn spring-boot:run
   ```
   应用端口：**8081**

## 配置

`src/main/resources/application.yml`：

```yaml
spring.rabbitmq:
  host: localhost
  port: 5672
  username: guest
  password: guest

inventory.mq:
  exchange: inventory.exchange
  queue: inventory.supplier.queue
  routing-key: inventory.supplier
```

生产环境可通过环境变量覆盖，例如：`SPRING_RABBITMQ_HOST`、`SPRING_RABBITMQ_USERNAME` 等。

## 接口示例

### 1. 模拟供应商推送单条库存（写入 MQ）

```bash
curl -X POST http://localhost:8081/api/inventory/push \
  -H "Content-Type: application/json" \
  -d '{
    "supplierCode": "SUP001",
    "partCode": "P-1001",
    "quantity": 500,
    "warehouseCode": "WH01",
    "updatedAt": "2025-01-29T10:00:00Z",
    "bizId": "SUP001-P-1001-001"
  }'
```

### 2. 批量推送

```bash
curl -X POST http://localhost:8081/api/inventory/push-batch \
  -H "Content-Type: application/json" \
  -d '[
    {"supplierCode":"SUP001","partCode":"P-1001","quantity":500},
    {"supplierCode":"SUP001","partCode":"P-1002","quantity":300}
  ]'
```

### 3. 查询已落库的库存列表（消费者写入后的结果）

```bash
curl http://localhost:8081/api/inventory/list
```

## 数据流

```
供应商系统 / 测试 curl
    → POST /api/inventory/push
    → InventoryProducer.send()
    → RabbitMQ (exchange → queue)
    → InventoryConsumer.onMessage()
    → InventoryStore.upsert()
    → GET /api/inventory/list 可查询
```

## 与 PPM 主系统对接

当前示例使用内存存储。若需对接到 PPM 主系统：

1. **方式一**：在 `InventoryStore.upsert()` 中调用 PPM 主系统提供的 REST API（如 `POST /api/supplier-inventory`），将消息体转成主系统所需格式。
2. **方式二**：本模块与主系统共用数据库，引入 JPA 与主系统同一库，在 `InventoryStore` 中写入 `supplier_inventory` 表。

本模块不依赖 `ppm-backend`，可单独部署、单独扩容。
