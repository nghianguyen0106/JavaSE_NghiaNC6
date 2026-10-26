# 06. Non-Functional Requirements & Observability Architecture

## 1. Định Nghĩa SLI / SLO (Service Level Indicators & Objectives)

Áp dụng cho thành phần cốt lõi: Payroll Service & Payslip API.

| STT | Tên chỉ số | Phân loại | Định nghĩa kỹ thuật (SLI Formula) | Mục tiêu cam kết (SLO Target) |
| :---: | :--- | :--- | :--- | :--- |
| 1 | Payroll API Availability | Technical | $\frac{\sum \text{Successful HTTP Requests (Status } < 500)}{\sum \text{Total HTTP Requests}} \times 100\%$ | $\ge 99.95\%$ trong chu kỳ 30 ngày |
| 2 | Payslip Read Latency | Technical | Thời gian phản hồi $P95$ của endpoint `GET /api/v1/payslips/{id}` | $P95 \le 400\text{ms}$, $P99 \le 1.5\text{s}$ |
| 3 | Calculation Throughput | Business | Thời gian hoàn tất mẻ tính lương quy mô 100,000 nhân viên | $T_{\text{complete}} \le 25\text{ phút}$ |
| 4 | Duplicate Run Tolerance | Business | Số lượng bản ghi `PayrollRun` bị trùng trạng thái `COMPLETED` cho cùng 1 tenant trong 1 tháng | 0 (Zero Tolerance) |
| 5 | Dead Letter Queue Rate | Technical | Tỷ lệ bản tin sự kiện rơi vào `payroll.events.DLQ` trên tổng số event xuất bản | $\le 0.01\%$ |

---

## 2. Hệ Thống Cảnh Báo Chủ Động (Actionable Alerts Specification)

| Alert Name | Severity | Điều kiện kích hoạt (PromQL / Metric Condition) | Tác động hệ thống (Impact) | First-Response Action Plan (Runbook Link) |
| :--- | :--- | :--- | :--- | :--- |
| HighApiErrorRate5xx | `P1 - CRITICAL` | `sum(rate(http_requests_total{status=~"5.."}[5m])) / sum(rate(http_requests_total[5m])) > 0.05` liên tục trong 2 phút | API tính lương hoặc xem phiếu lương bị lỗi hàng loạt | 1. Kiểm tra database connection pool saturation.<br>2. Kiểm tra log lỗi gần nhất qua OpenSearch.<br>3. Rollback canary release nếu vừa deploy phiên bản mới. |
| HighPayrollLatencyP99 | `P2 - WARNING` | `histogram_quantile(0.99, sum(rate(http_request_duration_seconds_bucket{endpoint="get_payslip"}[5m])) by (le)) > 2.0` trong 5 phút | Trải nghiệm người dùng suy giảm nghiêm trọng khi tra cứu phiếu lương | 1. Kiểm tra cache hit ratio trên Redis cluster.<br>2. Kiểm tra slow query logs trên PostgreSQL Read Replica. |
| KafkaConsumerLagSpike | `P2 - WARNING` | `kafka_consumergroup_lag{topic="payroll.events", group="notification-group"} > 50000` trong 10 phút | Nhân viên không nhận được email phiếu lương kịp thời | 1. Tự động scale số lượng consumer pods (HPA).<br>2. Kiểm tra tình trạng kết nối mạng tới SMTP / SendGrid Gateway. |
| DlqMessageCountSpike | `P1 - CRITICAL` | `increase(kafka_topic_records_in_total{topic="payroll.events.DLQ"}[5m]) > 5` | Dữ liệu tài chính/thông báo bị từ chối xử lý do lỗi cú pháp hoặc logic | 1. Kiểm tra header và payload của message trong DLQ.<br>2. Tạm dừng consumer nếu có lỗi deserialization hàng loạt. |
| DbConnectionPoolExhaustion | `P1 - CRITICAL` | `hikaricp_connections_pending{pool="PayrollHikariPool"} > 20` liên tục 1 phút | Toàn bộ thread worker bị block chờ DB connection | 1. Tăng tạm thời pool size nếu DB instance còn dư CPU/RAM.<br>2. Thu thập Thread Dump để xác định transaction nào đang bị treo lâu. |

---

## 3. Phân Định Business Metrics vs Technical Metrics

```
+-----------------------------------------------------------------------------------+
|                         Phân loại chỉ số giám sát                                 |
+-----------------------------------------------------------------------------------+
           |                                                      |
           v                                                      v
+---------------------------------------+  +---------------------------------------+
|          Technical Metrics            |  |           Business Metrics            |
+---------------------------------------+  +---------------------------------------+
| - JVM Heap / GC Pause duration        |  | - Tổng số Payroll Runs hoàn tất / ngày|
| - HikariCP Active / Idle Connections  |  | - Tổng số tiền lương đã giải ngân     |
| - CPU Utilization / Load Average      |  | - Số lượng nhân viên đã nhận payslip  |
| - Kafka Broker Partition Under-rep    |  | - Tỷ lệ gửi thông báo email thành công|
| - Redis Memory Used / Eviction Count  |  | - Số lượng khiếu nại sai lương        |
| - Network I/O, Disk Write Bytes       |  | - Thời gian trung bình duyệt đơn phép |
+---------------------------------------+  +---------------------------------------+
```

---

## 4. Cơ Chế Truy Vết Phân Tán (Correlation ID & Distributed Tracing Flow)

Toàn bộ các log và span xuyên suốt các service bắt buộc phải mang chung ngữ cảnh truy vết theo chuẩn W3C TraceContext (`traceparent`):

```
Client Request
      |
      | HTTP Header: X-Correlation-ID: "c1a8-9921-ef4b"
      v
[API Gateway]
      | 1. Log: Gateway received request [trace_id=4bf92f3577b34da6, correlation_id=c1a8-9921-ef4b]
      | 2. Forward request qua HTTP Header xuống Payroll Service
      v
[Payroll Service]
      | 1. Filter trích xuất header nạp vào MDC (Mapped Diagnostic Context)
      | 2. Giao dịch DB ghi Outbox Event kèm `correlation_id` trong payload
      v
[PostgreSQL WAL / Debezium]
      | Relay bản tin sang Kafka Message Header: `ce_correlation_id = c1a8-9921-ef4b`
      v
[Notification Service / Consumer]
      | 1. Trích xuất Kafka Header nạp lại vào ThreadLocal MDC
      | 2. Log: Sending email [trace_id=4bf92f3577b34da6, correlation_id=c1a8-9921-ef4b]
```

### Tiêu chuẩn cấu trúc Log định dạng JSON (Structured Logging):
```json
{
  "timestamp": "2026-10-26T08:15:30.124Z",
  "level": "INFO",
  "service": "payroll-service",
  "traceId": "4bf92f3577b34da6",
  "spanId": "00f067aa0ba902b7",
  "correlationId": "c1a8-9921-ef4b",
  "tenantId": "tenant-corp-alpha",
  "message": "Payroll calculation completed for period 2026-10",
  "durationMs": 420
}
```
