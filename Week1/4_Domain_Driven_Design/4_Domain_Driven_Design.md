## **Cần học**

Mentee cần tìm hiểu:

- Entity.
- Value Object.
- Aggregate.
- Aggregate Root.
- Domain Service.
- Domain Event.
- Invariant.
- Ubiquitous Language.

## **Cần hiểu**

Trong bài toán Order:

- `Order` là Entity và Aggregate Root.
- `OrderItem` thuộc về `Order`.
- `Money` hoặc `Price` có thể là Value Object.
- `OrderStatus` biểu diễn trạng thái đơn.
- Business rules nằm trong domain.
- Bên ngoài không được tự ý thay đổi trạng thái hoặc danh sách item.

## **Checklist: Bạn đã hiểu DDD?**

- [ ]  Ubiquitous Language: tên class/method phản ánh domain.
- [ ]  Entity: có identity, mutable, lifecycle.
- [ ]  Value Object: không có identity, immutable, compare by value.
- [ ]  Aggregate: nhóm object, protected by root.
- [ ]  Aggregate Root: entry point, bảo vệ invariant.
- [ ]  Invariant: business rule luôn phải đúng.
- [ ]  Domain Service: logic không thuộc entity/value object.
- [ ]  Repository: persist/retrieve aggregate, interface ở application.
- [ ]  Bounded Context: ranh giới domain.
- [ ]  Domain không import framework.
- [ ]  Business rule nằm trong domain, không ở controller/service.
- [ ]  Có thể viết unit test cho domain mà không cần mock/database.

---

# **Domain-Driven Design**

---

# **1. DDD là gì? (Khái niệm cơ bản)**

## **Định nghĩa đơn giản**

**DDD (Domain-Driven Design) là một cách tư duy về thiết kế phần mềm, tập trung vào "domain" (lĩnh vực kinh doanh thực tế) thay vì công nghệ.**

### **Từ "Domain"**

**Domain** = lĩnh vực kinh doanh, bài toán thực tế mà bạn đang giải quyết.

**Ví dụ domains:**

Code

`Domain: E-Commerce
  - Khách hàng đặt hàng
  - Thanh toán
  - Giao hàng
  - Trả hàng

Domain: Banking
  - Mở tài khoản
  - Chuyển tiền
  - Rút tiền
  - Tính lãi suất

Domain: Hospital
  - Bệnh nhân đăng ký khám
  - Bác sĩ khám bệnh
  - Kê đơn thuốc
  - Thanh toán viện phí`

### **Từ "Driven"**

**"Driven"** = "được điều khiển bởi".

Nghĩa là: **thiết kế phần mềm nên được điều khiển bởi domain, không phải technology.**

### **Kết hợp**

**DDD = Thiết kế phần mềm dựa trên hiểu biết sâu về domain business.**

---

# **2. Vấn đề DDD giải quyết**

## **Vấn đề 1: Code và Business không nói cùng ngôn ngữ**

### **Trước khi dùng DDD**

**Business (Quản lý E-Commerce) nói:**

> "Khách hàng xác nhận đơn hàng, hệ thống kiểm tra tồn kho, nếu hết hàng thì thông báo cho khách."
> 

**Dev code:**

Java

`public class OrderService {
    public void updateOrderStatus(Order order, String newStatus) {
        order.setStatus(newStatus);
        database.update(order);
    }
}`

**Vấn đề:**

- Business nói "xác nhận đơn", dev code "update status".
- Business nói "kiểm tra tồn kho", dev code không thấy logic này ở đâu.
- Business nói "thông báo", dev code không có email logic.
- **Code không phản ánh business logic!**

### **Sau khi dùng DDD**

**Dev code:**

Java

`public class Order {
    public void confirm() {
        if (status != OrderStatus.PENDING) {
            throw new DomainException("Can only confirm PENDING orders");
        }
        this.status = OrderStatus.CONFIRMED;
    }
}

public class CreateOrderService {
    public void create(Order order) {
        order.confirm();
        
        if (!inventoryService.hasStock(order)) {
            throw new OutOfStockException("Not enough inventory");
        }
        
        orderRepository.save(order);
        notificationService.sendConfirmation(order);
    }
}`

**Lợi ích:**

- `order.confirm()` phản ánh business language.
- Tồn kho check, thông báo đều rõ ràng.
- **Code phản ánh business logic!**

---

# **3. DDD vs Cách thiết kế truyền thống**

## **Cách thiết kế truyền thống (Database-Driven)**

Code

`1. Thiết kế Database Schema
   ┌─────────────┐
   │   orders    │
   ├─────────────┤
   │ id          │
   │ status      │
   │ total       │
   │ customer_id │
   └─────────────┘

2. Tạo JPA Entity
   @Entity
   public class Order { ... }

3. Tạo Repository
   public interface OrderRepository { ... }

4. Tạo Service
   @Service
   public class OrderService { ... }

5. Tạo Controller
   @RestController
   public class OrderController { ... }`

**Vấn đề:**

- Bắt đầu từ database, không từ business requirement.
- Service chỉ là CRUD, không có business logic.
- Entity chỉ là container data, không có behavior.

## **Cách DDD**

Code

`1. Hiểu Domain Business
   "Khách hàng đặt hàng, xác nhận, thanh toán, giao hàng"
   "Order có item, status phải hợp lệ"
   "Không thể hủy order đã giao"

2. Thiết kế Domain Model (Entity, Value Object)
   public class Order { ... }  // Entity
   public class Money { ... }  // Value Object
   public class OrderStatus { ... }  // Enum

3. Bảo vệ Business Rule trong Domain
   order.confirm()  // Domain logic
   order.calculateTotal()  // Domain logic

4. Tạo Port (Repository Interface)
   OrderRepository.save(order)  // Abstraction

5. Tạo Adapter (Implement Repository)
   JpaOrderRepository extends OrderRepository

6. Tạo Application Service
   CreateOrderService gọi domain và repository`

**Lợi ích:**

- Bắt đầu từ business requirement.
- Domain model chứa logic, không chỉ data.
- Business rule được bảo vệ và không thể vi phạm.

---

# **4. Các thành phần chính của DDD**

Tại sao DDD có nhiều khái niệm? Vì DDD cần các "tool" để mô phỏng business.

## **Thành phần 1: Ubiquitous Language (Ngôn ngữ chung)**

**Mục đích:** Tất cả team (business, product, dev) nói cùng ngôn ngữ.

Code

`Thay vì:
  Biz nói: "order status"
  Dev hiểu: updateOrderState()

Dùng Ubiquitous Language:
  Biz nói: "confirm order"
  Dev code: order.confirm()
  
  Biz nói: "order must have items"
  Dev code: if (items.isEmpty()) throw exception
  
  Biz nói: "calculate total"
  Dev code: order.calculateTotal()`

## **Thành phần 2: Entity (Thực thể)**

**Mục đích:** Mô phỏng "vật" trong business có identity và lifecycle.

Code

`Ví dụ: Order
- Có ID riêng (123, 456, ...)
- Thay đổi theo thời gian (PENDING → CONFIRMED → SHIPPED)
- Tồn tại dài hạn (lưu database)
- Có behavior (confirm, cancel, ...)`

## **Thành phần 3: Value Object (Đối tượng giá trị)**

**Mục đích:** Mô phỏng "giá trị" không có identity, chỉ quan tâm đến giá trị thực.

Code

`Ví dụ: Money
- Không có ID
- Immutable (100$ luôn là 100$, không bao giờ thay đổi)
- So sánh bằng giá trị (100$ = 100$, không cần kiểm tra ID)
- Có meaning (thay vì double price, dùng Money price)`

## **Thành phần 4: Aggregate**

**Mục đích:** Nhóm entity và value object lại, bảo vệ business rule chung.

Code

`Order Aggregate:
┌──────────────────────────────────┐
│ Order (Aggregate Root)           │
├──────────────────────────────────┤
│ - OrderId (Value Object)         │
│ - OrderItem[] (Child Entities)   │
│   ├ ProductId (Value Object)     │
│   ├ Quantity (Value Object)      │
│   └ Price (Value Object)         │
│ - OrderStatus (Value Object)     │
└──────────────────────────────────┘

Quy tắc:
- Tất cả truy cập đi qua Order (root)
- Không được access OrderItem trực tiếp từ bên ngoài
- Business rule "Order phải có item" được bảo vệ bởi Order`

## **Thành phần 5: Invariant (Quy tắc bất biến)**

**Mục đích:** Định nghĩa business rule LUÔN PHẢI ĐÚNG.

Code

`Order Invariants:
1. "Order phải có ít nhất 1 item"
   → Kiểm tra trong constructor

2. "Quantity phải > 0"
   → Kiểm tra trong OrderItem constructor

3. "Không thể confirm non-PENDING order"
   → Kiểm tra trong order.confirm()

4. "Không thể cancel SHIPPED order"
   → Kiểm tra trong order.cancel()`

**Lợi ích:** Không thể tạo invalid state, business rule tự động được bảo vệ.

## **Thành phần 6: Domain Service**

**Mục đích:** Logic không thuộc entity hay value object nào.

Code

`Ví dụ: Tính giá với discount
- Logic này không phù hợp với Order (Order không biết discount policy)
- Logic này không phù hợp với Money (Money chỉ biết cộng/trừ)
- Cần Domain Service: DiscountPolicy

public class DiscountPolicy {
    public Money applyDiscount(Money total, Customer customer) {
        if (customer.isVIP()) {
            return total.multiply(0.9);  // 10% discount
        }
        return total;
    }
}`

## **Thành phần 7: Repository**

**Mục đích:** Persist/retrieve aggregate từ database.

Code

`Interface (Application Layer):
  public interface OrderRepository {
      void save(Order order);
      Optional<Order> findById(OrderId id);
  }

Implementation (Adapter Layer):
  @Repository
  public class JpaOrderRepository implements OrderRepository {
      // Save to PostgreSQL
      // Or MongoDB, or file system, ...
  }`

---

# **5. Tại sao DDD lại có nhiều khái niệm?**

## **Nguyên nhân**

DDD cần các khái niệm khác nhau để **mô phỏng chính xác domain business.**

### **So sánh với Toán học**

Code

`Toán học có:
- Số (tương tự Entity/Value Object)
- Phép cộng, trừ (tương tự Behavior)
- Hàm (tương tự Domain Service)
- Tập hợp (tương tự Aggregate)

Lý do: Để có thể mô phỏng bất kỳ bài toán nào.`

### **So sánh với ngôn ngữ tự nhiên**

Code

`Tiếng Anh có:
- Noun (danh từ): Order, Customer, Product
- Verb (động từ): confirm, cancel, calculateTotal
- Adjective (tính từ): PENDING, CONFIRMED, SHIPPED
- Preposition (giới từ): "Order contains OrderItem"

Lý do: Để có thể diễn tả bất kỳ ý tưởng nào.`

### **DDD cũng vậy**

Code

`DDD có:
- Entity/Value Object: mô phỏng "vật" trong business
- Behavior (method): mô phỏng "hành động"
- Invariant: mô phỏng "quy tắc"
- Aggregate: mô phỏng "mối quan hệ"
- Domain Service: mô phỏng "process"

Lý do: Để có thể mô phỏng chính xác bất kỳ domain business nào.`

---

# **6. Hành trình học DDD**

## **Mức 1: Hiểu "Domain là gì"**

Bạn cần biết:

- Domain = business problem bạn đang giải quyết.
- Ubiquitous Language = nói cùng ngôn ngữ với business.
- Code phải phản ánh business language.

## **Mức 2: Phân biệt Entity vs Value Object**

Bạn cần biết:

- **Entity:** có identity (ID), thay đổi, tồn tại lâu.
- **Value Object:** không có identity, không thay đổi (immutable), so sánh bằng giá trị.

**Quy tắc dễ:**

- Mọi "vật" trong business (Order, Customer, Product) → Entity.
- Mọi "giá trị" trong business (Money, Date, Address) → Value Object.

## **Mức 3: Aggregate để bảo vệ business rule**

Bạn cần biết:

- Nhóm entity/value object liên quan thành 1 aggregate.
- Aggregate Root là entry point duy nhất.
- Business rule được bảo vệ bởi aggregate root.

**Quy tắc dễ:**

- 1 use case = 1 aggregate được sửa.
- Không modify 2 aggregate trong 1 transaction (tạo complexity).

## **Mức 4: Invariant để tránh invalid state**

Bạn cần biết:

- Invariant = business rule LUÔN phải đúng.
- Kiểm tra invariant trong constructor/method.
- Nếu vi phạm invariant → throw exception.

**Quy tắc dễ:**

- Mỗi constructor/method nhất định phải kiểm tra rule gì?
- Write unit test để verify.

## **Mức 5: Domain Service cho logic phức tạp**

Bạn cần biết:

- Domain Service = logic không thuộc entity nào.
- Stateless (không có state).
- Orchestrate nhiều entity/value object.

**Quy tắc dễ:**

- Nếu logic liên quan đến 2+ entity → Domain Service.
- Nếu logic là rule của domain (không technical) → Domain Service.

---

# **7. Ví dụ thực tế: Order Service**

## **Hiểu Domain**

**Business requirement:**

Code

`1. Khách hàng tạo đơn hàng với 1 hoặc nhiều sản phẩm
2. Tính tổng tiền
3. Khách hàng xác nhận đơn (confirm)
4. Sau khi xác nhận, không thể sửa sản phẩm
5. Khách hàng có thể hủy đơn (cancel)
6. Nếu đơn đã giao (shipped), không thể hủy
7. Tính discount dựa trên customer type`

## **Thiết kế Domain Model**

### **Bước 1: Xác định Entity**

Code

`Entities (có identity, lifecycle):
- Order (id = orderId)
- Customer (id = customerId, ngoài domain này)`

### **Bước 2: Xác định Value Object**

Code

`Value Objects (không identity, immutable):
- OrderId (giá trị của ID đơn hàng)
- Money (giá trị tiền)
- ProductId (giá trị của ID sản phẩm)
- OrderStatus (PENDING, CONFIRMED, CANCELLED, SHIPPED)
- Quantity (số lượng)`

### **Bước 3: Xác định Aggregate**

Code

`Order Aggregate Root:
┌─────────────────────────────────────┐
│ Order                               │
├─────────────────────────────────────┤
│ - id: OrderId                       │
│ - items: List<OrderItem>            │
│ - status: OrderStatus               │
│ - total: Money                      │
│                                     │
│ Methods:                            │
│ + confirm()  → status PENDING→CONF  │
│ + cancel()   → status →CANCELLED    │
│ + ship()     → status CONF→SHIPPED  │
│ + addItem()  → chỉ khi PENDING      │
│ + getTotal() → tính từ items        │
└─────────────────────────────────────┘

OrderItem (Child Entity):
├ - productId: ProductId
├ - quantity: Quantity
├ - price: Money
└ - getSubtotal()`

### **Bước 4: Xác định Invariant**

Code

`Invariant 1: "Order phải có item"
  → kiểm tra: if (items.isEmpty()) throw

Invariant 2: "Quantity phải > 0"
  → kiểm tra: if (quantity <= 0) throw

Invariant 3: "Price phải >= 0"
  → kiểm tra: if (price < 0) throw

Invariant 4: "Trạng thái chuyển hợp lệ"
  PENDING →[confirm]→ CONFIRMED
  CONFIRMED →[ship]→ SHIPPED
  SHIPPED →[deliver]→ DELIVERED
  (PENDING/CONFIRMED) →[cancel]→ CANCELLED
  
Invariant 5: "Không thể sửa item sau confirm"
  → kiểm tra: if (status != PENDING) throw

Invariant 6: "Không thể cancel SHIPPED order"
  → kiểm tra: if (status == SHIPPED) throw`

### **Bước 5: Thiết kế Domain Service**

Code

DiscountPolicy (Domain Service):
- tính discount dựa vào customer type
- không thuộc Order, không thuộc Money
- stateless

```Java
public class DiscountPolicy {
    public Money applyDiscount(Money total, CustomerType type) {
        switch (type) {
            case VIP: return total.multiply(0.9);   // 10% off
            case PREMIUM: return total.multiply(0.95); // 5% off
            default: return total;
        }
    }
}
```

### **Bước 6: Code Domain Model**

Java

```Java
// Value Objects
public class OrderId {
    private final String value;
    public OrderId(String value) {
        if (value == null || value.isEmpty()) 
            throw new DomainException("OrderId empty");
        this.value = value;
    }
    // equals() by value, immutable
}

public class Money {
    private final double amount;
    public Money(double amount) {
        if (amount < 0) 
            throw new DomainException("Money negative");
        this.amount = amount;
    }
    public Money add(Money other) { return new Money(...); }
    // equals() by value, immutable
}

public enum OrderStatus {
    PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED
}

// Entities
public class OrderItem {
    private final ProductId productId;
    private final int quantity;
    private final Money price;
    
    public OrderItem(ProductId productId, int quantity, Money price) {
        if (quantity <= 0) throw new DomainException("Qty <= 0");
        // Invariant checked!
        this.productId = productId;
        this.quantity = quantity;
        this.price = price;
    }
    
    public Money getSubtotal() {
        return price.multiply(quantity);
    }
}

// Aggregate Root
public class Order {
    private final OrderId id;
    private final List<OrderItem> items;
    private OrderStatus status;
    
    public Order(OrderId id, List<OrderItem> items) {
        if (items == null || items.isEmpty()) 
            throw new DomainException("Order must have items"); // Invariant 1
        this.id = id;
        this.items = new ArrayList<>(items);
        this.status = OrderStatus.PENDING;
    }
    
    public void confirm() {
        if (status != OrderStatus.PENDING) 
            throw new DomainException("Can only confirm PENDING"); // Invariant 4
        this.status = OrderStatus.CONFIRMED;
    }
    
    public void cancel() {
        if (status == OrderStatus.SHIPPED) 
            throw new DomainException("Cannot cancel SHIPPED"); // Invariant 6
        this.status = OrderStatus.CANCELLED;
    }
    
    public void addItem(OrderItem item) {
        if (status != OrderStatus.PENDING) 
            throw new DomainException("Cannot add after confirm"); // Invariant 5
        items.add(item);
    }
    
    public Money getTotal() {
        Money total = new Money(0);
        for (OrderItem item : items) {
            total = total.add(item.getSubtotal());
        }
        return total;
    }
}

// Domain Service
public class DiscountPolicy {
    public Money applyDiscount(Money total, CustomerType type) {
        if (type == CustomerType.VIP) {
            return total.multiply(0.9);
        } else if (type == CustomerType.PREMIUM) {
            return total.multiply(0.95);
        }
        return total;
    }
}
```

---

# **8. Tại sao phải học DDD? (Lợi ích)**

| **Lợi ích** | **Chi tiết** |
| --- | --- |
| **Business rule được bảo vệ** | Không thể tạo invalid state. Invariant được check tự động. |
| **Code phản ánh business** | order.confirm() dễ hiểu hơn order.setStatus("CONFIRMED"). |
| **Dễ test** | Test domain logic không cần mock database. |
| **Dễ bảo trì** | Business rule tập trung ở domain, không scattered everywhere. |
| **Dễ mở rộng** | Thêm feature mới chỉ cần update domain, không sửa controller/repository. |
| **Communication tốt** | Team (business, product, dev) cùng ngôn ngữ. |

---

# **9. Tổng kết: DDD là gì?**

## **Simple Version**

Code

```
DDD = Thiết kế code dựa trên HIỂU BIẾT SÃU về business domain.

Thay vì:
  1. Vẽ database schema
  2. Viết entity/repository
  3. Viết service CRUD
  4. Hy vọng business logic đúng

Làm theo DDD:
  1. Hiểu business domain kỹ (Entity, Value Object, Invariant)
  2. Thiết kế domain model (Order, Money, OrderStatus)
  3. Bảo vệ business rule trong domain (throw exception)
  4. Gắn domain model vào adapter (Repository, Service)
  5. Đảm bảo business logic luôn đúng
```

## **Toán tử / Biểu thức**

Code

`DDD = Business Understanding + Domain Modeling + Code
    = Ubiquitous Language + Entity/Value Object + Invariant + Aggregate
    = Rich Domain Model (có logic) + Persistence Abstraction`

## **So sánh**

| **Aspect** | **Truyền thống** | **DDD** |
| --- | --- | --- |
| Bắt đầu từ | Database schema | Business requirement |
| Tôn tại ở | Database | Domain model |
| Logic ở đâu | Controller/Service | Entity/Value Object/Aggregate |
| Test | Cần mock DB | Mock domain object |
| Invalid state | Có thể xảy ra | Không thể xảy ra (throw exception) |