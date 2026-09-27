package com.example.order.application.dto;

public class OrderResponse {
    private final String orderId;
    private final double total;
    private final String status;

    public OrderResponse(String orderId, double total, String status) {
        this.orderId = orderId;
        this.total = total;
        this.status = status;
    }

    public String getOrderId() {
        return orderId;
    }

    public double getTotal() {
        return total;
    }

    public String getStatus() {
        return status;
    }
}
