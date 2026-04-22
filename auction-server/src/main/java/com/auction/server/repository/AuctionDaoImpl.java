package com.auction.server.repository;

import com.auction.server.model.Auction;
import com.auction.server.model.Item;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AuctionDaoImpl implements IAuctionDAO {

    // Dùng ItemDaoImpl để lấy Item khi cần dựng Auction
    private final IItemDAO itemDAO = new ItemDaoImpl();

    // Dựng object Auction từ 1 dòng ResultSet
    private Auction buildAuction(ResultSet rs) throws SQLException {
        int    id        = rs.getInt("id");
        int    itemId    = rs.getInt("item_id");
        double highBid   = rs.getDouble("current_highest_bid");
        String winner    = rs.getString("current_winner_username");
        LocalDateTime startTime = rs.getTimestamp("start_time").toLocalDateTime();
        LocalDateTime endTime   = rs.getTimestamp("end_time").toLocalDateTime();
        String statusStr = rs.getString("status");

        // Lấy Item tương ứng
        Item item = itemDAO.getItemById(itemId);
        if (item == null) {
            System.err.println("[AuctionDaoImpl] Không tìm thấy Item với id = " + itemId);
            return null;
        }

        Auction auction = new Auction(id, item, startTime, endTime);

        // Đồng bộ giá và trạng thái từ DB vào object
        item.setCurrentHighestBid(highBid);
        //auction.updateHighestBid(highBid, winner);
        //auction.updateStatus(Auction.Status.valueOf(statusStr));

        return auction;
    }

    @Override
    public Auction getAuctionById(int id) {
        String sql = "SELECT * FROM auctions WHERE id = ?";

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
    public List<Auction> getAllAuctions() {
        String sql = "SELECT * FROM auctions ORDER BY created_at DESC";
        List<Auction> auctions = new ArrayList<>();

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Auction auction = buildAuction(rs);
                if (auction != null) auctions.add(auction);
            }
        } catch (Exception e) {
            System.err.println("[AuctionDaoImpl] Lỗi getAllAuctions: " + e.getMessage());
        }
        return auctions;
    }

    @Override
    public List<Auction> getAuctionsByStatus(Auction.Status status) {
        String sql = "SELECT * FROM auctions WHERE status = ? ORDER BY created_at DESC";
        List<Auction> auctions = new ArrayList<>();

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status.name());
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
    public int insertAuction(int itemId, LocalDateTime startTime, LocalDateTime endTime) {
        // Lấy startingPrice của item để set làm current_highest_bid ban đầu
        Item item = itemDAO.getItemById(itemId);
        if (item == null) {
            System.err.println("[AuctionDaoImpl] Không tìm thấy Item id = " + itemId);
            return -1;
        }

        String sql = "INSERT INTO auctions (item_id, current_highest_bid, start_time, end_time, status) " +
                "VALUES (?, ?, ?, ?, 'OPEN')";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, itemId);
            stmt.setDouble(2, item.getStartingPrice());
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
}
