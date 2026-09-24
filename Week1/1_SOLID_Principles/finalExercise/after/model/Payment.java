package model;

public class Payment {
    private final double amount;
    private final PaymentMethod method;

    public Payment(double amount, PaymentMethod method) {
        this.amount = amount;
        this.method = method;
    }

    public double getAmount() {
        return amount;
    }

    public PaymentMethod getMethod() {
        return method;
    }
}
