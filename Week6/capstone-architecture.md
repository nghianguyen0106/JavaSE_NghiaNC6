# Capstone Architecture: Enterprise Multi-Tenant Payroll SaaS

## 1. Tổng Quan Kiến Trúc Toàn Hệ Thống (High-Level Architecture)

Kiến trúc kết hợp giữa **Event-Driven Microservices** cho các tác vụ tính toán bất đồng bộ quy mô lớn và **Modular Monolith Core** cho các nghiệp vụ quản trị CRUD có tính toàn vẹn quan hệ cao:

```
                                      [Web Dashboard / Mobile Client]
                                                     |
                                                     v (HTTPS / TLS 1.3)
                                      +-------------------------------+
                                      |          API Gateway          |
                                      | - Spring Cloud Gateway        |
                                      | - OAuth 2.0 Resource Server   |
                                      | - Rate Limiter / WAF          |
                                      +-------------------------------+
                                                     |
             +---------------------------------------+---------------------------------------+
             |                                       |                                       |
             v (GraphQL / REST)                      v (REST / gRPC)                         v (SSE)
+-------------------------+             +-------------------------+             +-------------------------+
|    Core Domain Service  |             |  Payroll Calculation    |             |    Progress Tracking    |
|   (Modular Monolith)    |             |       Core Service      |             |         Service         |
| - Identity Context      |             | - Spring Batch Job      |             +-------------------------+
| - Employee Context      |             | - Distributed Lock      |                          ^
| - Leave Context         |             +-------------------------+                          |
+-------------------------+                          |                                       |
             |                                       v (Task Event)                          |
             | (Outbox)                +---------------------------+                         |
             +------------------------>|    Apache Kafka Cluster   |-------------------------+
                                       +---------------------------+
                                                     |
                         +---------------------------+---------------------------+
                         |                                                       |
                         v (Consumer Group)                                      v (Consumer Group)
            +-------------------------+                             +-------------------------+
            |  Notification Service   |                             |    Reporting Service    |
            | - Email / SMS Dispatch  |                             | - Debezium CDC Pipeline |
            | - Idempotent Consumer   |                             | - OLAP / Elasticsearch  |
            +-------------------------+                             +-------------------------+
```

---

## 2. Chiến Lược Cô Lập Đa Khách Thuê (Multi-Tenancy Isolation Strategy)

Hệ thống phục vụ 50,000 tenants với 5 triệu nhân viên, áp dụng mô hình **Hybrid Multi-Tenancy**:

1. **Standard Tenants (Small & Medium): Shared Database - Shared Schema**
   - Mọi bảng dữ liệu đều chứa cột bắt buộc: `tenant_id VARCHAR(64) NOT NULL`.
   - Cơ chế bảo vệ: Sử dụng **PostgreSQL Row-Level Security (RLS)** kết hợp Hibernate Tenant Filter. Ứng dụng tự động thiết lập context session: `SET LOCAL app.current_tenant = :tenantId` trước khi thực thi query.
2. **Enterprise Tenants (Quy mô $\ge 10,000$ nhân viên): Dedicated Database (Database-per-tenant)**
   - Các tenant đặc thù được định tuyến kết nối sang Database Cluster riêng biệt qua Dynamic Routing DataSource (`AbstractRoutingDataSource`), loại bỏ hoàn toàn hiện tượng "Noisy Neighbor" gây nghẽn tài nguyên cho các tenant nhỏ.

---

## 3. Kiến Trúc Tính Lương Bất Đồng Bộ Quy Mô Lớn (Scalable Payroll Engine)

```
[HR Admin: RunPayroll] 
       |
       v
[Payroll Core Service] ---> (Validate + Distributed Lock: Redisson)
       |
       | Publish Job Partition Task
       v
[Apache Kafka: Topic `payroll.calculation.jobs`] (Partitions: 32)
       |
       +---> [Worker Pod 1] ---> Chunk 1..1000 records   ---> Batch INSERT DB (HikariCP)
       |
       +---> [Worker Pod 2] ---> Chunk 1001..2000 records ---> Batch INSERT DB (HikariCP)
       |
       +---> [Worker Pod N] ---> Chunk N..N+1000 records  ---> Batch INSERT DB (HikariCP)
```

- **Mô hình xử lý:** Chunk-oriented Processing với kích thước Chunk tối ưu = **1,000 bản ghi**.
- **Cơ chế Commit:** Mỗi Chunk hoàn tất được commit trong một local database transaction riêng biệt, đi kèm lưu lại trạng thái tiến trình (StepExecution Offset) vào `JobRepository`.
- **Khả năng tự phục hồi (Restartability):** Trường hợp máy chủ sập nguồn giữa chừng, Job tự động nhận diện điểm dừng và xử lý tiếp từ bản ghi chưa commit, không gây trùng lặp dữ liệu và không phải chạy lại từ đầu.

---

## 4. Bảng Đánh Giá Mức Độ Đáp Ứng Các Yêu Cầu Định Nghĩa Hoàn Thành (DoD Checklist)

| Hạng mục kiểm tra (DoD) | Tài liệu minh chứng cụ thể | Trạng thái kỹ thuật |
| :--- | :--- | :---: |
| **Business Requirement & NFR rõ ràng** | [04-system-design-process.md](file:///c:/Users/ADMIN/Desktop/learn/JavaSE/JavaSE_NghiaNC6/Week6/04-system-design-process.md) | **ĐẠT** |
| **Bounded Contexts & Data Ownership** | [02-ddd-boundary.md](file:///c:/Users/ADMIN/Desktop/learn/JavaSE/JavaSE_NghiaNC6/Week6/02-ddd-boundary.md) | **ĐẠT** |
| **Event Flow & Async Failure Strategy (DLQ)** | [03-event-driven-design.md](file:///c:/Users/ADMIN/Desktop/learn/JavaSE/JavaSE_NghiaNC6/Week6/03-event-driven-design.md) | **ĐẠT** |
| **Cache Strategy & Invalidation Plan** | [05-caching-design.md](file:///c:/Users/ADMIN/Desktop/learn/JavaSE/JavaSE_NghiaNC6/Week6/05-caching-design.md) | **ĐẠT** |
| **Security & STRIDE Threat Model** | [07-security-threat-model.md](file:///c:/Users/ADMIN/Desktop/learn/JavaSE/JavaSE_NghiaNC6/Week6/07-security-threat-model.md) | **ĐẠT** |
| **API Style Decision Matrix** | [08-api-style-decision.md](file:///c:/Users/ADMIN/Desktop/learn/JavaSE/JavaSE_NghiaNC6/Week6/08-api-style-decision.md) | **ĐẠT** |
| **Observability Plan & SLI/SLO** | [06-nfr-observability.md](file:///c:/Users/ADMIN/Desktop/learn/JavaSE/JavaSE_NghiaNC6/Week6/06-nfr-observability.md) | **ĐẠT** |
| **Modernization & Strangler Fig Migration** | [09-modernization-plan.md](file:///c:/Users/ADMIN/Desktop/learn/JavaSE/JavaSE_NghiaNC6/Week6/09-modernization-plan.md) | **ĐẠT** |
| **Trade-off Analysis (Monolith vs Microservices)** | [01-architecture-tradeoff.md](file:///c:/Users/ADMIN/Desktop/learn/JavaSE/JavaSE_NghiaNC6/Week6/01-architecture-tradeoff.md) | **ĐẠT** |
