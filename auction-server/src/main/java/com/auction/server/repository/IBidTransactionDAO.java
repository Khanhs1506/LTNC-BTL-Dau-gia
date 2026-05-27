package com.auction.server.repository;

import com.auction.server.model.BidTransaction;
import java.util.List;

public interface IBidTransactionDAO {

    // Lưu 1 lần đặt giá xuống DB
    boolean insertBid(BidTransaction transaction);

    // Lấy toàn bộ lịch sử đặt giá của 1 phiên
    List<BidTransaction> getBidsByAuctionId(int auctionId);

    // Lấy giá cao nhất hiện tại của 1 phiên
    BidTransaction getHighestBid(int auctionId);

    //LẤY TẤT CẢ LỊCH SỬ ĐẶT GIÁ
    List<BidTransaction> getAllBids();
}