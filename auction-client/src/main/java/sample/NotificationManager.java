package sample;

import java.util.*;

public class NotificationManager {

    public static class Notification {
        public final String message;
        public final long timestamp;

        public Notification(String message) {
            this.message = message;
            this.timestamp = System.currentTimeMillis();
        }
    }

    private static NotificationManager instance;
    public synchronized static NotificationManager getInstance(){
        if (instance == null) instance = new NotificationManager();
        return instance;
    }

    private final List<Notification> notifications = Collections.synchronizedList(new ArrayList<>());
    private int unreadCount = 0;
    private Runnable onNewNotification;

    private NotificationManager(){}

    public synchronized void addNotification(String message) {
        notifications.add(new Notification(message));
        unreadCount++;
        if (onNewNotification != null) {
            onNewNotification.run();
        }
    }

    public synchronized void markAllRead() {
        unreadCount = 0;
    }

    public synchronized int getUnreadCount() {
        return unreadCount;
    }

    public synchronized List<Notification> getAll() {
        return new ArrayList<>(notifications);
    }

    public void setOnNewNotification(Runnable callback) {
        this.onNewNotification = callback;
    }
}
