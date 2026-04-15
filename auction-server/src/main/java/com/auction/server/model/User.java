package com.auction.server.model;

public class User implements Entity {
    private String id;
    private String username;
    private String password;
    private String role; //ADMIN hoặc SELLER hoặc BIDDER

    // Constructor cho User
    public User(String id, String username, String password, String role) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    //Getters & Setters cho các thuộc tính
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    // Hàm in thông tin
    public void printUserInfo() {
        System.out.println("[User] ID: " + id + " | Tên: " + username + " | Vai trò: " + role);
    }
}