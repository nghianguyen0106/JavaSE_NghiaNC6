# Observability Design — Order Service

## 1. Logs
- Structured log fields: Sử dụng log định dạng JSON với các trường chuẩn: `timestamp`, `level`, `service`, `traceId`, `spanId`, `correlationId`, `tenantId`, `userId`, `method`, `path`, `statusCode`, `durationMs`, `message`.
- Sensitive data masking: Tích hợp Logback Regex Masking Pattern để tự động che thông tin: Số thẻ tín dụng (`4111-XXXX-XXXX-1111`), mật khẩu, mã CVV, số căn cước công dân.
- Correlation/trace ID strategy: Kế thừa W3C Trace Context (`traceparent`) qua HTTP Headers và Kafka Headers; nạp tự động vào `org.slf4j.MDC` thông qua WebFilter và Kafka Record Interceptor.

---

## 2. Metrics
- HTTP metrics: `http.server.requests` (đo lường Throughput, Status code, Latency percentiles P50, P95, P99).
- JVM metrics: `jvm.memory.used`, `jvm.memory.max`, `jvm.gc.pause` (theo dõi thời gian dừng Stop-The-World của Garbage Collector).
- Database metrics: `hikaricp.connections.active`, `hikaricp.connections.pending`, `hikaricp.connections.timeout.total` (giám sát connection pool saturation).
- Kafka / Outbox / DLQ metrics: `kafka.consumer.fetch.latency`, `kafka.consumer.records.lag`, `outbox.unpublished.records.count`, `dlq.messages.incoming.rate`.
- Circuit breaker metrics: `resilience4j.circuitbreaker.state` (CLOSED, OPEN, HALF_OPEN), `resilience4j.circuitbreaker.failure.rate`.
- Business metrics: `checkout.success.count`, `checkout.failed.count`, `payment.status.unknown.count`, `order.stuck.processing.count`.

---

## 3. Traces
- Trace propagation flow: Client $\rightarrow$ API Gateway $\rightarrow$ Order Service (HTTP) $\rightarrow$ Inventory Service (OpenFeign/HTTP) $\rightarrow$ PostgreSQL (JDBC) $\rightarrow$ Kafka Topic (Messaging) $\rightarrow$ Notification Consumer.
- Sampling policy: Áp dụng Probabilistic Adaptive Sampling: Lấy mẫu $10\%$ tổng lượng traffic thành công (để tối ưu chi phí lưu trữ APM), nhưng luôn lấy mẫu 100% (Force 100% Sampling) đối với các request có status code $\ge 500$ hoặc latency vượt quá ngưỡng SLA ($> 1500\text{ms}$).
- Important spans: `http:POST /api/orders/checkout`, `feign:reserveInventory`, `jdbc:commitCheckoutTransaction`, `kafka:publishOrderConfirmed`.

---

## 4. Dashboard
- Business dashboard: Tỷ lệ checkout thành công theo thời gian thực, tổng doanh thu theo phút, số lượng đơn hàng kẹt ở trạng thái thanh toán.
- API dashboard: RED metrics (Rate, Errors, Duration P50/P95/P99) của từng endpoint, HTTP Status breakdown (2xx, 4xx, 5xx).
- Event-processing dashboard: Kafka Consumer Lag theo từng Consumer Group, Outbox backlog queue size, số lượng message rơi vào DLQ.
- Dependency dashboard: Latency và Error rate của các dịch vụ bên ngoài (Inventory Service, Payment Gateway), trạng thái Circuit Breakers.

---

## 5. Alerts
| Alert Condition | Severity | First-Response Runbook | Owner |
| :--- | :--- | :--- | :--- |
| `ErrorRate5xx > 2%` trong 2 phút | `P1 - CRITICAL` | Kiểm tra log OpenSearch, đối chiếu dependency latency, kích hoạt Circuit Breaker hoặc rollback release gần nhất | Team Core Order |
| `HikariPendingConnections > 10` trong 1 phút | `P1 - CRITICAL` | Kiểm tra slow queries trên Postgres, thu thập Thread Dump kiểm tra connection leak | Team Database Admin |
| `CircuitBreakerOpen == true` cho Inventory | `P2 - WARNING` | Kiểm tra trạng thái sức khỏe của Inventory Service, verify fallback response | Team Order & Inventory |
| `KafkaConsumerLag > 20000` trong 5 phút | `P2 - WARNING` | Scale số lượng consumer pod, kiểm tra bottleneck xử lý của worker | Team Platform Ops |
| `DlqIncomingRate > 0` trong 1 phút | `P1 - CRITICAL` | Mở DLQ Runbook, trích xuất headers bản tin lỗi để phân tích nguyên nhân | Team Core Order |

---

## 6. Performance risks (Top 3 Likely Bottlenecks)
1. Database Connection Pool Exhaustion:
   - *Bằng chứng:* `hikaricp.connections.pending` tăng cao, thread dump hiển thị nhiều thread ở trạng thái `WAITING` tại `HikariPool.getConnection()`.
   - *Mitigation:* Tối ưu hóa thời gian giữ transaction (`@Transactional`), tách các network call bên thứ ba ra ngoài transaction boundary.
2. Cascading Failure do Downstream Service chậm:
   - *Bằng chứng:* Inventory Service phản hồi chậm $3\text{s}$, làm P99 của Order Service tăng vọt, số lượng active thread chạm ngưỡng tối đa.
   - *Mitigation:* Cấu hình TimeLimiter ngắn ($1000\text{ms}$), áp dụng Bulkhead cô lập số lượng request đồng thời tối đa là 50.
3. High Allocation Pressure gây GC Pause liên tục:
   - *Bằng chứng:* Heap usage có biểu đồ răng cưa dốc đứng, `jvm.gc.pause` tăng cao, CPU máy chủ ở mức trung bình ($40\%$).
   - *Mitigation:* Sử dụng Stream processing thay vì load toàn bộ danh sách bản ghi vào memory (`findAll()`), tối ưu kích thước buffer của Kafka Producer.
