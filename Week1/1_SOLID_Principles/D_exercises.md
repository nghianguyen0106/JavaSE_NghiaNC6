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
Cần phải thêm phương thức Paypal vào hàm process, thêm class xử lý Paypal.

2. Hãy refactor dùng interface.

```Java
// Abstraction
public interface PaymentGateway {
    void charge(double amount);
}

// Low-level: Stripe
public class StripePaymentGateway implements PaymentGateway {
    @Override
    public void charge(double amount) {
        // Call Stripe
    }
}

// Low-level: PayPal
public class PayPalPaymentGateway implements PaymentGateway {
    @Override
    public void charge(double amount) {
        // Call PayPal
    }
}

// High-level: PaymentProcessor
public class PaymentProcessor {
    private final PaymentGateway paymentGateway;
    
    public PaymentProcessor(PaymentGateway paymentGateway) {
        this.paymentGateway = paymentGateway;
    }
    
    public void process(double amount) {
        paymentGateway.charge(amount);
    }
}
```

3. Cách test mà không cần Stripe/PayPal.

```Java
// Mock implementation cho testing
public class MockPaymentGateway implements PaymentGateway {
    private boolean charged = false;
    
    @Override
    public void charge(double amount) {
        charged = true;
    }
    
    public boolean isCharged() {
        return charged;
    }
}

// Test
@Test
public void testPaymentProcessor() {
    MockPaymentGateway mockGateway = new MockPaymentGateway();
    PaymentProcessor processor = new PaymentProcessor(mockGateway);
    
    processor.process(100.0);
    
    assertTrue(mockGateway.isCharged());
}
```
