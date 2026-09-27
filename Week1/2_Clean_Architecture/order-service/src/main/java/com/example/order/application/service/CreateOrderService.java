package com.example.order.application.service;

import com.example.order.application.dto.CreateOrderCommand;
import com.example.order.application.dto.OrderResponse;
import com.example.order.application.port.in.CreateOrderUseCase;
import com.example.order.application.port.out.OrderRepository;
import com.example.order.domain.model.Money;
import com.example.order.domain.model.Order;
import com.example.order.domain.model.OrderId;
import com.example.order.domain.model.OrderItem;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Use Case Implementation / Application Service.
 * Điều phối giữa Domain và Out Port, hoàn toàn KHÔNG phụ thuộc Spring Framework.
 */
public class CreateOrderService implements CreateOrderUseCase {

    private final OrderRepository orderRepository;
    private final OrderValidator validator;

    public CreateOrderService(OrderRepository orderRepository, OrderValidator validator) {
        this.orderRepository = orderRepository;
        this.validator = validator;
    }

    @Override
    public OrderResponse execute(CreateOrderCommand command) {
        // 1. Validate application rules
        validator.validate(command);

        // 2. Chuyển đổi Command sang Domain Objects
        OrderId orderId = new OrderId(command.getOrderId());
        List<OrderItem> items = command.getItems().stream()
                .map(itemCmd -> new OrderItem(
                        itemCmd.getProductId(),
                        itemCmd.getQuantity(),
                        itemCmd.getPrice()
                ))
                .collect(Collectors.toList());

        // 3. Thực thi nghiệp vụ trên Domain Entity
        Order order = new Order(orderId, items);
        order.confirm();

        // 4. Lưu lại thông qua Output Port (OrderRepository)
        orderRepository.save(order);

        // 5. Trả về Response DTO
        Money total = order.calculateTotal();
        return new OrderResponse(
                order.getId().getValue(),
                total.getAmount(),
                order.getStatus().name()
        );
    }
}
