package com.auction.server.repository;

import com.auction.server.model.Item;
import java.util.List;

public interface IItemDAO {
    int DEFAULT_PAGE_SIZE = 100;

    // Lấy tất cả sản phẩm
    default List<Item> getAllItems() {
        return getAllItems(DEFAULT_PAGE_SIZE, 0);
    }
    List<Item> getAllItems(int limit, int offset);

    // Lấy danh sách sản phẩm của 1 seller
    List<Item> getItemsBySellerId(String sellerId);

    // Thêm sản phẩm mới — trả về id được DB sinh ra, -1 nếu thất bại
    int insertItem(Item item, String sellerId);

    // Xóa sản phẩm theo id
    boolean deleteItem(int id);

    // Cập nhật giá cao nhất hiện tại
    boolean updateCurrentHighestBid(int itemId, double newBid);

    // Lấy danh sách item theo danh mục
    List<Item> getItemsByCategory(String category);
    String getSellerUsernameByItemId(int itemId);
    Item getItemById(int itemId);
}