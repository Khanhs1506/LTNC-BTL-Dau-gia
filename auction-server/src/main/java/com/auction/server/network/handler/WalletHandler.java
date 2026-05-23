package server;

import com.google.gson.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Xử lý tất cả lệnh liên quan đến ví trên server.
 * Tích hợp vào ClientHandler: if (cmd.startsWith("GET_WALLET")) ...
 *
 * ── Schema DB cần tạo trước ──────────────────────────────────────────
 * Chạy wallet_schema.sql để tạo bảng wallets và transactions.
 */
public class WalletHandler {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ── GET_WALLET ────────────────────────────────────────────────────
    public static String handleGetWallet(String username, Connection conn)
            throws SQLException {

        // Tự tạo ví nếu chưa có
        ensureWalletExists(username, conn);

        String sql = """
            SELECT balance, total_deposited, total_held,
                   total_refunded, total_paid
            FROM wallets WHERE username = ?
            """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                JsonObject obj = new JsonObject();
                obj.addProperty("balance",        rs.getDouble("balance"));
                obj.addProperty("totalDeposited", rs.getDouble("total_deposited"));
                obj.addProperty("totalHeld",      rs.getDouble("total_held"));
                obj.addProperty("totalRefunded",  rs.getDouble("total_refunded"));
                obj.addProperty("totalPaid",      rs.getDouble("total_paid"));
                return "WALLET===" + obj;
            }
        }
        return "ERROR===Wallet not found";
    }

    // ── DEPOSIT ───────────────────────────────────────────────────────
    public static String handleDeposit(String username, String jsonBody,
                                       Connection conn) throws SQLException {
        JsonObject req    = JsonParser.parseString(jsonBody).getAsJsonObject();
        double     amount = req.get("amount").getAsDouble();
        String     method = req.get("paymentMethod").getAsString();

        if (amount <= 0) return "ERROR===Số tiền không hợp lệ";

        conn.setAutoCommit(false);
        try {
            // 1. Lấy số dư hiện tại
            double before = getBalance(username, conn);

            // 2. Cộng tiền vào ví
            String updSql = """
                UPDATE wallets
                SET balance = balance + ?,
                    total_deposited = total_deposited + ?
                WHERE username = ?
                """;
            try (PreparedStatement ps = conn.prepareStatement(updSql)) {
                ps.setDouble(1, amount);
                ps.setDouble(2, amount);
                ps.setString(3, username);
                ps.executeUpdate();
            }

            // 3. Ghi transaction
            long txId = insertTransaction(
                    conn, username, "DEPOSIT", "SUCCESS",
                    amount, before, before + amount,
                    method, null, null, "Nạp tiền qua " + method
            );

            conn.commit();
            return "TX===" + buildTxJson(txId, "DEPOSIT", "SUCCESS",
                    amount, before, before + amount, "Nạp tiền qua " + method);

        } catch (Exception e) {
            conn.rollback();
            return "ERROR===" + e.getMessage();
        } finally {
            conn.setAutoCommit(true);
        }
    }

    // ── BID_HOLD ──────────────────────────────────────────────────────
    /**
     * Giữ tiền khi Bidder đặt giá.
     * Dùng SELECT ... FOR UPDATE để tránh race condition.
     */
    public static String handleBidHold(String username, String jsonBody,
                                       Connection conn) throws SQLException {
        JsonObject req    = JsonParser.parseString(jsonBody).getAsJsonObject();
        String     auctionId     = req.get("auctionId")    .getAsString();
        double     bidAmount     = req.get("bidAmount")    .getAsDouble();
        double     depositAmount = req.get("depositAmount").getAsDouble();

        conn.setAutoCommit(false);
        try {
            // 1. Khóa hàng để tránh đặt giá đồng thời trừ 2 lần
            String lockSql = "SELECT balance FROM wallets WHERE username = ? FOR UPDATE";
            double before;
            try (PreparedStatement ps = conn.prepareStatement(lockSql)) {
                ps.setString(1, username);
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) { conn.rollback(); return "ERROR===Wallet not found"; }
                before = rs.getDouble("balance");
            }

            // 2. Kiểm tra đủ tiền
            if (before < depositAmount) {
                conn.rollback();
                return "ERROR===Số dư không đủ để đặt cọc";
            }

            // 3. Trừ tiền và cộng tổng giữ
            String updSql = """
                UPDATE wallets
                SET balance   = balance - ?,
                    total_held = total_held + ?
                WHERE username = ?
                """;
            try (PreparedStatement ps = conn.prepareStatement(updSql)) {
                ps.setDouble(1, depositAmount);
                ps.setDouble(2, depositAmount);
                ps.setString(3, username);
                ps.executeUpdate();
            }

            // 4. Ghi transaction
            long txId = insertTransaction(
                    conn, username, "BID_HOLD", "SUCCESS",
                    depositAmount, before, before - depositAmount,
                    null, auctionId, null,
                    String.format("Đặt cọc đấu giá #%s (giá: %,.0f VNĐ)",
                            auctionId, bidAmount)
            );

            // 5. Ghi bid_deposits
            insertBidDeposit(conn, username, auctionId, depositAmount, bidAmount, txId);

            conn.commit();
            return "TX===" + buildTxJson(txId, "BID_HOLD", "SUCCESS",
                    depositAmount, before, before - depositAmount,
                    "Đặt cọc đấu giá #" + auctionId);

        } catch (Exception e) {
            conn.rollback();
            return "ERROR===" + e.getMessage();
        } finally {
            conn.setAutoCommit(true);
        }
    }

    // ── BID_REFUND (gọi nội bộ khi kết thúc đấu giá) ─────────────────
    public static void processRefund(String username, String auctionId,
                                     Connection conn) throws SQLException {
        conn.setAutoCommit(false);
        try {
            // Lấy deposit
            String depSql = """
                SELECT id, deposit_amount FROM bid_deposits
                WHERE username = ? AND auction_id = ? AND status = 'HELD'
                """;
            double depositAmount = 0;
            long   depositId     = 0;
            try (PreparedStatement ps = conn.prepareStatement(depSql)) {
                ps.setString(1, username);
                ps.setString(2, auctionId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    depositId     = rs.getLong("id");
                    depositAmount = rs.getDouble("deposit_amount");
                }
            }
            if (depositAmount == 0) { conn.rollback(); return; }

            double before = getBalance(username, conn);

            // Hoàn tiền
            String updWallet = """
                UPDATE wallets
                SET balance = balance + ?,
                    total_refunded = total_refunded + ?
                WHERE username = ?
                """;
            try (PreparedStatement ps = conn.prepareStatement(updWallet)) {
                ps.setDouble(1, depositAmount);
                ps.setDouble(2, depositAmount);
                ps.setString(3, username);
                ps.executeUpdate();
            }

            // Cập nhật bid_deposit
            String updDep = "UPDATE bid_deposits SET status='RELEASED' WHERE id=?";
            try (PreparedStatement ps = conn.prepareStatement(updDep)) {
                ps.setLong(1, depositId);
                ps.executeUpdate();
            }

            insertTransaction(conn, username, "BID_REFUND", "SUCCESS",
                    depositAmount, before, before + depositAmount,
                    null, auctionId, null,
                    "Hoàn cọc - thua đấu giá #" + auctionId);

            conn.commit();
        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    // ── GET_TRANSACTIONS ──────────────────────────────────────────────
    public static String handleGetTransactions(String username, String jsonBody,
                                               Connection conn) throws SQLException {
        JsonObject req    = JsonParser.parseString(jsonBody).getAsJsonObject();
        String     type   = req.get("type")  .getAsString();
        String     status = req.get("status").getAsString();
        String     from   = req.get("dateFrom").getAsString();
        String     to     = req.get("dateTo")  .getAsString();
        int        page   = req.get("page")    .getAsInt();
        int        size   = req.get("pageSize").getAsInt();
        int        offset = (page - 1) * size;

        StringBuilder sql = new StringBuilder("""
            SELECT id, type, status, amount, balance_before, balance_after,
                   note, auction_id, created_at
            FROM transactions
            WHERE username = ?
            """);

        if (!"ALL".equals(type)  ) sql.append(" AND type = '")  .append(type)  .append("'");
        if (!"ALL".equals(status)) sql.append(" AND status = '").append(status).append("'");
        if (!from.isEmpty())       sql.append(" AND created_at >= '").append(from).append(" 00:00:00'");
        if (!to.isEmpty())         sql.append(" AND created_at <= '").append(to)  .append(" 23:59:59'");
        sql.append(" ORDER BY created_at DESC LIMIT ? OFFSET ?");

        JsonArray arr = new JsonArray();
        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            ps.setString(1, username);
            ps.setInt(2, size);
            ps.setInt(3, offset);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                JsonObject obj = new JsonObject();
                obj.addProperty("id",            rs.getLong  ("id"));
                obj.addProperty("type",          rs.getString("type"));
                obj.addProperty("status",        rs.getString("status"));
                obj.addProperty("amount",        rs.getDouble("amount"));
                obj.addProperty("balanceBefore", rs.getDouble("balance_before"));
                obj.addProperty("balanceAfter",  rs.getDouble("balance_after"));
                obj.addProperty("note",          rs.getString("note"));
                obj.addProperty("auctionId",     rs.getString("auction_id"));
                obj.addProperty("createdAt",     rs.getString("created_at"));
                arr.add(obj);
            }
        }
        return "TRANSACTIONS===" + arr;
    }

    // ── Helpers ────────────────────────────────────────────────────────
    private static void ensureWalletExists(String username, Connection conn)
            throws SQLException {
        String check = "SELECT 1 FROM wallets WHERE username = ?";
        try (PreparedStatement ps = conn.prepareStatement(check)) {
            ps.setString(1, username);
            if (!ps.executeQuery().next()) {
                String ins = """
                    INSERT INTO wallets (username, balance, total_deposited,
                        total_held, total_refunded, total_paid)
                    VALUES (?, 0, 0, 0, 0, 0)
                    """;
                try (PreparedStatement pi = conn.prepareStatement(ins)) {
                    pi.setString(1, username);
                    pi.executeUpdate();
                }
            }
        }
    }

    private static double getBalance(String username, Connection conn)
            throws SQLException {
        String sql = "SELECT balance FROM wallets WHERE username = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getDouble("balance") : 0;
        }
    }

    private static long insertTransaction(Connection conn, String username,
                                          String type, String status, double amount,
                                          double before, double after, String paymentMethod,
                                          String auctionId, String bidId, String note) throws SQLException {

        String sql = """
            INSERT INTO transactions
                (username, type, status, amount, balance_before, balance_after,
                 payment_method, auction_id, bid_id, note, created_at)
            VALUES (?,?,?,?,?,?,?,?,?,?, NOW())
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql,
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, type);
            ps.setString(3, status);
            ps.setDouble(4, amount);
            ps.setDouble(5, before);
            ps.setDouble(6, after);
            ps.setString(7, paymentMethod);
            ps.setString(8, auctionId);
            ps.setString(9, bidId);
            ps.setString(10, note);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            return keys.next() ? keys.getLong(1) : -1;
        }
    }

    private static void insertBidDeposit(Connection conn, String username,
                                         String auctionId, double depositAmount,
                                         double bidAmount, long txId) throws SQLException {
        String sql = """
            INSERT INTO bid_deposits
                (username, auction_id, deposit_amount, bid_amount, status,
                 transaction_id, held_at)
            VALUES (?,?,?,?,'HELD',?, NOW())
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, auctionId);
            ps.setDouble(3, depositAmount);
            ps.setDouble(4, bidAmount);
            ps.setLong  (5, txId);
            ps.executeUpdate();
        }
    }

    private static String buildTxJson(long id, String type, String status,
                                      double amount, double before,
                                      double after, String note) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id",            id);
        obj.addProperty("type",          type);
        obj.addProperty("status",        status);
        obj.addProperty("amount",        amount);
        obj.addProperty("balanceBefore", before);
        obj.addProperty("balanceAfter",  after);
        obj.addProperty("note",          note);
        obj.addProperty("createdAt",
                LocalDateTime.now().format(FMT));
        return obj.toString();
    }
}
