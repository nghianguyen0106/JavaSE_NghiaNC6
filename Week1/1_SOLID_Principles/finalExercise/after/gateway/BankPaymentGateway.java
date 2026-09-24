package gateway;

import dummy.BankAPI;

public class BankPaymentGateway implements PaymentGateway {
    private final BankAPI bankAPI;

    public BankPaymentGateway(BankAPI bankAPI) {
        this.bankAPI = bankAPI;
    }

    @Override
    public void charge(double amount) {
        this.bankAPI.transfer(amount);
    }
}