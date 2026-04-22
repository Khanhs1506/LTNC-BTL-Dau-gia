package com.auction.server.repository;

import com.auction.server.model.Auction;
import java.time.LocalDateTime;
import java.util.List;

public interface IAuctionDAO {
    // Lấy phiên đấu giá theo id
    Auction getAuctionById(int id);

    // Lấy tất cả phiên đấu giá
    List<Auction> getAllAuctions();

    // Lấy các phiên theo trạng thái (ví dụ: RUNNING)
    List<Auction> getAuctionsByStatus(Auction.Status status);

    // Thêm phiên đấu giá mới — trả về id được DB sinh ra, -1 nếu thất bại
    int insertAuction(int itemId, LocalDateTime startTime, LocalDateTime endTime);

    // Cập nhật trạng thái phiên (OPEN → RUNNING → FINISHED / CANCELED)
    boolean updateStatus(int auctionId, Auction.Status status);

    // Cập nhật giá cao nhất và người dẫn đầu sau mỗi bid hợp lệ
    boolean updateHighestBid(int auctionId, double newBid, String winnerUsername);
}