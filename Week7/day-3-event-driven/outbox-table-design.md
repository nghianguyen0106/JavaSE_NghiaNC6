# Transactional Outbox Pattern: Database Schema & Relay Pipeline

## 1. Bản Chất Vấn Đề Dual-Write

Trong kiến trúc phân tán, việc thực thi cập nhật trạng thái đơn hàng trong Database và gửi message vào Kafka không thể gộp chung vào một giao dịch ACID nếu không dùng Two-Phase Commit (2PC) nặng nề:
- **Trường hợp A:** DB commit thành công $\rightarrow$ Gửi Kafka bị lỗi mạng $\rightarrow$ Message bị mất, downstream không xử lý.
- **Trường hợp B:** Gửi Kafka thành công $\rightarrow$ DB commit bị lỗi rollback $\rightarrow$ Sinh ra bản tin giả (Phantom Event).

---

## 2. Thiết Kế Bảng `outbox_events` (PostgreSQL)

Bản ghi event được ghi nhận cùng lúc với lệnh cập nhật đơn hàng trong cùng một Local RDBMS Transaction duy nhất:

```sql
CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(128) NOT NULL,
    payload JSONB NOT NULL,
    headers JSONB NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    published BOOLEAN DEFAULT FALSE
);

-- Chỉ mục tối ưu hóa cho Polling Fallback (chỉ index các bản ghi chưa publish)
CREATE INDEX idx_outbox_unpublished ON outbox_events (created_at ASC) 
WHERE published = FALSE;
```

---

## 3. Cơ Chế Chuyển Tiếp (Message Relay Pipeline)

### Giải pháp tối ưu: Debezium CDC (Change Data Capture)
1. Ứng dụng chỉ cần ghi dữ liệu vào bảng `orders` và bảng `outbox_events` trong một transaction Spring `@Transactional`.
2. PostgreSQL ghi nhận thay đổi vào Write-Ahead Log (WAL).
3. **Debezium Connector (Kafka Connect):** Đọc trực tiếp WAL theo thời gian thực (zero impact tới hiệu năng truy vấn của database).
4. Debezium trích xuất trường `payload` và tự động đẩy sang Kafka topic tương ứng theo cấu hình routing (`com.platform.orders`).
5. Đảm bảo cam kết phân phối bản tin: **At-Least-Once Delivery**.
