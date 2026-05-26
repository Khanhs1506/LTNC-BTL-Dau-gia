package com.auction.server.repository;

import com.auction.server.model.User;
import java.util.List;

public interface IUserDAO {
    User getUserByUsername(String username);
    boolean registerUser(User newUser);
    List<User> getAllUsers();
    boolean setUserStatus(String username, String status);
}