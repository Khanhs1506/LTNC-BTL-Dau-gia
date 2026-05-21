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
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;

import sample.AuctionItemDTO;
import javafx.stage.Modality;

public class SellerDashboardController implements Initializable {

    // ── Topbar ────────────────────────────────────────────────────
    @FXML private Label lblUsername;
    @FXML private Label pageTitle;

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
    @FXML private TableView<Auction> historyTable;

    @FXML private TableColumn<Auction,String> colOvId, colOvName, colOvBid,
            colOvBidder, colOvStatus;
    @FXML private TableColumn<Auction,String> colPId, colPName, colPPrice,
            colPCat, colPStatus, colPActions;

    // ── Search / filter ───────────────────────────────────────────
    @FXML private TextField txtSearch;
    @FXML private TextField searchProducts;
    @FXML private ComboBox<String> filterCategory;

    @FXML private StackPane bellStackSeller;
    private Label notifBadgeSeller;

    // ── Mock data ────────────────────────────────────────────────
    private ObservableList<Auction> allAuctions;
    private final List<Button> sidebarButtons = new java.util.ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        String user = UserSession.getInstance().getUsername();
        lblUsername.setText(user != null ? user : "SELLER");
        avatarInitials();

        sidebarButtons.addAll(List.of(
                menuOverview, menuActive, menuPaid,
                menuUnpaid, menuExpired, menuProducts,
                menuRevenue, menuHistory));

        //HIỆN THỊ DỮ LIỆU TỪ SERVER
        loadFromServer();
        setupOverviewTable();
        setupProductsTable();
        setupRevenueChart();
        setupPieChart();
        showPanel(panelOverview, menuOverview, "Dashboard");
        setupNotificationBadge();
    }

    //SET UP NÚT CHUÔNG THÔNG BÁO
    private void setupNotificationBadge() {
        notifBadgeSeller = creatBadgeLabel();
        if (bellStackSeller != null) bellStackSeller.getChildren().add(notifBadgeSeller);
        notifBadgeSeller.setVisible(false);
        NotificationManager.getInstance().addNotificationListener(() -> javafx.application.Platform.runLater(this :: refreshBadge));
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

    //LẤY DỮ LIỆU TỪ SERVER
    private void loadFromServer() {
        new Thread(() -> {
            try {
                String response = ServerConnection.getInstance().getAuctionsBySeller();

                if (response != null && response.startsWith("AUCTIONS===")) {
                    String json = response.substring("AUCTIONS===".length());
                    JsonArray arr = JsonParser.parseString(json).getAsJsonArray();

                    ObservableList<Auction> loaded = FXCollections.observableArrayList();
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
                        LocalDateTime startTime =
                                LocalDateTime.parse(obj.get("startTime").getAsString(), fmt);
                        LocalDateTime endTime   =
                                LocalDateTime.parse(obj.get("endTime").getAsString(), fmt);

                        // Tạo Item ẩn danh vì Item là abstract
                        final String finalItemType = itemType;
                        Item item = new Item(String.valueOf(auctionId), itemName, startPrice) {
                            @Override public String getType_item() { return finalItemType; }
                            @Override public void printInfo() { System.out.println(getName()); }
                        };
                        item.setCurrentHighestBid(highBid);

                        Auction a = new Auction(auctionId, item, startTime, endTime);
                        a.updateHighestBid(highBid, winner);
                        a.updateStatus(Auction.Status.valueOf(statusStr));
                        loaded.add(a);
                    }

                    Platform.runLater(() -> {
                        allAuctions = loaded;
                        updateStatsFromData();
                        setupPieChart();
                        overviewTable.setItems(allAuctions);
                        productsTable.setItems(allAuctions);
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

    //LÀM MỚI TRẠNG THÁI
    private void updateStatsFromData() {
        statTotal.setText(String.valueOf(allAuctions.size()));
        statActive.setText(String.valueOf(count(Auction.Status.RUNNING)));
        statUnpaid.setText(String.valueOf(count(Auction.Status.FINISHED)));
        unpaidBadge.setText(count(Auction.Status.FINISHED) + " phiên chờ xử lý");

        long revenue = allAuctions.stream()
                .filter(a -> a.getStatus() == Auction.Status.FINISHED)
                .mapToLong(a -> (long) a.getCurrentHighestBid())
                .sum();
        statRevenue.setText(formatMoney(revenue));
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
        return a;
    }

    private long count(Auction.Status status) {
        return allAuctions.stream()
                .filter(a -> a.getStatus() == status)
                .count();
    }

    // ── Setup tables ─────────────────────────────────────────────
    private void setupOverviewTable() {
        colOvId.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(
                        String.valueOf(c.getValue().getId())));
        colOvName.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(
                        c.getValue().getItem().getName()));
        colOvBid.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(
                        formatMoney((long) c.getValue().getCurrentHighestBid())));
        colOvBidder.setCellValueFactory(c -> {
            String winner = c.getValue().getCurrentWinnerUsername();
            return new javafx.beans.property.SimpleStringProperty(
                    winner != null ? winner : "—");
        });
        colOvStatus.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(
                        c.getValue().getStatus().name()));
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
                new javafx.beans.property.SimpleStringProperty(
                        String.valueOf(c.getValue().getId())));
        colPName.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(
                        c.getValue().getItem().getName()));
        colPPrice.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(
                        formatMoney((long) c.getValue().getItem().getStartingPrice())));
        colPCat.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(
                        c.getValue().getItem().getTypeItem()));
        colPStatus.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(
                        c.getValue().getStatus().name()));
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
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Doanh thu (triệu ₫)");
        series.getData().addAll(
                new XYChart.Data<>("Th.12", 21.5),
                new XYChart.Data<>("Th.1",  28.8),
                new XYChart.Data<>("Th.2",  18.2),
                new XYChart.Data<>("Th.3",  34.6),
                new XYChart.Data<>("Th.4",  40.8),
                new XYChart.Data<>("Th.5",  48.2)
        );
        revenueChart.getData().add(series);
        revenueChart.setLegendVisible(false);

        XYChart.Series<String, Number> series2 = new XYChart.Series<>();
        series2.setName("Doanh thu");
        series2.getData().addAll(
                new XYChart.Data<>("Th.12", 21.5),
                new XYChart.Data<>("Th.1",  28.8),
                new XYChart.Data<>("Th.2",  18.2),
                new XYChart.Data<>("Th.3",  34.6),
                new XYChart.Data<>("Th.4",  40.8),
                new XYChart.Data<>("Th.5",  48.2)
        );
        detailRevenueChart.getData().add(series2);
    }

    private void setupPieChart() {
        if (allAuctions == null) return;
        statusPieChart.setData(FXCollections.observableArrayList(
                new PieChart.Data("Đang diễn ra", count(Auction.Status.RUNNING)),
                new PieChart.Data("Chưa TT",      count(Auction.Status.FINISHED)),
                new PieChart.Data("Hết hạn",      count(Auction.Status.CANCELED))
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
        activeTable.setItems(filter(Auction.Status.RUNNING));
    }
    @FXML void showUnpaid() {
        showPanel(panelUnpaid, menuUnpaid, "Chưa thanh toán");
        unpaidTable.setItems(filter(Auction.Status.FINISHED));
    }
    @FXML void showPaid() {
        showPanel(panelPaid, menuPaid, "Đã thanh toán");
        paidTable.setItems(filter(Auction.Status.FINISHED));
    }
    @FXML void showExpired() {
        showPanel(panelExpired, menuExpired, "Hết hạn");
        expiredTable.setItems(filter(Auction.Status.CANCELED));
    }
    @FXML void showRevenue()   { showPanel(panelRevenue,   menuRevenue,   "Doanh thu"); }
    @FXML void showHistory()   { showPanel(panelHistory,   menuHistory,   "Lịch sử giao dịch"); }

    @FXML
    void handleAddProduct() throws IOException {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/sample/seller_create_auction.fxml"));
        Parent root = loader.load();
        SellerCreateAuctionController ctrl = loader.getController();
        Stage stage = new Stage();
        stage.initOwner(menuProducts.getScene().getWindow()); // ← thêm owner
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setScene(new Scene(root, 900, 720));
        stage.showAndWait();

        AuctionItemDTO result = ctrl.getResult();
        if (result != null) {
            // Sau khi tạo thành công, reload lại từ server để đồng bộ
            loadFromServer();
        }
    }

    private void handleEditProduct(Auction a) {
        // TODO: Mở dialog sửa
    }

    private void handleDeleteProduct(Auction a) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Xóa sản phẩm \"" + a.getItem().getName() + "\"?",
                ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) allAuctions.remove(a);
        });
    }

    @FXML void handleLogout() {
        UserSession.getInstance().logout();
        // TODO: quay về Home
    }

    // ── Helpers ──────────────────────────────────────────────────
    private ObservableList<Auction> filter(Auction.Status status) {
        return allAuctions.filtered(a -> a.getStatus() == status);
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
        // TODO: lấy 2 chữ đầu username set vào avatarLabel
    }
}