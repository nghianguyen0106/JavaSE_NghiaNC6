# Assignment 01: Performance Testing Fundamentals

## 1. Định nghĩa các loại Performance Test

| Loại Test | Mục đích chính | Kịch bản thực hiện tiêu biểu | Khi nào cần thực hiện? |
| :--- | :--- | :--- | :--- |
Load  | Kiểm tra hành vi và hiệu năng của hệ thống dưới mức tải dự kiến (Expected Load / Peak Load bình thường). | Tăng tải dần dần (Ramp-up) từ 0 lên 1,000 RPS, duy trì tải trong 30–60 phút, sau đó hạ tải (Ramp-down). | Trước khi release phiên bản mới, định kỳ trong CI/CD, chuẩn bị cho các đợt tăng trưởng người dùng bình thường. |
Stress  | Đẩy tải vượt quá mức thiết kế tối đa (Beyond Peak) để điểm gãy (Breaking Point và xem cách hệ thống phục hồi (Graceful Degradation vs Crash). | Tăng tải liên tục: 1,000 RPS → 2,000 RPS → 4,000 RPS cho đến khi Error rate tăng vọt hoặc response time quá ngưỡng chịu đựng. | Đánh giá năng lực tối đa, tìm thành phần chết đầu tiên (DB, Network, CPU, Memory) để lập kế hoạch scale. |
Spike  | Kiểm tra phản ứng của hệ thống khi lượng traffic tăng đột ngột trong thời gian rất ngắn (Gấp 3–10 lần trong vài chục giây). | Đang chạy 200 RPS, đột ngột bơm 1,000 – 5,000 RPS trong 1–2 phút rồi hạ về bình thường. | Chuẩn bị cho Flash Sale mở bán lúc 0h, sự kiện phát sóng trực tiếp, đẩy thông báo Push Notification tới hàng triệu user. |
Soak / Endurance  | Kiểm tra độ ổn định của hệ thống dưới tải liên tục trong thời gian dài (vài giờ đến vài ngày) để phát hiện rò rỉ tài nguyên. | Giữ tải ổn định ở mức 70–80% Peak load (ví dụ 700 RPS) liên tục trong 4 đến 24 giờ. | Phát hiện Memory Leak, rò rỉ DB Connection Pool, tràn Thread Pool, đầy ổ cứng do Log, bộ nhớ đệm (Cache) phình to không giải phóng. |
Capacity  | Xác định năng lực xử lý tối đa của một cấu hình hạ tầng hiện tại (ví dụ: 1 Pod 2 Core 4GB RAM gánh được tối đa bao nhiêu RPS trước khi SLA bị vi phạm). | Tăng tải từng nấc (Step Load), mỗi nấc đo p95 và CPU/RAM cho đến khi p95 chạm ngưỡng 500ms hoặc CPU chạm 80%. | Lập kế hoạch dự toán chi phí máy chủ (Capacity Planning), tính toán chính xác số lượng Pod cần cấu hình cho Horizontal Pod Autoscaler (HPA). |

---

## 2. Bảng định nghĩa các Metrics cốt lõi bằng ví dụ thực tế

| Metric | Đơn vị | Định nghĩa bản chất | Ví dụ cụ thể & Ý nghĩa |
| :--- | :---: | :--- | :--- |
Latency / Response  | `ms` hoặc `s` | Khoảng thời gian từ lúc client gửi byte đầu tiên của request đến khi nhận được byte cuối cùng của response. | API `/api/orders` có Latency = `320ms`. Nghĩa là từ lúc user bấm Checkout đến khi màn hình báo thành công mất 0.32 giây. |
Percentiles (p50, p90, p95, p99 | `ms` | Bách phân vị: Ngưỡng thời gian mà có $X\%$ lượng request hoàn thành nhanh hơn hoặc bằng mức đó. Phản ánh trải nghiệm thực tế hơn Average. | p50 = 120ms 50% user phản hồi $\le 120ms$.<br>p95 = 450ms 95% user phản hồi $\le 450ms$.<br>p99 = 2.1s 1% user chậm nhất bị chờ tới 2.1s (Tail Latency). |
Throughput /  | `req/sec` hoặc `tps` | Tốc độ xử lý của hệ thống: Số request hoặc transaction mà hệ thống hoàn thành trong 1 giây. | Hệ thống đạt `1,000 RPS` nghĩa là mỗi giây gánh trọn 1,000 request checkout mà không bị drop. |
Error  | `%` | Tỷ lệ phần trăm giữa số request thất bại (HTTP 5xx, timeout, network error) trên tổng số request gửi đi. | Tổng 100,000 request có 200 request lỗi 500 $\rightarrow$ `Error Rate = (200 / 100,000) * 100 = 0.2%`. |
Virtual Users (VUs | `users` | Số luồng client giả lập chạy song song, mô phỏng hành vi của người dùng thật gửi request liên tục. | 100 VUs chạy kịch bản Checkout. Nếu mỗi VU mất 100ms cho 1 vòng lặp thì có thể tạo ra throughput $\approx 1,000 \text{ RPS}$. |
Resource  | `%` hoặc `count` | Mức độ bão hòa tài nguyên phần cứng hoặc bộ đệm hệ thống khi tải tăng. | App CPU: 60 (Chưa bão hòa)<br>DB CPU: 98 (Đã bão hòa - CPU Bound)<br>DB Connection Pool: 100/ (Cạn kiệt kết nối)<br>GC Pause:  (Stop-the-world làm đơ app). |

> Không dùng Average Response Time làm metric duy nhất
> Do là số trung bình chỉ đánh giá tổng quan , có thể che lấp các vấn đề gây ảnh hưởng đến trải nghiệm người dùng.

---

## 3. Phân tích Requirement bài toán Order API

### Yêu cầu đề bài:
```Text
- Peak traffic: 1,000 requests/second.
- p95 response time phải dưới 500ms.
- Error rate phải dưới 0.5%.
- Hệ thống cần chịu được traffic liên tục trong 4 giờ.
- Trong flash sale, traffic có thể tăng gấp 5 lần trong 2 phút.
```
### Bảng Mapping Requirement → Test Type → Success Criteria

| Requirement | Test Type | Metric | Lý do chọn | Success Criteria  |
| :--- | :--- | :--- | :--- | :--- |
| **1,000 RPS bình thường** | **Load Testing** | • RPS (Throughput)<br>• p95 Latency<br>• Error Rate<br>• CPU/Memory App & DB | Xác minh hệ thống đáp ứng đúng cam kết nghiệp vụ trong điều kiện giờ cao điểm thông thường. | • Throughput duy trì ổn định $\ge 1,000 \text{ RPS}$.<br>• p95 latency $\le 500\text{ms}$.<br>• Error rate $< 0.5\%$.<br>• CPU/RAM của App & DB $\le 70\%$. |
| **Traffic liên tục 4 giờ** | **Soak / Endurance Testing** | • Memory Heap (JVM Heap usage)<br>• DB Connection Pool Wait Time<br>• Thread Count<br>• Disk I/O & Log storage<br>• GC Pause Frequency | Đảm bảo không xảy ra hiện tượng rò rỉ bộ nhớ (Memory Leak), cạn kiệt connection pool hay nghẽn bộ đệm sau thời gian dài vận hành. | • Đồ thị Heap Memory giữ dạng "răng cưa" ổn định, GC thu hồi sạch, không tăng vô tận (No OOM).<br>• DB connection pool không bị leak (trả về pool đầy đủ).<br>• Latency không bị trôi (Drift) tăng dần theo thời gian. |
| **Flash sale tăng 5 lần (5,000 RPS trong 2 phút)** | **Spike Testing** | • Latency p95, p99<br>• Error Rate<br>• HPA Scaling Time<br>• Queue Depth (nếu có MQ)<br>• DB Lock Contention | Kiểm tra xem hệ thống có bị "nghẹn thở" sập đột ngột khi traffic tăng vọt hay tự kích hoạt cơ chế phòng vệ (Rate Limiting, Circuit Breaker). | • Hệ thống không bị crash toàn bộ.<br>• Nếu vượt ngưỡng, hệ thống từ chối có kiểm soát (HTTP 429 - Rate Limit hoặc Queue) thay vì crash 500.<br>• Khi spike kết thúc (sau 2 phút), latency và error rate tự hồi phục về trạng thái bình thường trong vòng $< 30$ giây. |
| **Tìm giới hạn tối đa** | **Stress / Capacity Testing** | • Breaking Point RPS<br>• Saturation point của CPU/DB<br>• Bottleneck component<br>• Recovery time sau sập | Xác định chính xác ngưỡng sập của hệ thống hiện tại để lên phương án Auto-scaling và đặt giới hạn chặn tải (Load Shedding). | • Xác định chính xác ngưỡng Max RPS (ví dụ: gãy ở 3,200 RPS).<br>• Tìm ra thành phần chạm trần đầu tiên (ví dụ: DB connection pool chạm 100%). |