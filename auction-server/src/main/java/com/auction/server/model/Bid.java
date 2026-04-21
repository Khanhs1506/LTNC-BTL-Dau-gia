package com.auction.server.model;

import java.time.LocalDateTime;


public class Bid implements Entity<String> {
    private String id;
    private String itemId;
    private String userId;
    private double amount;
    private LocalDateTime time;

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