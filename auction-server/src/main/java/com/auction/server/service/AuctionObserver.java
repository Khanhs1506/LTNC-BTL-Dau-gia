package com.auction.server.service;


import java.time.LocalDateTime;

public interface AuctionObserver {
    void onNewBidPlaced(int auctionId, String bidderUsername, double newBidAmount);
    default void onTimeExtended(int auctionId, LocalDateTime newEndTime) {}
}
// implement interface này để nhận thông báo realtime