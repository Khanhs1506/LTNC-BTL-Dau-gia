package com.auction.server.model;

public class ElectronicsItem extends Item {
    private int warrantyMonths;

    public ElectronicsItem(String id, String name, double startingPrice, int warrantyMonths) {
        super(id, name, startingPrice); // Gọi constructor của class cha (Item)
        this.warrantyMonths = warrantyMonths;
    }

    @Override
    public void printInfo() {
        System.out.println("[Điện tử] " + name + " | Bảo hành: " + warrantyMonths + " tháng | Giá cao nhất: " + currentHighestBid);
    }
}

