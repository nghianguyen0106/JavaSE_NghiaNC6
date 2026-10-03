# **5. CQRS — Command Query Responsibility Segregation**
## **Cần học**

Mentee cần tìm hiểu:

- Command là gì.
- Query là gì.
- Sự khác nhau giữa model ghi và model đọc.
- Khi nào CQRS hữu ích.
- Khi nào CQRS tạo thêm complexity không cần thiết.

## **Cần hiểu**

- Command dùng để thay đổi state.
- Query dùng để đọc dữ liệu.
- CQRS không bắt buộc phải dùng microservices.
- CQRS không bắt buộc phải dùng Kafka.
- Không phải hệ thống nào cũng cần CQRS.

---

# **CQRS (Command Query Responsibility Segregation)**

---

# **0. Intro: CQRS là gì?**

**CQRS** là một pattern tách biệt **Command (Write)** và **Query (Read)** thành hai model riêng biệt.

Ghi nhớ 1 ý chính:

> **Mô hình ghi data và mô hình đọc data không nhất thiết phải giống nhau.**
> 

---

# **1. Vấn đề CQRS giải quyết**

## **Vấn đề 1: Một model cho cả read và write khó scale**

### **Trước khi dùng CQRS**

Java

`// Một model cho tất cả: Order
@Entity
public class Order {
    @Id
    private String id;
    @Column
    private double total;
    @Column
    private String status;
    @OneToMany
    private List<OrderItem> items;
}

// Controller
@RestController
public class OrderController {
    
    // Command (Write)
    @PostMapping
    public void createOrder(OrderRequest req) {
        Order order = new Order();
        order.setId(req.getId());
        order.setTotal(req.getTotal());
        orderRepository.save(order);
    }
    
    // Query (Read)
    @GetMapping
    public OrderResponse getOrder(String id) {
        Order order = orderRepository.findById(id);
        return new OrderResponse(order.getId(), order.getTotal());
    }
}`

### **Vấn đề**

**Scenario:** E-commerce site

- 100,000 users **đọc** order (GET).
- 10,000 users **ghi** order (POST).

Tỷ lệ read:write = 10:1 (phổ biến).

Code

`Nếu dùng 1 model cho cả hai:
- Database phải optimize cho CẢ read AND write
- Nếu optimize cho write → read chậm (phải JOIN nhiều bảng)
- Nếu optimize cho read → write chậm (phải update nhiều table)
- Không có giải pháp perfect!`

**Read optimization khó:**

Java

`// User muốn xem:
// "Top 100 orders by total, với customer name, email, number of items"

// Với 1 model: phải JOIN Order + Customer + OrderItem + ... 
// Query phức tạp, chậm!`

## **Vấn đề 2: Write model phức tạp, Read model đơn giản**

Code

`Write side (complex):
- Validate data
- Business rule
- Persist
- Publish event
- Update cache

Read side (simple):
- Fetch data
- Format output`

Nếu dùng 1 model → Write model bị contaminate bởi read concern.

---

# **2. CQRS là gì?**

## **Khái niệm**

**Tách model ghi (Write Model) và model đọc (Read Model).**

Text

`┌──────────────────────────────────────────────────────────┐
│                     External World                       │
│                   (HTTP, Message, ...)                   │
└──────────────────────────────────────────────────────────┘
                    ↓                    ↓
         Command (Write)          Query (Read)
                    ↓                    ↓
    ┌─────────────────────────┐  ┌─────────────────────────┐
    │   Write Model           │  │   Read Model            │
    ├─────────────────────────┤  ├─────────────────────────┤
    │ - Validate              │  │ - Optimized for read    │
    │ - Business rule         │  │ - Denormalized          │
    │ - Persist (DB)          │  │ - Maybe cached          │
    │ - Publish event         │  │ - Maybe separate DB     │
    └─────────────────────────┘  └─────────────────────────┘
                ↓                         ↑
    ┌─────────────────────────┐
    │    Source of Truth      │
    │    (Write Database)     │
    │    (Event Store)        │
    └─────────────────────────┘
                ↓
    ┌─────────────────────────┐
    │   Update Read Model     │
    │   (Async, event-driven) │
    └─────────────────────────┘`

## **Đơn giản hóa**

Text

`Write:
  Command → Validate → Business Logic → Persist → Event
  
       ↓
     Publish Event
       ↓
Read:
  Event → Update Read Model → Query`

---

# **3. Từng bước CQRS**

## **Step 1: Write Model (Command Side)**

Java

`// ============================================
// COMMAND: Định nghĩa input
// ============================================

public class CreateOrderCommand {
    private final String orderId;
    private final List<OrderItemCommand> items;
    
    public CreateOrderCommand(String orderId, List<OrderItemCommand> items) {
        this.orderId = orderId;
        this.items = items;
    }
    
    // Getters
}

public class OrderItemCommand {
    private final String productId;
    private final int quantity;
    private final double price;
    
    // Constructor, getters
}

// ============================================
// COMMAND HANDLER: Xử lý command
// ============================================

@Service
public class CreateOrderCommandHandler {
    private final OrderRepository orderRepository;      // Write to DB
    private final EventPublisher eventPublisher;        // Publish event
    private final OrderValidator validator;
    
    public void handle(CreateOrderCommand command) {
        // 1. Validate
        validator.validate(command);
        
        // 2. Create domain object
        Order order = new Order(
            new OrderId(command.getOrderId()),
            command.getItems().stream()
                .map(item -> new OrderItem(
                    item.getProductId(),
                    item.getQuantity(),
                    new Money(item.getPrice())
                ))
                .collect(Collectors.toList())
        );
        
        // 3. Business logic
        order.confirm();
        
        // 4. Persist (Write to database)
        orderRepository.save(order);
        
        // 5. Publish event (async)
        eventPublisher.publish(new OrderCreatedEvent(order.getId()));
    }
}

// ============================================
// WRITE REPOSITORY: Persist write model
// ============================================

public interface OrderRepository {
    void save(Order order);
}

@Repository
public class JpaOrderRepository implements OrderRepository {
    private final OrderJpaRepository jpaRepository;
    
    @Override
    public void save(Order order) {
        OrderEntity entity = new OrderEntity(
            order.getId().getValue(),
            order.calculateTotal().getAmount(),
            order.getStatus().toString()
        );
        jpaRepository.save(entity);
    }
}

// ============================================
// WRITE DATABASE: Source of truth
// ============================================

@Entity
@Table(name = "orders")
public class OrderEntity {
    @Id
    private String id;
    @Column
    private double total;
    @Column
    private String status;
    // ... items
}`

## **Step 2: Event Publishing**

Java

`// ============================================
// DOMAIN EVENT
// ============================================

public class OrderCreatedEvent {
    private final String orderId;
    private final Instant timestamp;
    
    public OrderCreatedEvent(OrderId orderId) {
        this.orderId = orderId.getValue();
        this.timestamp = Instant.now();
    }
    
    public String getOrderId() { return orderId; }
    public Instant getTimestamp() { return timestamp; }
}

// ============================================
// EVENT PUBLISHER
// ============================================

public interface EventPublisher {
    void publish(DomainEvent event);
}

@Component
public class KafkaEventPublisher implements EventPublisher {
    private final KafkaTemplate<String, String> kafkaTemplate;
    
    @Override
    public void publish(DomainEvent event) {
        String topic = event.getClass().getSimpleName();
        String message = new ObjectMapper().writeValueAsString(event);
        
        kafkaTemplate.send(topic, message);
        System.out.println("Event published: " + topic);
    }
}`

## **Step 3: Read Model (Query Side)**

Java

`// ============================================
// QUERY: Định nghĩa input
// ============================================

public class GetOrderQuery {
    private final String orderId;
    
    public GetOrderQuery(String orderId) {
        this.orderId = orderId;
    }
    
    public String getOrderId() { return orderId; }
}

// ============================================
// QUERY HANDLER: Xử lý query
// ============================================

@Service
public class GetOrderQueryHandler {
    private final OrderReadRepository readRepository;  // Read from read DB
    
    public OrderReadResponse handle(GetOrderQuery query) {
        // Đọc từ read model (optimized, maybe cached)
        return readRepository.findById(query.getOrderId());
    }
}

// ============================================
// READ MODEL: Denormalized data
// ============================================

public class OrderReadModel {
    private final String id;
    private final String customerId;
    private final String customerName;
    private final String customerEmail;
    private final double total;
    private final String status;
    private final int itemCount;
    private final List<OrderItemReadModel> items;
    
    // Constructor, getters
}

public class OrderItemReadModel {
    private final String productId;
    private final String productName;
    private final int quantity;
    private final double price;
    private final double subtotal;
    
    // Constructor, getters
}

// ============================================
// READ REPOSITORY: Query read model
// ============================================

public interface OrderReadRepository {
    OrderReadModel findById(String orderId);
    List<OrderReadModel> findByCustomer(String customerId);
    List<OrderReadModel> findByStatus(String status);
    List<OrderReadModel> findTopByTotal(int limit);
}

@Repository
public class OrderReadRepositoryImpl implements OrderReadRepository {
    private final OrderReadModelJpaRepository jpaRepository;
    
    @Override
    public OrderReadModel findById(String orderId) {
        return jpaRepository.findById(orderId)
            .map(entity -> new OrderReadModel(
                entity.getId(),
                entity.getCustomerId(),
                entity.getCustomerName(),
                entity.getCustomerEmail(),
                entity.getTotal(),
                entity.getStatus(),
                entity.getItemCount(),
                entity.getItems().stream()
                    .map(item -> new OrderItemReadModel(...))
                    .collect(Collectors.toList())
            ))
            .orElse(null);
    }
    
    @Override
    public List<OrderReadModel> findByCustomer(String customerId) {
        return jpaRepository.findByCustomerId(customerId)
            .stream()
            .map(this::toReadModel)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<OrderReadModel> findTopByTotal(int limit) {
        // Optimized query: ORDER BY total DESC LIMIT
        return jpaRepository.findTopByOrderByTotalDesc(limit)
            .stream()
            .map(this::toReadModel)
            .collect(Collectors.toList());
    }
}

@Repository
public interface OrderReadModelJpaRepository extends JpaRepository<OrderReadEntity, String> {
    List<OrderReadEntity> findByCustomerId(String customerId);
    List<OrderReadEntity> findTopByOrderByTotalDesc(int limit);
}

// ============================================
// READ DATABASE: Denormalized
// ============================================

@Entity
@Table(name = "orders_read")
public class OrderReadEntity {
    @Id
    private String id;
    
    @Column
    private String customerId;
    
    @Column
    private String customerName;  // ← Denormalized: copied from customer table
    
    @Column
    private String customerEmail; // ← Denormalized
    
    @Column
    private double total;
    
    @Column
    private String status;
    
    @Column
    private int itemCount;  // ← Denormalized: count items
    
    @OneToMany
    @JoinColumn(name = "order_id")
    private List<OrderItemReadEntity> items;
    
    // Getters, setters
}`

## **Step 4: Event Handler (Update Read Model)**

Java

`// ============================================
// EVENT LISTENER: Lắng nghe event
// ============================================

@Component
public class OrderCreatedEventHandler {
    private final OrderReadRepository readRepository;
    private final CustomerService customerService;  // Get customer info
    
    @KafkaListener(topics = "OrderCreatedEvent")
    public void handle(String message) throws JsonProcessingException {
        OrderCreatedEvent event = new ObjectMapper()
            .readValue(message, OrderCreatedEvent.class);
        
        // 1. Fetch write model
        Order order = orderRepository.findById(new OrderId(event.getOrderId()));
        
        // 2. Fetch additional data (customer info, ...)
        Customer customer = customerService.getCustomer(order.getCustomerId());
        
        // 3. Build read model (denormalized)
        OrderReadModel readModel = new OrderReadModel(
            order.getId().getValue(),
            customer.getId(),
            customer.getName(),
            customer.getEmail(),
            order.calculateTotal().getAmount(),
            order.getStatus().toString(),
            order.getItems().size(),
            order.getItems().stream()
                .map(item -> new OrderItemReadModel(...))
                .collect(Collectors.toList())
        );
        
        // 4. Save to read database
        readRepository.save(readModel);
        
        System.out.println("Read model updated for order: " + event.getOrderId());
    }
}`

---

# **4. CQRS vs Không CQRS**

## **Không dùng CQRS**

Java

`// Một model cho tất cả
@Entity
public class Order {
    @Id
    private String id;
    @Column
    private double total;
    @Column
    private String status;
    @OneToMany
    private List<OrderItem> items;
    @ManyToOne
    private Customer customer;  // Foreign key
}

// Repository
public interface OrderRepository extends JpaRepository<Order, String> {
    List<Order> findByCustomer(Customer customer);
    List<Order> findByStatus(String status);
    List<Order> findByTotalGreaterThan(double total);
}

// Service
@Service
public class OrderService {
    public void createOrder(Order order) {
        orderRepository.save(order);
    }
    
    public Order getOrder(String id) {
        return orderRepository.findById(id).orElse(null);
    }
    
    public List<Order> getOrdersByCustomer(String customerId) {
        return orderRepository.findByCustomer(customerId);  // JOIN query
    }
}

// Controller
@RestController
public class OrderController {
    @PostMapping
    public void createOrder(OrderRequest req) { ... }
    
    @GetMapping("/{id}")
    public Order getOrder(@PathVariable String id) { ... }
    
    @GetMapping("/customer/{customerId}")
    public List<Order> getOrdersByCustomer(@PathVariable String customerId) { ... }
}`

**Vấn đề:**

- ❌ Query `findByCustomer` phải JOIN Customer table → chậm.
- ❌ Write model có foreign key → update khó.
- ❌ Một bảng Order cho cả read và write.
- ❌ Scale khó.

## **Dùng CQRS**

Java

`// WRITE SIDE
@Service
public class CreateOrderCommandHandler {
    private final OrderRepository writeRepository;
    private final EventPublisher eventPublisher;
    
    public void handle(CreateOrderCommand command) {
        Order order = new Order(...);
        order.confirm();
        writeRepository.save(order);
        eventPublisher.publish(new OrderCreatedEvent(...));
    }
}

// READ SIDE
@Service
public class GetOrderQueryHandler {
    private final OrderReadRepository readRepository;
    
    public OrderReadModel handle(GetOrderQuery query) {
        return readRepository.findById(query.getOrderId());
    }
}

// SEPARATE DATABASES
orders (write)        orders_read (read, denormalized)
├─ id                 ├─ id
├─ total              ├─ customerId
├─ status             ├─ customerName ← denormalized
└─ items              ├─ customerEmail ← denormalized
                      ├─ total
                      ├─ status
                      ├─ itemCount ← denormalized
                      └─ items`

**Lợi ích:**

- ✅ Write model: pure (chỉ business logic).
- ✅ Read model: denormalized (tối ưu query).
- ✅ Separate database: scale riêng.
- ✅ Query `findByCustomer` chỉ scan orders_read table → nhanh.

---

# **5. Ưu & Nhược điểm CQRS**

## **Ưu điểm ✅**

| **Ưu điểm** | **Chi tiết** |
| --- | --- |
| **Read/Write scale riêng** | Có thể optimize read và write khác nhau |
| **Read optimize** | Denormalized, cached, aggregate data |
| **Write simple** | Chỉ focus business logic, không care read |
| **Event-driven** | Có event log, audit trail |
| **Async update** | Read model update async, không block write |

## **Nhược điểm ❌**

| **Nhược điểm** | **Chi tiết** |
| --- | --- |
| **Complexity cao** | 2 model, 2 database, event sync |
| **Eventual consistency** | Read model có thể không real-time consistent với write |
| **Data duplication** | Read model copy data từ write model |
| **Sync logic phức tạp** | Phải handle event → update read model |
| **Khó debug** | Flow: command → event → async update |

---

# **6. Khi nào nên dùng CQRS?**

## **✅ Dùng CQRS khi**

- **Read nhiều hơn write** (tỷ lệ 10:1 trở lên).
- **Query pattern phức tạp** (GROUP BY, AGGREGATE, JOIN nhiều table).
- **Need audit trail** (lịch sử mọi thay đổi).
- **Scale independently** (read và write load khác nhau).
- **Microservices** (mỗi service có read/write model riêng).

**Ví dụ:**

Code

`E-Commerce:
- Read: 100,000 users xem order/product
- Write: 1,000 users tạo order
→ Nên CQRS

Blog:
- Read: 10,000 users đọc bài
- Write: 100 users viết bài
→ Nên CQRS

Admin Dashboard:
- Read/Write cân bằng
→ Không cần CQRS`

## **❌ Không nên dùng CQRS khi**

- **Read/Write cân bằng** (tỷ lệ 1:1 hoặc gần bằng).
- **Data consistency critical** (cần real-time consistency).
- **Team nhỏ** (CQRS phức tạp, team phải giỏi).
- **MVP** (nên làm simple trước, optimize sau).
- **Query simple** (chỉ CRUD basic).

---

# **7. Ví dụ thực tế**

## **Order Service với CQRS**

Java

`// ============================================
// WRITE SIDE (Command)
// ============================================

// Controller nhận command
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final CreateOrderCommandHandler commandHandler;
    
    @PostMapping
    public ResponseEntity<Void> createOrder(@RequestBody CreateOrderRequest req) {
        CreateOrderCommand command = new CreateOrderCommand(
            req.getOrderId(),
            req.getItems()
        );
        
        commandHandler.handle(command);
        return ResponseEntity.ok().build();
    }
}

// ============================================
// READ SIDE (Query)
// ============================================

// Controller nhận query
@RestController
@RequestMapping("/api/orders")
public class OrderQueryController {
    private final GetOrderQueryHandler queryHandler;
    
    @GetMapping("/{id}")
    public ResponseEntity<OrderReadModel> getOrder(@PathVariable String id) {
        GetOrderQuery query = new GetOrderQuery(id);
        OrderReadModel order = queryHandler.handle(query);
        return ResponseEntity.ok(order);
    }
    
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<OrderReadModel>> getCustomerOrders(
            @PathVariable String customerId) {
        GetCustomerOrdersQuery query = new GetCustomerOrdersQuery(customerId);
        List<OrderReadModel> orders = queryHandler.handle(query);
        return ResponseEntity.ok(orders);
    }
}

// ============================================
// Architecture
// ============================================

POST /api/orders
    ↓
CreateOrderCommandHandler
    ↓
Order (domain model)
    ↓
OrderRepository.save() → PostgreSQL (write DB)
    ↓
EventPublisher.publish(OrderCreatedEvent)
    ↓
Kafka (event stream)
    ↓
OrderCreatedEventHandler (async)
    ↓
OrderReadRepository.save() → MongoDB (read DB, denormalized)
    ↓
GET /api/orders/{id}
    ↓
OrderQueryController
    ↓
OrderReadRepository.findById() → MongoDB
    ↓
Return OrderReadModel (fast!)`

---

# **8. Chi tiết: Write Model vs Read Model**

## **Write Model (Command Side)**

Java

`// Normalized, business-focused
public class Order {
    private final OrderId id;
    private final List<OrderItem> items;  // Child objects
    private OrderStatus status;
    
    // Business logic
    public void confirm() { ... }
    public void cancel() { ... }
    public Money calculateTotal() { ... }
}

// Persist to PostgreSQL`

## **Read Model (Query Side)**

Java

`// Denormalized, query-optimized
public class OrderReadModel {
    private String id;
    private String customerId;
    private String customerName;        // ← Denormalized (from Customer)
    private String customerEmail;       // ← Denormalized
    private double total;
    private String status;
    private int itemCount;              // ← Denormalized (count)
    private List<OrderItemReadModel> items;
    
    // Just data, no business logic
}

// Persist to MongoDB (or Elasticsearch, or Cache, ...)`

---

# **9. Complete CQRS Example**

Java

`// ============================================
// COMMANDS
// ============================================

public class CreateOrderCommand {
    private final String orderId;
    private final List<OrderItemCommand> items;
    
    // Constructor, getters
}

public class CancelOrderCommand {
    private final String orderId;
    
    // Constructor, getter
}

// ============================================
// QUERIES
// ============================================

public class GetOrderQuery {
    private final String orderId;
    
    // Constructor, getter
}

public class GetCustomerOrdersQuery {
    private final String customerId;
    
    // Constructor, getter
}

public class SearchOrdersQuery {
    private final OrderStatus status;
    private final double minTotal;
    private final double maxTotal;
    
    // Constructor, getters
}

// ============================================
// COMMAND HANDLERS
// ============================================

@Service
public class CreateOrderCommandHandler {
    private final OrderRepository writeRepository;
    private final EventPublisher eventPublisher;
    private final OrderValidator validator;
    
    public void handle(CreateOrderCommand command) {
        validator.validate(command);
        
        Order order = new Order(
            new OrderId(command.getOrderId()),
            command.getItems().stream()
                .map(item -> new OrderItem(...))
                .collect(Collectors.toList())
        );
        
        order.confirm();
        writeRepository.save(order);
        eventPublisher.publish(new OrderCreatedEvent(order.getId()));
    }
}

@Service
public class CancelOrderCommandHandler {
    private final OrderRepository writeRepository;
    private final EventPublisher eventPublisher;
    
    public void handle(CancelOrderCommand command) {
        Order order = writeRepository.findById(new OrderId(command.getOrderId()))
            .orElseThrow(() -> new NotFoundException("Order not found"));
        
        order.cancel();
        writeRepository.save(order);
        eventPublisher.publish(new OrderCancelledEvent(order.getId()));
    }
}

// ============================================
// QUERY HANDLERS
// ============================================

@Service
public class GetOrderQueryHandler {
    private final OrderReadRepository readRepository;
    
    public OrderReadModel handle(GetOrderQuery query) {
        return readRepository.findById(query.getOrderId());
    }
}

@Service
public class GetCustomerOrdersQueryHandler {
    private final OrderReadRepository readRepository;
    
    public List<OrderReadModel> handle(GetCustomerOrdersQuery query) {
        return readRepository.findByCustomerId(query.getCustomerId());
    }
}

@Service
public class SearchOrdersQueryHandler {
    private final OrderReadRepository readRepository;
    
    public List<OrderReadModel> handle(SearchOrdersQuery query) {
        return readRepository.search(
            query.getStatus(),
            query.getMinTotal(),
            query.getMaxTotal()
        );
    }
}

// ============================================
// EVENT HANDLERS (Update Read Model)
// ============================================

@Component
public class OrderCreatedEventHandler {
    private final OrderReadRepository readRepository;
    private final CustomerService customerService;
    
    @KafkaListener(topics = "OrderCreatedEvent")
    public void handle(String message) throws JsonProcessingException {
        OrderCreatedEvent event = objectMapper.readValue(message, OrderCreatedEvent.class);
        
        // Fetch write model
        Order order = orderRepository.findById(new OrderId(event.getOrderId()));
        Customer customer = customerService.getCustomer(order.getCustomerId());
        
        // Build read model
        OrderReadModel readModel = new OrderReadModel(
            order.getId().getValue(),
            customer.getId(),
            customer.getName(),
            customer.getEmail(),
            order.calculateTotal().getAmount(),
            order.getStatus().toString(),
            order.getItems().size(),
            order.getItems().stream()
                .map(item -> new OrderItemReadModel(...))
                .collect(Collectors.toList())
        );
        
        // Save to read DB
        readRepository.save(readModel);
    }
}

@Component
public class OrderCancelledEventHandler {
    private final OrderReadRepository readRepository;
    
    @KafkaListener(topics = "OrderCancelledEvent")
    public void handle(String message) throws JsonProcessingException {
        OrderCancelledEvent event = objectMapper.readValue(message, OrderCancelledEvent.class);
        
        // Update read model
        OrderReadModel readModel = readRepository.findById(event.getOrderId());
        readModel.setStatus("CANCELLED");
        readRepository.save(readModel);
    }
}

// ============================================
// CONTROLLERS
// ============================================

@RestController
@RequestMapping("/api/orders/commands")
public class OrderCommandController {
    private final CreateOrderCommandHandler createHandler;
    private final CancelOrderCommandHandler cancelHandler;
    
    @PostMapping("/create")
    public ResponseEntity<Void> createOrder(@RequestBody CreateOrderRequest req) {
        CreateOrderCommand command = new CreateOrderCommand(...);
        createHandler.handle(command);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/cancel/{orderId}")
    public ResponseEntity<Void> cancelOrder(@PathVariable String orderId) {
        CancelOrderCommand command = new CancelOrderCommand(orderId);
        cancelHandler.handle(command);
        return ResponseEntity.ok().build();
    }
}

@RestController
@RequestMapping("/api/orders/queries")
public class OrderQueryController {
    private final GetOrderQueryHandler getHandler;
    private final GetCustomerOrdersQueryHandler getCustomerHandler;
    private final SearchOrdersQueryHandler searchHandler;
    
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderReadModel> getOrder(@PathVariable String orderId) {
        GetOrderQuery query = new GetOrderQuery(orderId);
        OrderReadModel order = getHandler.handle(query);
        return ResponseEntity.ok(order);
    }
    
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<OrderReadModel>> getCustomerOrders(
            @PathVariable String customerId) {
        GetCustomerOrdersQuery query = new GetCustomerOrdersQuery(customerId);
        List<OrderReadModel> orders = getCustomerHandler.handle(query);
        return ResponseEntity.ok(orders);
    }
    
    @GetMapping("/search")
    public ResponseEntity<List<OrderReadModel>> search(
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "0") double minTotal,
            @RequestParam(required = false, defaultValue = "999999") double maxTotal) {
        SearchOrdersQuery query = new SearchOrdersQuery(
            OrderStatus.valueOf(status),
            minTotal,
            maxTotal
        );
        List<OrderReadModel> results = searchHandler.handle(query);
        return ResponseEntity.ok(results);
    }
}`

---

# **10. CQRS Flow Diagram**

Text

`┌─────────────────────────────────────────────────────────────┐
│                    CLIENT / HTTP                            │
└─────────────────────────────────────────────────────────────┘
                ↓                          ↓
        POST /orders/create         GET /orders/123
        (Command)                   (Query)
                ↓                          ↓
    ┌─────────────────────────┐  ┌─────────────────────────┐
    │ OrderCommandController  │  │ OrderQueryController    │
    └───────────┬─────────────┘  └────────────┬────────────┘
                ↓                             ↓
    ┌─────────────────────────┐  ┌─────────────────────────┐
    │ CreateOrderCommand      │  │ GetOrderQuery           │
    │ Handler                 │  │ Handler                 │
    └───────────┬─────────────┘  └────────────┬────────────┘
                ↓                             ↓
    ┌─────────────────────────┐  ┌─────────────────────────┐
    │ Order (Domain Model)    │  │ OrderReadRepository     │
    └───────────┬─────────────┘  └────────────┬────────────┘
                ↓                             ↓
    ┌─────────────────────────┐  ┌─────────────────────────┐
    │ OrderRepository.save()  │  │ MongoDB (Read DB)       │
    │ PostgreSQL (Write DB)   │  │ Denormalized            │
    └───────────┬─────────────┘  └────────────┬────────────┘
                ↓                             ↑
    ┌─────────────────────────┐              │
    │ EventPublisher          │              │
    │ Kafka (Event Stream)    │              │
    └───────────┬─────────────┘              │
                ↓                            │
    ┌─────────────────────────┐              │
    │ OrderCreatedEventHandler│──────────────┘
    │ (Async, Update Read DB) │
    └─────────────────────────┘`

---

# **11. Checklist: CQRS**

- [ ]  Hiểu vấn đề: Read/Write có pattern khác nhau.
- [ ]  Command: viết data, có business logic.
- [ ]  Query: đọc data, no logic (chỉ fetch).
- [ ]  Write Model: normalized, business-focused.
- [ ]  Read Model: denormalized, query-optimized.
- [ ]  Event: publish sau khi write thành công.
- [ ]  Event Handler: update read model async.
- [ ]  Eventual Consistency: read model có thể delay.
- [ ]  Khi nào dùng CQRS: read >> write, complex query.
- [ ]  Khi nào không dùng: CRUD simple, real-time consistency.