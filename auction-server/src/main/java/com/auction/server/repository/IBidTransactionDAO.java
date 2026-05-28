package com.auction.server.repository;

import com.auction.server.model.BidTransaction;
import java.util.List;
import java.util.Map;

public interface IBidTransactionDAO {
    int DEFAULT_PAGE_SIZE = 100;

    // Lưu 1 lần đặt giá xuống DB
    boolean insertBid(BidTransaction transaction);

    // Lấy toàn bộ lịch sử đặt giá của 1 phiên
    List<BidTransaction> getBidsByAuctionId(int auctionId);

    // Lấy giá cao nhất hiện tại của 1 phiên
    BidTransaction getHighestBid(int auctionId);

    //LẤY TẤT CẢ LỊCH SỬ ĐẶT GIÁ
    default List<BidTransaction> getAllBids() {
        return getAllBids(DEFAULT_PAGE_SIZE, 0);
    }
    List<BidTransaction> getAllBids(int limit, int offset);
    Map<Integer, Integer> getBidCounts(List<Integer> auctionIds);
}