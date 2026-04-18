
package com.auction.server.model;

public abstract class User implements Entity<String> {
    private String id;
    private String username;
    private String password;

    public User(String id, String username, String password) {
        this.id = id;
        this.username = username;
        this.password = password;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getUsername(){
        return username;
    }


    // Phương thức trừu tượng để các class con (Bidder, Admin) tự thực hiện logic riêng
    public abstract void displayRoleInfo();
}