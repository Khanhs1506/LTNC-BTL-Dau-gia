package sample.model;

import java.time.LocalDateTime;

/**
 * Theo dõi trạng thái tiền đặt cọc cho từng phiên đấu giá.
 */
public class BidDeposit {

    public enum DepositStatus {
        HELD      ("Đang giữ",    "#e67e22"),
        RELEASED  ("Đã hoàn",     "#27ae60"),
        FORFEITED ("Đã trừ",      "#e74c3c"),
        PENDING   ("Chờ xử lý",   "#f39c12");

        public final String label;
        public final String color;
        DepositStatus(String label, String color){ this.label = label; this.color = color; }
    }

    private long          id;
    private String        username;
    private String        auctionId;
    private String        auctionName;
    private double        depositAmount;   // Số tiền đặt cọc
    private double        bidAmount;       // Giá đặt
    private DepositStatus status;
    private LocalDateTime heldAt;
    private LocalDateTime releasedAt;
    private String        transactionId;

    public BidDeposit() { this.heldAt = LocalDateTime.now(); }

    public BidDeposit(String username, String auctionId, String auctionName,
                      double depositAmount, double bidAmount) {
        this();
        this.username      = username;
        this.auctionId     = auctionId;
        this.auctionName   = auctionName;
        this.depositAmount = depositAmount;
        this.bidAmount     = bidAmount;
        this.status        = DepositStatus.HELD;
    }

    public String getFormattedDeposit() {
        return String.format("%,.0f VNĐ", depositAmount);
    }

    public String getFormattedBid() {
        return String.format("%,.0f VNĐ", bidAmount);
    }

    // ── Getters / Setters ──────────────────────────────────────────────
    public long          getId()             { return id; }
    public void          setId(long id)      { this.id = id; }
    public String        getUsername()       { return username; }
    public String        getAuctionId()      { return auctionId; }
    public void          setAuctionId(String a){ auctionId = a; }
    public String        getAuctionName()    { return auctionName; }
    public void          setAuctionName(String n){ auctionName = n; }
    public double        getDepositAmount()  { return depositAmount; }
    public void          setDepositAmount(double d){ depositAmount = d; }
    public double        getBidAmount()      { return bidAmount; }
    public void          setBidAmount(double b){ bidAmount = b; }
    public DepositStatus getStatus()         { return status; }
    public void          setStatus(DepositStatus s){ status = s; }
    public LocalDateTime getHeldAt()         { return heldAt; }
    public void          setHeldAt(LocalDateTime t){ heldAt = t; }
    public LocalDateTime getReleasedAt()     { return releasedAt; }
    public void          setReleasedAt(LocalDateTime t){ releasedAt = t; }
    public String        getTransactionId()  { return transactionId; }
    public void          setTransactionId(String t){ transactionId = t; }
}
