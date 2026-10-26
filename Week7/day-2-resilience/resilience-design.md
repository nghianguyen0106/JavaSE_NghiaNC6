# Resilience Architecture: OpenFeign, WebClient, Circuit Breaker & Bulkhead

## 1. Thiết Kế Luồng Gọi Dịch Vụ Có Khả Năng Phục Hồi (Resilient Service Flow)

```
[Order Service: Checkout Request]
        |
        v
+---------------------------------------------------------------------------------+
|                       Resilience4j Layer (Decorated Client)                     |
|                                                                                 |
| 1. Bulkhead (Semaphore / Concurrency Limit: Max 50 concurrent calls)            |
| 2. Rate Limiter (Max 200 calls/sec)                                             |
| 3. Circuit Breaker (Sliding Window 100 calls, Failure Rate Threshold 50%)       |
| 4. Retry Engine (Max 2 attempts, Exponential Backoff + Full Jitter)             |
| 5. TimeLimiter (Read Timeout = 1000ms)                                          |
+---------------------------------------------------------------------------------+
        |
        v (HTTP / REST)
[Inventory Service: POST /api/inventory/reserve]
```

---

## 2. Cấu Hình Khả Năng Chịu Lỗi Chi Tiết (Resilience4j Configuration)

File `application.yml` cho `order-service`:

```yaml
resilience4j:
  # 1. Cấu hình Circuit Breaker
  circuitbreaker:
    instances:
      inventoryService:
        sliding-window-type: COUNT_BASED
        sliding-window-size: 100
        minimum-number-of-calls: 20
        failure-rate-threshold: 50
        slow-call-rate-threshold: 75
        slow-call-duration-threshold: 1000ms
        wait-duration-in-open-state: 10000ms
        permitted-number-of-calls-in-half-open-state: 10
        automatic-transition-from-open-to-half-open-enabled: true
        record-exceptions:
          - org.springframework.web.client.HttpServerErrorException
          - java.util.concurrent.TimeoutException
          - java.io.IOException
        ignore-exceptions:
          - org.springframework.web.client.HttpClientErrorException.BadRequest
          - org.springframework.web.client.HttpClientErrorException.Unauthorized

  # 2. Cấu hình Retry Engine
  retry:
    instances:
      inventoryService:
        max-attempts: 2
        wait-duration: 500ms
        enable-exponential-backoff: true
        exponential-backoff-multiplier: 2.0
        retry-exceptions:
          - java.net.ConnectException
          - java.net.SocketTimeoutException
          - org.springframework.web.client.HttpServerErrorException.ServiceUnavailable
        ignore-exceptions:
          - org.springframework.web.client.HttpClientErrorException

  # 3. Cấu hình Bulkhead (Cô lập tài nguyên)
  bulkhead:
    instances:
      inventoryService:
        max-concurrent-calls: 50
        max-wait-duration: 20ms
```

---

## 3. Phân Loại Ngoại Lệ: Transient Errors vs Non-Retryable Errors

| Phân loại | Mã lỗi / Exception Types | Bản chất kỹ thuật | Hành vi xử lý (Action) |
| :--- | :--- | :--- | :--- |
| **Transient Errors (Được phép Retry)** | `503 Service Unavailable`<br>`504 Gateway Timeout`<br>`ConnectException`<br>`SocketTimeoutException` | Lỗi mạng tạm thời, quá tải tức thời hoặc instance đích đang restart. Khả năng thành công cao khi thử lại sau một khoảng thời gian chờ (Backoff). | **Thực thi Retry:** Áp dụng Exponential Backoff ($500\text{ms} \rightarrow 1000\text{ms}$) kết hợp Random Jitter để tránh làm nghẽn thêm server đích. |
| **Non-Retryable Errors (CẤM Retry)** | `400 Bad Request`<br>`401 Unauthorized`<br>`403 Forbidden`<br>`404 Not Found`<br>`409 Conflict`<br>`422 Unprocessable Entity` | Yêu cầu không hợp lệ về mặt cấu trúc (Schema), vi phạm ràng buộc nghiệp vụ (ví dụ: Hết tồn kho), hoặc sai lệch quyền. Gửi lại request sẽ tiếp tục thất bại $100\%$. | **Fail-Fast ngay lập tức:** Trả lỗi về cho client, không thực hiện bất kỳ lệnh retry nào. |

---

## 4. Nguyên Tắc An Toàn Cho Tác Vụ Thanh Toán (Payment Retry Safety)

```
[Order Service] -----------------------------------------------> [Payment Service]
                 HTTP POST /api/payments/charge
                 Header: X-Idempotency-Key: "ord-8891-tx"
```

1. **Nguy cơ Double Charge:** Nếu Order Service gửi lệnh trừ tiền sang Payment Service, Payment Gateway đã trừ tiền trong thẻ của khách hàng nhưng gói tin phản hồi HTTP 200 bị drop trên đường mạng $\rightarrow$ Order Service bị SocketTimeout. Nếu Order Service retry mù quáng một request thanh toán mới, khách hàng sẽ bị trừ tiền 2 lần.
2. **Giải pháp bắt buộc (Idempotency Key):**
   - Mọi request thanh toán bắt buộc sinh một khóa định danh bất biến: `X-Idempotency-Key: {orderId}:{paymentAttempt}`.
   - Payment Service sử dụng Redis / Database lưu trạng thái khóa này trong Atomic Transaction (`SET key value NX EX 3600`).
   - Nếu nhận được request có cùng `Idempotency-Key` đang xử lý hoặc đã xử lý thành công, Payment Service lập tức trả về kết quả cũ mà không thực hiện trừ tiền thẻ lần 2.

---

## 5. Nguyên Lý Bulkhead Isolation

- **Mục tiêu:** Sự cố chậm hoặc lỗi của một dependency không được phép làm tê liệt các chức năng độc lập khác trong cùng ứng dụng.
- **Kịch bản thực tế:** `Inventory Service` bị treo 5 giây do nghẽn DB.
  - **Nếu không có Bulkhead:** Toàn bộ 200 thread của Tomcat container bị chiếm dụng để chờ kết nối tới Inventory Service $\rightarrow$ Khách hàng gọi API `GET /orders/{id}` (chỉ đọc đơn hàng từ DB nội bộ) cũng bị timeout vì không còn thread trống.
  - **Khi có Bulkhead (Semaphore max 50):** Chỉ tối đa 50 requests gọi sang Inventory được giữ thread. 150 worker threads còn lại của Tomcat vẫn phục vụ bình thường các API khác như `GET /orders/{id}` hoặc `POST /cart`.
