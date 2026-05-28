package com.auction.server.repository;

import com.auction.server.model.*;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AuctionDaoImpl implements IAuctionDAO {

    private static final int MAX_PAGE_SIZE = 200;

    private static final String BASE_JOIN_QUERY =
            "SELECT a.id AS a_id, a.item_id, a.current_highest_bid AS a_high_bid, " +
                    "a.current_winner_username, a.start_time, a.end_time, a.status, a.created_at, " +
                    "i.name, i.item_type, i.startingPrice, i.currentHighestBid AS i_high_bid, i.seller_id, " +
                    "e.warranty_months, art.artist_name, v.brand, v.year " +
                    "FROM auctions a " +
                    "JOIN Items i ON a.item_id = i.id " +
                    "LEFT JOIN Electronics_Items e ON i.id = e.item_id " +
                    "LEFT JOIN Art_Items art ON i.id = art.item_id " +
                    "LEFT JOIN Vehicle_Items v ON i.id = v.item_id ";

    // Dựng object Auction từ 1 dòng ResultSet
    private Auction buildAuction(ResultSet rs) throws SQLException {
        // 1. Đọc thông tin Auction từ alias
        int auctionId = rs.getInt("a_id");
        int itemId = rs.getInt("item_id");
        double highBid = rs.getDouble("a_high_bid");
        String winner = rs.getString("current_winner_username");
        LocalDateTime startTime = rs.getTimestamp("start_time").toLocalDateTime();
        LocalDateTime endTime = rs.getTimestamp("end_time").toLocalDateTime();
        String statusStr = rs.getString("status");

        // 2. Đọc thông tin Item ngay trên cùng 1 ResultSet
        String name = rs.getString("name");
        String itemType = rs.getString("item_type");
        double startingPrice = rs.getDouble("startingPrice");
        double itemHighBid = rs.getDouble("i_high_bid");

        Item item = null;
        switch (itemType) {
            case "ELECTRONICS":
                item = new ElectronicsItem(String.valueOf(itemId), name, startingPrice, rs.getInt("warranty_months"));
                break;
            case "ART":
                item = new ArtItem(String.valueOf(itemId), name, startingPrice, rs.getString("artist_name"));
                break;
            case "VEHICLE":
                item = new VehicleItem(String.valueOf(itemId), name, startingPrice, rs.getString("brand"), rs.getInt("year"));
                break;
            default:
                System.err.println("[AuctionDaoImpl] Loại sản phẩm không hợp lệ: " + itemType);
        }

        if (item != null) {
            item.setCurrentHighestBid(itemHighBid);
        }

        Auction auction = new Auction(auctionId, item, startTime, endTime);
        auction.updateHighestBid(highBid, winner);
        auction.updateStatus(Auction.Status.valueOf(statusStr));
        return auction;
    }

    private int sanitizeLimit(int limit) {
        if (limit <= 0) {
            return IAuctionDAO.DEFAULT_PAGE_SIZE; // Đảm bảo IAuctionDAO có biến này
        }
        return Math.min(limit, MAX_PAGE_SIZE);
    }

    private int sanitizeOffset(int offset) {
        return Math.max(offset, 0);
    }

    @Override
    public Auction getAuctionById(int id) {
        String sql = BASE_JOIN_QUERY + " WHERE a.id = ?";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return buildAuction(rs);
                }
            }
        } catch (Exception e) {
            System.err.println("[AuctionDaoImpl] Lỗi getAuctionById: " + e.getMessage());
        }
        return null;
    }

    @Override
    public List<Auction> getAllAuctions(int limit, int offset) {
        String sql = BASE_JOIN_QUERY + " ORDER BY a.created_at DESC LIMIT ? OFFSET ?";
        List<Auction> auctions = new ArrayList<>();

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, sanitizeLimit(limit));
            stmt.setInt(2, sanitizeOffset(offset));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Auction auction = buildAuction(rs);
                    if (auction != null) auctions.add(auction);
                }
            }
        } catch (Exception e) {
            System.err.println("[AuctionDaoImpl] Lỗi getAllAuctions: " + e.getMessage());
        }
        return auctions;
    }

    @Override
    public List<Auction> getAuctionsByStatus(Auction.Status status, int limit, int offset) {
        String sql = BASE_JOIN_QUERY + " WHERE a.status = ? ORDER BY a.created_at DESC LIMIT ? OFFSET ?";
        List<Auction> auctions = new ArrayList<>();

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status.name());
            stmt.setInt(2, sanitizeLimit(limit));
            stmt.setInt(3, sanitizeOffset(offset));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Auction auction = buildAuction(rs);
                    if (auction != null) auctions.add(auction);
                }
            }
        } catch (Exception e) {
            System.err.println("[AuctionDaoImpl] Lỗi getAuctionsByStatus: " + e.getMessage());
        }
        return auctions;
    }

    @Override
    public List<Auction> getAuctionsBySellerId(String sellerId, int limit, int offset) {
        String sql = BASE_JOIN_QUERY + " WHERE i.seller_id = ? ORDER BY a.created_at DESC LIMIT ? OFFSET ?";
        List<Auction> auctions = new ArrayList<>();

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, sellerId);
            stmt.setInt(2, sanitizeLimit(limit));
            stmt.setInt(3, sanitizeOffset(offset));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Auction auction = buildAuction(rs);
                    if (auction != null) auctions.add(auction);
                }
            }
        } catch (Exception e) {
            System.err.println("[AuctionDaoImpl] Lỗi getAuctionsBySellerId: " + e.getMessage());
        }
        return auctions;
    }

    @Override
    public int insertAuction(int itemId, LocalDateTime startTime, LocalDateTime endTime) {
        // Lấy startingPrice trong 1 query nhỏ — chỉ dùng 1 connection, không phụ thuộc ItemDAO ngoài
        double startingPrice = 0;
        String sqlPrice = "SELECT startingPrice FROM Items WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sqlPrice)) {
            stmt.setInt(1, itemId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    startingPrice = rs.getDouble("startingPrice");
                } else {
                    System.err.println("[AuctionDaoImpl] Không tìm thấy Item id = " + itemId);
                    return -1;
                }
            }
        } catch (Exception e) {
            System.err.println("[AuctionDaoImpl] Lỗi lấy startingPrice: " + e.getMessage());
            return -1;
        }

        String sql = "INSERT INTO auctions (item_id, current_highest_bid, start_time, end_time, status) " +
                "VALUES (?, ?, ?, ?, 'OPEN')";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, itemId);
            stmt.setDouble(2, startingPrice);
            stmt.setTimestamp(3, Timestamp.valueOf(startTime));
            stmt.setTimestamp(4, Timestamp.valueOf(endTime));
            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int generatedId = generatedKeys.getInt(1);
                    System.out.println("[AuctionDaoImpl] Tạo phiên đấu giá thành công, id = " + generatedId);
                    return generatedId;
                }
            }
        } catch (Exception e) {
            System.err.println("[AuctionDaoImpl] Lỗi insertAuction: " + e.getMessage());
        }
        return -1;
    }

    @Override
    public boolean updateStatus(int auctionId, Auction.Status status) {
        String sql = "UPDATE auctions SET status = ? WHERE id = ?";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status.name());
            stmt.setInt(2, auctionId);
            return stmt.executeUpdate() > 0;

        } catch (Exception e) {
            System.err.println("[AuctionDaoImpl] Lỗi updateStatus: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean updateEndTime(int auctionId, LocalDateTime newEndTime) {
        String sql = "UPDATE auctions SET end_time = ? WHERE id = ?";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, Timestamp.valueOf(newEndTime));
            stmt.setInt(2, auctionId);
            return stmt.executeUpdate() > 0;

        } catch (Exception e) {
            System.err.println("[AuctionDaoImpl] Lỗi updateEndTime: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean updateHighestBid(int auctionId, double newBid, String winnerUsername) {
        String sql = "UPDATE auctions SET current_highest_bid = ?, current_winner_username = ? WHERE id = ?";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDouble(1, newBid);
            stmt.setString(2, winnerUsername);
            stmt.setInt(3, auctionId);
            return stmt.executeUpdate() > 0;

        } catch (Exception e) {
            System.err.println("[AuctionDaoImpl] Lỗi updateHighestBid: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean deleteAuctionByItemId(int itemId) {
        String sql = "DELETE FROM auctions WHERE item_id = ?";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, itemId);
            int rows = stmt.executeUpdate();
            System.out.println("[AuctionDaoImpl] Xóa " + rows + " phiên đấu giá của item_id=" + itemId);
            return rows >= 0;

        } catch (Exception e) {
            System.err.println("[AuctionDaoImpl] Lỗi deleteAuctionByItemId: " + e.getMessage());
            return false;
        }
    }

    @Override
    public List<Integer> getSellerPaidAuctionIds(String sellerId) {
        List<Integer> paidAuctionIds = new ArrayList<>();
        String sql = "SELECT DISTINCT auction_id FROM (" +
                " SELECT spc.auction_id AS auction_id FROM seller_payment_confirmations spc WHERE spc.seller_id = ? " +
                " UNION " +
                " SELECT wt.related_auction_id AS auction_id FROM wallet_transactions wt " +
                " JOIN auctions a ON a.id = wt.related_auction_id " +
                " JOIN Items i ON i.id = a.item_id " +
                " WHERE wt.type = 'PAYMENT' AND wt.related_auction_id IS NOT NULL AND i.seller_id = ? ) t";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, sellerId);
            stmt.setString(2, sellerId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) paidAuctionIds.add(rs.getInt("auction_id"));
            }
        } catch (Exception e) {
            System.err.println("[AuctionDaoImpl] getSellerPaidAuctionIds: " + e.getMessage());
        }
        return paidAuctionIds;
    }

    @Override
    public boolean markAuctionPaid(int auctionId, String sellerId) {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            String validateSql = "SELECT a.id FROM auctions a JOIN Items i ON i.id = a.item_id " +
                    "WHERE a.id = ? AND i.seller_id = ? AND a.status = 'FINISHED' AND a.current_winner_username IS NOT NULL";
            try (PreparedStatement validate = conn.prepareStatement(validateSql)) {
                validate.setInt(1, auctionId);
                validate.setString(2, sellerId);
                try (ResultSet rs = validate.executeQuery()) {
                    if (!rs.next()) return false;
                }
            }
            String upsertSql = "INSERT INTO seller_payment_confirmations (auction_id, seller_id, confirmed_at) " +
                    "VALUES (?, ?, NOW()) ON DUPLICATE KEY UPDATE confirmed_at = VALUES(confirmed_at)";
            try (PreparedStatement upsert = conn.prepareStatement(upsertSql)) {
                upsert.setInt(1, auctionId);
                upsert.setString(2, sellerId);
                upsert.executeUpdate();
                return true;
            }
        } catch (Exception e) {
            System.err.println("[AuctionDaoImpl] markAuctionPaid: " + e.getMessage());
        }
        return false;
    }
}