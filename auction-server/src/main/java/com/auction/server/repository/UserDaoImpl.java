package com.auction.server.repository;

import com.auction.server.model.User;
import com.auction.server.model.Admin;
import com.auction.server.model.Seller;
import com.auction.server.model.Bidder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
                    double dbBalance = rs.getDouble("balance");

                    switch (dbRole) {
                        case "ADMIN":
                            foundUser = new Admin(dbId, dbUser, dbPass);
                            break;
                        case "SELLER":
                            foundUser = new Seller(dbId, dbUser, dbPass);
                            break;
                        case "BIDDER":
                            foundUser = new Bidder(dbId, dbUser, dbPass, dbBalance);
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

    @Override
    public boolean registerUser(User newUser) {
        if (newUser.getId() == null || newUser.getId().isEmpty()) {
            newUser.setId(UUID.randomUUID().toString());
        }
        String sql = "INSERT INTO users (id, username, password, role) VALUES (?, ?, ?, ?)";
        boolean isSuccess = false;

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, newUser.getId());
            stmt.setString(2, newUser.getUsername());
            stmt.setString(3, newUser.getPassword());
            stmt.setString(4, newUser.getRole()); // Lấy Role từ Object

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                isSuccess = true; // Nếu có ít nhất 1 dòng được thêm vào kho -> Success
            }

        } catch (Exception e) {
            System.err.println("Error occurred while trying to register: " + e.getMessage());
        }
        return isSuccess;
    }

    //LẤY DANH SÁCH NGƯỜI DÙNG
    @Override
    public List<User> getAllUsers() {
        String sql = "SELECT * FROM users ORDER BY role, username";
        List<User> users = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                User u = buildUser(rs);
                if (u != null) users.add(u);
            }
        } catch (Exception e) {
            System.err.println("[UserDaoImpl] getAllUsers error: " + e.getMessage());
        }
        return users;
    }

    //BAN VÀ UNBAN
    @Override
    public boolean setUserStatus(String username, String status) {
        String sql = "UPDATE users SET status = ? WHERE username = ?";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status);
            stmt.setString(2, username);
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("[UserDaoImpl] setUserStatus error: " + e.getMessage());
        }
        return false;
    }

    //HÀM HỖ TRỢ
    private User buildUser(ResultSet rs) throws SQLException {
        String dbId   = rs.getString("id");
        String dbUser = rs.getString("username");
        String dbPass = rs.getString("password");
        String dbRole = rs.getString("role");

        // Đọc status — nếu cột chưa tồn tại thì mặc định "active"
        String dbStatus = "active";
        try { dbStatus = rs.getString("status"); } catch (SQLException ignored) {}

        User user = null;
        switch (dbRole != null ? dbRole : "") {
            case "ADMIN":  user = new Admin(dbId, dbUser, dbPass);            break;
            case "SELLER": user = new Seller(dbId, dbUser, dbPass);           break;
            case "BIDDER": user = new Bidder(dbId, dbUser, dbPass, 0);        break;
            default: System.err.println("Unknown role: " + dbRole);
        }

        if (user != null) {
            user.setRole(dbRole != null ? dbRole : "");
        }
        return user;
    }

    public List<UserInfo> getAllUserInfos() {
        String sql = "SELECT id, username, role, status FROM users ORDER BY role, username";
        List<UserInfo> list = new ArrayList<>();

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                UserInfo ui = new UserInfo();
                ui.id       = rs.getString("id");
                ui.username = rs.getString("username");
                ui.role     = rs.getString("role");
                try { ui.status = rs.getString("status"); } catch (SQLException e) { ui.status = "active"; }
                if (ui.status == null) ui.status = "active";
                list.add(ui);
            }
        } catch (Exception e) {
            System.err.println("[UserDaoImpl] getAllUserInfos error: " + e.getMessage());
        }
        return list;
    }

    public static class UserInfo {
        public String id;
        public String username;
        public String role;
        public String status;
    }
}