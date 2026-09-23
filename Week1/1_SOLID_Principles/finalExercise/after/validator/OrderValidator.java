package validator;

import model.Order;

public class OrderValidator {
    public void check(Order order) {
        if (order == null || order.getTotal() <= 0) {
            throw new IllegalArgumentException("Invalid order: Order must not be null and total > 0");
        }
    }
}
