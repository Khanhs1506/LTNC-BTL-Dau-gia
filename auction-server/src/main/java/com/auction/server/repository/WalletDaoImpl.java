
package com.auction.server.repository;

import com.auction.server.model.WalletTransaction;
import com.auction.server.model.WalletTransaction.Type;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Triển khai IWalletDAO.
 *
 * Mọi thao tác thay đổi số dư đều chạy trong một transaction DB:
 *   1. SELECT ... FOR UPDATE để khóa hàng user, tránh race condition.
 *   2. Kiểm tra điều kiện (đủ tiền, …).
 *   3. UPDATE users.balance.
 *   4. INSERT wallet_transactions.
 *   5. COMMIT — hoặc ROLLBACK nếu bất kỳ bước nào thất bại.
 */
public class WalletDaoImpl implements IWalletDAO {

    //LẤY SỐ DƯ
    @Override
    public double getBalance(String userId) {
        String sql = "SELECT balance FROM users WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble("balance");
            }
        } catch (Exception e) {
            System.err.println("[WalletDAO] getBalance lỗi: " + e.getMessage());
        }
        return -1;
    }

    //NẠP TIỀN
    @Override
    public WalletTransaction deposit(String userId, double amount, String note) {
        if (amount <= 0) {
            System.err.println("[WalletDAO] deposit: amount phải > 0");
            return null;
        }
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Khóa và lấy số dư hiện tại
                double balanceBefore = lockAndGetBalance(conn, userId);
                if (balanceBefore < 0) {
                    conn.rollback();
                    System.err.println("[WalletDAO] deposit: không tìm thấy user " + userId);
                    return null;
                }
                double balanceAfter = balanceBefore + amount;

                // 2. Cộng tiền vào ví
                updateBalance(conn, userId, balanceAfter);

                // 3. Ghi lịch sử giao dịch
                String txId = UUID.randomUUID().toString();
                insertTransaction(conn, txId, userId, Type.DEPOSIT, amount, balanceBefore, balanceAfter, null, note);
                conn.commit();
                System.out.printf("[WalletDAO] DEPOSIT: user=%s | +%.0f | sau=%.0f%n", userId, amount, balanceAfter);
                return buildTransaction(txId, userId, Type.DEPOSIT, amount, balanceBefore, balanceAfter, null, note);

            } catch (Exception e) {
                conn.rollback();
                System.err.println("[WalletDAO] deposit rollback: " + e.getMessage());
                return null;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            System.err.println("[WalletDAO] deposit lỗi kết nối: " + e.getMessage());
            return null;
        }
    }

    //TRỪ TIỀN KHI THANH TOÁN
    @Override
    public WalletTransaction payment(String userId, double amount, int auctionId, String note) {
        if (amount <= 0) {
            System.err.println("[WalletDAO] payment: amount phải > 0");
            return null;
        }
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try {
                double balanceBefore = lockAndGetBalance(conn, userId);
                if (balanceBefore < 0) {
                    conn.rollback();
                    System.err.println("[WalletDAO] payment: không tìm thấy user " + userId);
                    return null;
                }
                // Kiểm tra đủ tiền
                if (balanceBefore < amount) {
                    conn.rollback();
                    System.err.printf("[WalletDAO] payment: không đủ tiền (cần=%.0f, có=%.0f)%n", amount, balanceBefore);
                    return null;
                }

                double balanceAfter = balanceBefore - amount;
                updateBalance(conn, userId, balanceAfter);
                String txId = UUID.randomUUID().toString();
                insertTransaction(conn, txId, userId, Type.PAYMENT, amount, balanceBefore, balanceAfter, auctionId, note);
                conn.commit();
                System.out.printf("[WalletDAO] PAYMENT: user=%s | -%.0f | sau=%.0f | auction=%d%n", userId, amount, balanceAfter, auctionId);

                return buildTransaction(txId, userId, Type.PAYMENT, amount, balanceBefore, balanceAfter, auctionId, note);

            } catch (Exception e) {
                conn.rollback();
                System.err.println("[WalletDAO] payment rollback: " + e.getMessage());
                return null;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            System.err.println("[WalletDAO] payment lỗi kết nối: " + e.getMessage());
            return null;
        }
    }

    //HOÀN TIỀN KHI THUA HOẶC HỦY PHIÊN
    @Override
    public WalletTransaction refund(String userId, double amount, Integer auctionId, String note) {
        if (amount <= 0) {
            System.err.println("[WalletDAO] refund: amount phải > 0");
            return null;
        }
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try {
                double balanceBefore = lockAndGetBalance(conn, userId);
                if (balanceBefore < 0) {
                    conn.rollback();
                    System.err.println("[WalletDAO] refund: không tìm thấy user " + userId);
                    return null;
                }

                double balanceAfter = balanceBefore + amount;
                updateBalance(conn, userId, balanceAfter);
                String txId = UUID.randomUUID().toString();
                insertTransaction(conn, txId, userId, Type.REFUND, amount, balanceBefore, balanceAfter, auctionId, note);

                conn.commit();
                System.out.printf("[WalletDAO] REFUND: user=%s | +%.0f | sau=%.0f%n", userId, amount, balanceAfter);
                return buildTransaction(txId, userId, Type.REFUND, amount, balanceBefore, balanceAfter, auctionId, note);

            } catch (Exception e) {
                conn.rollback();
                System.err.println("[WalletDAO] refund rollback: " + e.getMessage());
                return null;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            System.err.println("[WalletDAO] refund lỗi kết nối: " + e.getMessage());
            return null;
        }
    }

    //GIỮ TIỀN CỌC KHI ĐẶT GIÁ
    @Override
    public WalletTransaction bidHold(String userId, double amount, int auctionId, String note) {
        if (amount <= 0) {
            System.err.println("[WalletDAO] bidHold: amount phải > 0");
            return null;
        }
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try {
                double balanceBefore = lockAndGetBalance(conn, userId);
                if (balanceBefore < 0) {
                    conn.rollback();
                    System.err.println("[WalletDAO] bidHold: không tìm thấy user " + userId);
                    return null;
                }

                // Kiểm tra đủ tiền để đặt cọc
                if (balanceBefore < amount) {
                    conn.rollback();
                    System.err.printf("[WalletDAO] bidHold: không đủ tiền cọc (cần=%.0f, có=%.0f)%n", amount, balanceBefore);
                    return null;
                }

                double balanceAfter = balanceBefore - amount;
                updateBalance(conn, userId, balanceAfter);
                String txId = UUID.randomUUID().toString();
                insertTransaction(conn, txId, userId, Type.BID_HOLD, amount, balanceBefore, balanceAfter, auctionId, note);
                conn.commit();
                System.out.printf("[WalletDAO] BID_HOLD: user=%s | -%.0f | sau=%.0f | auction=%d%n", userId, amount, balanceAfter, auctionId);
                return buildTransaction(txId, userId, Type.BID_HOLD, amount, balanceBefore, balanceAfter, auctionId, note);

            } catch (Exception e) {
                conn.rollback();
                System.err.println("[WalletDAO] bidHold rollback: " + e.getMessage());
                return null;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            System.err.println("[WalletDAO] bidHold lỗi kết nối: " + e.getMessage());
            return null;
        }
    }

    //HOÀN TIỀN CỌC KHI THUA
    @Override
    public WalletTransaction bidRelease(String userId, double amount, int auctionId, String note) {
        if (amount <= 0) {
            System.err.println("[WalletDAO] bidRelease: amount phải > 0");
            return null;
        }
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try {
                double balanceBefore = lockAndGetBalance(conn, userId);
                if (balanceBefore < 0) {
                    conn.rollback();
                    System.err.println("[WalletDAO] bidRelease: không tìm thấy user " + userId);
                    return null;
                }
                double balanceAfter = balanceBefore + amount;
                updateBalance(conn, userId, balanceAfter);
                String txId = UUID.randomUUID().toString();
                insertTransaction(conn, txId, userId, Type.BID_RELEASE, amount, balanceBefore, balanceAfter, auctionId, note);
                conn.commit();
                System.out.printf("[WalletDAO] BID_RELEASE: user=%s | +%.0f | sau=%.0f | auction=%d%n", userId, amount, balanceAfter, auctionId);
                return buildTransaction(txId, userId, Type.BID_RELEASE, amount, balanceBefore, balanceAfter, auctionId, note);

            } catch (Exception e) {
                conn.rollback();
                System.err.println("[WalletDAO] bidRelease rollback: " + e.getMessage());
                return null;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            System.err.println("[WalletDAO] bidRelease lỗi kết nối: " + e.getMessage());
            return null;
        }
    }

    //LẤY LỊCH SỬ GIAO DỊCH
    @Override
    public List<WalletTransaction> getTransactionHistory(String userId, int limit) {
        List<WalletTransaction> list = new ArrayList<>();

        String sql = "SELECT id, user_id, type, amount, balance_before, balance_after, " +
                "       related_auction_id, note, created_at " +
                "FROM wallet_transactions " +
                "WHERE user_id = ? " +
                "ORDER BY created_at DESC" +
                (limit > 0 ? " LIMIT ?" : "");

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, userId);
            if (limit > 0) ps.setInt(2, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Integer auctionId = rs.getObject("related_auction_id") != null ? rs.getInt("related_auction_id") : null;

                    WalletTransaction tx = new WalletTransaction(
                            rs.getString("id"),
                            rs.getString("user_id"),
                            Type.valueOf(rs.getString("type")),
                            rs.getDouble("amount"),
                            rs.getDouble("balance_before"),
                            rs.getDouble("balance_after"),
                            auctionId,
                            rs.getString("note"),
                            rs.getTimestamp("created_at").toLocalDateTime());
                    list.add(tx);
                }
            }
        } catch (Exception e) {
            System.err.println("[WalletDAO] getTransactionHistory lỗi: " + e.getMessage());
        }
        return list;
    }

    //HÀM HỖ TRỢ KHÓA LÀ LẤY SỐ DƯ
    private double lockAndGetBalance(Connection conn, String userId) throws SQLException {
        String sql = "SELECT balance FROM users WHERE id = ? FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble("balance");
            }
        }
        return -1;
    }

    //CẬP NHẬT SỐ DƯ
    private void updateBalance(Connection conn, String userId, double newBalance)
            throws SQLException {
        String sql = "UPDATE users SET balance = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, newBalance);
            ps.setString(2, userId);
            ps.executeUpdate();
        }
    }

    //THÊM VÀO DATABASE
    private void insertTransaction(Connection conn, String txId, String userId,
                                   Type type, double amount,
                                   double balanceBefore, double balanceAfter,
                                   Integer relatedAuctionId, String note)
            throws SQLException {

        String sql = "INSERT INTO wallet_transactions " +
                "(id, user_id, type, amount, balance_before, balance_after, " +
                " related_auction_id, note, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, txId);
            ps.setString(2, userId);
            ps.setString(3, type.name());
            ps.setDouble(4, amount);
            ps.setDouble(5, balanceBefore);
            ps.setDouble(6, balanceAfter);
            if (relatedAuctionId != null) {
                ps.setInt(7, relatedAuctionId);
            } else {
                ps.setNull(7, Types.INTEGER);
            }
            ps.setString(8, note);
            ps.executeUpdate();
        }
    }

    private WalletTransaction buildTransaction(String txId, String userId,
                                               Type type, double amount,
                                               double before, double after,
                                               Integer auctionId, String note) {
        return new WalletTransaction(txId, userId, type, amount, before, after,
                auctionId, note, LocalDateTime.now());
    }
}