package com.auction.client.utils;



import com.auction.client.model.User; // Giả sử bạn có class User ở model

public class SessionManager {
    private static SessionManager instance;
    private String username;
    private String role;
    private String token; // Nếu dùng REST API thì lưu token ở đây

    // Private constructor ngăn tạo đối tượng bằng từ khóa 'new'
    private SessionManager() {}

    // Phương thức tĩnh để lấy đối tượng duy nhất (Singleton)
    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    // Lưu thông tin khi login thành công
    public void setSession(String username, String role) {
        this.username = username;
        this.role = role;
    }

    // Xóa thông tin khi logout
    public void clearSession() {
        this.username = null;
        this.role = null;
    }

    public String getUsername() { return username; }
    public String getRole() { return role; }
}
