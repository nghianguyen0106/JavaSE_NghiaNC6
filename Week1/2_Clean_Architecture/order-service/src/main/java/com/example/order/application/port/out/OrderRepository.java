package com.example.order.application.port.out;

import com.example.order.domain.model.Order;
import com.example.order.domain.model.OrderId;
import java.util.Optional;

/**
 * Output Port (Repository Interface).
 * Tầng Application sở hữu interface này để giao tiếp với DB mà không phụ thuộc DB cụ thể.
 */
public interface OrderRepository {
    void save(Order order);
    Optional<Order> findById(OrderId orderId);
    boolean existsById(OrderId orderId);
}
