package com.auction.server.network;

//LỚP MẪU ĐỂ TẠO VÀ CHUYỂN CHUỖI JSON
public class AuctionSummary {
    public int auctionId;
    public String itemId;
    public String itemName;
    public String itemType;
    public double startingPrice;
    public double currentHighestBid;
    public String currentWinnerUsername;
    public String sellerUsername;
    public String startTime;
    public String endTime;
    public String status;
    public int bidCount;
}