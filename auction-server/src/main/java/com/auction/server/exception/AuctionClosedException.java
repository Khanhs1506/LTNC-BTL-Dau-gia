package com.auction.server.exception;

// xử lí lỗi dặt giá sau kh phiên đã hết
public class AuctionClosedException extends Exception {

    public AuctionClosedException(String message) {
        super(message);
    }

    public AuctionClosedException(String message, Throwable cause) {
        super(message, cause);
    }
}