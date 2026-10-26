# Day 1: API Gateway, Externalized Configuration & Service Communication

## 1. Trả Lời Câu Hỏi Tự Review Kỹ Thuật

### Câu 1: Gateway khác Load Balancer ở điểm nào?
- Load Balancer (L4/L7 LB như AWS ALB, NGINX, HAProxy): Tập trung thuần túy vào việc phân phối lưu lượng truy cập mạng (Network Traffic Distribution) giữa các instances của cùng một cụm dịch vụ nhằm tối ưu hóa thông lượng và độ sẵn sàng. Không có khả năng can thiệp sâu vào application domain logic hoặc token payload.
- API Gateway (L7 Application Gateway như Spring Cloud Gateway, Kong): Đóng vai trò là Single Point of Entry cho toàn bộ hệ thống Microservices. Ngoài chức năng cân bằng tải, Gateway cung cấp các chức năng biên (Cross-Cutting Edge Concerns): API Composition / Aggregation, Protocol Translation (HTTP sang gRPC), Global Rate Limiting, Authentication & Token Relaying, Centralized Circuit Breaker, và Dynamic Request Routing.

### Câu 2: Gateway có thay thế authorization trong service không?
- KHÔNG THAY THẾ ĐƯỢC.
- Nguyên lý phân tầng bảo mật (Defense in Depth):
  - Gateway thực hiện Coarse-Grained Authorization: Xác thực tính hợp lệ của Token JWT (Signature, Expiration, Issuer) và kiểm tra quyền ở mức endpoint thô (ví dụ: endpoint `/admin/` chỉ chấp nhận token có role `ROLE_ADMIN`).
  - Downstream Services bắt buộc thực hiện Fine-Grained / Domain Authorization: Kiểm tra quyền sở hữu dữ liệu (Data-level authorization / ABAC) mà Gateway không thể biết. Ví dụ: User A có quyền `ORDER_READ` nhưng chỉ được đọc đơn hàng của chính User A (`order.userId == currentUserId`). Nếu đẩy logic này lên Gateway, Gateway sẽ bị "nhiễm" domain logic (Domain Leaking) và trở thành Monolith bottleneck.

### Câu 3: Config nào là secret?
- Toàn bộ các thông tin xác thực danh tính, thông tin mật mã học, và thông tin truy cập tài nguyên bảo mật được định nghĩa là Secret:
  - Database Credentials (Username, Password).
  - Private Keys (RSA / EC Private Keys dùng ký JWT).
  - OAuth 2.0 Client Secrets, Third-party API Keys (Stripe, Twilio, SendGrid).
  - TLS / SSL Keystore Passwords, Encryption Master Keys.
- Các giá trị này tuyệt đối không được lưu trữ dưới dạng plain-text trong mã nguồn hoặc commit vào Git repository.

### Câu 4: Nếu chạy Kubernetes, có bắt buộc cần Eureka không?
- KHÔNG BẮT BUỘC (Và khuyến nghị KHÔNG NÊN DÙNG).
- Lý do: Kubernetes đã có sẵn hạ tầng Service Discovery và Internal Load Balancing native ở tầng platform:
  - Kubernetes CoreDNS: Tự động phân giải tên miền service (ví dụ: `http://order-service:8080`) thành ClusterIP.
  - Kube-Proxy / IPVS / eBPF: Tự động điều phối traffic L4 cân bằng tải tới các Pods backend.
  - Việc nhúng thêm Eureka/Consul vào cụm Kubernetes làm tăng độ phức tạp vận hành không cần thiết (Dual Service Registry), tiêu tốn tài nguyên và dễ gây xung đột vòng đời pod với Kubernetes Controller.

### Câu 5: Nếu một service scale từ 1 lên 5 instances, client route request thế nào?
- Mô hình Client-Side Load Balancing (Spring Cloud LoadBalancer):
  1. Service Discovery (Eureka hoặc Kube API) cung cấp danh sách 5 IP/Port của các instance đang healthy cho Client.
  2. Client lưu cache danh sách instance này tại local memory.
  3. Khi gửi request, thư viện LoadBalancer tại Client áp dụng thuật toán (Round-Robin hoặc Weighted Response Time) để chọn trực tiếp 1 instance và thực thi kết nối TCP.
- Mô hình Server-Side Load Balancing (Kubernetes ClusterIP):
  1. Client luôn gửi request tới một địa chỉ ảo duy nhất: `http://order-service`.
  2. Kube-proxy (iptables / IPVS) chặn request ở tầng network và tự động định tuyến gói tin tới 1 trong 5 Pod IP đang sẵn sàng (Readiness Probe pass).
