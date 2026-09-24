package gateway;

import java.util.Map;
import model.PaymentMethod;

public class PaymentGatewayFactory {
    private final Map<PaymentMethod, PaymentGateway> paymentGateways;

    public PaymentGatewayFactory(
            StripePaymentGateway stripePaymentGateway,
            PaypalPaymentGateway paypalPaymentGateway,
            BankPaymentGateway bankPaymentGateway) {
        this.paymentGateways = Map.of(
                PaymentMethod.CREDIT_CARD, stripePaymentGateway,
                PaymentMethod.PAYPAL, paypalPaymentGateway,
                PaymentMethod.BANK, bankPaymentGateway);
    }

    public PaymentGateway getPaymentGateway(PaymentMethod paymentMethod) {
        PaymentGateway paymentGateway = paymentGateways.get(paymentMethod);
        if (paymentGateway == null) {
            System.err.println("Method unsupported: " + paymentMethod);
        }
        return paymentGateway;
    }
}
