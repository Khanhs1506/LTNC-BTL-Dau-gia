package com.auction.server.repository;

import com.auction.server.model.BidTransaction;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BidTransactionDaoImpl implements IBidTransactionDAO {
    private static final int MAX_PAGE_SIZE = 200;

    private int sanitizeLimit(int limit) {
        if (limit <= 0) {
            return IBidTransactionDAO.DEFAULT_PAGE_SIZE;
        }
        return Math.min(limit, MAX_PAGE_SIZE);
    }

    private int sanitizeOffset(int offset) {
        return Math.max(offset, 0);
    }

    @Override
    public boolean insertBid(BidTransaction transaction) {
        String sql = "INSERT INTO bid_transactions (transaction_id, auction_id, bidder_username, bid_amount, timestamp) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, transaction.getTransactionId());
            stmt.setInt(2, transaction.getAuctionId());
            stmt.setString(3, transaction.getBidderUsername());
            stmt.setDouble(4, transaction.getBidAmount());
            stmt.setTimestamp(5, Timestamp.valueOf(transaction.getTimestamp()));
            int rows = stmt.executeUpdate();

            if (rows > 0) {
                System.out.println("[BidTransactionDaoImpl] Lưu bid thành công: " + transaction);
                return true;
            }
        } catch (Exception e) {
            System.err.println("[BidTransactionDaoImpl] Lỗi insertBid: " + e.getMessage());
        }
        return false;
    }

    @Override
    public List<BidTransaction> getBidsByAuctionId(int auctionId) {
        String sql = "SELECT * FROM bid_transactions WHERE auction_id = ? ORDER BY timestamp ASC";
        List<BidTransaction> bids = new ArrayList<>();

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, auctionId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    BidTransaction bid = buildBidTransaction(rs);
                    if (bid != null) bids.add(bid);
                }
            }
        } catch (Exception e) {
            System.err.println("[BidTransactionDaoImpl] Lỗi getBidsByAuctionId: " + e.getMessage());
        }
        return bids;
    }

    @Override
    public BidTransaction getHighestBid(int auctionId) {
        String sql = "SELECT * FROM bid_transactions WHERE auction_id = ? ORDER BY bid_amount DESC LIMIT 1";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, auctionId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return buildBidTransaction(rs);
                }
            }
        } catch (Exception e) {
            System.err.println("[BidTransactionDaoImpl] Lỗi getHighestBid: " + e.getMessage());
        }
        return null;
    }

    //LẤY LỊCH SỬ ĐẶT GIÁ CHO ADMIN
    @Override
    public List<BidTransaction> getAllBids(int limit, int offset) {
        String sql = "SELECT * FROM bid_transactions ORDER BY timestamp DESC LIMIT ? OFFSET ?";
        List<BidTransaction> bids = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, sanitizeLimit(limit));
            stmt.setInt(2, sanitizeOffset(offset));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    BidTransaction bid = buildBidTransaction(rs);
                    if (bid != null) bids.add(bid);
                }
            }
        } catch (Exception e) {
            System.err.println("[BidTransactionDaoImpl] Lỗi getAllBids: " + e.getMessage());
        }
        return bids;
    }

    // Dựng BidTransaction từ DB — cần constructor đặc biệt trong BidTransaction.java
    private BidTransaction buildBidTransaction(ResultSet rs) throws SQLException {
        String        transactionId   = rs.getString("transaction_id");
        int           auctionId       = rs.getInt("auction_id");
        String        bidderUsername  = rs.getString("bidder_username");
        double        bidAmount       = rs.getDouble("bid_amount");
        java.time.LocalDateTime timestamp = rs.getTimestamp("timestamp").toLocalDateTime();
        return new BidTransaction(transactionId, auctionId, bidderUsername, bidAmount, timestamp);
    }
}