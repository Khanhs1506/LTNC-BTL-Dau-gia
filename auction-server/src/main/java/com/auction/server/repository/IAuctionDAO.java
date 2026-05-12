package com.auction.server.repository;

import sample.model.Auction;
import java.time.LocalDateTime;
import java.util.List;

public interface IAuctionDAO {

    // Lấy phiên đấu giá theo id
    Auction getAuctionById(int id);

    // Lấy tất cả phiên đấu giá
    List<Auction> getAllAuctions();

    // Lấy các phiên theo trạng thái
    List<Auction> getAuctionsByStatus(Auction.Status status);

    // Thêm phiên đấu giá mới
    int insertAuction(int itemId, LocalDateTime startTime, LocalDateTime endTime);

    // Cập nhật trạng thái phiên
    boolean updateStatus(int auctionId, Auction.Status status);

    // Cập nhật giá cao nhất
    boolean updateHighestBid(int auctionId, double amount, String winnerUsername);
}