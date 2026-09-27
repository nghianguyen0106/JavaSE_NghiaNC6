package com.example.order.application.dto;

import java.util.Collections;
import java.util.List;

public class CreateOrderCommand {
    private final String orderId;
    private final List<CreateOrderItemCommand> items;

    public CreateOrderCommand(String orderId, List<CreateOrderItemCommand> items) {
        this.orderId = orderId;
        this.items = items != null ? items : Collections.emptyList();
    }

    public String getOrderId() {
        return orderId;
    }

    public List<CreateOrderItemCommand> getItems() {
        return items;
    }
}
