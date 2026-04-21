package com.auction.server.exception;

// xử lí lỗi trong phần đăng nhập khi không tìm thấy thông tin người dùng
public class UserNotFoundException extends Exception {

    public UserNotFoundException(String message) {
        super(message);
    }

    public UserNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
