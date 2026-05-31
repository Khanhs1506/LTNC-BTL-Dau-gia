
package com.auction.server.service;

import com.auction.server.exception.AuctionClosedException;
import com.auction.server.exception.AuctionNotFoundException;
import com.auction.server.exception.InvalidBidException;
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
 *   3. Anti-sniping: gia hạn phiên nếu bid xuất hiện trong 5 phút cuối.
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

    /** Số giây cuối phiên để áp dụng anti-sniping (5 phút = 300 giây). */
    private static final long ANTI_SNIPE_THRESHOLD_SECONDS = 5 * 60;
    /** Số phút gia hạn khi anti-sniping kích hoạt. */
    private static final int ANTI_SNIPE_EXTENSION_MINUTES = 5;

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

    /**
     * Thông báo đến tất cả observer rằng phiên đấu giá đã được gia hạn (Anti-sniping).
     */
    private void notifyObserversTimeExtended(int auctionId, LocalDateTime newEndTime) {
        for (AuctionObserver obs : observers) {
            try {
                obs.onTimeExtended(auctionId, newEndTime);
            } catch (Exception e) {
                System.err.println("[BiddingEngine] Observer onTimeExtended error: " + e.getMessage());
            }
        }
    }

    // ── Core ──────────────────────────────────────────────────────────────────

    /**
     * Xử lý một lần đặt giá thủ công.
     *
     * @param auctionId ID phiên đấu giá
     * @param username Tên người đặt giá
     * @param bidAmount Số tiền đặt giá
     * @return true nếu đặt giá thành công
     * @throws AuctionNotFoundException nếu không tìm thấy phiên đấu giá
     * @throws AuctionClosedException nếu phiên đã đóng hoặc hết giờ
     * @throws InvalidBidException nếu giá đặt không hợp lệ
     */
    public boolean processBid(int auctionId, String username, double bidAmount)
            throws AuctionNotFoundException, AuctionClosedException, InvalidBidException {
        Auction auction = auctionManager.getAuction(auctionId);
        if (auction == null) {
            throw new AuctionNotFoundException(auctionId);
        }

        // 1. Đặt giá vào object (synchronized bên trong Auction.placeBid)
        boolean success = auction.placeBid(username, bidAmount);

        if (success) {
            // 2. Persist BidTransaction vào DB
            BidTransaction transaction = new BidTransaction(auctionId, username, bidAmount);
            bidDao.insertBid(transaction);

            // 3. Cập nhật giá cao nhất trong DB (bảng auctions)
            auctionDao.updateHighestBid(auctionId, bidAmount, username);

            // 4. Anti-sniping: gia hạn nếu bid xuất hiện trong 5 phút cuối
            synchronized (auction) {
                long secondsLeft = ChronoUnit.SECONDS.between(LocalDateTime.now(), auction.getEndTime());
                if (secondsLeft >= 0 && secondsLeft < ANTI_SNIPE_THRESHOLD_SECONDS) {
                    auction.extendEndTime(ANTI_SNIPE_EXTENSION_MINUTES);
                    LocalDateTime newEndTime = auction.getEndTime();
                    auctionDao.updateEndTime(auctionId, newEndTime);

                    System.out.printf("[Anti-Sniping] Phiên %d gia hạn thêm %d phút (còn %.0f giây). EndTime mới: %s%n",
                            auctionId, ANTI_SNIPE_EXTENSION_MINUTES, (double) secondsLeft, newEndTime);

                    // Thông báo đến tất cả client về thời gian mới
                    notifyObserversTimeExtended(auctionId, newEndTime);
                }
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