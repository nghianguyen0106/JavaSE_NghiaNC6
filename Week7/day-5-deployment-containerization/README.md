# Day 5: Deployment & Containerization Review Questions

## Trả Lời Chi Tiết 10 Câu Hỏi Kỹ Thuật

### 1. Docker container start được có đồng nghĩa service sẵn sàng nhận traffic không?
- KHÔNG.
- Lý do: Lệnh khởi chạy container thành công chỉ phản ánh tiến trình hệ điều hành (Linux Process / JVM) đã được cấp phát PID. Tại thời điểm này, Spring Boot Application Context có thể vẫn đang trong giai đoạn khởi tạo (nạp Bean definitions, thiết lập Database Connection Pool, khởi tạo Kafka Consumers, chạy Liquibase/Flyway migrations). Nếu gửi traffic ngay, toàn bộ request sẽ bị lỗi `502 Bad Gateway` hoặc `Connection Refused`. Cần sử dụng Readiness Probe để xác nhận ứng dụng đã hoàn tất warmup và sẵn sàng phục vụ.

### 2. Liveness và readiness khác nhau thế nào?
- Liveness Probe: Xác định xem process bên trong container còn hoạt động bình thường hay đã rơi vào trạng thái chết (Deadlock, OOM, infinite loop). Nếu Liveness thất bại, Kubernetes sẽ KILL và RESTART container.
- Readiness Probe: Xác định xem container tại thời điểm hiện tại có đủ năng lực xử lý HTTP request hay không. Nếu Readiness thất bại, Kubernetes sẽ TẠM THỜI GỠ POD KHỎI SERVICE ENDPOINTS (ngừng route traffic), nhưng KHÔNG RESTART POD.

### 3. Vì sao liveness không nên fail chỉ vì Kafka hoặc Payment Service down?
- Vì việc restart pod cục bộ không thể khắc phục được lỗi của hệ thống bên ngoài (Third-party / External Broker). Nếu Kafka down 30 phút, toàn bộ cụm Order Service sẽ rơi vào vòng lặp restart liên tục (CrashLoopBackOff), tiêu tốn tài nguyên CPU/RAM để khởi động lại vô ích, và khiến toàn bộ các chức năng nội bộ không phụ thuộc Kafka (như tra cứu đơn hàng) cũng bị sập theo.

### 4. Vì sao rollback image không đủ nếu migration đã drop column?
- Rollback container image chỉ khôi phục mã nguồn ứng dụng về phiên bản cũ. Nếu database migration trước đó đã thực thi lệnh DDL phá hủy dữ liệu (`DROP COLUMN status`), mã nguồn phiên bản cũ khi khởi chạy lại sẽ thực thi câu lệnh SQL `SELECT status FROM orders ...` và bị ném ngoại lệ `SQLException: Column "status" does not exist`. Toàn bộ dữ liệu của cột cũ đã bị xóa vĩnh viễn khỏi ổ đĩa vật lý và không thể khôi phục chỉ bằng việc đổi image tag.

### 5. Khi rolling deployment, old/new version có thể cùng chạy bao lâu?
- Thời gian chạy song song phụ thuộc vào tham số `terminationGracePeriodSeconds`, số lượng replica, và thời gian chờ draining connections của load balancer. Thông thường, old và new version sẽ cùng tồn tại song song từ vài phút đến vài chục phút (trong trường hợp Canary có thể kéo dài hàng giờ). Mọi thiết kế API và Database Schema bắt buộc phải tương thích ngược (Backward Compatible) trong suốt khoảng thời gian này.

### 6. ConfigMap và Secret khác nhau thế nào?
- ConfigMap: Dùng để lưu trữ các tham số cấu hình phi nhạy cảm dưới dạng plain-text (ví dụ: Log level, Endpoint host, Port).
- Secret: Dùng để lưu trữ thông tin nhạy cảm (Passwords, Tokens, Keys). Về mặt kỹ thuật trong Kubernetes, Secret được mã hóa ở tầng lưu trữ (Encryption at Rest trong etcd), chỉ được mount vào pod dưới dạng tmpfs (in-memory volume), và được kiểm soát quyền truy cập chặt chẽ hơn qua RBAC.

### 7. Vì sao không được commit secret thật vào Git, kể cả private repository?
- Vì lịch sử commit của Git là bất biến (Git Commit History lưu vĩnh viễn bản sao của secret). Private repository có thể bị rò rỉ qua các vector: Backup repository không mã hóa, cấp quyền cho nhân sự nghỉ việc, tài khoản developer bị chiếm quyền, hoặc clone về máy cá nhân không có bảo vệ ổ cứng. Khi secret đã vào Git, việc xóa bỏ hoàn toàn (purge commit) đòi hỏi viết lại toàn bộ Git Tree (`git filter-repo`) rất tốn kém và rủi ro.

### 8. Nếu readiness của toàn bộ Order Service pods fail vì Inventory Service down, business impact là gì?
- Toàn bộ Pods của `Order Service` đều bị đánh dấu `Unready` $\rightarrow$ Kubernetes Service Endpoints rỗng $\rightarrow$ API Gateway và Client nhận lỗi `503 Service Unavailable` trên toàn bộ hệ thống Order. Khách hàng không thể thực hiện bất kỳ thao tác nào, kể cả xem lại lịch sử đơn hàng cũ (vốn không hề cần kết nối tới Inventory Service). Đây là lỗi thiết kế Cascading Failure kinh điển do đưa dependency bên ngoài vào Readiness Probe.

### 9. Nếu canary release làm payment mismatch tăng, cần rollback app hay disable feature flag trước?
- DISABLE FEATURE FLAG TRƯỚC.
- Lý do: Tắt Feature Flag thông qua hệ thống quản trị cấu hình động (LaunchDarkly/Unleash/Spring Cloud Bus) chỉ mất vài mili-giây đến vài giây để toàn bộ các pods cập nhật mà không cần thay đổi trạng thái container. Việc rollback application yêu cầu tái điều phối traffic, restart pods hoặc deploy lại pipeline có thể mất từ 5 đến 15 phút, khiến số lượng đơn hàng bị sai lệch thanh toán tiếp tục tăng lên trong thời gian đó.

### 10. Metric nào cần xem trước khi kết luận deploy mới gây incident?
Cần đối chiếu đồng thời bộ chỉ số:
1. HTTP 5xx Error Rate: Tỷ lệ lỗi 5xx của Canary pods so với Baseline pods đang chạy song song.
2. P99 / P95 Latency: Độ trễ phản hồi của phiên bản mới so với phiên bản cũ dưới cùng một mức tải.
3. Database Connection Pool Saturation & Lock Waits: Tránh trường hợp deploy mới sinh ra câu lệnh N+1 hoặc thiếu index gây nghẽn DB.
4. JVM Memory & GC Pause Times: Xác định xem code mới có gây tăng đột biến allocation rate hoặc rò rỉ bộ nhớ hay không.
