# 02. JVM Diagnostics: Heap Dump vs Thread Dump

## 1. Phân tích 2 tình huống sự cố thực tế

### Scenario A — Memory Leak (Payroll Export OOM)
* **Bối cảnh:** Payroll export làm heap tăng từ 1GB lên 6GB. Sau Full GC, heap vẫn gần đầy và cuối cùng bị `java.lang.OutOfMemoryError: Java heap space`.
* **Artifact lấy trước:** **Heap Dump** (kèm cờ tự động `-XX:+HeapDumpOnOutOfMemoryError` hoặc chủ động dùng `jcmd <pid> GC.heap_dump`).
* **Quan sát gì trong artifact:**
  * **Dominator Tree:** Tìm object nào có **Retained Size** lớn nhất (chiếm phần lớn dung lượng bộ nhớ).
  * **Incoming References:** Lần theo GC Roots xem object đó đang bị biến static, singleton service, hay cache nào giữ lại không chịu nhả.
  * **Class Histogram:** Kiểm tra các class có số lượng instance tăng đột biến (ví dụ: `byte[]`, `HashMap$Node`, `EmployeeDetailDTO`).
* **Root Cause Hypothesis:**
  * Logic export đọc toàn bộ dữ liệu bảng lương của hàng chục ngàn nhân viên lên bộ nhớ cùng một lúc (`findAll()`) thay vì dùng paging/streaming.
  * Hoặc export xong lại lưu kết quả vào một static cache in-memory không có cơ chế hết hạn (eviction/TTL).
* **Mitigation ngắn hạn:**
  * Giới hạn dung lượng file export hoặc chặn query range quá lớn.
  * Tạm thời restart pod/service có kiểm soát khi heap chạm ngưỡng 80%.
* **Fix dài hạn:**
  * Chuyển flow export sang Stream processing (JPA Streaming / JDBC `fetchSize` hoặc Spring Batch) ghi trực tiếp ra disk/S3 theo chunk.
  * Sử dụng bounded cache có TTL và LRU (ví dụ Caffeine Cache) thay vì dùng static `ConcurrentHashMap`.

---

### Scenario B — Thread Starvation (Nghẽn kết nối Tax Service)
* **Bối cảnh:** API phản hồi cực chậm, p99 tăng vọt nhưng CPU máy chủ chỉ 30%. Cả 200 threads trong Tomcat thread pool đều bận.
* **Artifact lấy trước:** **Thread Dump** (lấy liên tiếp 3 lần cách nhau 5–10 giây bằng `jcmd <pid> Thread.print`).
* **Quan sát gì trong artifact:**
  * Trạng thái của 200 worker threads (`http-nio-8080-exec-*`).
  * Stack trace xem các thread đang dừng lại ở dòng code nào: Thường thấy ở trạng thái `WAITING` hoặc `TIMED_WAITING` tại tầng socket read (ví dụ `SocketInputStream.socketRead0()` gọi sang Tax Service).
  * Kiểm tra có deadlock hoặc tranh chấp synchronized/lock không (`BLOCKED`).
* **Root Cause Hypothesis:**
  * Dịch vụ bên ngoài (Tax Service) bị chậm hoặc treo, trong khi HTTP Client bên mình không đặt `ReadTimeout` (hoặc timeout quá dài như 30–60 giây).
  * 200 worker threads đều bị giữ chặt để chờ kết quả từ Tax Service, làm cạn kiệt Tomcat thread pool (Thread Pool Starvation).
* **Mitigation ngắn hạn:**
  * Cấu hình lại `connectTimeout` (2s) và `readTimeout` (3s) cho HTTP Client.
  * Bật Circuit Breaker (Resilience4j) để fail-fast nếu Tax Service tiếp tục lỗi.
* **Fix dài hạn:**
  * Áp dụng Bulkhead pattern: Tách riêng thread pool cho các cuộc gọi sang bên thứ ba, không dùng chung thread pool của Web container.
  * Chuyển các tác vụ đồng bộ không cần thiết sang bất đồng bộ (Async qua Message Queue / Kafka).

---

## 2. Checklist: Khi nào lấy Heap Dump, khi nào lấy Thread Dump?

| Tiêu chí | Thread Dump | Heap Dump |
| :--- | :--- | :--- |
| **Khi nào sử dụng?** | • API chậm, treo, timeout nhưng CPU thấp.<br>• Nghi ngờ Deadlock, Thread starvation, cạn DB connection pool.<br>• CPU tăng vọt 100% (cần tìm hot thread). | • Bộ nhớ Heap tăng liên tục không hạ sau Full GC.<br>• Xảy ra lỗi `OutOfMemoryError`.<br>• Cần tìm đối tượng nào đang rò rỉ bộ nhớ (leak). |
| **Mức độ tác động** | Cực kỳ nhẹ, chụp tức thì trong vài chục mili-giây, an toàn chạy trên Production. | **Tác động rất nặng!** JVM bị Stop-The-World trong suốt quá trình ghi file ra đĩa (vài chục giây với heap lớn). Có thể làm đứt kết nối hoặc sập service. |
| **Dữ liệu nhạy cảm** | Thường chỉ chứa tên class, method và số dòng code (an toàn). | **Có thể chứa dữ liệu nhạy cảm (PII):** Token, password, thông tin cá nhân của user lưu trong bộ nhớ. |

---

## 3. Runbook chẩn đoán an toàn trên Production

```bash
# 1. Tìm PID của ứng dụng Java
jcmd
# hoặc
jps -v

# 2. Thu thập Thread Dump an toàn (Chụp 3 lần cách nhau 5 giây để so sánh trạng thái thread)
jcmd <PID> Thread.print > /tmp/thread_dump_$(date +%H%M%S).txt
sleep 5
jcmd <PID> Thread.print > /tmp/thread_dump_$(date +%H%M%S).txt

# 3. Xem nhanh Class Histogram (Đếm số lượng object và dung lượng chiếm dụng - nhẹ hơn nhiều so với Heap Dump)
jcmd <PID> GC.class_histogram | head -n 30

# 4. Thu thập Heap Dump (CHỈ làm khi bắt buộc và nên cô lập instance khỏi Load Balancer trước)
jcmd <PID> GC.heap_dump /tmp/heap_dump_<PID>_$(date +%Y%m%d_%H%M%S).hprof

# 5. Khuyến nghị cấu hình sẵn JVM Flag tự động dump khi OOM (Không tốn chi phí lúc chạy bình thường)
# -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/data/dumps/oom.hprof
```
