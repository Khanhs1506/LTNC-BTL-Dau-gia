package com.auction.server.repository;

import com.auction.server.model.Item;
import java.util.List;

public interface IItemDAO {
    // Lấy sản phẩm theo id
    Item getItemById(int id);

    // Lấy tất cả sản phẩm
    List<Item> getAllItems();

    // Lấy danh sách sản phẩm của 1 seller
    List<Item> getItemsBySellerId(String sellerId);

    // Thêm sản phẩm mới — trả về id được DB sinh ra, -1 nếu thất bại
    int insertItem(Item item, String sellerId);

    // Xóa sản phẩm theo id
    boolean deleteItem(int id);

    // Cập nhật giá cao nhất hiện tại (gọi sau mỗi bid hợp lệ)
    boolean updateCurrentHighestBid(int itemId, double newBid);

    /**
     * Lấy danh sách item theo danh mục.
     *
     * @param category tên danh mục cần tìm
     * @return danh sách item thuộc danh mục
     */
    List<Item> getItemsByCategory(String category);
}