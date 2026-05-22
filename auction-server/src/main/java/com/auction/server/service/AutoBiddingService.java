package com.auction.server.service;

import com.auction.server.model.AutoBidEntry;
import com.auction.server.model.Auction;
import com.auction.server.repository.AutoBidDaoImpl;
import com.auction.server.repository.IAutoBidDAO;

import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AutoBiddingService – Dịch vụ đấu giá tự động.
 *
 * Luồng hoạt động:
 *  1. Bidder đăng ký auto-bid (maxBid + increment) qua registerAutoBid().
 *  2. Sau mỗi lần có bid thủ công thành công, BiddingEngine gọi triggerAutoBids().
 *  3. Service lấy PriorityQueue của phiên (max-heap theo maxBid, tie-break theo
 *     thời gian đăng ký sớm nhất), rồi tự động đặt giá cho người có thể thắng.
 *
 * Yêu cầu đề bài đã đáp ứng:
 *  - maxBid + increment ✓
 *  - Không vượt quá maxBid ✓
 *  - So sánh nhiều auto-bid cùng lúc bằng PriorityQueue ✓
 *  - Ưu tiên theo thời điểm đăng ký (tie-break trong compareTo) ✓
 *  - Xử lý xung đột bid đồng thời (synchronized + delegate sang Auction.placeBid) ✓
 */
public class AutoBiddingService {

    // ── Singleton

    private static final class Holder {
        static final AutoBiddingService INSTANCE = new AutoBiddingService();
    }

    public static AutoBiddingService getInstance() {
        return Holder.INSTANCE;
    }

    // ── State ─

    /**
     * Mỗi phiên đấu giá có một PriorityQueue riêng (max-heap).
     * Dùng ConcurrentHashMap để thread-safe khi nhiều phiên chạy cùng lúc.
     */
    private final Map<Integer, PriorityQueue<AutoBidEntry>> queueMap =
            new ConcurrentHashMap<>();

    private final IAutoBidDAO autoBidDao = new AutoBidDaoImpl();
    private final AuctionManager auctionManager = AuctionManager.getInstance();

    private AutoBiddingService() {}

    // ── API công khai ─────────────────────────────────────────────────────────

    /**
     * Đăng ký auto-bid cho một bidder.
     * Nếu bidder đã đăng ký trước → cập nhật entry mới (upsert).
     *
     * @return true nếu lưu DB thành công
     */
    public synchronized boolean registerAutoBid(AutoBidEntry entry) {
        // 1. Kiểm tra phiên tồn tại và đang RUNNING
        Auction auction = auctionManager.getAuction(entry.getAuctionId());
        if (auction == null) {
            System.err.println("[AutoBid] Không tìm thấy phiên " + entry.getAuctionId());
            return false;
        }
        if (auction.getStatus() != Auction.Status.RUNNING) {
            System.err.println("[AutoBid] Phiên " + entry.getAuctionId() + " không RUNNING");
            return false;
        }
        if (entry.getMaxBid() <= auction.getCurrentHighestBid()) {
            System.err.println("[AutoBid] maxBid phải > giá hiện tại (" +
                    auction.getCurrentHighestBid() + ")");
            return false;
        }

        // 2. Lưu vào DB
        boolean saved = autoBidDao.insertAutoBid(entry);
        if (!saved) return false;

        // 3. Cập nhật PriorityQueue trong RAM
        PriorityQueue<AutoBidEntry> pq = queueMap.computeIfAbsent(
                entry.getAuctionId(), id -> new PriorityQueue<>());

        // Xóa entry cũ của cùng user nếu có (upsert)
        pq.removeIf(e -> e.getUsername().equals(entry.getUsername()));
        pq.add(entry);

        System.out.println("[AutoBid] Đã đăng ký: " + entry);
        return true;
    }

    /**
     * Hủy đăng ký auto-bid của một bidder.
     */
    public synchronized boolean cancelAutoBid(int auctionId, String username) {
        boolean deleted = autoBidDao.deleteAutoBid(auctionId, username);
        PriorityQueue<AutoBidEntry> pq = queueMap.get(auctionId);
        if (pq != null) {
            pq.removeIf(e -> e.getUsername().equals(username));
        }
        return deleted;
    }

    /**
     * Nạp lại toàn bộ auto-bid của một phiên từ DB vào RAM (dùng khi server restart).
     */
    public void loadFromDatabase(int auctionId) {
        List<AutoBidEntry> entries = autoBidDao.getAutoBidsByAuction(auctionId);
        PriorityQueue<AutoBidEntry> pq = new PriorityQueue<>(entries);
        queueMap.put(auctionId, pq);
        System.out.println("[AutoBid] Loaded " + entries.size() +
                " auto-bid entries for auction " + auctionId);
    }

    /**
     * Kích hoạt auto-bid sau khi có bid mới (thủ công hoặc auto).
     * Phương thức này được gọi từ BiddingEngine mỗi khi placeBid() thành công.
     *
     * Thuật toán:
     *  - Lấy top của PriorityQueue (maxBid cao nhất / đăng ký sớm nhất).
     *  - Nếu top.maxBid > currentHighest + increment → đặt giá tự động.
     *  - Xóa entry của bidder hiện tại khỏi queue nếu họ vừa đặt thủ công.
     *  - Lặp lại cho đến khi không còn ai có thể auto-bid hơn.
     *
     * @param auctionId    phiên đấu giá vừa có bid mới
     * @param triggerUser  username vừa đặt giá (để bỏ qua auto-bid của chính họ)
     */
    public synchronized void triggerAutoBids(int auctionId, String triggerUser) {
        Auction auction = auctionManager.getAuction(auctionId);
        if (auction == null || auction.getStatus() != Auction.Status.RUNNING) return;

        PriorityQueue<AutoBidEntry> pq = queueMap.get(auctionId);
        if (pq == null || pq.isEmpty()) return;

        boolean biddingHappened;
        do {
            biddingHappened = false;
            AutoBidEntry top = pq.peek();
            if (top == null) break;

            // Bỏ qua nếu top là người vừa đặt giá (để tránh vòng lặp vô tận)
            if (top.getUsername().equals(triggerUser)) break;

            double currentHighest = auction.getCurrentHighestBid();
            double nextBid = currentHighest + top.getIncrement();

            // Kiểm tra: người này còn có thể auto-bid không?
            if (top.getMaxBid() < nextBid) {
                // maxBid không đủ → loại khỏi hàng đợi
                pq.poll();
                autoBidDao.deleteAutoBid(auctionId, top.getUsername());
                System.out.println("[AutoBid] " + top.getUsername() +
                        " hết ngân sách, bị loại khỏi queue.");
                continue; // thử người tiếp theo
            }

            // Đặt giá tự động
            try {
                boolean success = auction.placeBid(top.getUsername(), nextBid);
                if (success) {
                    System.out.printf("[AutoBid] %s tự động đặt giá %.0f cho phiên %d%n",
                            top.getUsername(), nextBid, auctionId);
                    biddingHappened = true;
                    // Cập nhật triggerUser để vòng tiếp theo bỏ qua người này
                    triggerUser = top.getUsername();
                }
            } catch (Exception e) {
                System.err.println("[AutoBid] Lỗi khi auto-bid: " + e.getMessage());
                break;
            }

        } while (biddingHappened);
    }

    /**
     * Dọn dẹp queue khi phiên kết thúc (giải phóng RAM).
     */
    public void clearQueue(int auctionId) {
        queueMap.remove(auctionId);
        System.out.println("[AutoBid] Đã xóa queue của phiên " + auctionId);
    }

    /**
     * Kiểm tra xem một user đã đăng ký auto-bid cho phiên này chưa.
     */
    public boolean hasAutoBid(int auctionId, String username) {
        return autoBidDao.existsAutoBid(auctionId, username);
    }
}