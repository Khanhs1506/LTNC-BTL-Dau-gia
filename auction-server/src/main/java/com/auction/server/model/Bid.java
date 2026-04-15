package com.auction.server.model;

import java.time.LocalDateTime;

// Bid cũng là một thực thể cần lưu trữ, nên phải implements Entity
public class Bid implements Entity {
    private String id;          // Mã của lượt bid (ví dụ: BID-001)
    private String itemId;      // Mã sản phẩm được trả giá
    private String userId;      // Mã người dùng thực hiện trả giá
    private double amount;      // Số tiền trả
    private LocalDateTime time; // Thời điểm bid

    public Bid(String id, String itemId, String userId, double amount) {
        this.id = id;
        this.itemId = itemId;
        this.userId = userId;
        this.amount = amount;
        this.time = LocalDateTime.now(); // Tự động lấy giờ hệ thống lúc tạo
    }

    @Override
    public String getId() { return id; }

    @Override
    public void setId(String id) { this.id = id; }

    // Getters & Setters
    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public LocalDateTime getTime() { return time; }

    // Hàm in lịch sử
    public void printBidDetails() {
        System.out.println("[Lịch sử] User: " + userId + " | Sản phẩm: " + itemId + " | Giá: " + amount + " | Lúc: " + time);
    }
}