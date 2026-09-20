# 2 Clean Architecture
## **Cần học**

Mentee cần tìm hiểu:

- Clean Architecture là gì.
- Dependency Rule.
- Domain layer.
- Application/use case layer.
- Interface adapter layer.
- Infrastructure/framework layer.
- Vì sao business logic nên độc lập với framework.

## **Cần hiểu**

Mentee cần trả lời được:

- Dependency Rule là gì?
- Vì sao dependency chỉ nên hướng vào bên trong?
- Domain có được import Spring hoặc JPA không?
- Controller có nên chứa business rule không?
- Use case khác gì với controller?
- Nếu đổi database thì layer nào bị ảnh hưởng?

## **Sơ đồ cần nắm**

Text

`Frameworks & Drivers
    REST Controller
    Database
    Message Broker
            ↓
Interface Adapters
    Controller Adapter
    Persistence Adapter
            ↓
Application
    Use Cases
            ↓
Domain
    Entities
    Value Objects
    Business Rules`

## **Yêu cầu thực hành**

Tạo project theo cấu trúc:

Text

`domain
application
adapter
infrastructure`

Trong đó:

- `domain`: entity, value object, business rule.
- `application`: use case và application service.
- `adapter.in`: REST controller hoặc CLI.
- `adapter.out`: repository implementation.
- `infrastructure`: cấu hình framework và database.

## **Sản phẩm cần nộp**

- Project có cấu trúc layer rõ ràng.
- Sơ đồ dependency.
- Một use case hoàn chỉnh: `CreateOrderUseCase`.
- Unit test cho use case.
- File giải thích vai trò của từng layer.

## **Tiêu chí hoàn thành**

- Domain không phụ thuộc Spring/JPA.
- Controller không chứa business logic chính.
- Use case không biết chi tiết database.
- Có thể thay `InMemoryRepository` bằng database adapter khác.
- Các layer có trách nhiệm rõ ràng.

---

# **Clean Architecture — Học chi tiết**

## **0. Intro: Clean Architecture là gì?**

**Clean Architecture** là một cách tổ chức code thành các layer, sao cho:

- Business logic độc lập với framework (Spring, JPA, ...).
- Business logic độc lập với database.
- Dễ test.
- Dễ bảo trì và mở rộng.

Ghi nhớ 1 ý chính:

> **Dependency chỉ được hướng vào bên trong (Dependency Rule).**
> 

---

# **1. Dependency Rule**

## **Khái niệm**

**Inner layer không biết về outer layer. Outer layer có thể biết về inner layer.**

**Flow:**
`┌────────────────────────────────────┐
│     Frameworks & Drivers            │ ← Outer (Framework, DB, UI)
│  (Spring, JPA, REST, Message)       │
└─────────────────────────────────────┘
              ↓
┌─────────────────────────────────────┐
│    Interface Adapters               │ ← Controller, Repository Adapter
│  (Controllers, Presenters)          │
└─────────────────────────────────────┘
              ↓
┌─────────────────────────────────────┐
│     Application Business Rules      │ ← Use Cases, Application Services
│  (Use Cases, Interactors)           │
└─────────────────────────────────────┘
              ↓
┌─────────────────────────────────────┐
│      Enterprise Business Rules      │ ← Domain Entities, Value Objects
│     (Entities, Value Objects)       │
└─────────────────────────────────────┘`

## **Dependency Flow**

**Flow:**

`Controller  →  Use Case  →  Domain Entity  ← Repository Interface
    ↓            ↓             ↑                 (Inner)
  Spring        Business      Business
  (Outer)       Logic          Rules`

**Quy tắc:**

1. **Domain** không import Spring, JPA, hoặc thứ gì từ framework.
2. **Application** không phụ thuộc trực tiếp vào database.
3. **Controllers** gọi vào **Use Cases**, không gọi thẳng database.
4. **Database Adapter** implement **Repository Interface** (được định nghĩa ở application layer).

## **Ví dụ sai (Vi phạm Dependency Rule)**

Java

`// ❌ Domain import JPA (Outer layer)
@Entity
@Table(name = "orders")
public class Order {
    @Id
    private String id;
    
    @Column
    private double total;
}

// ❌ Use Case import Spring
@Service
public class CreateOrderUseCase {
    @Autowired
    private OrderRepository repository;
    
    public void create(Order order) {
        repository.save(order);
    }
}

// ❌ Controller chứa business logic
@RestController
public class OrderController {
    @PostMapping
    public void create(@RequestBody OrderRequest request) {
        // Validate
        if (request.getTotal() < 0) throw new Exception("Invalid");
        
        // Calculate
        double total = request.getItems().stream()
            .mapToDouble(i -> i.getPrice() * i.getQuantity())
            .sum();
        
        // Save
        Order order = new Order(request.getId(), total);
        // database.save(order);
    }
}`

## **Ví dụ đúng (Tuân thủ Dependency Rule)**

Java

`// ✅ Domain không import framework
public class Order {
    private final OrderId id;
    private final List<OrderItem> items;
    private final OrderStatus status;
    
    public Order(OrderId id, List<OrderItem> items) {
        if (items.isEmpty()) {
            throw new IllegalArgumentException("Order must have at least one item");
        }
        this.id = id;
        this.items = items;
        this.status = OrderStatus.PENDING;
    }
    
    public double calculateTotal() {
        return items.stream()
            .mapToDouble(item -> item.getPrice() * item.getQuantity())
            .sum();
    }
    
    public void confirm() {
        if (status != OrderStatus.PENDING) {
            throw new IllegalStateException("Cannot confirm non-pending order");
        }
        this.status = OrderStatus.CONFIRMED;
    }
}

// ✅ Repository Interface nằm ở Application layer
public interface OrderRepository {
    void save(Order order);
    Optional<Order> findById(OrderId id);
}

// ✅ Use Case không import Spring
public class CreateOrderUseCase {
    private final OrderRepository repository;
    private final OrderValidator validator;
    
    public CreateOrderUseCase(OrderRepository repository, OrderValidator validator) {
        this.repository = repository;
        this.validator = validator;
    }
    
    public OrderResponse create(CreateOrderCommand command) {
        // Validate
        validator.validate(command);
        
        // Create domain object
        Order order = new Order(
            new OrderId(command.getOrderId()),
            command.getItems().stream()
                .map(itemCmd -> new OrderItem(
                    new ProductId(itemCmd.getProductId()),
                    itemCmd.getQuantity(),
                    itemCmd.getPrice()
                ))
                .collect(Collectors.toList())
        );
        
        // Persist
        repository.save(order);
        
        return new OrderResponse(order.getId(), order.calculateTotal());
    }
}

// ✅ Controller chỉ điều phối, không chứa business logic
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final CreateOrderUseCase createOrderUseCase;
    
    public OrderController(CreateOrderUseCase createOrderUseCase) {
        this.createOrderUseCase = createOrderUseCase;
    }
    
    @PostMapping
    public ResponseEntity<OrderResponse> create(@RequestBody CreateOrderRequest request) {
        CreateOrderCommand command = new CreateOrderCommand(
            request.getOrderId(),
            request.getItems()
        );
        
        OrderResponse response = createOrderUseCase.create(command);
        return ResponseEntity.ok(response);
    }
}

// ✅ Repository Adapter implement interface, chứa JPA/Database code
@Repository
public class JpaOrderRepository implements OrderRepository {
    private final OrderJpaRepository jpaRepository;
    
    public JpaOrderRepository(OrderJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }
    
    @Override
    public void save(Order order) {
        // Convert domain Order to JPA Entity
        OrderJpaEntity entity = new OrderJpaEntity(
            order.getId().value(),
            order.calculateTotal()
        );
        jpaRepository.save(entity);
    }
    
    @Override
    public Optional<Order> findById(OrderId id) {
        return jpaRepository.findById(id.value())
            .map(entity -> new Order(
                new OrderId(entity.getId()),
                // ... reconstruct order ...
            ));
    }
}`

---

# **2. Các Layer trong Clean Architecture**

## **Layer 1: Domain (Entities & Business Rules)**

**Đây là tâm của ứng dụng.**

### **Nội dung**

- **Entity**: object có identity, dữ liệu thay đổi.
- **Value Object**: object không có identity riêng.
- **Domain Rules**: business logic.
- **Domain Exception**: exception cụ thể domain.

### **Đặc điểm**

- Không import bất kỳ framework nào.
- Không import database library.
- Không import Spring, Lombok, hoặc annotation framework.
- Pure Java.

### **Ví dụ**

Java

`// Domain Entity
public class Order {
    private final OrderId id;
    private final List<OrderItem> items;
    private OrderStatus status;
    
    public Order(OrderId id, List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            throw new DomainException("Order must have items");
        }
        this.id = id;
        this.items = new ArrayList<>(items);
        this.status = OrderStatus.PENDING;
    }
    
    public double getTotal() {
        return items.stream()
            .mapToDouble(OrderItem::getSubtotal)
            .sum();
    }
    
    public void confirm() {
        if (status != OrderStatus.PENDING) {
            throw new DomainException("Can only confirm PENDING orders");
        }
        this.status = OrderStatus.CONFIRMED;
    }
    
    public void cancel() {
        if (status == OrderStatus.SHIPPED) {
            throw new DomainException("Cannot cancel shipped order");
        }
        this.status = OrderStatus.CANCELLED;
    }
}

// Value Object
public class OrderId {
    private final String value;
    
    public OrderId(String value) {
        if (value == null || value.isEmpty()) {
            throw new DomainException("OrderId cannot be empty");
        }
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrderId)) return false;
        OrderId orderId = (OrderId) o;
        return Objects.equals(value, orderId.value);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}

// Domain Exception
public class DomainException extends RuntimeException {
    public DomainException(String message) {
        super(message);
    }
}`

## **Layer 2: Application (Use Cases & Interactors)**

**Điều phối domain để hoàn thành use case.**

### **Nội dung**

- **Use Case Interface**: định nghĩa input/output.
- **Application Service/Interactor**: implement use case.
- **DTO (Data Transfer Object)**: input/output.
- **Repository Interface**: định nghĩa port giao tiếp với database.

### **Đặc điểm**

- Không import Spring annotation vào core logic.
- Phụ thuộc vào abstraction (interface), không dependency cụ thể.
- Không biết database là gì.
- Không có HTTP/REST code ở đây.

### **Ví dụ**

Java

`// Use Case Input (Command)
public class CreateOrderCommand {
    private final String orderId;
    private final List<OrderItemCommand> items;
    
    public CreateOrderCommand(String orderId, List<OrderItemCommand> items) {
        this.orderId = orderId;
        this.items = items;
    }
    
    // getters
}

// Use Case Output (Response)
public class OrderResponse {
    private final String orderId;
    private final double total;
    private final String status;
    
    public OrderResponse(String orderId, double total, String status) {
        this.orderId = orderId;
        this.total = total;
        this.status = status;
    }
    
    // getters
}

// Repository Port (interface định nghĩa, không implement)
public interface OrderRepository {
    void save(Order order);
    Optional<Order> findById(OrderId orderId);
    boolean existsById(OrderId orderId);
}

// Use Case Implementation
public class CreateOrderUseCase {
    private final OrderRepository orderRepository;
    private final OrderValidator validator;
    
    // Constructor Injection
    public CreateOrderUseCase(OrderRepository orderRepository, OrderValidator validator) {
        this.orderRepository = orderRepository;
        this.validator = validator;
    }
    
    public OrderResponse execute(CreateOrderCommand command) {
        // 1. Validate
        validator.validate(command);
        
        // 2. Create domain object
        Order order = new Order(
            new OrderId(command.getOrderId()),
            command.getItems().stream()
                .map(this::toOrderItem)
                .collect(Collectors.toList())
        );
        
        // 3. Business operation
        order.confirm(); // gọi domain logic
        
        // 4. Persist
        orderRepository.save(order);
        
        // 5. Return response
        return new OrderResponse(
            order.getId().getValue(),
            order.getTotal(),
            order.getStatus().toString()
        );
    }
    
    private OrderItem toOrderItem(OrderItemCommand cmd) {
        return new OrderItem(
            new ProductId(cmd.getProductId()),
            cmd.getQuantity(),
            cmd.getPrice()
        );
    }
}`

## **Layer 3: Interface Adapters (Controllers, Repositories)**

**Chuyển đổi dữ liệu giữa external world (HTTP, Database) và application.**

### **Nội dung**

- **Controller**: HTTP endpoint.
- **Repository Adapter**: implement Repository interface.
- **Presenter**: format response (JSON, XML, ...).
- **Request/Response DTO**: định dạng gửi/nhận từ client.

### **Đặc điểm**

- Chứa Spring annotation (@RestController, @Service, ...).
- Chứa JPA annotation (@Repository, @Entity, ...).
- Không chứa business logic chính.
- Chuyển đổi giữa domain object và database/HTTP format.

### **Ví dụ**

Java

`// Controller (HTTP Adapter)
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final CreateOrderUseCase createOrderUseCase;
    
    public OrderController(CreateOrderUseCase createOrderUseCase) {
        this.createOrderUseCase = createOrderUseCase;
    }
    
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @RequestBody CreateOrderRequest request) {
        
        // Convert HTTP request to use case command
        CreateOrderCommand command = new CreateOrderCommand(
            request.getOrderId(),
            request.getItems().stream()
                .map(itemReq -> new OrderItemCommand(
                    itemReq.getProductId(),
                    itemReq.getQuantity(),
                    itemReq.getPrice()
                ))
                .collect(Collectors.toList())
        );
        
        // Execute use case
        OrderResponse response = createOrderUseCase.execute(command);
        
        // Return HTTP response
        return ResponseEntity.ok(response);
    }
}

// Repository Adapter (Database Adapter)
@Repository
public class JpaOrderRepository implements OrderRepository {
    private final OrderJpaRepository jpaRepository;
    
    public JpaOrderRepository(OrderJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }
    
    @Override
    public void save(Order order) {
        // Convert domain Order to JPA Entity
        OrderEntity entity = new OrderEntity(
            order.getId().getValue(),
            order.getTotal(),
            order.getStatus().toString()
        );
        
        // Save to database
        jpaRepository.save(entity);
    }
    
    @Override
    public Optional<Order> findById(OrderId orderId) {
        // Query database
        return jpaRepository.findById(orderId.getValue())
            // Convert JPA Entity back to domain Order
            .map(entity -> new Order(
                new OrderId(entity.getId()),
                // ... reconstruct order from entity ...
            ));
    }
    
    @Override
    public boolean existsById(OrderId orderId) {
        return jpaRepository.existsById(orderId.getValue());
    }
}

// JPA Entity (database schema)
@Entity
@Table(name = "orders")
public class OrderEntity {
    @Id
    private String id;
    
    @Column(name = "total")
    private double total;
    
    @Column(name = "status")
    private String status;
    
    // Constructor, getters, setters
}

// JPA Repository (Spring Data)
@Repository
public interface OrderJpaRepository extends JpaRepository<OrderEntity, String> {
}

// HTTP Request DTO
public class CreateOrderRequest {
    private String orderId;
    private List<OrderItemRequest> items;
    
    // getters, setters
}

public class OrderItemRequest {
    private String productId;
    private int quantity;
    private double price;
    
    // getters, setters
}`

## **Layer 4: Frameworks & Drivers**

**External libraries, cấu hình, database driver.**

### **Nội dung**

- Spring configuration.
- Database connection.
- Message broker setup.
- HTTP server.
- Logging framework.

### **Đặc điểm**

- Bạn không cần viết code ở layer này, chỉ cấu hình.
- Nếu viết code ở đây, nó phải có thể thay thế được dễ dàng.

### **Ví dụ**

Java

`// Spring Configuration
@Configuration
public class ApplicationConfig {
    
    @Bean
    public OrderRepository orderRepository(OrderJpaRepository jpaRepository) {
        return new JpaOrderRepository(jpaRepository);
    }
    
    @Bean
    public OrderValidator orderValidator() {
        return new OrderValidator();
    }
    
    @Bean
    public CreateOrderUseCase createOrderUseCase(
            OrderRepository repository,
            OrderValidator validator) {
        return new CreateOrderUseCase(repository, validator);
    }
}

// DataSource Configuration
@Configuration
public class DataSourceConfig {
    
    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://localhost:3306/orderdb");
        config.setUsername("root");
        config.setPassword("password");
        return new HikariDataSource(config);
    }
}`

---

# **3. Sơ đồ hoàn chỉnh**

**Flow**

`┌────────────────────────────────────────┐
│        Frameworks & Drivers             │
│  (Spring Config, JPA, HTTP Server)      │
├─────────────────────────────────────────┤
│      Interface Adapters                 │
│  ┌──────────┐              ┌──────────┐ │
│  │Controller│              │Repository│ │
│  │(HTTP In) │              │(DB Out)  │ │
│  └──────────┘              └──────────┘ │
├─────────────────────────────────────────┤
│       Application                       │
│  ┌──────────────────────────────────┐   │
│  │CreateOrderUseCase                │   │
│  │  - Execute business logic        │   │
│  │  - Coordinate domain & ports     │   │
│  └──────────────────────────────────┘   │
│  ┌──────────────────────────────────┐   │
│  │OrderRepository Interface         │   │
│  │(Port definition, no impl)        │   │
│  └──────────────────────────────────┘   │
├─────────────────────────────────────────┤
│          Domain                         │
│  ┌──────────────────────────────────┐   │
│  │Order (Entity)                    │   │
│  │  - OrderId (Value Object)        │   │
│  │  - OrderItem (Value Object)      │   │
│  │  - OrderStatus (Value Object)    │   │
│  │  - confirm()                     │   │
│  │  - cancel()                      │   │
│  │  - calculateTotal()              │   │
│  └──────────────────────────────────┘   │
└─────────────────────────────────────────┘`

---

# **4. Project Structure (Maven)**

**Flow**

`order-service/
├── pom.xml
├── src/
│   ├── main/
│   │   └── java/
│   │       └── com/example/order/
│   │           ├── domain/
│   │           │   ├── model/
│   │           │   │   ├── Order.java
│   │           │   │   ├── OrderId.java
│   │           │   │   ├── OrderItem.java
│   │           │   │   ├── OrderStatus.java
│   │           │   │   └── Money.java
│   │           │   └── exception/
│   │           │       └── DomainException.java
│   │           │
│   │           ├── application/
│   │           │   ├── port/
│   │           │   │   ├── in/
│   │           │   │   │   └── CreateOrderUseCase.java
│   │           │   │   └── out/
│   │           │   │       └── OrderRepository.java
│   │           │   ├── service/
│   │           │   │   ├── CreateOrderService.java
│   │           │   │   └── OrderValidator.java
│   │           │   └── dto/
│   │           │       ├── CreateOrderCommand.java
│   │           │       └── OrderResponse.java
│   │           │
│   │           ├── adapter/
│   │           │   ├── in/
│   │           │   │   └── web/
│   │           │   │       └── OrderController.java
│   │           │   └── out/
│   │           │       └── persistence/
│   │           │           ├── JpaOrderRepository.java
│   │           │           └── OrderEntity.java
│   │           │
│   │           └── config/
│   │               └── ApplicationConfig.java
│   │
│   └── test/
│       └── java/
│           └── com/example/order/
│               ├── domain/
│               │   └── model/
│               │       └── OrderTest.java
│               ├── application/
│               │   └── service/
│               │       └── CreateOrderServiceTest.java
│               └── adapter/
│                   └── in/
│                       └── web/
│                           └── OrderControllerTest.java`

---

# **5. Ví dụ thực hiện use case hoàn chỉnh**

## **Request flow**

Code

`1. POST /api/orders
        ↓
2. OrderController receives HTTP request
        ↓
3. Convert to CreateOrderCommand
        ↓
4. Call CreateOrderUseCase.execute()
        ↓
5. CreateOrderUseCase
   - Validate command
   - Create Order entity
   - Call order.confirm()
   - Call orderRepository.save()
        ↓
6. JpaOrderRepository converts Order to OrderEntity
        ↓
7. Save to database via Spring Data
        ↓
8. Return OrderResponse to client`

## **Code đầy đủ**

Java

`// 1. HTTP Request DTO
public class CreateOrderRequest {
    private String orderId;
    private List<OrderItemRequest> items;
    
    public CreateOrderRequest() {}
    
    public CreateOrderRequest(String orderId, List<OrderItemRequest> items) {
        this.orderId = orderId;
        this.items = items;
    }
    
    public String getOrderId() { return orderId; }
    public List<OrderItemRequest> getItems() { return items; }
}

public class OrderItemRequest {
    private String productId;
    private int quantity;
    private double price;
    
    public OrderItemRequest() {}
    
    public OrderItemRequest(String productId, int quantity, double price) {
        this.productId = productId;
        this.quantity = quantity;
        this.price = price;
    }
    
    public String getProductId() { return productId; }
    public int getQuantity() { return quantity; }
    public double getPrice() { return price; }
}

// 2. Domain Entities (No Spring, No JPA)
public class OrderId {
    private final String value;
    
    public OrderId(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new DomainException("OrderId cannot be empty");
        }
        this.value = value;
    }
    
    public String getValue() { return value; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrderId)) return false;
        OrderId orderId = (OrderId) o;
        return Objects.equals(value, orderId.value);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}

public class Money {
    private final double amount;
    
    public Money(double amount) {
        if (amount < 0) {
            throw new DomainException("Money cannot be negative");
        }
        this.amount = amount;
    }
    
    public double getAmount() { return amount; }
    
    public Money add(Money other) {
        return new Money(this.amount + other.amount);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Money)) return false;
        Money money = (Money) o;
        return Double.compare(money.amount, amount) == 0;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(amount);
    }
}

public class OrderItem {
    private final String productId;
    private final int quantity;
    private final Money price;
    
    public OrderItem(String productId, int quantity, double price) {
        if (quantity <= 0) {
            throw new DomainException("Quantity must be positive");
        }
        this.productId = productId;
        this.quantity = quantity;
        this.price = new Money(price);
    }
    
    public String getProductId() { return productId; }
    public int getQuantity() { return quantity; }
    public Money getPrice() { return price; }
    
    public Money getSubtotal() {
        return new Money(price.getAmount() * quantity);
    }
}

public enum OrderStatus {
    PENDING, CONFIRMED, CANCELLED, SHIPPED
}

public class Order {
    private final OrderId id;
    private final List<OrderItem> items;
    private OrderStatus status;
    
    public Order(OrderId id, List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            throw new DomainException("Order must have at least one item");
        }
        this.id = id;
        this.items = new ArrayList<>(items);
        this.status = OrderStatus.PENDING;
    }
    
    public OrderId getId() { return id; }
    public List<OrderItem> getItems() { return new ArrayList<>(items); }
    public OrderStatus getStatus() { return status; }
    
    public Money calculateTotal() {
        Money total = new Money(0);
        for (OrderItem item : items) {
            total = total.add(item.getSubtotal());
        }
        return total;
    }
    
    public void confirm() {
        if (status != OrderStatus.PENDING) {
            throw new DomainException("Can only confirm PENDING orders");
        }
        this.status = OrderStatus.CONFIRMED;
    }
    
    public void cancel() {
        if (status == OrderStatus.SHIPPED) {
            throw new DomainException("Cannot cancel shipped order");
        }
        this.status = OrderStatus.CANCELLED;
    }
}

public class DomainException extends RuntimeException {
    public DomainException(String message) {
        super(message);
    }
}

// 3. Application Layer
public class CreateOrderCommand {
    private final String orderId;
    private final List<CreateOrderItemCommand> items;
    
    public CreateOrderCommand(String orderId, List<CreateOrderItemCommand> items) {
        this.orderId = orderId;
        this.items = items;
    }
    
    public String getOrderId() { return orderId; }
    public List<CreateOrderItemCommand> getItems() { return items; }
}

public class CreateOrderItemCommand {
    private final String productId;
    private final int quantity;
    private final double price;
    
    public CreateOrderItemCommand(String productId, int quantity, double price) {
        this.productId = productId;
        this.quantity = quantity;
        this.price = price;
    }
    
    public String getProductId() { return productId; }
    public int getQuantity() { return quantity; }
    public double getPrice() { return price; }
}

public class OrderResponse {
    private final String orderId;
    private final double total;
    private final String status;
    
    public OrderResponse(String orderId, double total, String status) {
        this.orderId = orderId;
        this.total = total;
        this.status = status;
    }
    
    public String getOrderId() { return orderId; }
    public double getTotal() { return total; }
    public String getStatus() { return status; }
}

// Repository Port (Interface in Application Layer)
public interface OrderRepository {
    void save(Order order);
    Optional<Order> findById(OrderId orderId);
}

// Validator
public class OrderValidator {
    public void validate(CreateOrderCommand command) {
        if (command.getOrderId() == null || command.getOrderId().trim().isEmpty()) {
            throw new ApplicationException("OrderId is required");
        }
        
        if (command.getItems() == null || command.getItems().isEmpty()) {
            throw new ApplicationException("Order must have items");
        }
        
        for (CreateOrderItemCommand item : command.getItems()) {
            if (item.getQuantity() <= 0) {
                throw new ApplicationException("Quantity must be positive");
            }
            if (item.getPrice() < 0) {
                throw new ApplicationException("Price cannot be negative");
            }
        }
    }
}

public class ApplicationException extends RuntimeException {
    public ApplicationException(String message) {
        super(message);
    }
}

// Use Case Implementation
public class CreateOrderUseCase {
    private final OrderRepository orderRepository;
    private final OrderValidator validator;
    
    public CreateOrderUseCase(OrderRepository orderRepository, OrderValidator validator) {
        this.orderRepository = orderRepository;
        this.validator = validator;
    }
    
    public OrderResponse execute(CreateOrderCommand command) {
        // 1. Validate
        validator.validate(command);
        
        // 2. Create domain objects
        OrderId orderId = new OrderId(command.getOrderId());
        List<OrderItem> items = command.getItems().stream()
            .map(itemCmd -> new OrderItem(
                itemCmd.getProductId(),
                itemCmd.getQuantity(),
                itemCmd.getPrice()
            ))
            .collect(Collectors.toList());
        
        // 3. Create and manipulate aggregate
        Order order = new Order(orderId, items);
        order.confirm(); // business operation
        
        // 4. Persist
        orderRepository.save(order);
        
        // 5. Return response
        Money total = order.calculateTotal();
        return new OrderResponse(
            order.getId().getValue(),
            total.getAmount(),
            order.getStatus().toString()
        );
    }
}

// 4. Adapter Layer - Controller
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final CreateOrderUseCase createOrderUseCase;
    
    public OrderController(CreateOrderUseCase createOrderUseCase) {
        this.createOrderUseCase = createOrderUseCase;
    }
    
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @RequestBody CreateOrderRequest request) {
        try {
            // Convert HTTP request to command
            CreateOrderCommand command = new CreateOrderCommand(
                request.getOrderId(),
                request.getItems().stream()
                    .map(itemReq -> new CreateOrderItemCommand(
                        itemReq.getProductId(),
                        itemReq.getQuantity(),
                        itemReq.getPrice()
                    ))
                    .collect(Collectors.toList())
            );
            
            // Execute use case
            OrderResponse response = createOrderUseCase.execute(command);
            
            // Return response
            return ResponseEntity.ok(response);
        } catch (ApplicationException e) {
            return ResponseEntity.badRequest().build();
        } catch (DomainException e) {
            return ResponseEntity.status(400).build();
        }
    }
}

// 4. Adapter Layer - Repository Adapter
@Repository
public class JpaOrderRepository implements OrderRepository {
    private final OrderJpaRepository jpaRepository;
    
    public JpaOrderRepository(OrderJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }
    
    @Override
    public void save(Order order) {
        OrderEntity entity = new OrderEntity();
        entity.setId(order.getId().getValue());
        entity.setTotal(order.calculateTotal().getAmount());
        entity.setStatus(order.getStatus().toString());
        
        // Save items
        List<OrderItemEntity> itemEntities = order.getItems().stream()
            .map(item -> {
                OrderItemEntity itemEntity = new OrderItemEntity();
                itemEntity.setProductId(item.getProductId());
                itemEntity.setQuantity(item.getQuantity());
                itemEntity.setPrice(item.getPrice().getAmount());
                return itemEntity;
            })
            .collect(Collectors.toList());
        entity.setItems(itemEntities);
        
        jpaRepository.save(entity);
    }
    
    @Override
    public Optional<Order> findById(OrderId orderId) {
        return jpaRepository.findById(orderId.getValue())
            .map(entity -> {
                List<OrderItem> items = entity.getItems().stream()
                    .map(itemEntity -> new OrderItem(
                        itemEntity.getProductId(),
                        itemEntity.getQuantity(),
                        itemEntity.getPrice()
                    ))
                    .collect(Collectors.toList());
                
                return new Order(
                    new OrderId(entity.getId()),
                    items
                );
            });
    }
}

// 4. JPA Entities
@Entity
@Table(name = "orders")
public class OrderEntity {
    @Id
    private String id;
    
    @Column(name = "total")
    private double total;
    
    @Column(name = "status")
    private String status;
    
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "order_id")
    private List<OrderItemEntity> items;
    
    // Getters, Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public List<OrderItemEntity> getItems() { return items; }
    public void setItems(List<OrderItemEntity> items) { this.items = items; }
}

@Entity
@Table(name = "order_items")
public class OrderItemEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "order_id")
    private String orderId;
    
    @Column(name = "product_id")
    private String productId;
    
    @Column(name = "quantity")
    private int quantity;
    
    @Column(name = "price")
    private double price;
    
    // Getters, Setters
    public Long getId() { return id; }
    public String getOrderId() { return orderId; }
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
}

// JPA Repository
@Repository
public interface OrderJpaRepository extends JpaRepository<OrderEntity, String> {
}

// 5. Configuration
@Configuration
public class ApplicationConfig {
    
    @Bean
    public OrderValidator orderValidator() {
        return new OrderValidator();
    }
    
    @Bean
    public OrderRepository orderRepository(OrderJpaRepository jpaRepository) {
        return new JpaOrderRepository(jpaRepository);
    }
    
    @Bean
    public CreateOrderUseCase createOrderUseCase(
            OrderRepository orderRepository,
            OrderValidator validator) {
        return new CreateOrderUseCase(orderRepository, validator);
    }
}`

---

# **6. Unit Testing**

Một lợi ích lớn của Clean Architecture là **test dễ**.

Java

`// Test Domain
public class OrderTest {
    
    @Test
    public void testOrderMustHaveItems() {
        assertThrows(DomainException.class, () -> {
            new Order(new OrderId("123"), new ArrayList<>());
        });
    }
    
    @Test
    public void testCalculateTotal() {
        List<OrderItem> items = Arrays.asList(
            new OrderItem("P1", 2, 100.0),
            new OrderItem("P2", 1, 50.0)
        );
        Order order = new Order(new OrderId("123"), items);
        
        assertEquals(250.0, order.calculateTotal().getAmount());
    }
    
    @Test
    public void testConfirmOrder() {
        List<OrderItem> items = Arrays.asList(
            new OrderItem("P1", 1, 100.0)
        );
        Order order = new Order(new OrderId("123"), items);
        
        order.confirm();
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
    }
    
    @Test
    public void testCannotConfirmTwice() {
        List<OrderItem> items = Arrays.asList(
            new OrderItem("P1", 1, 100.0)
        );
        Order order = new Order(new OrderId("123"), items);
        
        order.confirm();
        assertThrows(DomainException.class, order::confirm);
    }
}

// Test Use Case
public class CreateOrderUseCaseTest {
    
    private OrderRepository orderRepository;
    private OrderValidator validator;
    private CreateOrderUseCase useCase;
    
    @Before
    public void setUp() {
        orderRepository = new InMemoryOrderRepository();
        validator = new OrderValidator();
        useCase = new CreateOrderUseCase(orderRepository, validator);
    }
    
    @Test
    public void testCreateOrderSuccess() {
        CreateOrderCommand command = new CreateOrderCommand(
            "ORD123",
            Arrays.asList(
                new CreateOrderItemCommand("P1", 2, 100.0)
            )
        );
        
        OrderResponse response = useCase.execute(command);
        
        assertEquals("ORD123", response.getOrderId());
        assertEquals(200.0, response.getTotal());
        assertEquals("CONFIRMED", response.getStatus());
    }
    
    @Test
    public void testCreateOrderEmptyItems() {
        CreateOrderCommand command = new CreateOrderCommand("ORD123", new ArrayList<>());
        
        assertThrows(ApplicationException.class, () -> useCase.execute(command));
    }
}

// In-Memory Repository for Testing
public class InMemoryOrderRepository implements OrderRepository {
    private Map<OrderId, Order> storage = new HashMap<>();
    
    @Override
    public void save(Order order) {
        storage.put(order.getId(), order);
    }
    
    @Override
    public Optional<Order> findById(OrderId orderId) {
        return Optional.ofNullable(storage.get(orderId));
    }
}

// Test Controller
@SpringBootTest
@AutoConfigureMockMvc
public class OrderControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private CreateOrderUseCase createOrderUseCase;
    
    @Test
    public void testCreateOrderEndpoint() throws Exception {
        OrderResponse response = new OrderResponse("ORD123", 200.0, "CONFIRMED");
        when(createOrderUseCase.execute(any())).thenReturn(response);
        
        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "orderId": "ORD123",
                        "items": [
                            {"productId": "P1", "quantity": 2, "price": 100.0}
                        ]
                    }
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderId").value("ORD123"))
            .andExpect(jsonPath("$.total").value(200.0));
    }
}`

---

# **7. Checklist: Bạn đã hiểu Clean Architecture?**

- [ ]  Giải thích được Dependency Rule.
- [ ]  Biết Domain không nên import framework.
- [ ]  Biết Application layer không phụ thuộc database cụ thể.
- [ ]  Biết Controller không chứa business logic.
- [ ]  Phân biệt được Entity và DTO.
- [ ]  Hiểu Repository Interface nằm ở Application, implement ở Adapter.
- [ ]  Có thể vẽ sơ đồ layer.
- [ ]  Có thể test domain mà không cần database.
- [ ]  Có thể thay database mà không sửa business logic.
- [ ]  Có thể vẽ flow từ HTTP request đến database.