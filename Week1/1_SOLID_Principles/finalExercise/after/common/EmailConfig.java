package common;

import java.util.HashMap;
import java.util.Map;

public class EmailConfig {
    private final Map<String, String> configs = new HashMap<>();

    public EmailConfig() {
        configs.put("__SMTP_GMAIL_DOMAIN__", "smtp.gmail.com");
        configs.put("__SMTP_GMAIL_PORT__", "587");
        configs.put("__SMTP_GMAIL_EMAIL__", "user@gmail.com");
        configs.put("__SMTP_GMAIL_PASSWORD__", "password");
    }

    public String get(String key) {
        return configs.getOrDefault(key, "");
    }
}