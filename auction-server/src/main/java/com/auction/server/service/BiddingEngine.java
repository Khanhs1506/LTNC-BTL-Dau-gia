package com.auction.server.service;

import sample.model.Auction;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


public class BiddingEngine {

    // bản thiết kế Singleton
    private static BiddingEngine instance;
    private final AuctionManager auctionManager;

    // Danh sách người quan sát
    private final List<AuctionObserver> observers;

    private BiddingEngine() {
        this.auctionManager = AuctionManager.getInstance();
        this.observers = new CopyOnWriteArrayList<>();
    }

    public static synchronized BiddingEngine getInstance() {
        if (instance == null) {
            instance = new BiddingEngine();
        }
        return instance;
    }


// cho phép đăng kí nhânj thông báo khi đang bên trong ứng dụng
    public void addObserver(AuctionObserver observer) {
        observers.add(observer);
    }
//khi thoát ứng dụng sẽ không hiện thông báo nx
    public void removeObserver(AuctionObserver observer) {
        observers.remove(observer);
    }

    private void notifyAllObservers(int auctionId, String username, double bidAmount) {
        for (AuctionObserver observer : observers) {
            observer.onNewBidPlaced(auctionId, username, bidAmount);
        }
    }

    // logic sử lý đấu giá


    public boolean processBid(int auctionId, String username, double bidAmount) throws Exception {
        // Lấy phiên đấu giá từ Manager
        Auction auction = auctionManager.getAuction(auctionId);
        if (auction == null) {
            throw new Exception("Lỗi: Không tìm thấy phiên đấu giá với ID " + auctionId);
        }

        // Gọi logic đặt giá bên Auction
        boolean isSuccess = auction.placeBid(username, bidAmount);

        if (isSuccess) {
            // Nếu có người đặt giá trong 30 giây cuối cùng, tự động gia hạn thêm 1 phút.
            long secondsLeft = ChronoUnit.SECONDS.between(LocalDateTime.now(), auction.getEndTime());
            if (secondsLeft <= 30 && secondsLeft > 0) {
                auction.extendEndTime(1);
                System.out.println("[Anti-Sniping] Phiên " + auctionId + " được gia hạn thêm 1 phút hãy!");
            }

            //  Thông báo Realtime cho các Client khác nếu có gia hạn hoăc không
            notifyAllObservers(auctionId, username, bidAmount);
        }

        return isSuccess;
    }
}
