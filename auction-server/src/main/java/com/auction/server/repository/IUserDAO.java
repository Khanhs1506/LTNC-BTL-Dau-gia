package com.auction.server.repository;

import com.auction.server.model.User;

public interface IUserDAO {
    User getUserByUsername(String username);
    boolean registerUser(User newUser);
}