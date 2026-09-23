# BÀI TẬP SOLID:

1. Chỉ ra vi phạm SOLID ở đâu.
2. Refactor để tuân thủ SOLID.
3. Giải thích từng quyết định.
---

## 1. Chỉ ra vi phạm SOLID ở đâu.
### Vi phạm SRP
  1. Nghiệp vụ / Business: Khi thay đổi quy tắc validate đơn hàng
  2. Thanh toán / Payment Provider: Khi thêm/sửa/đổi API Stripe, PayPal, Bank
  3. Hạ tầng dữ liệu / Database Admin: Khi thay đổi schema DB, câu lệnh SQL, DB engine
  4. Hệ thống thông báo / Marketing / Customer Service: Khi đổi mẫu email, đổi từ SMTP sang SendGrid/SMS
  5. Hệ thống giám sát / Logging / DevOps: Khi đổi console print sang log framework
- Hậu quả thực tế:
  - Các phần code bị dính liên kết chặt chẽ theo dạng mắc xích nên sẽ khó thay thế. Dẫn đến rủi ro lỗi dây chuyền
  - Khó Unit Test: Muốn test logic thanh toán thì bắt buộc phải kết nối DB thật, gửi mail thật, gọi API thật.

---

### Vi phạm OCP
- Đoạn code `if (paymentMethod.equals("CREDIT_CARD")) { ... } else if .....`:
- Khi tích hợp thêm cổng thanh toán mới (ví dụ: `MOMO`, `ZALOPAY`, `APPLE_PAY`) Phải vào sửa trực tiếp class `PaymentService`

---

### Vi phạm LSP
- Vi phạm: Khi thiết kế các cổng thanh toán mới kế thừa/implement `PaymentGateway`, cần đảm bảo điều gì để các implement có thể thay thế hoàn hảo cho nhau mà không làm gãy flow của `PaymentService`?

---

### Vi phạm ISP
- ISP: Các interface nên chia nhỏ, thay vì một interface lớn, các interface nên được chia nhỏ:
  - `PaymentGateway`: interface cho cổng thanh toán
  - `PaymentRepository`: interface cho lưu database
  - `NotificationService`: interface cho gửi thông báo
  - `EventPublisher`: interface cho publish event
  
---

### Vi phạm DIP
- `PaymentService` (High-level: xử lý nghiệp vụ thanh toán) đang trực tiếp phụ thuộc vào các Low-level modules thông qua toán tử `new`:
- `new StripeAPI()`, `new PayPalAPI()`, `new BankAPI()`
- `DriverManager.getConnection(...)`
- `new SMTP(...)`
- Tác động đến Unit Test:
    - Khi viết Unit Test cho `PaymentService` không thể test độc lập vì phải dùng DB thật, thư viện gửi mail thật, gọi API thật.
    - Cần đảo ngược phụ thuộc thông qua Dependency Injection để có thể Mock/Stub dễ dàng.



---

## 2. Giải thích từng phần sau khi refactor


## IV. So sánh Beffore vs After

| Tiêu chí |  Beffore |  After |
| :--- | :--- | :--- |
| Số lượng trách nhiệm | 5+ trách nhiệm trong 1 class | Mỗi class đảm nhiệm đúng 1 trách nhiệm duy nhất |
| Thêm cổng thanh toán mới | Phải sửa code trong `PaymentService` (`else if`) | Chỉ cần tạo thêm 1 class implement `PaymentGateway` |
| Phụ thuộc hạ tầng (DB, Mail) | Dính chặt vào MySQL, SMTP cụ thể | Phụ thuộc vào Interface trừu tượng |
| Khả năng viết Unit Test | Rất khó, cần môi trường DB và mạng thật | Cực kỳ dễ dàng bằng cách inject Mock/Fake objects |
| Mức độ Coupling | Tight Coupling (Kết dính cao) | Loose Coupling (Liên kết lỏng) |
