package sample;

import sample.model.PlacedBidRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

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
    private final List<Runnable> listeners = new ArrayList<>();

    //OBSERVER CHO BID_UPDATE
    private final List<Consumer<PlacedBidRequest>> bidUpdateListeners = new ArrayList<>();

    private NotificationManager() {}

   //THÊM THÔNG BÁO VÀO DANH SÁCH
    public synchronized void addNotification(String message) {
        notifications.add(new Notification(message));
        unreadCount++;
        for (Runnable callback : listeners) {
            callback.run();
        }
    }

    //PHÁT BID_UPDATE ĐẾN TẤT CẢ SUBCRIBER
    public synchronized void notifyBidUpdate(PlacedBidRequest req) {
        for (Consumer<PlacedBidRequest> listener : bidUpdateListeners) {
            listener.accept(req);
        }
    }

    //ĐĂNG KÍ THÔNG BÁO
    public synchronized void addBidUpdateListener(Consumer<PlacedBidRequest> listener) {
        bidUpdateListeners.add(listener);
    }

    // HUỶ ĐĂNG KÝ
    public synchronized void removeBidUpdateListener(Consumer<PlacedBidRequest> listener) {
        bidUpdateListeners.remove(listener);
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
    public synchronized void addNotificationListener(Runnable callback) {
        listeners.add(callback);
    }
}