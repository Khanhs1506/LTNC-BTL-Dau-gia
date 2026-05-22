package com.auction.server.service;

import com.auction.server.model.Auction;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


public class BiddingEngine {

    // ── Singleton ─────────────────────────────────────────────────────────────
    private static BiddingEngine instance;

    private final AuctionManager     auctionManager;
    private final AutoBiddingService autoBiddingService;
    private final List<AuctionObserver> observers;

    private BiddingEngine() {
        this.auctionManager     = AuctionManager.getInstance();
        this.autoBiddingService = AutoBiddingService.getInstance();
        this.observers          = new CopyOnWriteArrayList<>();
    }

    public static synchronized BiddingEngine getInstance() {
        if (instance == null) {
            instance = new BiddingEngine();
        }
        return instance;
    }

    // ── Observer ──────────────────────────────────────────────────────────────

    public void addObserver(AuctionObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(AuctionObserver observer) {
        observers.remove(observer);
    }

    private void notifyAllObservers(int auctionId, String username, double bidAmount) {
        for (AuctionObserver observer : observers) {
            observer.onNewBidPlaced(auctionId, username, bidAmount);
        }
    }

    // ── Core Logic ────────────────────────────────────────────────────────────

    /**
     * Xử lý một lần đặt giá thủ công.
     * Sau khi bid thủ công thành công, kích hoạt auto-bid cho các bidder khác.
     */
    public boolean processBid(int auctionId, String username, double bidAmount) throws Exception {
        Auction auction = auctionManager.getAuction(auctionId);
        if (auction == null) {
            throw new Exception("Lỗi: Không tìm thấy phiên đấu giá với ID " + auctionId);
        }

        // 1. Đặt giá thủ công
        boolean isSuccess = auction.placeBid(username, bidAmount);

        if (isSuccess) {
            // 2. Anti-sniping: gia hạn nếu còn <= 30 giây
            long secondsLeft = ChronoUnit.SECONDS.between(LocalDateTime.now(), auction.getEndTime());
            if (secondsLeft <= 30 && secondsLeft > 0) {
                auction.extendEndTime(1);
                System.out.println("[Anti-Sniping] Phiên " + auctionId + " được gia hạn thêm 1 phút!");
            }

            // 3. Thông báo realtime cho tất cả client
            notifyAllObservers(auctionId, username, bidAmount);

            // 4. Kích hoạt auto-bid cho các bidder khác
            //    (chạy sau notify để client nhận giá thủ công trước)
            autoBiddingService.triggerAutoBids(auctionId, username);

            // 5. Nếu auto-bid đã nâng giá, thông báo lại giá mới nhất
            double newHighest = auction.getCurrentHighestBid();
            if (newHighest > bidAmount) {
                notifyAllObservers(auctionId, auction.getCurrentWinnerUsername(), newHighest);
            }
        }

        return isSuccess;
    }
}
