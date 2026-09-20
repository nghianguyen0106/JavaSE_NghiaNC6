# 7. Saga Pattern
## **Cần học**

Mentee cần tìm hiểu:

- Distributed transaction.
- Vì sao không nên dùng một transaction database cho nhiều service.
- Saga là gì.
- Saga choreography.
- Saga orchestration.
- Compensation action.
- Retry.
- Idempotency.

## **Tình huống thực hành**

Flow:

Text

`Order Service
      ↓
Payment Service
      ↓
Inventory Service
      ↓
Shipping Service`

Tình huống lỗi:

1. Order được tạo.
2. Payment thành công.
3. Inventory không còn hàng.
4. Hệ thống cần hoàn tiền.
5. Order chuyển sang trạng thái failed/cancelled.

## **Yêu cầu thực hành**

Mentee cần:

1. Vẽ sequence diagram.
2. Xác định từng bước của saga.
3. Xác định compensation action.
4. So sánh choreography và orchestration.
5. Chọn một cách triển khai và giải thích lý do.

Ví dụ compensation:

Text

`Payment successful
Inventory failed
        ↓
Refund payment
        ↓
Cancel order`

## **Sản phẩm cần nộp**

- Sequence diagram hoặc flow diagram.
- Bảng gồm:

| **Step** | **Action** | **Failure** | **Compensation** |
| --- | --- | --- | --- |
| 1 | Create order | Create failed | None |
| 2 | Reserve payment | Payment failed | Cancel order |
| 3 | Reserve inventory | Inventory failed | Refund payment |
| 4 | Create shipment | Shipping failed | Release inventory |
- Pseudo-code hoặc Java demo đơn giản.
- Phân tích choreography và orchestration.

## **Tiêu chí hoàn thành**

- Hiểu Saga không rollback database như transaction thông thường.
- Xác định được compensation action.
- Biết tại sao operation cần idempotent.
- Biết retry không phải lúc nào cũng an toàn.