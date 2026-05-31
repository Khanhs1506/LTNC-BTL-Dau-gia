package com.auction.server.model;

import com.auction.server.exception.AuctionClosedException;
import com.auction.server.exception.InvalidBidException;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


public class Auction implements Serializable {
    private static final long serialVersionUID = 1L; // gửi nhận qua mạng

    // quản lý trạng thái phiên đấu giá
    public enum Status {
        OPEN,
        RUNNING,
        FINISHED,
        CANCELED
    }

    private final int id;
    private final Item item;
    private double currentHighestBid;
    private String currentWinnerUsername;
    private final LocalDateTime startTime;
    private LocalDateTime endTime;
    private Status status;

    // Bước giá tối thiểu mỗi lần đặt
    private double bidStep = 0;

    // Lưu lịch sử đặt giá.
    private final List<BidTransaction> bidHistory;

    public Auction(int id, Item item, LocalDateTime startTime, LocalDateTime endTime) {
        this.id = id;
        this.item = item;
        this.currentHighestBid = item.getStartingPrice(); // Giá khởi điểm
        this.currentWinnerUsername = null;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = Status.OPEN;
        this.bidHistory = new ArrayList<>();
    }

    public synchronized boolean placeBid(String username, double bidAmount)
            throws AuctionClosedException, InvalidBidException {
        //  Kiểm tra trạng thái phiên
        if (this.status != Status.RUNNING) {
            throw new AuctionClosedException(
                    "Phiên đấu giá #" + id + " không trong trạng thái RUNNING (hiện tại: " + this.status + ")!"
            );
        }

        // 2. Kiểm tra thời gian thực tế
        if (LocalDateTime.now().isAfter(endTime)) {
            this.status = Status.FINISHED;
            throw new AuctionClosedException(
                    "Phiên đấu giá #" + id + " đã kết thúc thời gian (endTime: " + endTime + ")!"
            );
        }

        // Kiểm tra bước giá
        if (bidStep > 0) {
            double minBid = currentHighestBid + bidStep;
            if (bidAmount < minBid) {
                throw new InvalidBidException(
                        "Giá đặt (" + bidAmount + ") phải tối thiểu " + minBid
                                + " (giá hiện tại: " + currentHighestBid + ", bước giá: " + bidStep + ")!"
                );
            }
        } else {
            if (bidAmount <= currentHighestBid) {
                throw new InvalidBidException(
                        "Giá đặt (" + bidAmount + ") phải lớn hơn giá hiện tại (" + currentHighestBid + ")!"
                );
            }
        }

        //  Cập nhật thông tin người thắng hiện tại
        this.currentHighestBid = bidAmount;
        this.currentWinnerUsername = username;

        // Lưu vào lịch sử
        BidTransaction transaction = new BidTransaction(this.id, username, bidAmount);
        bidHistory.add(transaction);

        System.out.println("[Auction " + id + "] Cập nhật giá mới: " + bidAmount + " bởi " + username);
        return true;
    }

    public void startAuction() {
        if (this.status == Status.OPEN && LocalDateTime.now().isAfter(startTime) && LocalDateTime.now().isBefore(endTime)) {
            this.status = Status.RUNNING;
        }
    }

    public void closeAuction() {
        this.status = Status.FINISHED;
    }

    //chức năng nâng cao anti-snipping để gia hạn thời gian đấu giá
    public synchronized void extendEndTime(int minutes) {
        this.endTime = this.endTime.plusMinutes(minutes);
    }

    // lấy thông tin
    public int getId() { return id; }
    public Item getItem() { return item; }
    public double getCurrentHighestBid() { return currentHighestBid; }
    public String getCurrentWinnerUsername() { return currentWinnerUsername; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public Status getStatus() { return status; }
    public List<BidTransaction> getBidHistory() { return new ArrayList<>(bidHistory); }

    public double getBidStep() { return bidStep; }
    public void setBidStep(double bidStep) { this.bidStep = bidStep; }

    // Dùng khi load dữ liệu từ DB — đồng bộ giá và người thắng vào RAM
    public void updateHighestBid(double highestBid, String winnerUsername) {
        this.currentHighestBid = highestBid;
        this.currentWinnerUsername = winnerUsername;
    }

    // Dùng khi load dữ liệu từ DB — đồng bộ trạng thái vào RAM
    public void updateStatus(Status newStatus) {
        this.status = newStatus;
    }
}