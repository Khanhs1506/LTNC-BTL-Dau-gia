package com.auction.server.exception;

public class AuctionNotFoundException extends Exception {
    private final int auctionId;
    public AuctionNotFoundException(int auctionId) {
        super("Không tìm thấy phiên đấu giá với ID " + auctionId);
        this.auctionId = auctionId;
    }

    public AuctionNotFoundException(String message) {
        super(message);
        this.auctionId = -1;
    }

    public AuctionNotFoundException(String message, Throwable cause) {
        super(message, cause);
        this.auctionId = -1;
    }

    public int getAuctionId() {
        return auctionId;
    }
}