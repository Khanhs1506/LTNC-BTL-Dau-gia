package sample;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.stage.Popup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javafx.stage.Stage;
import sample.model.Auction;
import sample.model.BidTransaction;
import sample.model.Report;
import sample.model.User;

import java.awt.*;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import javafx.scene.image.ImageView;
import javafx.scene.image.Image;

import java.awt.Desktop;
import java.net.URI;

public class AdminDashboardController implements Initializable {

    // ── Topbar ────────────────────────────────────────────────────
    @FXML private Label adminPageTitle;

    // ── Panels ────────────────────────────────────────────────────
    @FXML private VBox adminPanelOverview;
    @FXML private VBox adminPanelUsers;
    @FXML private VBox adminPanelAuctions;
    @FXML private VBox adminPanelBids;
    @FXML private VBox adminPanelReports;
    @FXML private VBox adminPanelSettings;

    // ── Sidebar buttons ───────────────────────────────────────────
    @FXML private Button adminMenuOverview, adminMenuUsers;
    @FXML private Button adminMenuAuctions, adminMenuBids;
    @FXML private Button adminMenuReports,  adminMenuSettings;

    // ── Stat labels ───────────────────────────────────────────────
    @FXML private Label adminStatUsers, adminStatAuctions;
    @FXML private Label adminStatRevenue, adminStatLive;
    @FXML private Label adminStatPaid, adminStatUnpaid;

    // ── Charts ────────────────────────────────────────────────────
    @FXML private BarChart<String, Number> adminRevenueChart;

    // ── Tables ───────────────────────────────────────────────────
    @FXML private TableView<User>    usersTable;
    @FXML private TableView<Auction> auctionsTable;
    @FXML private TableView<BidTransaction> bidsTable;
    @FXML private TableView<Report>  reportsTable;

    // Table columns – users
    @FXML private TableColumn<User,String> colUId, colUName, colURole,
            colUEmail, colUDate, colUStatus, colUAction;
    // Table columns – auctions
    @FXML private TableColumn<Auction,String> colAAId, colAAName, colAASeller,
            colAAPrice, colAAWinner, colAAStatus, colAAAction;
    // Table columns – bids
    @FXML private TableColumn<BidTransaction,String> colBId, colBAuction,
            colBBidder, colBAmount, colBTime, colBResult;

    // ── Filters ───────────────────────────────────────────────────
    @FXML private TextField userSearchField;
    @FXML private ComboBox<String> roleFilter;
    @FXML private TextField auctionSearch;
    @FXML private ComboBox<String> auctionStatusFilter;
    @FXML private TextField platformFeeField;
    @FXML private TextField sessionDurationField;

    @FXML private javafx.scene.image.ImageView logoImageView;

    //TẠO CHUÔNG
    @FXML private StackPane bellStackAdmin;
    private Label notifBadgeAdmin;

    // ── Data ──────────────────────────────────────────────────────
    private ObservableList<User>           allUsers;
    private ObservableList<Auction>        allAuctions;
    private ObservableList<BidTransaction> allBids;
    private final List<VBox>   panels  = new java.util.ArrayList<>();
    private final List<Button> sbtns   = new java.util.ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        // Load logo giống hệt LoginController
        try {
            URL logoUrl = getClass().getResource("/images/logo_app.png");
            if (logoUrl != null)
                logoImageView.setImage(new Image(logoUrl.toString()));
        } catch (Exception e) {
            System.out.println("Không tải được logo");
        }

        loadMockData();
        setupUsersTable();
        setupAuctionsTable();
        setupBidsTable();
        setupAdminChart();
        initRoleFilter();
        initAuctionFilter();
        showPanel(adminPanelOverview, adminMenuOverview, "Dashboard");
        setupNotificationBadge();
    }

    //SET UP NÚT THÔNG BÁO
    private void setupNotificationBadge() {
        notifBadgeAdmin = createBadgeLabel();
        if (bellStackAdmin != null) bellStackAdmin.getChildren().add(notifBadgeAdmin);
        notifBadgeAdmin.setVisible(false);

        NotificationManager.getInstance().addNotificationListener(() ->
                javafx.application.Platform.runLater(this::refreshBadge)
        );
    }

    //CẬP NHẬT DẤU ĐỎ
    private void refreshBadge() {
        int count = NotificationManager.getInstance().getUnreadCount();
        String text = count > 99 ? "99+" : String.valueOf(count);
        boolean show = count > 0;
        notifBadgeAdmin.setText(text);
        notifBadgeAdmin.setVisible(show);
    }

    //TẠO DẤU ĐỎ HIỆN SỐ LƯỢNG
    private Label createBadgeLabel() {
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

    //XỬ LÍ BẤM CHUÔNG
    @FXML
    private void handleBellClick() {
        NotificationManager.getInstance().markAllRead();
        refreshBadge();

        List<NotificationManager.Notification> list = NotificationManager.getInstance().getAll();

        Popup popup = new Popup();
        popup.setAutoHide(true);

        VBox box = new VBox(0);
        box.setPrefWidth(340);
        box.setMaxHeight(420);
        box.setStyle(
                "-fx-background-color: #1e2228;" +
                        "-fx-border-color: #444;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 10;" +
                        "-fx-background-radius: 10;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 20, 0, 0, 8);"
        );

        Label header = new Label("🔔  Thông báo đặt giá");
        header.setStyle(
                "-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #f0f0f0;" +
                        "-fx-padding: 14 16 12 16;" +
                        "-fx-border-color: #333; -fx-border-width: 0 0 1 0;"
        );
        header.setMaxWidth(Double.MAX_VALUE);
        box.getChildren().add(header);

        if (list.isEmpty()) {
            Label empty = new Label("Chưa có thông báo nào.");
            empty.setStyle("-fx-text-fill: #888; -fx-font-size: 13; -fx-padding: 20 16;");
            box.getChildren().add(empty);
        } else {
            ScrollPane scroll = new ScrollPane();
            scroll.setFitToWidth(true);
            scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            scroll.setPrefViewportHeight(Math.min(list.size() * 68.0, 360));
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
                        ? "-fx-background-color: #1e2228;"
                        : "-fx-background-color: #252930;");

                Label msgLabel = new Label(notif.message);
                msgLabel.setWrapText(true);
                msgLabel.setStyle("-fx-font-size: 13; -fx-text-fill: #e0e0e0;");

                java.time.LocalDateTime ldt = java.time.Instant
                        .ofEpochMilli(notif.timestamp)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDateTime();
                Label timeLabel = new Label(fmt.format(ldt));
                timeLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #888;");

                row.getChildren().addAll(msgLabel, timeLabel);
                items.getChildren().add(row);
            }
            scroll.setContent(items);
            box.getChildren().add(scroll);
        }

        popup.getContent().add(box);

        javafx.geometry.Bounds bounds = bellStackAdmin.localToScreen(bellStackAdmin.getBoundsInLocal());
        popup.show(bellStackAdmin.getScene().getWindow(),
                bounds.getMaxX() - 340,
                bounds.getMaxY() + 4);
    }

    // ── Mock data ─────────────────────────────────────────────────
    private void loadMockData() {
        // Users mock
        allUsers = FXCollections.observableArrayList(
                new User(1, "bidder_alpha", "BIDDER"),
                new User(2, "seller_pro",   "SELLER"),
                new User(3, "user_spammer", "BIDDER")
        );

        // Auctions mock – reuse Auction model
        allAuctions = FXCollections.observableArrayList();

        // Bids mock
        allBids = FXCollections.observableArrayList();
    }

    // ── Setup tables ──────────────────────────────────────────────
    private void setupUsersTable() {
        colUId.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(
                        String.valueOf(c.getValue().getId())));
        colUName.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(c.getValue().getUsername()));
        colURole.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(c.getValue().getRole()));

        // Role badge
        colURole.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setGraphic(null); return; }
                Label badge = new Label(s);
                badge.setStyle(roleBadgeStyle(s));
                setGraphic(badge); setText(null);
            }
        });

        // Action: Khóa / Mở
        colUAction.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("Khóa");
            { btn.getStyleClass().add("admin-btn-danger"); }
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty) { setGraphic(null); return; }
                User u = getTableRow().getItem();
                if (u != null) {
                    btn.setText("banned".equals(u.getStatus()) ? "Mở khóa" : "Khóa");
                    btn.setOnAction(e -> toggleBan(u, btn));
                }
                setGraphic(btn);
            }
        });

        usersTable.setItems(allUsers);

        // Search + filter
        userSearchField.textProperty().addListener((obs, old, nw) -> filterUsers());
        roleFilter.valueProperty().addListener((obs, old, nw) -> filterUsers());
    }

    private void setupAuctionsTable() {
        colAAId.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(
                        String.valueOf(c.getValue().getId())));
        colAAName.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(c.getValue().getItemName()));
        colAAStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setGraphic(null); return; }
                Label pill = new Label(s);
                pill.setStyle(auctionPillStyle(s));
                setGraphic(pill); setText(null);
            }
        });
        auctionsTable.setItems(allAuctions);
        auctionSearch.textProperty().addListener((obs, old, nw) -> filterAuctions());
    }

    private void setupBidsTable() {
        bidsTable.setItems(allBids);
    }

    // ── Chart ─────────────────────────────────────────────────────
    private void setupAdminChart() {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Doanh thu (triệu ₫)");
        series.getData().addAll(
                new XYChart.Data<>("Th.1",  210),
                new XYChart.Data<>("Th.2",  180),
                new XYChart.Data<>("Th.3",  420),
                new XYChart.Data<>("Th.4",  548),
                new XYChart.Data<>("Th.5",  762)
        );
        adminRevenueChart.getData().add(series);
        adminRevenueChart.setLegendVisible(false);
    }

    // ── Filters ───────────────────────────────────────────────────
    private void initRoleFilter() {
        roleFilter.setItems(FXCollections.observableArrayList(
                "Tất cả", "BIDDER", "SELLER", "ADMIN"));
        roleFilter.setValue("Tất cả");
    }

    private void initAuctionFilter() {
        auctionStatusFilter.setItems(FXCollections.observableArrayList(
                "Tất cả", "LIVE", "PAID", "UNPAID", "EXPIRED"));
        auctionStatusFilter.setValue("Tất cả");
    }

    private void filterUsers() {
        String kw   = userSearchField.getText().trim().toLowerCase();
        String role = roleFilter.getValue();
        usersTable.setItems(allUsers.filtered(u ->
                (kw.isEmpty() || u.getUsername().toLowerCase().contains(kw))
                        && ("Tất cả".equals(role) || role.equals(u.getRole()))
        ));
    }

    private void filterAuctions() {
        String kw = auctionSearch.getText().trim().toLowerCase();
        auctionsTable.setItems(allAuctions.filtered(a ->
                kw.isEmpty() || a.getItemName().toLowerCase().contains(kw)));
    }

    // ── Panel switching ───────────────────────────────────────────
    private void initPanels() {
        panels.addAll(List.of(adminPanelOverview, adminPanelUsers,
                adminPanelAuctions, adminPanelBids, adminPanelReports, adminPanelSettings));
        sbtns.addAll(List.of(adminMenuOverview, adminMenuUsers,
                adminMenuAuctions, adminMenuBids, adminMenuReports, adminMenuSettings));
    }

    private void showPanel(VBox target, Button activeBtn, String title) {
        if (panels.isEmpty()) initPanels();
        panels.forEach(p -> { p.setVisible(false); p.setManaged(false); });
        target.setVisible(true); target.setManaged(true);
        sbtns.forEach(b -> {
            b.getStyleClass().removeAll("admin-sidebar-item-active");
            if (!b.getStyleClass().contains("admin-sidebar-item"))
                b.getStyleClass().add("admin-sidebar-item");
        });
        activeBtn.getStyleClass().add("admin-sidebar-item-active");
        adminPageTitle.setText(title);
    }

    // ── FXML handlers ─────────────────────────────────────────────
    @FXML void showOverview()  { showPanel(adminPanelOverview,  adminMenuOverview,  "Dashboard"); }
    @FXML void showUsers()     { showPanel(adminPanelUsers,     adminMenuUsers,     "Người dùng"); }
    @FXML void showAuctions()  { showPanel(adminPanelAuctions,  adminMenuAuctions,  "Phiên đấu giá"); }
    @FXML void showBids()      { showPanel(adminPanelBids,      adminMenuBids,      "Bid Transactions"); }
    @FXML void showReports()   { showPanel(adminPanelReports,   adminMenuReports,   "Báo cáo vi phạm"); }
    @FXML void showSettings()  { showPanel(adminPanelSettings,  adminMenuSettings,  "Cài đặt"); }

    @FXML void handleSaveSettings() {
        String fee      = platformFeeField.getText();
        String duration = sessionDurationField.getText();
        // TODO: gửi lên server để lưu config
        new Alert(Alert.AlertType.INFORMATION, "Đã lưu cài đặt!").showAndWait();
    }

    @FXML
    void handleLogout() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Đăng xuất");
        confirm.setHeaderText("Bạn có chắc muốn đăng xuất?");
        confirm.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                // 1. Xóa session
                UserSession.getInstance().logout();

                // 2. Tìm FXML qua chính HomeController class (luôn đúng package)
                java.net.URL fxmlUrl = HomeController.class.getResource("home.fxml");

                // Fallback nếu home.fxml cùng thư mục với HomeController
                if (fxmlUrl == null) {
                    fxmlUrl = HomeController.class.getResource("/sample/home_demo.fxml");
                }

                if (fxmlUrl == null) {
                    // In ra tất cả để debug
                    System.err.println("[Logout] Thư mục class: "
                            + HomeController.class.getPackage().getName());
                    System.err.println("[Logout] Thử path: "
                            + HomeController.class.getResource("."));
                    new Alert(Alert.AlertType.ERROR,
                            "Không tìm thấy home.fxml!\n"
                                    + "Kiểm tra tên file có đúng chữ thường/hoa không.")
                            .showAndWait();
                    return;
                }

                try {
                    javafx.fxml.FXMLLoader loader =
                            new javafx.fxml.FXMLLoader(fxmlUrl);
                    javafx.scene.Parent root = loader.load();

                    // 3. Reset Home về trạng thái khách
                    HomeController homeCtrl = loader.getController();
                    homeCtrl.resetToGuest();

                    // 4. Chuyển scene
                    javafx.stage.Stage stage =
                            (javafx.stage.Stage) adminMenuOverview.getScene().getWindow();
                    stage.setScene(new javafx.scene.Scene(root, 1200, 800));
                    stage.setTitle("TINY HOARDER'S KEY MARKET");
                    stage.centerOnScreen();

                } catch (java.io.IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    // ── Helpers ───────────────────────────────────────────────────
    private void toggleBan(User u, Button btn) {
        boolean isBanned = "banned".equals(u.getStatus());
        // TODO: gửi lệnh lên server
        btn.setText(isBanned ? "Khóa" : "Mở khóa");
    }

    private String roleBadgeStyle(String role) {
        return switch (role) {
            case "BIDDER" -> "-fx-background-color:#E6F1FB; -fx-text-fill:#185FA5;"
                    + "-fx-background-radius:10; -fx-padding:2 8; -fx-font-size:11;";
            case "SELLER" -> "-fx-background-color:#EAF3DE; -fx-text-fill:#3B6D11;"
                    + "-fx-background-radius:10; -fx-padding:2 8; -fx-font-size:11;";
            case "ADMIN"  -> "-fx-background-color:#EEEDFE; -fx-text-fill:#534AB7;"
                    + "-fx-background-radius:10; -fx-padding:2 8; -fx-font-size:11;";
            default       -> "";
        };
    }

    private String auctionPillStyle(String s) {
        return switch (s.toUpperCase()) {
            case "LIVE"    -> "-fx-background-color:#FDECEA; -fx-text-fill:#A32D2D;"
                    + "-fx-background-radius:10; -fx-padding:2 8; -fx-font-size:11;";
            case "PAID"    -> "-fx-background-color:#EAF3DE; -fx-text-fill:#3B6D11;"
                    + "-fx-background-radius:10; -fx-padding:2 8; -fx-font-size:11;";
            case "UNPAID"  -> "-fx-background-color:#FAEEDA; -fx-text-fill:#633806;"
                    + "-fx-background-radius:10; -fx-padding:2 8; -fx-font-size:11;";
            default        -> "";
        };
    }

    // =====SỰ KIỆN CỦA CHUÔNG THÔNG BÁO=====
//    @FXML
//    private void handleBellClick() {
//        try {
//            Desktop.getDesktop().browse(new URI("https://youtu.be/dQw4w9WgXcQ?si=pkMnzjiXULSLfJeH"));
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
}