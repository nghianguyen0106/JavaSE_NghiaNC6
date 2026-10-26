# Deployment Strategy — Order Service

## 1. Image strategy
- Image naming/tagging: Tuân thủ chuẩn Semantic Versioning kết hợp Git SHA: `registry.internal/order-service:1.2.0-git-b7c1a9e`. Tuyệt đối cấm sử dụng tag `:latest` trên môi trường Production.
- Build method: Docker Multi-stage build kết hợp Spring Boot Layertools (`layertools extract`) để phân tách các layer dependencies, loader và application code, tối ưu hóa caching của container registry.
- Base image: `eclipse-temurin:17-jre-jammy` (chỉ chứa JRE tối giản, loại bỏ hoàn toàn JDK toolchain và package build không cần thiết nhằm giảm attack surface).
- Non-root policy: Khởi tạo user và group chuyên dụng (`appuser:10001`, `appgroup:10001`). Container thực thi với chỉ thị `USER 10001:10001`, loại bỏ quyền root trong Linux namespace.

---

## 2. Runtime configuration
- ConfigMap fields: Các tham số môi trường phi nhạy cảm: `DB_HOST`, `DB_PORT`, `DB_NAME`, `KAFKA_BOOTSTRAP_SERVERS`, `SPRING_PROFILES_ACTIVE`.
- Secret fields: Các thông tin bảo mật nhạy cảm: `DB_USERNAME`, `DB_PASSWORD`, `KAFKA_SASL_PASSWORD`, `JWT_PUBLIC_KEY`.
- Fields not allowed in Git: Toàn bộ mật khẩu plain-text, API Keys của bên thứ ba, private keys mã hóa dữ liệu. Các trường này chỉ được đưa vào runtime thông qua HashiCorp Vault hoặc Sealed Secrets của Kubernetes.

---

## 3. Health checks
- Liveness endpoint: `/actuator/health/liveness` (Probe kiểm tra process JVM còn sống, thread scheduling không bị deadlock).
- Readiness endpoint: `/actuator/health/readiness` (Probe kiểm tra service đã hoàn tất nạp context và sẵn sàng nhận HTTP traffic từ Kube-Proxy/Ingress).
- Why external dependencies are excluded/included:
  - Liveness BẮT BUỘC LOẠI TRỪ external dependencies: Nếu PostgreSQL hoặc Payment Gateway bị lỗi mạng mà Liveness đánh dấu FAIL, Kubernetes sẽ restart pod liên tục (CrashLoopBackOff). Việc restart pod không khắc phục được lỗi mạng bên ngoài mà chỉ làm trầm trọng thêm tình trạng quá tải khởi động của hệ thống.
  - Readiness CÓ THỂ BAO GỒM kiểm tra kết nối cục bộ (Postgres/Kafka): Khi kết nối DB bị đứt, Pod đánh dấu Unready để Kube-Proxy ngừng route traffic tới Pod đó, chờ kết nối phục hồi mà không phải kill process.

---

## 4. Rollout strategy
- Rolling / Canary: Áp dụng Canary Deployment kết hợp Service Mesh (Istio / Ingress Canary).
- Canary percentage: Bắt đầu với $10\%$ traffic định tuyến tới Canary Pods trong vòng 15 phút. Sau đó tăng lên $50\%$ trong 15 phút kế tiếp trước khi chuyển $100\%$ sang phiên bản mới.
- Duration: Tổng thời gian quan sát Canary tối thiểu là 30 phút.
- Metrics monitored: 
  - HTTP 5xx Error Rate ($< 0.1\%$).
  - P99 Latency ($< 800\text{ms}$).
  - Payment Mismatch / Unprocessed Order Count ($= 0$).

---

## 5. Rollback criteria
Kích hoạt hủy bỏ Canary và tự động rollback $100\%$ về phiên bản cũ ngay lập tức khi:
1. Tỷ lệ lỗi HTTP 5xx trên Canary pods vượt quá $1\%$ trong cửa sổ 2 phút liên tiếp.
2. P99 Latency của endpoint `/api/orders` tăng gấp 2 lần so với baseline hiện tại.
3. Số lượng ngoại lệ `DeadlockLoserDataAccessException` hoặc `SerializationException` trên database vượt quá 5 lỗi/phút.

---

## 6. Database migration (Expand / Contract Pattern)
Áp dụng mẫu Parallel Run / Expand-Contract để đảm bảo Zero-Downtime Deployment:
- Expand step: Thêm cột mới `payment_status VARCHAR(32) NULL` vào bảng `orders`. Cột `status` cũ vẫn được giữ nguyên.
- Compatibility step: Mã nguồn phiên bản mới (v1.2.0) đọc dữ liệu từ cả 2 cột; khi ghi dữ liệu mới sẽ thực thi Dual-Write ghi đồng thời vào cả cột cũ `status` và cột mới `payment_status`.
- Backfill: Chạy background script di chuyển và ánh xạ dữ liệu lịch sử từ cột cũ sang cột mới cho toàn bộ các đơn hàng cũ.
- Cutover: Chuyển đổi mã nguồn đọc và ghi hoàn toàn trên cột mới `payment_status`.
- Contract / Remove step: Sau 2 chu kỳ release ổn định, thực thi lệnh DDL drop cột cũ `status`.

---

## 7. Rollback plan
- Application rollback: Tại Kubernetes Ingress/VirtualService, thiết lập lại trọng số traffic: `weight: 100` cho version cũ, `weight: 0` cho Canary.
- Feature-flag rollback: Tắt feature flag `features.order.new-status-flow.enabled` thông qua Unleash / LaunchDarkly trong $< 10\text{ giây}$ mà không cần redeploy ứng dụng.
- Data repair: Nhờ áp dụng Expand-Contract, cấu trúc bảng không bị phá vỡ (Non-destructive DDL), do đó không cần restore database backup, loại bỏ hoàn toàn rủi ro mất dữ liệu phát sinh trong giai đoạn triển khai.
