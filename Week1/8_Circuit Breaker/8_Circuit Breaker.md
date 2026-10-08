# 8. Circuit Breaker
## **Cần học**

Mentee cần tìm hiểu:

- Cascading failure.
- Timeout.
- Retry.
- Circuit Breaker.
- Trạng thái Closed.
- Trạng thái Open.
- Trạng thái Half-open.
- Fallback.

## **Cần hiểu**

Text

`Closed
  ↓ lỗi nhiều
Open
  ↓ hết thời gian chờ
Half-open
  ↓ request thành công
Closed`

Circuit Breaker giúp ngăn việc tiếp tục gọi một service đang lỗi, tránh làm sự cố lan rộng.

## **Yêu cầu thực hành**

Mentee cần mô phỏng payment service:

- Payment service phản hồi thành công.
- Payment service timeout.
- Payment service trả lỗi liên tục.
- Circuit chuyển sang Open.
- Sau một khoảng thời gian, thử lại ở Half-open.
- Nếu thành công, đóng circuit.

## **Sản phẩm cần nộp**

- State diagram.
- Code mô phỏng hoặc demo dùng thư viện phù hợp.
- Cấu hình:
    - Failure threshold.
    - Timeout.
    - Retry count.
    - Fallback behavior.
- Phân tích sự khác nhau giữa retry và circuit breaker.

## **Tiêu chí hoàn thành**

Mentee giải thích được:

- Retry dùng để thử lại request.
- Circuit Breaker dùng để ngừng gọi dependency đang lỗi.
- Retry sai cách có thể làm hệ thống quá tải hơn.
- Fallback cần trả về hành vi có ý nghĩa cho người dùng hoặc hệ thống.

---

# **Circuit Breaker Pattern — Học chi tiết**

---

# **0. Intro: Circuit Breaker là gì?**

## **Định nghĩa đơn giản**

**Circuit Breaker** là một pattern ngừng gọi một dependency (service) đang bị lỗi để tránh lãng phí tài nguyên và tạo cascading failure.

## **Từ "Circuit Breaker"**

**Circuit Breaker** (trong điện):

- Bình thường: điều khoản cho dòng điện chạy.
- Khi quá tải: ngắt mạch (mở circuit) để bảo vệ.
- Sau đó: tự động đóng lại khi an toàn.

## **Vấn đề Circuit Breaker giải quyết**

### **Scenario: Cascading Failure**

Code

`User gửi request:
Request → Order Service
       → Payment Service (timeout)
       → Retry, retry, retry (10s)
       → Still timeout
       → Retry, retry, retry (10s)
       → Still timeout
       
Kết quả:
- Order Service bị chặn 30s
- Resources (connection pool, thread) bị exhaust
- Order Service không thể process request khác
- Tất cả request đến Order Service fail
- Cascading failure! 💥`

### **Vấn đề**

Code

`Timeline:

10:00:00 - Payment Service down
10:00:01 - Order Service gọi Payment, timeout (1s)
10:00:02 - Retry, timeout (1s)
10:00:03 - Retry, timeout (1s)
...
10:00:10 - 10 timeouts, Order Service resources exhaust
10:00:11 - New customer request → Order Service cannot process
10:00:12 - Cascading failure spreads to other services

Solution: STOP gọi Payment Service sớm!`

---

# **1. Cascading Failure — Chi tiết**

## **Định nghĩa**

**Cascading Failure** là một sự cố trong một service gây ra sự cố dây chuyền tới các service khác.

## **Ví dụ thực tế**

Code

```
┌─────────────────┐
│ Payment Service │  ← OUT OF MEMORY
└────────┬────────┘
         ↓
   Can't process
   requests
         ↓
   Return errors
         ↓
┌─────────────────────────────────────┐
│ Order Service                       │
│ ├─ getOrderHandler calls payment    │
│ ├─ getOrderHandler calls payment    │
│ ├─ getOrderHandler calls payment    │  ← Retrying
│ ├─ getOrderHandler calls payment    │     desperately
│ └─ ... (100s of concurrent requests)
└─────────────────────────────────────┘
         ↓
   Thread pool exhausted
   Connection pool exhausted
         ↓
┌──────────────────┐
│ Customer Service │
│ getCustomer() →  │  ← Can't even talk to Order Service
│ Order Service    │     because it's dead!
│ (timeout)        │
└──────────────────┘
         ↓
   Cascading failure
   propagates further
```

## **Timeline**

Code

`Payment Service fails:
10:00:00 - Payment service runs out of memory

Order Service keeps retrying:
10:00:01 - Retry #1 to Payment
10:00:02 - Retry #2 to Payment
10:00:03 - Retry #3 to Payment
... (wasting thread/connection resources)

Order Service becomes slow:
10:00:10 - Order Service now has only 5 free threads (out of 200)

Customer Service is affected:
10:00:11 - Customer trying to list orders
         → Call Order Service
         → Order Service has no free threads
         → Customer request times out

Even when Payment Service recovers:
10:00:15 - Payment Service comes back online
10:00:16 - But Order Service is still busy with old requests
10:00:20 - Order Service slowly recovers

Result: 20s downtime instead of 5s downtime!`

---

# **2. Circuit Breaker States**

## **Định nghĩa từng state**

### **State 1: CLOSED (Normal)**

Text

```
Status: ✓ OK
Behavior: Pass requests through
Flow:
  Request → Circuit Breaker (CLOSED) → Service
                                           ↓
                                         OK
                                           ↓
                                    Return response
                                           ↓
                                      ✓ Success
```

**Ví dụ:**

Java

```Java
// Payment Service is healthy
CircuitBreaker breaker = new CircuitBreaker();
breaker.setState(State.CLOSED);

// All requests pass through
try {
    response = breaker.execute(() -> paymentService.charge(100));
    // ✓ Success: response received
} catch (Exception e) {
    // If error: increment failure counter
}
```

### **State 2: OPEN (Blocked)**

Text

```
Status: ✗ Service failing
Behavior: Reject all requests immediately
Flow:
  Request → Circuit Breaker (OPEN)
                  ↓
            Reject immediately
            (no call to service)
                  ↓
            Throw CircuitBreakerOpenException
```

**Ví dụ:**

Java

```Java
CircuitBreaker breaker = new CircuitBreaker();
breaker.setState(State.OPEN);

try {
    response = breaker.execute(() -> paymentService.charge(100));
} catch (CircuitBreakerOpenException e) {
    // ✗ Rejected immediately
    // Service was called 0 times (saved resources!)
    logger.warn("Circuit is OPEN, payment service unreachable");
}
```

**Khi chuyển sang OPEN:**

- Failure count >= threshold (ví dụ: 5 failures).
- Hoặc slow request count >= threshold.

### **State 3: HALF-OPEN (Testing)**

Text

```
Status: ? Testing if service recovered
Behavior: Allow limited test requests
Flow:
  Request → Circuit Breaker (HALF-OPEN)
                  ↓
         Allow 1 test request
                  ↓
              Call service
                  ↓
         Success → Go to CLOSED
         Failure → Go back to OPEN
```

**Ví dụ:**

Java

```Java
CircuitBreaker breaker = new CircuitBreaker();
breaker.setState(State.HALF_OPEN);

// Circuit breaker allows 1 test request
try {
    response = breaker.execute(() -> paymentService.charge(100));
    
    if (success) {
        breaker.setState(State.CLOSED);  // Service recovered!
        logger.info("Circuit is now CLOSED, service healthy");
    }
} catch (Exception e) {
    breaker.setState(State.OPEN);  // Service still failing
    logger.warn("Circuit is OPEN again, service still unhealthy");
}
```

## **State Transition Diagram**

Text

```
        ┌──────────────────┐
        │     CLOSED       │
        │   ✓ OK, normal   │
        └────────┬─────────┘
                 │
          Failure count >= threshold
          (e.g., 5 failures)
                 │
                 ↓
        ┌──────────────────┐
        │     OPEN         │
        │ ✗ Reject all     │
        └────────┬─────────┘
                 │
         Timeout duration elapsed
         (e.g., 30s)
                 │
                 ↓
        ┌──────────────────┐
        │   HALF-OPEN      │
        │ ? Allow 1 test   │
        └────────┬─────────┘
                 │
       ┌─────────┴─────────┐
       │                   │
     Success            Failure
       │                   │
       ↓                   ↓
    CLOSED              OPEN
```

---

# **3. Circuit Breaker Configuration**

## **Các tham số chính**

### **1. Failure Threshold**

**Định nghĩa:** Số lỗi liên tiếp trước khi mở circuit.

Java

```Java
// ============================================
// FAILURE THRESHOLD
// ============================================

public class CircuitBreakerConfig {
    // Mở circuit sau 5 lỗi liên tiếp
    private static final int FAILURE_THRESHOLD = 5;
    
    // Mở circuit nếu 50% request fail
    private static final double FAILURE_RATE_THRESHOLD = 0.5;  // 50%
}

// ============================================
// IMPLEMENTATION
// ============================================

public class CircuitBreaker {
    private int failureCount = 0;
    private int successCount = 0;
    private State state = State.CLOSED;
    
    public void recordSuccess() {
        failureCount = 0;  // Reset failure counter
        successCount++;
    }
    
    public void recordFailure() {
        failureCount++;
        
        if (failureCount >= FAILURE_THRESHOLD) {
            state = State.OPEN;
            logger.warn("Circuit opened after " + failureCount + " failures");
        }
    }
}

// Usage:
CircuitBreaker breaker = new CircuitBreaker();

try {
    paymentService.charge(100);
    breaker.recordSuccess();  // ✓
} catch (Exception e) {
    breaker.recordFailure();  // ✗ (count = 1)
}

// After 5 failures: circuit is OPEN
// All subsequent requests fail immediately
```

### **2. Timeout Duration**

**Định nghĩa:** Thời gian chờ trước khi chuyển từ OPEN sang HALF-OPEN.

Java

```Java
// ============================================
// TIMEOUT DURATION
// ============================================

public class CircuitBreakerConfig {
    // Chuyển sang HALF-OPEN sau 30s
    private static final int TIMEOUT_DURATION_SECONDS = 30;
}

// ============================================
// IMPLEMENTATION
// ============================================

public class CircuitBreaker {
    private State state = State.CLOSED;
    private Instant openedAt = null;
    
    public void execute(Callable<Response> request) {
        if (state == State.OPEN) {
            // Check nếu timeout expired
            Instant now = Instant.now();
            long elapsedSeconds = Duration.between(openedAt, now).getSeconds();
            
            if (elapsedSeconds >= TIMEOUT_DURATION_SECONDS) {
                // Timeout expired, try to recover
                state = State.HALF_OPEN;
                logger.info("Circuit transitioned to HALF-OPEN");
            } else {
                // Still in timeout period, reject
                throw new CircuitBreakerOpenException(
                    "Circuit is OPEN, will retry in " + 
                    (TIMEOUT_DURATION_SECONDS - elapsedSeconds) + "s"
                );
            }
        }
        
        // Rest of execution
    }
}

// Example:
Instant now = 10:00:30

10:00:00 - Circuit opens (OPEN)
10:00:01 - Request → Rejected
10:00:15 - Request → Rejected
10:00:30 - Timeout expired (30s)
10:00:30 - Circuit transitions to HALF-OPEN
10:00:31 - First request allowed (test request)
10:00:31 - Success → Circuit closes (CLOSED)
```

### **3. Success Threshold (in HALF-OPEN)**

**Định nghĩa:** Số request thành công cần để đóng circuit.

Java

```Java
// ============================================
// SUCCESS THRESHOLD
// ============================================

public class CircuitBreakerConfig {
    // Cần 2 successful requests liên tiếp để close circuit
    private static final int SUCCESS_THRESHOLD = 2;
}

// ============================================
// IMPLEMENTATION
// ============================================

public class CircuitBreaker {
    private int successCountInHalfOpen = 0;
    
    public void execute(Callable<Response> request) {
        if (state == State.HALF_OPEN) {
            try {
                Response response = request.call();
                
                successCountInHalfOpen++;
                
                if (successCountInHalfOpen >= SUCCESS_THRESHOLD) {
                    // Recovered!
                    state = State.CLOSED;
                    successCountInHalfOpen = 0;
                    logger.info("Circuit closed, service recovered");
                }
                
                return response;
            } catch (Exception e) {
                // Still failing
                state = State.OPEN;
                openedAt = Instant.now();
                successCountInHalfOpen = 0;
                logger.warn("Circuit reopened, service still failing");
                throw e;
            }
        }
    }
}

// Example:
Timeout expired, HALF-OPEN:
Request 1: Success (count = 1) → HALF-OPEN
Request 2: Success (count = 2) → COUNT >= THRESHOLD
           → Circuit closes → CLOSED

OR

Request 1: Success (count = 1) → HALF-OPEN
Request 2: Failure (count = 0) → Circuit reopens → OPEN
```

---

# **4. Complete Circuit Breaker Implementation**

Java

```Java
// ============================================
// CIRCUIT BREAKER CLASS
// ============================================

public class CircuitBreaker {
    private final static Logger logger = LoggerFactory.getLogger(CircuitBreaker.class);
    
    // Configuration
    private final int failureThreshold;           // 5 failures → open
    private final int successThreshold;           // 2 successes in HALF_OPEN → close
    private final int timeoutDurationSeconds;     // 30s before HALF_OPEN
    
    // State
    private State state = State.CLOSED;
    private int failureCount = 0;
    private int successCount = 0;
    private Instant openedAt = null;
    
    public CircuitBreaker(int failureThreshold, int successThreshold, int timeoutDurationSeconds) {
        this.failureThreshold = failureThreshold;
        this.successThreshold = successThreshold;
        this.timeoutDurationSeconds = timeoutDurationSeconds;
    }
    
    // ========== PUBLIC METHOD ==========
    
    public <T> T execute(Callable<T> request) throws Exception {
        // Check if circuit should transition to HALF_OPEN
        if (state == State.OPEN) {
            checkHalfOpenTransition();
        }
        
        // If circuit is OPEN, reject immediately
        if (state == State.OPEN) {
            throw new CircuitBreakerOpenException(
                "Circuit breaker is OPEN. Service is unavailable."
            );
        }
        
        // Try to execute request
        try {
            T response = request.call();
            recordSuccess();
            return response;
        } catch (Exception e) {
            recordFailure();
            throw e;
        }
    }
    
    // ========== PRIVATE METHODS ==========
    
    private void checkHalfOpenTransition() {
        if (openedAt == null) return;
        
        Instant now = Instant.now();
        long elapsedSeconds = Duration.between(openedAt, now).getSeconds();
        
        if (elapsedSeconds >= timeoutDurationSeconds) {
            state = State.HALF_OPEN;
            successCount = 0;  // Reset success count for HALF_OPEN phase
            logger.info("Circuit transitioned from OPEN to HALF_OPEN. Testing service...");
        }
    }
    
    private void recordSuccess() {
        if (state == State.HALF_OPEN) {
            successCount++;
            logger.debug("Success in HALF_OPEN state (count: " + successCount + ")");
            
            if (successCount >= successThreshold) {
                state = State.CLOSED;
                failureCount = 0;
                successCount = 0;
                logger.info("Circuit breaker CLOSED. Service is healthy again.");
            }
        } else if (state == State.CLOSED) {
            // In CLOSED state, reset failure counter on success
            failureCount = 0;
            logger.debug("Request successful in CLOSED state");
        }
    }
    
    private void recordFailure() {
        if (state == State.HALF_OPEN) {
            // Any failure in HALF_OPEN → go back to OPEN
            state = State.OPEN;
            openedAt = Instant.now();
            successCount = 0;
            logger.warn("Service failed during HALF_OPEN test. Circuit reopened.");
            
        } else if (state == State.CLOSED) {
            failureCount++;
            logger.debug("Failure in CLOSED state (count: " + failureCount + ")");
            
            if (failureCount >= failureThreshold) {
                state = State.OPEN;
                openedAt = Instant.now();
                logger.warn("Circuit OPENED after " + failureCount + " failures.");
            }
        }
    }
    
    // ========== GETTERS ==========
    
    public State getState() { return state; }
    public int getFailureCount() { return failureCount; }
    public int getSuccessCount() { return successCount; }
}

// ============================================
// STATE ENUM
// ============================================

public enum State {
    CLOSED,      // Normal, accept requests
    OPEN,        // Failing, reject requests
    HALF_OPEN    // Testing, allow limited requests
}

// ============================================
// EXCEPTION
// ============================================

public class CircuitBreakerOpenException extends RuntimeException {
    public CircuitBreakerOpenException(String message) {
        super(message);
    }
}
```

---

# **5. Usage Example**

Java

```Java
// ============================================
// SERVICE CLIENT WITH CIRCUIT BREAKER
// ============================================

@Service
public class PaymentServiceClient {
    private final CircuitBreaker circuitBreaker;
    private final RestTemplate restTemplate;
    
    public PaymentServiceClient() {
        // failureThreshold=5, successThreshold=2, timeout=30s
        this.circuitBreaker = new CircuitBreaker(5, 2, 30);
    }
    
    public PaymentResponse charge(String orderId, double amount) throws Exception {
        // Execute with circuit breaker
        return circuitBreaker.execute(() -> {
            // This might fail
            return restTemplate.postForObject(
                "http://payment-service/charge",
                new ChargeRequest(orderId, amount),
                PaymentResponse.class
            );
        });
    }
}

// ============================================
// CONTROLLER
// ============================================

@RestController
@RequestMapping("/orders")
public class OrderController {
    private final PaymentServiceClient paymentClient;
    
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@RequestBody OrderRequest req) {
        try {
            // Try to charge
            PaymentResponse payment = paymentClient.charge(req.getOrderId(), req.getTotal());
            
            // Create order
            Order order = new Order(req.getOrderId(), req.getItems());
            orderRepository.save(order);
            
            return ResponseEntity.ok(new OrderResponse(order.getId()));
            
        } catch (CircuitBreakerOpenException e) {
            // Circuit is open, payment service is down
            logger.error("Payment service is unavailable: " + e.getMessage());
            
            // Option 1: Return error to client
            return ResponseEntity.status(503)
                .body(new ErrorResponse("Payment service temporarily unavailable"));
            
            // Option 2: Use fallback
            // return createOrderWithoutPayment(req);
            
        } catch (Exception e) {
            logger.error("Unexpected error: " + e.getMessage());
            return ResponseEntity.status(500)
                .body(new ErrorResponse("Internal server error"));
        }
    }
}

// ============================================
// MONITORING
// ============================================

@RestController
@RequestMapping("/health")
public class HealthController {
    private final PaymentServiceClient paymentClient;
    
    @GetMapping("/circuit-breaker")
    public ResponseEntity<CircuitBreakerStatus> getCircuitBreakerStatus() {
        CircuitBreaker breaker = paymentClient.getCircuitBreaker();
        
        return ResponseEntity.ok(new CircuitBreakerStatus(
            breaker.getState().toString(),
            breaker.getFailureCount(),
            breaker.getSuccessCount()
        ));
    }
}
```

---

# **6. Fallback Strategy**

## **Định nghĩa**

**Fallback** là một hành động thay thế khi circuit breaker open (service down).

## **Các loại Fallback**

### **Fallback 1: Return Error**

Java

```Java
try {
    paymentClient.charge(orderId, amount);
} catch (CircuitBreakerOpenException e) {
    return ResponseEntity.status(503)
        .body(new ErrorResponse("Service temporarily unavailable"));
}
```

### **Fallback 2: Return Default/Cached Value**

Java

```Java
@Service
public class UserService {
    private final UserCache userCache;
    private final CircuitBreaker circuitBreaker;
    
    public User getUser(String userId) {
        try {
            return circuitBreaker.execute(() -> 
                userServiceClient.getUser(userId)
            );
        } catch (CircuitBreakerOpenException e) {
            // Return cached user
            logger.warn("Using cached user for: " + userId);
            return userCache.get(userId);
        }
    }
}
```

### **Fallback 3: Graceful Degradation**

Java

```Java
@Service
public class OrderService {
    private final InventoryClient inventoryClient;
    private final CircuitBreaker circuitBreaker;
    
    public List<Order> getAvailableOrders() {
        try {
            return circuitBreaker.execute(() -> 
                inventoryClient.getAvailableOrders()
            );
        } catch (CircuitBreakerOpenException e) {
            // Return all orders without checking inventory
            logger.warn("Inventory service down, returning all orders");
            return orderRepository.findAll();
        }
    }
}
```

### **Fallback 4: Queue for Later Processing**

Java

```Java
@Service
public class EmailService {
    private final EmailClient emailClient;
    private final CircuitBreaker circuitBreaker;
    private final EmailQueue emailQueue;
    
    public void sendEmail(Email email) {
        try {
            circuitBreaker.execute(() -> 
                emailClient.send(email)
            );
        } catch (CircuitBreakerOpenException e) {
            // Queue email for later retry
            logger.warn("Email service down, queuing email");
            emailQueue.enqueue(email);
        }
    }
}
```

---

# **7. Metrics & Monitoring**

## **Định nghĩa**

**Metrics** là các số liệu để monitor circuit breaker health.

Java

```Java
// ============================================
// CIRCUIT BREAKER METRICS
// ============================================

public class CircuitBreakerMetrics {
    private AtomicInteger successCount = new AtomicInteger(0);
    private AtomicInteger failureCount = new AtomicInteger(0);
    private AtomicInteger rejectionCount = new AtomicInteger(0);
    private AtomicLong lastFailureTime = new AtomicLong(0);
    
    public void recordSuccess() {
        successCount.incrementAndGet();
    }
    
    public void recordFailure() {
        failureCount.incrementAndGet();
        lastFailureTime.set(System.currentTimeMillis());
    }
    
    public void recordRejection() {
        rejectionCount.incrementAndGet();
    }
    
    // ========== GETTERS ==========
    
    public int getSuccessCount() { return successCount.get(); }
    public int getFailureCount() { return failureCount.get(); }
    public int getRejectionCount() { return rejectionCount.get(); }
    public double getFailureRate() {
        int total = successCount.get() + failureCount.get();
        if (total == 0) return 0;
        return (double) failureCount.get() / total;
    }
}

// ============================================
// METRICS ENDPOINT
// ============================================

@RestController
@RequestMapping("/metrics")
public class MetricsController {
    private final CircuitBreakerMetrics metrics;
    
    @GetMapping("/circuit-breaker")
    public ResponseEntity<MetricsResponse> getMetrics() {
        return ResponseEntity.ok(new MetricsResponse(
            metrics.getSuccessCount(),
            metrics.getFailureCount(),
            metrics.getRejectionCount(),
            String.format("%.2f%%", metrics.getFailureRate() * 100)
        ));
    }
}

// Response:
{
  "successCount": 1000,
  "failureCount": 5,
  "rejectionCount": 15,
  "failureRate": "0.50%"
}
```

---

# **8. Circuit Breaker vs Retry vs Timeout**

## **So sánh**

| **Aspect** | **Circuit Breaker** | **Retry** | **Timeout** |
| --- | --- | --- | --- |
| **Purpose** | Prevent cascading failure | Recover from transient errors | Limit wait time |
| **When used** | Service is down | Temporary network glitch | Request taking too long |
| **Action** | Stop calling service | Call again | Stop waiting |
| **Resource impact** | Saves resources | Uses resources | Uses resources |
| **Client impact** | Fast fail | May take time | Predictable wait |

## **Best Practice: Combine All Three**

Java

```Java
// ============================================
// TIMEOUT + RETRY + CIRCUIT BREAKER
// ============================================

@Service
public class ResilientPaymentClient {
    private final CircuitBreaker circuitBreaker;
    private final RestTemplate restTemplate;
    
    private static final int TIMEOUT_MS = 3000;      // 3 second timeout
    private static final int MAX_RETRIES = 3;        // Retry 3 times
    private static final int RETRY_DELAY_MS = 1000;  // Wait 1s between retries
    
    public PaymentResponse charge(String orderId, double amount) throws Exception {
        return circuitBreaker.execute(() -> 
            chargeWithRetry(orderId, amount)
        );
    }
    
    private PaymentResponse chargeWithRetry(String orderId, double amount) throws Exception {
        int retries = 0;
        
        while (retries < MAX_RETRIES) {
            try {
                // Execute with timeout
                return executeWithTimeout(
                    () -> callPaymentService(orderId, amount),
                    TIMEOUT_MS
                );
            } catch (TimeoutException e) {
                retries++;
                if (retries >= MAX_RETRIES) {
                    throw e;  // Give up after max retries
                }
                
                logger.warn("Timeout, retry " + retries + " in " + RETRY_DELAY_MS + "ms");
                Thread.sleep(RETRY_DELAY_MS);
                
            } catch (Exception e) {
                if (isTransientError(e)) {
                    retries++;
                    if (retries >= MAX_RETRIES) {
                        throw e;
                    }
                    Thread.sleep(RETRY_DELAY_MS);
                } else {
                    throw e;  // Don't retry permanent errors
                }
            }
        }
        
        throw new Exception("All retries exhausted");
    }
    
    private <T> T executeWithTimeout(Callable<T> task, int timeoutMs) throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<T> future = executor.submit(task);
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            throw new TimeoutException("Request timed out after " + timeoutMs + "ms");
        } finally {
            executor.shutdown();
        }
    }
    
    private PaymentResponse callPaymentService(String orderId, double amount) {
        // Actual call to payment service
        return restTemplate.postForObject(...);
    }
    
    private boolean isTransientError(Exception e) {
        // Transient errors worth retrying:
        // - Connection timeout
        // - Temporary network failure
        // - 503 Service Unavailable
        // - 429 Too Many Requests
        
        return e instanceof TimeoutException ||
               e instanceof ConnectException ||
               e instanceof SocketException;
    }
}
```

---

# **9. Ưu & Nhược điểm Circuit Breaker**

## **Ưu điểm ✅**

| **Ưu điểm** | **Chi tiết** |
| --- | --- |
| **Prevents cascading failure** | Stop wasting resources on dead service |
| **Fast fail** | Reject request immediately instead of timeout |
| **Automatic recovery** | Transition to HALF_OPEN automatically |
| **Saves resources** | Prevent thread/connection exhaustion |
| **Improves availability** | Healthy services stay responsive |

## **Nhược điểm ❌**

| **Nhược điểm** | **Chi tiết** |
| --- | --- |
| **Extra latency** | In HALF_OPEN, one request is slower |
| **Configuration complex** | Thresholds, timeouts, success count |
| **Testing difficult** | Need to simulate service down |
| **Doesn't fix root cause** | Only prevents cascading, doesn't heal |

---

# **10. Khi nào dùng Circuit Breaker**

## **✅ Dùng khi**

- **Calling external services**: API, database, message queue.
- **Microservices architecture**: Prevent cascading failure.
- **Network calls prone to failure**: Timeouts, retries.
- **Want fast fail**: Don't wait for timeout.

## **❌ Không dùng khi**

- **Local in-process calls**: No risk of cascading failure.
- **Already has timeout**: Circuit breaker adds complexity.
- **Very strict SLA**: Can't afford any additional latency.

---

# **11. Libraries**

## **Hystrix (by Netflix)**

Java

```Java
// Command pattern
public class PaymentCommand extends HystrixCommand<PaymentResponse> {
    private String orderId;
    private double amount;
    private PaymentService paymentService;
    
    public PaymentCommand(String orderId, double amount, PaymentService paymentService) {
        super(HystrixCommandGroupKey.Factory.asKey("PaymentService"));
        this.orderId = orderId;
        this.amount = amount;
        this.paymentService = paymentService;
    }
    
    @Override
    protected PaymentResponse run() throws Exception {
        return paymentService.charge(orderId, amount);
    }
    
    @Override
    protected PaymentResponse getFallback() {
        return new PaymentResponse("FALLBACK", false);
    }
}

// Usage
PaymentResponse response = new PaymentCommand(orderId, amount, paymentService).execute();
```

## **Resilience4j (Modern alternative)**

Java

```Java
// Declarative style
@Service
public class PaymentService {
    private final CircuitBreaker circuitBreaker;
    
    public PaymentService() {
        this.circuitBreaker = CircuitBreaker.ofDefaults("payment");
    }
    
    @CircuitBreaker(name = "payment", fallbackMethod = "chargeWithFallback")
    public PaymentResponse charge(String orderId, double amount) {
        return paymentServiceClient.charge(orderId, amount);
    }
    
    public PaymentResponse chargeWithFallback(String orderId, double amount, Exception e) {
        logger.error("Payment service down", e);
        return new PaymentResponse("FALLBACK", false);
    }
}
```

---

# **12. Checklist: Circuit Breaker**

- [ ]  Understand: CLOSED → OPEN → HALF_OPEN → CLOSED.
- [ ]  Configuration: failure threshold, timeout, success threshold.
- [ ]  Failure detection: count or rate.
- [ ]  Fallback strategy: error, cache, degrade, queue.
- [ ]  Monitoring: success rate, rejection rate, state.
- [ ]  Combine: timeout + retry + circuit breaker.
- [ ]  Libraries: Hystrix, Resilience4j.
- [ ]  Testing: simulate service down.
- [ ]  Logging: track state transitions.
- [ ]  Metrics: expose for monitoring.
