package com.auction.server.repository;

import com.auction.server.model.Auction;
import java.time.LocalDateTime;
import java.util.List;

public interface IAuctionDAO {
    int DEFAULT_PAGE_SIZE = 100;

    // Lấy phiên đấu giá theo id
    Auction getAuctionById(int id);

    // Lấy tất cả phiên đấu giá
    default List<Auction> getAllAuctions() {
        return getAllAuctions(DEFAULT_PAGE_SIZE, 0);
    }
    List<Auction> getAllAuctions(int limit, int offset);

    // Lấy các phiên theo trạng thái
    default List<Auction> getAuctionsByStatus(Auction.Status status) {
        return getAuctionsByStatus(status, DEFAULT_PAGE_SIZE, 0);
    }
    List<Auction> getAuctionsByStatus(Auction.Status status, int limit, int offset);

    //LẤY PHIÊN THEO ID SELLER
    default List<Auction> getAuctionsBySellerId(String sellerId) {
        return getAuctionsBySellerId(sellerId, DEFAULT_PAGE_SIZE, 0);
    }
    List<Auction> getAuctionsBySellerId(String sellerId, int limit, int offset);

    // Thêm phiên đấu giá mới
    default int insertAuction(int itemId, LocalDateTime startTime, LocalDateTime endTime) {
        return insertAuction(itemId, startTime, endTime, 0.0);
    }
    int insertAuction(int itemId, LocalDateTime startTime, LocalDateTime endTime, double bidStep);

    // Cập nhật trạng thái phiên
    boolean updateStatus(int auctionId, Auction.Status status);

    // Cập nhật giá cao nhất
    boolean updateHighestBid(int auctionId, double amount, String winnerUsername);

    // cập nhật thời gian kêt thúc cho anti-snipping
    boolean updateEndTime(int auctionId, LocalDateTime newEndTime);

    //xóa phiên đấu giá theo item_id (dùng khi seller xóa)
    boolean deleteAuctionByItemId(int itemId);

    List<Integer> getSellerPaidAuctionIds(String sellerId);
    boolean markAuctionPaid(int auctionId, String sellerId);
}