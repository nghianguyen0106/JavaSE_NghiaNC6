# Idempotent Consumer Design: At-Least-Once Delivery Handling

## 1. Cơ Chế Khử Trùng Lặp (Idempotency Mechanism)

Do mạng chập chờn, rebalance consumer group hoặc producer retry, một consumer có thể nhận cùng một event nhiều lần. Để đảm bảo tính bất biến của kết quả, Consumer áp dụng kỹ thuật **Idempotency via Unique Constraint (RDBMS Table)**:

```sql
CREATE TABLE processed_events (
    event_id UUID NOT NULL,
    consumer_name VARCHAR(100) NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (event_id, consumer_name)
);
```

---

## 2. Mã Nguồn Mẫu (Spring Cloud Stream / Kafka Consumer)

```java
@Component
public class NotificationOrderConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationOrderConsumer.class);
    private final ProcessedEventRepository processedEventRepository;
    private final EmailDispatchService emailDispatchService;

    public NotificationOrderConsumer(ProcessedEventRepository repo, EmailDispatchService emailService) {
        this.processedEventRepository = repo;
        this.emailDispatchService = emailService;
    }

    @Transactional
    public void consumeOrderConfirmed(OrderConfirmedEvent event) {
        UUID eventId = UUID.fromString(event.getId());
        String consumerName = "NotificationConsumer";

        // 1. Kiểm tra và ghi nhận xử lý nguyên tử (Atomic Check-and-Insert)
        try {
            processedEventRepository.saveAndFlush(new ProcessedEvent(eventId, consumerName));
        } catch (DataIntegrityViolationException ex) {
            // Đã tồn tại bản ghi -> Event đã được xử lý thành công trước đó
            log.warn("Duplicate event detected: eventId={}, consumer={}. Skipping processing.", eventId, consumerName);
            return; // Trả về an toàn để Kafka commit offset
        }

        // 2. Thực thi nghiệp vụ phụ trợ (chỉ chạy duy nhất 1 lần)
        emailDispatchService.sendOrderConfirmationEmail(
            event.getData().getCustomerEmail(),
            event.getData().getOrderId(),
            event.getData().getTotalAmount()
        );
        log.info("Successfully processed order confirmation notification for orderId={}", event.getData().getOrderId());
    }
}
```
