package common;

public class MessageCommon {
    public String get(String key, Object... args) {
        if ("payment_process".equals(key)) {
            Object amount = args.length > 0 ? args[0] : 0;
            return "Payment of " + amount + " processed";
        }
        if ("payment_confirm".equals(key)) {
            return "Payment Confirmation";
        }
        return key;
    }
}
