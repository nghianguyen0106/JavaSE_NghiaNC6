# 7. Chaos Engineering in Practice
### **Cần học gì**

- Fault injection types:
    - Latency injection.
    - Network packet loss.
    - Network partition.
    - HTTP 5xx injection.
    - Dependency timeout.
    - Kill process/pod.
    - CPU stress.
    - Memory pressure.
    - Database connection failure.
    - Disk full simulation.
- Resilience patterns liên quan:
    - Timeout.
    - Retry with backoff.
    - Circuit breaker.
    - Bulkhead.
    - Fallback.
    - Rate limiting.
    - Queue / asynchronous processing.
- Chaos tools ở mức overview:
    - Chaos Monkey.
    - LitmusChaos.
    - Chaos Mesh.
    - Toxiproxy.
    - Kubernetes fault injection.
    - Mock server fault injection.

### **Cần hiểu đến mức nào**

cần biết mỗi fault không phải để “xem hệ thống có chết không”, mà để kiểm tra một assumption cụ thể.

Ví dụ:

| **Fault** | **Assumption cần kiểm tra** |
| --- | --- |
| Inventory API thêm 2 giây latency | Timeout/circuit breaker có bảo vệ Order Service không? |
| Payment API trả 500 | Retry có gây duplicate charge không? |
| One application instance down | Load balancer có route sang instance khác không? |
| Database connections bị exhaustion | Application fail fast hay treo toàn bộ? |
| Kafka/message broker unavailable | Event có bị mất không, retry/DLQ hoạt động không? |

### **Assignment**

Thiết kế **2 chaos experiments** cho Order Service:

#### **Experiment A — Latency Injection**

`Inject 3-second latency vào Inventory Service.`

#### **Experiment B — Payment Service Returns 500**

`Payment Service trả HTTP 500 trong 20% request trong 5 phút.`

Mentee cần mô tả:

| **Nội dung** | **Experiment A** | **Experiment B** |
| --- | --- | --- |
| Hypothesis |  |  |
| Fault |  |  |
| Blast radius |  |  |
| Expected behavior |  |  |
| Timeout/retry/circuit breaker behavior |  |  |
| Abort condition |  |  |
| Metrics |  |  |
| Rollback |  |  |

### **Sản phẩm cần nộp**

- `07-chaos-experiments.md`
- Hai experiment plans.
- Một sequence diagram cho failure flow.
- Danh sách metrics, logs, alerts bắt buộc.

### **Hoàn thành khi**

- [ ]  Fault injection có mục tiêu cụ thể.
- [ ]  Có guardrail.
- [ ]  Có expected result.
- [ ]  Phân biệt retry và circuit breaker.
- [ ]  Nhận diện được retry có thể làm sự cố tệ hơn.
- [ ]  Có đề xuất DLQ/manual recovery nếu thao tác tài chính thất bại.