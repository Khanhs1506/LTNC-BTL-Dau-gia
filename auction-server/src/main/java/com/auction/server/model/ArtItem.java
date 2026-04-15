package com.auction.server.model;

public class ArtItem extends Item {
    private String artist;

    public ArtItem(String id, String name, double startingPrice, String artist) {
        super(id, name, startingPrice); // Gọi constructor của class cha (Item)
        this.artist = artist;
    }

    @Override
    public void printInfo() {
        System.out.println("[Nghệ thuật] " + name + " | Họa sĩ: " + artist + " | Giá cao nhất: " + currentHighestBid);
    }
}