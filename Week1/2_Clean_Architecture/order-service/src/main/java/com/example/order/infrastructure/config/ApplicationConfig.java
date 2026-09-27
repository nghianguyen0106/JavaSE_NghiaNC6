package com.example.order.infrastructure.config;

import com.example.order.application.port.in.CreateOrderUseCase;
import com.example.order.application.port.out.OrderRepository;
import com.example.order.application.service.CreateOrderService;
import com.example.order.application.service.OrderValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Infrastructure Configuration.
 * Đóng vai trò Composition Root: Wire các POJO Use Case với Repository Adapter.
 * Giúp tầng Application hoàn toàn không cần dùng @Service hay @Autowired.
 */
@Configuration
public class ApplicationConfig {

    @Bean
    public OrderValidator orderValidator() {
        return new OrderValidator();
    }

    @Bean
    public CreateOrderUseCase createOrderUseCase(OrderRepository orderRepository, OrderValidator orderValidator) {
        return new CreateOrderService(orderRepository, orderValidator);
    }
}
