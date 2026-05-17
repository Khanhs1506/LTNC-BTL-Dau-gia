package sample;

public class AuctionItemDTO {

    // ── Đã có (giữ nguyên) ────────────────────────────────────
    public int    id;
    public String title;
    public String description;
    public String category;
    public String status;
    public int    warrantyMonths;
    public String artist;
    public String brand;
    public int    year;

    // ── THÊM những fields này vào ─────────────────────────────
    public double startingPrice;
    public double stepPrice;
    public double buyNowPrice;
    public double currentHighest;
    public int    totalBids;

    public String imageUrl;
    public String sellerUsername;
    public String currentWinner;

    public String platform;
    public String version;
    public String productKey;

    // Thời gian
    public java.time.LocalDateTime startTime;
    public java.time.LocalDateTime endTime;

    // ── Constructor rỗng
    public AuctionItemDTO() {}
}