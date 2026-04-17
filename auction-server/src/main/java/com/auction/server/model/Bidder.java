package com.auction.server.model;

import java.util.ArrayList;
import java.util.List;

public class Bidder extends User {

    //Danh sách ID các sản phẩm Bidder đang đặt giá
    private List<String> participatedItemIds;
    //Danh sách ID các sản phẩm Bidder theo dõi
    private List<String> favoriteItemIds;


    public Bidder(String id, String name, String password){
        super(id, name, password, "BIDDER");
        participatedItemIds = new ArrayList<>();
        favoriteItemIds = new ArrayList<>();
    }


    //Đánh dấu tham gia đấu giá
    public void joinAuction(String itemId){
        if (!participatedItemIds.contains(itemId)){
            participatedItemIds.add(itemId);
        }
    }


    //Lấy danh sách Id sản phẩm đang đấu giá
    public List<String> getParticipatedItemIds(){
        return participatedItemIds;
    }


    //Thêm sản phảm vào danh sách theo dõi
    public void addToFavorites(String itemId){
        if (!favoriteItemIds.contains(itemId)){
            favoriteItemIds.add(itemId);
        }
    }


    //Xóa sản phẩm khỏi danh sách theo dõi
    public void removeFromFavorites(String itemId){
        if (favoriteItemIds.contains(itemId)){
            favoriteItemIds.remove(itemId);
        }
    }
}