package com.auction.server.repository;

import java.util.List;

public interface IFavoriteDAO {
    boolean addFavorite(String username, int auctionId);
    boolean removeFavorite(String username, int auctionId);
    boolean isFavorite(String username, int auctionId);
    List<Integer> getFavoriteAuctionIds(String username);
}