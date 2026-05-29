package com.auction.server.model;

abstract public class Item {

    protected String id;
    protected String name;
    protected double startingPrice;
    protected double currentHighestBid;
    protected String typeItem;
    protected String imageUrl;

    public abstract String getType_item();

    public Item(String id, String name, double startingPrice) {
        this.id = id;
        this.name = name;
        this.startingPrice = startingPrice;
        this.currentHighestBid = startingPrice;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public double getStartingPrice() { return startingPrice; }
    public String getTypeItem() { return typeItem; }
    public double getCurrentHighestBid() { return currentHighestBid; }
    public void setCurrentHighestBid(double currentHighestBid) {
        this.currentHighestBid = currentHighestBid;
    }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public abstract void printInfo();
}