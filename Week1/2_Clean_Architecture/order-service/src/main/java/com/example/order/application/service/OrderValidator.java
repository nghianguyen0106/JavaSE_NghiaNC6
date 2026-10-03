package com.example.order.application.service;

import com.example.order.application.dto.CreateOrderCommand;
import com.example.order.application.dto.CreateOrderItemCommand;
import com.example.order.application.exception.ApplicationException;

public class OrderValidator {

    public void validate(CreateOrderCommand command) {
        if (command == null) {
            throw new ApplicationException("Command cannot be null");
        }
        if (command.getOrderId() == null || command.getOrderId().trim().isEmpty()) {
            throw new ApplicationException("OrderId is required");
        }
        if (command.getItems() == null || command.getItems().isEmpty()) {
            throw new ApplicationException("Order must have items");
        }
        for (CreateOrderItemCommand item : command.getItems()) {
            if (item.getProductId() == null || item.getProductId().trim().isEmpty()) {
                throw new ApplicationException("ProductId is required for each item");
            }
            if (item.getQuantity() <= 0) {
                throw new ApplicationException("Quantity must be positive");
            }
            if (item.getPrice() < 0) {
                throw new ApplicationException("Price cannot be negative");
            }
        }
    }
}
