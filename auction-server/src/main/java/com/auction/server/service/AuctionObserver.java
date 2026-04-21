package com.auction.server.service;


public interface AuctionObserver {
    void onNewBidPlaced(int auctionId, String bidderUsername, double newBidAmount);
}
// implement interface này để nhận thông báo realtime