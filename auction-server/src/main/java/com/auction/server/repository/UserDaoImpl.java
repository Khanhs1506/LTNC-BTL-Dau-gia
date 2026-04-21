package com.auction.server.repository;

import com.auction.server.model.User;
import com.auction.server.model.Admin;
import com.auction.server.model.Seller;
import com.auction.server.model.Bidder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class UserDaoImpl implements IUserDAO{
    @Override
    public User getUserByUsername(String inputUsername) {

        String sql = "SELECT * FROM users WHERE username = ?";
        User foundUser = null;

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // Điền chữ người dùng nhập vào cái dấu "?" đầu tiên
            stmt.setString(1, inputUsername);

            // Chạy lệnh SQL và hứng cái bảng kết quả trả về vào ResultSet
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    // 1. Nhặt từng thông tin từ cơ sở dữ liệu ra trước
                    String dbId = rs.getString("id");
                    String dbUser = rs.getString("username");
                    String dbPass = rs.getString("password");
                    String dbRole = rs.getString("role"); // Đọc cột role để biết là ai

                    switch (dbRole) {
                        case "ADMIN":
                            foundUser = new Admin(dbId, dbUser, dbPass);
                            break;
                        case "SELLER":
                            foundUser = new Seller(dbId, dbUser, dbPass);
                            break;
                        case "BIDDER":
                            foundUser = new Bidder(dbId, dbUser, dbPass);
                            break;
                        default:
                            System.err.println("Invalid Role");
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error occurred while trying to find User: " + e.getMessage());
        }

        return foundUser;
    }
}