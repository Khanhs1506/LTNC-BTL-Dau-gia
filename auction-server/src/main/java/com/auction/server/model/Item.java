package com.auction.server.model;

<<<<<<< HEAD
abstract public class Item implements Entity{
=======
abstract public class Item implements Entity<String>{
>>>>>>> 817410f54e5bfcefbf958f5c1aab6ba102d2f415
    protected String id;
    protected String name;
    protected double startingPrice;
    protected double currentHighestBid;
<<<<<<< HEAD

    //Constructor cho Item
=======
    protected String typeItem;

    public abstract String getType_item();

>>>>>>> 817410f54e5bfcefbf958f5c1aab6ba102d2f415
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
<<<<<<< HEAD

    //Getters & Setters cho các thuộc tính
=======
    public String getName() {
        return name;
    }
    public double getStartingPrice() {
        return startingPrice;
    }

    public String getTypeItem() { return typeItem; };

>>>>>>> 817410f54e5bfcefbf958f5c1aab6ba102d2f415
    public double getCurrentHighestBid() { return currentHighestBid; }
    public void setCurrentHighestBid(double currentHighestBid) { this.currentHighestBid = currentHighestBid; }

    //Abstract method để lớp con print ra info
    public abstract void printInfo();
}
