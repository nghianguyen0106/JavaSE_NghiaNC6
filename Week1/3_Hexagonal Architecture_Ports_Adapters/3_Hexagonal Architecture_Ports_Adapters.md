# 3. Hexagonal Architecture / Ports and Adapters
## **Cần học**

Mentee cần tìm hiểu:

- Hexagonal Architecture.
- Port là gì.
- Adapter là gì.
- Primary/driving adapter.
- Secondary/driven adapter.
- Input port.
- Output port.

## **Cần hiểu**

### **Primary adapter**

Là thành phần gọi vào hệ thống:

- REST Controller.
- CLI.
- Message Consumer.
- Scheduled Job.

## **Tiêu chí hoàn thành**

- Có thể thay REST Controller bằng CLI mà không sửa domain.
- Có thể thay in-memory repository bằng database adapter.
- Application service chỉ phụ thuộc vào port/interface.
- Adapter không làm thay business rule của domain.

---

## Hexagonal Architecture là gì?

**Hexagonal Architecture** (còn gọi **Ports & Adapters**) là một cách tổ chức code dựa trên **port và adapter**.

Tên gọi "hexagonal" vì biểu diễn hình dạng 6 cạnh (lục giác), nhưng số cạnh không quan trọng. Ý chính là:

> **Hệ thống là một hình lục giác với các "port" ở các cạnh. Bên ngoài gắn các "adapter" để giao tiếp với external world.**
> 

Ghi nhớ 1 ý chính:

> **Application (trái tim hệ thống) giao tiếp với external world thông qua port và adapter.**
> 

---

# **Sơ đồ Hexagonal Architecture**

## **Hình lục giác**

Text

                    `┌─────────────────────────────────────┐
                   ╱                                       ╲
                  ╱          HEXAGONAL CORE                ╲
                 ╱        (Application & Domain)            ╲
                │                                           │
         HTTP   │        ┌───────────────────────┐         │  CLI
        Adapter │        │  CreateOrderUseCase   │         │ Adapter
                │        │  (Input Port)         │         │
                │        └───────────────────────┘         │
                │                ↓                         │
                │        ┌───────────────────────┐         │
                │        │ OrderApplicationSvc   │         │ Message
                │        │ (Business Logic)      │         │ Adapter
                │        └───────────────────────┘         │
                │                ↓                         │
                │        ┌───────────────────────┐         │
                │        │ Order (Domain Model)  │         │
                │        └───────────────────────┘         │
                │                ↑                         │
                │        ┌───────────────────────┐         │
                │        │ OrderRepository       │         │
                │        │ (Output Port)         │         │
                │        └───────────────────────┘         │
                 ╲                                        ╱
                  ╲          Port Definitions            ╱
                   ╲                                    ╱
                    └─────────────────────────────────┘
                            │      │      │
                    ┌───────┴──┬───┴──┬───┴──────┐
                    │          │      │          │
              PostgreSQL    MongoDB  Kafka   Email
              Adapter       Adapter  Adapter Adapter
              (Output)      (Output) (Output)(Output)`

## **Sơ đồ tuyến tính (dễ hiểu hơn)**

Text

`┌──────────────────────────────────────────────────────────────────────┐
│                          EXTERNAL WORLD                              │
│  (HTTP, CLI, Message Broker, Database, Email, Payment Gateway)       │
└──────────────────────────────────────────────────────────────────────┘
   ↑                                                                ↑
   │ Input                                                 Output  │
   │ Adapters                                              Adapters│
   │                                                              │
   ├──────────────────────────────────────────────────────────────┤
   │                                                              │
   │  ┌─────────────┐         ┌──────────────────────────────┐  │
   │  │HTTP Adapter │         │PostgreSQL/Mongo Adapter      │  │
   │  │ (Controller)│         │Email Adapter                 │  │
   │  │CLI Adapter  │         │Payment Adapter               │  │
   │  │Message      │         │Kafka Adapter                 │  │
   │  │Adapter      │         └──────────────────────────────┘  │
   │  └─────┬───────┘                     ▲                     │
   │        │                             │                     │
   │        └─────────────────┬───────────┘                     │
   │                          │                                 │
   │  ┌───────────────────────┴────────────────────────┐        │
   │  │          HEXAGONAL CORE                       │        │
   │  │  ┌────────────────────────────────────────┐   │        │
   │  │  │  INPUT PORTS (Use Cases)               │   │        │
   │  │  │  - CreateOrderUseCase                  │   │        │
   │  │  │  - GetOrderUseCase                     │   │        │
   │  │  │  - CancelOrderUseCase                  │   │        │
   │  │  └────────────────────────────────────────┘   │        │
   │  │                     ▼                          │        │
   │  │  ┌────────────────────────────────────────┐   │        │
   │  │  │  APPLICATION SERVICES                  │   │        │
   │  │  │  - CreateOrderService                  │   │        │
   │  │  │  - GetOrderService                     │   │        │
   │  │  │  - CancelOrderService                  │   │        │
   │  │  └────────────────────────────────────────┘   │        │
   │  │                     ▼                          │        │
   │  │  ┌────────────────────────────────────────┐   │        │
   │  │  │  DOMAIN MODEL (Business Rules)         │   │        │
   │  │  │  - Order                               │   │        │
   │  │  │  - OrderItem                           │   │        │
   │  │  │  - OrderStatus                         │   │        │
   │  │  │  - Business Rules & Invariants         │   │        │
   │  │  └────────────────────────────────────────┘   │        │
   │  │                     ▼                          │        │
   │  │  ┌────────────────────────────────────────┐   │        │
   │  │  │  OUTPUT PORTS (Interfaces)             │   │        │
   │  │  │  - OrderRepository                     │   │        │
   │  │  │  - EmailService                        │   │        │
   │  │  │  - PaymentGateway                      │   │        │
   │  │  │  - EventPublisher                      │   │        │
   │  │  └────────────────────────────────────────┘   │        │
   │  │                                                │        │
   │  └────────────────────────────────────────────────┘        │
   │                                                              │
   └──────────────────────────────────────────────────────────────┘`

# **So sánh: Hexagonal vs Clean Architecture**

| **Khía cạnh** | **Clean Architecture** | **Hexagonal Architecture** |
| --- | --- | --- |
| **Focus** | Layer (Domain, Application, Adapter, Framework) | Port & Adapter |
| **Dependency flow** | Hướng vào trong | Hướng vào trong |
| **Input** | Controller | Input Adapter (Controller, CLI, Message) |
| **Output** | Repository | Output Adapter (Database, Email, API) |
| **Port** | Repository Interface | Input Port + Output Port |
| **Testing** | Có thể test với in-memory repository | Có thể test với mock adapter |
| **Swapping dependencies** | Có thể thay database | Có thể thay bất kỳ external dependency |

**Tương tự nhau:**

- Cả hai đều tách domain khỏi framework.
- Cả hai đều dùng dependency inversion.
- Cả hai đều dễ test.

**Khác nhau:**

- Hexagonal tập trung vào **port & adapter** (giao tiếp).
- Clean Architecture tập trung vào **layer** (tổ chức code).

**Kết hợp:** Bạn có thể dùng cả hai cùng lúc!

Text

`Hexagonal Port & Adapter (giao tiếp)
           ↓
Clean Architecture Layer (tổ chức)`

---

# **Checklist: Bạn đã hiểu Hexagonal Architecture?**

- [ ]  Input port là interface định nghĩa use case.
- [ ]  Input adapter nhận request từ bên ngoài và gọi input port.
- [ ]  Có thể có N input adapter cho 1 input port (HTTP, CLI, Message).
- [ ]  Output port là interface định nghĩa external dependency.
- [ ]  Output adapter implement output port.
- [ ]  Có thể có N output adapter cho 1 output port (PostgreSQL, MongoDB, Mock).
- [ ]  Application service implement input port và gọi output port.
- [ ]  Domain không import framework.
- [ ]  Controller không chứa business logic.
- [ ]  Có thể test với mock/in-memory adapter.
- [ ]  Có thể swap adapter bằng configuration.
- [ ]  Có thể vẽ sơ đồ hexagon cho bài toán Order.

---

# **2. Ví dụ thực tế: Complete Order Service**

Bạn có thể copy toàn bộ code dưới đây và chạy:

## **pom.xml**

XML

`<project>
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.example</groupId>
    <artifactId>order-service</artifactId>
    <version>1.0.0</version>
    
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.7.0</version>
    </parent>
    
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>runtime</scope>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>`

## **Domain Layer**

Java

`// com.example.order.domain.model.OrderId
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

// com.example.order.domain.model.Money
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

// com.example.order.domain.model.OrderItem
public class OrderItem {
    private final String productId;
    private final int quantity;
    private final Money price;
    
    public OrderItem(String productId, int quantity, Money price) {
        if (quantity <= 0) {
            throw new DomainException("Quantity must be positive");
        }
        this.productId = productId;
        this.quantity = quantity;
        this.price = price;
    }
    
    public String getProductId() { return productId; }
    public int getQuantity() { return quantity; }
    public Money getPrice() { return price; }
    
    public Money getSubtotal() {
        return new Money(price.getAmount() * quantity);
    }
}

// com.example.order.domain.model.OrderStatus
public enum OrderStatus {
    PENDING, CONFIRMED, CANCELLED, SHIPPED
}

// com.example.order.domain.model.Order
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

// com.example.order.domain.exception.DomainException
public class DomainException extends RuntimeException {
    public DomainException(String message) {
        super(message);
    }
}`

## **Application Layer**

Java

`// com.example.order.application.port.in.CreateOrderCommand
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

// com.example.order.application.port.in.OrderResponse
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

// com.example.order.application.port.in.CreateOrderUseCase
public interface CreateOrderUseCase {
    OrderResponse create(CreateOrderCommand command);
}

// com.example.order.application.port.out.OrderRepository
public interface OrderRepository {
    void save(Order order);
    Optional<Order> findById(OrderId orderId);
}

// com.example.order.application.port.out.EmailService
public interface EmailService {
    void sendConfirmation(Order order);
}

// com.example.order.application.service.CreateOrderService
@Service
public class CreateOrderService implements CreateOrderUseCase {
    private final OrderRepository orderRepository;
    private final EmailService emailService;
    private final OrderValidator validator;
    
    public CreateOrderService(
        OrderRepository orderRepository,
        EmailService emailService,
        OrderValidator validator
    ) {
        this.orderRepository = orderRepository;
        this.emailService = emailService;
        this.validator = validator;
    }
    
    @Override
    public OrderResponse create(CreateOrderCommand command) {
        validator.validate(command);
        
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
        
        order.confirm();
        orderRepository.save(order);
        emailService.sendConfirmation(order);
        
        return new OrderResponse(
            order.getId().getValue(),
            order.calculateTotal().getAmount(),
            order.getStatus().toString()
        );
    }
}

// com.example.order.application.service.OrderValidator
@Component
public class OrderValidator {
    public void validate(CreateOrderCommand command) {
        if (command.getOrderId() == null || command.getOrderId().trim().isEmpty()) {
            throw new ApplicationException("OrderId is required");
        }
        if (command.getItems() == null || command.getItems().isEmpty()) {
            throw new ApplicationException("Order must have items");
        }
    }
}

// com.example.order.application.exception.ApplicationException
public class ApplicationException extends RuntimeException {
    public ApplicationException(String message) {
        super(message);
    }
}`

## **Adapter Layer - Input**

Java

`// com.example.order.adapter.in.web.OrderController
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
            CreateOrderCommand command = new CreateOrderCommand(
                request.getOrderId(),
                request.getItems().stream()
                    .map(item -> new CreateOrderItemCommand(
                        item.getProductId(),
                        item.getQuantity(),
                        item.getPrice()
                    ))
                    .collect(Collectors.toList())
            );
            
            OrderResponse response = createOrderUseCase.create(command);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}

// com.example.order.adapter.in.web.CreateOrderRequest
public class CreateOrderRequest {
    private String orderId;
    private List<OrderItemRequest> items;
    
    public CreateOrderRequest() {}
    
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    
    public List<OrderItemRequest> getItems() { return items; }
    public void setItems(List<OrderItemRequest> items) { this.items = items; }
}

public class OrderItemRequest {
    private String productId;
    private int quantity;
    private double price;
    
    public OrderItemRequest() {}
    
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
}`

## **Adapter Layer - Output**

Java

`// com.example.order.adapter.out.persistence.JpaOrderRepository
@Repository
public class JpaOrderRepository implements OrderRepository {
    private final OrderJpaRepository jpaRepository;
    
    public JpaOrderRepository(OrderJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }
    
    @Override
    public void save(Order order) {
        OrderEntity entity = new OrderEntity(
            order.getId().getValue(),
            order.calculateTotal().getAmount(),
            order.getStatus().toString()
        );
        jpaRepository.save(entity);
    }
    
    @Override
    public Optional<Order> findById(OrderId orderId) {
        return jpaRepository.findById(orderId.getValue())
            .map(this::toDomain);
    }
    
    private Order toDomain(OrderEntity entity) {
        return new Order(
            new OrderId(entity.getId()),
            entity.getItems().stream()
                .map(item -> new OrderItem(
                    item.getProductId(),
                    item.getQuantity(),
                    new Money(item.getPrice())
                ))
                .collect(Collectors.toList())
        );
    }
}

// com.example.order.adapter.out.persistence.OrderEntity
@Entity
@Table(name = "orders")
public class OrderEntity {
    @Id
    private String id;
    
    @Column(name = "total")
    private double total;
    
    @Column(name = "status")
    private String status;
    
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "order_id")
    private List<OrderItemEntity> items = new ArrayList<>();
    
    public OrderEntity() {}
    
    public OrderEntity(String id, double total, String status) {
        this.id = id;
        this.total = total;
        this.status = status;
    }
    
    public String getId() { return id; }
    public double getTotal() { return total; }
    public String getStatus() { return status; }
    public List<OrderItemEntity> getItems() { return items; }
    public void setItems(List<OrderItemEntity> items) { this.items = items; }
}

@Entity
@Table(name = "order_items")
public class OrderItemEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "product_id")
    private String productId;
    
    @Column(name = "quantity")
    private int quantity;
    
    @Column(name = "price")
    private double price;
    
    public OrderItemEntity() {}
    
    public OrderItemEntity(String productId, int quantity, double price) {
        this.productId = productId;
        this.quantity = quantity;
        this.price = price;
    }
    
    public String getProductId() { return productId; }
    public int getQuantity() { return quantity; }
    public double getPrice() { return price; }
}

@Repository
public interface OrderJpaRepository extends JpaRepository<OrderEntity, String> {
}

// com.example.order.adapter.out.notification.SmtpEmailAdapter
@Component
public class SmtpEmailAdapter implements EmailService {
    private static final Logger logger = LoggerFactory.getLogger(SmtpEmailAdapter.class);
    
    @Override
    public void sendConfirmation(Order order) {
        logger.info("Sending confirmation email for order: " + order.getId().getValue());
        // In production, use JavaMailSender
    }
}

// com.example.order.adapter.out.notification.MockEmailAdapter
public class MockEmailAdapter implements EmailService {
    private List<String> sentEmails = new ArrayList<>();
    
    @Override
    public void sendConfirmation(Order order) {
        sentEmails.add("Confirmation: " + order.getId().getValue());
    }
    
    public List<String> getSentEmails() {
        return sentEmails;
    }
}

// com.example.order.adapter.out.persistence.InMemoryOrderRepository
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
}`

## **Configuration**

Java

`// com.example.order.config.ApplicationConfig
@Configuration
public class ApplicationConfig {
    
    @Bean
    public EmailService emailService() {
        return new SmtpEmailAdapter();
    }
    
    @Bean
    public OrderValidator orderValidator() {
        return new OrderValidator();
    }
    
    @Bean
    public CreateOrderUseCase createOrderUseCase(
            OrderRepository orderRepository,
            EmailService emailService,
            OrderValidator validator) {
        return new CreateOrderService(orderRepository, emailService, validator);
    }
}

// com.example.order.config.TestApplicationConfig
@Configuration
@Profile("test")
public class TestApplicationConfig {
    
    @Bean
    public OrderRepository orderRepository() {
        return new InMemoryOrderRepository();
    }
    
    @Bean
    public EmailService emailService() {
        return new MockEmailAdapter();
    }
    
    @Bean
    public OrderValidator orderValidator() {
        return new OrderValidator();
    }
    
    @Bean
    public CreateOrderUseCase createOrderUseCase(
            OrderRepository orderRepository,
            EmailService emailService,
            OrderValidator validator) {
        return new CreateOrderService(orderRepository, emailService, validator);
    }
}`

## **Application Properties**

YAML

`# application.properties
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driverClassName=org.h2.Driver
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=create-drop
spring.h2.console.enabled=true`

---

# **Port vs Adapter — Hiểu rõ khái niệm**

---

**1. Định nghĩa cơ bản**

## **Port**

**Port là một interface định nghĩa "hợp đồng" (contract) giao tiếp.**

- Một **input port** định nghĩa những hành động mà hệ thống **có thể thực hiện**.
- Một **output port** định nghĩa những dịch vụ bên ngoài mà hệ thống **cần sử dụng**.

**Đặc điểm:**

- Là interface Java.
- Không chứa implementation.
- Định nghĩa rõ "vào" và "ra" của hệ thống.
- Language-agnostic (independent từ ngôn ngữ).

## **Adapter**

**Adapter là implementation cụ thể của port.**

- Một **input adapter** nhận yêu cầu từ bên ngoài (HTTP, CLI, message, ...) và gọi vào application thông qua port.
- Một **output adapter** implement port và kết nối với external dependency (database, API, message broker, ...).

**Đặc điểm:**

- Là concrete class implement interface.
- Chứa implementation details (Spring, JPA, HTTP, ...).
- Có thể dễ dàng thay thế.
- Language-specific (đặc thù Java, Spring, ...).

---

# **2. Hình dung bằng hình vẽ**

## **Trước khi dùng Port & Adapter**

`HTTP Request
    ↓
┌─────────────────────────────┐
│   OrderController           │
│  (Chứa HTTP + Business)     │
└─────────────────────────────┘
    ↓
┌─────────────────────────────┐
│   OrderService              │
│  (Chứa Business Logic)      │
└─────────────────────────────┘
    ↓
┌─────────────────────────────┐
│   OrderJpaRepository        │
│  (JPA Implementation)       │
└─────────────────────────────┘
    ↓
Database`

**Vấn đề:**

- Controller biết HTTP details.
- Service không biết ai gọi nó (HTTP, CLI, message).
- Repository luôn là JPA, không thể đổi.

## **Sau khi dùng Port & Adapter**

`HTTP Request
    ↓
┌──────────────────────────────┐
│  HTTP Adapter (Controller)   │ ← INPUT ADAPTER
│  (Chỉ chuyển đổi format)     │
└──────────────────────────────┘
    ↓
┌──────────────────────────────┐
│  CreateOrderUseCase          │ ← INPUT PORT
│  (Interface)                 │
└──────────────────────────────┘
    ↓
┌──────────────────────────────┐
│  OrderApplicationService     │ ← APPLICATION
│  (Implement use case)        │
└──────────────────────────────┘
    ↓
┌──────────────────────────────┐
│  OrderRepository             │ ← OUTPUT PORT
│  (Interface)                 │
└──────────────────────────────┘
    ↓
┌──────────────────────────────┐
│  JpaOrderRepository          │ ← OUTPUT ADAPTER
│  (JPA Implementation)        │
└──────────────────────────────┘
    ↓
Database

Có thể thay thế:
- HTTP Adapter → CLI Adapter → Message Adapter
- JpaOrderRepository → MongoOrderRepository → InMemoryOrderRepository`

---

# **3. Ví dụ chi tiết: E-Commerce Order Service**

## **Tình huống**

Bạn cần xây dựng hệ thống quản lý order.

- Client gọi thông qua **HTTP REST API**.
- Server lưu order vào **PostgreSQL Database**.
- Trong tương lai có thể cần:
    - Gọi từ **CLI**.
    - Gọi từ **Message Queue**.
    - Lưu vào **MongoDB**.
    - Lưu vào **In-memory cache**.

## **Thiết kế Port & Adapter**

### **Step 1: Định nghĩa Port**

Java

`// ============================================
// INPUT PORT: "Hệ thống có thể làm gì?"
// ============================================

public interface CreateOrderUseCase {
    OrderResponse create(CreateOrderCommand command);
}

// ============================================
// OUTPUT PORT: "Hệ thống cần gì từ bên ngoài?"
// ============================================

public interface OrderRepository {
    void save(Order order);
    Optional<Order> findById(OrderId orderId);
}

public interface EmailService {
    void sendConfirmation(Order order);
}

public interface PaymentGateway {
    boolean charge(OrderId orderId, double amount);
}`

**Lưu ý:**

- Ports là **interfaces**.
- Không chứa Spring, JPA, HTTP annotation.
- Chỉ định nghĩa "hợp đồng" (method signature).

### **Step 2: Implement Application**

Java

`// ============================================
// APPLICATION: Core business logic
// ============================================

public class CreateOrderService implements CreateOrderUseCase {
    private final OrderRepository orderRepository;      // Phụ thuộc output port
    private final EmailService emailService;            // Phụ thuộc output port
    private final PaymentGateway paymentGateway;        // Phụ thuộc output port
    private final OrderValidator orderValidator;
    
    public CreateOrderService(
        OrderRepository orderRepository,
        EmailService emailService,
        PaymentGateway paymentGateway,
        OrderValidator orderValidator
    ) {
        this.orderRepository = orderRepository;
        this.emailService = emailService;
        this.paymentGateway = paymentGateway;
        this.orderValidator = orderValidator;
    }
    
    @Override
    public OrderResponse create(CreateOrderCommand command) {
        // 1. Validate
        orderValidator.validate(command);
        
        // 2. Create domain object
        Order order = new Order(
            new OrderId(command.getOrderId()),
            command.getItems().stream()
                .map(item -> new OrderItem(item.getProductId(), item.getQuantity(), item.getPrice()))
                .collect(Collectors.toList())
        );
        
        // 3. Process payment (OUTPUT PORT)
        if (!paymentGateway.charge(order.getId(), order.calculateTotal().getAmount())) {
            throw new PaymentFailedException("Payment failed");
        }
        
        // 4. Confirm order
        order.confirm();
        
        // 5. Save to persistence (OUTPUT PORT)
        orderRepository.save(order);
        
        // 6. Send confirmation email (OUTPUT PORT)
        emailService.sendConfirmation(order);
        
        return new OrderResponse(
            order.getId().getValue(),
            order.calculateTotal().getAmount(),
            order.getStatus().toString()
        );
    }
}`

**Quan trọng:**

- Application service **chỉ gọi thông qua port (interface)**.
- **Không biết** OrderRepository là PostgreSQL hay MongoDB.
- **Không biết** EmailService gửi email hay SMS.
- **Không biết** PaymentGateway là Stripe hay PayPal.

### **Step 3: Tạo Input Adapter**

#### **Input Adapter 1: HTTP REST**

Java

`@RestController
@RequestMapping("/api/orders")
public class HttpOrderAdapter {  // ← INPUT ADAPTER
    private final CreateOrderUseCase createOrderUseCase;  // ← INPUT PORT
    
    public HttpOrderAdapter(CreateOrderUseCase createOrderUseCase) {
        this.createOrderUseCase = createOrderUseCase;
    }
    
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @RequestBody HttpCreateOrderRequest request) {  // ← HTTP format
        
        try {
            // Convert HTTP request to command
            CreateOrderCommand command = new CreateOrderCommand(
                request.getOrderId(),
                request.getItems().stream()
                    .map(item -> new CreateOrderItemCommand(
                        item.getProductId(),
                        item.getQuantity(),
                        item.getPrice()
                    ))
                    .collect(Collectors.toList())
            );
            
            // Call use case through INPUT PORT
            OrderResponse response = createOrderUseCase.create(command);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}`

#### **Input Adapter 2: CLI**

Java

`@Component
public class CliOrderAdapter implements CommandLineRunner {  // ← INPUT ADAPTER
    private final CreateOrderUseCase createOrderUseCase;  // ← INPUT PORT
    
    public CliOrderAdapter(CreateOrderUseCase createOrderUseCase) {
        this.createOrderUseCase = createOrderUseCase;
    }
    
    @Override
    public void run(String... args) throws Exception {
        if (args.length > 0 && args[0].equals("--create-order")) {
            // Parse CLI arguments
            String orderId = args[1];
            List<CreateOrderItemCommand> items = parseItems(args);
            
            CreateOrderCommand command = new CreateOrderCommand(orderId, items);
            
            // Call use case through INPUT PORT
            OrderResponse response = createOrderUseCase.create(command);
            
            System.out.println("Order created: " + response.getOrderId());
        }
    }
    
    private List<CreateOrderItemCommand> parseItems(String[] args) {
        // Parse CLI format
        return Arrays.asList(
            new CreateOrderItemCommand("P1", 2, 100.0)
        );
    }
}`

#### **Input Adapter 3: Message Consumer**

Java

`@Component
public class MessageOrderAdapter {  // ← INPUT ADAPTER
    private final CreateOrderUseCase createOrderUseCase;  // ← INPUT PORT
    
    public MessageOrderAdapter(CreateOrderUseCase createOrderUseCase) {
        this.createOrderUseCase = createOrderUseCase;
    }
    
    @KafkaListener(topics = "order-requests")
    public void consumeOrderRequest(String message) {
        // Parse message (JSON, Avro, ...)
        OrderRequestMessage requestMsg = parseMessage(message);
        
        CreateOrderCommand command = new CreateOrderCommand(
            requestMsg.getOrderId(),
            requestMsg.getItems().stream()
                .map(item -> new CreateOrderItemCommand(
                    item.getProductId(),
                    item.getQuantity(),
                    item.getPrice()
                ))
                .collect(Collectors.toList())
        );
        
        // Call use case through INPUT PORT
        OrderResponse response = createOrderUseCase.create(command);
        
        // Publish result back to topic
        publishResponse(response);
    }
    
    private OrderRequestMessage parseMessage(String message) {
        // Deserialize message
        return new ObjectMapper().readValue(message, OrderRequestMessage.class);
    }
    
    private void publishResponse(OrderResponse response) {
        // Publish to Kafka topic
    }
}`

**Insight:**

- Cùng một **INPUT PORT** (`CreateOrderUseCase`).
- Nhưng có **3 INPUT ADAPTERS** khác nhau.
- Mỗi adapter chỉ xử lý format input khác nhau.
- **Application logic không đổi**.

### **Step 4: Tạo Output Adapter**

#### **Output Adapter 1: PostgreSQL**

Java

`@Repository
public class PostgreSqlOrderRepository implements OrderRepository {  // ← OUTPUT ADAPTER
    private final OrderJpaRepository jpaRepository;
    
    public PostgreSqlOrderRepository(OrderJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }
    
    @Override
    public void save(Order order) {
        OrderEntity entity = new OrderEntity(
            order.getId().getValue(),
            order.calculateTotal().getAmount(),
            order.getStatus().toString()
        );
        jpaRepository.save(entity);
    }
    
    @Override
    public Optional<Order> findById(OrderId orderId) {
        return jpaRepository.findById(orderId.getValue())
            .map(this::toDomain);
    }
    
    private Order toDomain(OrderEntity entity) {
        // Convert JPA entity back to domain object
        return new Order(
            new OrderId(entity.getId()),
            entity.getItems().stream()
                .map(item -> new OrderItem(
                    item.getProductId(),
                    item.getQuantity(),
                    item.getPrice()
                ))
                .collect(Collectors.toList())
        );
    }
}`

#### **Output Adapter 2: MongoDB**

Java

`@Repository
public class MongoDbOrderRepository implements OrderRepository {  // ← OUTPUT ADAPTER
    private final OrderMongoRepository mongoRepository;
    
    public MongoDbOrderRepository(OrderMongoRepository mongoRepository) {
        this.mongoRepository = mongoRepository;
    }
    
    @Override
    public void save(Order order) {
        OrderMongoDocument document = new OrderMongoDocument(
            order.getId().getValue(),
            order.calculateTotal().getAmount(),
            order.getStatus().toString(),
            order.getItems()
        );
        mongoRepository.save(document);
    }
    
    @Override
    public Optional<Order> findById(OrderId orderId) {
        return mongoRepository.findById(orderId.getValue())
            .map(this::toDomain);
    }
    
    private Order toDomain(OrderMongoDocument doc) {
        // Convert MongoDB document to domain object
        return new Order(
            new OrderId(doc.getId()),
            doc.getItems().stream()
                .map(item -> new OrderItem(
                    item.getProductId(),
                    item.getQuantity(),
                    item.getPrice()
                ))
                .collect(Collectors.toList())
        );
    }
}`

#### **Output Adapter 3: In-Memory (for Testing)**

Java

`public class InMemoryOrderRepository implements OrderRepository {  // ← OUTPUT ADAPTER
    private Map<OrderId, Order> storage = new HashMap<>();
    
    @Override
    public void save(Order order) {
        storage.put(order.getId(), order);
    }
    
    @Override
    public Optional<Order> findById(OrderId orderId) {
        return Optional.ofNullable(storage.get(orderId));
    }
}`

#### **Output Adapter 4: Email Service (Smtp)**

Java

`@Component
public class SmtpEmailAdapter implements EmailService {  // ← OUTPUT ADAPTER
    private final JavaMailSender mailSender;
    
    public SmtpEmailAdapter(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }
    
    @Override
    public void sendConfirmation(Order order) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo("customer@example.com");
        message.setSubject("Order Confirmation");
        message.setText("Your order " + order.getId() + " has been confirmed.");
        
        mailSender.send(message);
    }
}`

#### **Output Adapter 5: Email Service (Mock for Testing)**

Java

`public class MockEmailAdapter implements EmailService {  // ← OUTPUT ADAPTER
    private List<String> sentEmails = new ArrayList<>();
    
    @Override
    public void sendConfirmation(Order order) {
        sentEmails.add("Order confirmation sent for " + order.getId());
    }
    
    public List<String> getSentEmails() {
        return sentEmails;
    }
}`

#### **Output Adapter 6: Stripe Payment**

Java

`@Component
public class StripePaymentAdapter implements PaymentGateway {  // ← OUTPUT ADAPTER
    private final StripeClient stripeClient;
    
    public StripePaymentAdapter(StripeClient stripeClient) {
        this.stripeClient = stripeClient;
    }
    
    @Override
    public boolean charge(OrderId orderId, double amount) {
        try {
            StripeChargeRequest request = new StripeChargeRequest(
                orderId.getValue(),
                amount,
                "Order payment"
            );
            StripeChargeResponse response = stripeClient.charge(request);
            return response.isSuccess();
        } catch (Exception e) {
            return false;
        }
    }
}`

#### **Output Adapter 7: Mock Payment (for Testing)**

Java

`public class MockPaymentAdapter implements PaymentGateway {  // ← OUTPUT ADAPTER
    private boolean shouldSucceed = true;
    
    @Override
    public boolean charge(OrderId orderId, double amount) {
        return shouldSucceed;
    }
    
    public void setSuccess(boolean success) {
        this.shouldSucceed = success;
    }
}`

---

# **4. So sánh: Port vs Adapter**

| **Khía cạnh** | **Port** | **Adapter** |
| --- | --- | --- |
| **Là gì** | Interface | Concrete class |
| **Mục đích** | Định nghĩa "hợp đồng" | Implement hợp đồng |
| **Vị trí** | Application layer | Adapter layer |
| **Import** | Không import framework | Import framework |
| **Có thể thay đổi** | Không (thuộc contract) | Có (implementation chi tiết) |
| **Ví dụ** | `OrderRepository` interface | `JpaOrderRepository` class |
| **Số lượng** | 1 input port, N output ports | N input adapters, N output adapters |

---

# **5. Sơ đồ hoàn chỉnh**

`┌─────────────────────────────────────────────────────────────────┐
│                        External World                           │
│  (HTTP, CLI, Kafka, Database, Email, Payment Gateway)           │
└─────────────────────────────────────────────────────────────────┘
                     ↓                              ↓
        ┌─────────────────────────┐    ┌─────────────────────────┐
        │   INPUT ADAPTERS        │    │   OUTPUT ADAPTERS       │
        ├─────────────────────────┤    ├─────────────────────────┤
        │ HttpOrderAdapter        │    │ PostgreSqlOrderRepo     │
        │ CliOrderAdapter         │    │ MongoDbOrderRepo        │
        │ MessageOrderAdapter     │    │ InMemoryOrderRepo       │
        │                         │    │ SmtpEmailAdapter        │
        │                         │    │ StripePaymentAdapter    │
        │                         │    │ MockPaymentAdapter      │
        └─────────────────────────┘    └─────────────────────────┘
                     ↓                              ↑
        ┌─────────────────────────┐    ┌─────────────────────────┐
        │   INPUT PORTS           │    │   OUTPUT PORTS          │
        ├─────────────────────────┤    ├─────────────────────────┤
        │ CreateOrderUseCase      │    │ OrderRepository         │
        │ (Interface)             │    │ (Interface)             │
        │                         │    │                         │
        │ get(command): response  │    │ EmailService (Interface)│
        │                         │    │ PaymentGateway (Iface)  │
        └─────────────────────────┘    └─────────────────────────┘
                     ↓                              ↓
        ┌─────────────────────────────────────────────────────────┐
        │            APPLICATION LOGIC                            │
        ├─────────────────────────────────────────────────────────┤
        │ CreateOrderService implements CreateOrderUseCase        │
        │                                                         │
        │ - Validate command                                      │
        │ - Create Order domain object                            │
        │ - Call paymentGateway.charge() [via OUTPUT PORT]        │
        │ - Call order.confirm()                                  │
        │ - Call orderRepository.save() [via OUTPUT PORT]         │
        │ - Call emailService.sendConfirmation() [via OUTPUT PORT]│
        │                                                         │
        │ [Không import Spring, JPA, HTTP, Kafka, ...]           │
        └─────────────────────────────────────────────────────────┘
                     ↓
        ┌─────────────────────────────────────────────────────────┐
        │              DOMAIN MODEL                               │
        ├─────────────────────────────────────────────────────────┤
        │ Order (Entity)                                          │
        │ - OrderId (Value Object)                                │
        │ - OrderItem (Entity)                                    │
        │ - OrderStatus (Enum)                                    │
        │                                                         │
        │ Business Rules:                                         │
        │ - confirm()                                             │
        │ - calculateTotal()                                      │
        │ - cancel()                                              │
        └─────────────────────────────────────────────────────────┘`

---

# **6. Ví dụ: Swap Adapter**

Giả sử ban đầu dùng **PostgreSQL + Smtp**, sau đó muốn đổi thành **MongoDB + Kafka**.

### **Ban đầu**

Java

`@Configuration
public class ProdConfig {
    
    @Bean
    public OrderRepository orderRepository(OrderJpaRepository jpa) {
        return new PostgreSqlOrderRepository(jpa);  // ← PostgreSQL
    }
    
    @Bean
    public EmailService emailService(JavaMailSender sender) {
        return new SmtpEmailAdapter(sender);  // ← Smtp
    }
    
    @Bean
    public PaymentGateway paymentGateway() {
        return new StripePaymentAdapter(...);  // ← Stripe
    }
    
    @Bean
    public CreateOrderUseCase createOrderUseCase(
        OrderRepository repo,
        EmailService email,
        PaymentGateway payment
    ) {
        return new CreateOrderService(repo, email, payment, ...);
    }
}`

### **Sau khi đổi adapter**

Java

`@Configuration
public class NewProdConfig {
    
    @Bean
    public OrderRepository orderRepository(OrderMongoRepository mongo) {
        return new MongoDbOrderRepository(mongo);  // ← MongoDB (chỉ sửa dòng này!)
    }
    
    @Bean
    public EmailService emailService(KafkaTemplate<String, String> kafka) {
        return new KafkaEmailAdapter(kafka);  // ← Kafka (chỉ sửa dòng này!)
    }
    
    @Bean
    public PaymentGateway paymentGateway() {
        return new StripePaymentAdapter(...);  // ← Stripe (giữ nguyên)
    }
    
    @Bean
    public CreateOrderUseCase createOrderUseCase(
        OrderRepository repo,
        EmailService email,
        PaymentGateway payment
    ) {
        return new CreateOrderService(repo, email, payment, ...);  // ← Code không đổi!
    }
}`

**Insight:**

- **Application service không đổi một dòng code**.
- Chỉ thay adapter ở configuration.
- Port interface vẫn như cũ.

---

# **7. Bảng so sánh: Với và Không có Port & Adapter**

## **❌ Không có Port & Adapter**

Java

`public class OrderService {
    private final OrderJpaRepository jpaRepository;  // Phụ thuộc cụ thể JPA
    private final SmtpEmailService emailService;    // Phụ thuộc cụ thể Smtp
    private final StripeGateway paymentGateway;     // Phụ thuộc cụ thể Stripe
    
    public OrderResponse create(OrderRequest request) {
        // ... business logic ...
        jpaRepository.save(order);  // Hard-code JPA
        emailService.send(...);     // Hard-code Smtp
        paymentGateway.charge(...); // Hard-code Stripe
    }
}

// Vấn đề:
// 1. Không thể test mà không có database, email server, payment gateway
// 2. Muốn đổi database phải sửa OrderService
// 3. Muốn dùng CLI, REST, hoặc message adapter phải sửa OrderService`

## **✅ Có Port & Adapter**

Java

`// Ports
public interface OrderRepository { ... }
public interface EmailService { ... }
public interface PaymentGateway { ... }

// Application (chỉ phụ thuộc interface)
public class OrderService {
    private final OrderRepository repository;  // Phụ thuộc abstraction
    private final EmailService emailService;   // Phụ thuộc abstraction
    private final PaymentGateway paymentGateway;
    
    public OrderResponse create(OrderRequest request) {
        // ... business logic không đổi ...
        repository.save(order);      // Gọi interface, không biết JPA
        emailService.send(...);      // Gọi interface, không biết Smtp
        paymentGateway.charge(...);  // Gọi interface, không biết Stripe
    }
}

// Adapters
public class PostgreSqlOrderRepository implements OrderRepository { ... }
public class MongoDbOrderRepository implements OrderRepository { ... }
public class InMemoryOrderRepository implements OrderRepository { ... }

public class SmtpEmailAdapter implements EmailService { ... }
public class KafkaEmailAdapter implements EmailService { ... }
public class MockEmailAdapter implements EmailService { ... }

// Lợi ích:
// 1. Test dễ: dùng MockEmailAdapter, InMemoryOrderRepository
// 2. Đổi database không sửa OrderService
// 3. Thêm CLI, REST, message adapter không sửa OrderService
// 4. Swap adapter chỉ ở configuration`

---

# **8. Checklist: Bạn đã hiểu Port vs Adapter?**

- [ ]  Port là interface, Adapter là implementation.
- [ ]  Input port định nghĩa những gì hệ thống có thể làm.
- [ ]  Output port định nghĩa những gì hệ thống cần từ bên ngoài.
- [ ]  Có thể có N input adapters nhưng chỉ 1 input port.
- [ ]  Có thể có N output adapters nhưng chỉ 1 output port.
- [ ]  Application phụ thuộc vào port (abstraction), không adapter (implementation).
- [ ]  Có thể swap adapter bằng configuration mà không đổi application code.
- [ ]  Test dễ vì có thể dùng mock/in-memory adapter.
- [ ]  Có thể vẽ sơ đồ port & adapter cho bài toán Order.