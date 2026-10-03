# 6. Event Sourcing
## **Cần học**

Mentee cần tìm hiểu:

- Event là gì.
- Event Sourcing là gì.
- State được rebuild từ event như thế nào.
- Event khác command như thế nào.
- Event versioning.
- Replay event.
- Ưu điểm và hạn chế.

## **Cần hiểu**

Ví dụ các event:

Text

`OrderCreated
ItemAddedToOrder
OrderConfirmed
OrderCancelled`

Thay vì chỉ lưu:

Text

`Order status = CONFIRMED`

Hệ thống lưu chuỗi sự kiện:

Text

`OrderCreated
ItemAddedToOrder
OrderConfirmed`

Sau đó rebuild lại trạng thái order từ các event này.

## **Tiêu chí hoàn thành**

- Phân biệt được command và event.
- Hiểu event là điều đã xảy ra.
- Hiểu event sourcing không đơn giản chỉ là publish message.
- Biết nêu ít nhất hai ưu điểm và hai hạn chế.

## **Ưu & Nhược điểm Event Sourcing**

#### **Ưu điểm ✅**

| **Ưu điểm** | **Chi tiết** |
| --- | --- |
| **Complete audit trail** | Tất cả thay đổi được ghi lại |
| **Time travel** | Có thể rebuild state tại bất kỳ thời điểm |
| **Debugging** | Dễ debug: xem lại sequence of events |
| **Event-driven** | Natural fit với event-driven architecture |
| **No data loss** | Không mất bất kỳ info nào |
| **Concurrent writes** | Không race condition (append-only) |

#### **Nhược điểm ❌**

| **Nhược điểm** | **Chi tiết** |
| --- | --- |
| **Complexity** | Phức tạp hơn CRUD, khó học |
| **Event versioning** | Thay đổi event schema khó |
| **Eventual consistency** | Read model lag behind write |
| **Storage** | Lưu tất cả events → lớn hơn |
| **Query difficulty** | Phải rebuild state để query |
| **Debugging hard** | Trace flow phức tạp: events → handlers → updates |

---

# **Event Sourcing — Học chi tiết**

---

# **0. Intro: Event Sourcing là gì?**

**Event Sourcing** là một cách lưu trữ dữ liệu bằng cách ghi lại **mọi thay đổi** (events) thay vì chỉ lưu **state cuối cùng**.

Ghi nhớ 1 ý chính:

> **Thay vì lưu "Order status = CONFIRMED", hãy lưu "OrderConfirmed event happened at 2025-01-15 10:30:00".**
> 

---

# **1. Vấn đề Event Sourcing giải quyết**

## **Vấn đề 1: Mất lịch sử thay đổi**

### **Trước khi dùng Event Sourcing**

Java

`// Truyền thống: chỉ lưu state hiện tại
@Entity
public class Order {
    @Id
    private String id;
    
    @Column
    private OrderStatus status;  // CONFIRMED
    
    @Column
    private double total;        // 200.0
    
    @Column
    private Timestamp updatedAt; // 2025-01-15 10:30:00
}

// Database
orders table
┌─────┬──────────┬───────┬─────────────────────┐
│ id  │ status   │ total │ updatedAt           │
├─────┼──────────┼───────┼─────────────────────┤
│ ORD1│CONFIRMED │ 200.0 │ 2025-01-15 10:30:00 │
└─────┴──────────┴───────┴─────────────────────┘`

**Vấn đề:**

- ❌ Không biết order trải qua những status nào.
- ❌ Không biết tổng tiền thay đổi như thế nào.
- ❌ Không biết ai thay đổi, khi nào, tại sao.
- ❌ Không thể "rewind" quay lại state cũ.
- ❌ Audit trail không đầy đủ.

## **Vấn đề 2: Race condition khi cập nhật**

Java

`// Scenario: 2 thread cùng update order
Thread 1: order.confirm()
Thread 2: order.cancel()

// Kết quả? Undefined behavior, có thể race condition.`

## **Vấn đề 3: Không có single source of truth**

Code

`Database có state hiện tại: CONFIRMED
Nhưng làm sao biết nó đến được CONFIRMED từ đâu?
- Từ PENDING → CONFIRMED?
- Hay từ CANCELLED → CONFIRMED (impossible)?`

---

# **2. Event Sourcing là gì?**

## **Khái niệm**

**Lưu chuỗi events (sự kiện) thay vì state.**

Text

`Truyền thống:
  Order: { id: ORD1, status: CONFIRMED, total: 200 }
  
Event Sourcing:
  Events:
  1. OrderCreated(id: ORD1, items: [...])
  2. ItemAdded(id: ORD1, productId: P1, qty: 2, price: 100)
  3. OrderConfirmed(id: ORD1, timestamp: ...)
  4. OrderShipped(id: ORD1, timestamp: ...)`

## **Rebuild State từ Events**

Text

`Start: Order không tồn tại

Apply Event 1 (OrderCreated):
  Order = { status: PENDING, items: [...] }

Apply Event 2 (ItemAdded):
  Order = { status: PENDING, items: [..., new item] }

Apply Event 3 (OrderConfirmed):
  Order = { status: CONFIRMED, items: [...] }

Apply Event 4 (OrderShipped):
  Order = { status: SHIPPED, items: [...] }
  
Result: Current state = SHIPPED`

---

# **3. Event vs State**

## **Sự khác biệt**

### **State (Truyền thống)**

Java

`// Snapshot tại thời điểm hiện tại
public class Order {
    private String id = "ORD1";
    private OrderStatus status = OrderStatus.SHIPPED;
    private double total = 200;
    private List<OrderItem> items = [...];
}

// Lưu vào database
orders
├─ id: ORD1
├─ status: SHIPPED
├─ total: 200
└─ items: [...]`

**Đặc điểm:**

- Snapshot tại 1 thời điểm.
- Mất lịch sử.
- Chỉ biết current state.

### **Events (Event Sourcing)**

Java

`// Chuỗi thay đổi lịch sử
public abstract class Event {
    protected String orderId;
    protected Instant timestamp;
}

public class OrderCreatedEvent extends Event {
    private List<OrderItem> items;
}

public class OrderConfirmedEvent extends Event {
    // Không cần data, chỉ ghi "confirmed"
}

public class OrderShippedEvent extends Event {
    private String trackingNumber;
}

// Lưu vào database
events
├─ 1: OrderCreatedEvent(ORD1, [P1x2], 2025-01-15 10:00:00)
├─ 2: OrderConfirmedEvent(ORD1, 2025-01-15 10:15:00)
└─ 3: OrderShippedEvent(ORD1, TRACK123, 2025-01-15 11:00:00)`

**Đặc điểm:**

- Chuỗi lịch sử.
- Có đầy đủ thông tin.
- Có thể rebuild lại bất kỳ state nào.

---

# **4. Event Sourcing Architecture**

## **Sơ đồ**

Text

`┌─────────────────────────────────────────────────────────┐
│                   Command (Write)                       │
│          (POST /orders, POST /orders/confirm)           │
└──────────────────────┬──────────────────────────────────┘
                       ↓
            ┌──────────────────────┐
            │ Command Handler      │
            │ CreateOrderHandler   │
            └──────────┬───────────┘
                       ↓
            ┌──────────────────────┐
            │ Domain Logic         │
            │ Order.confirm()      │
            └──────────┬───────────┘
                       ↓
            ┌──────────────────────┐
            │ Generate Events      │
            │ OrderConfirmedEvent  │
            └──────────┬───────────┘
                       ↓
            ┌──────────────────────┐
            │ Event Store (DB)     │
            │ events table         │
            └──────────┬───────────┘
                       ↓
            ┌──────────────────────┐
            │ Snapshot (optional)  │
            │ For performance      │
            └──────────┬───────────┘
                       ↓
            ┌──────────────────────┐
            │ Publish Event        │
            │ (Kafka, etc.)        │
            └──────────┬───────────┘
                       ↓
┌──────────────────────────────────────────────────────────┐
│            Query (Read)                                  │
│      (GET /orders, GET /orders?status=SHIPPED)           │
│                                                          │
│  1. Fetch events from event store                        │
│  2. Replay events                                        │
│  3. Build current state                                  │
│  4. Return to client                                     │
└──────────────────────────────────────────────────────────┘`

---

# **5. Chi tiết: Event Sourcing Implementation**

## **Step 1: Define Events**

Java

`// ============================================
// BASE EVENT CLASS
// ============================================

public abstract class DomainEvent {
    private final String aggregateId;        // Aggregate root ID (OrderId)
    private final Instant timestamp;
    private final int version;               // Event version (for schema evolution)
    
    public DomainEvent(String aggregateId, int version) {
        this.aggregateId = aggregateId;
        this.timestamp = Instant.now();
        this.version = version;
    }
    
    public String getAggregateId() { return aggregateId; }
    public Instant getTimestamp() { return timestamp; }
    public int getVersion() { return version; }
}

// ============================================
// CONCRETE EVENTS
// ============================================

public class OrderCreatedEvent extends DomainEvent {
    private final List<OrderItemData> items;
    
    public OrderCreatedEvent(String orderId, List<OrderItemData> items) {
        super(orderId, 1);  // version 1
        this.items = items;
    }
    
    public List<OrderItemData> getItems() { return items; }
}

public class OrderItemData {
    private final String productId;
    private final int quantity;
    private final double price;
    
    public OrderItemData(String productId, int quantity, double price) {
        this.productId = productId;
        this.quantity = quantity;
        this.price = price;
    }
    
    // Getters
}

public class OrderConfirmedEvent extends DomainEvent {
    private final double total;
    
    public OrderConfirmedEvent(String orderId, double total) {
        super(orderId, 1);
        this.total = total;
    }
    
    public double getTotal() { return total; }
}

public class OrderShippedEvent extends DomainEvent {
    private final String trackingNumber;
    
    public OrderShippedEvent(String orderId, String trackingNumber) {
        super(orderId, 1);
        this.trackingNumber = trackingNumber;
    }
    
    public String getTrackingNumber() { return trackingNumber; }
}

public class OrderCancelledEvent extends DomainEvent {
    private final String reason;
    
    public OrderCancelledEvent(String orderId, String reason) {
        super(orderId, 1);
        this.reason = reason;
    }
    
    public String getReason() { return reason; }
}`

## **Step 2: Event Store (Persist Events)**

Java

`// ============================================
// EVENT STORE INTERFACE
// ============================================

public interface EventStore {
    // Lưu event
    void append(DomainEvent event);
    
    // Lấy tất cả events của aggregate
    List<DomainEvent> getEvents(String aggregateId);
    
    // Lấy events từ version nào đó (cho snapshot)
    List<DomainEvent> getEventsSince(String aggregateId, int fromVersion);
}

// ============================================
// EVENT STORE IMPLEMENTATION (Database)
// ============================================

@Repository
public class JpaEventStore implements EventStore {
    private final EventJpaRepository jpaRepository;
    private final ObjectMapper objectMapper;
    
    public JpaEventStore(EventJpaRepository jpaRepository, ObjectMapper objectMapper) {
        this.jpaRepository = jpaRepository;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public void append(DomainEvent event) {
        EventEntity entity = new EventEntity(
            event.getAggregateId(),
            event.getClass().getSimpleName(),  // Event type
            objectMapper.writeValueAsString(event),  // Event data as JSON
            event.getTimestamp(),
            event.getVersion()
        );
        jpaRepository.save(entity);
    }
    
    @Override
    public List<DomainEvent> getEvents(String aggregateId) {
        return jpaRepository.findByAggregateIdOrderByVersionAsc(aggregateId)
            .stream()
            .map(this::toDomainEvent)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<DomainEvent> getEventsSince(String aggregateId, int fromVersion) {
        return jpaRepository
            .findByAggregateIdAndVersionGreaterThanEqualOrderByVersionAsc(aggregateId, fromVersion)
            .stream()
            .map(this::toDomainEvent)
            .collect(Collectors.toList());
    }
    
    private DomainEvent toDomainEvent(EventEntity entity) {
        try {
            Class<?> eventClass = Class.forName("com.example.event." + entity.getEventType());
            return (DomainEvent) objectMapper.readValue(entity.getData(), eventClass);
        } catch (Exception e) {
            throw new RuntimeException("Error deserializing event", e);
        }
    }
}

// ============================================
// EVENT ENTITY (Database Table)
// ============================================

@Entity
@Table(name = "events")
public class EventEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column
    private String aggregateId;  // OrderId
    
    @Column
    private String eventType;    // "OrderCreatedEvent"
    
    @Column(columnDefinition = "TEXT")
    private String data;         // JSON
    
    @Column
    private Instant timestamp;
    
    @Column
    private int version;         // Sequential version per aggregate
    
    public EventEntity() {}
    
    public EventEntity(String aggregateId, String eventType, String data, Instant timestamp, int version) {
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.data = data;
        this.timestamp = timestamp;
        this.version = version;
    }
    
    // Getters
}

// ============================================
// SPRING DATA REPOSITORY
// ============================================

@Repository
public interface EventJpaRepository extends JpaRepository<EventEntity, Long> {
    List<EventEntity> findByAggregateIdOrderByVersionAsc(String aggregateId);
    
    List<EventEntity> findByAggregateIdAndVersionGreaterThanEqualOrderByVersionAsc(
        String aggregateId, int fromVersion);
}`

## **Step 3: Aggregate Rebuilding**

Java

`// ============================================
// AGGREGATE WITH EVENT SOURCING
// ============================================

public class Order {
    private final OrderId id;
    private final List<OrderItem> items;
    private OrderStatus status;
    private Money total;
    
    // Private constructor (không public)
    private Order(OrderId id) {
        this.id = id;
        this.items = new ArrayList<>();
        this.status = OrderStatus.PENDING;
        this.total = new Money(0);
    }
    
    // ========== CREATE NEW AGGREGATE ==========
    
    public static Order create(OrderId id, List<OrderItem> items) {
        Order order = new Order(id);
        order.addItems(items);
        // Không save vào database ở đây
        return order;
    }
    
    // ========== REBUILD FROM EVENTS ==========
    
    public static Order fromEvents(List<DomainEvent> events) {
        Order order = null;
        
        for (DomainEvent event : events) {
            if (event instanceof OrderCreatedEvent) {
                order = new Order(
                    new OrderId(event.getAggregateId())
                );
                
                OrderCreatedEvent createdEvent = (OrderCreatedEvent) event;
                for (OrderItemData itemData : createdEvent.getItems()) {
                    order.items.add(new OrderItem(
                        new ProductId(itemData.getProductId()),
                        itemData.getQuantity(),
                        new Money(itemData.getPrice())
                    ));
                }
                order.total = order.calculateTotal();
                
            } else if (event instanceof OrderConfirmedEvent) {
                OrderConfirmedEvent confirmedEvent = (OrderConfirmedEvent) event;
                order.status = OrderStatus.CONFIRMED;
                order.total = new Money(confirmedEvent.getTotal());
                
            } else if (event instanceof OrderShippedEvent) {
                order.status = OrderStatus.SHIPPED;
                
            } else if (event instanceof OrderCancelledEvent) {
                order.status = OrderStatus.CANCELLED;
            }
        }
        
        return order;
    }
    
    // ========== BUSINESS OPERATIONS (Generate Events) ==========
    
    public List<DomainEvent> getUncommittedEvents() {
        // Return events that haven't been saved yet
        // (implementation varies)
        return new ArrayList<>();
    }
    
    public void confirm() {
        if (status != OrderStatus.PENDING) {
            throw new DomainException("Can only confirm PENDING orders");
        }
        
        // Don't change state directly
        // Instead, record event
        this.status = OrderStatus.CONFIRMED;
        
        // Generate event (to be saved by event store)
    }
    
    public void ship(String trackingNumber) {
        if (status != OrderStatus.CONFIRMED) {
            throw new DomainException("Can only ship CONFIRMED orders");
        }
        
        this.status = OrderStatus.SHIPPED;
    }
    
    public void cancel(String reason) {
        if (status == OrderStatus.SHIPPED) {
            throw new DomainException("Cannot cancel SHIPPED orders");
        }
        
        this.status = OrderStatus.CANCELLED;
    }
    
    // ========== GETTERS ==========
    
    public OrderId getId() { return id; }
    public List<OrderItem> getItems() { return new ArrayList<>(items); }
    public OrderStatus getStatus() { return status; }
    public Money getTotal() { return total; }
    
    private void addItems(List<OrderItem> newItems) {
        if (newItems == null || newItems.isEmpty()) {
            throw new DomainException("Order must have items");
        }
        items.addAll(newItems);
        this.total = calculateTotal();
    }
    
    private Money calculateTotal() {
        Money sum = new Money(0);
        for (OrderItem item : items) {
            sum = sum.add(item.getSubtotal());
        }
        return sum;
    }
}`

## **Step 4: Command Handler (Generate & Save Events)**

Java

`// ============================================
// COMMAND HANDLER WITH EVENT SOURCING
// ============================================

@Service
public class CreateOrderCommandHandler {
    private final EventStore eventStore;
    private final EventPublisher eventPublisher;
    private final OrderValidator validator;
    
    public CreateOrderCommandHandler(
        EventStore eventStore,
        EventPublisher eventPublisher,
        OrderValidator validator
    ) {
        this.eventStore = eventStore;
        this.eventPublisher = eventPublisher;
        this.validator = validator;
    }
    
    public void handle(CreateOrderCommand command) {
        // 1. Validate
        validator.validate(command);
        
        // 2. Create aggregate (new)
        Order order = Order.create(
            new OrderId(command.getOrderId()),
            command.getItems().stream()
                .map(item -> new OrderItem(
                    new ProductId(item.getProductId()),
                    item.getQuantity(),
                    new Money(item.getPrice())
                ))
                .collect(Collectors.toList())
        );
        
        // 3. Apply business operation
        order.confirm();
        
        // 4. Generate event
        OrderCreatedEvent event = new OrderCreatedEvent(
            order.getId().getValue(),
            command.getItems().stream()
                .map(item -> new OrderItemData(
                    item.getProductId(),
                    item.getQuantity(),
                    item.getPrice()
                ))
                .collect(Collectors.toList())
        );
        
        // 5. Save event to event store
        eventStore.append(event);
        
        // 6. Generate confirm event
        OrderConfirmedEvent confirmEvent = new OrderConfirmedEvent(
            order.getId().getValue(),
            order.getTotal().getAmount()
        );
        eventStore.append(confirmEvent);
        
        // 7. Publish events (async)
        eventPublisher.publish(event);
        eventPublisher.publish(confirmEvent);
    }
}

@Service
public class ShipOrderCommandHandler {
    private final EventStore eventStore;
    private final EventPublisher eventPublisher;
    
    public void handle(ShipOrderCommand command) {
        // 1. Fetch events
        List<DomainEvent> events = eventStore.getEvents(command.getOrderId());
        
        // 2. Rebuild aggregate from events
        Order order = Order.fromEvents(events);
        
        // 3. Apply business operation
        order.ship(command.getTrackingNumber());
        
        // 4. Generate event
        OrderShippedEvent event = new OrderShippedEvent(
            order.getId().getValue(),
            command.getTrackingNumber()
        );
        
        // 5. Save event
        eventStore.append(event);
        
        // 6. Publish event
        eventPublisher.publish(event);
    }
}`

## **Step 5: Query (Rebuild State)**

Java

`// ============================================
// QUERY HANDLER WITH EVENT SOURCING
// ============================================

@Service
public class GetOrderQueryHandler {
    private final EventStore eventStore;
    
    public GetOrderQueryHandler(EventStore eventStore) {
        this.eventStore = eventStore;
    }
    
    public OrderResponse handle(GetOrderQuery query) {
        // 1. Fetch all events for this order
        List<DomainEvent> events = eventStore.getEvents(query.getOrderId());
        
        // 2. Rebuild order from events
        Order order = Order.fromEvents(events);
        
        // 3. Return current state
        return new OrderResponse(
            order.getId().getValue(),
            order.getTotal().getAmount(),
            order.getStatus().toString()
        );
    }
}`

---

# **6. Snapshot Pattern (Optimization)**

## **Vấn đề**

Nếu order có 10,000 events, mỗi query phải replay 10,000 events → chậm!

## **Giải pháp: Snapshot**

Periodically save **snapshot** (state at a specific point).

Text

`Events:
1. OrderCreatedEvent
2. OrderConfirmedEvent
3. OrderShippedEvent
...
1000. OrderRefundEvent

Instead of replaying 1000 events:
- Fetch latest snapshot (at event 800)
- Rebuild state from snapshot
- Apply remaining events (801-1000)
- Much faster!`

## **Implementation**

Java

`// ============================================
// SNAPSHOT
// ============================================

public class OrderSnapshot {
    private final String aggregateId;
    private final int version;  // At which event version
    private final OrderStatus status;
    private final double total;
    private final List<OrderItemData> items;
    
    // Constructor, getters
}

// ============================================
// EVENT STORE WITH SNAPSHOT
// ============================================

@Repository
public class SnapshotEventStore implements EventStore {
    private final EventJpaRepository eventRepository;
    private final SnapshotJpaRepository snapshotRepository;
    
    @Override
    public List<DomainEvent> getEvents(String aggregateId) {
        // 1. Try to fetch snapshot
        Optional<SnapshotEntity> latestSnapshot = 
            snapshotRepository.findLatestByAggregateId(aggregateId);
        
        if (latestSnapshot.isPresent()) {
            // 2. Fetch events since snapshot
            int fromVersion = latestSnapshot.get().getVersion();
            return eventRepository
                .findByAggregateIdAndVersionGreaterThanOrderByVersionAsc(
                    aggregateId, fromVersion
                )
                .stream()
                .map(this::toDomainEvent)
                .collect(Collectors.toList());
        } else {
            // 3. No snapshot, fetch all events
            return eventRepository.findByAggregateIdOrderByVersionAsc(aggregateId)
                .stream()
                .map(this::toDomainEvent)
                .collect(Collectors.toList());
        }
    }
    
    @Override
    public void append(DomainEvent event) {
        // Save event
        EventEntity entity = new EventEntity(...);
        eventRepository.save(entity);
        
        // Every 100 events, create snapshot
        List<DomainEvent> allEvents = getEvents(event.getAggregateId());
        if (allEvents.size() % 100 == 0) {
            Order order = Order.fromEvents(allEvents);
            
            SnapshotEntity snapshot = new SnapshotEntity(
                event.getAggregateId(),
                order.getStatus(),
                order.getTotal(),
                order.getItems(),
                allEvents.size()  // version
            );
            snapshotRepository.save(snapshot);
        }
    }
}

// ============================================
// SNAPSHOT ENTITY
// ============================================

@Entity
@Table(name = "snapshots")
public class SnapshotEntity {
    @Id
    private Long id;
    
    @Column
    private String aggregateId;
    
    @Column
    private String status;
    
    @Column
    private double total;
    
    @Column(columnDefinition = "TEXT")
    private String items;  // JSON
    
    @Column
    private int version;  // At which event
    
    @Column
    private Instant timestamp;
    
    // Constructor, getters
}

@Repository
public interface SnapshotJpaRepository extends JpaRepository<SnapshotEntity, Long> {
    Optional<SnapshotEntity> findLatestByAggregateId(String aggregateId);
}`

---

# **7. Event Store Queries**

## **Không chỉ dùng cho Rebuild**

Event store có thể query lấy lịch sử.

Java

`// ============================================
// QUERY HISTORY
// ============================================

@Service
public class OrderHistoryQueryHandler {
    private final EventStore eventStore;
    
    public List<OrderEventResponse> getHistory(String orderId) {
        List<DomainEvent> events = eventStore.getEvents(orderId);
        
        return events.stream()
            .map(event -> new OrderEventResponse(
                event.getClass().getSimpleName(),
                event.getTimestamp(),
                eventToString(event)
            ))
            .collect(Collectors.toList());
    }
    
    private String eventToString(DomainEvent event) {
        if (event instanceof OrderCreatedEvent) {
            OrderCreatedEvent e = (OrderCreatedEvent) event;
            return "Order created with " + e.getItems().size() + " items";
        } else if (event instanceof OrderConfirmedEvent) {
            OrderConfirmedEvent e = (OrderConfirmedEvent) event;
            return "Order confirmed, total: $" + e.getTotal();
        } else if (event instanceof OrderShippedEvent) {
            OrderShippedEvent e = (OrderShippedEvent) event;
            return "Order shipped, tracking: " + e.getTrackingNumber();
        }
        return event.getClass().getSimpleName();
    }
}`

---

# **8. Event Sourcing + CQRS**

Thường dùng cùng nhau!

Text

`┌──────────────────────────────────────────────────────┐
│           Command (Write)                            │
└───────────┬──────────────────────────────────────────┘
            ↓
┌──────────────────────────────────────────────────────┐
│  Command Handler                                     │
│  - Rebuild from Event Store                          │
│  - Apply business logic                              │
│  - Generate new events                               │
└───────────┬──────────────────────────────────────────┘
            ↓
┌──────────────────────────────────────────────────────┐
│  Event Store (Source of Truth)                       │
│  events table (PostgreSQL, MongoDB, ...)             │
└───────────┬──────────────────────────────────────────┘
            ↓
┌──────────────────────────────────────────────────────┐
│  Event Publisher (Kafka, RabbitMQ, ...)              │
└───────────┬──────────────────────────────────────────┘
            ↓
┌──────────────────────────────────────────────────────┐
│  Event Handler (Async)                               │
│  - Update read model                                 │
│  - Cache                                             │
│  - Send notification                                 │
└───────────┬──────────────────────────────────────────┘
            ↓
┌──────────────────────────────────────────────────────┐
│  Read Model (MongoDB, Elasticsearch, Cache)          │
│  orders_read table (denormalized)                    │
└───────────┬──────────────────────────────────────────┘
            ↓
┌──────────────────────────────────────────────────────┐
│           Query (Read)                               │
└──────────────────────────────────────────────────────┘`

---

# **9. Ưu & Nhược điểm Event Sourcing**

## **Ưu điểm ✅**

| **Ưu điểm** | **Chi tiết** |
| --- | --- |
| **Complete audit trail** | Tất cả thay đổi được ghi lại |
| **Time travel** | Có thể rebuild state tại bất kỳ thời điểm |
| **Debugging** | Dễ debug: xem lại sequence of events |
| **Event-driven** | Natural fit với event-driven architecture |
| **No data loss** | Không mất bất kỳ info nào |
| **Concurrent writes** | Không race condition (append-only) |

## **Nhược điểm ❌**

| **Nhược điểm** | **Chi tiết** |
| --- | --- |
| **Complexity** | Phức tạp hơn CRUD, khó học |
| **Event versioning** | Thay đổi event schema khó |
| **Eventual consistency** | Read model lag behind write |
| **Storage** | Lưu tất cả events → lớn hơn |
| **Query difficulty** | Phải rebuild state để query |
| **Debugging hard** | Trace flow phức tạp: events → handlers → updates |

---

# **10. Khi nào dùng Event Sourcing?**

## **✅ Dùng khi**

- **Need complete audit trail**: Banking, legal compliance.
- **Complex business logic**: Track state transitions.
- **Event-driven system**: Fit naturally.
- **Temporal queries**: "State at 2025-01-15?"
- **Microservices**: Each service has own event stream.

**Ví dụ:**

Code

`Banking:
- Cần lịch sử tất cả transaction
- Audit trail bắt buộc
→ Event Sourcing tốt

E-Commerce:
- Cần biết order trải qua trạng thái nào
- Cần refund/return logic
→ Event Sourcing tốt

Blog:
- Simple CRUD
- Không cần lịch sử chi tiết
→ Không cần Event Sourcing`

## **❌ Không dùng khi**

- **Simple CRUD**: No complex state machine.
- **Real-time consistency critical**: Rebuild lag không chấp nhận được.
- **High-frequency updates**: Append-only bottleneck.
- **Team không familiar**: Learning curve cao.

---

# **11. Complete Example**

Java

`// ============================================
// FULL EVENT SOURCING FLOW
// ============================================

// Step 1: Command
@PostMapping("/orders")
public void createOrder(@RequestBody CreateOrderRequest req) {
    CreateOrderCommand command = new CreateOrderCommand(
        req.getOrderId(),
        req.getItems()
    );
    createOrderCommandHandler.handle(command);
}

// Step 2: Command Handler
@Service
public class CreateOrderCommandHandler {
    public void handle(CreateOrderCommand command) {
        // Generate event
        OrderCreatedEvent event = new OrderCreatedEvent(
            command.getOrderId(),
            command.getItems()
        );
        
        // Save to event store
        eventStore.append(event);
        
        // Publish
        eventPublisher.publish(event);
    }
}

// Step 3: Event stored in database
events table:
├─ id: 1
├─ aggregateId: ORD1
├─ eventType: OrderCreatedEvent
├─ data: {"orderId": "ORD1", "items": [...]}
├─ timestamp: 2025-01-15 10:00:00
└─ version: 1

// Step 4: Event Handler (async)
@KafkaListener(topics = "OrderCreatedEvent")
public void handle(OrderCreatedEvent event) {
    // Update read model
    OrderReadModel readModel = new OrderReadModel(
        event.getAggregateId(),
        ...
    );
    readRepository.save(readModel);
}

// Step 5: Query
@GetMapping("/orders/{id}")
public OrderResponse getOrder(@PathVariable String id) {
    GetOrderQuery query = new GetOrderQuery(id);
    
    // Fetch events
    List<DomainEvent> events = eventStore.getEvents(id);
    
    // Rebuild from events
    Order order = Order.fromEvents(events);
    
    // Return current state
    return new OrderResponse(...);
}`

---

# **12. Checklist: Event Sourcing**

- [ ]  Event: thay đổi được ghi lại, không state.
- [ ]  Event Store: lưu tất cả events.
- [ ]  Replay: rebuild state từ events.
- [ ]  Snapshot: optimize rebuild (optional).
- [ ]  Event Handler: update read model từ events.
- [ ]  Audit trail: có đầy đủ lịch sử.
- [ ]  Time travel: có thể rebuild state lúc nào.
- [ ]  Eventual consistency: read model có thể lag.
- [ ]  Schema evolution: phải handle khi event structure thay.
- [ ]  Khi nào dùng: complex business, audit trail, event-driven.