package com.example.order.adapter.in.web;

import com.example.order.adapter.in.web.dto.CreateOrderRequest;
import com.example.order.application.dto.CreateOrderCommand;
import com.example.order.application.dto.CreateOrderItemCommand;
import com.example.order.application.dto.OrderResponse;
import com.example.order.application.exception.ApplicationException;
import com.example.order.application.port.in.CreateOrderUseCase;
import com.example.order.domain.exception.DomainException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * In Adapter (REST Controller).
 * Chuyển đổi giao thức HTTP thành Command cho Use Case.
 * Không chứa business rule/logic.
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final CreateOrderUseCase createOrderUseCase;

    public OrderController(CreateOrderUseCase createOrderUseCase) {
        this.createOrderUseCase = createOrderUseCase;
    }

    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody CreateOrderRequest request) {
        try {
            List<CreateOrderItemCommand> itemCommands = request.getItems() != null
                    ? request.getItems().stream()
                    .map(itemReq -> new CreateOrderItemCommand(
                            itemReq.getProductId(),
                            itemReq.getQuantity(),
                            itemReq.getPrice()
                    ))
                    .collect(Collectors.toList())
                    : Collections.emptyList();

            CreateOrderCommand command = new CreateOrderCommand(request.getOrderId(), itemCommands);

            OrderResponse response = createOrderUseCase.execute(command);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (ApplicationException | DomainException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
