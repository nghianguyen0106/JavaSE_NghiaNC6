package com.example.order.application.port.in;

import com.example.order.application.dto.CreateOrderCommand;
import com.example.order.application.dto.OrderResponse;

/**
 * Input Port (Use Case Interface).
 * Định nghĩa use case xử lý tạo đơn hàng.
 */
public interface CreateOrderUseCase {
    OrderResponse execute(CreateOrderCommand command);
}
