# Custom Business Metrics Implementation: Micrometer

## 1. Mã Nguồn Đo Lường Nghiệp Vụ (CheckoutMetrics.java)

Sử dụng Micrometer MeterRegistry để đăng ký các chỉ số nghiệp vụ với Common Tags tiêu chuẩn:

```java
package com.platform.order.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class CheckoutMetrics {

    private final Counter checkoutSuccess;
    private final Counter checkoutFailed;
    private final Counter paymentStatusUnknown;
    private final Counter orderStuckProcessing;

    public CheckoutMetrics(MeterRegistry registry) {
        this.checkoutSuccess = Counter.builder("checkout.success")
                .description("Số lượng giao dịch checkout hoàn tất thành công")
                .tag("service", "order-service")
                .register(registry);

        this.checkoutFailed = Counter.builder("checkout.failed")
                .description("Số lượng giao dịch checkout thất bại do lỗi kỹ thuật hoặc nghiệp vụ")
                .tag("service", "order-service")
                .register(registry);

        this.paymentStatusUnknown = Counter.builder("payment.status.unknown")
                .description("Số lượng giao dịch thanh toán rơi vào trạng thái timeout chưa rõ kết quả")
                .tag("service", "order-service")
                .register(registry);

        this.orderStuckProcessing = Counter.builder("order.stuck.processing")
                .description("Số lượng đơn hàng bị kẹt ở trạng thái PROCESSING quá 15 phút")
                .tag("service", "order-service")
                .register(registry);
    }

    public void incrementSuccess() {
        checkoutSuccess.increment();
    }

    public void incrementFailed(String reason) {
        checkoutFailed.increment();
    }

    public void incrementPaymentUnknown() {
        paymentStatusUnknown.increment();
    }

    public void incrementOrderStuck() {
        orderStuckProcessing.increment();
    }
}
```

---

## 2. Quy Tắc Tránh High Cardinality Anti-Pattern
- TUYỆT ĐỐI KHÔNG ĐƯỢC ĐẶT: `userId`, `orderId`, `email` hoặc bất kỳ giá trị UUID định danh cá nhân nào vào Micrometer Tags.
- Lý do kỹ thuật: Mỗi tổ hợp giá trị tag duy nhất sinh ra một chuỗi thời gian (Time-series) riêng biệt trong Prometheus. Việc đưa hàng triệu `orderId` vào tags sẽ gây hiện tượng Cardinality Explosion, làm cạn kiệt RAM của Prometheus Server và làm tê liệt hệ thống giám sát. Chỉ sử dụng các tags có số lượng giá trị hữu hạn cố định (Low Cardinality) như: `status_code`, `environment`, `service`, `payment_method`.
