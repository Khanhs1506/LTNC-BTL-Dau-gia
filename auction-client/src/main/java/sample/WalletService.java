package sample;

import com.google.gson.*;
import sample.model.Transaction;
import sample.model.Wallet;
import sample.model.BidDeposit;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Giao tiếp với server cho tất cả nghiệp vụ ví.
 * Mọi phương thức trả về kết quả đồng bộ (chạy trên thread riêng ở tầng trên).
 */
public class WalletService {

    private static WalletService instance;
    public static WalletService getInstance() {
        if (instance == null) instance = new WalletService();
        return instance;
    }

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    //LẤY SỐ DƯ VÍ
    public double fetchBalance() {
        try {
            String resp = ServerConnection.getInstance().getBalance();
            JsonObject data = extractOkData(resp, "GET_BALANCE");
            if (data != null) return data.get("balance").getAsDouble();
        } catch (Exception e) {
            System.err.println("[WalletService] fetchBalance error: " + e.getMessage());
        }
        return 0.0;
    }

    // ── Lấy thông tin ví ──────────────────────────────────────────────
    public Wallet fetchWallet() {
        double balance = fetchBalance();
        Wallet w = new Wallet((UserSession.getInstance().getUsername()));
        w.setBalance(balance);
        return w;
    }

    // ── Nạp tiền ──────────────────────────────────────────────────────
    /**
     * @return Transaction với status SUCCESS / FAILED / PENDING
     */
    public Transaction deposit(double amount, String paymentMethod) {
        try {
            String note = "Nạp tiền - " + paymentMethod;
            String resp = ServerConnection.getInstance().deposit(amount, note);
            JsonObject data = extractOkData(resp, "DEPOSIT");
            if (data != null) return parseTransaction(data);
        } catch (Exception e) {
            System.err.println("[WalletService] deposit error: " + e.getMessage());
        }
        // Fallback: trả về FAILED
        return failedTx(Transaction.Type.DEPOSIT, amount, "không kết nối được server");
    }

    // ── Giữ tiền khi đặt giá ──────────────────────────────────────────
    public Transaction holdForBid(int auctionId, double depositAmount) {
        try {
            String resp = ServerConnection.getInstance().bidHold(auctionId, depositAmount);
            JsonObject data = extractOkData(resp, "BID_HOLD");
            if (data != null) return parseTransaction(data);
        } catch (Exception e) {
            System.err.println("[WalletService] holdForBid error: " + e.getMessage());
        }
        return failedTx(Transaction.Type.BID_HOLD, depositAmount, "lỗi đặt cock");
    }

    //hoàn tiền cọc
    public Transaction releaseForBid(int auctionId, double amount) {
        try {
            String resp = ServerConnection.getInstance().bidRelease(auctionId, amount);
            JsonObject data = extractOkData(resp, "BID_RELEASE");
            if (data != null) return parseTransaction(data);
        } catch (Exception e) {
            System.err.println("[WalletService] releaseForBid error: " + e.getMessage());
        }
        return failedTx(Transaction.Type.BID_REFUND, amount, "Lỗi hoàn cọc");
    }

    //thanh toán
    public Transaction payment(int auctionId, double amount) {
        try {
            String resp = ServerConnection.getInstance().payment(auctionId, amount);
            JsonObject data = extractOkData(resp, "PAYMENT");
            if (data != null) return parseTransaction(data);
        } catch (Exception e) {
            System.err.println("[WalletService] payment error: " + e.getMessage());
        }
        return failedTx(Transaction.Type.PAYMENT, amount, "Lỗi thanh toán");
    }

    // ── Lấy lịch sử giao dịch ─────────────────────────────────────────
    public List<Transaction> fetchTransactions(String typeFilter,
                                               String statusFilter,
                                               String dateFrom,
                                               String dateTo,
                                               int page,
                                               int pageSize) {
        try {
            String resp = ServerConnection.getInstance().getTransactionHistory(pageSize);
            JsonObject data = extractOkData(resp, "GET_TX_HISTORY");
            if (data != null && data.has("transactions")) {
                return parseTransactionList(data.getAsJsonArray("transactions"), typeFilter, statusFilter, dateFrom, dateTo, page, pageSize);
            }
        } catch (Exception e) {
            System.err.println("[WalletService] fetchTransactions error: " + e.getMessage());
        }
        return buildMockTransactions();
    }

    //
    private JsonObject extractOkData(String response, String command) {
        if (response == null) return null;
        String prefix = "WALLET_" + command + "===";

        if (!response.startsWith(prefix)) {
            System.err.println("[WalletService] Unexpected response: " + response);
            return null;
        }
        String rest = response.substring(prefix.length()); // "OK==={...}" hoặc "ERROR===msg"
        if (rest.startsWith("OK===")) {
            String json = rest.substring(5);
            return JsonParser.parseString(json).getAsJsonObject();
        } else if (rest.startsWith("ERROR===")) {
            System.err.println("[WalletService] Server error [" + command + "]: " + rest.substring(8));
        }
        return null;
    }

    private Transaction parseTransaction(JsonObject obj) {
        Transaction tx = new Transaction();
        if (obj.has("id")) tx.setId(obj.get("id").getAsString());
        if (obj.has("type"))          tx.setType(mapType(obj.get("type").getAsString()));
        if (obj.has("amount"))        tx.setAmount(obj.get("amount").getAsDouble());
        if (obj.has("balanceBefore")) tx.setBalanceBefore(obj.get("balanceBefore").getAsDouble());
        if (obj.has("balanceAfter"))  tx.setBalanceAfter(obj.get("balanceAfter").getAsDouble());
        if (obj.has("note"))          tx.setNote(obj.get("note").getAsString());
        if (obj.has("relatedAuctionId") && !obj.get("relatedAuctionId").isJsonNull())
            tx.setAuctionId(obj.get("relatedAuctionId").getAsString());
        if (obj.has("createdAt") && !obj.get("createdAt").getAsString().isEmpty()) {
            try { tx.setCreatedAt(LocalDateTime.parse(obj.get("createdAt").getAsString())); }
            catch (Exception ignored) {}
        }
        // Server WalletHandler luôn thành công nếu đến đây
        tx.setStatus(Transaction.Status.SUCCESS);
        return tx;
    }

    private List<Transaction> parseTransactionList(JsonArray arr,
                                                   String typeFilter, String statusFilter,
                                                   String dateFrom, String dateTo,
                                                   int page, int pageSize) {
        List<Transaction> all = new ArrayList<>();
        for (JsonElement el : arr) all.add(parseTransaction(el.getAsJsonObject()));

        // Lọc phía client (server trả tất cả trong limit)
        List<Transaction> filtered = all.stream()
                .filter(tx -> typeFilter == null || tx.getType() == mapType(typeFilter))
                .filter(tx -> statusFilter == null || tx.getStatus() == mapStatus(statusFilter))
                .collect(java.util.stream.Collectors.toList());

        // Phân trang
        int from = (page - 1) * pageSize;
        int to   = Math.min(from + pageSize, filtered.size());
        return from >= filtered.size() ? new ArrayList<>() : filtered.subList(from, to);
    }

    // ── Offline mock ───────────────────────────────────────────────────
    private Transaction.Type mapType(String s) {
        if (s == null) return Transaction.Type.DEPOSIT;
        return switch (s) {
            case "DEPOSIT"    -> Transaction.Type.DEPOSIT;
            case "PAYMENT"    -> Transaction.Type.PAYMENT;
            case "REFUND"     -> Transaction.Type.BID_REFUND;
            case "BID_HOLD"   -> Transaction.Type.BID_HOLD;
            case "BID_RELEASE"-> Transaction.Type.BID_REFUND;
            default           -> Transaction.Type.DEPOSIT;
        };
    }

    private Transaction.Status mapStatus(String s) {
        if (s == null) return null;
        try { return Transaction.Status.valueOf(s); } catch (Exception e) { return null; }
    }

    private Transaction failedTx(Transaction.Type type, double amount, String note) {
        Transaction tx = new Transaction();
        tx.setType(type);
        tx.setStatus(Transaction.Status.FAILED);
        tx.setAmount(amount);
        tx.setNote(note);
        return tx;
    }

    private List<Transaction> buildMockTransactions() {
        List<Transaction> list = new ArrayList<>();
        list.add(mockTx("1", Transaction.Type.DEPOSIT,   Transaction.Status.SUCCESS,  5_000_000, "Nạp tiền VNPay"));
        list.add(mockTx("2", Transaction.Type.BID_HOLD,  Transaction.Status.SUCCESS,  2_000_000, "Đặt cọc - Rolex 001"));
        list.add(mockTx("3", Transaction.Type.BID_REFUND,Transaction.Status.SUCCESS,  2_000_000, "Hoàn cọc - Rolex 001"));
        list.add(mockTx("4", Transaction.Type.PAYMENT,   Transaction.Status.SUCCESS, 12_000_000, "Thanh toán - Mercedes"));
        return list;
    }

    private Transaction mockTx(String id, Transaction.Type type,
                               Transaction.Status status, double amount, String note) {
        Transaction tx = new Transaction();
        tx.setId(id); tx.setType(type); tx.setStatus(status);
        tx.setAmount(amount); tx.setNote(note);
        tx.setCreatedAt(LocalDateTime.now().minusDays(7));
        return tx;
    }
}