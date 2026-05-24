package com.auction.server.model;

import java.io.Serializable;

// Implement Entity<String> để đồng nhất cây kế thừa với User
// Implement Serializable để có thể gửi object qua socket
public abstract class Item implements Entity<String>, Serializable {

    private static final long serialVersionUID = 6L;

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

    public String getName() { return name; }
    public double getStartingPrice() { return startingPrice; }
    public String getTypeItem() { return typeItem; }
    public double getCurrentHighestBid() { return currentHighestBid; }
    public void setCurrentHighestBid(double currentHighestBid) {
        this.currentHighestBid = currentHighestBid;
    }

    public abstract void printInfo();
}