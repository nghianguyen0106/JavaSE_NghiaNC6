package com.example.order.adapter.out.persistence;

import com.example.order.application.port.out.OrderRepository;
import com.example.order.domain.model.Order;
import com.example.order.domain.model.OrderId;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-Memory Database Adapter.
 * Dùng để test hoặc chạy nhanh không cần database thật.
 */
public class InMemoryOrderRepository implements OrderRepository {

    private final Map<OrderId, Order> database = new ConcurrentHashMap<>();

    @Override
    public void save(Order order) {
        database.put(order.getId(), order);
    }

    @Override
    public Optional<Order> findById(OrderId orderId) {
        return Optional.ofNullable(database.get(orderId));
    }

    @Override
    public boolean existsById(OrderId orderId) {
        return database.containsKey(orderId);
    }

    public void clear() {
        database.clear();
    }
}
