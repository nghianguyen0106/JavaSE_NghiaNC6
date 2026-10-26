# Message Contract Specification: OrderConfirmed Event

## 1. Hợp Đồng Bản Tin Không Đồng Bộ (Spring Cloud Contract Message DSL)

Hợp đồng xác lập cấu trúc message giữa Producer (`order-service`) và Consumer (`notification-service`):

```groovy
package contracts.events

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Khi đơn hàng xác nhận thành công, Order Service xuất bản sự kiện OrderConfirmed sang Message Broker"
    
    label 'order_confirmed_trigger'
    
    input {
        triggeredBy('publishOrderConfirmedEvent()')
    }
    
    outputMessage {
        sentTo 'com.platform.orders'
        headers {
            header('contentType', 'application/json')
            header('ce_type', 'OrderConfirmed')
            header('X-Correlation-Id', $(consumer(regex('[a-zA-Z0-9-]+')), producer('c1a8-9921-ef4b')))
        }
        body([
            id: $(consumer(regex('^[0-9a-fA-F-]+$')), producer('evt-77b31e92-31fa-4f2e-bc35-64d8a1789c01')),
            orderId: $(consumer('ord-99210-vn'), producer(regex('.+'))),
            customerEmail: $(consumer('buyer@domain.com'), producer(regex('^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$'))),
            totalAmount: $(consumer(2500000.00), producer(regex('^[0-9]+\\.[0-9]{2}$'))),
            status: $(consumer('CONFIRMED'), producer('CONFIRMED'))
        ])
    }
}
```

---

## 2. Kiểm Thử Khử Phụ Thuộc (Consumer-Driven Verification)
Phía `notification-service` không cần kết nối tới Kafka thật trong Unit/Integration Test:
1. Spring Cloud Contract Stub Runner nạp hợp đồng trên và giả lập (trigger) việc bắn message vào channel nội bộ của test runner.
2. `NotificationConsumer` nhận message và kích hoạt xử lý.
3. Test case assert xem hàm gửi email có được gọi đúng với email và số tiền trong contract hay không.
