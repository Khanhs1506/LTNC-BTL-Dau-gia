package com.auction.server.repository;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

public class AdminStatsDaoImpl implements IAdminStatsDAO {
    @Override
    public JsonObject getAdminStats() {
        JsonObject result = new JsonObject();

        try (Connection conn = DatabaseManager.getInstance().getConnection()) {

            // 1. Tổng doanh thu Admin (Động theo phí sàn)
            double totalRevenue = 0;
            String sqlRevenue = "SELECT COALESCE(SUM(current_highest_bid), 0) * " +
                    "(SELECT CAST(setting_value AS DECIMAL(10,2)) FROM system_settings WHERE setting_key = 'PLATFORM_FEE_RATE') " +
                    "AS total_admin FROM auctions WHERE status = 'FINISHED'";
            try (PreparedStatement ps = conn.prepareStatement(sqlRevenue);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) totalRevenue = rs.getDouble("total_admin");
            }

            // 2. Số phiên đã thanh toán và chưa thanh toán
            int paidCount = 0, unpaidCount = 0;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT status, COUNT(*) AS cnt FROM auctions GROUP BY status");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String status = rs.getString("status");
                    int cnt = rs.getInt("cnt");
                    if ("FINISHED".equalsIgnoreCase(status)) paidCount += cnt;
                    else if ("OPEN".equalsIgnoreCase(status) || "RUNNING".equalsIgnoreCase(status)) unpaidCount += cnt;
                }
            }

            // 3. Doanh thu từng tháng (Động theo phí sàn)
            JsonArray monthlyArr = new JsonArray();
            String sqlMonthly = "SELECT MONTH(end_time) AS m, COALESCE(SUM(current_highest_bid), 0) * " +
                    "(SELECT CAST(setting_value AS DECIMAL(10,2)) FROM system_settings WHERE setting_key = 'PLATFORM_FEE_RATE') " +
                    "AS rev_admin FROM auctions " +
                    "WHERE status = 'FINISHED' AND YEAR(end_time) = YEAR(CURDATE()) " +
                    "GROUP BY MONTH(end_time) ORDER BY m";

            try (PreparedStatement ps = conn.prepareStatement(sqlMonthly);
                 ResultSet rs = ps.executeQuery()) {
                Map<Integer, Double> map = new LinkedHashMap<>();
                for (int i = 1; i <= 12; i++) map.put(i, 0.0);
                while (rs.next()) {
                    map.put(rs.getInt("m"), rs.getDouble("rev_admin"));
                }
                int currentMonth = LocalDate.now().getMonthValue();
                for (int i = 1; i <= currentMonth; i++) {
                    JsonObject mo = new JsonObject();
                    mo.addProperty("month", "Th." + i);
                    mo.addProperty("revenue", map.get(i));
                    monthlyArr.add(mo);
                }
            }
            // Đóng gói tất cả vào JsonObject
            result.addProperty("totalRevenue", totalRevenue);
            result.addProperty("paidCount", paidCount);
            result.addProperty("unpaidCount", unpaidCount);
            result.add("monthlyRevenue", monthlyArr);
        } catch (Exception e) {
            System.err.println("[AdminStatsDaoImpl] Lỗi truy xuất thống kê: " + e.getMessage());
        }
        return result;
    }
}
