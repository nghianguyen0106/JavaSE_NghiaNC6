package com.example.order.application.service;

import com.example.order.adapter.out.persistence.InMemoryOrderRepository;
import com.example.order.application.dto.CreateOrderCommand;
import com.example.order.application.dto.CreateOrderItemCommand;
import com.example.order.application.dto.OrderResponse;
import com.example.order.application.exception.ApplicationException;
import com.example.order.application.port.out.OrderRepository;
import com.example.order.domain.model.OrderId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Application Layer - CreateOrderUseCase Unit Tests")
class CreateOrderServiceTest {

    private OrderRepository orderRepository;
    private OrderValidator validator;
    private CreateOrderService useCase;

    @BeforeEach
    void setUp() {
        // Sử dụng In-Memory Repository: Chạy test siêu nhanh không cần MySQL/H2!
        orderRepository = new InMemoryOrderRepository();
        validator = new OrderValidator();
        useCase = new CreateOrderService(orderRepository, validator);
    }

    @Test
    @DisplayName("Tạo đơn hàng thành công và lưu vào repository")
    void testCreateOrderSuccess() {
        CreateOrderCommand command = new CreateOrderCommand(
                "ORD123",
                List.of(
                        new CreateOrderItemCommand("P1", 2, 100.0)
                )
        );

        OrderResponse response = useCase.execute(command);

        assertNotNull(response);
        assertEquals("ORD123", response.getOrderId());
        assertEquals(200.0, response.getTotal());
        assertEquals("CONFIRMED", response.getStatus());

        // Kiểm tra dữ liệu đã thực sự được lưu vào repository
        assertTrue(orderRepository.existsById(new OrderId("ORD123")));
    }

    @Test
    @DisplayName("Ném ApplicationException khi danh sách item bị rỗng")
    void testCreateOrderEmptyItems() {
        CreateOrderCommand command = new CreateOrderCommand("ORD123", new ArrayList<>());

        assertThrows(ApplicationException.class, () -> useCase.execute(command));
    }

    @Test
    @DisplayName("Ném ApplicationException khi OrderId bị để trống")
    void testCreateOrderBlankId() {
        CreateOrderCommand command = new CreateOrderCommand("   ", List.of(
                new CreateOrderItemCommand("P1", 1, 50.0)
        ));

        assertThrows(ApplicationException.class, () -> useCase.execute(command));
    }
}
