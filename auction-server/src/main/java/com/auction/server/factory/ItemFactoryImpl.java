package com.auction.server.factory;

import com.auction.server.model.ArtItem;
import com.auction.server.model.ElectronicsItem;
import sample.model.Item;
import com.auction.server.model.VehicleItem;

public class ItemFactoryImpl implements ItemFactory {

    @Override
    public Item createItem(String type, String id, String name, double startingPrice, String... extraDetails) {
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("Loại sản phẩm không được để trống!");
            //Throw để catch ở phần Main
        }

        switch (type.toUpperCase()) {
            case "ELECTRONICS":
                // Đồ điện tử cần 1 thông số: Bảo hành
                if (extraDetails.length < 1) {
                    throw new IllegalArgumentException("Lỗi: Đồ điện tử cần nhập số tháng bảo hành!");
                }
                int warranty = Integer.parseInt(extraDetails[0].trim());
                return new ElectronicsItem(id, name, startingPrice, warranty);

            case "ART":
                // Nghệ thuật cần 1 thông số: Tên họa sĩ
                if (extraDetails.length < 1) {
                    throw new IllegalArgumentException("Lỗi: Đồ nghệ thuật cần nhập tên họa sĩ!");
                }
                String artist = extraDetails[0].trim();
                return new ArtItem(id, name, startingPrice, artist);

            case "VEHICLE":
                // Phương tiện cần 2 thông số: Hãng xe và Năm sản xuất
                if (extraDetails.length < 2) {
                    throw new IllegalArgumentException("Lỗi: Phương tiện cần nhập đủ Hãng xe và Năm sản xuất!");
                    //Throw để catch ở phần Main
                }
                String brand = extraDetails[0].trim();
                int year = Integer.parseInt(extraDetails[1].trim());
                return new VehicleItem(id, name, startingPrice, brand, year);

            default:
                throw new IllegalArgumentException("Xưởng chưa hỗ trợ đúc loại sản phẩm: " + type);
                //Throw để catch ở phần Main
        }
    }
}