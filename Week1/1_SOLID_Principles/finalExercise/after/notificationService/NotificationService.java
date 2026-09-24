package notificationService;

import model.Notification;

public interface NotificationService {
    void send(Notification notification);
}