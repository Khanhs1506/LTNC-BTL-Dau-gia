package com.auction.server.service;

import com.auction.server.model.Auction;
import com.auction.server.model.AutoBidEntry;
import com.auction.server.repository.AuctionDaoImpl;
import com.auction.server.repository.IAuctionDAO;
import com.auction.server.repository.IItemDAO;
import com.auction.server.model.User;
import com.auction.server.model.WalletTransaction;
import com.auction.server.repository.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AuctionManager – Singleton quản lý tất cả các phiên đấu giá đang hoạt động trong RAM.
 * Mọi thao tác thêm / kết thúc phiên đều được đồng bộ xuống DB qua IAuctionDAO.
 */
public class AuctionManager {

    // ── Singleton ─────────────────────────────────────────────────────────────

    private static class Holder {
        private static final AuctionManager INSTANCE = new AuctionManager();
    }

    public static AuctionManager getInstance() {
        return Holder.INSTANCE;
    }

    // ── State ─────────────────────────────────────────────────────────────────

    /** Cache RAM: auctionId → Auction (chỉ chứa các phiên chưa FINISHED/CANCELED). */
    private final Map<Integer, Auction> activeAuctions = new ConcurrentHashMap<>();

    private final IAuctionDAO auctionDao = new AuctionDaoImpl();

    private AuctionManager() {}

    // ── Khởi tạo ──────────────────────────────────────────────────────────────

    /**
     * Load tất cả phiên đấu giá OPEN và RUNNING từ DB vào RAM.
     * Gọi một lần duy nhất khi ServerApp khởi động.
     */
    public void loadFromDatabase() {
        List<Auction> openAuctions    = auctionDao.getAuctionsByStatus(Auction.Status.OPEN);
        List<Auction> runningAuctions = auctionDao.getAuctionsByStatus(Auction.Status.RUNNING);

        for (Auction a : openAuctions)    activeAuctions.put(a.getId(), a);
        for (Auction a : runningAuctions) activeAuctions.put(a.getId(), a);

        System.out.println("[AuctionManager] Loaded " + activeAuctions.size()
                + " active auction(s) from database.");
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    /**
     * Thêm phiên đấu giá mới: lưu DB trước, rồi thêm vào cache RAM.
     *
     * @return id phiên được DB tạo ra, hoặc -1 nếu thất bại.
     */
    public int createAuction(int itemId, LocalDateTime startTime, LocalDateTime endTime, double bidStep) {
        int auctionId = auctionDao.insertAuction(itemId, startTime, endTime, bidStep);
        if (auctionId <= 0) return -1;

        // Load lại từ DB để có đầy đủ thông tin (item, ...)
        Auction auction = auctionDao.getAuctionById(auctionId);
        if (auction != null) {
            activeAuctions.put(auctionId, auction);
            LocalDateTime now = LocalDateTime.now();
            if (auction.getStatus() == Auction.Status.OPEN
                    && !now.isBefore(auction.getStartTime())
                    && now.isBefore(auction.getEndTime())) {
                auction.startAuction();
                auctionDao.updateStatus(auctionId, Auction.Status.RUNNING);
                System.out.println("[AuctionManager] Phiên " + auctionId
                        + " tạo xong → tự động RUNNING ngay.");
            }
            System.out.println("[AuctionManager] Tạo phiên đấu giá id=" + auctionId);
        }
        return auctionId;
    }

    /**
     * Thêm thẳng object Auction vào cache (dùng khi đã có auction tạo từ ngoài).
     */
    public void addAuction(Auction auction) {
        if (auction != null) {
            activeAuctions.put(auction.getId(), auction);
        }
    }

    /** Lấy phiên từ cache RAM. */
    public Auction getAuction(int auctionId) {
        return activeAuctions.get(auctionId);
    }

    /** Lấy toàn bộ phiên đang trong RAM (OPEN + RUNNING). */
    public List<Auction> getAllActiveAuctions() {
        return new ArrayList<>(activeAuctions.values());
    }

    //Kết thúc phiên đấu giá: cập nhật DB → tự động trừ tiền người thắng → xóa khỏi cache RAM.
    public synchronized void endAuction(int auctionId) {
        Auction auction = activeAuctions.get(auctionId);
        if (auction != null) {
            auction.closeAuction();
            auctionDao.updateStatus(auctionId, Auction.Status.FINISHED);
            //Tự động thanh toán cho người thắng
            String winner   = auction.getCurrentWinnerUsername();
            double winBid   = auction.getCurrentHighestBid();
            double startPrice = auction.getItem().getStartingPrice();
            if (winner != null && winBid > startPrice) {
                try {
                    // Lấy userId từ username
                    IUserDAO userDao = new UserDaoImpl();
                    User winnerUser = userDao.getUserByUsername(winner);
                    if (winnerUser != null) {
                        String winnerId = String.valueOf(winnerUser.getId());
                        IWalletDAO walletDao = new WalletDaoImpl();
                        WalletTransaction tx = walletDao.payment(winnerId, winBid, auctionId,
                                String.format("Thanh toán đấu giá #%d - %s",
                                        auctionId, auction.getItem().getName()));
                        if (tx != null) {
                            System.out.printf("[AuctionManager] AUTO_PAYMENT phiên %d: user=%s | -%.0f | thành công%n", auctionId, winner, winBid);
                        } else {
                            System.err.printf("[AuctionManager] AUTO_PAYMENT phiên %d: THẤT BẠI (số dư không đủ?) user=%s%n", auctionId, winner);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("[AuctionManager] AUTO_PAYMENT lỗi phiên " + auctionId + ": " + e.getMessage());
                }
            }
            activeAuctions.remove(auctionId);
            System.out.println("[AuctionManager] Phiên " + auctionId + " đã kết thúc.");
        }
    }

    /**
     * Hủy phiên đấu giá.
     */
    public synchronized void cancelAuction(int auctionId) {
        Auction auction = activeAuctions.get(auctionId);
        if (auction != null) {
            auction.updateStatus(Auction.Status.CANCELED);
            auctionDao.updateStatus(auctionId, Auction.Status.CANCELED);
            activeAuctions.remove(auctionId);
            System.out.println("[AuctionManager] Phiên " + auctionId + " đã bị hủy.");
        }
    }

    /**
     * Cập nhật trạng thái một phiên trong cả cache lẫn DB.
     */
    public void updateAuctionStatus(int auctionId, Auction.Status status) {
        Auction auction = activeAuctions.get(auctionId);
        if (auction != null) {
            auction.updateStatus(status);
        }
        auctionDao.updateStatus(auctionId, status);
    }

    public synchronized boolean deleteItemAndAuction(int itemId, IItemDAO itemDao) {
        Integer foundAuctionId = null;
        for (Map.Entry<Integer, Auction> entry : activeAuctions.entrySet()) {
            if (entry.getValue().getItem().getId() != null && entry.getValue().getItem().getId().equalsIgnoreCase(String.valueOf(itemId))) {
                foundAuctionId = entry.getKey();
                break;
            }
        }
        if (foundAuctionId != null) {
            activeAuctions.remove(foundAuctionId);
            System.out.println("[AuctionManager] Xóa phiên id=" + foundAuctionId + " khỏi RAM (item_id=" + itemId + ")");
        }

        boolean auctionDeleted = auctionDao.deleteAuctionByItemId(itemId);
        boolean itemDeleted = itemDao.deleteItem(itemId);
        return auctionDeleted && itemDeleted;
    }

    /**
     * Duyệt tất cả phiên trong RAM và tự động chuyển trạng thái theo thời gian thực.
     * Gọi định kỳ từ một ScheduledExecutorService trong ServerApp.
     */
    public void checkAndUpdateStatuses() {
        LocalDateTime now = LocalDateTime.now();
        for (Auction auction : activeAuctions.values()) {
            synchronized (auction) {
                // Đọc lại endTime bên trong synchronized để lấy giá trị mới nhất sau khi anti-sniping gia hạn
                LocalDateTime endTime = auction.getEndTime();
                if (auction.getStatus() == Auction.Status.OPEN
                        && !now.isBefore(auction.getStartTime())
                        && now.isBefore(endTime)) {
                    auction.startAuction();
                    auctionDao.updateStatus(auction.getId(), Auction.Status.RUNNING);
                    System.out.println("[AuctionManager] Phiên " + auction.getId() + " chuyển sang RUNNING.");
                } else if ((auction.getStatus() == Auction.Status.OPEN
                        || auction.getStatus() == Auction.Status.RUNNING)
                        && !now.isBefore(endTime)) {
                    endAuction(auction.getId());
                }
            }
        }

        for (Auction auction : activeAuctions.values()) {
            if (auction.getStatus() != Auction.Status.RUNNING) continue;
            long secsLeft = java.time.temporal.ChronoUnit.SECONDS
                    .between(java.time.LocalDateTime.now(), auction.getEndTime());

            PriorityQueue<AutoBidEntry> pq =
                    AutoBiddingService.getInstance().getQueue(auction.getId());
            if (pq == null || pq.isEmpty()) continue;

            for (com.auction.server.model.AutoBidEntry entry : pq) {
                int triggerMins = AutoBiddingService.getInstance()
                        .getMinutesTrigger(auction.getId(), entry.getUsername());
                long triggerSecs = triggerMins * 60L;

                // Trigger khi còn đúng trong khoảng triggerSecs (±5 giây để không bỏ lỡ)
                if (secsLeft <= triggerSecs && secsLeft > triggerSecs - 5) {
                    System.out.println("[AutoBid] Trigger theo giờ cho "
                            + entry.getUsername() + " phiên " + auction.getId());
                    AutoBiddingService.getInstance().triggerAutoBids(
                            auction.getId(), "", BiddingEngine.getInstance());
                    break;
                }
            }
        }
    }
}