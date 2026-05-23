package com.auction.server.service;

import com.auction.server.model.Auction;
import com.auction.server.model.BidTransaction;
import com.auction.server.repository.AuctionDaoImpl;
import com.auction.server.repository.BidTransactionDaoImpl;
import com.auction.server.repository.IAuctionDAO;
import com.auction.server.repository.IBidTransactionDAO;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * BiddingEngine – Xử lý toàn bộ logic đặt giá:
 *   1. Validate và đặt giá vào Auction (in-memory, thread-safe).
 *   2. Persist BidTransaction + cập nhật currentHighestBid trong DB.
 *   3. Anti-sniping: gia hạn phiên nếu bid xuất hiện trong 30 giây cuối.
 *   4. Notify tất cả Observer (realtime update cho client).
 *   5. Kích hoạt AutoBiddingService sau mỗi bid thành công.
 */
public class BiddingEngine {

    // ── Singleton ─────────────────────────────────────────────────────────────

    private static BiddingEngine instance;

    public static synchronized BiddingEngine getInstance() {
        if (instance == null) instance = new BiddingEngine();
        return instance;
    }

    // ── Dependencies ──────────────────────────────────────────────────────────

    private final AuctionManager       auctionManager;
    private final AutoBiddingService   autoBiddingService;
    private final IAuctionDAO          auctionDao;
    private final IBidTransactionDAO   bidDao;
    private final List<AuctionObserver> observers;

    /** Số giây cuối phiên để áp dụng anti-sniping. */
    private static final int ANTI_SNIPE_THRESHOLD_SECONDS = 30;
    /** Số phút gia hạn khi anti-sniping. */
    private static final int ANTI_SNIPE_EXTENSION_MINUTES = 1;

    private BiddingEngine() {
        this.auctionManager     = AuctionManager.getInstance();
        this.autoBiddingService = AutoBiddingService.getInstance();
        this.auctionDao         = new AuctionDaoImpl();
        this.bidDao             = new BidTransactionDaoImpl();
        this.observers          = new CopyOnWriteArrayList<>();
    }

    // ── Observer ──────────────────────────────────────────────────────────────

    public void addObserver(AuctionObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(AuctionObserver observer) {
        observers.remove(observer);
    }

    private void notifyObservers(int auctionId, String username, double bidAmount) {
        for (AuctionObserver obs : observers) {
            try {
                obs.onNewBidPlaced(auctionId, username, bidAmount);
            } catch (Exception e) {
                System.err.println("[BiddingEngine] Observer error: " + e.getMessage());
            }
        }
    }

    // ── Core ──────────────────────────────────────────────────────────────────

    /**
     * Xử lý một lần đặt giá thủ công.
     *
     * @param auctionId  ID phiên đấu giá
     * @param username   Tên người đặt giá
     * @param bidAmount  Số tiền đặt giá
     * @return true nếu đặt giá thành công
     * @throws Exception nếu phiên không hợp lệ hoặc giá không đủ cao
     */
    public boolean processBid(int auctionId, String username, double bidAmount) throws Exception {
        Auction auction = auctionManager.getAuction(auctionId);
        if (auction == null) {
            throw new Exception("Không tìm thấy phiên đấu giá với ID " + auctionId);
        }

        // 1. Đặt giá vào object (synchronized bên trong Auction.placeBid)
        boolean success = auction.placeBid(username, bidAmount);

        if (success) {
            // 2. Persist BidTransaction vào DB
            BidTransaction transaction = new BidTransaction(auctionId, username, bidAmount);
            bidDao.insertBid(transaction);

            // 3. Cập nhật giá cao nhất trong DB (bảng auctions)
            auctionDao.updateHighestBid(auctionId, bidAmount, username);

            // 4. Anti-sniping: gia hạn nếu còn <= 30 giây
            long secondsLeft = ChronoUnit.SECONDS.between(LocalDateTime.now(), auction.getEndTime());
            if (secondsLeft > 0 && secondsLeft <= ANTI_SNIPE_THRESHOLD_SECONDS) {
                auction.extendEndTime(ANTI_SNIPE_EXTENSION_MINUTES);
                // Cập nhật end_time mới vào DB
                auctionDao.updateEndTime(auctionId, auction.getEndTime());
                System.out.printf("[Anti-Sniping] Phiên %d gia hạn thêm %d phút (còn %ds)%n",
                        auctionId, ANTI_SNIPE_EXTENSION_MINUTES, secondsLeft);
            }

            // 5. Notify observers (realtime update cho tất cả client)
            notifyObservers(auctionId, username, bidAmount);

            // 6. Kích hoạt auto-bid cho các bidder khác
            autoBiddingService.triggerAutoBids(auctionId, username, this);

            // 7. Nếu auto-bid đã nâng giá cao hơn, notify thêm lần nữa
            double latestHighest = auction.getCurrentHighestBid();
            if (latestHighest > bidAmount) {
                notifyObservers(auctionId, auction.getCurrentWinnerUsername(), latestHighest);
            }
        }

        return success;
    }

    /**
     * Persist một auto-bid vào DB (gọi từ AutoBiddingService).
     * Tách ra để AutoBiddingService không cần tự import DAO.
     */
    public void persistAutoBid(int auctionId, String username, double bidAmount) {
        BidTransaction transaction = new BidTransaction(auctionId, username, bidAmount);
        bidDao.insertBid(transaction);
        auctionDao.updateHighestBid(auctionId, bidAmount, username);
    }

    /**
     * Notify observers từ AutoBiddingService (package-level access).
     */
    void notifyObserversPublic(int auctionId, String username, double bidAmount) {
        notifyObservers(auctionId, username, bidAmount);
    }
}
