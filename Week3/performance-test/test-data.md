# Test Data Design

## 1. Product Catalog

### Yêu cầu dữ liệu:
* Số lượng: Tối thiểu 500 sản phẩm đa dạng trong bảng `products`.
* Trạng thái: Tất cả các sản phẩm dùng cho bài test phải được cấu hình `available = true`.
* Số lượng tồn kho (Inventory Stock): Mỗi sản phẩm được cấp tối thiểu `10,000` đơn vị tồn kho trong DB trước khi test để tránh lỗi hết hàng (Out of Stock) trong suốt quá trình chạy 50 VUs liên tục.

### Cấu trúc bản ghi mẫu (Table: `products`):
```sql
INSERT INTO products (id, name, price, stock, available, category_id)
VALUES 
('P-101', 'Bàn phím cơ Leopold FC900R', 3200000, 10000, true, 'CAT-01'),
('P-102', 'Chuột không dây Logitech MX Master 3S', 2490000, 10000, true, 'CAT-01'),
('P-103', 'Màn hình Dell UltraSharp U2723QE 4K', 14500000, 10000, true, 'CAT-02'),
('P-104', 'Tai nghe Sony WH-1000XM5 Chống ồn', 7990000, 10000, true, 'CAT-03'),
('P-105', 'Ghế công thái học Herman Miller Aeron', 31500000, 10000, true, 'CAT-04');
```

---

## 2. Customer Data & Auth

### Yêu cầu dữ liệu:
* Để tránh hiện tượng Row Lock Contention vô lý (khi 50 VUs cùng mua hàng và cùng cập nhật chung 1 giỏ hàng của 1 user), mỗi Virtual User (VU) trong k6 được gán một `customerId` riêng biệt tương ứng với ID luồng:
  * `customerId: customer-vu-1` đến `customer-vu-50`.
* Token xác thực được tạo sẵn dạng JWT Bearer Token với thời hạn sống (TTL) là 24 giờ để không bị hết hạn giữa chừng khi đang chạy test.

---

## 3. Idempotency Key

Mỗi đơn hàng gửi lên phải chứa một mã duy nhất:
```javascript
idempotencyKey: `idemp-vu${__VU}-iter${__ITER}-${Date.now()}`
```
* Mục đích: Mô phỏng đúng chuẩn Production: Mỗi lần nhấn nút Checkout là một transaction duy nhất, tránh việc các transaction bị đè dữ liệu hoặc bị Order Service từ chối do trùng lặp token.

---

## 4. Data Cleanup & Teardown

Sau khi hoàn tất bài kiểm thử tải, Database sẽ có hàng ngàn đơn hàng rác được sinh ra. Cần thực thi script cleanup để giải phóng ổ đĩa và reset tồn kho:

```sql
-- 1. Xóa các đơn hàng được tạo bởi test VUs
DELETE FROM order_items WHERE order_id IN (SELECT id FROM orders WHERE customer_id LIKE 'customer-vu-%');
DELETE FROM orders WHERE customer_id LIKE 'customer-vu-%';

-- 2. Khôi phục lại số lượng tồn kho ban đầu
UPDATE products SET stock = 10000 WHERE available = true;
```
