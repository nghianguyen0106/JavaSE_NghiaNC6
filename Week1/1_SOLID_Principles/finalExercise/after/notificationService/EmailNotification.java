package notificationService;

import common.EmailConfig;
import common.MessageCommon;
import model.Notification;
import dummy.SMTP;

public class EmailNotification implements NotificationService {

    private final EmailConfig emailConfig;
    private final MessageCommon messageCommon;

    public EmailNotification(EmailConfig emailConfig, MessageCommon messageCommon) {
        this.emailConfig = emailConfig;
        this.messageCommon = messageCommon;
    }

    @Override
    public void send(Notification notification) {
        String emailContent = messageCommon.get("payment_process", notification.getOrder().getTotal());
        SMTP smtp = new SMTP(
                emailConfig.get("__SMTP_GMAIL_DOMAIN__"),
                emailConfig.get("__SMTP_GMAIL_PORT__"),
                emailConfig.get("__SMTP_GMAIL_EMAIL__"),
                emailConfig.get("__SMTP_GMAIL_PASSWORD__"));
        smtp.send(notification.getUserEmail(), messageCommon.get("payment_confirm"), emailContent);
    }
}