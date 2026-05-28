package com.auction.server.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ReportDaoImpl implements IReportDAO {

    @Override
    public int insertReport(String reporterUsername, String targetUsername, String reason) {
        String sql = "INSERT INTO reports (reporter_username, target_username, reason) VALUE (?, ?, ?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, reporterUsername);
            stmt.setString(2, targetUsername);
            stmt.setString(3, reason);
            int row = stmt.executeUpdate();

            if (row > 0) {
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) return keys.getInt(1);
                }
            }
        } catch (Exception e) {
            System.err.println("[ReportDaoImpl] insertReport error: " + e.getMessage());
        }
        return -1;
    }

    @Override
    public List<ReportInfo> getAllReports() {
        String sql = "SELECT id, reporter_username, target_username, reason, " +
                "DATE_FORMAT(created_at, '%d/%m/%Y %H:%i') AS created_at, status " +
                "FROM reports ORDER BY created_at DESC";
        List<ReportInfo> list = new ArrayList<>();

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                ReportInfo r = new ReportInfo();
                r.id = rs.getInt("id");
                r.reporterUsername = rs.getString("reporter_username");
                r.targetUsername = rs.getString("target_username");
                r.reason = rs.getString("reason");
                r.createdAt = rs.getString("created_at");
                r.status = rs.getString("status");
                list.add(r);
            }
        } catch (Exception e) {
            System.err.println("[ReportDaoImpl] getAllReports error: " + e.getMessage());
        }
        return list;
    }

    @Override
    public boolean resolveReport(int reportId) {
        String sql = "UPDATE reports SET status = 'RESOLVED' WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, reportId);
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("[ReportDaoImpl] resolveReport error: " + e.getMessage());
        }
        return false;
    }
}
