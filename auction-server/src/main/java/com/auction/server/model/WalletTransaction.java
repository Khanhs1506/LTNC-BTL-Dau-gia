package com.auction.server.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/*Đại diện cho một giao dịch ví trong bảng wallet_transactions.
Các loại giao dịch (Type):
- DEPOSIT     : Người dùng nạp tiền vào ví
- PAYMENT     : Người thắng thanh toán đấu giá (trừ tiền)
- REFUND      : Hoàn tiền khi đấu giá hủy hoặc người thua
- BID_HOLD    : Giữ tiền đặt cọc khi đặt giá
- BID_RELEASE : Giải phóng tiền cọc (hoàn lại khi thua)
*/
public class WalletTransaction implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Type {
        DEPOSIT,
        PAYMENT,
        REFUND,
        BID_HOLD,
        BID_RELEASE
    }

    private String id;               // UUID, khóa chính
    private String userId;           // FK -> users.id
    private Type type;
    private double amount;
    private double balanceBefore;
    private double balanceAfter;
    private Integer relatedAuctionId; // nullable
    private String note;
    private LocalDateTime createdAt;

    /** Constructor đầy đủ — dùng khi đọc từ DB */
    public WalletTransaction(String id, String userId, Type type, double amount, double balanceBefore, double balanceAfter, Integer relatedAuctionId, String note, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.type = type;
        this.amount = amount;
        this.balanceBefore = balanceBefore;
        this.balanceAfter = balanceAfter;
        this.relatedAuctionId = relatedAuctionId;
        this.note = note;
        this.createdAt = createdAt;
    }

    /** Constructor tạo mới — id và createdAt do DB tự sinh */
    public WalletTransaction(String userId, Type type, double amount, double balanceBefore, double balanceAfter, Integer relatedAuctionId, String note) {
        this(null, userId, type, amount, balanceBefore, balanceAfter, relatedAuctionId, note, null);
    }

    // ── Getters ──────────────────────────────────────────────────────────
    public String getId()  { return id; }
    public String getUserId() { return userId; }
    public Type getType() { return type; }
    public double getAmount() { return amount; }
    public double getBalanceBefore() { return balanceBefore; }
    public double getBalanceAfter() { return balanceAfter; }
    public Integer getRelatedAuctionId() { return relatedAuctionId; }
    public String getNote() { return note; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // ── Setters (chỉ những field cần thiết) ─────────────────────────────
    public void setId(String id) { this.id = id; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return String.format("[WalletTransaction] %s | %s | %.0f | %s -> %s",
                             type, id, amount, balanceBefore, balanceAfter);
    }
}