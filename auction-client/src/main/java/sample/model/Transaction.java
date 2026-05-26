package sample.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Transaction {

    // ── Enums ──────────────────────────────────────────────────────────
    public enum Type {
        DEPOSIT   ("Nạp tiền",          "#27ae60"),
        BID_HOLD  ("Đặt cọc đấu giá",   "#e67e22"),
        BID_REFUND("Hoàn tiền",          "#2980b9"),
        PAYMENT   ("Thanh toán",         "#8e44ad"),
        ADJUSTMENT("Điều chỉnh số dư",   "#7f8c8d");

        public final String label;
        public final String color;
        Type(String label, String color) { this.label = label; this.color = color; }
    }

    public enum Status {
        PENDING  ("Đang xử lý", "#f39c12"),
        SUCCESS  ("Thành công",  "#27ae60"),
        FAILED   ("Thất bại",    "#e74c3c"),
        CANCELLED("Đã hủy",      "#95a5a6");

        public final String label;
        public final String color;
        Status(String label, String color) { this.label = label; this.color = color; }
    }

    // ── Fields ─────────────────────────────────────────────────────────
    private String          id;
    private String        username;
    private Type          type;
    private Status        status;
    private double        amount;
    private double        balanceBefore;
    private double        balanceAfter;
    private String        note;
    private String        auctionId;
    private String        bidId;
    private LocalDateTime createdAt;
    private String        paymentMethod;

    // ── Constructors ───────────────────────────────────────────────────
    public Transaction() { this.createdAt = LocalDateTime.now(); }

    public Transaction(Type type, Status status, double amount,
                       double balanceBefore, String note) {
        this();
        this.type          = type;
        this.status        = status;
        this.amount        = amount;
        this.balanceBefore = balanceBefore;
        this.balanceAfter  = computeBalanceAfter(type, balanceBefore, amount);
        this.note          = note;
    }

    private double computeBalanceAfter(Type type, double before, double amt) {
        return switch (type) {
            case DEPOSIT, BID_REFUND, ADJUSTMENT -> before + amt;
            case BID_HOLD, PAYMENT               -> before - amt;
        };
    }

    // ── Helpers ────────────────────────────────────────────────────────
    public String getFormattedAmount() {
        String sign = (type == Type.DEPOSIT || type == Type.BID_REFUND) ? "+" : "-";
        return sign + String.format("%,.0f VNĐ", amount);
    }

    public String getFormattedDate() {
        if (createdAt == null) return "";
        return createdAt.format(DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy"));
    }

    public boolean isCredit() {
        return type == Type.DEPOSIT || type == Type.BID_REFUND;
    }

    // ── Getters / Setters ──────────────────────────────────────────────
    public String         getId()            { return id; }
    public void          setId(String id)     { this.id = id; }
    public String        getUsername()      { return username; }
    public void          setUsername(String u) { this.username = u; }
    public Type          getType()          { return type; }
    public void          setType(Type t)    { this.type = t; }
    public Status        getStatus()        { return status; }
    public void          setStatus(Status s){ this.status = s; }
    public double        getAmount()        { return amount; }
    public void          setAmount(double a){ this.amount = a; }
    public double        getBalanceBefore() { return balanceBefore; }
    public void          setBalanceBefore(double b){ this.balanceBefore = b; }
    public double        getBalanceAfter()  { return balanceAfter; }
    public void          setBalanceAfter(double b) { this.balanceAfter = b; }
    public String        getNote()          { return note; }
    public void          setNote(String n)  { this.note = n; }
    public String        getAuctionId()     { return auctionId; }
    public void          setAuctionId(String a){ this.auctionId = a; }
    public String        getBidId()         { return bidId; }
    public void          setBidId(String b) { this.bidId = b; }
    public LocalDateTime getCreatedAt()     { return createdAt; }
    public void          setCreatedAt(LocalDateTime d){ this.createdAt = d; }
    public String        getPaymentMethod() { return paymentMethod; }
    public void          setPaymentMethod(String m){ this.paymentMethod = m; }
}
