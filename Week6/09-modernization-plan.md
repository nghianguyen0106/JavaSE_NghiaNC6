# 09. Legacy Modernization Plan: Strangler Fig Pattern & Technical Debt Management

## 1. Hiện Trạng Hệ Thống Kế Thừa (Legacy System Baseline)
- Kiến trúc: 1 Monolith 8 năm tuổi, 1 Shared Relational Database duy nhất.
- Hạn chế:
  - Module Notification gửi email đồng bộ (synchronous SMTP call) trực tiếp trong HTTP request thread $\rightarrow$ Tăng nguy cơ cạn kiệt thread pool khi SMTP chậm.
  - Module Reporting thực thi các truy vấn nặng trực tiếp trên Primary DB $\rightarrow$ Gây khóa bảng, suy giảm hiệu năng OLTP.
  - Module Payroll thay đổi thường xuyên nhưng thiếu automated test coverage (thiếu Safety Net).

---

## 2. Thứ Tự Ưu Tiên Phân Tách Nghiệp Vụ (Decomposition Strategy)

```
[Legacy Monolith: 8 Years Old]
         |
         | Phase 1: Trích xuất Notification (Thấp rủi ro, cô lập I/O)
         +--------------------------------------------------------> [Notification Service]
         |
         | Phase 2: Tách biệt luồng Reporting (Tách biệt I/O dữ liệu)
         +--------------------------------------------------------> [Reporting Service / Read Model]
         |
         | Phase 3: Hiện đại hóa nội bộ Payroll (Viết Characterization Tests)
         +--------------------------------------------------------> [Isolated Payroll Module]
         |
         | Deferred: Giữ lại Employee & Leave trong Monolith
         v
[Refined Core Monolith: Employee, Leave, Identity]
```

### 2.1. Nghiệp vụ trích xuất đầu tiên: Notification Module
- Lý do: Rủi ro nghiệp vụ thấp nhất, dữ liệu mang tính tạm thời (transient), không có ràng buộc Foreign Key khắt khe với các bảng lõi. Việc chuyển sang kiến trúc bất đồng bộ (Message-driven) giải quyết ngay lập tức sự cố nghẽn thread của Monolith.

### 2.2. Nghiệp vụ thứ hai: Reporting Module
- Lý do: Tách biệt gánh nặng tính toán OLAP khỏi Primary Database. Sử dụng cơ chế CDC đồng bộ dữ liệu sang Read Replica/Elasticsearch mà không cần can thiệp sâu vào code nghiệp vụ cũ của Monolith.

### 2.3. Nghiệp vụ KHÔNG NÊN tách đầu tiên: Payroll Module
- Lý do: Payroll chứa logic tính toán cốt lõi, phụ thuộc dữ liệu đa chiều (Employee, Leave, Tax, Bank). Vì Monolith chưa có bộ Test Suite hoàn chỉnh, việc tách Payroll ngay từ đầu sẽ gây rủi ro sai lệch số liệu tài chính cực lớn (Financial Inaccuracy).
- Chiến lược tiếp cận: Trước tiên phải viết Characterization Tests (Golden Master Testing) để "đóng băng" hành vi đúng hiện tại của Payroll, sau đó mới tiến hành refactor bên trong Monolith trước khi cân nhắc tách vật lý.

---

## 3. Quy Trình Strangler Fig & Chuyển Đổi Dữ Liệu (Strangler Flow & Data Migration)

```
[Client Traffic]
       |
       v
+---------------------------------------------------------------------------------+
|                       Strangler Facade / API Gateway                            |
|             (Điều hướng traffic dựa trên Path / Tenant / Canary Weight)         |
+---------------------------------------------------------------------------------+
       |                                                    |
       | Route cũ (Legacy)                                  | Route mới (Extracted)
       v                                                    v
+-------------------------------+                  +-------------------------------+
|        Legacy Monolith        |                  |      New Microservice         |
+-------------------------------+                  +-------------------------------+
       |                                                    ^
       | Write                                              | Read / Write
       v                                                    |
+-------------------------------+                  +-------------------------------+
|       Legacy Database         |                  |         New Database          |
+-------------------------------+                  +-------------------------------+
       |                                                    ^
       +--- CDC Pipeline (Debezium + Kafka) ----------------+
            (Đồng bộ dữ liệu hai chiều có kiểm soát)
```

### Các bước chuyển đổi dữ liệu (Dual-Write / CDC Strategy):
1. Giai đoạn 1 (Replication Phase): Dữ liệu vẫn được ghi vào Legacy Database. Debezium CDC chụp log thay đổi và sao chép theo thời gian thực sang New Database.
2. Giai đoạn 2 (Dark Launch / Shadow Traffic): API Gateway nhân đôi traffic (Request Shadowing). Kết quả từ New Service được đối chiếu với Legacy Service để xác minh tính tương đồng mà không trả về cho client.
3. Giai đoạn 3 (Cutover Phase): API Gateway chuyển hướng toàn bộ traffic sang New Service. New Service trở thành Source of Truth.
4. Giai đoạn 4 (Decommission Phase): Ngắt kết nối CDC và gỡ bỏ code cũ trong Monolith.

---

## 4. Kế Hoạch Rollback An Toàn (Rollback Plan)

1. Feature Flag / Traffic Shift: Tại API Gateway, cấu hình trọng số routing qua dynamic configuration (Spring Cloud Gateway / Envoy). Nếu phát hiện tỷ lệ lỗi $5xx > 1\%$ hoặc latency tăng vọt, chuyển ngay lập tức $100\%$ traffic về Legacy Monolith trong $< 5\text{ giây}$.
2. Reverse CDC (Đồng bộ ngược): Trong suốt giai đoạn Cutover, thiết lập một pipeline đồng bộ ngược từ New Database về Legacy Database. Nếu phải rollback sau nhiều giờ vận hành, dữ liệu phát sinh mới không bị thất thoát.

---

## 5. Sổ Đăng Ký Nợ Kỹ Thuật (Technical Debt Register)

| ID | Vấn đề kỹ thuật (Technical Debt) | Mức độ rủi ro | Chi phí xử lý | Giải pháp khắc phục (Remediation) |
| :---: | :--- | :--- | :--- | :--- |
| TD-01 | Thiếu Unit & Integration Test trong module tính lương | Rất cao | Trung bình | Xây dựng bộ Characterization Tests chụp snapshot input/output của 1.000 trường hợp tính lương thực tế. |
| TD-02 | Gửi email đồng bộ làm nghẽn Tomcat worker threads | Cao | Thấp | Trích xuất Notification Service và thay thế bằng Spring Cloud Stream Kafka. |
| TD-03 | Truy vấn Reporting chạy trực tiếp trên Primary OLTP DB | Cao | Trung bình | Thiết lập PostgreSQL Read Replica và cấu hình Routing DataSource (`@Transactional(readOnly = true)`). |
| TD-04 | Dùng chung 1 Entity `Employee` cho toàn bộ hệ thống | Trung bình | Cao | Refactor theo DDD Bounded Context, chia nhỏ entity theo từng package nội bộ. |

---

## 6. Chỉ Số Đo Lường Thành Công (Success Metrics)

1. Hạ tầng & Vận hành: Giảm P99 Latency của API lõi từ $8\text{s} \rightarrow < 500\text{ms}$.
2. Khả năng phục hồi: Tỷ lệ lỗi gửi email không làm ảnh hưởng đến tỷ lệ thành công của HTTP response (tỷ lệ lỗi $503 \rightarrow 0\%$).
3. Tần suất phát hành: Giảm thời gian kiểm thử hồi quy (Regression Testing) từ 2 tuần xuống còn $< 2\text{ giờ}$ nhờ Characterization Tests tự động.
