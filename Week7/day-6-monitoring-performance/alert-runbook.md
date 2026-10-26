# Alerting Rules & First-Response Runbooks

## 1. Alert: HighHttpErrorRate5xx (P1 - CRITICAL)

- PromQL Condition:
  ```promql
  sum(rate(http_server_requests_seconds_count{status=~"5.."}[2m])) 
  / 
  sum(rate(http_server_requests_seconds_count[2m])) > 0.02
  ```
- Severity: P1 - CRITICAL (Trực tiếp ảnh hưởng tới khả năng thanh toán của khách hàng).
- Runbook:
  1. Kiểm tra Dashboard Dependency xem lỗi 5xx xuất phát từ mã nguồn Order Service hay do upstream database / downstream payment service.
  2. Mở OpenSearch / Kibana truy vấn log với query: `service:order-service AND level:ERROR`.
  3. Nếu lỗi xảy ra sau khi vừa triển khai phiên bản mới: Kích hoạt Rollback về image cũ ngay lập tức qua ArgoCD / Kubernetes Deployment.
  4. Nếu lỗi do Database cạn kết nối: Xem xét tăng tạm thời `maximumPoolSize` của HikariCP hoặc khởi động lại read-replica.

---

## 2. Alert: InventoryCircuitBreakerOpen (P2 - WARNING)

- PromQL Condition:
  ```promql
  resilience4j_circuitbreaker_state{name="inventoryService", state="open"} == 1
  ```
- Severity: P2 - WARNING.
- Runbook:
  1. Xác nhận xem tính năng Fallback đã hoạt động bình thường chưa (đơn hàng có được chuyển sang trạng thái chờ xác minh tồn kho hay bị văng lỗi 500).
  2. Liên hệ với On-call Engineer của Inventory Service để kiểm tra tình trạng tài nguyên pod và database của Inventory.
  3. Theo dõi chỉ số `resilience4j_circuitbreaker_buffered_calls` để xác định thời điểm circuit chuyển sang `HALF_OPEN`.

---

## 3. Alert: KafkaConsumerLagSpike (P2 - WARNING)

- PromQL Condition:
  ```promql
  sum(kafka_consumergroup_lag{consumergroup="notification-group"}) by (topic) > 10000
  ```
- Severity: P2 - WARNING.
- Runbook:
  1. Kiểm tra số lượng Consumer Pods đang chạy. Kích hoạt Horizontal Pod Autoscaler (HPA) để scale tăng số lượng replicas (tối đa bằng số lượng Partitions của topic).
  2. Kiểm tra log của Notification Consumer để xem có hiện tượng retry vô hạn do lỗi định dạng bản tin hay không.
  3. Kiểm tra throughput của dịch vụ SMTP / SMS bên ngoài.
