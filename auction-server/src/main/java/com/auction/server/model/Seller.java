package com.auction.server.model;

import java.util.ArrayList;
import java.util.List;


public class Seller extends User {

    //gửi qua socket
    private static final long serialVersionUID = 5L;

    // Điểm uy tín
    private double rating;


    private List<String> myItemIds;

    public Seller(String id, String username, String password) {

        super(id, username, password,"SELLER");
        this.rating = 5.0; // Mặc định người bán mới có 5 sao
        this.myItemIds = new ArrayList<>();
    }


    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public List<String> getMyItemIds() {
        return myItemIds;
    }

//thêm
    public void addItem(String itemId) {
        if (!myItemIds.contains(itemId)) {
            myItemIds.add(itemId);
        }
    }

//xóa
    public void removeItem(String itemId) {
        myItemIds.remove(itemId);
    }

    @Override
    public void displayRoleInfo() {
        System.out.println("[SELLER] ID: " + getId()
                + " | Tên: " + getUsername()
                + " | Uy tín: " + rating + " sao"
                + " | Số sản phẩm đang bán: " + myItemIds.size());
    }
}
