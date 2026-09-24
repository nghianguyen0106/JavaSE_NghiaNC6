package gateway;

import dummy.StripeAPI;

public class StripePaymentGateway implements PaymentGateway {

    private final StripeAPI stripeAPI;

    public StripePaymentGateway(StripeAPI stripeAPI) {
        this.stripeAPI = stripeAPI;
    }

    @Override
    public void charge(double amount) {
        this.stripeAPI.charge(amount);
    }
}