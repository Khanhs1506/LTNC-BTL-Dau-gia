package sample;

/**
 * Lưu thông tin phiên đăng nhập hiện tại.
 * Dùng ở bất kỳ Controller nào cần biết user đang online.
 */
public class Session {
    private static String currentUsername;

    public static void setCurrentUsername(String username) {
        currentUsername = username;
    }

    public static String getCurrentUsername() {
        return currentUsername;
    }

    public static void clear() {
        currentUsername = null;
    }
}