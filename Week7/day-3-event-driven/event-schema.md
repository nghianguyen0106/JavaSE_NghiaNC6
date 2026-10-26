# Event Schema Specification: OrderConfirmedEvent

## 1. Tiêu Chuẩn Đặc Tả (CloudEvents Compliant Schema)

Bản tin sự kiện `OrderConfirmedEvent` tuân thủ đặc tả CloudEvents v1.0, được tuần tự hóa (serialization) dưới định dạng JSON:

```json
{
  "specversion": "1.0",
  "id": "evt-b184f09a-62df-47ec-a4e1-7dc8e3514a02",
  "source": "/services/order-service",
  "type": "com.platform.orders.OrderConfirmed",
  "subject": "orders/ord-99210-vn",
  "time": "2026-10-26T12:00:00Z",
  "datacontenttype": "application/json",
  "correlationid": "c1a8-9921-ef4b",
  "data": {
    "orderId": "ord-99210-vn",
    "customerId": "cust-5512",
    "customerEmail": "customer@example.com",
    "totalAmount": "2500000.00",
    "currency": "VND",
    "confirmedAt": "2026-10-26T12:00:00Z",
    "items": [
      {
        "sku": "SKU-IPHONE-15",
        "quantity": 1,
        "unitPrice": "2500000.00"
      }
    ]
  }
}
```

---

## 2. Các Quy Tắc Trường Bắt Buộc (Schema Invariants)

1. **`id` (Event ID):** Bắt buộc sử dụng UUIDv4 sinh ra tại thời điểm tạo Outbox record. Dùng làm khóa khử trùng lặp (Deduplication Key) tại các Consumer.
2. **`correlationid`:** Được kế thừa trực tiếp từ HTTP request header `X-Correlation-Id` nhằm duy trì luồng phân tích vết trên OpenTelemetry / Jaeger.
3. **Partition Key:** Khi ghi vào Apache Kafka, partition key bắt buộc là `orderId` (hoặc `customerId`) để đảm bảo toàn bộ sự kiện liên quan đến cùng một đơn hàng được phân bổ vào cùng một Kafka Partition, duy trì thứ tự tuần tự tuyệt đối (Strict Ordering).
4. **Schema Evolution:** Tuân thủ quy tắc tương thích ngược (Backward Compatibility). Chỉ được phép thêm trường mới có giá trị default/optional, cấm xóa hoặc đổi tên trường đang tồn tại.
