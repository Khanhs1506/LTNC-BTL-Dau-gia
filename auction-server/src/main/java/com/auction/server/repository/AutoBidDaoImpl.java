package com.auction.server.repository;

import com.auction.server.model.AutoBidEntry;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AutoBidDaoImpl implements IAutoBidDAO {

    @Override
    public boolean insertAutoBid(AutoBidEntry entry) {
        // Nếu đã tồn tại thì thay thế (cập nhật maxBid / increment mới)
        String sql = "INSERT INTO auto_bids (auction_id, username, max_bid, increment_amount, registered_at) " +
                "VALUES (?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE max_bid = VALUES(max_bid), " +
                "increment_amount = VALUES(increment_amount), " +
                "registered_at = VALUES(registered_at)";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt   (1, entry.getAuctionId());
            stmt.setString(2, entry.getUsername());
            stmt.setDouble(3, entry.getMaxBid());
            stmt.setDouble(4, entry.getIncrement());
            stmt.setTimestamp(5, Timestamp.valueOf(entry.getRegisteredAt()));

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                System.out.println("[AutoBidDao] Đã lưu: " + entry);
                return true;
            }

        } catch (Exception e) {
            System.err.println("[AutoBidDao] Lỗi insertAutoBid: " + e.getMessage());
        }
        return false;
    }

    @Override
    public boolean deleteAutoBid(int auctionId, String username) {
        String sql = "DELETE FROM auto_bids WHERE auction_id = ? AND username = ?";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt   (1, auctionId);
            stmt.setString(2, username);
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                System.out.println("[AutoBidDao] Đã xóa auto-bid: auction=" + auctionId + " user=" + username);
                return true;
            }

        } catch (Exception e) {
            System.err.println("[AutoBidDao] Lỗi deleteAutoBid: " + e.getMessage());
        }
        return false;
    }

    @Override
    public List<AutoBidEntry> getAutoBidsByAuction(int auctionId) {
        String sql = "SELECT * FROM auto_bids WHERE auction_id = ? ORDER BY max_bid DESC, registered_at ASC";
        List<AutoBidEntry> list = new ArrayList<>();

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, auctionId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(buildEntry(rs));
                }
            }

        } catch (Exception e) {
            System.err.println("[AutoBidDao] Lỗi getAutoBidsByAuction: " + e.getMessage());
        }
        return list;
    }

    @Override
    public boolean existsAutoBid(int auctionId, String username) {
        String sql = "SELECT 1 FROM auto_bids WHERE auction_id = ? AND username = ? LIMIT 1";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt   (1, auctionId);
            stmt.setString(2, username);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }

        } catch (Exception e) {
            System.err.println("[AutoBidDao] Lỗi existsAutoBid: " + e.getMessage());
        }
        return false;
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private AutoBidEntry buildEntry(ResultSet rs) throws SQLException {
        int    auctionId    = rs.getInt("auction_id");
        String username     = rs.getString("username");
        double maxBid       = rs.getDouble("max_bid");
        double increment    = rs.getDouble("increment_amount");
        java.time.LocalDateTime registeredAt =
                rs.getTimestamp("registered_at").toLocalDateTime();
        return new AutoBidEntry(auctionId, username, maxBid, increment, registeredAt);
    }
}