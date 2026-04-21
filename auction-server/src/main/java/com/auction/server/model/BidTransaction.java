package com.auction.server.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;


public class BidTransaction implements Serializable {
    private static final long serialVersionUID = 1L;


    private final String transactionId;
    private final int auctionId;
    private final String bidderUsername;
    private final double bidAmount;
    private final LocalDateTime timestamp;


    public BidTransaction(int auctionId, String bidderUsername, double bidAmount) {
        this.transactionId = UUID.randomUUID().toString(); // tạo ngẫu nhiên
        this.auctionId = auctionId;
        this.bidderUsername = bidderUsername;
        this.bidAmount = bidAmount;
        this.timestamp = LocalDateTime.now();
    }


    // chỉ lấy (getter) không sửa

    public String getTransactionId() {
        return transactionId;
    }

    public int getAuctionId() {
        return auctionId;
    }

    public String getBidderUsername() {
        return bidderUsername;
    }

    public double getBidAmount() {
        return bidAmount;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }


    public String getFormattedTimestamp() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");//thời gian theo cấu trúc việt nam
        return timestamp.format(formatter);
    }

    @Override
    public String toString() {
        return String.format("[%s] %s đã đặt %.2f VNĐ (Mã GD: %s)",// đây là các kí tự dữ chỗ để sau chèn dữ liệu
                getFormattedTimestamp(), bidderUsername, bidAmount, transactionId);
    }
}