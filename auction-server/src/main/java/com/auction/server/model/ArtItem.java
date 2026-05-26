package com.auction.server.model;

public class ArtItem extends Item {
    private String artist;

    public ArtItem(String id, String name, double startingPrice, String artist) {
        super(id, name, startingPrice);
        this.typeItem = "Art Item"; // gán vào field của lớp cha, không khai báo lại
        this.artist = artist;
    }

    @Override
    public void printInfo() {
        System.out.println("[Nghệ thuật] " + name + " | Họa sĩ: " + artist + " | Giá cao nhất: " + currentHighestBid);
    }

    @Override
    public String getType_item() {
        return typeItem;
    }

    public String getArtist() {
        return artist;
    }
}