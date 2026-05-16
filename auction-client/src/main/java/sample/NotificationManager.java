package sample;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/*Singleton lưu danh sách thông báo đấu giá realtime.
Thread-safe: dùng synchronized list.*/
public class NotificationManager {

    public static class Notification {
        public final String message;
        public final long   timestamp;

        public Notification(String message) {
            this.message   = message;
            this.timestamp = System.currentTimeMillis();
        }
    }

   // SINGLETON
    private static NotificationManager instance;
    public static synchronized NotificationManager getInstance() {
        if (instance == null) instance = new NotificationManager();
        return instance;
    }

   //DANH SÁCH THÔNG BÁO
    private final List<Notification> notifications =
            Collections.synchronizedList(new ArrayList<>());

    //SỐ LƯỢNG THÔNG BÁO CHƯA ĐỌC
    private int unreadCount = 0;

    //GỌI LẠI ĐỂ CẬP NHẬT
    private Runnable onNewNotification;

    private NotificationManager() {}

   //THÊM THÔNG BÁO VÀO DANH SÁCH
    public synchronized void addNotification(String message) {
        notifications.add(new Notification(message));
        unreadCount++;
        if (onNewNotification != null) {
            onNewNotification.run();
        }
    }

    //ĐÁNH DẤU ĐỌC TẤT CẢ
    public synchronized void markAllRead() {
        unreadCount = 0;
    }

    public synchronized int getUnreadCount() { return unreadCount; }

    public synchronized List<Notification> getAll() {
        return new ArrayList<>(notifications);
    }

    //TẠO CALLBACK
    public void setOnNewNotification(Runnable callback) {
        this.onNewNotification = callback;
    }
}