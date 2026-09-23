package before;

import before.dummy.BankAPI;
import before.dummy.Order;
import before.dummy.PayPalAPI;
import before.dummy.SMTP;
import before.dummy.StripeAPI;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

public class PaymentService {

    public void processPayment(Order order, String paymentMethod) throws Exception {
        // Validate
        if (order == null || order.getTotal() <= 0) {
            throw new Exception("Invalid order");
        }

        // Process payment
        double amount = order.getTotal();

        if (paymentMethod.equals("CREDIT_CARD")) {
            // Call Stripe API
            StripeAPI stripe = new StripeAPI();
            stripe.charge(amount);
        } else if (paymentMethod.equals("PAYPAL")) {
            // Call PayPal API
            PayPalAPI paypal = new PayPalAPI();
            paypal.pay(amount);
        } else if (paymentMethod.equals("BANK")) {
            // Call Bank API
            BankAPI bank = new BankAPI();
            bank.transfer(amount);
        }

        // Save to database
        String query = "INSERT INTO payments (amount, method) VALUES (?, ?)";
        Connection conn = DriverManager.getConnection("jdbc:mysql://localhost/db");
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setDouble(1, amount);
        stmt.setString(2, paymentMethod);
        stmt.executeUpdate();

        // Send email
        String emailContent = "Payment of " + amount + " processed";
        SMTP smtp = new SMTP("smtp.gmail.com", 587, "user@gmail.com", "password");
        smtp.send("customer@example.com", "Payment Confirmation", emailContent);

        // Log
        System.out.println("Payment processed for order " + order.getId());
    }
}
