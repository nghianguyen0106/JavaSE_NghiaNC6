# BỘ ĐỀ 3 BUỔI TECHNICAL AUDIT DỰ ÁN THỰC TẾ (WEEK 1 - WEEK 2 - WEEK 3)
**Dự án đối tượng:** FlashRetail Platform (Hệ thống Thương Mại Điện Tử & Flash Sale phân tán)  
**Hội đồng Audit:** Senior Software Architect & Technical Review Board  
**Đối tượng tham gia:** Senior Software Engineer / Kỹ sư phần mềm (Định hướng SE Dev 3)

---

# 📋 DANH MỤC CÁC BUỔI AUDIT

1. **Buổi Audit 1 (Week 1):** Kiến trúc mã nguồn, Mô hình hóa Core Domain (DDD), Clean Architecture & Xử lý Giao dịch phân tán (Saga).
2. **Buổi Audit 2 (Week 2):** Hệ thống phân tán, Đánh đổi CAP/PACELC, Tối ưu hóa Database quy mô lớn (Replication Lag, Sharding & Noisy Neighbor).
3. **Buổi Audit 3 (Week 3):** Phân tích nút thắt hiệu năng (Performance Bottleneck), Kiểm thử hợp đồng API (Pact CDC) & Độ kiên cường hệ thống (Chaos Engineering).

---

# 🏢 BUỔI AUDIT 1: KIẾN TRÚC MÃ NGUỒN & GIAO DỊCH PHÂN TÁN (WEEK 1)

### 1. Bối cảnh dự án & Triệu chứng sự cố
Dự án `order-service` của FlashRetail được phát triển gấp rút trong 3 tháng để kịp tiến độ MVP. Sau khi ra mắt, hệ thống bắt đầu bộc lộ các vấn đề nghiêm trọng:
*   **Sự cố mất tiền / Hàng ảo:** Có trường hợp tài khoản khách hàng bị trừ tiền qua VNPay nhưng trong cơ sở dữ liệu không có đơn hàng nào được tạo (do phía sau bị lỗi DB hoặc hết hàng trong kho).
*   **Trừ tiền 2 lần:** Khi mạng chập chờn khách bấm thanh toán nhiều lần, tài khoản bị trừ tiền lặp lại.
*   **Codebase "God Class":** Mỗi lần thêm một cổng thanh toán mới (như Momo, Apple Pay) hay sửa nội dung email thông báo, các lập trình viên đều phải sửa trực tiếp trên file `OrderService.java` dài hơn 1500 dòng, dẫn đến lỗi hồi quy (Regression bugs) liên tục.

---

### 2. Dữ liệu mã nguồn cần Audit (Hiện trạng thực tế)

Đoạn code sau đây đang chạy trên Production của `OrderService`:

```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    @Autowired
    private OrderService orderService;

    // Rò rỉ DTO / Entity và không validate chặt chẽ
    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(@RequestBody OrderEntity order, @RequestParam String paymentMethod) {
        orderService.processOrder(order, paymentMethod);
        return ResponseEntity.ok("Success");
    }
}

@Service
public class OrderService {
    @Autowired
    private OrderJpaRepository orderRepo;
    @Autowired
    private JavaMailSender mailSender;

    // TỬ HUYỆT 1: Dùng @Transactional bọc cả lời gọi HTTP mạng bên thứ ba
    @Transactional
    public void processOrder(OrderEntity order, String paymentMethod) {
        // TỬ HUYỆT 2: Rò rỉ Invariants ra ngoài, Entity rỗng tuếch (Anemic Domain Model)
        if (order.getItems() == null || order.getItems().isEmpty()) {
            throw new RuntimeException("Giỏ hàng rỗng!");
        }

        // Lưu đơn hàng tạm vào DB
        order.setStatus("PENDING");
        orderRepo.save(order);

        // TỬ HUYỆT 3: Vi phạm OCP nghiêm trọng với chuỗi if-else
        boolean paymentSuccess = false;
        if ("VNPAY".equalsIgnoreCase(paymentMethod)) {
            paymentSuccess = callVnPayApi(order.getTotalAmount());
        } else if ("MOMO".equalsIgnoreCase(paymentMethod)) {
            paymentSuccess = callMomoApi(order.getTotalAmount());
        } else if ("ZALOPAY".equalsIgnoreCase(paymentMethod)) {
            paymentSuccess = callZaloPayApi(order.getTotalAmount());
        }

        // TỬ HUYỆT 4: Nếu trừ tiền xong mà lưu DB lỗi -> Mất tiền của khách!
        if (paymentSuccess) {
            order.setStatus("PAID");
            orderRepo.save(order);

            // TỬ HUYỆT 5: Vi phạm SRP - Service đơn hàng kiêm luôn gửi mail Presentation
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(order.getCustomerEmail());
            message.setSubject("Đặt hàng thành công");
            message.setText("Đơn hàng " + order.getId() + " của bạn đã được thanh toán!");
            mailSender.send(message);
        } else {
            order.setStatus("FAILED");
            orderRepo.save(order);
            throw new RuntimeException("Thanh toán thất bại!");
        }
    }

    private boolean callVnPayApi(BigDecimal amount) {
        // Gọi HTTP POST sang cổng VNPay (mất từ 1s - 5s)
        return true;
    }
    // ... callMomoApi, callZaloPayApi ...
}
```

---

### 3. Bộ câu hỏi chất vấn của Hội đồng Audit (5 Câu hỏi)

*   **Câu hỏi 1 (SOLID Analysis):** Hãy chỉ ra ít nhất 3 vi phạm các nguyên tắc SOLID trong đoạn code trên. Phân tích cụ thể hậu quả vận hành khi hệ thống tích hợp thêm 5 cổng thanh toán quốc tế mới.
*   **Câu hỏi 2 (Transaction & Concurrency Pitfall):** Tại sao việc đặt annotation `@Transactional` trên phương thức vừa gọi Database vừa gọi HTTP API bên thứ ba (`callVnPayApi`) lại là một "án tử" về mặt hiệu năng và tài nguyên cơ sở dữ liệu?
*   **Câu hỏi 3 (Clean & Hexagonal Architecture):** Hãy vẽ sơ đồ các tầng (Layers) và thiết kế lại module này theo chuẩn Hexagonal Architecture. Chỉ rõ: Đâu là Inbound Port, Outbound Port, Inbound Adapter, Outbound Adapter? Tại sao Domain Model không được chứa annotation `@Entity` của Jakarta/JPA?
*   **Câu hỏi 4 (Distributed Transactions & Saga):** Giả sử việc đặt hàng gồm 3 bước phân tán: (1) Tạo đơn ở Order DB, (2) Trừ tiền ở Payment Service, (3) Giữ hàng ở Inventory Service. Nếu bước (3) thất bại do hết hàng, luồng xử lý giao dịch bù trừ (Compensating Transaction) của Saga Orchestration phải diễn ra như thế nào? Cần cơ chế gì để đảm bảo an toàn khi mạng bị mất gói tin?
*   **Câu hỏi 5 (CQRS Boundary):** Đội ngũ đề xuất áp dụng ngay CQRS và Event Sourcing cho toàn bộ các chức năng của `order-service`. Dưới góc nhìn của một Senior Architect, bạn đồng ý hay phản đối? Khi nào thì nên dùng CQRS và khi nào chỉ nên dùng CRUD truyền thống?

---

### 4. Barem chấm điểm & Tiêu chuẩn đánh giá (Rubric)

| Mức độ | Tiêu chuẩn đánh giá của Hội đồng |
| :--- | :--- |
| **Dưới chuẩn (Junior)** | Chỉ nói được SRP là "class làm nhiều việc", OCP là "có if-else". Không giải thích được bản chất connection pool bị giữ trong `@Transactional`. Không hiểu tại sao Domain phải là POJO. |
| **Đạt chuẩn (SE Dev 3)** | Nhận diện chính xác vi phạm SRP (lý do thay đổi gắn với Actor), OCP (vi phạm tính đóng/mở), DIP (dính chặt vào JPA/MailSender). Phân tích được việc `@Transactional` giữ Database Connection trong suốt thời gian chờ HTTP call (1-5s) dẫn đến cạn kiệt Connection Pool. Thiết kế được Inbound/Outbound Port và vẽ được luồng Saga Orchestrator LIFO. |
| **Xuất sắc (Tech Lead/Architect)** | Đề xuất giải pháp loại bỏ `if-else` bằng Spring Factory Map / Strategy Pattern. Thiết kế Aggregate Root bảo vệ Invariant. Trình bày được cơ chế Idempotency Key chống duplicate charge và cơ chế Outbox Pattern kết hợp Saga để đảm bảo At-least-once Delivery. |

---

### 5. Lời giải & Khuyến nghị kiến trúc chuẩn của Hội đồng

1.  **Khắc phục vi phạm SOLID & Clean Architecture:**
    *   Tách `PaymentGateway` thành Outbound Port (Interface). Các cổng cụ thể (`VnPayAdapter`, `MomoAdapter`) là Outbound Adapters. Sử dụng Spring Map Injection: `Map<String, PaymentGateway>` để triệt tiêu toàn bộ `if-else` (Tuân thủ triệt để OCP).
    *   Tách việc gửi email ra khỏi Use Case đặt hàng bằng cách bắn **Domain Event** (`OrderPlacedEvent`), một Notification Consumer bất đồng bộ sẽ lắng nghe và gửi mail (Đảm bảo SRP).
    *   Tách biệt Core Domain POJO (`Order`, `Money`) hoàn toàn khỏi JPA Entity. JPA Entity chỉ là chi tiết ở tầng Persistence Adapter.
2.  **Triệt tiêu nút thắt `@Transactional`:**
    *   Tuyệt đối **KHÔNG bao giờ bọc HTTP call bên ngoài vào trong `@Transactional`**. Transaction DB chỉ mở trong vài mili-giây để ghi trạng thái `PENDING`, sau đó commit ngay để nhả DB Connection về pool trước khi thực hiện cuộc gọi HTTP mạng.
3.  **Thiết kế Saga Orchestration cho bài toán 3 bước:**
    *   Thực hiện theo chuỗi: (1) Local Tx: `Order PENDING` $\rightarrow$ (2) HTTP: `Inventory reserveStock` $\rightarrow$ (3) HTTP: `Payment charge`.
    *   Nếu bước (3) thất bại: Kích hoạt Compensating Action theo thứ tự LIFO: Gọi `Inventory releaseStock` để nhả kho $\rightarrow$ Cập nhật `Order FAILED`.
    *   Mọi API tham gia Saga đều bắt buộc truyền `Idempotency-Key` (UUID của Order) để xử lý an toàn khi retry.

---

# 🌐 BUỔI AUDIT 2: HỆ THỐNG PHÂN TÁN, REPLICATION & SHARDING (WEEK 2)

### 1. Bối cảnh dự án & Triệu chứng sự cố
Sau khi tái cấu trúc mã nguồn, FlashRetail bước vào sự kiện khuyến mãi lớn. Cơ sở dữ liệu bắt đầu phình to với hơn 200 triệu dòng đơn hàng. Đội Infra đã thiết lập:
*   Mô hình DB: 1 Primary MySQL (xử lý Write) và 3 Read Replicas (xử lý Read) với cơ chế Asynchronous Replication.
*   Cơ chế Sharding: Chia 4 Shards theo thuật toán Hash Sharding tĩnh: `shard_id = hash(order_id) % 4`.

Vào ngày cao điểm, 3 sự cố liên tiếp bùng nổ:
1.  **Sự cố 1 (Xác thực & Stale Read):** Khách hàng đổi mật khẩu trên trang tài khoản cá nhân. Ngay sau khi trang web thông báo *"Đổi mật khẩu thành công"*, khách bấm Đăng nhập lại thì liên tục nhận thông báo lỗi *"Tên đăng nhập hoặc mật khẩu không đúng"*. Khoảng 3 giây sau thử lại thì đăng nhập được bình thường.
2.  **Sự cố 2 (Sập toàn sàn vì báo cáo):** Lúc 10:00 sáng, Giám đốc Tài chính mở trang Dashboard báo cáo tổng doanh thu và top 100 sản phẩm bán chạy trong 6 tháng. Query này quét toàn bộ 200 triệu dòng trên Read Replica số 1. Ngay sau đó, hàng ngàn khách hàng duyệt xem sản phẩm bị lỗi Timeout 504 Gateway Time-out.
3.  **Sự cố 3 (Nút thắt Shop VIP - Noisy Neighbor):** Gian hàng chính hãng "Apple Authorized Store" chạy Flash Sale độc quyền giảm giá iPhone 15. Do `order_id` sinh ngẫu nhiên băm đều, nhưng toàn bộ truy vấn xem đơn hàng và tồn kho của gian hàng này lại dồn tải gấp 100 lần bình thường vào Shard số 2, khiến CPU Shard 2 chạm 100%, làm sập luôn các đơn hàng của hàng ngàn shop nhỏ vô tội khác nằm chung shard.

---

### 2. Dữ liệu kỹ thuật cung cấp để Audit

**Cấu hình Routing hiện tại của Application:**
```java
public class DynamicDataSourceRouter extends AbstractRoutingDataSource {
    @Override
    protected Object determineCurrentLookupKey() {
        // Áp dụng quy tắc ngây thơ: Cứ query SELECT là đẩy sang Read Replica theo Round-Robin
        if (TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
            return getNextReadReplica(); // Luân phiên Replica 1, 2, 3
        }
        return "PRIMARY_DB";
    }
}
```

**MySQL Slow Query Log trích xuất từ Read Replica 1 (Sự cố 2):**
```sql
# Query_time: 42.158201  Lock_time: 0.000120 Rows_sent: 100  Rows_examined: 185,420,900
SELECT p.category_id, SUM(o.total_amount), COUNT(o.id)
FROM orders o 
JOIN order_items oi ON o.id = oi.order_id
JOIN products p ON oi.product_id = p.id
WHERE o.created_at >= '2026-04-01 00:00:00'
GROUP BY p.category_id
ORDER BY SUM(o.total_amount) DESC LIMIT 100;
```

---

### 3. Bộ câu hỏi chất vấn của Hội đồng Audit (5 Câu hỏi)

*   **Câu hỏi 1 (CAP/PACELC & Consistency Model):** Phân tích sự cố 1 dưới góc nhìn Định lý PACELC. Tại sao việc người dùng đổi mật khẩu xong đăng nhập thất bại lại vi phạm mô hình **Read-Your-Writes Consistency**? Hãy đề xuất ít nhất 2 giải pháp kiến trúc để chấm dứt hoàn toàn tình trạng này mà không biến Primary DB thành nút thắt cổ chai.
*   **Câu hỏi 2 (Tách biệt OLTP vs OLAP):** Tại sao việc đẩy các câu truy vấn báo cáo nặng sang Read Replica vẫn gây sập hệ thống (Sự cố 2)? Trình bày kiến trúc cách ly triệt để tải phân tích (OLAP) khỏi cơ sở dữ liệu giao dịch (OLTP) bằng giải pháp CDC (Change Data Capture) và Columnar Database.
*   **Câu hỏi 3 (Replication Strategy Trade-offs):** So sánh 3 chiến lược sao chép dữ liệu: *Synchronous, Asynchronous, và Semi-synchronous Replication*. Nếu Primary DB bị cháy phần cứng đột ngột, chiến lược Asynchronous sẽ gây ra hậu quả gì cho dữ liệu ví tiền của khách? Trong trường hợp của FlashRetail, bạn sẽ chọn chiến lược nào cho module Thanh toán và module Xem sản phẩm?
*   **Câu hỏi 4 (Đánh giá Shard Key & Noisy Neighbor):** Phân tích ưu và nhược điểm của việc chọn `order_id` làm Shard Key. Tại sao thuật toán Hash Sharding tĩnh lại hoàn toàn bất lực trước hiện tượng Hot Tenant / Noisy Neighbor ở Sự cố 3?
*   **Câu hỏi 5 (Directory-based Sharding & Zero-downtime Migration):** Hãy thiết kế giải pháp Directory-based Sharding sử dụng Redis Lookup Table để cô lập Shop VIP vào một **Dedicated Shard** riêng biệt. Trình bày chi tiết quy trình 4 bước di chuyển dữ liệu của Shop VIP sang Shard mới mà không cần dừng hệ thống (Zero-downtime Migration).

---

### 4. Barem chấm điểm & Tiêu chuẩn đánh giá (Rubric)

| Mức độ | Tiêu chuẩn đánh giá của Hội đồng |
| :--- | :--- |
| **Dưới chuẩn (Junior)** | Chỉ đổ lỗi do mạng chậm gây lỗi đổi pass. Đề xuất giải pháp tăng RAM server hoặc bảo kế toán chạy báo cáo ban đêm. Không phân biệt được OLTP và OLAP. Không hiểu khái niệm Hot Shard. |
| **Đạt chuẩn (SE Dev 3)** | Nhận diện được Replication Lag vi phạm Read-Your-Writes. Đề xuất cơ chế Time-window routing (ghim đọc Primary trong 3s sau write) hoặc Return-on-Write token. Phân tích được nhược điểm của việc dùng Replica cho reporting nặng (I/O saturation, buffer pool thrashing). Đề xuất giải pháp CDC (Debezium + Kafka) sang ClickHouse. Chỉ ra được điểm yếu của Hash Sharding tĩnh trước Hot Tenant. |
| **Xuất sắc (Tech Lead/Architect)** | Vẽ được sơ đồ kiến trúc Directory-based Sharding với bộ nhớ đệm Redis 2 tầng (Local Cache + Distributed Cache). Trình bày hoàn chỉnh quy trình Zero-downtime Migration 4 bước (Snapshot $\rightarrow$ CDC Stream $\rightarrow$ Atomic Cutover $\rightarrow$ Clean up). Đề xuất sử dụng Semi-synchronous replication cho phân hệ thanh toán để triệt tiêu Data-loss Window. |

---

### 5. Lời giải & Khuyến nghị kiến trúc chuẩn của Hội đồng

1.  **Xử lý sự cố Read-Your-Writes (Sự cố 1):**
    *   *Nguyên nhân:* Do Asynchronous Replication có độ trễ (Replication Lag 1-3s). Request Login đọc từ Replica chưa có dữ liệu mới.
    *   *Giải pháp:* 
        *   Quy tắc 1: 100% truy vấn xác thực tài khoản nhạy cảm (Authentication/Security) bắt buộc cấu hình đọc trực tiếp từ Primary DB.
        *   Quy tắc 2 (Time-window Routing): Sử dụng Cookie/Header ghi nhận mốc thời gian write gần nhất. Trong vòng 5 giây kể từ khi có thao tác Write, mọi thao tác Read của User đó được định tuyến về Primary DB.
2.  **Cách ly tuyệt đối OLTP và OLAP (Sự cố 2):**
    *   *Nguyên nhân:* Query quét 185 triệu dòng làm tràn InnoDB Buffer Pool, chiếm sạch disk I/O và CPU của Replica, làm các query SELECT phục vụ khách bị nghẽn (Queue saturation).
    *   *Giải pháp:* Tuyệt đối không chạy query phân tích trên DB quan hệ OLTP. Sử dụng **Debezium bắt Binlog của MySQL $\rightarrow$ Kafka $\rightarrow$ Nạp vào ClickHouse (Columnar DB)**. Query báo cáo chạy trên ClickHouse chỉ mất chưa đầy 300ms mà không tốn 1% CPU của hệ thống bán hàng.
3.  **Khắc phục Noisy Neighbor & Zero-downtime Migration (Sự cố 3):**
    *   Chuyển đổi từ Hash Sharding sang **Directory-based Sharding**.
    *   Lưu bảng ánh xạ trên Redis: `tenant_id:VIP_APPLE -> shard_id:DEDICATED_05`. Với các shop thông thường: `tenant_id:SHOP_123 -> shard_id:SHARED_01`.
    *   Quy trình Zero-downtime Re-sharding 4 bước:
        1.  *Bước 1 (Snapshot):* Dump dữ liệu lịch sử của Apple Store từ Shard 2 sang Shard 5.
        2.  *Bước 2 (CDC Sync):* Kích hoạt CDC Debezium lắng nghe các thay đổi phát sinh trên Shard 2 và ghi bù vào Shard 5 theo thời gian thực cho đến khi Replication Lag về 0.
        3.  *Bước 3 (Atomic Cutover):* Cập nhật Router trên Redis: Chuyển hướng `VIP_APPLE` sang Shard 5. Thao tác này hoàn tất trong < 1ms, khách hàng không hề cảm nhận thấy gián đoạn.
        4.  *Bước 4 (Cleanup):* Sau 48h kiểm tra ổn định, tiến hành xóa dữ liệu cũ của Apple Store trên Shard 2.

---

# ⚡ BUỔI AUDIT 3: PERFORMANCE, CONTRACT TESTING & CHAOS RESILIENCE (WEEK 3)

### 1. Bối cảnh dự án & Triệu chứng sự cố
FlashRetail chuẩn bị cho chiến dịch Flash Sale lớn nhất năm "12.12 Siêu Tiệc Mua Sắm". Nhằm đảm bảo hệ thống không bị sập, đội ngũ kỹ sư đã thực hiện một loạt các hoạt động chuẩn bị:
1.  **Giai đoạn Load Testing:** Đội dev dùng script kiểm thử gọi `POST /api/orders` với 100 Virtual Users trong 2 phút. Báo cáo ghi nhận: *"Average Response Time = 240ms, Error Rate = 0% $\rightarrow$ Hệ thống hoàn toàn sẵn sàng cho 1,000 RPS"*. Tuy nhiên, khi mở diễn tập tải ở 700 RPS, p95 latency vọt lên 2.8 giây, DB Connection Pool chạm 100/100, App CPU chỉ 60% trong khi DB CPU chạm 98%. Đội ngũ xử lý tình huống bằng cách tăng gấp 3 lần số lượng App Pods, dẫn đến Database sập ngay lập tức!
2.  **Giai đoạn Release (Contract Drift):** Đội Product Service triển khai bản cập nhật chuẩn hóa tên trường JSON (`name` $\rightarrow$ `productName`, `price` $\rightarrow$ `unitPrice`, xóa bỏ trường `available`). Tất cả Unit Test của cả 2 team đều xanh 100%. Tuy nhiên, ngay khi deploy lên môi trường Staging, Order Service không thể tạo được đơn hàng do lỗi parse JSON, conversion tụt dốc không phanh.
3.  **Giai đoạn Vận hành (Cascading Failure):** Dịch vụ Inventory Service bị khóa bảng (Database Lock) khiến thời gian phản hồi tăng từ 80ms lên 4.5 giây. Do cấu hình HTTP Client có Timeout 30 giây và Retry 3 lần tức thì, toàn bộ Thread Pool của Order Service bị treo cứng. Khách hàng truy cập vào xem danh sách đơn hàng đã mua (`GET /api/orders`) cũng bị lỗi Timeout 504, dù chức năng này hoàn toàn không đụng đến Inventory!

---

### 2. Dữ liệu kỹ thuật & Đo lường cung cấp để Audit

**Bảng số liệu Load Test diễn tập thực tế:**
| Tải mô phỏng | Latency Average | p95 Latency | p99 Latency | Error Rate | App CPU | DB CPU | DB Conn Pool |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **300 RPS** | 120ms | 240ms | 410ms | 0.05% | 40% | 50% | 55/100 |
| **500 RPS** | 180ms | 460ms | 920ms | 0.40% | 52% | 75% | 85/100 |
| **700 RPS** | 450ms | 2,800ms | 5,400ms | 8.20% | 60% | 98% | 100/100 |
| **900 RPS** | 1,200ms | 7,100ms | 11,000ms | 26.5% | 61% | 99% | 100/100 |

**File cấu hình HTTP Client & Resilience hiện tại của Order Service:**
```yaml
# application.yml của Order Service
external-services:
  inventory-service:
    url: "http://inventory-service/api"
    timeout: 30000        # TỬ HUYỆT 1: Timeout 30 giây
    retry:
      max-attempts: 3     # TỬ HUYỆT 2: Retry 3 lần
      backoff: 0          # TỬ HUYỆT 3: Retry ngay lập tức, không delay
server:
  tomcat:
    threads:
      max: 200            # Cả Checkout và View Order dùng chung 200 worker threads
```

---

### 3. Bộ câu hỏi chất vấn của Hội đồng Audit (5 Câu hỏi)

*   **Câu hỏi 1 (The Fallacy of Average & Resource Correlation):** 
    *   Tại sao kết quả load test cũ *"Average 240ms"* lại là một "lời nói dối"? Giải thích ý nghĩa của p95 và p99 đối với trải nghiệm khách hàng trong sự kiện Flash Sale.
    *   Dựa vào bảng số liệu trên, hãy chỉ ra vị trí chính xác của Nút thắt cổ chai (Bottleneck). Tại sao việc scale-out tăng số lượng App Pods trong trường hợp này lại là hành động "tự sát"? Cần kiểm tra những gì ở cơ sở dữ liệu trước khi kết luận "cần nâng cấp phần cứng DB"?
*   **Câu hỏi 2 (Realistic Load Test Design):** Hãy viết một đoạn script k6 hoàn chỉnh mô phỏng đúng kịch bản Flash Sale thực tế. Yêu cầu kịch bản phải có: Giai đoạn Ramp-up tăng tải dần dần, thiết lập Thresholds chuẩn cho p95 (< 800ms) và Error Rate (< 1%), và sử dụng dữ liệu kiểm thử (Test Data) sát thực tế.
*   **Câu hỏi 3 (Consumer-Driven Contract Testing):** 
    *   Tại sao 100% Unit Test kết hợp Mock (`Mockito.when`) của cả 2 đội Product và Order đều pass nhưng hệ thống vẫn gãy trên môi trường tích hợp?
    *   Trình bày luồng hoạt động của **Consumer-Driven Contract (CDC)** sử dụng Pact trong pipeline CI/CD. Nếu Product Team bắt buộc phải thay đổi cấu trúc API, hãy đề xuất quy trình API Evolution an toàn (Backward Compatibility) theo tiêu chuẩn sản xuất.
*   **Câu hỏi 4 (Audit Bộ tứ phòng vệ - Cascading Failure Analysis):** 
    *   Hãy phân tích 3 sai lầm chí mạng trong file `application.yml` ở trên đã trực tiếp dẫn đến việc API `GET /api/orders` bị sập theo Inventory Service.
    *   Cấu hình lại toàn diện theo **Bộ tứ phòng vệ**: Timeout hợp lý, Retry với Exponential Backoff & Jitter, Circuit Breaker (Resilience4j) và Bulkhead Isolation (Cách ly Thread Pool).
*   **Câu hỏi 5 (Thiết kế Kịch bản Chaos Engineering Thực chiến):** Hãy thiết kế một bản kế hoạch Thử nghiệm Chaos (Chaos Experiment Plan) hoàn chỉnh nhằm chứng minh rằng: *"Hệ thống có khả năng kiên cường chịu đựng khi Inventory Service bị chập chờn hoặc độ trễ tăng đột biến lên 3 giây"*. Bản kế hoạch phải có đầy đủ: *Hypothesis, Blast Radius, Tooling (Chaos Mesh/Toxiproxy), Abort Condition, và Metrics đánh giá*.

---

### 4. Barem chấm điểm & Tiêu chuẩn đánh giá (Rubric)

| Mức độ | Tiêu chuẩn đánh giá của Hội đồng |
| :--- | :--- |
| **Dưới chuẩn (Junior)** | Chỉ nhìn Average latency. Nghĩ rằng cứ timeout dài là an toàn cho request. Nghĩ rằng retry càng nhiều thì tỷ lệ thành công càng cao. Không biết Contract Drift là gì. Không hiểu nguyên lý Bulkhead. |
| **Đạt chuẩn (SE Dev 3)** | Giải thích tường tận p95/p99 che giấu tail latency. Chỉ ra correlation giữa DB CPU 98% và Conn Pool 100/100, cảnh báo việc scale-out App Pod sẽ làm kiệt quệ DB connection. Viết được k6 script có Ramp-up và Thresholds. Hiểu rõ Pact CDC chặn lỗi trên CI/CD của Provider. Cấu hình được Circuit Breaker và Bulkhead cách ly tài nguyên. |
| **Xuất sắc (Tech Lead/Architect)** | Đề xuất giải pháp kiểm tra Lock Contention / N+1 Query / DB Pool Wait Time trước khi scale DB. Trình bày trọn vẹn chiến lược API Evolution (Thêm mới song song $\rightarrow$ Deprecate $\rightarrow$ Xóa). Thiết kế kịch bản Chaos chuyên nghiệp với Abort Condition bảo vệ SLO và Exponential Backoff kết hợp Jitter chống Thundering Herd. |

---

### 5. Lời giải & Khuyến nghị kiến trúc chuẩn của Hội đồng

1.  **Phân tích Nút thắt & Sai lầm Scale-out:**
    *   *Bottleneck:* Nằm 100% tại **Database**. Bằng chứng: Ở 700 RPS, App CPU chỉ 60% (vẫn còn dư tài nguyên), nhưng DB CPU chạm 98% và Connection Pool kịch trần 100/100.
    *   *Sai lầm Scale-out:* Mỗi App Pod mở một connection pool (ví dụ: 50 connections). Khi tăng từ 3 lên 10 App Pods $\rightarrow$ Tổng số connection yêu cầu nhảy từ 150 lên 500 connections $\rightarrow$ Vượt quá `max_connections` của MySQL, gây tranh chấp CPU, tăng context switching và làm DB sập ngay lập tức.
    *   *Checklist kiểm tra DB:* Bật Slow Query Log, kiểm tra thiếu Index, kiểm tra Row Lock Contention do câu lệnh `SELECT ... FOR UPDATE` giữ lock quá lâu, và kiểm tra N+1 queries.
2.  **Script Load Test k6 Chuẩn Production:**
```javascript
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  // 1. Kịch bản Ramp-up tải thực tế
  stages: [
    { duration: '1m', target: 200 }, // Khởi động lên 200 users
    { duration: '2m', target: 700 }, // Tăng vọt lên đỉnh Flash Sale 700 users
    { duration: '1m', target: 0 },   // Hạ tải
  ],
  // 2. Thiết lập ngưỡng chặn SLO bắt buộc
  thresholds: {
    'http_req_duration': ['p(95)<800', 'p(99)<1500'], // 95% request phải dưới 800ms
    'http_req_failed': ['rate<0.01'],                 // Tỷ lệ lỗi dưới 1%
  },
};

export default function () {
  const payload = JSON.stringify({
    customerId: `CUST_${Math.floor(Math.random() * 10000)}`, // Dữ liệu ngẫu nhiên sát thực tế
    productId: 'PROD_FLASH_99',
    quantity: 1,
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
      'Idempotency-Key': `IDEMP_${__VU}_${__ITER}`, // Unique Idempotency Key
    },
  };

  const res = http.post('http://api-gateway/api/orders/checkout', payload, params);

  check(res, {
    'status is 200 or 201': (r) => r.status === 200 || r.status === 201,
  });

  sleep(1); // Thời gian suy nghĩ của user (Pacing)
}
```

3.  **Khắc phục Contract Drift với CDC (Pact):**
    *   *Nguyên nhân Unit test fail để phát hiện:* Mock chỉ kiểm tra giả định chủ quan của Consumer, không phản ánh hiện thực của Provider.
    *   *Pact CDC Flow:* Order Service định nghĩa Pact file (yêu cầu các trường: `id`, `name`, `price`, `available`). CI/CD của Product Service chạy `mvn pact:verify`. Khi Product Service đổi tên trường hoặc xóa `available`, pipeline CI của Product Service **bị fail ngay lập tức**, ngăn chặn deploy mã nguồn lỗi lên Production.
    *   *API Migration an toàn:*
        ```json
        // Version chuyển tiếp (Backward Compatible): Giữ cả trường cũ và mới
        {
          "id": "P-100",
          "name": "Mechanical Keyboard",        // Giữ lại và đánh dấu Deprecated
          "productName": "Mechanical Keyboard", // Trường mới
          "price": 120.50,                      // Giữ lại
          "unitPrice": 120.50,                  // Trường mới
          "available": true                     // Giữ lại cho đến khi Order team migrate xong
        }
        ```

4.  **Tái cấu trúc Bộ tứ phòng vệ (Resilience Configuration):**
    ```yaml
    # Cấu hình chuẩn Production với Resilience4j
    resilience4j:
      circuitbreaker:
        instances:
          inventoryService:
            slidingWindowSize: 20
            failureRateThreshold: 50        # Lỗi hoặc Timeout > 50% -> Ngắt mạch
            waitDurationInOpenState: 5000ms # Ngắt trong 5s rồi sang Half-Open
            slowCallRateThreshold: 50
            slowCallDurationThreshold: 800ms # Cuộc gọi > 800ms coi như chậm
      bulkhead:
        instances:
          inventoryBulkhead:
            maxConcurrentCalls: 30          # Chỉ cấp tối đa 30 threads cho Inventory
            maxWaitDuration: 100ms          # Quá tải thì fail-fast, bảo vệ luồng GET orders
      retry:
        instances:
          inventoryService:
            maxAttempts: 3
            waitDuration: 200ms
            enableExponentialBackoff: true  # Tăng thời gian chờ theo hàm mũ
            exponentialBackoffMultiplier: 2
            enableRandomJitter: true        # Rải rác thời gian tránh Thundering Herd
    
    external-services:
      inventory-service:
        connectTimeout: 500ms
        readTimeout: 800ms                  # Thay thế con số 30 giây bằng 800ms!
    ```

5.  **Bản kế hoạch Thử nghiệm Chaos (Chaos Experiment Plan):**
    *   **Tên thử nghiệm:** `CHAOS-EXP-01: Inventory Latency Injection Under Load`
    *   **Hypothesis (Giả thuyết):** "Khi Inventory Service bị tiêm độ trễ 3000ms (tỷ lệ 80% requests), Circuit Breaker của Order Service sẽ chuyển sang trạng thái `OPEN` trong vòng 10 giây; p95 latency của API Checkout được chặn ở mức < 1000ms (nhờ Fail-fast); và throughput của API `GET /api/orders` không bị suy giảm quá 5%".
    *   **Blast Radius (Phạm vi):** Môi trường Staging (hoặc 5% Canary Traffic trên Production với header `X-Chaos-Test: true`).
    *   **Công cụ thực hiện:** Sử dụng **Toxiproxy** hoặc **Chaos Mesh** tiêm lỗi: `toxiproxy-cli toxic add inventory -t latency -a latency=3000 -a jitter=500`.
    *   **Abort Condition (Dừng khẩn cấp):** Nếu tổng thể Error Rate của toàn sàn vượt quá **2%** trên các luồng nghiệp vụ không liên quan, lập tức rollback toxic và ngắt thí nghiệm tự động.
    *   **Kết quả mong đợi:** Circuit Breaker ngắt mạch thành công, Bulkhead bảo toàn 170 threads còn lại cho API `GET /orders`, hệ thống không bị sập dây chuyền (No Cascading Failure).

---

> [!IMPORTANT]
> **KẾT LUẬN TỪ HỘI ĐỒNG ARCHITECT:**
> Cả 3 buổi audit trên phản ánh đúng lộ trình tiến hóa của một kỹ sư phần mềm chuyên nghiệp:
> 1. **Tuần 1:** Xây dựng nền móng code vững chắc, mô hình hóa nghiệp vụ đúng đắn (DDD/Clean Architecture), không để rò rỉ logic.
> 2. **Tuần 2:** Làm chủ dữ liệu phân tán, hiểu rõ các đánh đổi vật lý (CAP/Replication/Sharding), không tin tưởng mù quáng vào phần cứng.
> 3. **Tuần 3:** Kiểm chứng bằng số liệu thực tế (Load Test/Pact CDC) và chủ động rèn luyện độ kiên cường của hệ thống trước mọi thảm họa (Chaos Engineering & Resilience).
