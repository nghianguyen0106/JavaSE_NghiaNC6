# 1 SOLID Principles
## **Cần học**

Mentee cần đọc/xem về:

- Single Responsibility Principle.
- Open/Closed Principle.
- Liskov Substitution Principle.
- Interface Segregation Principle.
- Dependency Inversion Principle.

Tập trung nhiều hơn vào:

- SRP.
- OCP.
- DIP.

Đây là ba nguyên tắc được sử dụng trực tiếp trong assignment.

## **Cần hiểu**

Mentee cần trả lời được:

- Một class có quá nhiều trách nhiệm sẽ gây ra vấn đề gì?
- Business logic khác infrastructure logic như thế nào?
- Vì sao service không nên phụ thuộc trực tiếp vào database implementation?
- Interface giúp code dễ test và dễ thay đổi như thế nào?
- Có phải cứ tạo nhiều interface là code sẽ tốt hơn không?

## **Yêu cầu thực hành**

Cho đoạn code ban đầu:

Java

`public class OrderService {

    public void createOrder(OrderRequest request) {
        // Validate request
        // Calculate total price
        // Save order to database
        // Send email
        // Publish event
    }
}`

Mentee cần:

1. Chỉ ra các vấn đề trong thiết kế hiện tại.
2. Xác định class đang vi phạm nguyên tắc SOLID nào.
3. Refactor thành các component phù hợp.
4. Tạo abstraction cho repository và notification service.
5. Viết unit test cho phần tính giá và validate order.

## **Tiêu chí hoàn thành**

- Không còn một `OrderService` làm tất cả mọi việc.
- Business logic có thể test mà không cần database.
- Có thể thay đổi database hoặc notification service mà không sửa business logic chính.
- Mentee giải thích được lý do tách các class.

---

# **SOLID Principles — Học chi tiết**

## **0. Intro: SOLID là gì?**

**SOLID** là 5 nguyên tắc thiết kế code để nó dễ bảo trì, dễ test, dễ mở rộng.

Ghi nhớ 1 ý chính:

> Code tốt là code dễ thay đổi mà không phá vỡ chức năng cũ.
> 

---

# **1. S — Single Responsibility Principle (SRP)**

## **Định nghĩa**

**Một class chỉ nên có một lý do để thay đổi.**

Nói cách khác:

- Một class chỉ nên có một trách nhiệm.
- Trách nhiệm = lý do để thay đổi.

## **Ví dụ xấu**

Java

`public class OrderService {
    
    // Trách nhiệm 1: Xác thực đơn hàng
    public boolean validateOrder(Order order) {
        if (order.getItems().isEmpty()) return false;
        if (order.getTotal() < 0) return false;
        return true;
    }
    
    // Trách nhiệm 2: Tính giá tiền
    public void calculateTotal(Order order) {
        double total = 0;
        for (OrderItem item : order.getItems()) {
            total += item.getPrice() * item.getQuantity();
        }
        order.setTotal(total);
    }
    
    // Trách nhiệm 3: Lưu database
    public void saveOrder(Order order) {
        // INSERT vào database
    }
    
    // Trách nhiệm 4: Gửi email
    public void sendConfirmationEmail(Order order) {
        // Gửi email
    }
    
    // Trách nhiệm 5: Publish event
    public void publishOrderEvent(Order order) {
        // Publish
    }
}`

### **Vấn đề**

- Class này có **5 lý do để thay đổi**:
    - Nếu rule validate order đổi.
    - Nếu cách tính giá đổi.
    - Nếu database đổi.
    - Nếu cách gửi email đổi.
    - Nếu event broker đổi.
- **Khó test**: để test tính giá, phải setup database.
- **Khó maintain**: sửa validation có thể ảnh hưởng đến code email.
- **Khó reuse**: nếu project khác cần tính giá, phải copy toàn class.

## **Ví dụ tốt**

Tách mỗi trách nhiệm thành class riêng:

Java

```java 
// Trách nhiệm 1: Validate order
public class OrderValidator {
    public boolean validate(Order order) {
        if (order.getItems().isEmpty()) return false;
        if (order.getTotal() < 0) return false;
        return true;
    }
}

// Trách nhiệm 2: Tính giá
public class PricingService {
    public void calculateTotal(Order order) {
        double total = 0;
        for (OrderItem item : order.getItems()) {
            total += item.getPrice() * item.getQuantity();
        }
        order.setTotal(total);
    }
}

// Trách nhiệm 3: Lưu database
public interface OrderRepository {
    void save(Order order);
}

// Trách nhiệm 4: Gửi email
public interface NotificationService {
    void sendEmail(Order order);
}

// Trách nhiệm 5: Publish event
public interface EventPublisher {
    void publish(Event event);
}

// Orchestrate các service
public class CreateOrderUseCase {
    private final OrderValidator validator;
    private final PricingService pricing;
    private final OrderRepository repository;
    private final NotificationService notification;
    private final EventPublisher publisher;
    
    public CreateOrderUseCase(
        OrderValidator validator,
        PricingService pricing,
        OrderRepository repository,
        NotificationService notification,
        EventPublisher publisher
    ) {
        this.validator = validator;
        this.pricing = pricing;
        this.repository = repository;
        this.notification = notification;
        this.publisher = publisher;
    }
    
    public void create(OrderRequest request) {
        Order order = new Order(request);
        
        validator.validate(order); // nếu fail → exception
        pricing.calculateTotal(order);
        repository.save(order);
        notification.sendEmail(order);
        publisher.publish(new OrderCreatedEvent(order));
    }
} 
```

### **Lợi ích**

- **Dễ test**: test `PricingService` không cần database.
- **Dễ bảo trì**: sửa validation không ảnh hưởng email.
- **Dễ reuse**: có thể dùng `PricingService` ở chỗ khác.
- **Dễ thay đổi**: muốn đổi từ email sang SMS, chỉ cần implement interface `NotificationService`.

## **Bài tập**

Có một class `UserService`:

Java

`public class UserService {
    
    public void createUser(String name, String email, String password) {
        // Validate email
        if (!email.contains("@")) {
            throw new Exception("Invalid email");
        }
        
        // Hash password
        String hashedPassword = hashPassword(password);
        
        // Save to database
        User user = new User(name, email, hashedPassword);
        saveToDatabase(user);
        
        // Send welcome email
        sendWelcomeEmail(email);
        
        // Log activity
        logger.info("User created: " + email);
        
        // Update user count
        incrementUserCount();
    }
}`

### **Hãy trả lời**

1. Class này có bao nhiêu trách nhiệm?
2. Nó sẽ cần thay đổi trong những trường hợp nào?
3. Hãy refactor thành các class nhỏ hơn.

---

# **2. O — Open/Closed Principle (OCP)**

## **Định nghĩa**

**Một class nên mở rộng (open for extension) nhưng đóng lại để sửa đổi (closed for modification).**

Nói cách khác:

- Có thể thêm tính năng mới mà không sửa code cũ.
- Sửa lại code cũ sẽ có nguy cơ phá vỡ chức năng cũ.

## **Ví dụ xấu**

Java

`public class DiscountCalculator {
    
    public double calculate(Order order, String customerType) {
        double discount = 0;
        
        if (customerType.equals("REGULAR")) {
            discount = order.getTotal() * 0.05; // 5%
        } else if (customerType.equals("PREMIUM")) {
            discount = order.getTotal() * 0.10; // 10%
        } else if (customerType.equals("VIP")) {
            discount = order.getTotal() * 0.20; // 20%
        }
        
        return discount;
    }
}`

### **Vấn đề**

- Mỗi lần thêm loại customer mới (ví dụ `CORPORATE`), phải sửa class `DiscountCalculator`.
- Sửa code cũ = rủi ro test lại toàn bộ logic cũ.

## **Ví dụ tốt**

Dùng **polymorphism** (interface/abstract class):

Java

```java
// Abstraction
public interface DiscountPolicy {
    double calculate(Order order);
}

// Implementation cho mỗi loại
public class RegularCustomerDiscount implements DiscountPolicy {
    @Override
    public double calculate(Order order) {
        return order.getTotal() * 0.05;
    }
}

public class PremiumCustomerDiscount implements DiscountPolicy {
    @Override
    public double calculate(Order order) {
        return order.getTotal() * 0.10;
    }
}

public class VIPCustomerDiscount implements DiscountPolicy {
    @Override
    public double calculate(Order order) {
        return order.getTotal() * 0.20;
    }
}

// Dùng interface, không quan tâm implementation cụ thể
public class DiscountCalculator {
    private final DiscountPolicy discountPolicy;
    
    public DiscountCalculator(DiscountPolicy discountPolicy) {
        this.discountPolicy = discountPolicy;
    }
    
    public double calculate(Order order) {
        return discountPolicy.calculate(order);
    }
}
```

### **Lợi ích**

- Thêm loại customer mới (ví dụ `CorporateCustomerDiscount`) không cần sửa `DiscountCalculator`.
- Code cũ (`DiscountCalculator`) không bị thay đổi → ít rủi ro.

### **Cách dùng**

Java

`// Regular customer
DiscountPolicy policy = new RegularCustomerDiscount();
DiscountCalculator calculator = new DiscountCalculator(policy);
double discount = calculator.calculate(order);

// Nếu thêm VIP
DiscountPolicy vipPolicy = new VIPCustomerDiscount();
DiscountCalculator vipCalculator = new DiscountCalculator(vipPolicy);
double vipDiscount = vipCalculator.calculate(order);`

## **Bài tập**

Có class `ReportGenerator`:

Java

`public class ReportGenerator {
    
    public void generate(String format, List<OrderData> data) {
        if (format.equals("PDF")) {
            // Code PDF
        } else if (format.equals("EXCEL")) {
            // Code Excel
        } else if (format.equals("JSON")) {
            // Code JSON
        }
    }
}`

### **Hãy trả lời**

1. Lỡ thêm định dạng mới `CSV` thì cần sửa gì?
2. Hãy refactor dùng interface.
3. Lợi ích của thiết kế mới là gì?

---

# **3. L — Liskov Substitution Principle (LSP)**

## **Định nghĩa**

**Subtypes phải có thể thay thế được base type mà không phá vỡ hành vi của chương trình.**

Nói cách khác:

- Nếu class `A` là subtype của class `B`, thì có thể dùng `A` ở chỗ cần `B` mà không gây lỗi.

## **Ví dụ xấu**

Java

`public class Bird {
    public void fly() {
        System.out.println("Flying...");
    }
}

public class Sparrow extends Bird {
    // Sparrow có thể bay
}

public class Penguin extends Bird {
    // Penguin không thể bay!
    @Override
    public void fly() {
        throw new UnsupportedOperationException("Penguins cannot fly");
    }
}`

### **Vấn đề**

Java

`public class BirdHandler {
    public void makeBirdFly(Bird bird) {
        bird.fly(); // Sẽ crash nếu bird là Penguin
    }
}

// Dùng
Bird bird = new Penguin();
BirdHandler handler = new BirdHandler();
handler.makeBirdFly(bird); // ❌ Exception!`

`Penguin` không thể thay thế `Bird` → vi phạm LSP.

## **Ví dụ tốt**

Tách hai interface:

Java

`public interface Bird {
    // Chỉ những hành vi chung
    void eat();
    void sleep();
}

public interface FlyingBird extends Bird {
    void fly();
}

public class Sparrow implements FlyingBird {
    @Override
    public void fly() {
        System.out.println("Sparrow flying...");
    }
    
    @Override
    public void eat() { ... }
    
    @Override
    public void sleep() { ... }
}

public class Penguin implements Bird {
    // Không có fly() vì penguin không bay
    
    @Override
    public void eat() { ... }
    
    @Override
    public void sleep() { ... }
}`

### **Dùng**

Java

`public class BirdHandler {
    public void makeBirdFly(FlyingBird bird) {
        bird.fly(); // Chỉ nhận FlyingBird
    }
    
    public void feedBird(Bird bird) {
        bird.eat(); // Nhận bất kỳ Bird nào
    }
}`

## **Ví dụ khác: Payment**

Java

`// Xấu
public abstract class PaymentProcessor {
    public abstract void process(double amount);
    public abstract void refund(double amount);
}

public class CreditCardProcessor extends PaymentProcessor {
    @Override
    public void process(double amount) { /* ... */ }
    
    @Override
    public void refund(double amount) { /* ... */ }
}

public class CashProcessor extends PaymentProcessor {
    @Override
    public void process(double amount) { /* ... */ }
    
    @Override
    public void refund(double amount) {
        throw new UnsupportedOperationException("Cash cannot be refunded online");
    }
}`

❌ `CashProcessor` không thể thay thế `PaymentProcessor`.

Java

`// Tốt
public interface PaymentProcessor {
    void process(double amount);
}

public interface Refundable {
    void refund(double amount);
}

public class CreditCardProcessor implements PaymentProcessor, Refundable {
    @Override
    public void process(double amount) { /* ... */ }
    
    @Override
    public void refund(double amount) { /* ... */ }
}

public class CashProcessor implements PaymentProcessor {
    @Override
    public void process(double amount) { /* ... */ }
    // Không có refund
}`

## **Bài tập**

Java

`public abstract class Vehicle {
    public abstract void startEngine();
    public abstract void accelerate();
    public abstract void chargeBattery();
}

public class Car extends Vehicle {
    @Override
    public void startEngine() { /* Petrol engine */ }
    
    @Override
    public void accelerate() { /* ... */ }
    
    @Override
    public void chargeBattery() {
        throw new UnsupportedOperationException("Car has no battery");
    }
}

public class ElectricCar extends Vehicle {
    @Override
    public void startEngine() {
        throw new UnsupportedOperationException("Electric car has no gas engine");
    }
    
    @Override
    public void accelerate() { /* ... */ }
    
    @Override
    public void chargeBattery() { /* ... */ }
}`

### **Hãy trả lời**

1. Class này vi phạm LSP ở đâu?
2. Hãy refactor bằng interface.
3. Giải thích lợi ích của thiết kế mới.

---

# **4. I — Interface Segregation Principle (ISP)**

## **Định nghĩa**

**Clients không nên bị ép phụ thuộc vào interface mà nó không sử dụng.**

Nói cách khác:

- Interface nên nhỏ, cụ thể, không "fat".
- Một class chỉ implement những gì nó thực sự cần.

## **Ví dụ xấu**

Java

`public interface Worker {
    void work();
    void eat();
    void sleep();
    void drive();
    void swim();
}

public class Developer implements Worker {
    @Override
    public void work() { /* code */ }
    
    @Override
    public void eat() { /* ... */ }
    
    @Override
    public void sleep() { /* ... */ }
    
    @Override
    public void drive() {
        throw new UnsupportedOperationException("Developer doesn't drive");
    }
    
    @Override
    public void swim() {
        throw new UnsupportedOperationException("Developer doesn't swim");
    }
}

public class Swimmer implements Worker {
    @Override
    public void work() {
        throw new UnsupportedOperationException("Swimmer doesn't work");
    }
    
    @Override
    public void eat() { /* ... */ }
    
    @Override
    public void sleep() { /* ... */ }
    
    @Override
    public void drive() {
        throw new UnsupportedOperationException("Swimmer doesn't drive");
    }
    
    @Override
    public void swim() { /* ... */ }
}`

### **Vấn đề**

- `Developer` bị ép implement `drive()` và `swim()` mà không cần.
- `Swimmer` bị ép implement `work()` và `drive()` mà không cần.
- Interface quá "fat", chứa những method không liên quan.

## **Ví dụ tốt**

Tách interface thành các phần nhỏ:

Java

`public interface Worker {
    void work();
}

public interface Eater {
    void eat();
}

public interface Sleeper {
    void sleep();
}

public interface Driver {
    void drive();
}

public interface Swimmer {
    void swim();
}

// Developer chỉ implement những gì cần
public class Developer implements Worker, Eater, Sleeper {
    @Override
    public void work() { /* code */ }
    
    @Override
    public void eat() { /* ... */ }
    
    @Override
    public void sleep() { /* ... */ }
}

// Swimmer chỉ implement những gì cần
public class NatationAthlete implements Swimmer, Eater, Sleeper {
    @Override
    public void swim() { /* ... */ }
    
    @Override
    public void eat() { /* ... */ }
    
    @Override
    public void sleep() { /* ... */ }
}`

## **Ví dụ khác: Repository**

Java

```Java
// Xấu: interface "fat"
public interface Repository<T> {
    void create(T entity);
    T read(String id);
    void update(T entity);
    void delete(String id);
    List<T> findAll();
    List<T> search(String criteria);
    void bulkInsert(List<T> entities);
    void bulkUpdate(List<T> entities);
    void bulkDelete(List<String> ids);
}

// Tốt: interface nhỏ, cụ thể
public interface ReadOnlyRepository<T> {
    T findById(String id);
    List<T> findAll();
}

public interface WriteRepository<T> {
    void save(T entity);
    void delete(String id);
}

public interface SearchRepository<T> {
    List<T> search(String criteria);
}

// Client chỉ dùng những interface cần
public class OrderService {
    private final ReadOnlyRepository<Order> reader;
    private final WriteRepository<Order> writer;
    
    public OrderService(
        ReadOnlyRepository<Order> reader,
        WriteRepository<Order> writer
    ) {
        this.reader = reader;
        this.writer = writer;
    }
}
```

## **Bài tập**

Java

```Java
public interface Animal {
    void eat();
    void run();
    void fly();
    void swim();
}

public class Dog implements Animal {
    @Override
    public void eat() { /* ... */ }
    
    @Override
    public void run() { /* ... */ }
    
    @Override
    public void fly() {
        throw new UnsupportedOperationException("Dog cannot fly");
    }
    
    @Override
    public void swim() { /* ... */ }
}

public class Bird implements Animal {
    @Override
    public void eat() { /* ... */ }
    
    @Override
    public void run() {
        throw new UnsupportedOperationException("Bird cannot run");
    }
    
    @Override
    public void fly() { /* ... */ }
    
    @Override
    public void swim() {
        throw new UnsupportedOperationException("Bird cannot swim");
    }
}
```

### **Hãy trả lời**

1. Interface này vi phạm ISP ở đâu?
2. Hãy tách thành các interface nhỏ hơn.
3. Giải thích lợi ích.

---

# **5. D — Dependency Inversion Principle (DIP)**

## **Định nghĩa**

**High-level modules không nên phụ thuộc vào low-level modules. Cả hai nên phụ thuộc vào abstraction.**

Nói cách khác:

- Business logic (high-level) không nên phụ thuộc trực tiếp vào database (low-level).
- Cả hai nên phụ thuộc vào interface (abstraction).

## **Ví dụ xấu**

Java

```Java
// Low-level: database
public class MySQLDatabase {
    public void save(Order order) {
        // Lưu vào MySQL
    }
    
    public Order findById(String id) {
        // Lấy từ MySQL
    }
}

// High-level: business logic
public class OrderService {
    private MySQLDatabase database; // ❌ Phụ thuộc trực tiếp vào MySQL
    
    public OrderService() {
        this.database = new MySQLDatabase();
    }
    
    public void createOrder(OrderRequest request) {
        Order order = new Order(request);
        database.save(order); // Phụ thuộc MySQL
    }
}
```

### **Vấn đề**

- `OrderService` bị ràng buộc với `MySQLDatabase`.
- Muốn đổi sang PostgreSQL, phải sửa `OrderService`.
- Không thể test `OrderService` mà không có MySQL.

## **Ví dụ tốt**

Dùng **abstraction (interface)**:

Java

```Java
// Abstraction
public interface OrderRepository {
    void save(Order order);
    Order findById(String id);
}

// Low-level: MySQL implementation
public class MySQLOrderRepository implements OrderRepository {
    @Override
    public void save(Order order) {
        // Lưu vào MySQL
    }
    
    @Override
    public Order findById(String id) {
        // Lấy từ MySQL
    }
}

// Low-level: PostgreSQL implementation
public class PostgreSQLOrderRepository implements OrderRepository {
    @Override
    public void save(Order order) {
        // Lưu vào PostgreSQL
    }
    
    @Override
    public Order findById(String id) {
        // Lấy từ PostgreSQL
    }
}

// High-level: business logic
public class OrderService {
    private final OrderRepository repository; // ✅ Phụ thuộc vào interface
    
    public OrderService(OrderRepository repository) {
        this.repository = repository;
    }
    
    public void createOrder(OrderRequest request) {
        Order order = new Order(request);
        repository.save(order); // Không biết cách implement cụ thể
    }
}`

### **Lợi ích**

- `OrderService` không biết database là gì.
- Dùng MySQL hoặc PostgreSQL, code `OrderService` không đổi.
- Test dễ: dùng in-memory repository.

Java

```Java
// Test
public class InMemoryOrderRepository implements OrderRepository {
    private Map<String, Order> storage = new HashMap<>();
    
    @Override
    public void save(Order order) {
        storage.put(order.getId(), order);
    }
    
    @Override
    public Order findById(String id) {
        return storage.get(id);
    }
}

// Dùng
OrderRepository testRepo = new InMemoryOrderRepository();
OrderService service = new OrderService(testRepo);
service.createOrder(...); // ✅ Chạy mà không cần database thật`
```
## **Ví dụ: Notification**

Java

```Java
// Xấu
public class User {
    public void register(String email) {
        // ... validate ...
        
        // Hard-code gửi email
        EmailService emailService = new EmailService();
        emailService.send(email, "Welcome!");
    }
}

// Tốt
public interface NotificationService {
    void send(String email, String message);
}

public class EmailNotificationService implements NotificationService {
    @Override
    public void send(String email, String message) {
        // Gửi email
    }
}

public class SMSNotificationService implements NotificationService {
    @Override
    public void send(String email, String message) {
        // Gửi SMS
    }
}

public class User {
    private final NotificationService notificationService;
    
    public User(NotificationService notificationService) {
        this.notificationService = notificationService;
    }
    
    public void register(String email) {
        // ... validate ...
        notificationService.send(email, "Welcome!");
    }
}

// Dùng
NotificationService emailService = new EmailNotificationService();
User user = new User(emailService);
user.register("test@example.com");

// Hoặc SMS
NotificationService smsService = new SMSNotificationService();
User user2 = new User(smsService);
user2.register("test@example.com");`
```

## **Bài tập**

Java

```Java
public class PaymentProcessor {
    public void process(double amount) {
        // Hard-code gọi Stripe API
        StripeAPI stripe = new StripeAPI();
        stripe.charge(amount);
    }
}

public class StripeAPI {
    public void charge(double amount) {
        // Call Stripe
    }
}
```

### **Hãy trả lời**

1. Nếu muốn đổi sang PayPal thì cần sửa gì?
2. Hãy refactor dùng interface.
3. Cách test mà không cần Stripe/PayPal.

---

# **TỔNG KẾT SOLID**

| **Nguyên tắc** | **Ý chính** |
| --- | --- |
| **S** | Một class một trách nhiệm |
| **O** | Mở rộng bằng class mới, không sửa class cũ |
| **L** | Subtype phải thay thế được base type |
| **I** | Interface nhỏ, không "fat" |
| **D** | Phụ thuộc vào abstraction, không direct implementation |

## **Cách nhớ nhanh**

Java

`// SOLID tốt
public class OrderService {
    private final OrderValidator validator;
    private final PricingService pricing;
    private final OrderRepository repository;
    private final NotificationService notification;
    
    public OrderService(
        OrderValidator validator,
        PricingService pricing,
        OrderRepository repository,
        NotificationService notification
    ) {
        this.validator = validator;
        this.pricing = pricing;
        this.repository = repository;
        this.notification = notification;
    }
    
    public void create(OrderRequest request) {
        Order order = new Order(request);
        validator.validate(order);
        pricing.calculateTotal(order);
        repository.save(order);
        notification.notify(order);
    }
}

// Từng class có một trách nhiệm (S)
// Có thể mở rộng thêm validator mới (O)
// Các implementation có thể thay thế (L)
// Interface nhỏ, cụ thể (I)
// Phụ thuộc vào interface, không direct class (D)`

---

# **Bài tập**

Cho đoạn code sau, hãy:

1. Chỉ ra vi phạm SOLID ở đâu.
2. Refactor để tuân thủ SOLID.
3. Giải thích từng quyết định.

Java

`public class PaymentService {
    
    public void processPayment(Order order, String paymentMethod) {
        // Validate
        if (order == null || order.getTotal() <= 0) {
            throw new Exception("Invalid order");
        }
        
        // Process payment
        double amount = order.getTotal();
        
        if (paymentMethod.equals("CREDIT_CARD")) {
            // Call Stripe API
            StripeAPI stripe = new StripeAPI();
            stripe.charge(amount);
        } else if (paymentMethod.equals("PAYPAL")) {
            // Call PayPal API
            PayPalAPI paypal = new PayPalAPI();
            paypal.pay(amount);
        } else if (paymentMethod.equals("BANK")) {
            // Call Bank API
            BankAPI bank = new BankAPI();
            bank.transfer(amount);
        }
        
        // Save to database
        String query = "INSERT INTO payments (amount, method) VALUES (?, ?)";
        Connection conn = DriverManager.getConnection("jdbc:mysql://localhost/db");
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setDouble(1, amount);
        stmt.setString(2, paymentMethod);
        stmt.executeUpdate();
        
        // Send email
        String emailContent = "Payment of " + amount + " processed";
        SMTP smtp = new SMTP("smtp.gmail.com", 587, "user@gmail.com", "password");
        smtp.send("customer@example.com", "Payment Confirmation", emailContent);
        
        // Log
        System.out.println("Payment processed for order " + order.getId());
    }
}`

## **Sản phẩm cần nộp**

- Source code trước và sau khi refactor.
- File `notes.md`, gồm:
    - Các vấn đề của code ban đầu.
    - Các nguyên tắc SOLID đã áp dụng.
    - Lý do chọn thiết kế mới.