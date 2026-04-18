package com.auction.server.model;


public class Admin extends User {

    private static final long serialVersionUID = 2L;//id để liên kết socket


    public Admin(String id, String username, String password){
        super(id, username, password);
    }


    @Override
    public void displayRoleInfo() {
        System.out.println("Vai trò: Quản trị viên hệ thống");
        System.out.println("Tên đăng nhập: " + getUsername());
    }
}