package com.auction.server.model;

import java.io.Serializable;
import java.time.LocalDateTime;


// Lưu thông tin đăng ký đấu giá tự động của một bidder cho một phiên.
//Dùng trong PriorityQueue của AutoBiddingService:
//  - maxBid càng cao  → ưu tiên cao hơn sẽ thắng cuối cùng
//  - Nếu maxBid bằng nhau → ai đăng ký trước sẽ được đặt giá trước

public class AutoBidEntry implements Comparable<AutoBidEntry>, Serializable {

    private static final long serialVersionUID = 1L;

    private final int           auctionId;
    private final String        username;
    private final double        maxBid;      // Giới hạn giá tối đa bidder chịu trả
    private final double        increment;   // Mỗi lần tự động tăng thêm bao nhiêu tiền
    private final LocalDateTime registeredAt;

    public AutoBidEntry(int auctionId, String username, double maxBid,
                        double increment, LocalDateTime registeredAt) {
        if (maxBid <= 0)       throw new IllegalArgumentException("maxBid phải > 0");
        if (increment <= 0)    throw new IllegalArgumentException("increment phải > 0");
        this.auctionId    = auctionId;
        this.username     = username;
        this.maxBid       = maxBid;
        this.increment    = increment;
        this.registeredAt = registeredAt;
    }

    /** Tiện ích: tạo entry với thời gian đăng ký là ngay bây giờ */
    public AutoBidEntry(int auctionId, String username, double maxBid, double increment) {
        this(auctionId, username, maxBid, increment, LocalDateTime.now());
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public int           getAuctionId()    { return auctionId; }
    public String        getUsername()     { return username; }
    public double        getMaxBid()       { return maxBid; }
    public double        getIncrement()    { return increment; }
    public LocalDateTime getRegisteredAt() { return registeredAt; }

    /**
     * So sánh cho PriorityQueue max-heap:
     *   - maxBid cao hơn → ưu tiên cao hơn (đứng đầu queue)
     *   - Nếu bằng nhau   → đăng ký trước (registeredAt nhỏ hơn) → ưu tiên cao hơn
     */
    @Override
    public int compareTo(AutoBidEntry other) {
        // Đảo dấu để tạo max-heap theo maxBid
        int cmp = Double.compare(other.maxBid, this.maxBid);
        if (cmp != 0) return cmp;
        // Tie-break: đăng ký trước thì ưu tiên hơn
        return this.registeredAt.compareTo(other.registeredAt);
    }

    @Override
    public String toString() {
        return String.format("AutoBid[auction=%d, user=%s, max=%.0f, inc=%.0f]",
                auctionId, username, maxBid, increment);
    }
}