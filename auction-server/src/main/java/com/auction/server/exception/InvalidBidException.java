package com.auction.server.exception;

// xử lí lỗi đặt giá thấp hơn giá hiện tại
public class InvalidBidException extends Exception {

    public InvalidBidException(String message) {
        super(message);
    }

    public InvalidBidException(String message, Throwable cause) {
        super(message, cause);
    }
}
