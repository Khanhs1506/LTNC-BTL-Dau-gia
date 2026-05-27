package com.auction.server.repository;

import com.auction.server.model.*;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AuctionDaoImpl implements IAuctionDAO {

    private static final String JOIN_SQL =
            "SELECT a.*, i.name, i.item_type, i.startingPrice, i.currentHighestBid, " +
                    "e.warranty_months, art.artist_name, v.brand, v.year " +
                    "FROM auctions a " +
                    "JOIN Items i ON a.item_id = i.id " +
                    "LEFT JOIN Electronics_Items e   ON i.id = e.item_id " +
                    "LEFT JOIN Art_Items         art ON i.id = art.item_id " +
                    "LEFT JOIN Vehicle_Items     v   ON i.id = v.item_id ";

    // Dựng Item từ ResultSet đã JOIN sẵn — không mở connection mới
    private Item buildItemFromRow(ResultSet rs) throws SQLException {
        int    itemId     = rs.getInt("item_id");
        String name       = rs.getString("name");
        String itemType   = rs.getString("item_type");
        double startPrice = rs.getDouble("startingPrice");
        double highestBid = rs.getDouble("currentHighestBid");

        Item item;
        switch (itemType) {
            case "ART":
                item = new ArtItem(String.valueOf(itemId), name, startPrice,
                        rs.getString("artist_name"));
                break;
            case "ELECTRONICS":
                item = new ElectronicsItem(String.valueOf(itemId), name, startPrice,
                        rs.getInt("warranty_months"));
                break;
            case "VEHICLE":
                item = new VehicleItem(String.valueOf(itemId), name, startPrice,
                        rs.getString("brand"), rs.getInt("year"));
                break;
            default:
                System.err.println("[AuctionDaoImpl] Loại không hợp lệ: " + itemType);
                return null;
        }
        item.setCurrentHighestBid(highestBid);
        return item;
    }

    // Dựng Auction từ ResultSet đã JOIN sẵn — không mở connection mới
    private Auction buildAuctionFromRow(ResultSet rs) throws SQLException {
        Item item = buildItemFromRow(rs);
        if (item == null) return null;

        int           id        = rs.getInt("id");
        double        highBid   = rs.getDouble("current_highest_bid");
        String        winner    = rs.getString("current_winner_username");
        LocalDateTime startTime = rs.getTimestamp("start_time").toLocalDateTime();
        LocalDateTime endTime   = rs.getTimestamp("end_time").toLocalDateTime();
        String        statusStr = rs.getString("status");

        Auction auction = new Auction(id, item, startTime, endTime);
        item.setCurrentHighestBid(highBid);
        auction.updateHighestBid(highBid, winner);
        auction.updateStatus(Auction.Status.valueOf(statusStr));
        return auction;
    }

    @Override
    public Auction getAuctionById(int id) {
        String sql = JOIN_SQL + "WHERE a.id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return buildAuctionFromRow(rs);
            }
        } catch (Exception e) {
            System.err.println("[AuctionDaoImpl] Lỗi getAuctionById: " + e.getMessage());
        }
        return null;
    }

    @Override
    public List<Auction> getAllAuctions() {
        String sql = JOIN_SQL + "ORDER BY a.created_at DESC";
        List<Auction> auctions = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Auction auction = buildAuctionFromRow(rs);
                if (auction != null) auctions.add(auction);
            }
        } catch (Exception e) {
            System.err.println("[AuctionDaoImpl] Lỗi getAllAuctions: " + e.getMessage());
        }
        return auctions;
    }

    @Override
    public List<Auction> getAuctionsByStatus(Auction.Status status) {
        String sql = JOIN_SQL + "WHERE a.status = ? ORDER BY a.created_at DESC";
        List<Auction> auctions = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status.name());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Auction auction = buildAuctionFromRow(rs);
                    if (auction != null) auctions.add(auction);
                }
            }
        } catch (Exception e) {
            System.err.println("[AuctionDaoImpl] Lỗi getAuctionsByStatus: " + e.getMessage());
        }
        return auctions;
    }

    @Override
    public List<Auction> getAuctionsBySellerId(String sellerId) {
        String sql = JOIN_SQL + "WHERE i.seller_id = ? ORDER BY a.created_at DESC";
        List<Auction> auctions = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, sellerId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Auction auction = buildAuctionFromRow(rs);
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
        // Lấy startingPrice trong 1 query nhỏ — chỉ dùng 1 connection
        double startingPrice = 0;
        String sqlPrice = "SELECT startingPrice FROM Items WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sqlPrice)) {
            stmt.setInt(1, itemId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) startingPrice = rs.getDouble("startingPrice");
                else {
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
}