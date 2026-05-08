package sample;

import javafx.fxml.FXML;

public class AuctionDetailController {

    private AuctionItemDTO auction;

    public void setAuction(AuctionItemDTO dto) {
        this.auction = dto;
        loadAuctionDetail();
        startCountdown();
        loadBidChart();
    }

    private void loadAuctionDetail() {
        System.out.println("📋 Load chi tiết: " + auction.title);
        // TODO: gán label, giá, trạng thái vào FXML
    }

    private void startCountdown() {
        System.out.println("⏱ Bắt đầu đếm ngược đến: " + auction.endTime);
        // TODO: Timeline JavaFX đếm ngược theo giây
    }

    public void stopCountdown() {
        System.out.println("⏹ Dừng đếm ngược");
        // TODO: timeline.stop()
    }

    private void loadBidChart() {
        System.out.println("📊 Load lịch sử giá cho: " + auction.title);
        // TODO: ServerConnection.getBidHistory(auction.id)
    }

    @FXML
    private void placeBid() {
        System.out.println("💰 Đặt giá cho: " + auction.title);
        // TODO: ServerConnection.placeBid(auction.id, amount)
    }
}