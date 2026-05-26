package sample;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import sample.model.PlacedBidRequest;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.function.Consumer;

public class AuctionDetailController {

    // FXML bindings
    @FXML private HBox headerBar;
    @FXML private Label itemNameLabel;
    @FXML private Label productCodeLabel;
    @FXML private Label sellerLabel;
    @FXML private ImageView mainImageView;
    @FXML private Label imgPlaceholder;
    @FXML private VBox thumbList;
    @FXML private Label startingPriceLabel;
    @FXML private Label currentBidLabel;
    @FXML private Label endTimeLabel;
    @FXML private Label countdownLabel;
    @FXML private Label statusBadge;
    @FXML private LineChart<String, Number> bidChart;
    @FXML private CategoryAxis chartXAxis;
    @FXML private NumberAxis chartYAxis;
    @FXML private HBox avatarBox;
    @FXML private Label participantLabel;
    @FXML private HBox quickBidBox;
    @FXML private TextField bidAmountField;
    @FXML private Label bidHintLabel;
    @FXML private Label messageLabel;
    @FXML private HBox notifyRow;
    @FXML private StackPane notifyTogglePane;

    // State
    private AuctionItemDTO auction;
    private Timeline countdown;
    private boolean notifyOn = false;
    private Rectangle toggleTrack;
    private Circle toggleThumb;
    private double dragOffsetX;
    private double dragOffsetY;
    private XYChart.Series<String, Number> bidSeries;

    // Observer BID_UPDATE — giữ reference để có thể huỷ đăng ký khi đóng màn hình
    private Consumer<PlacedBidRequest> bidUpdateListener;

    // Observer TIME_EXTENDED (Anti-sniping) — giữ reference để có thể huỷ đăng ký
    private Consumer<NotificationManager.TimeExtendedEvent> timeExtendedListener;

    // Colors
    private static final String RED  = "#B91C1C";
    private static final String GRAY = "#D1D5DB";

    private static String getAvatarColor(int index) {
        switch (index % 6) {
            case 0: return "#2563EB";
            case 1: return "#16A34A";
            case 2: return "#9333EA";
            case 3: return "#D97706";
            case 4: return "#DB2777";
            default: return "#0891B2";
        }
    }

    // =========================================================
    // initialize - called after FXML loads
    // =========================================================
    @FXML
    public void initialize() {
        buildToggleSwitch();
        setupChart();
    }

    private void buildToggleSwitch() {
        toggleTrack = new Rectangle(34, 18);
        toggleTrack.setArcWidth(18);
        toggleTrack.setArcHeight(18);
        toggleTrack.setFill(Color.web(GRAY));

        toggleThumb = new Circle(7, Color.WHITE);
        toggleThumb.setTranslateX(-8);

        notifyTogglePane.getChildren().addAll(toggleTrack, toggleThumb);
        notifyTogglePane.setAlignment(Pos.CENTER);
    }

    private void setupChart() {
        chartYAxis.setTickLabelFormatter(new javafx.util.StringConverter<Number>() {
            @Override
            public String toString(Number n) {
                long v = n.longValue();
                if (v >= 1000000) return (v / 1000000) + "M";
                if (v >= 1000) return (v / 1000) + "K";
                return String.valueOf(v);
            }
            @Override
            public Number fromString(String s) { return 0; }
        });

        bidChart.sceneProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                Platform.runLater(() -> applyChartStyle());
            }
        });
    }

    private void applyChartStyle() {
        bidChart.lookupAll(".chart-series-line").forEach(n ->
                n.setStyle("-fx-stroke: " + RED + "; -fx-stroke-width: 2;"));
        bidChart.lookupAll(".chart-line-symbol").forEach(n ->
                n.setStyle("-fx-background-color: " + RED + ", white;"
                        + "-fx-background-radius: 5; -fx-padding: 4;"));
        bidChart.lookupAll(".chart-plot-background").forEach(n ->
                n.setStyle("-fx-background-color: #F9FAFB;"));
        bidChart.lookupAll(".chart-vertical-grid-lines").forEach(n ->
                n.setStyle("-fx-stroke: #E5E7EB;"));
        bidChart.lookupAll(".chart-horizontal-grid-lines").forEach(n ->
                n.setStyle("-fx-stroke: #E5E7EB;"));
    }

    // =========================================================
    // Public API
    // =========================================================
    public void setAuction(AuctionItemDTO dto) {
        this.auction = dto;
        loadInfo();
        loadImage();
        loadStats();
        buildQuickBidButtons();
        buildParticipants();
        startCountdown();
        loadBidChart();
        registerBidUpdateListener();
        registerTimeExtendedListener();
    }

    //ĐĂNG KÍ THÔNG BÁO
    private void registerBidUpdateListener() {
        // Huỷ listener cũ nếu có (tránh leak khi setAuction gọi lại)
        unregisterBidUpdateListener();

        bidUpdateListener = (req) -> {
            // Chỉ xử lý nếu là phiên đấu giá đang xem
            if (req.auctionId != auction.id) return;

            // Cập nhật dữ liệu local
            auction.currentHighest = req.amount;
            auction.totalBids = auction.totalBids + 1;

            // Cập nhật UI (đã chạy trên FX thread vì NotificationManager gọi Platform.runLater)
            currentBidLabel.setText(formatVND(req.amount));
            bidHintLabel.setText("Gia toi thieu phai cao hon gia hien tai: " + formatVND(req.amount));
            buildQuickBidButtons();
            buildParticipants();
            if (bidSeries != null) {
                String timeLabel = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                bidSeries.getData().add(new XYChart.Data<>(timeLabel, req.amount));
                if (bidSeries.getData().size() > 20)
                    bidSeries.getData().remove(0);  // tránh chart chật
                applyChartStyle();
            }
            if (notifyOn) {
                String msg = "🔔 " + req.bidder + " vừa đặt giá " + formatVND(req.amount) + " cho \"" + auction.title + "\"";
                try {
                    Window win = headerBar.getScene().getWindow();
                    ToastNotification.info(win, msg);
                } catch (Exception ignored) {}
            }
        };
        NotificationManager.getInstance().addBidUpdateListener(bidUpdateListener);
    }

    private void unregisterBidUpdateListener() {
        if (bidUpdateListener != null) {
            NotificationManager.getInstance().removeBidUpdateListener(bidUpdateListener);
            bidUpdateListener = null;
        }
    }

    /**
     * Đăng ký lắng nghe sự kiện Anti-sniping gia hạn thời gian.
     * Khi server gia hạn, đồng hồ đếm ngược sẽ tự động cập nhật.
     */
    private void registerTimeExtendedListener() {
        unregisterTimeExtendedListener();

        timeExtendedListener = (event) -> {
            if (event.auctionId != auction.id) return;

            // Cập nhật endTime local
            auction.endTime = event.newEndTime;

            // Khởi động lại đồng hồ đếm ngược với thời gian mới
            startCountdown();

            // Cập nhật nhãn thời gian kết thúc
            java.time.format.DateTimeFormatter fmt =
                    java.time.format.DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
            endTimeLabel.setText(event.newEndTime.format(fmt));

            // Hiển thị thông báo nổi bật cho người dùng
            messageLabel.setText("⏰ Phiên được gia hạn thêm " + event.extensionMinutes + " phút!");
            messageLabel.setStyle(
                    "-fx-text-fill: #D97706; -fx-font-weight: bold; -fx-font-size: 12;");

            // Hiển thị toast nếu cửa sổ đang mở
            if (notifyOn) {
                try {
                    javafx.stage.Window win = headerBar.getScene().getWindow();
                    ToastNotification.info(win,
                            "⏰ Anti-Sniping: Phiên \"" + auction.title + "\" được gia hạn thêm "
                                    + event.extensionMinutes + " phút!");
                } catch (Exception ignored) {}
            }

            System.out.println("[AuctionDetail] Anti-sniping: Phiên " + event.auctionId
                    + " gia hạn đến " + event.newEndTime);
        };

        NotificationManager.getInstance().addTimeExtendedListener(timeExtendedListener);
    }

    private void unregisterTimeExtendedListener() {
        if (timeExtendedListener != null) {
            NotificationManager.getInstance().removeTimeExtendedListener(timeExtendedListener);
            timeExtendedListener = null;
        }
    }


    // =========================================================
    // Load data methods
    // =========================================================
    private void loadInfo() {
        itemNameLabel.setText(auction.title != null ? auction.title : "---");

        if (auction.id > 0) {
            productCodeLabel.setText("Ma: DG-" + auction.id);
        }
        if (auction.sellerUsername != null) {
            sellerLabel.setText("Nha ban: " + auction.sellerUsername);
        }
    }

    private void loadImage() {
        thumbList.getChildren().clear();

        if (auction.imageUrl != null && !auction.imageUrl.isBlank()) {
            imgPlaceholder.setVisible(false);
            imgPlaceholder.setManaged(false);

            String[] urls = auction.imageUrl.split("\\|");
            for (int i = 0; i < urls.length; i++) {
                String url = urls[i].trim();
                if (url.isEmpty()) continue;

                final int index = i;
                Image img = new Image(url, true);

                if (i == 0) {
                    mainImageView.setImage(img);
                }

                ImageView iv = new ImageView(img);
                iv.setFitWidth(42);
                iv.setFitHeight(38);
                iv.setPreserveRatio(false);

                StackPane wrap = new StackPane(iv);
                wrap.setPrefSize(44, 40);

                String activeStyle = "-fx-border-color: " + RED + ";"
                        + "-fx-border-width: 1.5;"
                        + "-fx-border-radius: 5;"
                        + "-fx-background-radius: 5;"
                        + "-fx-cursor: hand;";
                String normalStyle = "-fx-border-color: #E5E7EB;"
                        + "-fx-border-width: 0.5;"
                        + "-fx-border-radius: 5;"
                        + "-fx-background-radius: 5;"
                        + "-fx-cursor: hand;"
                        + "-fx-opacity: 0.7;";

                wrap.setStyle(i == 0 ? activeStyle : normalStyle);

                wrap.setOnMouseClicked(e -> {
                    mainImageView.setImage(img);
                    thumbList.getChildren().forEach(n -> ((StackPane) n).setStyle(normalStyle));
                    wrap.setStyle(activeStyle);
                });
                wrap.setOnMouseEntered(e -> {
                    if (!wrap.getStyle().contains(RED)) {
                        wrap.setStyle(wrap.getStyle().replace("-fx-opacity: 0.7;", "-fx-opacity: 1.0;"));
                    }
                });
                wrap.setOnMouseExited(e -> {
                    if (!wrap.getStyle().contains(RED)) {
                        wrap.setStyle(wrap.getStyle().replace("-fx-opacity: 1.0;", "-fx-opacity: 0.7;"));
                    }
                });

                thumbList.getChildren().add(wrap);
            }
        } else {
            imgPlaceholder.setVisible(true);
            imgPlaceholder.setManaged(true);
            mainImageView.setImage(null);
        }
    }

    private void loadStats() {
        startingPriceLabel.setText(formatVND(auction.startingPrice));
        currentBidLabel.setText(formatVND(auction.currentHighest));

        if (auction.endTime != null) {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm, dd/MM/yyyy");
            endTimeLabel.setText(auction.endTime.format(fmt));
        } else {
            endTimeLabel.setText("---");
        }

        bidHintLabel.setText("Gia toi thieu phai cao hon gia hien tai: "
                + formatVND(auction.currentHighest));
    }

    private void buildQuickBidButtons() {
        quickBidBox.getChildren().clear();
        double step = auction.stepPrice > 0 ? auction.stepPrice : 500000;
        double[] offsets = { step, step * 2, step * 4 };

        for (double offset : offsets) {
            double target = auction.currentHighest + offset;
            Button btn = new Button("+" + formatVND(offset));
            btn.setStyle(
                    "-fx-background-color: transparent;"
                            + "-fx-text-fill: #B91C1C;"
                            + "-fx-font-size: 12;"
                            + "-fx-padding: 4 12;"
                            + "-fx-border-color: #B91C1C;"
                            + "-fx-border-width: 0.5;"
                            + "-fx-border-radius: 6;"
                            + "-fx-background-radius: 6;"
                            + "-fx-cursor: hand;"
            );
            btn.setOnAction(e ->
                    bidAmountField.setText(String.format("%,.0f", target).replace(",", "."))
            );
            quickBidBox.getChildren().add(btn);
        }
    }

    private void buildParticipants() {
        avatarBox.getChildren().clear();
        int total = Math.max(auction.totalBids, 0);

        String[] initials = { "TH", "ML", "NP", "KV" };
        int show = Math.min(4, Math.max(1, total));

        for (int i = 0; i < show; i++) {
            Label av = new Label(i < initials.length ? initials[i] : "?");
            av.setPrefSize(26, 26);
            av.setMinSize(26, 26);
            av.setMaxSize(26, 26);
            av.setAlignment(Pos.CENTER);
            String translate = i > 0 ? "-fx-translate-x: -" + (i * 7) + ";" : "";
            av.setStyle(
                    "-fx-background-color: " + getAvatarColor(i) + ";"
                            + "-fx-text-fill: white;"
                            + "-fx-font-size: 10;"
                            + "-fx-font-weight: bold;"
                            + "-fx-background-radius: 13;"
                            + "-fx-border-color: white;"
                            + "-fx-border-width: 1.5;"
                            + "-fx-border-radius: 13;"
                            + translate
            );
            avatarBox.getChildren().add(av);
        }

        if (total > 4) {
            Label more = new Label("+" + (total - 4));
            more.setPrefSize(26, 26);
            more.setMinSize(26, 26);
            more.setMaxSize(26, 26);
            more.setAlignment(Pos.CENTER);
            more.setStyle(
                    "-fx-background-color: #6B7280;"
                            + "-fx-text-fill: white;"
                            + "-fx-font-size: 9;"
                            + "-fx-font-weight: bold;"
                            + "-fx-background-radius: 13;"
                            + "-fx-border-color: white;"
                            + "-fx-border-width: 1.5;"
                            + "-fx-border-radius: 13;"
                            + "-fx-translate-x: -" + (4 * 7) + ";"
            );
            avatarBox.getChildren().add(more);
        }

        participantLabel.setText(
                total > 0 ? total + " nguoi dang tham gia dau gia" : "Chua co nguoi tham gia"
        );
    }

    private void startCountdown() {
        if (countdown != null) countdown.stop();
        if (auction.endTime == null) {
            countdownLabel.setText("--:--:--");
            return;
        }

        countdown = new Timeline(new KeyFrame(Duration.seconds(1), ev -> {
            // Dùng auction.endTime trực tiếp để tự động nhận cập nhật Anti-sniping
            long secs = ChronoUnit.SECONDS.between(LocalDateTime.now(), auction.endTime);
            if (secs <= 0) {
                countdownLabel.setText("Da ket thuc");
                statusBadge.setText("Da ket thuc");
                statusBadge.setStyle(
                        "-fx-background-color: #F3F4F6;"
                                + "-fx-text-fill: #6B7280;"
                                + "-fx-font-size: 11;"
                                + "-fx-padding: 3 10;"
                                + "-fx-background-radius: 20;"
                );
                countdown.stop();
            } else {
                long h = secs / 3600;
                long m = (secs % 3600) / 60;
                long s = secs % 60;
                countdownLabel.setText(String.format("%02d:%02d:%02d", h, m, s));
            }
        }));
        countdown.setCycleCount(Timeline.INDEFINITE);
        countdown.play();
    }

    public void stopCountdown() {
        if (countdown != null) countdown.stop();
    }

    private void loadBidChart() {
        bidChart.getData().clear();

        // Hiển thị điểm khởi đầu ngay
        bidSeries = new XYChart.Series<>();
        XYChart.Series<String, Number> series = bidSeries;
        series.setName("Gia thau");
        series.getData().add(new XYChart.Data<>("Bat dau", auction.startingPrice));
        bidChart.getData().add(series);
        int auctionId = auction.id;

        // Gọi server trong background thread để không block UI
        new Thread(() -> {
            try {
                String raw = ServerConnection.getInstance().getBidHistory(auctionId);
                // raw = "BID_HISTORY===[ {time, bidder, amount}, ... ]"
                String jsonPart = raw.contains("===") ? raw.split("===", 2)[1] : "[]";

                com.google.gson.JsonArray arr =
                        com.google.gson.JsonParser.parseString(jsonPart).getAsJsonArray();

                Platform.runLater(() -> {
                    series.getData().clear();
                    series.getData().add(new XYChart.Data<>("Bat dau", auction.startingPrice));

                    for (com.google.gson.JsonElement el : arr) {
                        com.google.gson.JsonObject obj = el.getAsJsonObject();
                        String time   = obj.get("time").getAsString();
                        double amount = obj.get("amount").getAsDouble();
                        series.getData().add(new XYChart.Data<>(time, amount));
                    }
                    applyChartStyle();
                });

            } catch (Exception e) {
                System.out.println("[BidChart] Loi tai du lieu: " + e.getMessage());
                Platform.runLater(() -> applyChartStyle());
            }
        }, "BidChartLoader").start();
    }

    // =========================================================
    // FXML Event Handlers
    // =========================================================
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

    @FXML
    private void handleClose() {
        stopCountdown();
        unregisterBidUpdateListener();
        unregisterTimeExtendedListener();
        getStage().close();
    }

    @FXML
    private void handleMinimize() {
        getStage().setIconified(true);
    }

    @FXML
    private void handleViewDetail() {
        String desc = auction.description != null ? auction.description : "Khong co mo ta.";
        showInfo("Chi tiet san pham", desc);
    }

    @FXML
    private void handleViewHistory() {
        showInfo("Lich su dau gia",
                "Tong so lan dat gia: " + auction.totalBids
                        + "\nGia cao nhat: " + formatVND(auction.currentHighest)
                        + (auction.currentWinner != null ? "\nNguoi dang dan: " + auction.currentWinner : "")
        );
    }

    @FXML
    private void handleViewTerms() {
        showInfo("Dieu khoan dau gia",
                "- Gia dat phai cao hon gia hien tai.\n"
                        + "- Moi lan dat gia la cam ket mua neu thang.\n"
                        + "- Thanh toan trong vong 48 gio sau khi ket thuc.\n"
                        + "- Hoan tien neu nguoi ban khong giao hang."
        );
    }

    @FXML
    private void handleShare() {
        showInfo("Chia se phien dau gia",
                "Link: https://auction.example.com/item/" + auction.id);
    }

    @FXML
    private void handleToggleNotify() {
        notifyOn = !notifyOn;
        toggleTrack.setFill(notifyOn ? Color.web(RED) : Color.web(GRAY));
        toggleThumb.setTranslateX(notifyOn ? 8 : -8);
        messageLabel.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 11;");
        messageLabel.setText(notifyOn ? "Da bat thong bao." : "Da tat thong bao.");
    }

    @FXML
    private void placeBidOnAction() {
        messageLabel.setStyle("-fx-text-fill: #e05252; -fx-font-size: 13;");

        String raw = bidAmountField.getText()
                .replace(".", "")
                .replace(",", "")
                .trim();

        if (raw.isEmpty()) {
            messageLabel.setText("Vui long nhap so tien.");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            messageLabel.setText("Vui long nhap so hop le.");
            return;
        }

        if (amount <= auction.currentHighest) {
            messageLabel.setText("Gia phai lon hon " + formatVND(auction.currentHighest));
            return;
        }

        final int   auctionId = auction.id;
        final double finalAmt  = amount;

        new Thread(() -> {
            try {
                String response = ServerConnection.getInstance().placeBid(auctionId, UserSession.getInstance().getUsername(), finalAmt);

                Platform.runLater(() -> {
                    if (response != null && response.equalsIgnoreCase("BID SUCCESS")) {
                        System.out.println("[BID] auctionId=" + auctionId + " amount=" + finalAmt);
                        // Chart / giá / nút sẽ được cập nhật bởi bidUpdateListener
                        // khi BID_UPDATE broadcast về — không cập nhật lại ở đây
                        // để tránh duplicate data point trên chart.
                        bidAmountField.clear();
                        messageLabel.setStyle("-fx-text-fill: #16A34A; -fx-font-size: 13;");
                        messageLabel.setText("Dat gia thanh cong!");
                    } else {
                        messageLabel.setText("Dat gia that bai: " + response);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() ->
                        messageLabel.setText("Loi ket noi. Vui long thu lai."));
                e.printStackTrace();
            }
        }, "PlaceBidThread").start();
    }

    // =========================================================
    // Utilities
    // =========================================================
    private Stage getStage() {
        return (Stage) headerBar.getScene().getWindow();
    }

    private String formatVND(double amount) {
        return String.format("%,.0f VND", amount).replace(",", ".");
    }

    private void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}