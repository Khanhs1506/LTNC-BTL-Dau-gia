
package sample;

import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Popup;
import javafx.stage.Stage;
import sample.model.Auction;
import sample.model.Item;
import sample.model.PlacedBidRequest;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import java.util.function.Consumer;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;

public class SellerDashboardController implements Initializable {

    // ── Topbar ────────────────────────────────────────────────────
    @FXML private Label lblUsername;
    @FXML private Label pageTitle;
    @FXML private Label avatarLabel;

    // ── Panels ────────────────────────────────────────────────────
    @FXML private VBox panelOverview;
    @FXML private VBox panelProducts;
    @FXML private VBox panelActive;
    @FXML private VBox panelUnpaid;
    @FXML private VBox panelPaid;
    @FXML private VBox panelExpired;
    @FXML private VBox panelRevenue;
    @FXML private VBox panelHistory;

    // ── Sidebar buttons ───────────────────────────────────────────
    @FXML private Button menuOverview, menuActive, menuPaid;
    @FXML private Button menuUnpaid, menuExpired, menuProducts;
    @FXML private Button menuRevenue, menuHistory;

    // ── Stat labels ───────────────────────────────────────────────
    @FXML private Label statTotal, statActive, statUnpaid, statRevenue;
    @FXML private Label revTotal, revMonth, revFee, revNet;
    @FXML private Label unpaidBadge;

    // ── Charts ────────────────────────────────────────────────────
    @FXML private LineChart<String, Number> revenueChart;
    @FXML private LineChart<String, Number> detailRevenueChart;
    @FXML private PieChart statusPieChart;

    // ── Tables ───────────────────────────────────────────────────
    @FXML private TableView<Auction> overviewTable;
    @FXML private TableView<Auction> productsTable;
    @FXML private TableView<Auction> activeTable;
    @FXML private TableView<Auction> unpaidTable;
    @FXML private TableView<Auction> paidTable;
    @FXML private TableView<Auction> expiredTable;
    @FXML private TableView<SellerTransactionRow> historyTable;
    @FXML private TableColumn<SellerTransactionRow, String> colHiTx;
    @FXML private TableColumn<SellerTransactionRow, String> colHiName;
    @FXML private TableColumn<SellerTransactionRow, String> colHiAmt;
    @FXML private TableColumn<SellerTransactionRow, String> colHiBuyer;
    @FXML private TableColumn<SellerTransactionRow, String> colHiDate;
    @FXML private TableColumn<SellerTransactionRow, String> colHiStatus;

    // Overview table columns
    @FXML private TableColumn<Auction,String> colOvId, colOvName, colOvBid,
            colOvBidder, colOvStatus;

    // Products table columns
    @FXML private TableColumn<Auction,String> colPId, colPName, colPPrice,
            colPCat, colPStatus, colPActions;

    // ── Active table columns (Đang hoạt động) ─────────────────────
    @FXML private TableColumn<Auction,String> colActId, colActName, colActBid,
            colActBidder, colActEnd, colActCount;

    // ── Unpaid table columns (Chưa thanh toán) ────────────────────
    @FXML private TableColumn<Auction,String> colUpId, colUpName, colUpPrice,
            colUpWinner, colUpTime, colUpAction;

    // ── Paid table columns (Đã thanh toán) ───────────────────────
    @FXML private TableColumn<Auction,String> colPdId, colPdName, colPdPrice,
            colPdWinner, colPdDate;

    // ── Expired table columns (Hết hạn) ──────────────────────────
    @FXML private TableColumn<Auction,String> colExId, colExName, colExPrice,
            colExTime, colExAction;

    // ── Search / filter ───────────────────────────────────────────
    @FXML private TextField txtSearch;
    @FXML private TextField searchProducts;
    @FXML private ComboBox<String> filterCategory;

    @FXML private StackPane bellStackSeller;
    private Label notifBadgeSeller;

    // ── Data ─────────────────────────────────────────────────────
    private ObservableList<Auction> allAuctions;
    private final ObservableList<SellerTransactionRow> historyRows = FXCollections.observableArrayList();
    private final List<Button> sidebarButtons = new java.util.ArrayList<>();

    /**
     * Lưu ID các phiên đã thanh toán (đánh dấu cục bộ qua nút "Đánh dấu đã TT").
     * Phân biệt "Chưa thanh toán" vs "Đã thanh toán" trong trạng thái FINISHED.
     */
    private final Set<Integer> paidAuctionIds = new HashSet<>();

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    // ✅ Listener nhận BID_UPDATE realtime từ NotificationManager
    private Consumer<PlacedBidRequest> bidUpdateListener;

    // ── Setup bảng (chỉ gọi 1 lần) ───────────────────────────────
    private boolean activeTableReady   = false;
    private boolean unpaidTableReady   = false;
    private boolean paidTableReady     = false;
    private boolean expiredTableReady  = false;
    private boolean historyTableReady  = false;

    private static class SellerTransactionRow {
        private final String txId;
        private final String itemName;
        private final String amount;
        private final String buyer;
        private final String date;
        private final String status;

        private SellerTransactionRow(String txId, String itemName, String amount,
                                     String buyer, String date, String status) {
            this.txId = txId;
            this.itemName = itemName;
            this.amount = amount;
            this.buyer = buyer;
            this.date = date;
            this.status = status;
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        String user = UserSession.getInstance().getUsername();
        lblUsername.setText(user != null ? user : "SELLER");
        avatarInitials();

        sidebarButtons.addAll(List.of(
                menuOverview, menuActive, menuPaid,
                menuUnpaid, menuExpired, menuProducts,
                menuRevenue, menuHistory));

        // Hiển thị dữ liệu từ server
        loadFromServer();
        setupOverviewTable();
        setupProductsTable();
        setupRevenueChart();
        setupPieChart();
        showPanel(panelOverview, menuOverview, "Dashboard");
        setupNotificationBadge();
        // Đăng ký nhận BID_UPDATE realtime
        registerBidUpdateListener();
    }

    // ── Setup 4 bảng Đấu giá ─────────────────────────────────────

    /**
     * Bảng "Đang hoạt động" — hiển thị các phiên RUNNING.
     * Cột: ID | Sản phẩm | Giá cao nhất | Người dẫn đầu | Kết thúc | Lượt đấu
     */
    private void setupActiveTable() {
        if (activeTableReady) return;
        activeTableReady = true;

        colActId.setCellValueFactory(c ->
                new SimpleStringProperty(String.valueOf(c.getValue().getId())));

        colActName.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getItem().getName()));

        colActBid.setCellValueFactory(c ->
                new SimpleStringProperty(formatMoney((long) c.getValue().getCurrentHighestBid())));
        colActBid.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); return; }
                setText(s);
                setStyle("-fx-text-fill: #B91C1C; -fx-font-weight: bold;");
            }
        });

        colActBidder.setCellValueFactory(c -> {
            String w = c.getValue().getCurrentWinnerUsername();
            return new SimpleStringProperty(w != null ? w : "—");
        });

        colActEnd.setCellValueFactory(c ->
                new SimpleStringProperty(
                        c.getValue().getEndTime() != null
                                ? c.getValue().getEndTime().format(TIME_FMT) : "—"));

        // Lượt đấu lấy từ bidHistory.size() (có thể = 0 nếu server không trả về)
        colActCount.setCellValueFactory(c ->
                new SimpleStringProperty(
                        String.valueOf(c.getValue().getBidCount())));

        activeTable.setPlaceholder(new Label("Không có phiên nào đang hoạt động."));
    }

    /**
     * Bảng "Chưa thanh toán" — FINISHED, có người thắng, chưa được đánh dấu paid.
     * Cột: ID | Sản phẩm | Giá chốt | Người thắng | Chốt lúc | Hành động (Đánh dấu đã TT)
     */
    private void setupUnpaidTable() {
        if (unpaidTableReady) return;
        unpaidTableReady = true;

        colUpId.setCellValueFactory(c ->
                new SimpleStringProperty(String.valueOf(c.getValue().getId())));

        colUpName.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getItem().getName()));

        colUpPrice.setCellValueFactory(c ->
                new SimpleStringProperty(formatMoney((long) c.getValue().getCurrentHighestBid())));
        colUpPrice.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); return; }
                setText(s);
                setStyle("-fx-text-fill: #B91C1C; -fx-font-weight: bold;");
            }
        });

        colUpWinner.setCellValueFactory(c -> {
            String w = c.getValue().getCurrentWinnerUsername();
            return new SimpleStringProperty(w != null ? w : "Không có");
        });

        colUpTime.setCellValueFactory(c ->
                new SimpleStringProperty(
                        c.getValue().getEndTime() != null
                                ? c.getValue().getEndTime().format(TIME_FMT) : "—"));

        // Nút "Đánh dấu đã TT" → chuyển sang paidAuctionIds
        colUpAction.setCellFactory(col -> new TableCell<>() {
            private final Button btnPay = new Button("Đã thanh toán");
            {
                btnPay.setStyle(
                        "-fx-background-color: #16A34A; -fx-text-fill: white;" +
                                "-fx-font-size: 11; -fx-cursor: hand; -fx-background-radius: 6;" +
                                "-fx-padding: 4 8;");
                btnPay.setOnAction(e -> {
                    Auction a = getTableRow().getItem();
                    if (a == null) return;

                    new Thread(() -> {
                        try {
                            String res = ServerConnection.getInstance().markAuctionPaid(a.getId());
                            Platform.runLater(() -> {
                                if (res != null && res.startsWith("MARK_AUCTION_PAID===OK")) {
                                    paidAuctionIds.add(a.getId());
                                    // Làm mới cả 2 bảng
                                    refreshUnpaidTable();
                                    refreshPaidTable();
                                    refreshHistoryTable();
                                    updateStatsFromData();
                                } else {
                                    showAlert(Alert.AlertType.ERROR, "Không thể xác nhận",
                                            "Server không lưu được trạng thái thanh toán: " + res);
                                }
                            });
                        } catch (Exception ex) {
                            Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi kết nối",
                                    "Không gửi được xác nhận thanh toán: " + ex.getMessage()));
                        }
                    }, "MarkAuctionPaidThread").start();
                });
            }
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                setGraphic(empty ? null : btnPay);
            }
        });

        unpaidTable.setPlaceholder(new Label("Không có phiên nào chờ thanh toán."));
    }

    /**
     * Bảng "Đã thanh toán" — FINISHED + có trong paidAuctionIds.
     * Cột: ID | Sản phẩm | Giá chốt | Người thắng | Ngày TT
     */
    private void setupPaidTable() {
        if (paidTableReady) return;
        paidTableReady = true;

        colPdId.setCellValueFactory(c ->
                new SimpleStringProperty(String.valueOf(c.getValue().getId())));

        colPdName.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getItem().getName()));

        colPdPrice.setCellValueFactory(c ->
                new SimpleStringProperty(formatMoney((long) c.getValue().getCurrentHighestBid())));
        colPdPrice.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); return; }
                setText(s);
                setStyle("-fx-text-fill: #16A34A; -fx-font-weight: bold;");
            }
        });

        colPdWinner.setCellValueFactory(c -> {
            String w = c.getValue().getCurrentWinnerUsername();
            return new SimpleStringProperty(w != null ? w : "—");
        });

        // Dùng endTime làm ngày thanh toán (gần đúng)
        colPdDate.setCellValueFactory(c ->
                new SimpleStringProperty(
                        c.getValue().getEndTime() != null
                                ? c.getValue().getEndTime().format(TIME_FMT) : "—"));

        paidTable.setPlaceholder(new Label("Chưa có phiên nào được xác nhận thanh toán."));
    }

    /**
     * Bảng "Hết hạn" — CANCELED hoặc FINISHED không có người thắng.
     * Cột: ID | Sản phẩm | Giá khởi điểm | Hết hạn lúc | Hành động (Đăng lại)
     */
    private void setupExpiredTable() {
        if (expiredTableReady) return;
        expiredTableReady = true;

        colExId.setCellValueFactory(c ->
                new SimpleStringProperty(String.valueOf(c.getValue().getId())));

        colExName.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getItem().getName()));

        colExPrice.setCellValueFactory(c ->
                new SimpleStringProperty(formatMoney((long) c.getValue().getItem().getStartingPrice())));

        colExTime.setCellValueFactory(c ->
                new SimpleStringProperty(
                        c.getValue().getEndTime() != null
                                ? c.getValue().getEndTime().format(TIME_FMT) : "—"));

        // Nút "Đăng lại" — placeholder, mở SellerCreateAuction
        colExAction.setCellFactory(col -> new TableCell<>() {
            private final Button btnRepost = new Button("Đăng lại");
            {
                btnRepost.setStyle(
                        "-fx-background-color: #2563EB; -fx-text-fill: white;" +
                                "-fx-font-size: 11; -fx-cursor: hand; -fx-background-radius: 6;" +
                                "-fx-padding: 4 8;");
                btnRepost.setOnAction(e -> {
                    try {
                        handleAddProduct();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                });
            }
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                setGraphic(empty ? null : btnRepost);
            }
        });

        expiredTable.setPlaceholder(new Label("Không có phiên nào hết hạn."));
    }

    private void setupHistoryTable() {
        if (historyTableReady) return;
        historyTableReady = true;

        colHiTx.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().txId));
        colHiName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().itemName));
        colHiAmt.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().amount));
        colHiBuyer.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().buyer));
        colHiDate.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().date));
        colHiStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().status));
        colHiStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(s);
                if ("Đã thanh toán".equals(s)) {
                    setStyle("-fx-text-fill: #16A34A; -fx-font-weight: bold;");
                } else {
                    setStyle("-fx-text-fill: #B45309; -fx-font-weight: bold;");
                }
            }
        });

        historyTable.setItems(historyRows);
        historyTable.setPlaceholder(new Label("Chưa có giao dịch nào."));
    }

    private void refreshHistoryTable() {
        if (allAuctions == null) return;
        List<SellerTransactionRow> rows = new ArrayList<>();
        for (Auction a : allAuctions) {
            if (a.getStatus() != Auction.Status.FINISHED || a.getCurrentWinnerUsername() == null) {
                continue;
            }

            String txId = "TX-" + a.getId();
            String itemName = a.getItem().getName();
            String amount = formatMoney((long) a.getCurrentHighestBid());
            String buyer = a.getCurrentWinnerUsername();
            String date = a.getEndTime() != null ? a.getEndTime().format(TIME_FMT) : "—";
            String status = paidAuctionIds.contains(a.getId()) ? "Đã thanh toán" : "Chưa thanh toán";
            rows.add(new SellerTransactionRow(txId, itemName, amount, buyer, date, status));
        }
        historyRows.setAll(rows);
        historyTable.refresh();
    }

    private void refreshRevenueCharts() {
        if (allAuctions == null) return;

        Map<YearMonth, Long> monthlyRevenue = new LinkedHashMap<>();
        YearMonth currentMonth = YearMonth.now();
        for (int i = 5; i >= 0; i--) {
            monthlyRevenue.put(currentMonth.minusMonths(i), 0L);
        }

        for (Auction a : allAuctions) {
            if (a.getCurrentWinnerUsername() == null) continue;
            if (a.getEndTime() == null) continue;
            YearMonth ym = YearMonth.from(a.getEndTime());
            if (!monthlyRevenue.containsKey(ym)) continue;

            long amount = (long) a.getCurrentHighestBid();
            // Ưu tiên doanh thu đã thanh toán, fallback theo phiên đã kết thúc có người thắng.
            if (paidAuctionIds.contains(a.getId()) || a.getStatus() == Auction.Status.FINISHED) {
                monthlyRevenue.put(ym, monthlyRevenue.get(ym) + amount);
            }
        }

        XYChart.Series<String, Number> revenueSeries = new XYChart.Series<>();
        revenueSeries.setName("Doanh thu (VNĐ)");
        XYChart.Series<String, Number> detailSeries = new XYChart.Series<>();
        detailSeries.setName("Doanh thu (VNĐ)");

        for (Map.Entry<YearMonth, Long> entry : monthlyRevenue.entrySet()) {
            String label = "Th." + entry.getKey().getMonthValue();
            Number value = entry.getValue();
            revenueSeries.getData().add(new XYChart.Data<>(label, value));
            detailSeries.getData().add(new XYChart.Data<>(label, value));
        }

        revenueChart.getData().setAll(revenueSeries);
        detailRevenueChart.getData().setAll(detailSeries);
        revenueChart.setLegendVisible(false);
        detailRevenueChart.setLegendVisible(false);
    }

    // ── Refresh helpers ───────────────────────────────────────────

    private void refreshActiveTable() {
        if (allAuctions == null) return;
        activeTable.setItems(allAuctions.filtered(
                a -> a.getStatus() == Auction.Status.RUNNING));
        activeTable.refresh();
    }

    private void refreshUnpaidTable() {
        if (allAuctions == null) return;
        // Chưa thanh toán = FINISHED + có người thắng + chưa trong paidAuctionIds
        unpaidTable.setItems(allAuctions.filtered(a ->
                a.getStatus() == Auction.Status.FINISHED
                        && a.getCurrentWinnerUsername() != null
                        && !paidAuctionIds.contains(a.getId())));
        unpaidTable.refresh();
    }

    private void refreshPaidTable() {
        if (allAuctions == null) return;
        // Đã thanh toán = FINISHED + đã được đánh dấu trong paidAuctionIds
        paidTable.setItems(allAuctions.filtered(a ->
                a.getStatus() == Auction.Status.FINISHED
                        && paidAuctionIds.contains(a.getId())));
        paidTable.refresh();
    }

    private void refreshExpiredTable() {
        if (allAuctions == null) return;
        // Hết hạn = CANCELED hoặc FINISHED không có người thắng
        expiredTable.setItems(allAuctions.filtered(a ->
                a.getStatus() == Auction.Status.CANCELED
                        || (a.getStatus() == Auction.Status.FINISHED
                        && a.getCurrentWinnerUsername() == null)));
        expiredTable.refresh();
    }

    // ── Notification badge ────────────────────────────────────────

    private void setupNotificationBadge() {
        notifBadgeSeller = creatBadgeLabel();
        if (bellStackSeller != null) bellStackSeller.getChildren().add(notifBadgeSeller);
        notifBadgeSeller.setVisible(false);
        NotificationManager.getInstance().addNotificationListener(
                () -> javafx.application.Platform.runLater(this::refreshBadge));
    }

    private void registerBidUpdateListener() {
        bidUpdateListener = (req) -> {
            if (allAuctions == null) return;

            boolean changed = false;
            for (Auction a : allAuctions) {
                if (a.getId() == req.auctionId && req.amount > a.getCurrentHighestBid()) {
                    a.getItem().setCurrentHighestBid(req.amount);
                    a.updateHighestBid(req.amount, req.bidder);
                    a.setBidCount(a.getBidCount() + 1);
                    changed = true;
                    break;
                }
            }

            if (changed) {
                overviewTable.refresh();
                productsTable.refresh();
                if (activeTableReady)  activeTable.refresh();
                updateStatsFromData();
            }
        };
        NotificationManager.getInstance().addBidUpdateListener(bidUpdateListener);
    }

    private Label creatBadgeLabel() {
        Label badge = new Label();
        badge.setStyle(
                "-fx-background-color: #e74c3c;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 9;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 10;" +
                        "-fx-padding: 1 4;" +
                        "-fx-min-width: 16;" +
                        "-fx-alignment: center;"
        );
        StackPane.setAlignment(badge, Pos.TOP_RIGHT);
        StackPane.setMargin(badge, new Insets(-4, -4, 0, 0));
        return badge;
    }

    private void refreshBadge() {
        int count = NotificationManager.getInstance().getUnreadCount();
        String text = count > 99 ? "99+" : String.valueOf(count);
        boolean show = count > 0;
        notifBadgeSeller.setText(text);
        notifBadgeSeller.setVisible(show);
    }

    @FXML
    private void handleBellClick() {
        NotificationManager.getInstance().markAllRead();
        refreshBadge();
        List<NotificationManager.Notification> list = NotificationManager.getInstance().getAll();
        Popup popup = new Popup();
        popup.setAutoHide(true);
        VBox box = new VBox(0);
        box.setPrefWidth(320);
        box.setMaxHeight(400);
        box.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: #ddd;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 10;" +
                        "-fx-background-radius: 10;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 16, 0, 0, 6);"
        );

        Label header = new Label("🔔  Thông báo đấu giá");
        header.setStyle(
                "-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #222;" +
                        "-fx-padding: 14 16 12 16;" +
                        "-fx-border-color: #eee; -fx-border-width: 0 0 1 0;"
        );
        header.setMaxWidth(Double.MAX_VALUE);
        box.getChildren().add(header);
        if (list.isEmpty()) {
            Label empty = new Label("Chưa có thông báo nào.");
            empty.setStyle("-fx-text-fill: #999; -fx-font-size: 13; -fx-padding: 20 16;");
            box.getChildren().add(empty);
        } else {
            ScrollPane scroll = new ScrollPane();
            scroll.setFitToWidth(true);
            scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            scroll.setPrefViewportHeight(Math.min(list.size() * 68.0, 340));
            scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
            VBox items = new VBox(0);
            List<NotificationManager.Notification> reversed = new ArrayList<>(list);
            java.util.Collections.reverse(reversed);
            java.time.format.DateTimeFormatter fmt =
                    java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss dd/MM");
            for (int i = 0; i < reversed.size(); i++) {
                NotificationManager.Notification notif = reversed.get(i);
                VBox row = new VBox(3);
                row.setPadding(new Insets(10, 16, 10, 16));
                row.setStyle(i % 2 == 0
                        ? "-fx-background-color: #ffffff;"
                        : "-fx-background-color: #fafafa;");
                Label msgLabel = new Label(notif.message);
                msgLabel.setWrapText(true);
                msgLabel.setStyle("-fx-font-size: 13; -fx-text-fill: #222;");
                java.time.LocalDateTime ldt = java.time.Instant
                        .ofEpochMilli(notif.timestamp)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDateTime();
                Label timeLabel = new Label(fmt.format(ldt));
                timeLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #999;");
                row.getChildren().addAll(msgLabel, timeLabel);
                items.getChildren().add(row);
            }
            scroll.setContent(items);
            box.getChildren().add(scroll);
        }
        popup.getContent().add(box);

        javafx.geometry.Bounds bounds = bellStackSeller.localToScreen(bellStackSeller.getBoundsInLocal());
        popup.show(bellStackSeller.getScene().getWindow(),
                bounds.getMaxX() - 320,
                bounds.getMaxY() + 4);
    }

    // ── Load data from server ─────────────────────────────────────

    private void loadFromServer() {
        new Thread(() -> {
            try {
                String response = ServerConnection.getInstance().getAuctionsBySeller();
                String paidResponse = ServerConnection.getInstance().getSellerPaidAuctions();

                if (response != null && response.startsWith("AUCTIONS===")) {
                    String json = response.substring("AUCTIONS===".length());
                    JsonArray arr = JsonParser.parseString(json).getAsJsonArray();

                    ObservableList<Auction> loaded = FXCollections.observableArrayList();
                    Set<Integer> loadedPaidAuctionIds = new HashSet<>();
                    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

                    for (JsonElement el : arr) {
                        JsonObject obj = el.getAsJsonObject();

                        int    auctionId  = obj.get("auctionId").getAsInt();
                        String itemName   = obj.get("itemName").getAsString();
                        String itemType   = obj.get("itemType").getAsString();
                        double startPrice = obj.get("startingPrice").getAsDouble();
                        double highBid    = obj.get("currentHighestBid").getAsDouble();
                        String winner     = (obj.has("currentWinnerUsername")
                                && !obj.get("currentWinnerUsername").isJsonNull())
                                ? obj.get("currentWinnerUsername").getAsString() : null;
                        String statusStr  = obj.get("status").getAsString();
                        int bidCount = obj.has("bidCount") ? obj.get("bidCount").getAsInt() : 0;
                        LocalDateTime startTime =
                                LocalDateTime.parse(obj.get("startTime").getAsString(), fmt);
                        LocalDateTime endTime   =
                                LocalDateTime.parse(obj.get("endTime").getAsString(), fmt);

                        final String finalItemType = itemType;
                        Item item = new Item(String.valueOf(auctionId), itemName, startPrice) {
                            @Override public String getType_item() { return finalItemType; }
                            @Override public void printInfo() { System.out.println(getName()); }
                        };
                        item.setCurrentHighestBid(highBid);

                        Auction a = new Auction(auctionId, item, startTime, endTime);
                        a.updateHighestBid(highBid, winner);
                        a.updateStatus(Auction.Status.valueOf(statusStr));
                        a.setBidCount(bidCount);
                        loaded.add(a);
                    }

                    if (paidResponse != null && paidResponse.startsWith("SELLER_PAID_AUCTIONS===")) {
                        try {
                            String paidJson = paidResponse.substring("SELLER_PAID_AUCTIONS===".length());
                            JsonArray paidArr = JsonParser.parseString(paidJson).getAsJsonArray();
                            for (JsonElement paidEl : paidArr) {
                                loadedPaidAuctionIds.add(paidEl.getAsInt());
                            }
                        } catch (Exception ignored) {
                            System.err.println("[SellerDashboard] Không parse được SELLER_PAID_AUCTIONS.");
                        }
                    }

                    Platform.runLater(() -> {
                        allAuctions = loaded;
                        paidAuctionIds.clear();
                        paidAuctionIds.addAll(loadedPaidAuctionIds);
                        updateStatsFromData();
                        refreshRevenueCharts();
                        setupPieChart();
                        overviewTable.setItems(allAuctions);
                        productsTable.setItems(allAuctions);
                        refreshHistoryTable();
                    });

                } else {
                    System.err.println("[SellerDashboard] Server trả về: " + response);
                    Platform.runLater(() -> loadMockData());
                }

            } catch (Exception e) {
                System.err.println("[SellerDashboard] Không kết nối được server: "
                        + e.getMessage());
                Platform.runLater(() -> loadMockData());
            }
        }, "SellerLoader").start();
    }

    private void updateStatsFromData() {
        if (allAuctions == null) return;
        statTotal.setText(String.valueOf(allAuctions.size()));
        statActive.setText(String.valueOf(count(Auction.Status.RUNNING)));

        // Chưa thanh toán = FINISHED + có winner + chưa paid
        long unpaidCount = allAuctions.stream()
                .filter(a -> a.getStatus() == Auction.Status.FINISHED
                        && a.getCurrentWinnerUsername() != null
                        && !paidAuctionIds.contains(a.getId()))
                .count();
        statUnpaid.setText(String.valueOf(unpaidCount));
        unpaidBadge.setText(unpaidCount + " phiên chờ xử lý");

        long revenue = allAuctions.stream()
                .filter(a -> paidAuctionIds.contains(a.getId()))
                .mapToLong(a -> (long) a.getCurrentHighestBid())
                .sum();
        statRevenue.setText(formatMoney(revenue));

        // Đồng bộ số liệu ở panel Doanh thu
        double fee = revenue * 0.05;
        double net = revenue - fee;
        revTotal.setText(formatMoney(revenue));
        revMonth.setText(formatMoney(revenue));
        revFee.setText(formatMoney((long) fee));
        revNet.setText(formatMoney((long) net));

        refreshRevenueCharts();
        setupPieChart();
    }

    // ── Mock data ────────────────────────────────────────────────
    private void loadMockData() {
        allAuctions = FXCollections.observableArrayList(
                mockAuction("A001", "Windows 11 Pro Key",   500_000, 1_200_000, "bidder_a",  Auction.Status.RUNNING),
                mockAuction("A002", "Adobe CC 2024",        800_000, 2_500_000, "user_xyz",  Auction.Status.RUNNING),
                mockAuction("A003", "Office 365 Business",  300_000,   750_000, "user_abc",  Auction.Status.FINISHED),
                mockAuction("A004", "AutoCAD 2025",       1_200_000, 3_100_000, "bidder_k",  Auction.Status.FINISHED),
                mockAuction("A005", "Minecraft Java Ed.",   200_000,         0, "",           Auction.Status.CANCELED),
                mockAuction("A006", "Photoshop 2024",       600_000, 1_800_000, "bidder_m",  Auction.Status.RUNNING)
        );
        updateStatsFromData();
        refreshRevenueCharts();
        setupPieChart();
        refreshHistoryTable();
    }

    private Auction mockAuction(String id, String name, long startPrice,
                                long currentBid, String winner,
                                Auction.Status status) {
        final String sid = id;
        final String sname = name;
        Item item = new Item(sid, sname, startPrice) {
            @Override public String getType_item() { return "SOFTWARE"; }
            @Override public void printInfo() { System.out.println(sid + " - " + sname); }
        };
        item.setCurrentHighestBid(currentBid);
        Auction a = new Auction(
                Integer.parseInt(id.replaceAll("[^0-9]", "")),
                item,
                LocalDateTime.now().minusDays(2),
                LocalDateTime.now().plusHours(2)
        );
        a.updateHighestBid(currentBid, winner.isEmpty() ? null : winner);
        a.updateStatus(status);
        a.setBidCount(currentBid > 0 ? 1 : 0);
        return a;
    }

    private long count(Auction.Status status) {
        if (allAuctions == null) return 0;
        return allAuctions.stream()
                .filter(a -> a.getStatus() == status)
                .count();
    }

    // ── Setup tables ─────────────────────────────────────────────
    private void setupOverviewTable() {
        colOvId.setCellValueFactory(c ->
                new SimpleStringProperty(String.valueOf(c.getValue().getId())));
        colOvName.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getItem().getName()));
        colOvBid.setCellValueFactory(c ->
                new SimpleStringProperty(formatMoney((long) c.getValue().getCurrentHighestBid())));
        colOvBidder.setCellValueFactory(c -> {
            String winner = c.getValue().getCurrentWinnerUsername();
            return new SimpleStringProperty(winner != null ? winner : "—");
        });
        colOvStatus.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getStatus().name()));
        colOvStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setGraphic(null); return; }
                Label pill = new Label(statusLabel(s));
                pill.getStyleClass().add(pillStyle(s));
                setGraphic(pill); setText(null);
            }
        });
        overviewTable.setItems(allAuctions);
    }

    private void setupProductsTable() {
        colPId.setCellValueFactory(c ->
                new SimpleStringProperty(String.valueOf(c.getValue().getId())));
        colPName.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getItem().getName()));
        colPPrice.setCellValueFactory(c ->
                new SimpleStringProperty(formatMoney((long) c.getValue().getItem().getStartingPrice())));
        colPCat.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getItem().getTypeItem()));
        colPStatus.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getStatus().name()));
        colPStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setGraphic(null); return; }
                Label pill = new Label(statusLabel(s));
                pill.getStyleClass().add(pillStyle(s));
                setGraphic(pill); setText(null);
            }
        });
        colPActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit   = new Button("Sửa");
            private final Button btnDelete = new Button("Xóa");
            {
                btnEdit.getStyleClass().add("btn-secondary");
                btnDelete.getStyleClass().add("btn-danger");
                btnEdit.setOnAction(e -> handleEditProduct(getTableRow().getItem()));
                btnDelete.setOnAction(e -> handleDeleteProduct(getTableRow().getItem()));
            }
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty) { setGraphic(null); return; }
                javafx.scene.layout.HBox box =
                        new javafx.scene.layout.HBox(6, btnEdit, btnDelete);
                setGraphic(box);
            }
        });
        productsTable.setItems(allAuctions);
        searchProducts.textProperty().addListener((obs, old, nw) -> {
            String kw = nw.trim().toLowerCase();
            productsTable.setItems(kw.isEmpty() ? allAuctions :
                    allAuctions.filtered(a ->
                            a.getItem().getName().toLowerCase().contains(kw)));
        });
    }

    // ── Charts ───────────────────────────────────────────────────
    private void setupRevenueChart() {
        refreshRevenueCharts();
    }

    private void setupPieChart() {
        if (allAuctions == null) return;
        long runningCount = count(Auction.Status.RUNNING);
        long unpaidCount = allAuctions.stream()
                .filter(a -> a.getStatus() == Auction.Status.FINISHED
                        && a.getCurrentWinnerUsername() != null
                        && !paidAuctionIds.contains(a.getId()))
                .count();
        long paidCount = allAuctions.stream()
                .filter(a -> a.getStatus() == Auction.Status.FINISHED
                        && paidAuctionIds.contains(a.getId()))
                .count();
        long expiredCount = allAuctions.stream()
                .filter(a -> a.getStatus() == Auction.Status.CANCELED
                        || (a.getStatus() == Auction.Status.FINISHED
                        && a.getCurrentWinnerUsername() == null))
                .count();

        statusPieChart.setData(FXCollections.observableArrayList(
                new PieChart.Data("Đang diễn ra", runningCount),
                new PieChart.Data("Chưa TT", unpaidCount),
                new PieChart.Data("Đã TT", paidCount),
                new PieChart.Data("Hết hạn", expiredCount)
        ));
        statusPieChart.setLegendVisible(true);
    }

    // ── Panel switching ───────────────────────────────────────────
    private final List<VBox> allPanels = new java.util.ArrayList<>();

    private void initPanels() {
        allPanels.addAll(List.of(panelOverview, panelProducts, panelActive,
                panelUnpaid, panelPaid, panelExpired, panelRevenue, panelHistory));
    }

    private void showPanel(VBox target, Button activeBtn, String title) {
        if (allPanels.isEmpty()) initPanels();
        allPanels.forEach(p -> { p.setVisible(false); p.setManaged(false); });
        target.setVisible(true); target.setManaged(true);
        sidebarButtons.forEach(b -> {
            b.getStyleClass().removeAll("sidebar-item-active");
            if (!b.getStyleClass().contains("sidebar-item"))
                b.getStyleClass().add("sidebar-item");
        });
        activeBtn.getStyleClass().add("sidebar-item-active");
        pageTitle.setText(title);
    }

    @FXML void showOverview()  { showPanel(panelOverview,  menuOverview,  "Dashboard"); }
    @FXML void showProducts()  { showPanel(panelProducts,  menuProducts,  "Sản phẩm"); }

    @FXML void showActive() {
        showPanel(panelActive, menuActive, "Đang hoạt động");
        setupActiveTable();
        refreshActiveTable();
    }

    @FXML void showUnpaid() {
        showPanel(panelUnpaid, menuUnpaid, "Chưa thanh toán");
        setupUnpaidTable();
        refreshUnpaidTable();
    }

    @FXML void showPaid() {
        showPanel(panelPaid, menuPaid, "Đã thanh toán");
        setupPaidTable();
        refreshPaidTable();
    }

    @FXML void showExpired() {
        showPanel(panelExpired, menuExpired, "Hết hạn");
        setupExpiredTable();
        refreshExpiredTable();
    }

    @FXML void showRevenue()   { showPanel(panelRevenue,   menuRevenue,   "Doanh thu"); }
    @FXML void showHistory() {
        showPanel(panelHistory, menuHistory, "Lịch sử giao dịch");
        setupHistoryTable();
        refreshHistoryTable();
    }

    @FXML
    void handleAddProduct() throws IOException {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/sample/seller_create_auction.fxml"));
        Parent root = loader.load();
        SellerCreateAuctionController ctrl = loader.getController();
        Stage stage = new Stage();
        stage.initOwner(menuProducts.getScene().getWindow());
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setScene(new Scene(root, 900, 720));
        stage.showAndWait();

        AuctionItemDTO result = ctrl.getResult();
        if (result != null) {
            addAuctionToTable(result);
        }
    }

    private void addAuctionToTable(AuctionItemDTO dto) {
        final String type = dto.category != null ? dto.category : "OTHER";
        Item item = new Item(String.valueOf(dto.id), dto.title, dto.startingPrice) {
            @Override public String getType_item() { return type; }
            @Override public void printInfo() {}
        };
        item.setCurrentHighestBid(dto.startingPrice);

        LocalDateTime start = dto.startTime != null ? dto.startTime : LocalDateTime.now();
        LocalDateTime end   = dto.endTime   != null ? dto.endTime   : LocalDateTime.now().plusDays(7);

        Auction a = new Auction(dto.id, item, start, end);
        a.updateHighestBid(dto.startingPrice, null);
        a.updateStatus(Auction.Status.OPEN);
        a.setBidCount(0);

        allAuctions.add(0, a);
        updateStatsFromData();
        setupPieChart();
        refreshHistoryTable();
    }

    private void handleEditProduct(Auction a) {
        if (a == null) return;
        TextInputDialog dialog = new TextInputDialog(a.getItem().getName());
        dialog.setTitle("Sửa tên sản phẩm");
        dialog.setHeaderText("Cập nhật tên hiển thị");
        dialog.setContentText("Tên mới:");
        dialog.showAndWait().ifPresent(newName -> {
            String trimmed = newName == null ? "" : newName.trim();
            if (trimmed.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Không hợp lệ", "Tên sản phẩm không được để trống.");
                return;
            }
            a.getItem().setName(trimmed);
            overviewTable.refresh();
            productsTable.refresh();
            activeTable.refresh();
            unpaidTable.refresh();
            paidTable.refresh();
            expiredTable.refresh();
            refreshHistoryTable();
        });
    }

    private void handleDeleteProduct(Auction a) {
        if (a == null) return;

        int itemId;
        try {
            itemId = Integer.parseInt(a.getItem().getId());
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không đọc được ID sản phẩm");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận xóa");
        confirm.setHeaderText("Xóa sản phẩm \"" + a.getItem().getName() + "\"?");
        confirm.setContentText("Hành động này sẽ xóa sản phẩm và phiên đấu giá vĩnh viễn.");
        confirm.showAndWait().ifPresent(bt -> {
            if (bt != ButtonType.OK) return;

            final int fItemId = itemId;
            new Thread(() -> {
                try {
                    String response = ServerConnection.getInstance().deleteItem(fItemId);
                    Platform.runLater(() -> {
                        if ("DELETE_ITEM_SUCCESS".equals(response)) {
                            allAuctions.remove(a);
                            paidAuctionIds.remove(a.getId());
                            updateStatsFromData();
                            setupPieChart();
                            refreshHistoryTable();
                            showAlert(Alert.AlertType.INFORMATION, "Thành công",
                                    "Đã xóa sản phẩm \"" + a.getItem().getName() + "\".");
                        } else {
                            showAlert(Alert.AlertType.ERROR, "Thất bại",
                                    "Server từ chối xóa: " + response);
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() ->
                            showAlert(Alert.AlertType.ERROR, "Lỗi kết nối",
                                    "Không gửi được yêu cầu đến server: " + e.getMessage()));
                }
            }, "DeleteItemThread").start();
        });
    }

    @FXML
    void handleLogout() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Đăng xuất");
        confirm.setHeaderText("Bạn có chắc muốn đăng xuất?");
        confirm.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                UserSession.getInstance().logout();
                try { ServerConnection.getInstance().disconnect();
                    ServerConnection.getInstance().logout();}
                catch (Exception ignored) {}

                try {
                    FXMLLoader loader = new FXMLLoader(
                            getClass().getResource("/sample/home_demo.fxml"));
                    Parent root = loader.load();
                    HomeController homeCtrl = loader.getController();
                    homeCtrl.resetToGuest();

                    Stage stage = (Stage) menuOverview.getScene().getWindow();
                    stage.setScene(new Scene(root, 1200, 800));
                    stage.setTitle("TINY HOARDER'S KEY MARKET");
                    stage.centerOnScreen();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    // ── Helpers ──────────────────────────────────────────────────
    private ObservableList<Auction> filter(Auction.Status status) {
        return allAuctions.filtered(a -> a.getStatus() == status);
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private String formatMoney(long v) {
        return String.format("%,d", v).replace(",", ".") + " ₫";
    }

    private String statusLabel(String s) {
        return switch (s.toUpperCase()) {
            case "RUNNING"  -> "Đang diễn ra";
            case "FINISHED" -> "Đã kết thúc";
            case "CANCELED" -> "Hết hạn / Huỷ";
            case "OPEN"     -> "Sắp diễn ra";
            default         -> s;
        };
    }

    private String pillStyle(String s) {
        return switch (s.toUpperCase()) {
            case "RUNNING"  -> "pill-live";
            case "OPEN"     -> "pill-upcoming";
            case "FINISHED" -> "pill-paid";
            case "CANCELED" -> "pill-expired";
            default         -> "pill-upcoming";
        };
    }

    private void avatarInitials() {
        String username = UserSession.getInstance().getUsername();
        if (username == null || username.isBlank() || avatarLabel == null) return;
        String cleaned = username.trim().toUpperCase();
        String initials = cleaned.length() >= 2 ? cleaned.substring(0, 2) : cleaned;
        avatarLabel.setText(initials);
    }
}