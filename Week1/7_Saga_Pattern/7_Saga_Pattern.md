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

---

# **Saga Pattern — Học chi tiết**

---

# **0. Intro: Saga Pattern là gì?**

## **Định nghĩa đơn giản**

**Saga Pattern là một cách quản lý distributed transaction (giao dịch phân tán) trong microservices mà không dùng 2-phase commit (2PC).**

## **Từ "Saga"**

**Saga** = một chuỗi sự kiện/hành động có thể được hoàn nguyên nếu có lỗi.

Ví dụ trong đời sống:

Code

`Đặt vé máy bay:
1. Đặt chuyến bay ✓
2. Đặt khách sạn ✓
3. Đặt xe cộ ❌ (hết chỗ)
   → Hoàn nguyên 1 & 2
   → Return money`

## **Vấn đề Saga giải quyết**

### **Trước Saga: 2-Phase Commit (2PC)**

Text

`Client
  ↓
Coordinator (Global Transaction)
  ├─ Phase 1: Prepare
  │  ├─ Ask Service A: "Can you do X?"
  │  ├─ Ask Service B: "Can you do Y?"
  │  ├─ Ask Service C: "Can you do Z?"
  │  └─ All said "Yes"
  │
  └─ Phase 2: Commit
     ├─ Tell A: "Do X"
     ├─ Tell B: "Do Y"
     └─ Tell C: "Do Z"`

**Vấn đề:**

- ❌ **Blocking**: Phải lock resources trong Phase 1.
- ❌ **Tight coupling**: Coordinator phụ thuộc tất cả services.
- ❌ **Scalability**: Khó scale microservices.
- ❌ **Single point of failure**: Nếu coordinator down, tất cả fail.

### **Sau Saga: Event-Driven**

Text

`Order Service
  ├─ CreateOrder ✓
  ├─ Publish: OrderCreatedEvent
  └─ Wait for callbacks

Payment Service (Async)
  ├─ Listen: OrderCreatedEvent
  ├─ Process payment ✓
  └─ Publish: PaymentProcessedEvent

Inventory Service (Async)
  ├─ Listen: PaymentProcessedEvent
  ├─ Reserve stock ❌
  └─ Publish: InventoryReservationFailedEvent

Payment Service (Compensate)
  ├─ Listen: InventoryReservationFailedEvent
  ├─ Refund payment
  └─ Publish: PaymentRefundedEvent`

**Lợi ích:**

- ✅ **Non-blocking**: Async, không lock.
- ✅ **Loose coupling**: Services independent.
- ✅ **Scalable**: Dễ add/remove services.
- ✅ **Resilient**: Failure isolation.

---

# **1. Distributed Transaction Problem**

## **Scenario: Order Processing Flow**

Text

`Order Service
    ├─ Create order
    └─ status = PENDING

Payment Service
    ├─ Process payment
    └─ status = PAID

Inventory Service
    ├─ Reserve stock
    └─ status = RESERVED

Shipping Service
    ├─ Create shipment
    └─ status = READY_TO_SHIP`

## **Vấn đề 1: Một service fail**

Code

`Scenario: Inventory reserve fail

Timeline:
1. Order created ✓
2. Payment processed ✓
3. Inventory reserve ❌ (out of stock)

Kết quả:
- Order đã được tạo
- Tiền đã bị trừ
- Stock không được trừ
- INCONSISTENT STATE! 💥`

## **Vấn đề 2: Không thể rollback**

Code

`Truyền thống CRUD:
Order = {id: 1, status: PENDING}
Order.update(status = PAID)  // Database rollback nếu error
Order.update(status = SHIPPED)

Nhưng với microservices:
- Order Service: UPDATE order SET status=PAID
- Payment Service: UPDATE payment SET status=PAID
- Nếu Shipping Service fail, không thể rollback!`

## **Vấn đề 3: Không có "all or nothing"**

Code

`ACID transaction guarantee:
- Atomicity: All or nothing
- Consistency: Valid state
- Isolation: No interference
- Durability: Persisted

Nhưng microservices:
- Không có global transaction
- Một service success, một service fail
- Inconsistent state 💥`

---

# **2. Saga là gì? (Chi tiết)**

## **Định nghĩa chi tiết**

**Saga là một distributed transaction pattern gồm:**

1. **Sequence of local transactions**: Mỗi microservice thực hiện 1 local transaction (trong DB riêng).
2. **Async communication**: Services giao tiếp qua events/commands (async).
3. **Compensating transactions**: Nếu fail, chạy compensation để hoàn nguyên.

## **Ví dụ: Order Processing Saga**

Text

`Order Saga Flow:

┌─────────────────────────────────────────┐
│ 1. Create Order (Local Transaction)     │
│    Order Service: INSERT order          │
└────────────┬────────────────────────────┘
             ↓ Publish: OrderCreatedEvent
┌─────────────────────────────────────────┐
│ 2. Process Payment (Local Transaction)  │
│    Payment Service: INSERT payment      │
└────────────┬────────────────────────────┘
             ↓ Publish: PaymentProcessedEvent
┌─────────────────────────────────────────┐
│ 3. Reserve Inventory (Local Transaction)│
│    Inventory Service: UPDATE stock      │
└────────────┬────────────────────────────┘
             ↓ Publish: InventoryReservedEvent
┌─────────────────────────────────────────┐
│ 4. Create Shipment (Local Transaction)  │
│    Shipping Service: INSERT shipment    │
└────────────┬────────────────────────────┘
             ↓ Publish: ShipmentCreatedEvent
             ✓ Saga Success

ERROR Case:

┌─────────────────────────────────────────┐
│ 1. Create Order ✓                       │
└────────────┬────────────────────────────┘
             ↓
┌─────────────────────────────────────────┐
│ 2. Process Payment ✓                    │
└────────────┬────────────────────────────┘
             ↓
┌─────────────────────────────────────────┐
│ 3. Reserve Inventory ❌ (OUT OF STOCK)  │
│    Publish: InventoryReservationFailed  │
└────────────┬────────────────────────────┘
             ↓ Trigger Compensations
┌─────────────────────────────────────────┐
│ Compensate Step 2: Refund Payment ✓     │
│    Payment Service: UPDATE refund       │
└────────────┬────────────────────────────┘
             ↓
┌─────────────────────────────────────────┐
│ Compensate Step 1: Cancel Order ✓       │
│    Order Service: UPDATE status=CANCELLED
└─────────────────────────────────────────┘`

---

# **3. Hai loại Saga**

## **Loại 1: Choreography (Cơm Kèm Canh)**

**Mỗi service tự biết nên làm gì tiếp theo qua events.**

### **Cách hoạt động**

Text

`Service A:
  ├─ Do task 1
  └─ Publish Event A

Service B (Listen Event A):
  ├─ Receive Event A
  ├─ Do task 2
  └─ Publish Event B

Service C (Listen Event B):
  ├─ Receive Event B
  ├─ Do task 3
  └─ Publish Event C

Service D (Listen Event C):
  ├─ Receive Event C
  └─ Do task 4`

### **Code Example: Choreography**

Java

`// ============================================
// SERVICE A: Order Service
// ============================================

@Service
public class OrderService {
    private final EventPublisher eventPublisher;
    
    public void createOrder(OrderRequest request) {
        // 1. Create order in database
        Order order = new Order(request);
        orderRepository.save(order);
        
        // 2. Publish event
        eventPublisher.publish(
            new OrderCreatedEvent(order.getId())
        );
        
        // That's it! Service B will do the next step
    }
}

// ============================================
// SERVICE B: Payment Service
// ============================================

@Component
public class PaymentEventListener {
    private final PaymentService paymentService;
    private final EventPublisher eventPublisher;
    
    @KafkaListener(topics = "OrderCreatedEvent")
    public void onOrderCreated(OrderCreatedEvent event) {
        try {
            // 1. Process payment
            paymentService.charge(event.getOrderId());
            
            // 2. Publish success event
            eventPublisher.publish(
                new PaymentProcessedEvent(event.getOrderId())
            );
        } catch (PaymentException e) {
            // 3. Publish failure event (for compensation)
            eventPublisher.publish(
                new PaymentFailedEvent(event.getOrderId(), e.getMessage())
            );
        }
    }
}

// ============================================
// SERVICE C: Inventory Service
// ============================================

@Component
public class InventoryEventListener {
    private final InventoryService inventoryService;
    private final EventPublisher eventPublisher;
    
    @KafkaListener(topics = "PaymentProcessedEvent")
    public void onPaymentProcessed(PaymentProcessedEvent event) {
        try {
            // 1. Reserve inventory
            inventoryService.reserve(event.getOrderId());
            
            // 2. Publish success event
            eventPublisher.publish(
                new InventoryReservedEvent(event.getOrderId())
            );
        } catch (OutOfStockException e) {
            // 3. Publish failure event (will trigger compensation)
            eventPublisher.publish(
                new InventoryReservationFailedEvent(
                    event.getOrderId(), 
                    e.getMessage()
                )
            );
        }
    }
}

// ============================================
// COMPENSATION: Payment Service
// ============================================

@Component
public class PaymentCompensationListener {
    private final PaymentService paymentService;
    
    @KafkaListener(topics = "InventoryReservationFailedEvent")
    public void onInventoryFailed(InventoryReservationFailedEvent event) {
        // Refund the payment (compensation)
        paymentService.refund(event.getOrderId());
    }
}

// ============================================
// COMPENSATION: Order Service
// ============================================

@Component
public class OrderCompensationListener {
    private final OrderService orderService;
    
    @KafkaListener(topics = "PaymentFailedEvent")
    public void onPaymentFailed(PaymentFailedEvent event) {
        // Cancel the order (compensation)
        orderService.cancel(event.getOrderId());
    }
}`

### **Diagram: Choreography**

Text

`Happy Path:
OrderService → OrderCreatedEvent → PaymentService → PaymentProcessedEvent → InventoryService → InventoryReservedEvent → ShippingService

Error Path (Compensation):
InventoryService ❌ → InventoryReservationFailedEvent → PaymentService (refund) → OrderService (cancel)`

### **Ưu điểm Choreography ✅**

| **Ưu điểm** | **Chi tiết** |
| --- | --- |
| **Simple** | Không cần orchestrator |
| **Decoupled** | Services độc lập |
| **Reactive** | Event-driven, tự nhiên |

### **Nhược điểm Choreography ❌**

| **Nhược điểm** | **Chi tiết** |
| --- | --- |
| **Implicit flow** | Khó trace flow: "Tại sao service B không chạy?" |
| **Circular dependency** | Service A depend event từ B, B depend từ A → phức tạp |
| **Hard to test** | Phải setup tất cả services, mocks |
| **Difficult to debug** | Khó biết service nào fail |

---

## **Loại 2: Orchestration (Có Maestro)**

**Một orchestrator tập trung điều phối toàn bộ flow.**

### **Cách hoạt động**

Text

`Saga Orchestrator:
  ├─ Step 1: Send command to Service A
  │  └─ Wait for reply
  ├─ Step 2: Send command to Service B
  │  └─ Wait for reply
  ├─ Step 3: Send command to Service C
  │  └─ Wait for reply (FAIL)
  └─ Compensate:
     ├─ Send refund to B
     └─ Send cancel to A`

### **Code Example: Orchestration**

Java

`// ============================================
// SAGA ORCHESTRATOR
// ============================================

@Service
public class OrderSagaOrchestrator {
    private final OrderService orderService;
    private final PaymentService paymentService;
    private final InventoryService inventoryService;
    private final ShippingService shippingService;
    private final SagaLog sagaLog;  // Track saga state
    
    public void executeOrderSaga(OrderRequest request) {
        String orderId = request.getOrderId();
        String sagaId = UUID.randomUUID().toString();
        
        try {
            // Step 1: Create Order
            sagaLog.logStep(sagaId, "CREATE_ORDER", "STARTED");
            orderService.createOrder(orderId, request);
            sagaLog.logStep(sagaId, "CREATE_ORDER", "COMPLETED");
            
            // Step 2: Process Payment
            sagaLog.logStep(sagaId, "PROCESS_PAYMENT", "STARTED");
            try {
                paymentService.charge(orderId, request.getTotal());
                sagaLog.logStep(sagaId, "PROCESS_PAYMENT", "COMPLETED");
            } catch (PaymentException e) {
                // Compensation for step 1
                orderService.cancel(orderId);
                throw e;
            }
            
            // Step 3: Reserve Inventory
            sagaLog.logStep(sagaId, "RESERVE_INVENTORY", "STARTED");
            try {
                inventoryService.reserve(orderId, request.getItems());
                sagaLog.logStep(sagaId, "RESERVE_INVENTORY", "COMPLETED");
            } catch (OutOfStockException e) {
                // Compensation for step 2 (LIFO: last in, first out)
                paymentService.refund(orderId);
                // Compensation for step 1
                orderService.cancel(orderId);
                throw e;
            }
            
            // Step 4: Create Shipment
            sagaLog.logStep(sagaId, "CREATE_SHIPMENT", "STARTED");
            try {
                shippingService.createShipment(orderId, request.getAddress());
                sagaLog.logStep(sagaId, "CREATE_SHIPMENT", "COMPLETED");
            } catch (ShippingException e) {
                // Compensation for step 3
                inventoryService.release(orderId);
                // Compensation for step 2
                paymentService.refund(orderId);
                // Compensation for step 1
                orderService.cancel(orderId);
                throw e;
            }
            
            // All steps completed
            sagaLog.logSagaStatus(sagaId, "COMPLETED");
            
        } catch (Exception e) {
            sagaLog.logSagaStatus(sagaId, "FAILED");
            throw e;
        }
    }
}

// ============================================
// SERVICES (Remote calls)
// ============================================

@Service
public class OrderService {
    private final OrderRepository repository;
    private final RestTemplate restTemplate;
    
    public void createOrder(String orderId, OrderRequest request) {
        Order order = new Order(orderId, request);
        repository.save(order);
    }
    
    public void cancel(String orderId) {
        Order order = repository.findById(orderId);
        order.setStatus("CANCELLED");
        repository.save(order);
    }
}

@Service
public class PaymentService {
    private final PaymentClient paymentClient;
    
    public void charge(String orderId, double amount) {
        PaymentResponse response = paymentClient.charge(orderId, amount);
        if (!response.isSuccess()) {
            throw new PaymentException("Charge failed");
        }
    }
    
    public void refund(String orderId) {
        PaymentResponse response = paymentClient.refund(orderId);
        if (!response.isSuccess()) {
            throw new PaymentException("Refund failed");
        }
    }
}

@Service
public class InventoryService {
    private final InventoryClient inventoryClient;
    
    public void reserve(String orderId, List<OrderItem> items) {
        InventoryResponse response = inventoryClient.reserve(orderId, items);
        if (!response.isSuccess()) {
            throw new OutOfStockException("Out of stock");
        }
    }
    
    public void release(String orderId) {
        inventoryClient.release(orderId);
    }
}

@Service
public class ShippingService {
    private final ShippingClient shippingClient;
    
    public void createShipment(String orderId, Address address) {
        ShippingResponse response = shippingClient.createShipment(orderId, address);
        if (!response.isSuccess()) {
            throw new ShippingException("Shipment creation failed");
        }
    }
}

// ============================================
// SAGA LOG (Track state)
// ============================================

@Entity
@Table(name = "saga_logs")
public class SagaLogEntity {
    @Id
    private Long id;
    
    @Column
    private String sagaId;
    
    @Column
    private String step;
    
    @Column
    private String status;  // STARTED, COMPLETED, FAILED
    
    @Column
    private Instant timestamp;
    
    // Constructor, getters
}

@Service
public class SagaLog {
    private final SagaLogRepository repository;
    
    public void logStep(String sagaId, String step, String status) {
        SagaLogEntity entity = new SagaLogEntity(sagaId, step, status);
        repository.save(entity);
        System.out.println("Saga " + sagaId + ": " + step + " -> " + status);
    }
    
    public void logSagaStatus(String sagaId, String status) {
        // Log final saga status
    }
}`

### **Diagram: Orchestration**

Text

`OrderSagaOrchestrator:
  ├─ Command: CreateOrder → OrderService → OK
  ├─ Command: ProcessPayment → PaymentService → OK
  ├─ Command: ReserveInventory → InventoryService → FAIL
  └─ Compensate:
     ├─ Command: RefundPayment → PaymentService → OK
     └─ Command: CancelOrder → OrderService → OK`

### **Ưu điểm Orchestration ✅**

| **Ưu điểm** | **Chi tiết** |
| --- | --- |
| **Explicit flow** | Rõ ràng, dễ hiểu |
| **Centralized** | Tất cả logic ở một chỗ |
| **Easy to debug** | Biết chính xác flow |
| **Easy to test** | Mock từng service |

### **Nhược điểm Orchestration ❌**

| **Nhược điểm** | **Chi tiết** |
| --- | --- |
| **God object** | Orchestrator biết tất cả services |
| **Tight coupling** | Orchestrator phụ thuộc tất cả services |
| **Single point of failure** | Nếu orchestrator down, saga fail |
| **Complex** | Phải code tất cả step & compensation |

---

# **4. Compensation Pattern (Hoàn nguyên)**

## **Định nghĩa**

**Compensation là một transaction ngược lại để undo một hành động đã hoàn thành.**

### **Ví dụ**

Text

`Transaction: charge(100) → Account balance -100
Compensation: refund(100) → Account balance +100

Transaction: reserve(10 items) → Stock -10
Compensation: release(10 items) → Stock +10

Transaction: create order → order.status = PENDING
Compensation: cancel order → order.status = CANCELLED`

## **Quy tắc Compensation**

### **Quy tắc 1: Thứ tự hoàn nguyên = LIFO (Last In, First Out)**

Text

`Forward:
1. Create Order ✓
2. Process Payment ✓
3. Reserve Inventory ✓
4. Create Shipment ❌

Backward (Compensation):
4. (Skip, never executed)
3. Release Inventory ✓
2. Refund Payment ✓
1. Cancel Order ✓`

### **Quy tắc 2: Compensation phải idempotent**

**Idempotent** = chạy nhiều lần có kết quả giống nhau.

Java

`// ✅ Idempotent refund
public void refund(String orderId) {
    Order order = orderRepository.findById(orderId);
    
    if (order.status == REFUNDED) {
        // Already refunded, do nothing
        return;
    }
    
    paymentGateway.refund(order.getPaymentId());
    order.setStatus(REFUNDED);
    orderRepository.save(order);
}

// ❌ Non-idempotent (Danger!)
public void refund(String orderId) {
    paymentGateway.refund(order.getPaymentId());  // Run 2x = 2 refunds!
}`

### **Quy tắc 3: Compensation phải ghi log**

Java

`public void refund(String orderId) {
    logger.info("Refunding order: " + orderId);
    
    try {
        paymentGateway.refund(orderId);
        logger.info("Refund succeeded for order: " + orderId);
    } catch (Exception e) {
        logger.error("Refund failed for order: " + orderId, e);
        // Alert team, retry later
    }
}`

---

# **5. Handling Failure & Retry**

## **Scenario: Partial Failure**

Text

`Order Saga:
1. Create Order ✓
2. Process Payment ✓
3. Reserve Inventory ✓
4. Create Shipment ❌ (Connection timeout)

Compensation:
- Release Inventory ✓
- Refund Payment → ??? Payment service down too!
  (Retry? Deadletter? Manual intervention?)`

## **Retry Strategy**

Java

`// ============================================
// RETRY WITH EXPONENTIAL BACKOFF
// ============================================

@Service
public class RetryableCompensation {
    private final static int MAX_RETRIES = 3;
    private final static int INITIAL_DELAY_MS = 1000;
    
    public void refundWithRetry(String orderId) {
        int retries = 0;
        long delay = INITIAL_DELAY_MS;
        
        while (retries < MAX_RETRIES) {
            try {
                paymentService.refund(orderId);
                return;  // Success
            } catch (Exception e) {
                retries++;
                
                if (retries >= MAX_RETRIES) {
                    // All retries exhausted
                    logger.error("Failed to refund after " + MAX_RETRIES + " attempts");
                    sendAlertToTeam("Manual intervention needed for order: " + orderId);
                    throw e;
                }
                
                // Exponential backoff
                logger.warn("Refund attempt " + retries + " failed, retry in " + delay + "ms");
                Thread.sleep(delay);
                delay *= 2;  // 1s, 2s, 4s, 8s, ...
            }
        }
    }
}

// ============================================
// DEAD LETTER QUEUE (DLQ)
// ============================================

@Component
public class CompensationHandler {
    @KafkaListener(topics = "InventoryReservationFailedEvent")
    public void handleInventoryFailed(InventoryReservationFailedEvent event) {
        try {
            paymentService.refund(event.getOrderId());
        } catch (Exception e) {
            // Send to DLQ for manual review
            deadLetterQueue.send(
                new DeadLetterMessage(
                    event.getOrderId(),
                    "Failed to refund after payment: " + e.getMessage()
                )
            );
        }
    }
}`

---

# **6. Idempotency Key**

## **Định nghĩa**

**Idempotency Key** là một unique ID cho một operation để tránh duplicate processing.

## **Ví dụ**

Text

`Request 1: 
  POST /refund
  body: {orderId: ORD1, amount: 100}
  header: Idempotency-Key: abc123
  → Process, refund $100
  
Request 2 (Duplicate):
  POST /refund
  body: {orderId: ORD1, amount: 100}
  header: Idempotency-Key: abc123
  → Check cache: abc123 already processed
  → Return previous response (no double refund!)`

## **Implementation**

Java

`// ============================================
// IDEMPOTENCY KEY
// ============================================

@Entity
@Table(name = "idempotency_keys")
public class IdempotencyKeyEntity {
    @Id
    private String key;
    
    @Column
    private String operation;  // "refund", "charge", ...
    
    @Column
    private String operationId;  // "ORD1", "ORD2", ...
    
    @Column(columnDefinition = "TEXT")
    private String response;  // Previous response (JSON)
    
    @Column
    private Instant createdAt;
    
    // Constructor, getters
}

@Service
public class IdempotentRefundService {
    private final IdempotencyKeyRepository keyRepository;
    private final PaymentService paymentService;
    
    public RefundResponse refund(String orderId, String idempotencyKey) {
        // 1. Check if already processed
        Optional<IdempotencyKeyEntity> existing = 
            keyRepository.findById(idempotencyKey);
        
        if (existing.isPresent()) {
            // Return cached response
            return parseResponse(existing.get().getResponse());
        }
        
        // 2. Process refund
        try {
            RefundResponse response = paymentService.refund(orderId);
            
            // 3. Cache response
            IdempotencyKeyEntity entity = new IdempotencyKeyEntity(
                idempotencyKey,
                "refund",
                orderId,
                objectMapper.writeValueAsString(response)
            );
            keyRepository.save(entity);
            
            return response;
        } catch (Exception e) {
            // Don't cache failures (allow retry)
            throw e;
        }
    }
}

// Usage in Controller:
@PostMapping("/refund")
public ResponseEntity<RefundResponse> refund(
    @RequestParam String orderId,
    @RequestHeader("Idempotency-Key") String idempotencyKey) {
    
    RefundResponse response = refundService.refund(orderId, idempotencyKey);
    return ResponseEntity.ok(response);
}`

---

# **7. Saga State Machine**

## **Định nghĩa**

**Saga State Machine** là flow từng trạng thái trong saga.

Text

`Saga States:

START → [CreateOrder] → ORDER_CREATED
     → [PaymentFailed] → COMPENSATING
     ↓
ORDER_CREATED → [ProcessPayment] → PAYMENT_PROCESSED
            → [PaymentFailed] → COMPENSATING
            ↓
PAYMENT_PROCESSED → [ReserveInventory] → INVENTORY_RESERVED
              → [InventoryFailed] → COMPENSATING
              ↓
INVENTORY_RESERVED → [CreateShipment] → SHIPMENT_CREATED
               → [ShippingFailed] → COMPENSATING
               ↓
SHIPMENT_CREATED → SAGA_SUCCEEDED

COMPENSATING → [RefundPayment] → PAYMENT_REFUNDED
          → [RefundFailed] → COMPENSATION_FAILED
          ↓
PAYMENT_REFUNDED → [CancelOrder] → ORDER_CANCELLED
           ↓
ORDER_CANCELLED → SAGA_FAILED`

## **Implementation**

Java

`// ============================================
// SAGA STATE MACHINE
// ============================================

public enum SagaState {
    START,
    ORDER_CREATED,
    PAYMENT_PROCESSED,
    INVENTORY_RESERVED,
    SHIPMENT_CREATED,
    SAGA_SUCCEEDED,
    COMPENSATING,
    PAYMENT_REFUNDED,
    ORDER_CANCELLED,
    SAGA_FAILED,
    COMPENSATION_FAILED
}

@Entity
@Table(name = "saga_instances")
public class SagaInstance {
    @Id
    private String sagaId;
    
    @Column
    private String orderId;
    
    @Enumerated(EnumType.STRING)
    @Column
    private SagaState state;
    
    @Column
    private Instant createdAt;
    
    @Column
    private Instant updatedAt;
    
    // Getters, setters
}

@Service
public class SagaStateMachine {
    private final SagaInstanceRepository repository;
    
    public void transitionState(String sagaId, SagaState newState) {
        SagaInstance instance = repository.findById(sagaId)
            .orElseThrow(() -> new SagaNotFoundException(sagaId));
        
        // Validate state transition
        if (!isValidTransition(instance.getState(), newState)) {
            throw new InvalidStateTransitionException(
                instance.getState() + " -> " + newState
            );
        }
        
        instance.setState(newState);
        instance.setUpdatedAt(Instant.now());
        repository.save(instance);
        
        logger.info("Saga " + sagaId + " transitioned to " + newState);
    }
    
    private boolean isValidTransition(SagaState from, SagaState to) {
        // Define valid transitions
        Map<SagaState, Set<SagaState>> validTransitions = new HashMap<>();
        validTransitions.put(SagaState.START, 
            Set.of(SagaState.ORDER_CREATED));
        validTransitions.put(SagaState.ORDER_CREATED, 
            Set.of(SagaState.PAYMENT_PROCESSED, SagaState.COMPENSATING));
        validTransitions.put(SagaState.PAYMENT_PROCESSED, 
            Set.of(SagaState.INVENTORY_RESERVED, SagaState.COMPENSATING));
        validTransitions.put(SagaState.INVENTORY_RESERVED, 
            Set.of(SagaState.SHIPMENT_CREATED, SagaState.COMPENSATING));
        validTransitions.put(SagaState.SHIPMENT_CREATED, 
            Set.of(SagaState.SAGA_SUCCEEDED, SagaState.COMPENSATING));
        validTransitions.put(SagaState.COMPENSATING, 
            Set.of(SagaState.PAYMENT_REFUNDED, SagaState.COMPENSATION_FAILED));
        validTransitions.put(SagaState.PAYMENT_REFUNDED, 
            Set.of(SagaState.ORDER_CANCELLED));
        validTransitions.put(SagaState.ORDER_CANCELLED, 
            Set.of(SagaState.SAGA_FAILED));
        
        return validTransitions.getOrDefault(from, Set.of()).contains(to);
    }
}`

---

# **8. Choreography vs Orchestration**

## **So sánh**

| **Aspect** | **Choreography** | **Orchestration** |
| --- | --- | --- |
| **Flow** | Distributed (events) | Centralized (orchestrator) |
| **Coupling** | Loose | Tight |
| **Complexity** | Simple per service | Complex orchestrator |
| **Debugging** | Hard (implicit) | Easy (explicit) |
| **Testing** | Hard (need all services) | Easy (mock services) |
| **Scalability** | Good | Limited (orchestrator bottleneck) |
| **Failure handling** | Distributed retry | Centralized retry |

## **Khi nào dùng Choreography**

✅ Dùng khi:

- Flow đơn giản, 2-3 services.
- Services độc lập, không nhiều dependencies.
- Không cần tập trung monitoring.

## **Khi nào dùng Orchestration**

✅ Dùng khi:

- Flow phức tạp, 5+ services.
- Cần clear logging & monitoring.
- Team muốn explicit flow.
- Compensation phức tạp.

---

# **9. Complete Saga Example**

Java

`// ============================================
// ORCHESTRATION SAGA
// ============================================

@Service
public class OrderSagaOrchestrator {
    private final OrderService orderService;
    private final PaymentService paymentService;
    private final InventoryService inventoryService;
    private final ShippingService shippingService;
    private final SagaStateMachine stateMachine;
    private final SagaLog sagaLog;
    
    public void executeOrderSaga(OrderRequest request) {
        String orderId = request.getOrderId();
        String sagaId = UUID.randomUUID().toString();
        
        try {
            stateMachine.create(sagaId, orderId);
            stateMachine.transitionState(sagaId, SagaState.START);
            
            // ========== STEP 1: Create Order ==========
            sagaLog.logStep(sagaId, "CREATE_ORDER", "STARTED");
            stateMachine.transitionState(sagaId, SagaState.ORDER_CREATED);
            orderService.createOrder(orderId, request);
            sagaLog.logStep(sagaId, "CREATE_ORDER", "COMPLETED");
            
            // ========== STEP 2: Process Payment ==========
            sagaLog.logStep(sagaId, "PROCESS_PAYMENT", "STARTED");
            try {
                paymentService.charge(orderId, request.getTotal());
                stateMachine.transitionState(sagaId, SagaState.PAYMENT_PROCESSED);
                sagaLog.logStep(sagaId, "PROCESS_PAYMENT", "COMPLETED");
            } catch (PaymentException e) {
                sagaLog.logStep(sagaId, "PROCESS_PAYMENT", "FAILED");
                stateMachine.transitionState(sagaId, SagaState.COMPENSATING);
                
                // Compensation 1
                orderService.cancel(orderId);
                throw e;
            }
            
            // ========== STEP 3: Reserve Inventory ==========
            sagaLog.logStep(sagaId, "RESERVE_INVENTORY", "STARTED");
            try {
                inventoryService.reserve(orderId, request.getItems());
                stateMachine.transitionState(sagaId, SagaState.INVENTORY_RESERVED);
                sagaLog.logStep(sagaId, "RESERVE_INVENTORY", "COMPLETED");
            } catch (OutOfStockException e) {
                sagaLog.logStep(sagaId, "RESERVE_INVENTORY", "FAILED");
                stateMachine.transitionState(sagaId, SagaState.COMPENSATING);
                
                // Compensation 2 (LIFO)
                paymentService.refund(orderId);
                // Compensation 1
                orderService.cancel(orderId);
                throw e;
            }
            
            // ========== STEP 4: Create Shipment ==========
            sagaLog.logStep(sagaId, "CREATE_SHIPMENT", "STARTED");
            try {
                shippingService.createShipment(orderId, request.getAddress());
                stateMachine.transitionState(sagaId, SagaState.SHIPMENT_CREATED);
                sagaLog.logStep(sagaId, "CREATE_SHIPMENT", "COMPLETED");
            } catch (ShippingException e) {
                sagaLog.logStep(sagaId, "CREATE_SHIPMENT", "FAILED");
                stateMachine.transitionState(sagaId, SagaState.COMPENSATING);
                
                // Compensation 3 (LIFO)
                inventoryService.release(orderId);
                // Compensation 2
                paymentService.refund(orderId);
                // Compensation 1
                orderService.cancel(orderId);
                throw e;
            }
            
            // ========== SUCCESS ==========
            stateMachine.transitionState(sagaId, SagaState.SAGA_SUCCEEDED);
            sagaLog.logSagaStatus(sagaId, "SUCCESS");
            
        } catch (Exception e) {
            stateMachine.transitionState(sagaId, SagaState.SAGA_FAILED);
            sagaLog.logSagaStatus(sagaId, "FAILED");
            throw e;
        }
    }
}`

---

# **10. Checklist: Saga Pattern**

- [ ]  Distributed transaction: chia thành local transactions.
- [ ]  Choreography: events-driven, loose coupling.
- [ ]  Orchestration: orchestrator, tight coupling.
- [ ]  Compensation: undo, LIFO order.
- [ ]  Idempotent: chạy many times, same result.
- [ ]  Retry: với exponential backoff.
- [ ]  Idempotency Key: tránh duplicate.
- [ ]  State Machine: track saga state.
- [ ]  Logging: ghi lại mỗi step.
- [ ]  DLQ: deadletter queue cho failures.