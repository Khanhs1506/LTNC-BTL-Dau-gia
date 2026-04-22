package com.auction.server.model;

public class ElectronicsItem extends Item {
<<<<<<< HEAD
=======
    public String typeItem = "Electronics Item";
>>>>>>> 817410f54e5bfcefbf958f5c1aab6ba102d2f415
    private int warrantyMonths;

    public ElectronicsItem(String id, String name, double startingPrice, int warrantyMonths) {
        super(id, name, startingPrice); // Gọi constructor của class cha (Item)
        this.warrantyMonths = warrantyMonths;
    }

    @Override
    public void printInfo() {
        System.out.println("[Điện tử] " + name + " | Bảo hành: " + warrantyMonths + " tháng | Giá cao nhất: " + currentHighestBid);
    }
<<<<<<< HEAD
=======

    @Override
    public String getType_item() {
        return typeItem;
    }

    public int getWarrantyMonths() {
        return warrantyMonths;
    }
>>>>>>> 817410f54e5bfcefbf958f5c1aab6ba102d2f415
}

