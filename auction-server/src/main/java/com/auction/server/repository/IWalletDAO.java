package com.auction.server.repository;

import com.auction.server.model.WalletTransaction;

import java.util.List;

public interface IWalletDAO {
    double getBalance(String userId);
    WalletTransaction deposit(String userId, double amount, String note);
    WalletTransaction payment(String userId, double amount, int auctionId, String note);
    WalletTransaction refund(String userId, double amount, Integer auctionId, String note);
    WalletTransaction bidHold(String userId, double amount, int auctionId, String note);
    WalletTransaction bidRelease(String userId, double amount, int auctionId, String note);
    List<WalletTransaction> getTransactionHistory(String userId, int limit);

}
