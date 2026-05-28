package sample;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import sample.model.AdminReport;
import sample.model.User;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;

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
    @FXML private javafx.scene.chart.BarChart<String, Number> adminRevenueChart;

    // ── Bảng Người dùng ──────────────────────────────────────────
    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, String> colUId, colUName, colURole,
            colUEmail, colUDate, colUStatus, colUAction;

    // ── Bảng Phiên đấu giá ────────────────────────────────────────
    public static class AdminAuctionRow {
        public int    auctionId;
        public String itemName;
        public String sellerUsername;
        public double currentHighestBid;
        public String currentWinnerUsername;
        public String status;
        public String endTime;
    }

    @FXML private TableView<AdminAuctionRow> auctionsTable;
    @FXML private TableColumn<AdminAuctionRow, String> colAAId, colAAName, colAASeller,
            colAAPrice, colAAWinner, colAAStatus, colAAAction;

    // ── Bảng Lịch sử đặt giá ──────────────────────────────────────
    public static class AdminBidRow {
        public String transactionId;
        public int    auctionId;
        public String bidderUsername;
        public double bidAmount;
        public String timestamp;
    }

    @FXML private TableView<AdminBidRow> bidsTable;
    @FXML private TableColumn<AdminBidRow, String> colBId, colBAuction,
            colBBidder, colBAmount, colBTime, colBResult;

    //Bảng Báo cáo vi phạm
    @FXML private TableView<AdminReport> reportsTable;
    @FXML private TableColumn<AdminReport, String> colRId, colRReporter,
            colRTarget, colRReason, colRDate, colRAction;
    @FXML private Label reportPendingLabel;

    //Filters
    @FXML private TextField  userSearchField;
    @FXML private ComboBox<String> roleFilter;
    @FXML private TextField  auctionSearch;
    @FXML private ComboBox<String> auctionStatusFilter;
    @FXML private TextField  bidSearchField;

    //Cài đặt
    @FXML private TextField platformFeeField;
    @FXML private TextField sessionDurationField;

    //Logo + Bell
    @FXML private ImageView logoImageView;
    @FXML private StackPane bellStackAdmin;
    private Label notifBadgeAdmin;

    //Dữ liệu
    private ObservableList<User>             allUsers;
    private ObservableList<AdminAuctionRow>  allAuctions;
    private ObservableList<AdminBidRow>      allBids;
    private ObservableList<AdminReport>      allReports;

    private final List<VBox>   panels = new ArrayList<>();
    private final List<Button> sbtns  = new ArrayList<>();

    // ── Cache tối ưu: tránh gọi server mỗi lần bấm tab ──────────
    private static final long TTL_AUCTIONS = 30_000L;  // dữ liệu live: 30s
    private static final long TTL_USERS    = 60_000L;  // ít thay đổi: 60s
    private static final long TTL_BIDS     = 120_000L; // lịch sử: 2 phút
    private static final long TTL_REPORTS  = 60_000L;  // moderate: 60s
    private static final long TTL_STATS    = 60_000L;  // thống kê tổng quan: 60s

    private final Map<String, Long>          cacheTime    = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<String, AtomicBoolean> loadingFlag  = new java.util.concurrent.ConcurrentHashMap<>();

    private final java.util.concurrent.ExecutorService executor =
            java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "AdminBgLoader");
                t.setDaemon(true);
                return t;
            });

    private boolean isCacheStale(String key) {
        Long t = cacheTime.get(key);
        if (t == null) return true;
        long ttl = switch (key) {
            case "auctions" -> TTL_AUCTIONS;
            case "bids"     -> TTL_BIDS;
            case "reports"  -> TTL_REPORTS;
            case "stats"    -> TTL_STATS;
            default         -> TTL_USERS;
        };
        return (System.currentTimeMillis() - t) > ttl;
    }

    private void markCacheFresh(String key) {
        cacheTime.put(key, System.currentTimeMillis());
    }

    private boolean isLoading(String key) {
        return loadingFlag.computeIfAbsent(key, k -> new AtomicBoolean(false)).get();
    }

    private void setLoading(String key, boolean value) {
        loadingFlag.computeIfAbsent(key, k -> new AtomicBoolean(false)).set(value);
    }

    private boolean tryAcquireLoading(String key) {
        return loadingFlag.computeIfAbsent(key, k -> new AtomicBoolean(false))
                .compareAndSet(false, true);
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        // Load logo giống hệt LoginController
        try {
            URL logoUrl = getClass().getResource("/images/logo_app.png");
            if (logoUrl != null) logoImageView.setImage(new Image(logoUrl.toString()));
        } catch (Exception ignored) {}

        //khởi tạo tất cả ObservableList TRƯỚC KHI setup bảng
        allUsers    = FXCollections.observableArrayList();
        allAuctions = FXCollections.observableArrayList();
        allBids     = FXCollections.observableArrayList();
        allReports  = FXCollections.observableArrayList();

        //setup bảng
        setupUsersTable();
        setupAuctionsTable();
        setupBidsTable();
        setupReportsTable();

        //placeholder khi bảng đang tải
        usersTable.setPlaceholder(new Label("⏳ Đang tải dữ liệu..."));
        auctionsTable.setPlaceholder(new Label("⏳ Đang tải dữ liệu..."));
        bidsTable.setPlaceholder(new Label("⏳ Đang tải dữ liệu..."));
        reportsTable.setPlaceholder(new Label("⏳ Đang tải dữ liệu..."));

        //chart + filter
        setupAdminChart();
        initRoleFilter();
        initAuctionStatusFilter();

        //hiển thị panel mặc định
        showPanel(adminPanelOverview, adminMenuOverview, "Dashboard");

        //badge thông báo
        setupNotificationBadge();
        executor.submit(() -> {
            fetchAndCacheStats();
            fetchAndCacheUsers();
            fetchAndCacheAuctions();
            fetchAndCacheBids();
            fetchAndCacheReports();
        });
    }

    //  BẢNG NGƯỜI DÙNG
    private void setupUsersTable() {
        colUId.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(c.getValue().getId()));

        colUName.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(c.getValue().getUsername()));

        // Vai trò – badge màu
        colURole.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(c.getValue().getRole()));
        colURole.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setGraphic(null); setText(null); return; }
                Label badge = new Label(s);
                badge.setStyle(roleBadgeStyle(s));
                setGraphic(badge); setText(null);
            }
        });

        // Email – DB chưa có cột này, hiện "—"
        colUEmail.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty("—"));

        // Ngày đăng ký
        colUDate.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(
                        c.getValue().getCreatedAt() != null ? c.getValue().getCreatedAt() : "—"));

        // Trạng thái – badge màu
        colUStatus.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(
                        c.getValue().getStatus() != null ? c.getValue().getStatus() : "active"));
        colUStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setGraphic(null); setText(null); return; }
                Label pill = new Label("banned".equalsIgnoreCase(s) ? "Bị khóa" : "Hoạt động");
                pill.setStyle("banned".equalsIgnoreCase(s)
                        ? "-fx-background-color:#FDECEA;-fx-text-fill:#A32D2D;-fx-background-radius:10;-fx-padding:2 8;-fx-font-size:11;"
                        : "-fx-background-color:#EAF3DE;-fx-text-fill:#3B6D11;-fx-background-radius:10;-fx-padding:2 8;-fx-font-size:11;");
                setGraphic(pill); setText(null);
            }
        });

        // Hành động: Khóa / Mở khóa
        colUAction.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("Khóa");
            { btn.getStyleClass().add("admin-btn-danger"); }

            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty) { setGraphic(null); return; }
                User u = getTableRow().getItem();
                if (u == null) { setGraphic(null); return; }
                boolean banned = "banned".equalsIgnoreCase(u.getStatus());
                btn.setText(banned ? "Mở khóa" : "Khóa");
                btn.setOnAction(e -> toggleBan(u, btn));
                setGraphic(btn);
            }
        });

        usersTable.setItems(allUsers);

        userSearchField.textProperty().addListener((obs, o, n) -> filterUsers());
        roleFilter.valueProperty().addListener((obs, o, n) -> filterUsers());
    }

    private void loadUsersFromServer() {
        if (isLoading("users") || !isCacheStale("users")) return;
        executor.submit(this::fetchAndCacheUsers);
    }
    private void fetchAndCacheUsers() {
        if (!isCacheStale("users") || !tryAcquireLoading("users")) return;
        try {
            String raw      = ServerConnection.getInstance().getUsers();
            String jsonPart = raw.contains("===") ? raw.split("===", 2)[1] : "[]";

            JsonArray arr = JsonParser.parseString(jsonPart).getAsJsonArray();

            List<User> users = new ArrayList<>();
            for (JsonElement el : arr) {
                JsonObject obj = el.getAsJsonObject();
                User u = new User(
                        obj.get("id").getAsString(),
                        obj.get("username").getAsString(),
                        "",
                        obj.get("role").getAsString()
                );
                u.setStatus(obj.has("status")     ? obj.get("status").getAsString()     : "active");
                u.setCreatedAt(obj.has("createdAt") ? obj.get("createdAt").getAsString() : null);
                users.add(u);
            }

            // Đánh dấu cache fresh TRƯỚC runLater → thread khác thấy ngay, không gọi lại
            markCacheFresh("users");
            setLoading("users", false);

            javafx.application.Platform.runLater(() -> {
                allUsers.setAll(users);
                usersTable.setPlaceholder(new Label("Không có dữ liệu"));
                if (adminStatUsers != null)
                    adminStatUsers.setText(String.valueOf(users.size()));
            });
        } catch (Exception e) {
            setLoading("users", false);
            System.out.println("[Admin] Lỗi tải user: " + e.getMessage());
        }
    }

    private void filterUsers() {
        String kw   = userSearchField.getText().trim().toLowerCase();
        String role = roleFilter.getValue();
        usersTable.setItems(allUsers.filtered(u ->
                (kw.isEmpty() || u.getUsername().toLowerCase().contains(kw))
                        && ("Tất cả".equals(role) || role.equals(u.getRole()))
        ));
    }

    private void toggleBan(User u, Button btn) {
        boolean isBanned = "banned".equalsIgnoreCase(u.getStatus());
        executor.submit(() -> {
            try {
                String response = isBanned
                        ? ServerConnection.getInstance().unbanUser(u.getUsername())
                        : ServerConnection.getInstance().banUser(u.getUsername());
                boolean ok = response != null && response.contains("OK");
                javafx.application.Platform.runLater(() -> {
                    if (ok) {
                        u.setStatus(isBanned ? "active" : "banned");
                        btn.setText(isBanned ? "Khóa" : "Mở khóa");
                        usersTable.refresh();
                        cacheTime.remove("users"); // invalidate để lần sau reload mới
                        showToast(isBanned
                                        ? "Đã mở khóa: " + u.getUsername()
                                        : "Đã khóa: "    + u.getUsername(),
                                ToastNotification.ToastType.SUCCESS);
                    } else {
                        showToast("Lỗi: không thể cập nhật trạng thái",
                                ToastNotification.ToastType.ERROR);
                    }
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> showToast("Lỗi kết nối: " + e.getMessage(),
                        ToastNotification.ToastType.ERROR));
            }
        });
    }

    //  BẢNG PHIÊN ĐẤU GIÁ
    private void setupAuctionsTable() {
        colAAId.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(
                        String.valueOf(c.getValue().auctionId)));

        colAAName.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(c.getValue().itemName));

        colAASeller.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(
                        c.getValue().sellerUsername != null ? c.getValue().sellerUsername : "—"));

        colAAPrice.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(
                        String.format("%,.0f ₫", c.getValue().currentHighestBid)));

        colAAWinner.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(
                        c.getValue().currentWinnerUsername != null
                                ? c.getValue().currentWinnerUsername : "—"));

        // Trạng thái – badge màu
        colAAStatus.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(c.getValue().status));
        colAAStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setGraphic(null); setText(null); return; }
                Label pill = new Label(translateAuctionStatus(s));
                pill.setStyle(auctionPillStyle(s));
                setGraphic(pill); setText(null);
            }
        });

        // Hành động: Hủy phiên (chỉ OPEN/RUNNING)
        colAAAction.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("Hủy");
            { btn.getStyleClass().add("admin-btn-danger"); }

            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty) { setGraphic(null); return; }
                AdminAuctionRow row = getTableRow().getItem();
                if (row == null) { setGraphic(null); return; }
                boolean canCancel = "OPEN".equals(row.status) || "RUNNING".equals(row.status);
                btn.setDisable(!canCancel);
                btn.setOnAction(e -> cancelAuction(row));
                setGraphic(btn);
            }
        });

        auctionsTable.setItems(allAuctions);

        auctionSearch.textProperty().addListener((obs, o, n) -> filterAuctions());
        auctionStatusFilter.valueProperty().addListener((obs, o, n) -> filterAuctions());
    }

    private void loadAuctionsFromServer() {
        if (isLoading("auctions") || !isCacheStale("auctions")) return;
        executor.submit(this::fetchAndCacheAuctions);
    }

    private void fetchAndCacheAuctions() {
        if (!isCacheStale("auctions") || !tryAcquireLoading("auctions")) return;
        try {
            String raw      = ServerConnection.getInstance().getAdminAuctions();
            String jsonPart = raw.contains("===") ? raw.split("===", 2)[1] : "[]";
            if (jsonPart.startsWith("[]") || jsonPart.isEmpty()) jsonPart = "[]";

            JsonArray arr = JsonParser.parseString(jsonPart).getAsJsonArray();

            List<AdminAuctionRow> rows = new ArrayList<>();
            for (JsonElement el : arr) {
                JsonObject obj = el.getAsJsonObject();
                AdminAuctionRow r = new AdminAuctionRow();
                r.auctionId            = obj.has("auctionId")            ? obj.get("auctionId").getAsInt()            : 0;
                r.itemName             = obj.has("itemName")             ? obj.get("itemName").getAsString()           : "—";
                r.sellerUsername       = obj.has("sellerUsername")       ? obj.get("sellerUsername").getAsString()     : "—";
                r.currentHighestBid    = obj.has("currentHighestBid")    ? obj.get("currentHighestBid").getAsDouble()  : 0;
                r.currentWinnerUsername= obj.has("currentWinnerUsername") && !obj.get("currentWinnerUsername").isJsonNull()
                        ? obj.get("currentWinnerUsername").getAsString() : null;
                r.status               = obj.has("status")               ? obj.get("status").getAsString()             : "OPEN";
                r.endTime              = obj.has("endTime")              ? obj.get("endTime").getAsString()             : "";
                rows.add(r);
            }

            markCacheFresh("auctions");
            setLoading("auctions", false);

            javafx.application.Platform.runLater(() -> {
                allAuctions.setAll(rows);
                auctionsTable.setPlaceholder(new Label("Không có dữ liệu"));
                if (adminStatAuctions != null)
                    adminStatAuctions.setText(String.valueOf(rows.size()));
                long live = rows.stream().filter(r -> "RUNNING".equals(r.status)).count();
                if (adminStatLive != null) adminStatLive.setText(String.valueOf(live));
            });
        } catch (Exception e) {
            setLoading("auctions", false);
            System.out.println("[Admin] Lỗi tải auctions: " + e.getMessage());
        }
    }

    private void filterAuctions() {
        String kw     = auctionSearch.getText().trim().toLowerCase();
        String status = auctionStatusFilter.getValue();
        auctionsTable.setItems(allAuctions.filtered(r ->
                (kw.isEmpty() || r.itemName.toLowerCase().contains(kw)
                        || String.valueOf(r.auctionId).contains(kw))
                        && ("Tất cả".equals(status) || status.equals(r.status))
        ));
    }

    private void cancelAuction(AdminAuctionRow row) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Hủy phiên đấu giá #" + row.auctionId + " – " + row.itemName + "?");
        confirm.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                executor.submit(() -> {
                    try {
                        String res = ServerConnection.getInstance().cancelAuction(row.auctionId);
                        boolean ok = res != null && res.contains("OK");
                        javafx.application.Platform.runLater(() -> {
                            if (ok) {
                                row.status = "CANCELED";
                                auctionsTable.refresh();
                                cacheTime.remove("auctions");
                                showToast("Đã hủy phiên đấu giá #" + row.auctionId,
                                        ToastNotification.ToastType.SUCCESS);
                            } else {
                                showToast("Không thể hủy phiên này",
                                        ToastNotification.ToastType.ERROR);
                            }
                        });
                    } catch (Exception e) {
                        javafx.application.Platform.runLater(() ->
                                showToast("Lỗi kết nối: " + e.getMessage(),
                                        ToastNotification.ToastType.ERROR));
                    }
                });
            }
        });
    }

    //  BẢNG LỊCH SỬ ĐẶT GIÁ
    private void setupBidsTable() {
        colBId.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(
                        c.getValue().transactionId != null
                                ? c.getValue().transactionId.substring(0, 8) + "…"
                                : "—"));

        colBAuction.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(
                        "#" + c.getValue().auctionId));

        colBBidder.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(c.getValue().bidderUsername));

        colBAmount.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(
                        String.format("%,.0f ₫", c.getValue().bidAmount)));

        colBTime.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(c.getValue().timestamp));

        // colBResult: chỉ dùng để hiện "Hoàn tất" (không có real-time winner tracking ở đây)
        colBResult.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty("Hoàn tất"));
        colBResult.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setGraphic(null); setText(null); return; }
                Label pill = new Label(s);
                pill.setStyle("-fx-background-color:#EAF3DE;-fx-text-fill:#3B6D11;"
                        + "-fx-background-radius:10;-fx-padding:2 8;-fx-font-size:11;");
                setGraphic(pill); setText(null);
            }
        });

        bidsTable.setItems(allBids);

        if (bidSearchField != null)
            bidSearchField.textProperty().addListener((obs, o, n) -> filterBids());
    }

    private void loadBidsFromServer() {
        if (isLoading("bids") || !isCacheStale("bids")) return;
        executor.submit(this::fetchAndCacheBids);
    }

    private void fetchAndCacheBids() {
        if (!isCacheStale("bids") || !tryAcquireLoading("bids")) return;
        try {
            String raw      = ServerConnection.getInstance().getAdminBids();
            String jsonPart = raw.contains("===") ? raw.split("===", 2)[1] : "[]";
            if (jsonPart.startsWith("[]") || jsonPart.isEmpty()) jsonPart = "[]";
            JsonArray arr = JsonParser.parseString(jsonPart).getAsJsonArray();

            List<AdminBidRow> rows = new ArrayList<>();
            for (JsonElement el : arr) {
                JsonObject obj = el.getAsJsonObject();
                AdminBidRow r = new AdminBidRow();
                r.transactionId  = obj.has("transactionId")  ? obj.get("transactionId").getAsString()  : "";
                r.auctionId      = obj.has("auctionId")      ? obj.get("auctionId").getAsInt()         : 0;
                r.bidderUsername = obj.has("bidderUsername") ? obj.get("bidderUsername").getAsString() : "—";
                r.bidAmount      = obj.has("bidAmount")      ? obj.get("bidAmount").getAsDouble()      : 0;
                r.timestamp      = obj.has("timestamp")      ? obj.get("timestamp").getAsString()      : "";
                rows.add(r);
            }

            markCacheFresh("bids");
            setLoading("bids", false);

            javafx.application.Platform.runLater(() -> {
                allBids.setAll(rows);
                bidsTable.setPlaceholder(new Label("Không có dữ liệu"));
            });
        } catch (Exception e) {
            setLoading("bids", false);
            System.out.println("[Admin] Lỗi tải bids: " + e.getMessage());
        }
    }

    private void filterBids() {
        if (bidSearchField == null) return;
        String kw = bidSearchField.getText().trim().toLowerCase();
        bidsTable.setItems(allBids.filtered(r ->
                kw.isEmpty()
                        || r.bidderUsername.toLowerCase().contains(kw)
                        || String.valueOf(r.auctionId).contains(kw)));
    }

    //  BẢNG BÁO CÁO VI PHẠM
    private void setupReportsTable() {
        colRId.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(c.getValue().getId()));

        colRReporter.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(c.getValue().getReporterUsername()));

        colRTarget.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(c.getValue().getTargetUsername()));

        colRReason.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(c.getValue().getReason()));

        colRDate.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(c.getValue().getCreatedAt()));

        // Cột Xử lý – nút "Giải quyết" hoặc badge "Đã xử lý"
        colRAction.setCellValueFactory(c ->
                new javafx.beans.property.SimpleStringProperty(c.getValue().getStatus()));
        colRAction.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("Giải quyết");
            { btn.getStyleClass().add("admin-btn-primary"); }

            @Override protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty) { setGraphic(null); return; }
                AdminReport report = getTableRow().getItem();
                if (report == null) { setGraphic(null); return; }

                if ("RESOLVED".equals(status)) {
                    Label done = new Label("✓ Đã xử lý");
                    done.setStyle("-fx-text-fill:#27ae60;-fx-font-size:12;");
                    setGraphic(done);
                } else {
                    btn.setOnAction(e -> resolveReport(report));
                    setGraphic(btn);
                }
            }
        });

        reportsTable.setItems(allReports);
    }

    private void loadReportsFromServer() {
        if (isLoading("reports") || !isCacheStale("reports")) return;
        executor.submit(this::fetchAndCacheReports);
    }

    private void fetchAndCacheReports() {
        if (!isCacheStale("reports") || !tryAcquireLoading("reports")) return;
        try {
            String raw      = ServerConnection.getInstance().getReports();
            String jsonPart = raw.contains("===") ? raw.split("===", 2)[1] : "[]";
            if (jsonPart.startsWith("[]") || jsonPart.isEmpty()) jsonPart = "[]";
            JsonArray arr = JsonParser.parseString(jsonPart).getAsJsonArray();

            List<AdminReport> rows = new ArrayList<>();
            for (JsonElement el : arr) {
                JsonObject obj = el.getAsJsonObject();
                AdminReport r = new AdminReport(
                        obj.has("id") ? String.valueOf(obj.get("id").getAsInt()) : "?",
                        obj.has("reporterUsername") ? obj.get("reporterUsername").getAsString() : "—",
                        obj.has("targetUsername") ? obj.get("targetUsername").getAsString() : "—",
                        obj.has("reason") ? obj.get("reason").getAsString() : "—",
                        obj.has("createdAt") ? obj.get("createdAt").getAsString() : "—",
                        obj.has("status") ? obj.get("status").getAsString() : "PENDING"
                );
                rows.add(r);
            }

            markCacheFresh("reports");
            setLoading("reports", false);

            javafx.application.Platform.runLater(() -> {
                allReports.setAll(rows);
                reportsTable.setPlaceholder(new Label("Không có dữ liệu"));
                long pending = rows.stream()
                        .filter(r -> "PENDING".equals(r.getStatus())).count();
                if (reportPendingLabel != null)
                    reportPendingLabel.setText(pending + " chờ xử lý");
            });
        } catch (Exception e) {
            setLoading("reports", false);
            System.out.println("[Admin] Lỗi tải reports: " + e.getMessage());
        }
    }

    private void resolveReport(AdminReport report) {
        executor.submit(() -> {
            try {
                String res = ServerConnection.getInstance().resolveReport(report.getId());
                boolean ok = res != null && res.contains("OK");
                javafx.application.Platform.runLater(() -> {
                    if (ok) {
                        report.setStatus("RESOLVED");
                        reportsTable.refresh();
                        cacheTime.remove("reports");
                        long pending = allReports.stream()
                                .filter(r -> "PENDING".equals(r.getStatus())).count();
                        if (reportPendingLabel != null)
                            reportPendingLabel.setText(pending + " chờ xử lý");
                        showToast("Đã xử lý báo cáo #" + report.getId(),
                                ToastNotification.ToastType.SUCCESS);
                    } else {
                        showToast("Không thể xử lý báo cáo này",
                                ToastNotification.ToastType.ERROR);
                    }
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() ->
                        showToast("Lỗi kết nối: " + e.getMessage(),
                                ToastNotification.ToastType.ERROR));
            }
        });
    }


    // CHART
    private void setupAdminChart() {
        adminRevenueChart.setLegendVisible(false);
        adminRevenueChart.setAnimated(false); // tắt animation để update nhanh hơn
    }

    private void updateRevenueChart(List<String[]> monthly) {
        adminRevenueChart.getData().clear();
        javafx.scene.chart.XYChart.Series<String, Number> series = new javafx.scene.chart.XYChart.Series<>();
        series.setName("Doanh thu (triệu ₫)");
        for (String[] entry : monthly) {
            double revInMillions = Double.parseDouble(entry[1]) / 1_000_000.0;
            series.getData().add(new javafx.scene.chart.XYChart.Data<>(entry[0], revInMillions));
        }
        adminRevenueChart.getData().add(series);
    }

    private String formatRevenue(double amount) {
        if (amount >= 1_000_000_000) return String.format("%.2f tỷ ₫", amount / 1_000_000_000);
        if (amount >= 1_000_000)     return String.format("%.1f tr ₫",  amount / 1_000_000);
        return String.format("%,.0f ₫", amount);
    }

    // ── Stats (Overview) ──────────────────────────────────────────
    private void loadStatsFromServer() {
        if (!isCacheStale("stats") || isLoading("stats")) return;
        executor.submit(this::fetchAndCacheStats);
    }

    private void fetchAndCacheStats() {
        if (!isCacheStale("stats") || !tryAcquireLoading("stats")) return;
        try {
            String raw      = ServerConnection.getInstance().getAdminStats();
            String jsonPart = raw.contains("===") ? raw.split("===", 2)[1] : "{}";
            if (jsonPart.isEmpty() || "{}".equals(jsonPart.trim())) {
                markCacheFresh("stats");
                setLoading("stats", false);
                return;
            }
            JsonObject obj = JsonParser.parseString(jsonPart).getAsJsonObject();

            double revenue  = obj.has("totalRevenue") ? obj.get("totalRevenue").getAsDouble() : 0;
            int paid = obj.has("paidCount") ? obj.get("paidCount").getAsInt() : 0;
            int unpaid = obj.has("unpaidCount") ? obj.get("unpaidCount").getAsInt() : 0;


            List<String[]> monthly = new ArrayList<>();
            if (obj.has("monthlyRevenue")) {
                for (JsonElement el : obj.get("monthlyRevenue").getAsJsonArray()) {
                    JsonObject mo = el.getAsJsonObject();
                    String label = mo.has("month") ? mo.get("month").getAsString() : "?";
                    String rev = mo.has("revenue") ? String.valueOf(mo.get("revenue").getAsDouble()) : "0";
                    monthly.add(new String[]{label, rev});
                }
            }

            markCacheFresh("stats");
            setLoading("stats", false);

            javafx.application.Platform.runLater(() -> {
                if (adminStatRevenue != null) adminStatRevenue.setText(formatRevenue(revenue));
                if (adminStatPaid != null) adminStatPaid.setText(String.valueOf(paid));
                if (adminStatUnpaid != null) adminStatUnpaid.setText(String.valueOf(unpaid));
                if (!monthly.isEmpty()) updateRevenueChart(monthly);
            });
        } catch (Exception e) {
            setLoading("stats", false);
            System.out.println("[Admin] Lỗi tải stats: " + e.getMessage());
        }
    }

    //  FILTER COMBOS
    private void initRoleFilter() {
        roleFilter.setItems(FXCollections.observableArrayList(
                "Tất cả", "BIDDER", "SELLER", "ADMIN"));
        roleFilter.setValue("Tất cả");
    }

    private void initAuctionStatusFilter() {
        auctionStatusFilter.setItems(FXCollections.observableArrayList(
                "Tất cả", "OPEN", "RUNNING", "FINISHED", "CANCELED"));
        auctionStatusFilter.setValue("Tất cả");
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
        target.setVisible(true);
        target.setManaged(true);
        sbtns.forEach(b -> {
            b.getStyleClass().removeAll("admin-sidebar-item-active");
            if (!b.getStyleClass().contains("admin-sidebar-item"))
                b.getStyleClass().add("admin-sidebar-item");
        });
        activeBtn.getStyleClass().add("admin-sidebar-item-active");
        adminPageTitle.setText(title);
    }

    // ── Các handler menu sidebar ────────────────────────────────
    @FXML void showOverview() {
        showPanel(adminPanelOverview, adminMenuOverview, "Dashboard");
        loadStatsFromServer(); // tải lazy + cache 60s
    }

    @FXML void showUsers() {
        showPanel(adminPanelUsers, adminMenuUsers, "Người dùng");
        loadUsersFromServer(); // tải lazy + cache 30s
    }

    @FXML void showAuctions() {
        showPanel(adminPanelAuctions, adminMenuAuctions, "Phiên đấu giá");
        loadAuctionsFromServer(); // tải lazy + cache 30s
    }

    @FXML void showBids() {
        showPanel(adminPanelBids, adminMenuBids, "Lịch sử đặt giá");
        loadBidsFromServer(); // tải lazy + cache 30s
    }

    @FXML void showReports() {
        showPanel(adminPanelReports, adminMenuReports, "Báo cáo vi phạm");
        loadReportsFromServer(); // tải lazy + cache 30s
    }

    @FXML void showSettings()  { showPanel(adminPanelSettings, adminMenuSettings, "Cài đặt"); }

    @FXML void handleSaveSettings() {
        showToast("Đã lưu cài đặt!", ToastNotification.ToastType.SUCCESS);
    }

    @FXML void handleLogout() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Đăng xuất");
        confirm.setHeaderText("Bạn có chắc muốn đăng xuất?");
        confirm.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    ServerConnection.getInstance().logout();
                    UserSession.getInstance().logout();
                    URL fxmlUrl = HomeController.class.getResource("/sample/home_demo.fxml");
                    if (fxmlUrl == null) fxmlUrl = HomeController.class.getResource("home.fxml");
                    if (fxmlUrl == null) {
                        new Alert(Alert.AlertType.ERROR, "Không tìm thấy home.fxml!").showAndWait();
                        return;
                    }
                    javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(fxmlUrl);
                    javafx.scene.Parent root = loader.load();
                    HomeController homeCtrl = loader.getController();
                    homeCtrl.resetToGuest();
                    javafx.stage.Stage stage = (javafx.stage.Stage) adminMenuOverview.getScene().getWindow();
                    stage.setScene(new javafx.scene.Scene(root, 1200, 800));
                    stage.setTitle("TINY HOARDER'S KEY MARKET");
                    stage.centerOnScreen();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    //NOTIFICATION BELL
    private void setupNotificationBadge() {
        notifBadgeAdmin = createBadgeLabel();
        if (bellStackAdmin != null) bellStackAdmin.getChildren().add(notifBadgeAdmin);
        notifBadgeAdmin.setVisible(false);
        NotificationManager.getInstance().addNotificationListener(() ->
                javafx.application.Platform.runLater(this::refreshBadge));
    }

    private void refreshBadge() {
        int count = NotificationManager.getInstance().getUnreadCount();
        notifBadgeAdmin.setText(count > 99 ? "99+" : String.valueOf(count));
        notifBadgeAdmin.setVisible(count > 0);
    }

    private Label createBadgeLabel() {
        Label badge = new Label();
        badge.setStyle("-fx-background-color:#e74c3c;-fx-text-fill:white;-fx-font-size:9;"
                + "-fx-font-weight:bold;-fx-background-radius:10;-fx-padding:1 4;"
                + "-fx-min-width:16;-fx-alignment:center;");
        StackPane.setAlignment(badge, Pos.TOP_RIGHT);
        StackPane.setMargin(badge, new Insets(-4, -4, 0, 0));
        return badge;
    }

    @FXML private void handleBellClick() {
        NotificationManager.getInstance().markAllRead();
        refreshBadge();

        List<NotificationManager.Notification> list = NotificationManager.getInstance().getAll();
        Popup popup = new Popup();
        popup.setAutoHide(true);
        VBox box = new VBox(0);
        box.setPrefWidth(340);
        box.setMaxHeight(420);
        box.setStyle("-fx-background-color:#1e2228;-fx-border-color:#444;-fx-border-width:1;"
                + "-fx-border-radius:10;-fx-background-radius:10;"
                + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.4),20,0,0,8);");

        Label header = new Label("🔔  Thông báo");
        header.setStyle("-fx-font-size:14;-fx-font-weight:bold;-fx-text-fill:#f0f0f0;"
                + "-fx-padding:14 16 12 16;-fx-border-color:#333;-fx-border-width:0 0 1 0;");
        header.setMaxWidth(Double.MAX_VALUE);
        box.getChildren().add(header);

        if (list.isEmpty()) {
            Label empty = new Label("Chưa có thông báo nào.");
            empty.setStyle("-fx-text-fill:#888;-fx-font-size:13;-fx-padding:20 16;");
            box.getChildren().add(empty);
        } else {
            ScrollPane scroll = new ScrollPane();
            scroll.setFitToWidth(true);
            scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            scroll.setPrefViewportHeight(Math.min(list.size() * 68.0, 360));
            scroll.setStyle("-fx-background-color:transparent;-fx-background:transparent;");
            VBox items = new VBox(0);
            List<NotificationManager.Notification> reversed = new ArrayList<>(list);
            java.util.Collections.reverse(reversed);
            java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss dd/MM");
            for (int i = 0; i < reversed.size(); i++) {
                NotificationManager.Notification notif = reversed.get(i);
                VBox row = new VBox(3);
                row.setPadding(new Insets(10, 16, 10, 16));
                row.setStyle(i % 2 == 0 ? "-fx-background-color:#1e2228;" : "-fx-background-color:#252930;");
                Label msgLabel = new Label(notif.message);
                msgLabel.setWrapText(true);
                msgLabel.setStyle("-fx-font-size:13;-fx-text-fill:#e0e0e0;");
                java.time.LocalDateTime ldt = java.time.Instant.ofEpochMilli(notif.timestamp)
                        .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
                Label timeLabel = new Label(fmt.format(ldt));
                timeLabel.setStyle("-fx-font-size:11;-fx-text-fill:#888;");
                row.getChildren().addAll(msgLabel, timeLabel);
                items.getChildren().add(row);
            }
            scroll.setContent(items);
            box.getChildren().add(scroll);
        }
        popup.getContent().add(box);
        javafx.geometry.Bounds bounds = bellStackAdmin.localToScreen(bellStackAdmin.getBoundsInLocal());
        popup.show(bellStackAdmin.getScene().getWindow(),
                bounds.getMaxX() - 340, bounds.getMaxY() + 4);
    }

    // HELPERS
    private void showToast(String message) {
        showToast(message, ToastNotification.ToastType.INFO);
    }

    private void showToast(String message, ToastNotification.ToastType type) {
        javafx.stage.Window win = getWindow();
        if (win != null) {
            ToastNotification.show(win, message, type);
        }
    }

    private javafx.stage.Window getWindow() {
        // Lấy Stage từ bất kỳ node nào đang hiển thị
        if (adminMenuOverview != null && adminMenuOverview.getScene() != null)
            return adminMenuOverview.getScene().getWindow();
        return javafx.stage.Stage.getWindows().stream()
                .filter(javafx.stage.Window::isShowing)
                .findFirst().orElse(null);
    }

    private String roleBadgeStyle(String role) {
        return switch (role) {
            case "BIDDER" -> "-fx-background-color:#E6F1FB;-fx-text-fill:#185FA5;"
                    + "-fx-background-radius:10;-fx-padding:2 8;-fx-font-size:11;";
            case "SELLER" -> "-fx-background-color:#EAF3DE;-fx-text-fill:#3B6D11;"
                    + "-fx-background-radius:10;-fx-padding:2 8;-fx-font-size:11;";
            case "ADMIN"  -> "-fx-background-color:#EEEDFE;-fx-text-fill:#534AB7;"
                    + "-fx-background-radius:10;-fx-padding:2 8;-fx-font-size:11;";
            default       -> "";
        };
    }

    private String translateAuctionStatus(String s) {
        return switch (s.toUpperCase()) {
            case "OPEN"     -> "Chờ mở";
            case "RUNNING"  -> "Đang chạy";
            case "FINISHED" -> "Kết thúc";
            case "CANCELED" -> "Đã hủy";
            default         -> s;
        };
    }

    private String auctionPillStyle(String s) {
        return switch (s.toUpperCase()) {
            case "RUNNING"  -> "-fx-background-color:#FDECEA;-fx-text-fill:#A32D2D;"
                    + "-fx-background-radius:10;-fx-padding:2 8;-fx-font-size:11;";
            case "FINISHED" -> "-fx-background-color:#EAF3DE;-fx-text-fill:#3B6D11;"
                    + "-fx-background-radius:10;-fx-padding:2 8;-fx-font-size:11;";
            case "OPEN"     -> "-fx-background-color:#E6F1FB;-fx-text-fill:#185FA5;"
                    + "-fx-background-radius:10;-fx-padding:2 8;-fx-font-size:11;";
            case "CANCELED" -> "-fx-background-color:#F0F0F0;-fx-text-fill:#888;"
                    + "-fx-background-radius:10;-fx-padding:2 8;-fx-font-size:11;";
            default         -> "";
        };
    }
}