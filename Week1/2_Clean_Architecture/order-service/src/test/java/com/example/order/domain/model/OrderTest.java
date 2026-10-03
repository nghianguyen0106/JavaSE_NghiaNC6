package com.example.order.domain.model;

import com.example.order.domain.exception.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Domain Layer - Order Entity Unit Tests")
class OrderTest {

    @Test
    @DisplayName("Đơn hàng phải có ít nhất 1 item, nếu rỗng ném DomainException")
    void testOrderMustHaveItems() {
        OrderId orderId = new OrderId("ORD123");
        List<OrderItem> emptyList = new ArrayList<>();

        assertThrows(DomainException.class, () -> new Order(orderId, emptyList));
    }

    @Test
    @DisplayName("Tính đúng tổng tiền đơn hàng từ danh sách các items")
    void testCalculateTotal() {
        List<OrderItem> items = List.of(
                new OrderItem("P1", 2, 100.0), // 200.0
                new OrderItem("P2", 1, 50.0)   // 50.0
        );
        Order order = new Order(new OrderId("ORD123"), items);

        assertEquals(250.0, order.calculateTotal().getAmount());
    }

    @Test
    @DisplayName("Xác nhận đơn hàng từ PENDING chuyển sang CONFIRMED")
    void testConfirmOrder() {
        List<OrderItem> items = List.of(
                new OrderItem("P1", 1, 100.0)
        );
        Order order = new Order(new OrderId("ORD123"), items);

        assertEquals(OrderStatus.PENDING, order.getStatus());
        order.confirm();
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
    }

    @Test
    @DisplayName("Không được phép confirm 2 lần")
    void testCannotConfirmTwice() {
        List<OrderItem> items = List.of(
                new OrderItem("P1", 1, 100.0)
        );
        Order order = new Order(new OrderId("ORD123"), items);

        order.confirm();
        assertThrows(DomainException.class, order::confirm);
    }
}
