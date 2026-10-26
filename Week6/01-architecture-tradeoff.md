# 01. Architecture Trade-off Analysis: Monolith vs Modular Monolith vs Microservices

## 1. Bối cảnh hệ thống
- Hệ thống: HR/Payroll SaaS.
- Quy mô tổ chức: 20 software engineers.
- Hiện trạng kỹ thuật: 1 Spring Boot Application (Monolith), 1 PostgreSQL Database cluster.
- Các module hiện hữu: Identity, Employee, Leave, Payroll, Notification, Reporting.
- Đặc tính tải và biến động nghiệp vụ:
  - Module Payroll: Tần suất thay đổi business logic cao (luật thuế, chính sách bảo hiểm, công thức thưởng theo quý).
  - Module Reporting: Tải CPU và I/O cao (tính toán aggregation, analytics cuối tháng).
  - Module Notification: Yêu cầu scale thông lượng (throughput) đột biến theo lô (batch/burst email, SMS, push notification khi chốt bảng lương).
  - Module Identity, Employee, Leave: Tần suất deploy thấp, business logic ổn định, mô hình CRUD chiếm ưu thế.

---

## 2. Transitional Architecture

### Quyết định kỹ thuật
Không thực hiện tái cấu trúc toàn diện (Big-Bang rewrite) sang Microservices. Lựa chọn giải pháp: Modular Monolith làm nền tảng, kết hợp trích xuất (extract) chọn lọc các service độc lập dựa trên đặc tính tải và tần suất phát hành (deployment frequency).

```
                      +------------------------------------------+
                      |               API Gateway                |
                      +------------------------------------------+
                                     |             |
        +----------------------------+             +----------------------------+
        |                                                                       |
+---------------------------------------------+        +-----------------------------------+
|          Modular Monolith (Core)            |        |     Reporting Service (Async)     |
| - Identity Module                           |        | - Read Replica / OLAP Engine      |
| - Employee Module                           |        +-----------------------------------+
| - Leave Module                              |                                 |
| - Payroll Module (Tách nội bộ theo Package) |        +-----------------------------------+
+---------------------------------------------+        |   Notification Service (Worker)   |
        |                                              | - Message Queue Consumer (Kafka)  |
+---------------------------------------------+        +-----------------------------------+
|          PostgreSQL Primary DB              |
+---------------------------------------------+
```

---

## 3. Detailed analysis of service extraction roadmap

### 3.1. Modules to be extracted into independent services
1. Notification Service:
   - Reason for extraction: Workload is I/O-bound and bursty (load increases suddenly at the end of the month). Requires worker to scale independently (horizontal auto-scaling) based on queue lag, not dependent on the lifecycle of the main application.
   - Data impact: Data is transient, not dependent on ACID relationships with core database. Communication is completely asynchronous (Asynchronous Event-Driven via Apache Kafka).
   - Deployment Frequency: Thấp, nhưng yêu cầu runtime isolation để việc nghẽn thread pool gửi mail/SMS không gây cạn kiệt tài nguyên của Core API.

2. Reporting Service:
   - Lý do tách: Heavy computational & I/O workload. Các truy vấn tổng hợp lớn (Complex OLAP queries) khóa tài nguyên CPU/RAM, gây nghẽn Connection Pool và tăng latency của các giao dịch OLTP (như check-in, nộp đơn nghỉ phép).
   - Tác động dữ liệu: Chỉ đọc (Read-only model). Sử dụng cơ chế CDC (Change Data Capture via Debezium) hoặc Read Replica để tách biệt hoàn toàn I/O với Primary OLTP Database.

### 3.2. Các module giữ lại trong Modular Monolith (Retain in Core)
1. Identity, Employee, Leave:
   - Lý do: Tần suất thay đổi nghiệp vụ thấp, mô hình dữ liệu quan hệ chặt chẽ (Strong Relational Integrity), tải phân bổ đều. Việc duy trì trong Modular Monolith giúp bảo toàn Foreign Key constraints, tối ưu local in-memory method calls, giảm chi phí vận hành distributed network.
2. Payroll Module (Chưa tách ra Microservice độc lập ở giai đoạn đầu):
   - Lý do: Mặc dù thay đổi nghiệp vụ thường xuyên, Payroll có liên kết dữ liệu giao dịch phụ thuộc cao (High Data Coupling) với Employee Profile, Tax Grade, và Leave Balance. 
   - Rủi ro nếu tách ngay: Yêu cầu xử lý Distributed Transaction (Saga Pattern hoặc Two-Phase Commit), làm tăng độ phức tạp trong bảo toàn tính nhất quán (Consistency) của bảng lương.
   - Giải pháp chuyển tiếp: Tách thành Module cô lập bên trong Monolith (Isolated Package Boundary). Định nghĩa interface nghiêm ngặt (In-process boundary), cấm cross-module database join trực tiếp. Sau khi business contract ổn định mới trích xuất vật lý thành Service độc lập.

---

## 4. Ma trận đánh giá động lực kiến trúc (Architecture Drivers Matrix)

| Module | Business Change Rate | Deployment Frequency | Scalability Profile | Data Coupling Level | Target Architecture |
| :--- | :--- | :--- | :--- | :--- | :--- |
| Notification | Thấp | Thấp | Burst / Queue-based Scale | Thấp (Transient data) | Microservice (Worker) |
| Reporting | Trung bình | Trung bình | Heavy Read I/O / Compute-bound | Trung bình (Read-only copy) | Microservice (Read Model) |
| Payroll | Rất cao | Rất cao | CPU-intensive theo chu kỳ | Rất cao (Employee/Leave/Tax) | Module cô lập (Modular Monolith) |
| Employee | Thấp | Thấp | Phân bổ đều (Read-heavy) | Cao (Core Entity) | Modular Monolith |
| Leave | Thấp | Thấp | Phân bổ đều (OLTP) | Cao (Employee relationship) | Modular Monolith |
| Identity | Rất thấp | Rất thấp | Read-heavy (Token verification) | Thấp / Độc lập | Modular Monolith |

---

## 5. Rủi ro khi phân tách toàn diện đồng thời (Big-Bang Microservices Anti-pattern)

1. Distributed Data Inconsistency (Bất đồng bộ dữ liệu phân tán):
   - Việc loại bỏ Foreign Key vật lý và thay thế Local ACID Transaction bằng Eventual Consistency (Saga) khi boundary nghiệp vụ chưa ổn định dẫn đến rủi ro sai lệch dữ liệu bảng lương (Data Corruption), đối soát tài chính thất bại.
2. Operational Overhead (Quá tải năng lực vận hành):
   - Quy mô 20 engineers không đủ nguồn lực để chia nhỏ thành các team cross-functional (Stream-aligned teams) quản lý độc lập 6+ microservices kèm pipeline CI/CD, Distributed Tracing, Service Mesh, và hạ tầng Kubernetes riêng biệt.
3. Network Latency Amplification (Khuếch đại độ trễ mạng):
   - Chuyển đổi các hàm gọi nội bộ (in-memory invocation ~nanoseconds) thành HTTP/gRPC remote calls (~milliseconds) qua mạng tạo ra hiện tượng Network Hop tích lũy, làm suy giảm P99 Latency của toàn bộ hệ thống.
4. Distributed Monolith Risk:
   - Nếu tách service mà data schema vẫn còn quan hệ phụ thuộc chặt chẽ, hệ thống sẽ trở thành "Distributed Monolith" — kế thừa toàn bộ nhược điểm của Monolith (deploy phụ thuộc) cộng thêm toàn bộ nhược điểm của Microservices (mạng không tin cậy, khó debug).
