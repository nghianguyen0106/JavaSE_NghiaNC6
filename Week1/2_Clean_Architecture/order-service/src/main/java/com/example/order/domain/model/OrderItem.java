package com.example.order.domain.model;

import com.example.order.domain.exception.DomainException;
import java.util.Objects;

public class OrderItem {
    private final String productId;
    private final int quantity;
    private final Money price;

    public OrderItem(String productId, int quantity, double price) {
        if (productId == null || productId.trim().isEmpty()) {
            throw new DomainException("ProductId cannot be empty");
        }
        if (quantity <= 0) {
            throw new DomainException("Quantity must be positive");
        }
        this.productId = productId.trim();
        this.quantity = quantity;
        this.price = new Money(price);
    }

    public String getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public Money getPrice() {
        return price;
    }

    public Money getSubtotal() {
        return new Money(price.getAmount() * quantity);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrderItem orderItem)) return false;
        return quantity == orderItem.quantity &&
                Objects.equals(productId, orderItem.productId) &&
                Objects.equals(price, orderItem.price);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId, quantity, price);
    }
}
