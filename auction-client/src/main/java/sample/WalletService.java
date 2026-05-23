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

    // ── Lấy thông tin ví ──────────────────────────────────────────────
    public Wallet fetchWallet() {
        try {
            String resp = ServerConnection.getInstance().getWallet();
            if (resp != null && resp.startsWith("WALLET===")) {
                return parseWallet(resp.substring(9));
            }
        } catch (Exception e) {
            System.err.println("[WalletService] fetchWallet error: " + e.getMessage());
        }
        return buildOfflineWallet();
    }

    // ── Nạp tiền ──────────────────────────────────────────────────────
    /**
     * @return Transaction với status SUCCESS / FAILED / PENDING
     */
    public Transaction deposit(double amount, String paymentMethod) {
        try {
            String resp = ServerConnection.getInstance().deposit(amount, paymentMethod);

            if (resp != null && resp.startsWith("TX===")) {
                return parseTransaction(resp.substring(5));
            }
        } catch (Exception e) {
            System.err.println("[WalletService] deposit error: " + e.getMessage());
        }
        // Fallback: trả về FAILED
        Transaction failed = new Transaction(Transaction.Type.DEPOSIT,
                Transaction.Status.FAILED, amount, 0, "Không kết nối được server");
        return failed;
    }

    // ── Giữ tiền khi đặt giá ──────────────────────────────────────────
    public Transaction holdForBid(String auctionId, double bidAmount,
                                  double depositAmount) {
        try {
            String resp = ServerConnection.getInstance()
                    .bidHold(auctionId, bidAmount, depositAmount);

            if (resp != null && resp.startsWith("TX===")) {
                return parseTransaction(resp.substring(5));
            }
        } catch (Exception e) {
            System.err.println("[WalletService] holdForBid error: " + e.getMessage());
        }
        Transaction failed = new Transaction(Transaction.Type.BID_HOLD,
                Transaction.Status.FAILED, depositAmount, 0, "Lỗi giữ tiền");
        return failed;
    }

    // ── Lấy lịch sử giao dịch ─────────────────────────────────────────
    public List<Transaction> fetchTransactions(String typeFilter,
                                               String statusFilter,
                                               String dateFrom,
                                               String dateTo,
                                               int page,
                                               int pageSize) {
        try {
            String resp = ServerConnection.getInstance()
                    .getTransactions(typeFilter, statusFilter, dateFrom, dateTo, page, pageSize);

            if (resp != null && resp.startsWith("TRANSACTIONS===")) {
                return parseTransactionList(resp.substring(15));
            }
        } catch (Exception e) {
            System.err.println("[WalletService] fetchTransactions error: " + e.getMessage());
        }
        return buildMockTransactions();
    }

    // ── Parse JSON → models ────────────────────────────────────────────
    private Wallet parseWallet(String json) {
        JsonObject obj    = JsonParser.parseString(json).getAsJsonObject();
        Wallet     wallet = new Wallet(UserSession.getInstance().getUsername());
        wallet.setBalance      (obj.get("balance")       .getAsDouble());
        wallet.setTotalDeposited(obj.get("totalDeposited").getAsDouble());
        wallet.setTotalHeld    (obj.get("totalHeld")     .getAsDouble());
        wallet.setTotalRefunded(obj.get("totalRefunded") .getAsDouble());
        wallet.setTotalPaid    (obj.get("totalPaid")     .getAsDouble());
        return wallet;
    }

    private Transaction parseTransaction(String json) {
        JsonObject  obj = JsonParser.parseString(json).getAsJsonObject();
        Transaction tx  = new Transaction();
        tx.setId           (obj.get("id")    .getAsLong());
        tx.setType         (Transaction.Type  .valueOf(obj.get("type")  .getAsString()));
        tx.setStatus       (Transaction.Status.valueOf(obj.get("status").getAsString()));
        tx.setAmount       (obj.get("amount")       .getAsDouble());
        tx.setBalanceBefore(obj.get("balanceBefore").getAsDouble());
        tx.setBalanceAfter (obj.get("balanceAfter") .getAsDouble());
        tx.setNote         (obj.get("note")         .getAsString());
        if (obj.has("auctionId") && !obj.get("auctionId").isJsonNull())
            tx.setAuctionId(obj.get("auctionId").getAsString());
        if (obj.has("createdAt") && !obj.get("createdAt").isJsonNull())
            tx.setCreatedAt(LocalDateTime.parse(obj.get("createdAt").getAsString(), FMT));
        return tx;
    }

    private List<Transaction> parseTransactionList(String json) {
        JsonArray         arr  = JsonParser.parseString(json).getAsJsonArray();
        List<Transaction> list = new ArrayList<>();
        for (JsonElement el : arr) list.add(parseTransaction(el.toString()));
        return list;
    }

    // ── Offline mock ───────────────────────────────────────────────────
    private Wallet buildOfflineWallet() {
        Wallet w = new Wallet(UserSession.getInstance().getUsername());
        w.setBalance      (5_000_000);
        w.setTotalDeposited(20_000_000);
        w.setTotalHeld    (2_000_000);
        w.setTotalRefunded(1_500_000);
        w.setTotalPaid    (12_500_000);
        return w;
    }

    private List<Transaction> buildMockTransactions() {
        List<Transaction> list = new ArrayList<>();
        list.add(mockTx(1, Transaction.Type.DEPOSIT,    Transaction.Status.SUCCESS,  5_000_000, "Nạp tiền VNPay"));
        list.add(mockTx(2, Transaction.Type.BID_HOLD,   Transaction.Status.SUCCESS,  2_000_000, "Đặt cọc - Rolex 001"));
        list.add(mockTx(3, Transaction.Type.BID_REFUND, Transaction.Status.SUCCESS,  2_000_000, "Hoàn cọc - Rolex 001"));
        list.add(mockTx(4, Transaction.Type.PAYMENT,    Transaction.Status.SUCCESS, 12_000_000, "Thanh toán - Mercedes"));
        list.add(mockTx(5, Transaction.Type.DEPOSIT,    Transaction.Status.PENDING,  3_000_000, "Nạp tiền Momo"));
        return list;
    }

    private Transaction mockTx(long id, Transaction.Type type,
                               Transaction.Status status, double amount, String note) {
        Transaction tx = new Transaction();
        tx.setId(id); tx.setType(type); tx.setStatus(status);
        tx.setAmount(amount); tx.setNote(note);
        tx.setCreatedAt(LocalDateTime.now().minusDays(id));
        return tx;
    }
}