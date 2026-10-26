# Dead Letter Queue (DLQ) Operational Runbook & Replay Plan

## 1. Tiêu Chí Định Tuyến Vào DLQ (DLQ Routing Criteria)

Message bị chuyển vào Dead Letter Queue (Topic: `com.platform.orders.DLQ`) khi:
1. Đã vượt quá số lần retry tối đa cấu hình trên Consumer (ví dụ: 3 lần thử lại thất bại).
2. Gặp lỗi không thể phục hồi (Non-Retryable Exceptions): Lỗi phân tích cú pháp (`SerializationException`, `JsonParseException`), vi phạm schema hoặc dữ liệu bị thiếu trường bắt buộc.

Headers bắt buộc đính kèm khi đưa vào DLQ:
- `x-original-topic`: Tên topic gốc (`com.platform.orders`).
- `x-original-partition`: Partition ID gốc.
- `x-original-offset`: Offset của bản tin gốc.
- `x-exception-type`: Tên class ngoại lệ (ví dụ: `CustomerNotFoundException`).
- `x-exception-message`: Nội dung lỗi chi tiết.
- `x-failed-timestamp`: Thời điểm bản tin bị đẩy vào DLQ.

---

## 2. Quy Trình Phản Ứng & Phân Tích (Incident Analysis)

```
[DLQ Alert kích hoạt: >0 messages]
                 |
                 v
[1. Truy vấn Kafka DLQ Topic qua CLI / Kafdrop]
                 |
                 v
[2. Trích xuất Headers để xác định lỗi]
                 |
       +---------+---------+
       |                   |
       v                   v
[Lỗi Code / Logic]   [Lỗi Hạ tầng / Dependency]
       |                   |
       v                   v
Triển khai Hotfix     Khôi phục kết nối downstream service
       |                   |
       +---------+---------+
                 |
                 v
[3. Kích hoạt DLQ Replay Pipeline]
```

---

## 3. Quy Trình Tái Xử Lý Bản Tin (Replay Execution Plan)

### Bước 1: Dừng Consumer xử lý lỗi (nếu lỗi do code)
Triển khai phiên bản vá lỗi (hotfix) lên môi trường Production trước khi kích hoạt replay.

### Bước 2: Thực thi script Replay có kiểm soát tốc độ (Rate-Controlled Replay)
Sử dụng công cụ CLI nội bộ hoặc Kafka MirrorMaker để đọc bản tin từ DLQ và ghi ngược lại Topic chính:

```bash
# Lệnh trích xuất bản tin từ DLQ và đẩy lại topic gốc
kafka-console-consumer --bootstrap-server kafka:9092 \
  --topic com.platform.orders.DLQ \
  --from-beginning \
  --max-messages 100 \
| kafka-console-producer --bootstrap-server kafka:9092 \
  --topic com.platform.orders
```

### Bước 3: Giám sát chỉ số
Theo dõi dashboard Consumer Lag và Error Rate của Topic chính trong 15 phút. Nếu tỷ lệ lỗi $5xx = 0$ và message offset tăng bình thường, hoàn tất quy trình replay.
