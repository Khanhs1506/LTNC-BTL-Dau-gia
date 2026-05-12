package com.auction.server.repository;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

public class DatabaseManager {


    private static class Holder {
        private static final DatabaseManager INSTANCE = new DatabaseManager();
    }

    private String url;
    private String user;
    private String password;

    private DatabaseManager() {
        Properties properties = new Properties();
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("database.properties")) {

            if (input == null) {
                System.err.println("[DatabaseManager] Không tìm thấy file database.properties!");
                return;
            }

            properties.load(input);
            this.url      = properties.getProperty("db.url");
            this.user     = properties.getProperty("db.user");
            this.password = properties.getProperty("db.password");

            // Kiểm tra kết nối thử 1 lần khi khởi động server
            try (Connection testConn = DriverManager.getConnection(url, user, password)) {
                System.out.println("[DatabaseManager] Kết nối database thành công!");
            }

        } catch (Exception e) {
            System.err.println("[DatabaseManager] Lỗi kết nối: " + e.getMessage());
        }
    }

    public static DatabaseManager getInstance() {
        return Holder.INSTANCE;
    }

    // Mỗi lần gọi trả về 1 Connection MỚI
    // Caller phải tự đóng bằng try-with-resources: try (Connection conn = getConnection())
    public Connection getConnection() throws Exception {
        if (url == null) {
            throw new Exception("[DatabaseManager] Chưa load được cấu hình database!");
        }
        return DriverManager.getConnection(url, user, password);
    }
}