package model;

public class Notification {
    private final Order order;
    private final String userEmail;

    public Notification(Order order, String userEmail) {
        this.order = order;
        this.userEmail = userEmail;
    }

    public Order getOrder() {
        return order;
    }

    public String getUserEmail() {
        return userEmail;
    }
}
