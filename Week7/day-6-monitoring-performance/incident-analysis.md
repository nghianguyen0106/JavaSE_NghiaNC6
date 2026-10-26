# Incident Drill Analysis: Inventory Service Slowdown

## 1. Bối Cảnh Sự Cố (Incident Scenario)
- Tình huống: `Inventory Service` bị chậm $3\text{ giây}$ trong $20\%$ tổng số requests.
- Triệu chứng ghi nhận tại Order Service:
  - Latency checkout P95 tăng vọt từ $300\text{ms} \rightarrow 1.8\text{s}$.
  - Latency checkout P99 tăng vọt từ $700\text{ms} \rightarrow 6\text{s}$.
  - Chỉ số Retry Count tăng đột biến.
  - Circuit Breaker của kết nối Inventory bắt đầu chuyển trạng thái giữa `HALF_OPEN` và `OPEN`.
  - Mục tiêu SLA: Endpoint `GET /orders/{id}` bắt buộc duy trì $P95 < 300\text{ms}$.

---

## 2. Trả Lời Chi Tiết 7 Câu Hỏi Chẩn Đoán Cốt Lõi

### 1. Alert nào cần fire đầu tiên?
- Alert: `InventoryDependencyLatencyHigh` (hoặc `CircuitBreakerSlowCallRateExceeded`).
- Lý do: Đây là chỉ số gốc (Leading Indicator) phản ánh trực tiếp nguyên nhân gây ra sự cố khi thời gian phản hồi của dependency vượt quá ngưỡng cho phép ($> 1000\text{ms}$). Tiếp theo đó mới tới các alert triệu chứng (Lagging Indicators) như `CheckoutApiLatencyP99Breached`.

### 2. Dashboard nào xem trước?
- Dashboard xem trước: Dependency Dashboard (Downstream Health).
- Lý do: Cho phép đối chiếu ngay lập tức xem sự cố bắt nguồn từ hạ tầng nội bộ của Order Service hay do các dịch vụ phụ thuộc bên ngoài (Inventory / Payment / Database) gây ra. Dashboard này hiển thị ngay Latency, Failure Rate và trạng thái Circuit Breaker của từng external call.

### 3. Trace nào giúp xác minh Inventory là dependency chậm?
- Trace Span: Tìm kiếm các Trace có tổng thời gian thực thi (Trace Duration) $> 3\text{s}$ trên endpoint `POST /api/orders/checkout`.
- Bằng chứng trong Distributed Trace: Quan sát timeline hiển thị span cha `http:checkout` có thời lượng $3.2\text{s}$, trong đó span con `feign:reserveInventory` chiếm trọn $3.0\text{s}$, còn các span con nội bộ khác (`jdbc:saveOrder`, `kafka:publish`) chỉ tốn vài mili-giây. Điều này chứng minh tuyệt đối nút thắt cổ chai nằm ở Inventory Service.

### 4. Retry có đang làm traffic tệ hơn không?
- CÓ, RETRY ĐANG LÀM TRẦM TRỌNG HÓA SỰ CỐ (Retry Storm).
- Lý do: Khi Inventory Service đang bị nghẽn (do quá tải DB hoặc CPU), việc các request chậm bị timeout và tự động retry thêm 2 lần sẽ làm nhân ba tổng lượng traffic đánh vào Inventory Service ($20\% \times 3 = 60\%$ tải bổ sung). Điều này ngăn cản Inventory Service tự hồi phục.
- Hành động điều chỉnh: Phải tắt retry đối với các lỗi Timeout (chỉ retry khi có lỗi mạng rớt kết nối rõ ràng) hoặc áp dụng Exponential Backoff kèm Full Jitter để phân tán đỉnh tải.

### 5. Get Order có bị ảnh hưởng không? Nếu có, bulkhead sai ở đâu?
- Nếu thiết kế chuẩn: `GET /orders/{id}` HOÀN TOÀN KHÔNG BỊ ẢNH HƯỞNG và giữ vững $P95 < 300\text{ms}$.
- Nếu `GET /orders` bị chậm theo: Chứng tỏ cấu hình Bulkhead đã bị sai hoặc thiếu:
  - *Lỗi sai:* Không cấu hình Semaphore / ThreadPool Bulkhead riêng biệt cho Feign Client gọi sang Inventory, khiến các request checkout bị chậm chiếm dụng toàn bộ 200 worker threads của Tomcat Web Server. Hậu quả là các request `GET /orders` không còn thread trống để tiếp nhận và bị nghẽn tại hàng đợi của server (Thread Starvation).

### 6. Immediate mitigation (Biện pháp xử lý khẩn cấp tức thời là gì)?
1. Chủ động mở Circuit Breaker (Force Open): Đưa trạng thái Circuit Breaker của Inventory Service sang `OPEN` để kích hoạt Fail-Fast ngay lập tức, ngắt toàn bộ các cuộc gọi sang Inventory Service, loại bỏ thời gian chờ 3 giây.
2. Kích hoạt Fallback: Tạm thời chuyển sang chế độ chấp nhận đặt hàng với trạng thái `PENDING_INVENTORY_VERIFICATION` (kiểm tra tồn kho bất đồng bộ sau qua queue) thay vì kiểm tra đồng bộ trong request checkout.
3. Tạm thời tắt Retry: Chuyển cấu hình `resilience4j.retry.instances.inventoryService.max-attempts=1` thông qua Spring Cloud Config / Actuator Refresh mà không cần restart pod.

### 7. Long-term fix (Giải pháp khắc phục dài hạn là gì)?
1. Chuyển đổi giao tiếp sang Asynchronous Event-Driven: Thay thế cuộc gọi đồng bộ REST (Synchronous Call) kiểm tra tồn kho tại thời điểm checkout bằng mô hình Reservation Event / Saga Pattern (Chấp nhận đơn hàng trước, trừ tồn kho bất đồng bộ; nếu hết hàng thì kích hoạt Compensation Transaction hoàn tiền và hủy đơn).
2. Tối ưu hóa Inventory Service: Đánh chỉ mục (Index) database của Inventory Service, bổ sung Redis Cache cho các mặt hàng có tần suất truy vấn cao.
3. Thắt chặt TimeLimiter & Bulkhead: Giảm timeout từ $1000\text{ms}$ xuống $600\text{ms}$, giới hạn tối đa 30 kết nối đồng thời tới Inventory Service.
