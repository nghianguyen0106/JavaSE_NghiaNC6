# 04. System Design: Large-Scale Multi-Tenant Payroll SaaS

## 1. Yêu Cầu Hệ Thống (System Requirements)

### 1.1. Functional Requirements (FR)
1. Quản lý danh mục & hồ sơ: Quản lý Tenant, Employee Profile, Leave Balance, Salary Structure.
2. Thực thi tính lương bất đồng bộ (Async Payroll Calculation): Cho phép HR Admin kích hoạt `RunPayroll`. Hệ thống tính toán thuế, bảo hiểm, phụ cấp và sinh `Payslip` cho toàn bộ nhân viên thuộc kỳ lương.
3. Tra cứu phiếu lương (Payslip Retrieval): Nhân viên và quản trị viên xem chi tiết Payslip cá nhân.
4. Ngăn chặn chạy trùng (Duplicate Run Prevention): Đảm bảo duy nhất một tiến trình tính lương thành công cho một tenant trong một kỳ (`payPeriod`).
5. Gửi thông báo: Tự động gửi email/push notification khi Payslip sẵn sàng.
6. Lưu vết kiểm toán (Audit Trail): Lưu giữ toàn bộ lịch sử biến động lương và tính toán tối thiểu 7 năm theo quy định pháp lý.

### 1.2. Non-Functional Requirements (NFR)
1. Scale: 50,000 tenants, 5,000,000 employees. Enterprise tenant lớn nhất đạt 100,000 employees.
2. Peak Throughput: 10,000 payroll runs diễn ra tập trung trong 2 giờ cao điểm cuối tháng.
3. Latency SLA: 
   - Truy vấn Payslip: $P95 < 500\text{ms}$, $P99 < 2\text{s}$.
   - Khởi tạo lệnh `RunPayroll`: $P95 < 200\text{ms}$ (nhận lệnh và đưa vào hàng đợi).
   - Thời gian hoàn tất mẻ tính lương 100,000 nhân viên: $< 30\text{ phút}$.
4. Availability & Consistency:
   - Hệ sinh thái tính toán: Availability 99.9%.
   - Dữ liệu tài chính/lương: Strong Consistency trong phạm vi Tenant (ACID per Tenant).
5. Security & Compliance: Multi-tenant Data Isolation (Tenant A tuyệt đối không truy cập được dữ liệu Tenant B).

---

## 2. Ước Tính Năng Lực Hệ Thống (Capacity Estimation)

### 2.1. Thông lượng tính toán (Calculation Throughput)
- Tổng số lượt chạy trong peak: $10,000\text{ runs} / (2 \times 3600\text{s}) \approx 1.39\text{ runs/s}$.
- Số lượng nhân viên trung bình cần tính toán trong peak:
  - Giả định 10,000 tenants trong peak chứa trung bình 100 employees/tenant $\rightarrow 1,000,000\text{ employees}$.
  - Kèm 5 enterprise tenants (mỗi tenant 100,000 employees) chạy cùng lúc $\rightarrow 500,000\text{ employees}$.
  - Tổng số bản ghi lương cần tính trong 2 giờ: $1,500,000\text{ payslips}$.
  - Tốc độ sinh payslip trung bình: $1,500,000 / 7200\text{s} \approx 208\text{ payslips/s}$.
  - Peak throughput tính toán thiết kế: $1,000\text{ payslips/s}$.

### 2.2. Thông lượng đọc (Read QPS - Payslip Retrieval)
- Sau khi gửi thông báo, giả định $30\%$ trên tổng số 5 triệu nhân viên vào kiểm tra phiếu lương trong vòng 4 giờ đầu:
  $$\text{Total Requests} = 5,000,000 \times 30\% = 1,500,000\text{ requests}$$
  $$\text{Average Read QPS} = \frac{1,500,000}{4 \times 3600} \approx 104\text{ requests/s}$$
  $$\text{Peak Read QPS (hệ số an toàn } 5\times) \approx 520\text{ QPS}$$

### 2.3. Dung lượng lưu trữ (Storage Estimation - 7 Years Retention)
- Kích thước một bản ghi Payslip chi tiết (kèm metadata, breakdown thuế, khấu trừ): $\approx 2\text{ KB}$.
- Số bản ghi sinh ra mỗi tháng: 5,000,000 payslips.
- Dung lượng phát sinh mỗi tháng:
  $$5,000,000 \times 2\text{ KB} = 10,000,000\text{ KB} \approx 10\text{ GB/tháng}$$
- Dung lượng sau 7 năm (84 tháng):
  $$10\text{ GB} \times 84 = 840\text{ GB (Dữ liệu thuần)}$$
- Tính toán hệ số an toàn:
  - Kèm chỉ mục (Indexes 50%): $+420\text{ GB}$.
  - Replication factor 3 (HA Primary + 2 Replicas): $(840 + 420) \times 3 \approx \mathbf{3.78\text{ TB}}$.
  - Đây là quy mô dữ liệu hoàn toàn xử lý tốt bằng cơ sở dữ liệu quan hệ (PostgreSQL với cơ chế Table Partitioning theo `pay_period` và `tenant_id`).

---

## 3. Kiến Trúc Thành Phần (System Components)

```
                             [Client / Browser / Mobile]
                                         |
                                         v
                            +--------------------------+
                            |       API Gateway        |
                            | (Rate Limit, Auth Relay) |
                            +--------------------------+
                                         |
     +-----------------------------------+-----------------------------------+
     |                                                                       |
     v (Sync Read)                                                           v (Async Command)
+-------------------------+                                     +-------------------------+
|     Payslip Service     |                                     |     Payroll Service     |
|   (Read Model / Cache)  |                                     |    (Calculation Core)   |
+-------------------------+                                     +-------------------------+
     |               |                                                       |
     v               v                                                       v
+---------+    +------------+                                          +------------------+
| Redis   |    | PostgreSQL |                                          | Distributed Lock |
| Cluster |    | Read Reps  |                                          | (Redis/Redisson) |
+---------+    +------------+                                          +------------------+
                                                                             |
                                                                             v
                                                                   +----------------------+
                                                                   | Apache Kafka Broker  |
                                                                   +----------------------+
                                                                             |
                                        +------------------------------------+------------------------------------+
                                        |                                                                         |
                                        v                                                                         v
                            +-------------------------+                                               +-------------------------+
                            |  Payroll Worker Pool    |                                               |  Notification Service   |
                            |  (Spring Batch Chunks)  |                                               |  (Email/Push Dispatch)  |
                            +-------------------------+                                               +-------------------------+
```

1. API Gateway: Điều hướng routing, xác thực JWT, giải mã `tenantId`, thực thi Global Rate Limiting per Tenant.
2. Payroll Core Service: Nhận `RunPayrollCommand`, kiểm tra tính hợp lệ, chiếm Distributed Lock và gửi Task vào Kafka Partitioned Topic.
3. Payroll Worker Pool: Cụm Worker tiêu thụ event từ Kafka, thực thi tính toán theo mô hình Chunk-oriented (Spring Batch). Mỗi worker xử lý các mẻ 1,000 nhân viên.
4. Distributed Lock (Redis Cluster): Khóa tài nguyên theo khóa: `lock:payroll:{tenantId}:{payPeriod}` với TTL hợp lý.
5. Database Architecture: PostgreSQL hỗ trợ Multi-tenant bằng `tenant_id` column-level isolation kết hợp Row-Level Security (RLS) và Partitioning theo năm/tháng.

---

## 4. Dòng Dữ Liệu Thực Thi (End-to-End Data Flow)

1. Khởi tạo: HR Admin gọi `POST /api/v1/payroll-runs`.
2. Khóa chống chạy trùng:
   - Payroll Service gọi Redis: `SET lock:payroll:T1:2026-10 "LOCKED" NX EX 1800`.
   - Nếu trả về `nil` $\rightarrow$ Ném mã lỗi HTTP `409 Conflict`.
3. Tạo bản ghi gốc: Tạo record trong bảng `payroll_runs` với trạng thái `PENDING`.
4. Đẩy việc bất đồng bộ: Publish message `CalculatePayrollTask` vào Kafka topic `payroll.calculation.jobs`.
5. Tính toán phân tán:
   - Worker nhận task, phân mảnh danh sách nhân viên thành các chunk nhỏ (ví dụ 1,000 records).
   - Truy vấn thông tin lương cơ bản, ngày công từ Read Replica.
   - Tính toán khấu trừ thuế và bảo hiểm.
   - Batch insert kết quả vào bảng `payroll_payslips`.
6. Hoàn tất & Kích hoạt chuỗi sự kiện:
   - Worker cập nhật `payroll_runs.status = 'COMPLETED'`.
   - Giải phóng Distributed Lock.
   - Publish event `PayrollCompletedEvent` vào Kafka topic `payroll.events` qua Transactional Outbox.
7. Downstream Consumer:
   - Notification Service nhận event, render mẫu phiếu lương và gửi email/thông báo đến nhân viên.

---

## 5. Kịch Bản Lỗi & Cơ Chế Khắc Phục (Failure Scenarios)

| Kịch bản lỗi | Điểm phát sinh | Cơ chế bảo vệ & Xử lý (Mitigation) |
| :--- | :--- | :--- |
| Worker sập giữa chừng khi tính tenant 100k user | Payroll Worker Node sập nguồn (OOM/Hardware) | Kafka Consumer Group Rebalance + Chunk Commit: Worker lưu offset theo từng Chunk 1,000 records. Node mới nhận task sẽ đọc offset tiếp tục xử lý các Chunk chưa commit mà không chạy lại từ đầu. |
| Hot Tenant (100k user) làm nghẽn các tenant nhỏ | Kafka Queue Saturation | Priority Queuing / Dynamic Sharding: Phân bổ các Enterprise Tenant (>10k nhân viên) vào Kafka Topic riêng (`payroll.jobs.heavy`), các tenant thông thường chạy trên `payroll.jobs.standard`. |
| Lỗi mạng khi ghi Database Payslip | PostgreSQL connection timeout | Transient Retry with Exponential Backoff: Worker retry tối đa 3 lần. Nếu lỗi kéo dài, chuyển mẻ lỗi vào DLQ và đánh dấu PayrollRun là `FAILED_PARTIAL` kèm notification tới admin. |
| Xung đột ghi đồng thời (Double Click từ 2 Admin) | Client Layer | Distributed Lock + Unique Constraint DB: Cặp `(tenant_id, pay_period)` được đặt UNIQUE constraint trong DB để tạo lớp phòng thủ thứ hai sau Redis Lock. |

---

## 6. Câu Hỏi Thiết Kế Mở (Open Architectural Questions)

1. Chiến lược Multi-tenancy Database: Hiện tại chọn Shared Database - Shared Schema với `tenant_id` + RLS để tối ưu chi phí hạ tầng 50,000 tenants. Khi có 100+ Enterprise tenants quy mô cực lớn, có nên tách riêng Database độc lập (Database-per-tenant) cho nhóm Enterprise này hay không?
2. Lưu trữ Payslip dạng Cold Data: Bản ghi lương sau 12 tháng ít khi được truy vấn, có nên áp dụng chính sách Lifecycle Data chuyển dữ liệu cũ hơn 1 năm sang Object Storage (S3 / Cold Tier Table) để giảm tải storage cost và duy trì hiệu năng index cho Active Database hay không?
