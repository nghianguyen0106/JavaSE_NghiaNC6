# Week 6: Software Architecture & Microservice System Design

## Danh Mục Các Deliverables Hoàn Thành

| STT | Tài liệu bàn giao | Nội dung kỹ thuật cốt lõi |
| :---: | :--- | :--- |
| 1 | [01-architecture-tradeoff.md](file:///c:/Users/ADMIN/Desktop/learn/JavaSE/JavaSE_NghiaNC6/Week6/01-architecture-tradeoff.md) | Phân tích đánh giá Monolith vs Modular Monolith vs Microservices; tiêu chí trích xuất Notification, Reporting và cô lập Payroll. |
| 2 | [02-ddd-boundary.md](file:///c:/Users/ADMIN/Desktop/learn/JavaSE/JavaSE_NghiaNC6/Week6/02-ddd-boundary.md) | Thiết kế 4 Bounded Contexts, Aggregate Roots, Invariants và phân tích hiện tượng đa nghĩa (Polysemy) của thực thể `Employee`. |
| 3 | [03-event-driven-design.md](file:///c:/Users/ADMIN/Desktop/learn/JavaSE/JavaSE_NghiaNC6/Week6/03-event-driven-design.md) | Kiến trúc Event-Driven xử lý Payroll, Transactional Outbox Pattern, Deduplication Consumer và chiến lược xử lý DLQ. |
| 4 | [04-system-design-process.md](file:///c:/Users/ADMIN/Desktop/learn/JavaSE/JavaSE_NghiaNC6/Week6/04-system-design-process.md) | Quy trình System Design hoàn chỉnh cho hệ thống 50k tenants, 5M employees; ước tính Capacity (Throughput, QPS, Storage 7 năm). |
| 5 | [05-caching-design.md](file:///c:/Users/ADMIN/Desktop/learn/JavaSE/JavaSE_NghiaNC6/Week6/05-caching-design.md) | Ma trận Caching Decision cho 5 loại dữ liệu nghiệp vụ, thiết kế Cache Key Schema và cơ chế chống Cache Stampede/Penetration. |
| 6 | [06-nfr-observability.md](file:///c:/Users/ADMIN/Desktop/learn/JavaSE/JavaSE_NghiaNC6/Week6/06-nfr-observability.md) | Định nghĩa 5 SLI / 5 SLO, 5 Actionable Alerts, phân loại Technical vs Business metrics và Correlation ID Flow qua W3C TraceContext. |
| 7 | [07-security-threat-model.md](file:///c:/Users/ADMIN/Desktop/learn/JavaSE/JavaSE_NghiaNC6/Week6/07-security-threat-model.md) | Bảng mô hình đe dọa STRIDE & OWASP Top 10 cho 4 endpoint nhạy cảm, cơ chế phòng chống IDOR/BOLA và Data-Level Authorization. |
| 8 | [08-api-style-decision.md](file:///c:/Users/ADMIN/Desktop/learn/JavaSE/JavaSE_NghiaNC6/Week6/08-api-style-decision.md) | Ma trận quyết định API Style (REST, GraphQL, gRPC, SSE) cho 4 kịch bản tích hợp và phân tích trade-offs. |
| 9 | [09-modernization-plan.md](file:///c:/Users/ADMIN/Desktop/learn/JavaSE/JavaSE_NghiaNC6/Week6/09-modernization-plan.md) | Kế hoạch hiện đại hóa hệ thống 8 năm tuổi theo Strangler Fig, Dual-Write/CDC Data Migration, Rollback Plan và Sổ đăng ký nợ kỹ thuật. |
| 10 | [capstone-architecture.md](file:///c:/Users/ADMIN/Desktop/learn/JavaSE/JavaSE_NghiaNC6/Week6/capstone-architecture.md) | Tổng hợp kiến trúc toàn cảnh Capstone: Multi-tenancy Hybrid, Scalable Batch Engine 1.000 chunk, và Definition of Done verification. |
