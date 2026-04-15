package com.auction.server.model;

abstract public class Item implements Entity{
    protected String id;
    protected String name;
    protected double startingPrice;
    protected double currentHighestBid;

    //Constructor cho Item
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

    //Getters & Setters cho các thuộc tính
    public double getCurrentHighestBid() { return currentHighestBid; }
    public void setCurrentHighestBid(double currentHighestBid) { this.currentHighestBid = currentHighestBid; }

    //Abstract method để lớp con print ra info
    public abstract void printInfo();
}
