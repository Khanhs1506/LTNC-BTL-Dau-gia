package com.auction.server.factory;

import com.auction.server.model.Item;

public interface ItemFactory {
    // Hàm tạo sản phẩm. Tham số extraDetail dùng để linh hoạt truyền:
    // - Số tháng bảo hành (cho ElectronicsItem)
    // - Tên họa sĩ (cho Đồ ArtItems)
    // - Hãng xe và Năm sản xuất (cho Vehicle)

    // String... có nghĩa là có thể truyền vô số chuỗi vào cuối cùng
    // (Linh hoạt nhận vào 1 hoặc nhiều hơn các thông số mới)
    Item createItem(String type, String id, String name, double startingPrice, String... extraDetail);
}