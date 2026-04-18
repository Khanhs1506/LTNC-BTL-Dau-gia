package com.auction.server.model;

import java.util.ArrayList;
import java.util.List;


public class Seller extends User {

    // ID phiên bản để tương thích khi gửi qua Socket
    private static final long serialVersionUID = 5L;

    // Điểm uy tín của người bán
    private double rating;

    // Danh sách ID các sản phẩm  mà người  này đăng bán
    private List<String> myItemIds;

    public Seller(String id, String username, String password) {

        super(id, username, password);
        this.rating = 5.0; // Mặc định người bán mới có 5 sao
        this.myItemIds = new ArrayList<>();
    }

    // --- Getters & Setters ---

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

    /**
     * Ghi đè phương thức hiển thị từ lớp cha
     */
    @Override
    public void displayRoleInfo() {
        System.out.println("[SELLER] ID: " + getId()
                + " | Tên: " + getUsername()
                + " | Uy tín: " + rating + " sao"
                + " | Số sản phẩm đang bán: " + myItemIds.size());
    }
}
