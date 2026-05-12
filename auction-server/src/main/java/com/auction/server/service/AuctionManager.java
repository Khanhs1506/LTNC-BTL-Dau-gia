package com.auction.server.service;

import sample.model.Auction;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.ArrayList;


public class AuctionManager {


    private static class Holder {
        private static final AuctionManager INSTANCE = new AuctionManager();
    }

    //  Danh sách các phiên đấu giá  (Sử dụng ConcurrentHashMap để tránh lỗi Lost Update/Race Condition)
    private final Map<Integer, Auction> activeAuctions;


    private AuctionManager() {
        activeAuctions = new ConcurrentHashMap<>();
    }


    public static AuctionManager getInstance() {
        return Holder.INSTANCE;
    }

    // thêm phiên đấu giá
    public void addAuction(Auction auction) {
        if (auction != null) {
            activeAuctions.put(auction.getId(), auction);
        }
    }

  // lấy thông tin theo id
    public Auction getAuction(int auctionId) {
        return activeAuctions.get(auctionId);
    }


    public List<Auction> getAllAuctions() {
        return new ArrayList<>(activeAuctions.values());
    }


    public synchronized void endAuction(int auctionId) {
        Auction auction = activeAuctions.get(auctionId);
        if (auction != null) {

            activeAuctions.remove(auctionId);
            System.out.println("Phiên đấu giá " + auctionId + " đã kết thúc.");
        }
    }
}
