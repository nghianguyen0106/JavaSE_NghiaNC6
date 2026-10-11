# Assignment 06: Chaos Engineering Fundamentals & Experiment Canvas

Tài liệu này thiết kế một kịch bản thử nghiệm hỗn loạn (Chaos Experiment) hoàn chỉnh cho luồng nghiệp vụ quan trọng: Order Service gọi đồng bộ sang Inventory Service để giữ hàng tồn kho (Reserve Stock).

---

## 1. Bản chất của Chaos Engineering

> Chaos Engineering KHÔNG PHẢI là đập phá bừa bãi trong hệ thống.  
> Chaos Engineering là một phương pháp khoa học thực nghiệm: Ta đưa ra một giả thuyết về khả năng tự phục hồi của hệ thống, chủ động tiêm sự cố (Fault Injection) trong một phạm vi an toàn có kiểm soát (Blast Radius), quan sát phản ứng thông qua các chỉ số đo lường (Observability), và dừng ngay lập tức nếu vượt ngưỡng nguy hiểm (Abort Condition).

---

## 2. Kế hoạch Thử nghiệm: Chaos Experiment Canvas

| Hạng mục thiết kế | Chi tiết cấu hình & Quy chuẩn |
| :--- | :--- |
| Tên thử nghiệm | `EXP-01: Pod Crash Resilience on Inventory Service` |
| Mục tiêu thử nghiệm | Kiểm tra xem tính năng Client-Side Load Balancing và Retry của Order Service có tự động chuyển hướng request sang Pod sống khác mà không làm gián đoạn trải nghiệm người dùng hay không. |
| Môi trường thực hiện | Môi trường Staging (Pre-production), được bơm tải giả lập ổn định 200 RPS bằng k6. |
| 1. Trạng thái Ổn định (Steady State) | Trước khi tiêm lỗi, hệ thống phải đạt chuẩn:<br>• Checkout Success Rate: $\ge 99.5\%$ (Error rate $< 0.5\%$).<br>• p95 Latency: $< 500\text{ms}$.<br>• Số Pod Inventory khỏe mạnh: 3/3 pods đang Ready. |
| 2. Giả thuyết Khoa học (Hypothesis) | *"Khi 1 trong 3 Pod của Inventory Service bị tắt đột ngột (Killed), Order Service sẽ nhanh chóng nhận diện qua Healthcheck/Eureka/K8s Endpoints, tự động retry sang 2 Pod còn lại. Success rate của Checkout vẫn duy trì $\ge 99.0\%$ và p95 latency không vượt quá $800\text{ms}$."* |
| 3. Hành vi Tiêm lỗi (Fault Injection) | Sử dụng Chaos Mesh / LitmusChaos (hoặc lệnh `kubectl delete pod inventory-service-xxx --grace-period=0`) để xóa đột ngột 1 Pod Inventory. |
| 4. Bán kính Ảnh hưởng (Blast Radius) | • Giới hạn 1 Pod duy nhất trong cụm 3 Pods (chỉ chiếm 33% năng lực của Inventory).<br>• Thời gian thử nghiệm: Tối đa 3 phút.<br>• Chỉ áp dụng trên cụm Staging, hoàn toàn cô lập khỏi dữ liệu khách hàng thật. |
| 5. Điều kiện Dừng Khẩn cấp (Abort Condition) | DỪNG NGAY LẬP TỨC VÀ KHÔI PHỤC HẠ TẦNG NẾU:<br>• Tỷ lệ lỗi Checkout vượt quá 2.0% kéo dài quá 15 giây.<br>• p95 Latency của Checkout vượt quá 2,000ms (2 giây).<br>• 2 Pods Inventory còn lại bị quá tải CPU $> 90\%$. |
| 6. Quy trình Hoàn nguyên (Rollback Procedure) | 1. Hủy bỏ ngay tiến trình Chaos Mesh Experiment (`kubectl delete chaos ...`).<br>2. Kubernetes ReplicaSet tự động khởi tạo pod thay thế (chờ pod mới Ready trong 10s).<br>3. Kiểm tra xem Success Rate có quay trở về $\ge 99.5\%$ và p95 $< 500\text{ms}$ trong vòng 1 phút hay không. |

---

## 3. Các Chỉ số Quan sát Bắt buộc (Observability Dashboard)

Trong suốt quá trình chạy thử nghiệm, đội ngũ kỹ thuật phải theo dõi 4 bảng chỉ số (Golden Signals) theo thời gian thực (Real-time Prometheus / Grafana):

```
[Throughput]        200 RPS (Duy trì đều)
[Success Rate]     99.8% ──────────> 99.2% (Hơi giảm nhẹ do 1st retry) ──────────> 99.8%
[p95 Latency]      420ms ──────────> 680ms (Tăng do tốn thời gian retry) ─────────> 430ms
[Inventory Pods]   [Pod 1] [Pod 2] [Pod 3 (KILLED)] ───────> [Pod 3 (Recreated)]
```

1. Traffic & Error Rate: Số mã lỗi `HTTP 500` hoặc `503 Service Unavailable` sinh ra từ Order Service.
2. Client Retry Metrics: Metric `http_client_retries_total` của Order Service tăng lên (chứng minh logic retry có hoạt động).
3. Pod Endpoints Discovered: Thời gian để K8s Service / Spring Cloud LoadBalancer gạch tên Pod chết ra khỏi danh sách routing (thường mất từ 1s - 3s).
4. CPU & Thread Pool của 2 Pods còn lại: Đảm bảo 2 Pods còn sống gánh thêm 50% tải mà không bị sập dây chuyền (Cascading Failure).

---

## 4. Kết luận & Giá trị của Thử nghiệm

* Nếu Thử nghiệm Đạt (Hypothesis Validated): Ta tự tin rằng hệ thống có khả năng tự phục hồi (Self-healing) khi hạ tầng vật lý hoặc node Kubernetes gặp sự cố bất ngờ trên Production.
* Nếu Thử nghiệm Thất bại (Hypothesis Invalidated - ví dụ Checkout lỗi 15%): Ta phát hiện ra một lỗ hổng nghiêm trọng (ví dụ: Order Service đang cache IP của Pod cũ quá lâu mà không nhận ra Pod đã chết, hoặc timeout đặt quá dài). Từ đó, đội ngũ kỹ thuật tạo Task để khắc phục cấu hình trước khi sự cố thật xảy ra!
