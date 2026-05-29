package com.auction.server.network.handler;

import com.auction.server.model.WalletTransaction;
import com.auction.server.repository.IWalletDAO;
import com.auction.server.repository.WalletDaoImpl;
import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.List;

public class WalletHandler {

    private final IWalletDAO walletDAO;
    private final Gson       gson;

    public WalletHandler() {
        this.walletDAO = new WalletDaoImpl();
        this.gson      = new GsonBuilder().registerTypeAdapter(WalletTransaction.class, new WalletTxSerializer()).create();
    }


    public String handle(String command, String userId, String jsonBody) {
        try {
            JsonObject req = JsonParser.parseString(jsonBody).getAsJsonObject();
            return switch (command) {
                case "GET_BALANCE"    -> handleGetBalance(userId);
                case "DEPOSIT"        -> handleDeposit(userId, req);
                case "PAYMENT"        -> handlePayment(userId, req);
                case "REFUND"         -> handleRefund(userId, req);
                case "BID_HOLD"       -> handleBidHold(userId, req);
                case "BID_RELEASE"    -> handleBidRelease(userId, req);
                case "GET_TX_HISTORY" -> handleGetTxHistory(userId, req);
                default               -> error("Lệnh không được hỗ trợ: " + command);
            };
        } catch (JsonSyntaxException e) {
            return error("JSON không hợp lệ: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("[WalletHandler] Lỗi không mong muốn: " + e.getMessage());
            return error("Lỗi hệ thống");
        }
    }

    public double getBalance(String userId) {
        return walletDAO.getBalance(userId);
    }

    // ── Handlers ──────────────────────────────────────────────────────────

    /** Trả về số dư ví hiện tại */
    private String handleGetBalance(String userId) {
        double balance = walletDAO.getBalance(userId);
        if (balance < 0) return error("Không tìm thấy user");
        JsonObject result = new JsonObject();
        result.addProperty("balance", balance);
        return ok(result);
    }

    /** Nạp tiền vào ví */
    private String handleDeposit(String userId, JsonObject req) {
        // Validate tham số
        if (!req.has("amount")) return error("Thiếu trường 'amount'");
        double amount = req.get("amount").getAsDouble();
        if (amount <= 0) return error("Số tiền nạp phải lớn hơn 0");
        String note = req.has("note") ? req.get("note").getAsString() : "Nạp tiền vào ví";
        WalletTransaction tx = walletDAO.deposit(userId, amount, note);
        if (tx == null) return error("Nạp tiền thất bại");
        return ok(gson.toJsonTree(tx));
    }

    /** Thanh toán đấu giá — người thắng trả tiền */
    private String handlePayment(String userId, JsonObject req) {
        if (!req.has("amount"))    return error("Thiếu trường 'amount'");
        if (!req.has("auctionId")) return error("Thiếu trường 'auctionId'");
        double amount    = req.get("amount").getAsDouble();
        int    auctionId = req.get("auctionId").getAsInt();
        if (amount <= 0) return error("Số tiền thanh toán phải lớn hơn 0");
        String note = req.has("note") ? req.get("note").getAsString()
                : String.format("Thanh toán đấu giá #%d", auctionId);
        WalletTransaction tx = walletDAO.payment(userId, amount, auctionId, note);
        if (tx == null) return error("Thanh toán thất bại — có thể số dư không đủ");
        return ok(gson.toJsonTree(tx));
    }

    /** Hoàn tiền (đấu giá hủy hoặc hoàn cọc thua) */
    private String handleRefund(String userId, JsonObject req) {
        if (!req.has("amount")) return error("Thiếu trường 'amount'");
        double amount = req.get("amount").getAsDouble();
        if (amount <= 0) return error("Số tiền hoàn phải lớn hơn 0");
        Integer auctionId = req.has("auctionId") && !req.get("auctionId").isJsonNull()
                ? req.get("auctionId").getAsInt() : null;
        String note = req.has("note") ? req.get("note").getAsString() : "Hoàn tiền";
        WalletTransaction tx = walletDAO.refund(userId, amount, auctionId, note);
        if (tx == null) return error("Hoàn tiền thất bại");
        return ok(gson.toJsonTree(tx));
    }

    /** Giữ tiền cọc khi bidder đặt giá */
    private String handleBidHold(String userId, JsonObject req) {
        if (!req.has("amount"))    return error("Thiếu trường 'amount'");
        if (!req.has("auctionId")) return error("Thiếu trường 'auctionId'");
        double amount    = req.get("amount").getAsDouble();
        int    auctionId = req.get("auctionId").getAsInt();
        if (amount <= 0) return error("Số tiền đặt cọc phải lớn hơn 0");
        String note = req.has("note") ? req.get("note").getAsString()
                : String.format("Đặt cọc đấu giá #%d", auctionId);
        WalletTransaction tx = walletDAO.bidHold(userId, amount, auctionId, note);
        if (tx == null) return error("Đặt cọc thất bại — có thể số dư không đủ");
        return ok(gson.toJsonTree(tx));
    }

    /** Giải phóng tiền cọc khi người dùng bị vượt giá hoặc đấu giá kết thúc */
    private String handleBidRelease(String userId, JsonObject req) {
        if (!req.has("amount"))    return error("Thiếu trường 'amount'");
        if (!req.has("auctionId")) return error("Thiếu trường 'auctionId'");
        double amount    = req.get("amount").getAsDouble();
        int    auctionId = req.get("auctionId").getAsInt();
        if (amount <= 0) return error("Số tiền giải phóng phải lớn hơn 0");
        String note = req.has("note") ? req.get("note").getAsString()
                : String.format("Hoàn cọc đấu giá #%d", auctionId);
        WalletTransaction tx = walletDAO.bidRelease(userId, amount, auctionId, note);
        if (tx == null) return error("Giải phóng tiền cọc thất bại");

        return ok(gson.toJsonTree(tx));
    }

    /** Lấy lịch sử giao dịch */
    private String handleGetTxHistory(String userId, JsonObject req) {
        int limit = req.has("limit") ? req.get("limit").getAsInt() : 20;
        if (limit < 0) limit = 0; // 0 = không giới hạn
        List<WalletTransaction> history = walletDAO.getTransactionHistory(userId, limit);
        JsonArray arr = new JsonArray();
        for (WalletTransaction tx : history) {
            arr.add(gson.toJsonTree(tx));
        }
        JsonObject result = new JsonObject();
        result.addProperty("count", history.size());
        result.add("transactions", arr);
        return ok(result);
    }

    // ── Helpers ───────────────────────────────────────────────────────────
    private String ok(JsonElement data) {
        return "OK===" + data;
    }

    private String error(String message) {
        return "ERROR===" + message;
    }

    // ── Custom serializer để gửi WalletTransaction qua mạng ──────────────

    /**
     * Chuyển WalletTransaction thành JSON gọn gàng để gửi về client.
     * LocalDateTime được format thành chuỗi "yyyy-MM-dd HH:mm:ss".
     */
    private static class WalletTxSerializer
            implements JsonSerializer<WalletTransaction> {

        @Override
        public JsonElement serialize(WalletTransaction tx,
                                     Type typeOfSrc,
                                     JsonSerializationContext ctx) {
            JsonObject obj = new JsonObject();
            obj.addProperty("id", tx.getId());
            obj.addProperty("type", tx.getType().name());
            obj.addProperty("amount", tx.getAmount());
            obj.addProperty("balanceBefore", tx.getBalanceBefore());
            obj.addProperty("balanceAfter", tx.getBalanceAfter());
            obj.addProperty("note", tx.getNote());

            if (tx.getRelatedAuctionId() != null)
                obj.addProperty("relatedAuctionId", tx.getRelatedAuctionId());
            else
                obj.add("relatedAuctionId", JsonNull.INSTANCE);

            obj.addProperty("createdAt",
                    tx.getCreatedAt() != null ? tx.getCreatedAt().toString() : "");
            return obj;
        }
    }
}