package com.auction.server.exception;

public class DuplicateUsernameException extends Exception {
    private final String username;

    public DuplicateUsernameException(String username) {
        super("Tên đăng nhập '" + username + "' đã tồn tại!");
        this.username = username;
    }

    public DuplicateUsernameException(String message, Throwable cause) {
        super(message, cause);
        this.username = null;
    }

    public String getUsername() {
        return username;
    }
}