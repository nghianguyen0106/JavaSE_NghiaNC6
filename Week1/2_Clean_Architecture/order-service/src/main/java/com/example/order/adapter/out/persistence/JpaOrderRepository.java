package com.example.order.adapter.out.persistence;

import com.example.order.application.port.out.OrderRepository;
import com.example.order.domain.model.*;
import com.example.order.infrastructure.persistence.entity.OrderEntity;
import com.example.order.infrastructure.persistence.entity.OrderItemEntity;
import com.example.order.infrastructure.persistence.repository.OrderJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Persistence Adapter implementing the domain's OrderRepository Output Port.
 * Đóng vai trò cầu nối chuyển đổi giữa Domain Entity và Database JPA Entity.
 */
@Component
public class JpaOrderRepository implements OrderRepository {

    private final OrderJpaRepository jpaRepository;

    public JpaOrderRepository(OrderJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void save(Order order) {
        List<OrderItemEntity> itemEntities = order.getItems().stream()
                .map(item -> new OrderItemEntity(
                        item.getProductId(),
                        item.getQuantity(),
                        item.getPrice().getAmount()
                ))
                .collect(Collectors.toList());

        OrderEntity entity = new OrderEntity(
                order.getId().getValue(),
                order.calculateTotal().getAmount(),
                order.getStatus().name(),
                itemEntities
        );

        jpaRepository.save(entity);
    }

    @Override
    public Optional<Order> findById(OrderId orderId) {
        return jpaRepository.findById(orderId.getValue())
                .map(entity -> {
                    List<OrderItem> items = entity.getItems().stream()
                            .map(itemEntity -> new OrderItem(
                                    itemEntity.getProductId(),
                                    itemEntity.getQuantity(),
                                    itemEntity.getPrice()
                            ))
                            .collect(Collectors.toList());

                    return new Order(
                            new OrderId(entity.getId()),
                            items,
                            OrderStatus.valueOf(entity.getStatus())
                    );
                });
    }

    @Override
    public boolean existsById(OrderId orderId) {
        return jpaRepository.existsById(orderId.getValue());
    }
}
