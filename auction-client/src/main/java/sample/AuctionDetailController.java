package sample;

import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class AuctionDetailController {

    @FXML private Label    itemNameLabel;
    @FXML private Label    startingPriceLabel;
    @FXML private Label    currentBidLabel;
    @FXML private Label    endTimeLabel;
    @FXML private Label    countdownLabel;
    @FXML private Label    messageLabel;
    @FXML private TextField bidAmountField;
    @FXML private LineChart<String, Number> bidChart;
    @FXML private HBox     headerBar;

    private AuctionItemDTO auction;
    private Timeline       countdown;

    // ── Drag support ──────────────────────────────────────
    private double dragOffsetX, dragOffsetY;

    @FXML
    private void onHeaderPressed(MouseEvent e) {
        dragOffsetX = e.getScreenX() - getStage().getX();
        dragOffsetY = e.getScreenY() - getStage().getY();
    }

    @FXML
    private void onHeaderDragged(MouseEvent e) {
        getStage().setX(e.getScreenX() - dragOffsetX);
        getStage().setY(e.getScreenY() - dragOffsetY);
    }

    // ── Nút điều khiển ────────────────────────────────────
    @FXML
    private void handleClose() {
        stopCountdown();
        getStage().close();
    }

    @FXML
    private void handleMinimize() {
        getStage().setIconified(true);
    }

    private Stage getStage() {
        return (Stage) headerBar.getScene().getWindow();
    }

    // ── Set dữ liệu ───────────────────────────────────────
    public void setAuction(AuctionItemDTO dto) {
        this.auction = dto;
        loadAuctionDetail();
        startCountdown();
        loadBidChart();
    }

    private void loadAuctionDetail() {
        itemNameLabel.setText(auction.title);
        startingPriceLabel.setText(formatVND(auction.startingPrice));
        currentBidLabel.setText(formatVND(auction.currentHighest));
        endTimeLabel.setText(String.valueOf(auction.endTime));
    }

    private void startCountdown() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        LocalDateTime end;
        try {
            end = LocalDateTime.parse(String.valueOf(auction.endTime), fmt);
        } catch (Exception e) {
            countdownLabel.setText("⏳ --:--:--");
            return;
        }

        countdown = new Timeline(new KeyFrame(Duration.seconds(1), ev -> {
            long secs = ChronoUnit.SECONDS.between(LocalDateTime.now(), end);
            if (secs <= 0) {
                countdownLabel.setText("⏳ Đã kết thúc");
                countdown.stop();
            } else {
                long h = secs / 3600, m = (secs % 3600) / 60, s = secs % 60;
                countdownLabel.setText(String.format("⏳ %02d:%02d:%02d", h, m, s));
            }
        }));
        countdown.setCycleCount(Timeline.INDEFINITE);
        countdown.play();
    }

    public void stopCountdown() {
        if (countdown != null) countdown.stop();
    }

    private void loadBidChart() {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Giá thầu");
        // Dữ liệu mẫu — thay bằng ServerConnection.getBidHistory(auction.id)
        series.getData().add(new XYChart.Data<>("10:00", auction.startingPrice));
        series.getData().add(new XYChart.Data<>("10:15", auction.startingPrice * 1.05));
        series.getData().add(new XYChart.Data<>("10:30", auction.currentHighest * 0.9));
        series.getData().add(new XYChart.Data<>("10:45", auction.currentHighest));
        bidChart.getData().add(series);
    }

    @FXML
    private void placeBidOnAction() {
        try {
            double amount = Double.parseDouble(
                    bidAmountField.getText().replace(".", "").replace(",", "").trim());
            if (amount <= auction.currentHighest) {
                messageLabel.setText("⚠ Giá phải lớn hơn " + formatVND(auction.currentHighest));
                return;
            }

            // TODO: ServerConnection.getInstance().placeBid(auction.id, username, amount);
            auction.currentHighest = amount;
            currentBidLabel.setText(formatVND(amount));
            messageLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 13;");
            messageLabel.setText("✅ Đặt giá thành công!");
            bidAmountField.clear();

            String response = ServerConnection.getInstance().placeBid(auction.id, UserSession.getInstance().getUsername(), amount);
            if (response.equalsIgnoreCase("Bid success"))
                auction.currentHighest = amount;
                currentBidLabel.setText(formatVND(amount));
                messageLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 13;");
                messageLabel.setText("✅ Đặt giá thành công!");
                bidAmountField.clear();

        } catch (NumberFormatException e) {
            messageLabel.setText("⚠ Vui lòng nhập số hợp lệ");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String formatVND(double amount) {
        return String.format("%,.0f VNĐ", amount);
    }
}