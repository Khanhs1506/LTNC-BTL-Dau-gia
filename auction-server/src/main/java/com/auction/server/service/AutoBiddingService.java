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
 * AutoBiddingService – Dịch vụ đấu giá tự động (Auto-Bid).
 *
 * Yêu cầu đề bài đáp ứng:
 *  ✓ Người dùng đặt maxBid + increment
 *  ✓ Hệ thống tự động trả giá khi có bid mới từ đối thủ
 *  ✓ So sánh nhiều auto-bid cùng lúc bằng PriorityQueue (max-heap)
 *  ✓ Không vượt quá maxBid
 *  ✓ Ưu tiên theo thời điểm đăng ký sớm nhất (tie-break trong compareTo)
 *  ✓ Xử lý xung đột bid đồng thời (synchronized method)
 *  ✓ Persist vào DB (auto_bids + bid_transactions + auctions)
 */
public class AutoBiddingService {

    // ── Singleton ─────────────────────────────────────────────────────────────

    private static final class Holder {
        static final AutoBiddingService INSTANCE = new AutoBiddingService();
    }

    public static AutoBiddingService getInstance() {
        return Holder.INSTANCE;
    }

    // ── State ─────────────────────────────────────────────────────────────────

    /**
     * Mỗi phiên có một PriorityQueue max-heap riêng (maxBid cao nhất ở đầu).
     * ConcurrentHashMap đảm bảo thread-safe khi nhiều phiên chạy song song.
     */
    private final Map<Integer, PriorityQueue<AutoBidEntry>> queueMap =
            new ConcurrentHashMap<>();

    private final IAutoBidDAO    autoBidDao     = new AutoBidDaoImpl();
    private final AuctionManager auctionManager = AuctionManager.getInstance();

    private AutoBiddingService() {}

    // ── API công khai ─────────────────────────────────────────────────────────

    /**
     * Đăng ký auto-bid cho một bidder.
     * Nếu bidder đã đăng ký → upsert (cập nhật maxBid/increment mới).
     *
     * @return true nếu thành công
     */
    public synchronized boolean registerAutoBid(AutoBidEntry entry) {
        // Kiểm tra phiên tồn tại và đang RUNNING
        Auction auction = auctionManager.getAuction(entry.getAuctionId());
        if (auction == null) {
            System.err.println("[AutoBid] Không tìm thấy phiên " + entry.getAuctionId());
            return false;
        }
        if (auction.getStatus() != Auction.Status.RUNNING) {
            System.err.println("[AutoBid] Phiên " + entry.getAuctionId()
                    + " không ở trạng thái RUNNING (hiện tại: " + auction.getStatus() + ")");
            return false;
        }
        if (entry.getMaxBid() <= auction.getCurrentHighestBid()) {
            System.err.println("[AutoBid] maxBid (" + entry.getMaxBid()
                    + ") phải > giá hiện tại (" + auction.getCurrentHighestBid() + ")");
            return false;
        }

        // Persist vào DB (upsert nhờ ON DUPLICATE KEY UPDATE trong AutoBidDaoImpl)
        boolean saved = autoBidDao.insertAutoBid(entry);
        if (!saved) return false;

        // Cập nhật PriorityQueue trong RAM
        PriorityQueue<AutoBidEntry> pq = queueMap.computeIfAbsent(
                entry.getAuctionId(), id -> new PriorityQueue<>());

        // Xóa entry cũ của user này nếu đã tồn tại (upsert)
        pq.removeIf(e -> e.getUsername().equals(entry.getUsername()));
        pq.add(entry);

        System.out.println("[AutoBid] Đăng ký thành công: " + entry);
        return true;
    }

    /**
     * Hủy đăng ký auto-bid của một bidder.
     *
     * @return true nếu đã có entry và xóa thành công
     */
    public synchronized boolean cancelAutoBid(int auctionId, String username) {
        boolean deleted = autoBidDao.deleteAutoBid(auctionId, username);
        PriorityQueue<AutoBidEntry> pq = queueMap.get(auctionId);
        if (pq != null) {
            pq.removeIf(e -> e.getUsername().equals(username));
        }
        System.out.println("[AutoBid] Hủy auto-bid: auction=" + auctionId + " user=" + username);
        return deleted;
    }

    /**
     * Nạp lại toàn bộ auto-bid của một phiên từ DB vào RAM.
     * Gọi khi server restart hoặc khi phiên mới được load.
     */
    public void loadFromDatabase(int auctionId) {
        List<AutoBidEntry> entries = autoBidDao.getAutoBidsByAuction(auctionId);
        PriorityQueue<AutoBidEntry> pq = new PriorityQueue<>(entries.isEmpty() ? 1 : entries.size());
        pq.addAll(entries);
        queueMap.put(auctionId, pq);
        System.out.println("[AutoBid] Loaded " + entries.size()
                + " auto-bid(s) cho phiên " + auctionId);
    }

    /**
     * Kích hoạt auto-bid sau khi có bid mới (thủ công hoặc auto).
     * Được gọi từ BiddingEngine.processBid() sau mỗi bid thành công.
     *
     * Thuật toán:
     *  - Lấy top PriorityQueue (maxBid cao nhất / đăng ký sớm nhất).
     *  - Nếu top là người vừa đặt → dừng (không tự đua với chính mình).
     *  - Nếu top.maxBid >= currentHighest + increment → auto-bid.
     *  - Nếu top.maxBid < nextBid → loại khỏi queue và thử người tiếp theo.
     *  - Lặp đến khi không còn ai có thể auto-bid cao hơn.
     *
     * @param auctionId   phiên vừa có bid mới
     * @param triggerUser user vừa đặt giá (bỏ qua auto-bid của chính họ)
     * @param engine      BiddingEngine để persist kết quả vào DB
     */
    public synchronized void triggerAutoBids(int auctionId, String triggerUser,
                                             BiddingEngine engine) {
        Auction auction = auctionManager.getAuction(auctionId);
        if (auction == null || auction.getStatus() != Auction.Status.RUNNING) return;

        PriorityQueue<AutoBidEntry> pq = queueMap.get(auctionId);
        if (pq == null || pq.isEmpty()) return;

        boolean biddingHappened;
        do {
            biddingHappened = false;
            AutoBidEntry top = pq.peek();
            if (top == null) break;

            // Dừng nếu người đầu queue là người vừa đặt (tránh vòng lặp vô hạn)
            if (top.getUsername().equals(triggerUser)) break;

            double currentHighest = auction.getCurrentHighestBid();
            double nextBid        = currentHighest + top.getIncrement();

            if (top.getMaxBid() < nextBid) {
                // Hết ngân sách → loại khỏi queue và DB
                pq.poll();
                autoBidDao.deleteAutoBid(auctionId, top.getUsername());
                System.out.println("[AutoBid] " + top.getUsername()
                        + " hết ngân sách, loại khỏi queue (maxBid="
                        + top.getMaxBid() + " < nextBid=" + nextBid + ")");
                continue; // thử người tiếp theo
            }

            // Đặt giá tự động vào object Auction (thread-safe)
            try {
                boolean success = auction.placeBid(top.getUsername(), nextBid);
                if (success) {
                    // Persist vào DB
                    engine.persistAutoBid(auctionId, top.getUsername(), nextBid);

                    System.out.printf("[AutoBid] %s tự động đặt %.0f cho phiên %d%n",
                            top.getUsername(), nextBid, auctionId);

                    biddingHappened = true;
                    // Vòng tiếp theo bỏ qua người này
                    triggerUser = top.getUsername();
                }
            } catch (Exception e) {
                System.err.println("[AutoBid] Lỗi khi auto-bid: " + e.getMessage());
                break;
            }

        } while (biddingHappened);
    }

    /**
     * Kiểm tra user đã đăng ký auto-bid cho phiên này chưa.
     */
    public boolean hasAutoBid(int auctionId, String username) {
        return autoBidDao.existsAutoBid(auctionId, username);
    }

    /**
     * Dọn dẹp queue khi phiên kết thúc (giải phóng RAM).
     */
    public void clearQueue(int auctionId) {
        queueMap.remove(auctionId);
        System.out.println("[AutoBid] Đã xóa queue của phiên " + auctionId);
    }

    // Map lưu minutesTrigger của từng user/auction
    private final Map<String, Integer> triggerMinutesMap = new ConcurrentHashMap<>();

    private String triggerKey(int auctionId, String username) {
        return auctionId + ":" + username;
    }

    public void setMinutesTrigger(int auctionId, String username, int minutes) {
        triggerMinutesMap.put(triggerKey(auctionId, username), minutes);
    }

    public int getMinutesTrigger(int auctionId, String username) {
        return triggerMinutesMap.getOrDefault(triggerKey(auctionId, username), 5);
    }

    public AutoBidEntry getAutoBid(int auctionId, String username) {
        PriorityQueue<AutoBidEntry> pq = queueMap.get(auctionId);
        if (pq == null) return null;
        return pq.stream()
                .filter(e -> e.getUsername().equals(username))
                .findFirst().orElse(null);
    }

    public PriorityQueue<AutoBidEntry> getQueue(int auctionId) {
        return queueMap.get(auctionId);
    }
}