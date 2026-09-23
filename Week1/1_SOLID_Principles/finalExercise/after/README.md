# Hướng dẫn tổ chức mã nguồn sau khi Refactor (`after/`)

Thư mục này là nơi bạn viết mã nguồn đã được tái cấu trúc (refactored) tuân thủ 5 nguyên tắc **SOLID**.

---

### Gợi ý cấu trúc Package chuẩn mực:

```text
after/
├── model/                  # Chứa dữ liệu nghiệp vụ (Entities / Value Objects / DTOs)
│   ├── Order.java
│   └── PaymentResult.java (hoặc Payment.java, PaymentMethod enum...)
│
├── validator/              # Phụ trách xác thực (Single Responsibility)
│   └── OrderValidator.java
│
├── gateway/                # Tách biệt cổng thanh toán (OCP, DIP, Strategy Pattern)
│   ├── PaymentGateway.java          (Interface chung)
│   ├── StripePaymentGateway.java    (Implement Stripe)
│   ├── PayPalPaymentGateway.java    (Implement PayPal)
│   ├── BankPaymentGateway.java      (Implement Bank)
│   └── PaymentGatewayFactory.java   (Hoặc Strategy Registry / Map để triệt tiêu if-else)
│
├── repository/             # Lưu trữ dữ liệu (DIP: High-level không dính chặt JDBC)
│   ├── PaymentRepository.java       (Interface)
│   └── JdbcPaymentRepository.java   (Implement JDBC)
│
├── notification/           # Gửi thông báo / Email (DIP & SRP)
│   ├── NotificationService.java     (Interface)
│   └── EmailNotificationService.java (Implement SMTP)
│
├── service/                # Lớp nghiệp vụ điều phối chính
│   └── PaymentService.java          (Inject các interface qua Constructor Injection)
│
└── Main.java               # (Tùy chọn) Hàm main chạy thử nghiệm toàn bộ luồng
```

> [!TIP]
> - Hãy chú ý **Constructor Injection**: Các dependency của `PaymentService` (`OrderValidator`, `PaymentGatewayFactory`/`Strategy`, `PaymentRepository`, `NotificationService`) đều nên được inject qua constructor.
> - Tuyệt đối tránh `new` trực tiếp các implementation cụ thể bên trong `PaymentService`.
> - Để xử lý OCP triệt để, hãy loại bỏ khối `if-else` / `switch-case` chọn cổng thanh toán bằng **Strategy Pattern** (kết hợp Map hoặc Factory).
