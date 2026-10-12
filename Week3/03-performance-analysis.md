# Assignment 03: Phân tích Kết quả Kiểm thử Hiệu năng & Điều tra Bottleneck

## 1. Test Data

| Load (RPS) | p95 API Latency | Error Rate | App CPU | DB CPU | DB Connections | Cache Hit Rate |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| 100 RPS | 120ms | 0% | 25% | 20% | 20/100 | 95% |
| 500 RPS | 280ms | 0.1% | 55% | 60% | 70/100 | 91% |
| 800 RPS | 1.8s | 3.5% | 65% | 95% | 100/100 | 88% |
| 1,000 RPS | 5.2s | 15.0% | 68% | 98% | 100/100 | 85% |

---

## 2. Bảng Tương quan Metric & Điểm gãy (Correlation & Inflection Point Analysis)

```
Load (RPS)       100 RPS ────────> 500 RPS ───────┬> 800 RPS ─────────> 1,000 RPS
                                                  │ (INFLECTION POINT)
p95 Latency:     120ms             280ms          ├─> 1.8s (Tăng 6.4x) ──> 5.2s (Tăng 18.5x)
DB Connections:  20/100            70/100         ├─> 100/100 (CHẠM TRẦN)─> 100/100 (Bão hòa)
DB CPU:          20%               60%            ├─> 95% (BÃO HÒA)   ──> 98% (Treo)
App CPU:         25%               55%            └──> 65% (Tăng chậm) ──> 68% (Đứng yên)
```

* Điểm gãy hệ thống (Inflection Point): Nằm giữa mức 500 RPS và 800 RPS.
* Hiện tượng: 
  * Khi tải tăng từ 500 lên 800 RPS, trong khi App CPU chỉ nhích nhẹ từ 55% lên 65% (vẫn còn dư 35% tài nguyên), thì DB CPU đã vọt lên 95% và DB Connection Pool chạm đỉnh 100/100.
  * Ở mức 1,000 RPS, App CPU gần như đi ngang (68%) vì các luồng Application Thread không thể tiếp tục xử lý công việc mà đang bị chặn đứng (Blocked/Waiting) để chờ mượn kết nối DB.

---

## 3. Trả lời chi tiết 5 câu hỏi trọng tâm

### Q1. Bottleneck có khả năng cao nhất nằm ở đâu?
👉 Kết luận: Bottleneck nằm tại DATABASE TIER (Cơ sở dữ liệu), cụ thể là Cạn kiệt Connection Pool kết hợp với Bão hòa CPU của Database.

---

### Q2. Evidence nào hỗ trợ kết luận?
1. DB Connection Pool bị cạn kiệt tuyệt đối (Exhaustion):
   * Tại 800 và 1,000 RPS, số lượng kết nối luôn ở mức cực đại 100/100. Mọi request gửi tới sau thời điểm này đều bị rơi vào hàng đợi chờ kết nối (`Connection Acquisition Wait Queue`), dẫn đến latency tăng vọt từ 280ms lên 1.8s và 5.2s.
2. DB CPU bão hòa (CPU Saturation 95% - 98%):
   * CPU của DB server chạm trần 98%. Khi CPU DB chạm 98%, khả năng xử lý truy vấn bị nghẽn nghẹt, các query chạy chậm lại, giữ connection lâu hơn -> Vòng xoáy nghẽn connection pool càng trầm trọng.
3. Sự tách rời (Decoupling) giữa App CPU và Latency:
   * Trong khi latency tăng gấp gần 19 lần (từ 280ms lên 5.2s), App CPU chỉ tăng từ 55% lên 68%. Điều này chứng minh ứng dụng không hề bị thiếu CPU mà đang "ngồi chơi xơi nước" chờ DB phản hồi.

---

### Q3. Vì sao Application CPU không phải là Root Cause chính?
* App CPU chỉ là Triệu chứng (Symptom), không phải Nguyên nhân gốc rễ (Root Cause):
  * Nếu nguyên nhân là do Application (ví dụ: thuật toán nặng $O(n^2)$, serialize JSON nặng, xử lý CPU bound), thì App CPU phải chạm trần 95% - 100% trước tiên.
  * Ở đây, App CPU chỉ đạt 68% rồi chững lại. Lý do App CPU không tăng thêm được là vì các Worker Threads của ứng dụng đã bị đưa vào trạng thái `WAITING / BLOCKED` tại tầng DataSource (`HikariPool-1 - Connection is not available, request timed out after 30000ms`).
* Hậu quả nếu scale nhầm App: Nếu ta vội vàng tăng số lượng Pod/Instance của ứng dụng lên gấp đôi, mỗi Pod mới sẽ mở thêm hàng chục kết nối tới DB -> DB CPU sẽ chết ngay lập tức ở mức tải thấp hơn!

---

### Q4. Các bước điều tra tiếp theo là gì?
Không được đoán mò, kỹ sư cần thu thập các bằng chứng kỹ thuật theo thứ tự sau:

1. Bật và phân tích Slow Query Log:
   * Lọc các câu truy vấn có thời gian thực thi $> 100\text{ms}$.
   * Tính toán tỉ lệ câu truy vấn nào chiếm tổng thời gian CPU nhiều nhất (Query Execution Time Cumulative).
2. Kiểm tra Query Execution Plan (`EXPLAIN ANALYZE`):
   * Kiểm tra có câu lệnh nào đang bị `Sequential Scan` (Quét toàn bộ bảng) do thiếu Index ở các bảng `orders`, `order_items`, `products` hay không.
3. Rà soát lỗi N+1 Query:
   * Kiểm tra mã nguồn ORM (JPA/Hibernate) xem khi tạo hoặc xem đơn hàng có bị bắn hàng chục câu query con lấy thông tin sản phẩm/khách hàng trong vòng lặp hay không.
4. Kiểm tra Tranh chấp Khóa (Lock Contention / Deadlocks):
   * Chạy câu lệnh `SELECT * FROM pg_stat_activity WHERE wait_event_type = 'Lock'` (Postgres) hoặc kiểm tra InnoDB Lock Monitor (MySQL) để xem có các transaction đang giữ lock trên cùng hàng tồn kho hay không.
5. Đo thời gian chờ Connection Pool:
   * Theo dõi metric `hikaricp_connection_acquired_nanos` và `hikaricp_connection_pending_threads` trên Prometheus/Grafana.

---

## 4. Kế hoạch Khắc phục toàn diện (Remediation Plan)

### Immediate Mitigation
1. Thiết lập Rate Limiting & Concurrency Throttling:
   * Cấu hình giới hạn tại API Gateway tối đa 700 RPS đối với endpoint checkout. Trả về mã lỗi `HTTP 429 Too Many Requests` có kiểm soát thay vì để toàn bộ hệ thống sập và trả `HTTP 500`.
2. Cấu hình Connection Pool Timeout hợp lý:
   * Giảm `connectionTimeout` trong HikariCP từ 30s xuống 3s - 5s để ứng dụng fail-fast, không để các luồng bị treo giữ tài nguyên vô ích.
3. Tắt các cơ chế Retry không an toàn:
   * Tắt retry tức thì từ phía Client và Frontend để tránh hiện tượng khuếch đại lưu lượng (Traffic Amplification).

### Short-term Fix
1. Đánh Index bổ sung & Tối ưu Slow Queries:
   * Bổ sung Index cho các cột tìm kiếm trong `WHERE`, `JOIN` và `ORDER BY`.
2. Triệt tiêu dứt điểm lỗi N+1 Query:
   * Sử dụng `JOIN FETCH` hoặc `@EntityGraph` trong Spring Data JPA để gộp truy vấn.
3. Cải thiện Cache Hit Rate (Tăng từ 85% lên $\ge 95\%$):
   * Bảng số liệu cho thấy khi tải tăng, Cache hit rate giảm từ 95% xuống 85% -> Nhiều request đọc bị chọc thẳng xuống DB.
   * Cần cấu hình Redis Cache cho danh mục sản phẩm (Product Catalog), thông tin giá và áp dụng kỹ thuật Cache Aside / Cache Warm-up trước giờ cao điểm.
4. Tune Connection Pool:
   * Điều chỉnh kích thước connection pool theo công thức chuẩn:
     $$\text{Connections} = (\text{CPU Cores} \times 2) + \text{Disk Spindle Count}$$
   * Đặt pool ở mức vừa phải (ví dụ 30 - 40 connections/pod) để DB không bị nghẽn do Context Switching.

### Long-term Architecture Improvement
1. Tách biệt luồng Đọc và Ghi (Read-Write Splitting / CQRS nhẹ):
   * Thiết lập Database Read Replicas. Mọi truy vấn đọc (`GET /products`, `GET /orders`) chuyển hướng sang Read Replica; Primary DB chỉ phục vụ thao tác Ghi (`POST /orders`).
2. Chuyển đổi sang Bất đồng bộ (Asynchronous Order Processing):
   * Sử dụng Message Broker (Kafka / RabbitMQ): API Checkout nhận đơn -> lưu tạm vào hàng đợi -> trả về `202 Accepted` -> Worker xử lý trừ kho và lưu DB ở hạ tầng phía sau theo nhịp độ mà DB chịu tải được.
3. Database Sharding / Partitioning:
   * Khi quy mô đơn hàng vượt quá hàng chục triệu bản ghi, thực hiện phân vùng (Partitioning) theo thời gian hoặc Sharding theo Customer Region.
4. Tích hợp Load Test vào CI/CD Pipeline:
   * Chạy k6 test tự động ở môi trường Staging với tiêu chí chặn build nếu p95 vượt 500ms tại 800 RPS.
