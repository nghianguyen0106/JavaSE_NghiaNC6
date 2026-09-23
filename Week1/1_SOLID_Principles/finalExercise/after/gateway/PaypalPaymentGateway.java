package gateway;

import dummy.PayPalAPI;

public class PaypalPaymentGateway implements PaymentGateway {
    private final PayPalAPI payPalAPI;

    public PaypalPaymentGateway(PayPalAPI payPalAPI) {
        this.payPalAPI = payPalAPI;
    }

    @Override
    public void charge(double amount) {
        this.payPalAPI.pay(amount);
    }
}