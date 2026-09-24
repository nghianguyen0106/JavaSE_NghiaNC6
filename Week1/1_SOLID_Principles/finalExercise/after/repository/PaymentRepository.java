package repository;

import model.Payment;

public interface PaymentRepository {
    void save(Payment payment);
}