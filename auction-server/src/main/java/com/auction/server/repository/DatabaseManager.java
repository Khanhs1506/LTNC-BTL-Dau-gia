package com.auction.server.repository;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class DatabaseManager {

    private static class Holder {
        private static final DatabaseManager INSTANCE = new DatabaseManager();
    }
    private HikariDataSource dataSource;

    private DatabaseManager() {
        Properties properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("database.properties")) {

            if (input == null) {
                throw new IllegalStateException("[DatabaseManager] Không tìm thấy file database.properties!");
            }

            properties.load(input);
            String url = properties.getProperty("db.url");
            String user = properties.getProperty("db.user");
            String password = properties.getProperty("db.password");

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(url);
            config.setUsername(user);
            config.setPassword(password);
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setConnectionTimeout(10_000);
            config.setIdleTimeout(120_000);
            config.setMaxLifetime(600_000);
            config.setPoolName("AuctionHikariPool");
            this.dataSource = new HikariDataSource(config);

            // Kiểm tra mượn connection từ pool ngay khi khởi động
            try (Connection testConn = this.dataSource.getConnection()) {
                System.out.println("[DatabaseManager] Kết nối database thành công!");
            }

        } catch (Exception e) {
            throw new IllegalStateException("[DatabaseManager] Lỗi khởi tạo connection pool: " + e.getMessage(), e);
        }
    }

    public static DatabaseManager getInstance() {
        return Holder.INSTANCE;
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("[DatabaseManager] Connection pool chưa được khởi tạo!");
        }
        return dataSource.getConnection();
    }
}