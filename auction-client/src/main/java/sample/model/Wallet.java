package sample.model;

import java.util.ArrayList;
import java.util.List;

public class Wallet {

    private String username;
    private double balance;        // Số dư hiện tại
    private double totalDeposited; // Tổng đã nạp
    private double totalHeld;      // Tổng đang bị giữ (đặt cọc)
    private double totalRefunded;  // Tổng đã hoàn
    private double totalPaid;      // Tổng đã thanh toán

    private List<Transaction> transactions = new ArrayList<>();

    // ── Constructor ────────────────────────────────────────────────────
    public Wallet(String username) { this.username = username; }

    // ── Business methods ───────────────────────────────────────────────

    /** Cộng tiền vào ví (nạp / hoàn) — ghi transaction */
    public Transaction credit(Transaction.Type type, double amount,
                              String note, String paymentMethod) {
        Transaction tx = new Transaction(type, Transaction.Status.SUCCESS,
                amount, this.balance, note);
        tx.setUsername(username);
        tx.setPaymentMethod(paymentMethod);

        this.balance += amount;
        if (type == Transaction.Type.DEPOSIT)    totalDeposited += amount;
        if (type == Transaction.Type.BID_REFUND) totalRefunded  += amount;

        transactions.add(tx);
        return tx;
    }

    /** Trừ tiền khỏi ví — kiểm tra số dư trước */
    public Transaction debit(Transaction.Type type, double amount,
                             String note, String auctionId, String bidId) {
        if (balance < amount) {
            Transaction failed = new Transaction(type, Transaction.Status.FAILED,
                    amount, balance, "Số dư không đủ");
            failed.setUsername(username);
            failed.setAuctionId(auctionId);
            failed.setBidId(bidId);
            transactions.add(failed);
            return failed;
        }

        Transaction tx = new Transaction(type, Transaction.Status.SUCCESS,
                amount, this.balance, note);
        tx.setUsername(username);
        tx.setAuctionId(auctionId);
        tx.setBidId(bidId);

        this.balance -= amount;
        if (type == Transaction.Type.BID_HOLD) totalHeld += amount;
        if (type == Transaction.Type.PAYMENT)  totalPaid += amount;

        transactions.add(tx);
        return tx;
    }

    /** Kiểm tra đủ số dư để đặt cọc */
    public boolean canAfford(double amount) { return balance >= amount; }

    // ── Getters / Setters ──────────────────────────────────────────────
    public String             getUsername()      { return username; }
    public double             getBalance()       { return balance; }
    public void               setBalance(double b){ this.balance = b; }
    public double             getTotalDeposited(){ return totalDeposited; }
    public void               setTotalDeposited(double v){ totalDeposited = v; }
    public double             getTotalHeld()     { return totalHeld; }
    public void               setTotalHeld(double v)    { totalHeld = v; }
    public double             getTotalRefunded() { return totalRefunded; }
    public void               setTotalRefunded(double v){ totalRefunded = v; }
    public double             getTotalPaid()     { return totalPaid; }
    public void               setTotalPaid(double v)    { totalPaid = v; }
    public List<Transaction>  getTransactions()  { return transactions; }
    public void               setTransactions(List<Transaction> t){ transactions = t; }
}
