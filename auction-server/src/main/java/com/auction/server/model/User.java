<<<<<<< HEAD
package com.auction.server.model;

public class User implements Entity {
    private String id;
    private String username;
    private String password;
    private String role; //ADMIN hoặc SELLER hoặc BIDDER

    // Constructor cho User
=======

package com.auction.server.model;

import java.io.Serializable;


public abstract class User implements Entity<String>, Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String username;
    private String password;
    private String role;

>>>>>>> 817410f54e5bfcefbf958f5c1aab6ba102d2f415
    public User(String id, String username, String password, String role) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role;
    }
<<<<<<< HEAD

    @Override
    public String getId() {
        return this.id;
=======
    public User(String username, String password){
        this.username = username;
        this.password = password;
    }

    @Override
    public String getId() {
        return id;
>>>>>>> 817410f54e5bfcefbf958f5c1aab6ba102d2f415
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

<<<<<<< HEAD
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
=======
    public String getUsername(){
        return username;
    }


    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }


    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public abstract void displayRoleInfo();
>>>>>>> 817410f54e5bfcefbf958f5c1aab6ba102d2f415
}