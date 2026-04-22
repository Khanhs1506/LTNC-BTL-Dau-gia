package com.auction.server.model;

abstract public class Item implements Entity<String>{
    protected String id;
    protected String name;
    protected double startingPrice;
    protected double currentHighestBid;
    protected String typeItem;

    public abstract String getType_item();

    public Item(String id, String name, double startingPrice) {
        this.id = id;
        this.name = name;
        this.startingPrice = startingPrice;
        this.currentHighestBid = startingPrice;
    }

    @Override
    public String getId() { return id; }
    @Override
    public void setId(String id) { this.id = id; }
    public String getName() {
        return name;
    }
    public double getStartingPrice() {
        return startingPrice;
    }

    public String getTypeItem() { return typeItem; };

    public double getCurrentHighestBid() { return currentHighestBid; }
    public void setCurrentHighestBid(double currentHighestBid) { this.currentHighestBid = currentHighestBid; }

    //Abstract method để lớp con print ra info
    public abstract void printInfo();
}
