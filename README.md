# Multi-Cache Product Service

Ứng dụng quản lý sản phẩm sử dụng **2 lớp cache**:

* **L1 Cache:** Caffeine (In-Memory)
* **L2 Cache:** Redis (Distributed Cache)

Hỗ trợ chạy nhiều instance và đồng bộ cache theo thời gian thực thông qua **Redis Pub/Sub**.

---

## Kiến trúc

```text
Client
   │
   ▼
┌───────────────┐
│ Spring Boot   │
│ Instance      │
└───────┬───────┘
        │
        ▼
 ┌─────────────┐
 │ L1 Cache    │
 │ Caffeine    │
 └──────┬──────┘
        │ Miss
        ▼
 ┌─────────────┐
 │ L2 Cache    │
 │ Redis       │
 └──────┬──────┘
        │ Miss
        ▼
 ┌─────────────┐
 │ Database    │
 │ H2          │
 └─────────────┘
```

## Multi-Instance Cache Architecture 

                    ┌─────────────────────┐
                    │      Database       │
                    │        (H2)         │
                    └──────────┬──────────┘
                               │
                               │
                    ┌──────────▼──────────┐
                    │      Redis L2       │
                    │   Shared Cache      │
                    └──────────┬──────────┘
                               │
             ┌─────────────────┼─────────────────┐
             │                 │                 │
             │                 │                 │
    ┌────────▼───────┐ ┌───────▼────────┐ ┌──────▼─────────┐
    │   Instance 1   │ │   Instance 2   │ │   Instance 3  │
    │    Port 8080   │ │    Port 8081   │ │    Port 8082  │
    ├────────────────┤ ├────────────────┤ ├────────────────┤
    │ L1 Cache       │ │ L1 Cache       │ │ L1 Cache       │
    │ (Caffeine)     │ │ (Caffeine)     │ │ (Caffeine)     │
    └────────────────┘ └────────────────┘ └────────────────┘
             ▲                 ▲                 ▲
             │                 │                 │
             └─────────────────┼─────────────────┘
                               │
                    ┌──────────▼──────────┐
                    │   Redis Pub/Sub     │
                    │ product-changes     │
                    └─────────────────────┘

Nhiều instance có thể cùng sử dụng Redis để chia sẻ cache và đồng bộ dữ liệu.

---

## Công nghệ sử dụng

| Công nghệ     | Mục đích           |
| ------------- | ------------------ |
| Java 17       | Ngôn ngữ lập trình |
| Spring Boot 3 | Framework          |
| Caffeine      | L1 Cache           |
| Redis         | L2 Cache & Pub/Sub |
| H2 Database   | Database           |
| Maven         | Build Tool         |

---

## Tính năng

* CRUD Product API
* L1 Cache bằng Caffeine
* L2 Cache bằng Redis
* Đồng bộ cache giữa nhiều instance
* Redis Pub/Sub để cập nhật cache realtime
* Monitoring qua Spring Actuator

---

## Yêu cầu

* Java 17+
* Maven 3.9+
* Redis Server

---

## Cài đặt Redis

### Docker

```bash
docker run -d --name redis -p 6379:6379 redis:alpine
```

Kiểm tra:

```bash
redis-cli ping
```

Kết quả:

```text
PONG
```

---

## Build Project

```bash
mvn clean package
```

---

## Chạy ứng dụng

### Chạy 1 instance

```bash
java -jar target/multi-cache-1.0.0.jar
```

### Chạy nhiều instance

Windows:

```powershell
.\run-instances.ps1
```

Linux/Mac:

```bash
./run-instances.sh
```

---

## API

Base URL:

```text
http://localhost:8080/api/products
```

### Tạo sản phẩm

```http
POST /api/products
```

Ví dụ:

```bash
curl -X POST http://localhost:8080/api/products \
-H "Content-Type: application/json" \
-d '{
  "code":"P001",
  "name":"Laptop",
  "description":"Gaming Laptop",
  "price":1500,
  "quantity":5
}'
```

### Lấy sản phẩm

```http
GET /api/products/{id}
```

```bash
curl http://localhost:8080/api/products/1
```

### Cập nhật sản phẩm

```http
PUT /api/products/{id}
```

### Xóa sản phẩm

```http
DELETE /api/products/{id}
```

```bash
curl -X DELETE http://localhost:8080/api/products/1
```

---

## Cache Strategy

### L1 Cache (Caffeine)

* Lưu trong RAM của từng instance
* TTL: 5 phút
* Max Size: 1000 records

### L2 Cache (Redis)

* Dùng chung cho tất cả instance
* TTL: 10 phút
* Lưu dữ liệu dạng JSON

### Đồng bộ Cache

Redis Pub/Sub sử dụng channel:

```text
product-changes
```

Sự kiện hỗ trợ:

* CREATE
* UPDATE
* DELETE

Khi dữ liệu thay đổi, các instance khác sẽ tự động cập nhật hoặc xóa cache tương ứng.

---

## Kiểm thử Multi Instance

Khởi chạy các instance:

```powershell
.\run-instances.ps1
```

Chạy test:

```powershell
.\test-multi-instances.ps1
```

hoặc

```bash
./test-multi-instances.sh
```

---

## Monitoring

### Health Check

```bash
curl http://localhost:8080/actuator/health
```

### Cache Metrics

```bash
curl http://localhost:8080/actuator/metrics/cache.gets
curl http://localhost:8080/actuator/metrics/cache.puts
```

### Redis

```bash
redis-cli
KEYS product_*
DBSIZE
MONITOR
```

---

## Cấu trúc thư mục

```text
src/main/java/com/example/multi_cache
├── cache
├── config
├── controller
├── dto
├── entity
├── messaging
├── repository
└── service
```

---

## Luồng hoạt động

### Đọc dữ liệu

```text
L1 Cache
   ↓ miss
L2 Cache
   ↓ miss
Database
```

### Cập nhật/Xóa dữ liệu

```text
Database
   ↓
L1 Cache
   ↓
L2 Cache
   ↓
Redis Pub/Sub
   ↓
Các instance khác đồng bộ cache
```

---

## License

MIT License
