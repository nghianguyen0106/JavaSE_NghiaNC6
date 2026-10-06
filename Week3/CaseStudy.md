# Case 1 — Flash Sale: hệ thống “chịu được 1,000 RPS” nhưng sập ở 700 RPS

**Topics:** Performance metrics, load testing, bottleneck analysis.

## **1. Bài toán công ty cần giải quyết**

Công ty chuẩn bị flash sale cho một chiến dịch lớn.

Business requirement:

`- Hệ thống phải phục vụ 1,000 checkout requests/second.
- Checkout p95 dưới 800ms.
- Error rate dưới 1%.
- Không oversell sản phẩm.
- Không tạo duplicate order khi user retry.`

Luồng checkout:

`Client
  ↓
API Gateway
  ↓
Order Service
  ├─ Product Service: check product
  ├─ Inventory Service: reserve stock
  ├─ Payment Service: charge
  └─ Order Database: save order`

---

## **2. Kiến trúc/kỹ thuật hiện tại**

Team đã viết load test đơn giản:

`- 100 virtual users.
- Gọi POST /orders liên tục.
- Chạy 2 phút.
- Average response time khoảng 250ms.
- Error rate gần 0%.`

Team kết luận:

> “Hệ thống ổn, có thể chịu flash sale.”
> 

Tuy nhiên test hiện tại có vấn đề:

`- Chỉ nhìn average latency.
- Không có p95/p99.
- Không theo dõi DB metrics.
- Không dùng realistic ramp-up.
- Không có test data gần production.
- Không mô phỏng Product/Inventory/Payment latency.`

---

## **3. Incident xảy ra**

Ngày flash sale:

| **Load** | **p95 latency** | **Error rate** | **App CPU** | **DB CPU** | **DB connections** |
| --- | --- | --- | --- | --- | --- |
| 300 RPS | 240ms | 0.1% | 45% | 55% | 60/100 |
| 500 RPS | 460ms | 0.5% | 55% | 75% | 85/100 |
| 700 RPS | 2.8s | 8% | 60% | 98% | 100/100 |
| 900 RPS | 7.1s | 25% | 61% | 99% | 100/100 |

Triệu chứng:

`- Application CPU không cao.
- Nhưng checkout request timeout.
- Database connection pool đầy.
- Payment API retry tăng.
- Một số user click lại, tạo thêm traffic.`

---

## **4. Câu hỏi**

`Q1. Vì sao test cũ “average 250ms” không dự đoán được incident này?

Q2. p95/p99 nói cho chúng ta điều gì mà average không nói được?

Q3. Bottleneck có khả năng cao nhất nằm ở đâu?
    Evidence nào hỗ trợ nhận định đó?

Q4. Tại sao tăng thêm application instances có thể không giải quyết được?

Q5. Cần kiểm tra gì ở database trước khi kết luận “scale DB lên”?

Q6. Nếu payment API timeout, retry có thể làm incident tệ hơn thế nào?

Q7. Test plan mới cần thay đổi những gì?`

---

## **5. Hướng xử lý**

`- Average che giấu tail latency; một số user có thể chờ 7 giây dù average nhìn vẫn ổn.
- Evidence cho DB bottleneck:
  + DB CPU 98–99%.
  + Connection pool chạm 100/100.
  + App CPU chỉ 60%, chưa bão hòa.
- Scale application có thể làm DB chết nhanh hơn vì tăng concurrent connections.
- Cần kiểm tra:
  + Slow query log.
  + Query execution plan.
  + Missing index.
  + Lock contention.
  + N+1 query.
  + Connection-pool wait time.
- Retry payment nếu không backoff/idempotent có thể tạo traffic amplification và duplicate charge risk.`

Giải pháp theo thứ tự:

`Immediate:
- Rate limit checkout.
- Giảm connection pool hoặc giới hạn concurrency hợp lý.
- Disable/reduce unsafe retries.
- Queue/buffer non-critical operations nếu phù hợp.

Short-term:
- Tối ưu query/index.
- Fix N+1 query.
- Tune DB pool.
- Cache product data nếu dữ liệu cho phép stale ngắn.

Long-term:
- Tách workload.
- Partition/shard khi data/write volume vượt giới hạn.
- Có load test CI/CD với p95/p99/error threshold.`


# Case 2 — Một API change làm Order Service lỗi production dù tất cả unit test đều pass

**Topics:** Contract testing, consumer-driven contract, contract drift.

## **1. Bài toán công ty cần giải quyết**

Order Service cần gọi Product Service để lấy product information trước khi tạo order.

Expected API:

`GET /api/products/{productId}`

Order Service cần response:

JSON

`{
  "id": "P-100",
  "name": "Mechanical Keyboard",
  "price": 120.50,
  "available": true
}`

Business requirement:

`- Không cho tạo order với product không available.
- Giá trong order cần lấy từ Product Service.
- Product team và Order team deploy độc lập.`

---

## **2. Kiến trúc hiện tại**

Order Service có unit test:

Java

`when(productClient.getProduct("P-100"))
    .thenReturn(new ProductResponse(
        "P-100",
        "Mechanical Keyboard",
        120.50,
        true
    ));`

Test pass.

Product Service team refactor API vì muốn chuẩn hóa naming:

JSON

`{
  "id": "P-100",
  "productName": "Mechanical Keyboard",
  "unitPrice": 120.50
}`

Họ:

`- Đổi `name` thành `productName`.
- Đổi `price` thành `unitPrice`.
- Xóa field `available`.
- Deploy Product Service trước.`

Product Service health check vẫn xanh.

Product Service unit test vẫn xanh.

---

## **3. Incident xảy ra**

Sau deploy:

`Order Service gọi Product Service.
↓
Order Service parse response theo contract cũ.
↓
`available` = null hoặc default false.
↓
Nhiều order bị từ chối sai hoặc lỗi deserialize.
↓
Checkout conversion giảm mạnh.`

---

## **4. Câu hỏi**

`Q1. Vì sao unit test của cả hai service vẫn pass?

Q2. Đây là lỗi code, lỗi deployment hay lỗi communication giữa team?

Q3. Field nào là contract thật sự mà Order Service cần?

Q4. Thay đổi nào là breaking change?
    - Add optional field?
    - Rename field?
    - Change number thành string?
    - Remove field?

Q5. Có phải mọi endpoint đều cần full end-to-end test không?

Q6. Consumer-Driven Contract test sẽ thay đổi flow CI/CD như thế nào?

Q7. Nếu Product Service cần đổi field, migration API an toàn sẽ làm sao?`

---

## **5. Hướng xử lý**

`- Unit test pass vì Order Service mock response cũ; Product Service test implementation mới.
- Không có test nào xác minh provider thật đáp ứng expectation của consumer.
- Đây là contract drift.`

CDC flow:

`1. Order Service viết contract:
   cần id, name, price, available.

2. Contract được publish.

3. Product Service CI chạy provider verification.

4. Nếu Product Service xóa available hoặc đổi name:
   provider verification fail.

5. Deployment bị block hoặc bắt buộc có migration plan.`

API evolution an toàn:

`Version 1:
{
  "id": "...",
  "name": "...",
  "price": 120.50,
  "available": true
}

Version transition:
{
  "id": "...",
  "name": "...",
  "productName": "...",
  "price": 120.50,
  "unitPrice": 120.50,
  "available": true
}

Sau khi tất cả consumer migrate:
- deprecate field cũ;
- remove ở version API mới hoặc sau thời gian công bố.`

Bài học:

> Mock giúp test logic consumer, nhưng không chứng minh provider thật vẫn giữ contract. CDC tạo safety net giữa các team deploy độc lập.


# Case 3 — Inventory chậm 3 giây khiến toàn bộ Order Service “đứng hình”

**Topics:** Chaos engineering, latency injection, timeout, retry, circuit breaker, bulkhead.

## **1. Bài toán công ty cần giải quyết**

Order Service cần reserve inventory trước khi xác nhận order:

`Create Order
    ↓
Reserve Inventory
    ↓
Charge Payment
    ↓
Confirm Order`

Business requirement:

`- Checkout p95 dưới 1 giây.
- Không oversell.
- Nếu Inventory Service có sự cố, Order Service vẫn phải đáp ứng được
  một cách có kiểm soát.
- Không được làm các API unrelated như Get Order bị ảnh hưởng.`

---

## **2. Kiến trúc/kỹ thuật hiện tại**

`Client
  ↓
Order Service
  ├─ synchronous HTTP call → Inventory Service
  ├─ synchronous HTTP call → Payment Service
  └─ Order DB`

Current configuration:

`- HTTP client timeout: 30 seconds.
- Retry: 3 lần ngay lập tức.
- Không có circuit breaker.
- Checkout và Get Order dùng chung application thread pool.
- Không có bulkhead/concurrency limit cho outbound Inventory call.`

Team tin rằng:

> “Inventory rất ổn định, timeout 30 giây sẽ giúp request có cơ hội thành công.”
> 

---

## **3. Incident xảy ra**

Inventory Service bị chậm do database lock.

Chaos simulation/production symptom:

`- Inventory latency tăng từ 80ms lên 3–5 giây.
- Order Service giữ thread chờ Inventory.
- Retry ngay lập tức tạo thêm call đến Inventory.
- Thread pool Order Service bị đầy.
- Get Order API, vốn không cần gọi Inventory, cũng bắt đầu timeout.
- Checkout error rate tăng mạnh.`

---

## **4. Câu hỏi dẫn dắt**

`Q1. 30-second timeout có phải “an toàn” không? Vì sao?

Q2. Retry 3 lần ngay lập tức gây ảnh hưởng gì khi dependency đang chậm?

Q3. Vì sao Get Order API bị ảnh hưởng dù không gọi Inventory?

Q4. Circuit breaker khác retry ở điểm nào?

Q5. Bulkhead giải quyết vấn đề gì?

Q6. Chaos experiment hợp lệ cho case này cần hypothesis, blast radius,
    abort condition như thế nào?

Q7. Nếu Inventory unavailable, checkout API nên trả gì hoặc xử lý ra sao?`