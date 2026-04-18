package com.auction.server.model;

import java.util.ArrayList;
import java.util.List;

public class Bidder extends User {

    private static final long serialVersionUID = 4L;


    private double balance;


    private List<String> participatedItemIds;
    private List<String> favoriteItemIds;


    public Bidder(String id, String username, String password, double balance){
        super(id, username, password);
        this.balance = balance;
        this.participatedItemIds = new ArrayList<>();
        this.favoriteItemIds = new ArrayList<>();
    }

    // lấy và sửa số dư
    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }


    // danh sách id của người tham gia
    public void joinAuction(String itemId){
        if (!participatedItemIds.contains(itemId)){//nếu người này là người mới tham gia lần đầu thì thêm id
            participatedItemIds.add(itemId);
        }
    }
    // lấy danh sách
    public List<String> getParticipatedItemIds(){
        return participatedItemIds;
    }

    //thêm sản phẩm vào danh sách sản phẩm yêu thích
    public void addToFavorites(String itemId){
        if (!favoriteItemIds.contains(itemId)){// nếu chưa có thì thêm vaò
            favoriteItemIds.add(itemId);
        }
    }

    public void removeFromFavorites(String itemId){
        favoriteItemIds.remove(itemId); // xóa sản phẩm khỏi danh sách theo dõi
    }

    //lấy danh sách
    public List<String> getFavoriteItemIds() {
        return favoriteItemIds;
    }

    @Override
    public void displayRoleInfo() {
        System.out.println("[BIDDER] ID: " + getId()
                + " | Tên: " + getUsername()
                + " | Số dư ví: $" + balance
                + " | Đang theo dõi: " + favoriteItemIds.size() + " sản phẩm");
    }
}