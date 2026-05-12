package com.auction.server.model;

import sample.model.Item;

public class VehicleItem extends Item {
    public String typeItem = "Vehicle Item";
    private String brand; // Hãng xe
    private int year;     // Năm sản xuất

    // Constructor cho Vehicle
    public VehicleItem(String id, String name, double startingPrice, String brand, int year) {
        super(id, name, startingPrice); // Gọi constructor của class cha (Item)
        this.brand = brand;
        this.year = year;
    }

    @Override
    public void printInfo() {
        System.out.println("[Phương tiện] " + name + " | Hãng: " + brand + " | Năm SX: " + year + " | Giá cao nhất: " + currentHighestBid);
    }

    // Getters & Setters
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    @Override
    public String getType_item() {
        return typeItem;
    }
}