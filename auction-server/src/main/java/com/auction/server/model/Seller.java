package com.auction.server.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Seller extends User{
    private Map<String, Item> itemsOfId;

    public Seller(String id, String name, String password){
        super(id, name, password, "SELLER");
        itemsOfId = new HashMap<>();
    }

    //thêm sản phẩm
    public void addItem(Item item) {
        itemsOfId.put(item.getId(), item);
    }

    //Xóa sản phẩm
    public void removeItem(String id){
        if (itemsOfId.containsKey(id)){
            itemsOfId.remove(id);
            System.out.println("Đã xóa sản phẩm");
        } else {
            System.out.println("Không tìm thấy sản phẩm");
        }
    }


    //Lấy toàn bộ sảm phẩm
    public List<Item> getAllItems(){
        return new ArrayList<>(itemsOfId.values());
    }
}