# BÀI TẬP SOLID:

1. Chỉ ra vi phạm SOLID ở đâu.
2. Refactor để tuân thủ SOLID. - [Tham khảo thư mục "./afer" và PaymentService.java](./after/service/PaymentService.java)
3. Giải thích từng quyết định.
---

## 1. Chỉ ra vi phạm SOLID ở đâu.
### Vi phạm SRP
  1. Business: Khi thay đổi quy tắc validate đơn hàng
  2. Payment Provider: Khi thêm/sửa/đổi API Stripe, PayPal, Bank
  3. Database Admin: Khi thay đổi schema DB, câu lệnh SQL, DB engine
  4. Notification Service: Khi đổi mẫu email, đổi từ SMTP sang SendGrid/SMS
  5. Logging Service: Khi đổi console print sang log framework
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

### 2.1. Xác thực đơn hàng (`validator/OrderValidator`)
- Before: Logic validate nằm lẫn trong `PaymentService`, ném `Exception` chung chung.
- After: Tách thành class riêng `OrderValidator` (tuân thủ SRP). Áp dụng nguyên tắc Fail-Fast với `IllegalArgumentException` để ngắt luồng ngay khi dữ liệu không hợp lệ, bảo vệ an toàn các bước trừ tiền phía sau.

### 2.2. Cổng thanh toán (`gateway/`) - Strategy & Factory Pattern
- Before: Dùng chuỗi `if-else` kiểm tra chuỗi String và trực tiếp `new StripeAPI()`, `new PayPalAPI()`, `new BankAPI()` (vi phạm nghiêm trọng OCP và DIP).
- After:
  - Định nghĩa interface trừu tượng `PaymentGateway` với hợp đồng chuẩn `charge(double amount)`.
  - Mỗi cổng thanh toán là một class độc lập (`StripePaymentGateway`, `PayPalPaymentGateway`, `BankPaymentGateway`) triển khai `PaymentGateway` (tuân thủ LSP, DIP).
  - Sử dụng `PaymentGatewayFactory` với `Map<PaymentMethod, PaymentGateway>` để triệt tiêu toàn bộ `if-else`. Khi thêm cổng mới (Momo, ZaloPay), chỉ cần tạo class mới và đăng ký vào Factory, không cần sửa đổi `PaymentService` (tuân thủ tuyệt đối OCP).

### 2.3. Lưu trữ dữ liệu (`repository/` & `common/DatabaseConfig`)
- Before: Nhúng trực tiếp câu lệnh SQL `INSERT INTO...` và kết nối JDBC `DriverManager.getConnection(...)` vào `PaymentService` (vi phạm SRP, DIP).
- After:
  - Tách abstraction `PaymentRepository` với phương thức `save(Payment)`.
  - Class `JdbcPaymentRepository` nhận `javax.sql.DataSource` qua Constructor Injection (tuân thủ DIP).
  - Tách `DatabaseConfig` tập trung cấu hình: Cho phép hoán đổi giữa MySQL, PostgreSQL hoặc H2 (in-memory test) mà không cần sửa một dòng code nào trong `JdbcPaymentRepository` hay `PaymentService` (tuân thủ OCP).

### 2.4. Thông báo (`notificationService/`)
- Before: Khởi tạo trực tiếp `SMTP` và cấu hình mail ngay trong hàm thanh toán (vi phạm SRP, DIP).
- After:
  - Trừu tượng hóa thành interface `NotificationService` với phương thức `send(Notification)`.
  - Hiện tại triển khai `EmailNotification` (dùng `EmailConfig` và `MessageCommon`). Sau này mở rộng sang SMS, Push Notification chỉ cần viết thêm class mới mà không làm ảnh hưởng đến code thanh toán (tuân thủ OCP, DIP).

### 2.5. Service xử lý payment (`service/PaymentService` - Orchestrator)
- Before: Class ban đầu có quá nhiều trách nhiệm khác nhau, liên kết ràng buộc, không thể viết Unit Test độc lập cho từng trách nhiệm.
- After: Đóng vai trò là Điều phối viên nghiệp vụ (Orchestrator). Toàn bộ các thành phần phụ thuộc (`OrderValidator`, `PaymentGatewayFactory`, `PaymentRepository`, `NotificationService`) đều được Inject qua Constructor (tuân thủ DIP). Class chỉ có 1 lý do duy nhất để thay đổi khi quy trình nghiệp vụ tổng thể thay đổi (SRP), đồng thời có thể Mock 100% khi viết Unit Test.
