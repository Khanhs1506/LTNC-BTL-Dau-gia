package com.auction.server.model;

public class ArtItem extends Item {
<<<<<<< HEAD
=======
    public String typeItem = "Art Item";
>>>>>>> 817410f54e5bfcefbf958f5c1aab6ba102d2f415
    private String artist;

    public ArtItem(String id, String name, double startingPrice, String artist) {
        super(id, name, startingPrice); // Gọi constructor của class cha (Item)
        this.artist = artist;
    }

    @Override
    public void printInfo() {
        System.out.println("[Nghệ thuật] " + name + " | Họa sĩ: " + artist + " | Giá cao nhất: " + currentHighestBid);
    }
<<<<<<< HEAD
=======

    @Override
    public String getType_item() {
        return typeItem;
    }

    public String getArtist() {
        return artist;
    }
>>>>>>> 817410f54e5bfcefbf958f5c1aab6ba102d2f415
}