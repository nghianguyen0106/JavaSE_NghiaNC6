package com.example.order.domain.model;

import com.example.order.domain.exception.DomainException;
import java.util.Objects;

public class Money {
    private final double amount;

    public Money(double amount) {
        if (amount < 0) {
            throw new DomainException("Money cannot be negative");
        }
        this.amount = amount;
    }

    public double getAmount() {
        return amount;
    }

    public Money add(Money other) {
        return new Money(this.amount + other.amount);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Money money)) return false;
        return Double.compare(money.amount, amount) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount);
    }

    @Override
    public String toString() {
        return String.valueOf(amount);
    }
}
