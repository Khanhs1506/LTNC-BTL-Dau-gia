package com.auction.server.repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FavoriteDaoImpl implements IFavoriteDAO {

    @Override
    public boolean addFavorite(String username, int auctionId) {
        String sql = "INSERT IGNORE INTO favorites (username, auction_id) VALUES (?, ?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setInt(2, auctionId);
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("[FavoriteDaoImpl] Lỗi addFavorite: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean removeFavorite(String username, int auctionId) {
        String sql = "DELETE FROM favorites WHERE username = ? AND auction_id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setInt(2, auctionId);
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("[FavoriteDaoImpl] Lỗi removeFavorite: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean isFavorite(String username, int auctionId) {
        String sql = "SELECT 1 FROM favorites WHERE username = ? AND auction_id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setInt(2, auctionId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            System.err.println("[FavoriteDaoImpl] Lỗi isFavorite: " + e.getMessage());
            return false;
        }
    }

    @Override
    public List<Integer> getFavoriteAuctionIds(String username) {
        String sql = "SELECT auction_id FROM favorites WHERE username = ?";
        List<Integer> ids = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) ids.add(rs.getInt("auction_id"));
            }
        } catch (Exception e) {
            System.err.println("[FavoriteDaoImpl] Lỗi getFavoriteAuctionIds: " + e.getMessage());
        }
        return ids;
    }
}