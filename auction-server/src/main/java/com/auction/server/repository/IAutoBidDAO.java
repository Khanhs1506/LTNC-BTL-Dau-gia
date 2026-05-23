package com.auction.server.repository;

import com.auction.server.model.AutoBidEntry;
import java.util.List;


public interface IAutoBidDAO {

    // Lưu một đăng ký auto-bid vào DB. Trả về true nếu thành công.
    boolean insertAutoBid(AutoBidEntry entry);


//     Xóa đăng ký auto-bid của một user trong một phiên.
//     Trả về true nếu có dòng bị xóa.

    boolean deleteAutoBid(int auctionId, String username);

//     Lấy tất cả auto-bid còn hoạt động của một phiên đấu giá.
    List<AutoBidEntry> getAutoBidsByAuction(int auctionId);

    /** Kiểm tra user đã đăng ký auto-bid cho phiên này chưa. */
    boolean existsAutoBid(int auctionId, String username);
}