package service;

import gateway.*;
import model.*;
import validator.*;
import repository.*;
import notificationService.*;

public class PaymentService {
        private final OrderValidator orderValidator;
        private final PaymentGatewayFactory paymentGatewayFactory;
        private final PaymentRepository paymentRepository;
        private final NotificationService notificationService;

        public PaymentService(OrderValidator orderValidator, PaymentGatewayFactory paymentGatewayFactory,
                        PaymentRepository paymentRepository, NotificationService notificationService) {
                this.orderValidator = orderValidator;
                this.paymentGatewayFactory = paymentGatewayFactory;
                this.paymentRepository = paymentRepository;
                this.notificationService = notificationService;
        }

        public void processPayment(Order order, PaymentMethod paymentMethod) {
                // Validate
                orderValidator.check(order);

                // Process payment
                double amount = order.getTotal();
                PaymentGateway paymentGateway = paymentGatewayFactory.getPaymentGateway(paymentMethod);
                paymentGateway.charge(amount);

                // Save to database
                paymentRepository.save(new Payment(amount, paymentMethod));

                // Send email
                notificationService.send(new Notification(order, "customer@example.com"));
                // Log
                System.out.println("Payment processed for order " + order.getId());
        }
}
