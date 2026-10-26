# 08. API Style Architectural Decisions

## 1. Quyết Định API Style Cho 4 Kịch Bản Nghiệp Vụ

---

### Kịch bản A: Mobile App Xem Employee Profile + Salary + Leave Balance
- API Style lựa chọn: GraphQL (hoặc REST BFF - Backend For Frontend)
- Lý do kỹ thuật: 
  - Màn hình mobile cần tổng hợp dữ liệu từ 3 Bounded Context khác nhau (Employee, Payroll, Leave) trên một view duy nhất.
  - Giảm thiểu Over-fetching (chỉ lấy các trường cần render trên màn hình nhỏ) và Under-fetching (tránh việc mobile phải gọi 3 HTTP request liên tiếp qua kết nối mạng di động không ổn định).
- Security Model: OAuth 2.0 Bearer JWT. Ủy quyền ở mức trường dữ liệu (Field-level Authorization) kết hợp Query Depth & Complexity Limiting để chống tấn công DoS.
- Versioning / Evolution: Schema Evolution thông qua bổ sung field mới và đánh dấu `@deprecated` trên các field cũ, không làm vỡ các phiên bản app mobile cũ chưa kịp cập nhật.
- Observability: Đo lường Resolver execution time per field, theo dõi Query Complexity Score và nạp `traceId` vào GraphQL extensions.
- Trade-off: Chi phí CPU trên gateway tăng do phân tích AST (Abstract Syntax Tree) của query; độ phức tạp caching HTTP cao hơn so với RESTful endpoint.

---

### Kịch bản B: Internal Payroll Service Gọi Tax Calculation Service
- API Style lựa chọn: gRPC (HTTP/2 + Protocol Buffers)
- Lý do kỹ thuật:
  - Giao tiếp Service-to-Service nội bộ yêu cầu thông lượng cực cao và độ trễ cực thấp trong suốt quá trình tính toán lương cho hàng trăm ngàn nhân viên.
  - Serialization nhị phân (Binary Serialization) của Protobuf nhỏ hơn 4–6 lần so với JSON, giảm tải CPU parsing và băng thông mạng.
  - Strongly-typed contract được sinh tự động (Code-generated stubs) đảm bảo tính toàn vẹn giữa client và server.
- Security Model: mTLS (Mutual TLS) giữa các Pods trong hạ tầng nội bộ (Service Mesh / Istio), xác thực danh tính service qua SPIFFE ID.
- Versioning / Evolution: Tuân thủ quy tắc Protobuf: Không thay đổi tag number của các field hiện hữu, chỉ thêm field mới với tag number kế tiếp.
- Observability: Tích hợp gRPC Interceptors để tự động propagate W3C Trace Context và xuất các metrics: `grpc_server_handled_total`, `grpc_server_handling_seconds`.
- Trade-off: Khó debug bằng các công cụ thông thường (như curl) nếu không có reflection hoặc grpcui; yêu cầu cấu hình Load Balancing ở tầng L7 (gRPC connection multiplexing trên HTTP/2).

---

### Kịch bản C: HR Dashboard Realtime Hiển Thị Tiến Độ Tính Lương
- API Style lựa chọn: Server-Sent Events (SSE)
- Lý do kỹ thuật:
  - Luồng dữ liệu mang tính một chiều (Unidirectional: Server $\rightarrow$ Client) thông báo phần trăm tiến độ hoàn tất (ví dụ: `45%`, `80%`, `100%`).
  - SSE chạy trực tiếp trên nền tảng HTTP chuẩn, tương thích hoàn toàn với HTTP/2 Multiplexing, tự động hỗ trợ cơ chế Reconnect của trình duyệt (thông qua `Last-Event-ID`).
  - Nhẹ hơn và quản lý kết nối đơn giản hơn đáng kể so với WebSocket (không cần duy trì giao thức song công hai chiều).
- Security Model: Cookie-based HttpOnly Secure Session hoặc Bearer Token truyền qua `EventSource` polyfill header.
- Versioning / Evolution: Định dạng bản tin event dạng JSON linh hoạt theo chuẩn CloudEvents (`id`, `event`, `data`).
- Observability: Giám sát số lượng kết nối đồng thời (`active_sse_connections`) và độ trễ phân phối sự kiện.
- Trade-off: Chỉ truyền được dữ liệu dạng text/UTF-8; không phù hợp nếu client cần gửi ngược dữ liệu liên tục về server trên cùng một kênh.

---

### Kịch bản D: Public Integration API Cho Khách Hàng Enterprise
- API Style lựa chọn: RESTful API (OpenAPI 3.1 Specification)
- Lý do kỹ thuật:
  - REST qua HTTP/JSON là tiêu chuẩn phổ quát nhất trong ngành, dễ dàng tích hợp với mọi hệ thống ERP/HRM của khách hàng doanh nghiệp (SAP, Workday).
  - Tận dụng hạ tầng HTTP Caching tiêu chuẩn (`ETag`, `Cache-Control`) và HTTP Status Codes ngữ nghĩa (`200`, `201`, `400`, `404`, `409`, `429`).
- Security Model: OAuth 2.0 Client Credentials Grant (máy-máy) kết hợp API Key và IP Whitelisting; mã hóa bắt buộc qua TLS 1.3.
- Versioning / Evolution: URL Path Versioning (ví dụ: `/api/v1/enterprises/...`) để cam kết tính tương thích ngược lâu dài (LTS) theo hợp đồng SLA dịch vụ.
- Observability: Giám sát qua API Gateway logs, phân đoạn metric theo `client_id`, theo dõi P99 latency và quota tiêu thụ.
- Trade-off: Dễ phát sinh over-fetching/under-fetching nếu khách hàng có nhu cầu dữ liệu tùy biến cao; tốn chi phí băng thông hơn gRPC.

---

## 2. Bảng Tổng Hợp So Sánh Kiến Trúc API

| Tiêu chí | Kịch bản A (Mobile App) | Kịch bản B (Internal Tax Call) | Kịch bản C (Realtime Progress) | Kịch bản D (Enterprise Public API) |
| :--- | :--- | :--- | :--- | :--- |
| API Style | GraphQL / BFF | gRPC | Server-Sent Events (SSE) | RESTful (OpenAPI 3.1) |
| Giao thức mạng | HTTP/1.1 hoặc HTTP/2 | HTTP/2 (Multiplexed) | HTTP/1.1 hoặc HTTP/2 | HTTP/1.1 hoặc HTTP/2 |
| Payload Format | JSON | Protobuf (Binary) | Text / JSON stream | JSON |
| Direction | Request-Response | Request-Response / Streaming | Unidirectional (Server-to-Client) | Request-Response |
| Độ trễ (Latency) | Trung bình (do parsing) | Cực thấp (sub-millisecond) | Thấp (Streaming) | Trung bình |
| Khả năng Cache | Khó cache HTTP public | Không áp dụng HTTP cache | Không áp dụng | Rất tốt (HTTP Proxies / CDN) |
