package com.example.order.application.dto;

public class CreateOrderItemCommand {
    private final String productId;
    private final int quantity;
    private final double price;

    public CreateOrderItemCommand(String productId, int quantity, double price) {
        this.productId = productId;
        this.quantity = quantity;
        this.price = price;
    }

    public String getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getPrice() {
        return price;
    }
}
