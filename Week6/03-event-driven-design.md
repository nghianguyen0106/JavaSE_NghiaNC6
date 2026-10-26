# 03. Event-Driven Architecture: Asynchronous Payroll Processing Flow

## 1. Luồng Xử Lý Nghiệp Vụ (End-to-End Execution Flow)

Kịch bản: HR Admin kích hoạt thực thi tính lương tháng cho một Tenant.

```
[HR Admin / Client]
        |
        | 1. POST /api/v1/payroll-runs (Command: RunPayrollCommand)
        v
+---------------------------------------------------------------------------------+
|                                 Payroll Service                                 |
|                                                                                 |
| 2. Kiểm tra Invariant: Khóa Distributed Lock (tenantId + payPeriod)             |
| 3. Aggregate Root: PayrollRun.executeCalculation()                              |
| 4. Giao dịch cục bộ (Local DB Transaction):                                     |
|    - Cập nhật bảng `payroll_runs` (status = 'COMPLETED')                        |
|    - Insert chi tiết vào `payroll_payslips`                                     |
|    - Ghi bản tin vào bảng `outbox_events` (Transactional Outbox Pattern)        |
+---------------------------------------------------------------------------------+
        |
        | 5. CDC (Debezium / Kafka Connect) tail WAL -> Publish Message
        v
+---------------------------------------------------------------------------------+
|                       Apache Kafka: Topic `payroll.events`                      |
|                       Event: `PayrollCompletedEvent`                            |
+---------------------------------------------------------------------------------+
        |
        +----------------------------+----------------------------+
        |                            |                            |
        v                            v                            v
+-----------------------+   +-----------------------+   +-----------------------+
| Notification Service  |   |   Reporting Service   |   |     Audit Service     |
| Group: notif-payroll  |   | Group: report-payroll |   |  Group: audit-payroll |
| - Gửi email payslip   |   | - Cập nhật OLAP Read  |   | - Lưu vết compliance  |
|   kèm link truy cập   |   |   model / Dashboard   |   |   (7 năm retention)   |
+-----------------------+   +-----------------------+   +-----------------------+
```

---

## 2. Đặc Tả Command & Event Schema

### 2.1. Command: `RunPayrollCommand`
- Mục đích: Chỉ thị thực thi tác vụ tính toán lương. Có đích đến duy nhất (Point-to-Point invocation).
- Transport: HTTP REST / gRPC qua API Gateway.
```json
{
  "commandId": "cmd-8f3a1c24-5d82-411e-9a1b-99f1c712850a",
  "tenantId": "tenant-corp-alpha",
  "payPeriod": "2026-10",
  "initiatedBy": "usr-hr-admin-001",
  "requestedAt": "2026-10-26T08:00:00Z"
}
```

### 2.2. Event: `PayrollCompletedEvent`
- Mục đích: Thông báo một sự kiện nghiệp vụ đã hoàn tất trong hệ thống. Nhiều consumers có thể đăng ký nhận tin (Pub/Sub).
- Transport: Apache Kafka Topic: `payroll.events`.
- Partition Key: `tenantId` (bảo đảm ordering theo từng tenant).
```json
{
  "eventId": "evt-77b31e92-31fa-4f2e-bc35-64d8a1789c01",
  "eventType": "PayrollCompleted",
  "aggregateType": "PayrollRun",
  "aggregateId": "prun-202610-alpha",
  "tenantId": "tenant-corp-alpha",
  "occurredAt": "2026-10-26T08:15:30Z",
  "payload": {
    "payrollRunId": "prun-202610-alpha",
    "payPeriod": "2026-10",
    "totalEmployees": 15420,
    "totalGrossPay": "154200000000.00",
    "totalNetPay": "128450000000.00",
    "currency": "VND"
  }
}
```

---

## 3. Giải Pháp Dual-Write: Transactional Outbox Pattern

### 3.1. Vấn đề kỹ thuật (Dual-Write Problem)
Nếu thực hiện `UPDATE database` rồi gọi `kafkaTemplate.send()`:
- DB commit thành công nhưng Kafka broker lỗi/network timeout $\rightarrow$ Mất event, downstream services không xử lý (Data Inconsistency).
- Ngược lại, nếu gửi Kafka trước rồi DB rollback $\rightarrow$ Gửi event giả (Phantom Event).

### 3.2. Thiết kế bảng `outbox_events` (RDBMS Local Transaction)
Bản tin event được lưu cùng Transaction với logic nghiệp vụ tại cùng một database instance:

```sql
CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(128) NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    published BOOLEAN DEFAULT FALSE
);
CREATE INDEX idx_outbox_unpublished ON outbox_events(created_at) WHERE published = FALSE;
```

### 3.3. Cơ chế xuất bản (Relay Mechanism)
- Giải pháp chính: Sử dụng Debezium CDC (Change Data Capture) kết nối trực tiếp PostgreSQL Write-Ahead Log (WAL). Debezium streaming toàn bộ thay đổi trên bảng `outbox_events` và ghi sang Kafka Topic `payroll.events` với cam kết `At-Least-Once delivery`.
- Giải pháp dự phòng (Fallback): Polling Publisher định kỳ query các bản ghi `published = FALSE` kèm transaction lock ngắn nếu Debezium gặp sự cố.

---

## 4. Thiết Kế Idempotent Consumer (Chống Xử Lý Trùng Lặp)

Do Kafka tuân theo ngữ nghĩa phân phối At-Least-Once, Consumer bắt buộc phải thiết kế Idempotent.

### Cơ chế Deduplication (Bảng `processed_events` tại Consumer Database):
```
[Kafka Consumer] 
       |
       | Đọc message (eventId = 'evt-77b31e92...')
       v
+---------------------------------------------------------------------------------+
| Bắt đầu Local Transaction:                                                      |
|   1. INSERT INTO processed_events (event_id, consumer_name, processed_at)       |
|      VALUES ('evt-77b31e92...', 'NotificationConsumer', NOW());                 |
|   2. Nếu ném UniqueConstraintViolationException:                               |
|      -> Đã xử lý trước đó -> Bỏ qua, ACK commit Kafka offset ngay lập tức.      |
|   3. Nếu INSERT thành công:                                                     |
|      -> Thực thi nghiệp vụ (Gửi thông báo / Cập nhật Read Model)                |
|      -> COMMIT Transaction.                                                     |
+---------------------------------------------------------------------------------+
```

---

## 5. Chiến Lược Xử Lý Lỗi: Retry & Dead Letter Queue (DLQ)

```
[Kafka Message] 
       |
       v
[Main Consumer] -----> (Xử lý thành công) -----> [Commit Offset]
       |
       | Lỗi (Exception)
       v
[Phân loại Exception]
       |
       +---> Transient Error (Network, Timeout, 503)
       |        |
       |        v
       |     [Retry Topic với Exponential Backoff]
       |     - Attempt 1: delay 1s  (Topic: `payroll.events.retry-1s`)
       |     - Attempt 2: delay 5s  (Topic: `payroll.events.retry-5s`)
       |     - Attempt 3: delay 30s (Topic: `payroll.events.retry-30s`)
       |        |
       |        +---> Vượt quá 3 lần retry thất bại
       |
       +---> Non-Retryable Error (Data Corrupted, Deserialization, NullPointer)
                |
                v
       [Dead Letter Queue Topic: `payroll.events.DLQ`]
                |
                v
       [DLQ Storage & Alerting System] (PagerDuty / Ops Monitoring)
```

### Quy định vận hành DLQ:
1. Header Metadata: Message chuyển vào DLQ bắt buộc chứa headers:
   - `x-original-topic`: `payroll.events`
   - `x-exception-type`: Tên class ngoại lệ (ví dụ: `IllegalArgumentException`).
   - `x-exception-message`: Chi tiết stack trace rút gọn.
   - `x-retry-count`: Số lần đã retry trước khi vào DLQ.
2. Replay Strategy: Sau khi patch bug hoặc khắc phục dependency, kỹ sư vận hành sử dụng công cụ CLI quản trị kích hoạt replay các message từ `payroll.events.DLQ` trở lại `payroll.events` theo lô (Batch Replay) có kiểm soát tốc độ (Rate-controlled).
