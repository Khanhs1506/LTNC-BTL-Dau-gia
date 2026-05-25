package sample;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HomeController {

    // ===== Singleton =====
    private static HomeController instance;
    public static HomeController getInstance() { return instance; }

    // ===== FXML fields =====
    @FXML private FlowPane  flowPane;
    @FXML private TextField txtSearch;
    @FXML private HBox      guestBox;
    @FXML private HBox      userBox;
    @FXML private Label     lblUsername;
    @FXML private Button    btnKhac;
    @FXML private Button    btnTienSanh;
    @FXML private Button    btnBienSoXe;
    @FXML private Button    btnBatDongSan;
    @FXML private BorderPane rootPane;
    @FXML private Button btnBellGuest;
    @FXML private Button btnBellUser;
    @FXML private StackPane bellStackGuest;
    @FXML private StackPane bellStackUser;
    @FXML private Label     lblBalance;
    @FXML private Button btnWallet;

    @FXML private Button btnNgheThuat;
    @FXML private Button btnPhuongTien;
    @FXML private Button btnDienTu;


    private Label badgeGuest;
    private Label badgeUser;
    private ContextMenu khacMenu;

    // ===== State =====
    private String currentCategory = "Tất cả"; // danh mục đang lọc
    private final List<Button> allBidButtons = new ArrayList<>(); // để đổi text khi login/logout

    public Parent getRoot() {
        return rootPane;
    }

    // ===== Model =====
    static class AuctionItem {
        int auctionId;
        String title, giaKhoiDiem, giaCaoNhat, thoiGian, hanDangKi;
        int thauThu;
        String category;    // dùng để lọc
        boolean favorited = false;
        java.time.LocalDateTime endTimeRaw;

        AuctionItem(String title, String giaKhoiDiem, String giaCaoNhat,
                    String thoiGian, int thauThu, String hanDangKi, String category) {
            this.title       = title;
            this.giaKhoiDiem = giaKhoiDiem;
            this.giaCaoNhat  = giaCaoNhat;
            this.thoiGian    = thoiGian;
            this.thauThu     = thauThu;
            this.hanDangKi   = hanDangKi;
            this.category    = category;
        }
    }

    // ===== Toàn bộ dữ liệu (không thay đổi khi lọc) =====
    private List<AuctionItem> allItems = new ArrayList<>();

    // ===== Initialize =====
    @FXML
    public void initialize() {
        instance = this;

        guestBox.setVisible(true);
        guestBox.setManaged(true);
        userBox.setVisible(false);
        userBox.setManaged(false);

        buildKhacMenu();

        //LẤY DỮ LIỆU TỪ SERVER
        loadFromServer();
        setupNotificationBadge();

//        // ── Dữ liệu mẫu có category ──────────────────────────
//        allItems = Arrays.asList(
//                new AuctionItem("G.5 - BKS 30K - 888.88",
//                        "120.000.000 VNĐ", "215.000.000 VNĐ", "22:01:45", 36, "23/12/2026",
//                        "Biển số xe"),
//                new AuctionItem("H.5 - BKS 51K - 777.77",
//                        "80.000.000 VNĐ", "150.000.000 VNĐ", "18:30:00", 12, "15/11/2026",
//                        "Biển số xe"),
//                new AuctionItem("Biệt thự Hồ Tây",
//                        "5.000.000.000 VNĐ", "6.200.000.000 VNĐ", "10:00:00", 8, "01/08/2026",
//                        "Bất động sản"),
//                new AuctionItem("Căn hộ Quận 1 - Tầng 15",
//                        "3.500.000.000 VNĐ", "4.100.000.000 VNĐ", "14:30:00", 5, "20/07/2026",
//                        "Bất động sản"),
//                new AuctionItem("Tranh sơn dầu - Bùi Xuân Phái",
//                        "200.000.000 VNĐ", "380.000.000 VNĐ", "09:15:00", 19, "10/06/2026",
//                        "Nghệ thuật"),
//                new AuctionItem("Tượng đồng cổ - Thế kỷ 18",
//                        "450.000.000 VNĐ", "520.000.000 VNĐ", "16:00:00", 7, "25/06/2026",
//                        "Nghệ thuật"),
//                new AuctionItem("Mercedes-Benz S500 2020",
//                        "2.800.000.000 VNĐ", "3.100.000.000 VNĐ", "11:00:00", 14, "05/07/2026",
//                        "Xe cộ"),
//                new AuctionItem("Porsche 911 GT3 2022",
//                        "6.500.000.000 VNĐ", "7.200.000.000 VNĐ", "17:45:00", 22, "30/06/2026",
//                        "Xe cộ"),
//                new AuctionItem("Rolex Submariner Date",
//                        "180.000.000 VNĐ", "245.000.000 VNĐ", "13:20:00", 31, "18/06/2026",
//                        "Đồng hồ"),
//                new AuctionItem("Patek Philippe Nautilus",
//                        "950.000.000 VNĐ", "1.200.000.000 VNĐ", "20:00:00", 11, "12/07/2026",
//                        "Đồng hồ")
//        );
//
//        // Thêm dữ liệu random từ factory nếu cần 100 items
//        // allItems.addAll(AuctionDataFactory.generate(90));
//
//        renderCards(currentCategory);
    }

    private void loadFromServer() {
        new Thread(() -> {
            try {
                String response = ServerConnection.getInstance().getAuctions();

                if (response != null && response.startsWith("AUCTIONS===")) {
                    String json = response.substring("AUCTIONS===".length());
                    JsonArray arr = JsonParser.parseString(json).getAsJsonArray();
                    List<AuctionItem> loaded = new ArrayList<>();
                    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

                    for (JsonElement el : arr) {
                        JsonObject obj = el.getAsJsonObject();

                        String status = obj.get("status").getAsString();
                        if (!status.equals("RUNNING") && !status.equals("OPEN")) continue;

                        String name       = obj.get("itemName").getAsString();
                        double startPrice = obj.get("startingPrice").getAsDouble();
                        double highBid    = obj.get("currentHighestBid").getAsDouble();
                        String itemType   = obj.get("itemType").getAsString();
                        String endTimeStr = obj.get("endTime").getAsString();

                        LocalDateTime endTime   = LocalDateTime.parse(endTimeStr, fmt);
                        String timeLeft         = computeTimeLeft(endTime);
                        String endDateFormatted = endTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                        String category         = mapItemTypeToCategory(itemType);
                        int auctionId = obj.has("auctionId") ? obj.get("auctionId").getAsInt() : 0;
                        AuctionItem ai = new AuctionItem(
                                name, formatVND(startPrice), formatVND(highBid),
                                timeLeft, 0, endDateFormatted, category
                        );
                        ai.auctionId = auctionId;
                        ai.endTimeRaw = endTime; // ← thêm dòng này
                        loaded.add(ai);
                    }

                    // ✅ runLater ở NGOÀI for — đợi load xong hết rồi mới render 1 lần
                    Platform.runLater(() -> {
                        allItems = loaded.isEmpty() ? buildMockItems() : loaded;
                        renderCards(currentCategory);
                    });

                } else {
                    Platform.runLater(() -> {
                        allItems = buildMockItems();
                        renderCards(currentCategory);
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    allItems = buildMockItems();
                    renderCards(currentCategory);
                });
            }
        }, "HomeLoader").start();
    }

    private String formatVND(double amount) {
        return String.format("%,.0f VNĐ", amount);
    }

    //TÍNH THỜI GIAN CÒN LẠI
    private String computeTimeLeft(LocalDateTime endTime) {
        Duration d = Duration.between(LocalDateTime.now(), endTime);
        if (d.isNegative()) return "Đã kết thúc";
        long hours   = d.toHours();
        long minutes = d.toMinutesPart();
        long seconds = d.toSecondsPart();
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    //PHÂN LOẠI SẢN PHẨM
    private String mapItemTypeToCategory(String itemType) {
        if (itemType == null) return "Khác";
        return switch (itemType) {
            // Server trả về dạng viết hoa
            case "ART"              -> "Nghệ thuật";
            case "ELECTRONICS"      -> "Điện tử";
            case "VEHICLE"          -> "Phương tiện";
            // Client gửi lên dạng key
            case "ArtItem"          -> "Nghệ thuật";
            case "ElectronicsItem"  -> "Điện tử";
            case "VehicleItem"      -> "Phương tiện";
            // Các danh mục từ menu Khác — giữ nguyên tiếng Việt
            case "Nội thất",
                 "Bất động sản",
                 "Vé sự kiện",
                 "Trò chơi điện tử",
                 "Thể thao",
                 "Sách",
                 "Thời trang"       -> itemType;
            default                 -> "Khác";
        };
    }

    /**
     * Dữ liệu mẫu – chỉ dùng khi server chưa khởi động (chạy offline).
     * Có thể xóa khi ứng dụng hoàn thiện.
     */
    private List<AuctionItem> buildMockItems() {
        return Arrays.asList(
                new AuctionItem("G.5 - BKS 30K - 888.88",
                        "120.000.000 VNĐ", "215.000.000 VNĐ",
                        "22:01:45", 36, "23/12/2026", "Biển số xe"),
                new AuctionItem("H.5 - BKS 51K - 777.77",
                        "80.000.000 VNĐ", "150.000.000 VNĐ",
                        "18:30:00", 12, "15/11/2026", "Biển số xe"),
                new AuctionItem("Biệt thự Hồ Tây",
                        "5.000.000.000 VNĐ", "6.200.000.000 VNĐ",
                        "10:00:00", 8, "01/08/2026", "Bất động sản"),
                new AuctionItem("Tranh sơn dầu - Bùi Xuân Phái",
                        "200.000.000 VNĐ", "380.000.000 VNĐ",
                        "09:15:00", 19, "10/06/2026", "Nghệ thuật"),
                new AuctionItem("Mercedes-Benz S500 2020",
                        "2.800.000.000 VNĐ", "3.100.000.000 VNĐ",
                        "11:00:00", 14, "05/07/2026", "Xe cộ"),
                new AuctionItem("Rolex Submariner Date",
                        "180.000.000 VNĐ", "245.000.000 VNĐ",
                        "13:20:00", 31, "18/06/2026", "Khác")
        );
    }


    private void setupNotificationBadge() {
        //TẠO CHUÔNG
        badgeGuest = createBadgeLabel();
        badgeUser  = createBadgeLabel();

        if (bellStackGuest != null) bellStackGuest.getChildren().add(badgeGuest);
        if (bellStackUser  != null) bellStackUser .getChildren().add(badgeUser);

        badgeGuest.setVisible(false);
        badgeUser .setVisible(false);

        //TẠO THÔNG BÁO
        NotificationManager.getInstance().addNotificationListener(() ->
                javafx.application.Platform.runLater(this::refreshBadge)
        );
    }
    //HIỆN SỐ LƯỢNG THÔNG BÁO
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
        //HIỆN TRÊN GÓC PHẢI
        javafx.scene.layout.StackPane.setAlignment(badge, javafx.geometry.Pos.TOP_RIGHT);
        javafx.scene.layout.StackPane.setMargin(badge, new Insets(-4, -4, 0, 0));
        return badge;
    }

    //CẬP NHẬT SỐ LƯỢNG THÔNG BÁO LIÊN TỤC
    private void refreshBadge() {
        int count = NotificationManager.getInstance().getUnreadCount();
        String text = count > 99 ? "99+" : String.valueOf(count);
        boolean show = count > 0;

        badgeGuest.setText(text);
        badgeUser .setText(text);
        badgeGuest.setVisible(show);
        badgeUser .setVisible(show);
    }

    //XỬ LÍ BẤM CHUÔNG HIỆN DANH SÁCH THÔNG BÁO
    @FXML
    private void handleBellClick() {
        //ĐÁNH DẤU ĐÃ ĐỌC
        NotificationManager.getInstance().markAllRead();
        refreshBadge();

        //LẤY DANH SÁCH THÔNG BÁO
        List<NotificationManager.Notification> list =
                NotificationManager.getInstance().getAll();

        //HIỆN THỊ LÊN TRÊN CÙNG
        javafx.stage.Popup popup = new javafx.stage.Popup();
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


        Label header = new Label("🔔  Thông báo");
        header.setStyle(
                "-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #222;" +
                        "-fx-padding: 14 16 12 16;" +
                        "-fx-border-color: #eee; -fx-border-width: 0 0 1 0;"
        );
        header.setMaxWidth(Double.MAX_VALUE);
        box.getChildren().add(header);

        //HIỆN THỊ DANH SÁCH THÔNG BÁO
        if (list.isEmpty()) {
            Label empty = new Label("Chưa có thông báo nào.");
            empty.setStyle(
                    "-fx-text-fill: #999; -fx-font-size: 13;" +
                            "-fx-padding: 20 16;"
            );
            box.getChildren().add(empty);
        } else {
            ScrollPane scroll = new ScrollPane();
            scroll.setFitToWidth(true);
            scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            scroll.setPrefViewportHeight(Math.min(list.size() * 68.0, 340));
            scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

            VBox items = new VBox(0);
            //HIỆN THỊ THEO THỜI GIAN MỚI NHẤT TRƯỚC
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
                        : "-fx-background-color: #fafafa;"
                );

                Label msgLabel = new Label(notif.message);
                msgLabel.setWrapText(true);
                msgLabel.setStyle("-fx-font-size: 13; -fx-text-fill: #222;");

                java.time.LocalDateTime ldt = java.time.LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli(notif.timestamp),
                        java.time.ZoneId.systemDefault()
                );
                Label timeLabel = new Label(fmt.format(ldt));
                timeLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #999;");

                javafx.scene.control.Separator sep = new javafx.scene.control.Separator();
                sep.setStyle("-fx-opacity: 0.4;");

                row.getChildren().addAll(msgLabel, timeLabel, sep);
                items.getChildren().add(row);
            }

            scroll.setContent(items);
            box.getChildren().add(scroll);
        }

        popup.getContent().add(box);

        Button sourceBell = (userBox.isVisible() && userBox.isManaged())
                ? btnBellUser : btnBellGuest;
        if (sourceBell == null) return;

        javafx.geometry.Bounds b = sourceBell.localToScreen(
                sourceBell.getBoundsInLocal());
        popup.show(sourceBell.getScene().getWindow(),
                b.getMinX() - 260 + sourceBell.getWidth(),
                b.getMaxY() + 6);
    }

    // ===== Render cards theo danh mục =====
    private void renderCards(String category) {
        flowPane.getChildren().clear();
        allBidButtons.clear();

        for (AuctionItem item : allItems) {
            boolean match = category.equals("Tất cả") || item.category.equals(category);
            if (match) {
                flowPane.getChildren().add(createCard(item));
            }
        }
        highlightCategoryButton(category);
    }

    // ===== Highlight nút danh mục đang chọn =====
    private void highlightCategoryButton(String category) {
        String normal   = "-fx-background-color: transparent; -fx-text-fill: #444;" +
                "-fx-font-size: 13; -fx-font-weight: bold;" +
                "-fx-padding: 7 20; -fx-cursor: hand; -fx-background-radius: 20;";
        String selected = "-fx-background-color: #c0392b; -fx-text-fill: white;" +
                "-fx-font-size: 13; -fx-font-weight: bold;" +
                "-fx-padding: 7 20; -fx-cursor: hand; -fx-background-radius: 20;";

        // ✅ null-check: tránh NPE nếu fx:id chưa được gán
        if (btnTienSanh   != null)
            btnTienSanh.setStyle(category.equals("Tất cả")         ? selected : normal);
        if (btnNgheThuat  != null)
            btnNgheThuat.setStyle(category.equals("Nghệ thuật")  ? selected : normal);
        if (btnPhuongTien != null)
            btnPhuongTien.setStyle(category.equals("Phương tiện") ? selected : normal);
        if (btnDienTu     != null)
            btnDienTu.setStyle(category.equals("Điện tử")        ? selected : normal);
        if (btnKhac       != null)
            btnKhac.setStyle(
                    (!category.equals("Tất cả") && !category.equals("Nghệ thuật")
                            && !category.equals("Phương tiện") && !category.equals("Điện tử"))
                            ? selected : normal);
        if (btnBienSoXe   != null)
            btnBienSoXe.setStyle(category.equals("Biển số xe")     ? selected : normal);
        if (btnBatDongSan != null)
            btnBatDongSan.setStyle(category.equals("Bất động sản") ? selected : normal);
    }

    // ===== Xử lý nút menu =====
    @FXML private void handleTienSanh()    { currentCategory = "Tất cả";        renderCards(currentCategory); }
    @FXML private void handleBienSoXe()   { currentCategory = "Biển số xe";     renderCards(currentCategory); }
    @FXML private void handleBatDongSan() { currentCategory = "Bất động sản";   renderCards(currentCategory); }
    @FXML private void handleNgheThuat()  { currentCategory = "Nghệ thuật";  renderCards(currentCategory); }
    @FXML private void handlePhuongTien() { currentCategory = "Phương tiện"; renderCards(currentCategory); }
    @FXML private void handleDienTu()     { currentCategory = "Điện tử";     renderCards(currentCategory); }

    // ===== Menu Khác =====
    private void buildKhacMenu() {
        khacMenu = new ContextMenu();

        // Danh mục có trong dữ liệu sẽ lọc được
        String[] categories = {
                "Nội thất", "Bất động sản", "Vé sự kiện",
                "Trò chơi điện tử", "Thể thao", "Sách", "Thời trang"
        };

        for (String cat : categories) {
            MenuItem menuItem = new MenuItem(cat);
            menuItem.setStyle("-fx-font-size: 13px; -fx-padding: 6 16;");
            menuItem.setOnAction(e -> {
                currentCategory = cat;
                renderCards(cat);
            });
            khacMenu.getItems().add(menuItem);
        }

        khacMenu.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: #e8e0d0;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 8;" +
                        "-fx-background-radius: 8;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 12, 0, 0, 4);"
        );
    }

    @FXML
    private void handleKhacMenu(ActionEvent event) {
        khacMenu.show(btnKhac, Side.BOTTOM, 0, 6);
    }

    // ===== Login / Logout =====
    public void onLoginSuccess(String username) {
        guestBox.setVisible(false);
        guestBox.setManaged(false);
        lblUsername.setText(username);
        userBox.setVisible(true);
        userBox.setManaged(true);

        // Hiện số dư ví (chỉ Bidder mới có ví)
        refreshBalanceLabel();

        for (Button btn : allBidButtons) {
            btn.setText("Đấu giá");
        }
    }

    @FXML
    private void handleLogout() {
        UserSession.getInstance().logout();
        userBox.setVisible(false);
        userBox.setManaged(false);
        guestBox.setVisible(true);
        guestBox.setManaged(true);

        if (lblBalance != null) lblBalance.setText("");

        for (Button btn : allBidButtons) {
            btn.setText("Đăng kí đấu giá");
        }
    }

    // ===== Mở cửa sổ =====
    private void openWindow(String fxmlPath, String title, double width, double height) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.initStyle(StageStyle.UNDECORATED);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root, width, height));
            stage.setTitle(title);
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML private void handleDangNhap() { openWindow("/sample/login.fxml",    "Đăng Nhập", 551, 400); }
    @FXML private void handleDangKi()   { openWindow("/sample/register.fxml", "Đăng Ký",   520, 600); }

    @FXML
    private void handleSearch() {
        String keyword = txtSearch.getText().trim().toLowerCase();
        if (keyword.isEmpty()) { renderCards(currentCategory); return; }

        flowPane.getChildren().clear();
        allBidButtons.clear();
        for (AuctionItem item : allItems) {
            if (item.title.toLowerCase().contains(keyword) ||
                    item.category.toLowerCase().contains(keyword)) {
                flowPane.getChildren().add(createCard(item));
            }
        }
    }

    // ===== Mở AuctionDetail =====
    private void openAuctionDetail(AuctionItem item) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/sample/AuctionDetail.fxml"));
            Parent root = loader.load();

            // ✅ Map đầy đủ dữ liệu từ AuctionItem → AuctionItemDTO
            AuctionItemDTO dto = new AuctionItemDTO();
            dto.title          = item.title;
            dto.sellerUsername = "";
            dto.startingPrice  = parseAmount(item.giaKhoiDiem);
            dto.currentHighest = parseAmount(item.giaCaoNhat);
            dto.stepPrice      = 500_000;
            dto.totalBids      = item.thauThu;

            // Parse endTime từ hanDangKi (dd/MM/yyyy)
            if (item.endTimeRaw != null) {
                dto.endTime = item.endTimeRaw; // ← dùng endTime thực từ server
            } else {
                try {
                    java.time.format.DateTimeFormatter fmt =
                            java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
                    java.time.LocalDate date = java.time.LocalDate.parse(item.hanDangKi, fmt);
                    dto.endTime = date.atTime(23, 59, 59);
                } catch (Exception ex) {
                    dto.endTime = java.time.LocalDateTime.now().plusDays(1);
                }
            }

            AuctionDetailController ctrl = loader.getController();
            ctrl.setAuction(dto);

            Stage stage = new Stage();
            stage.initStyle(StageStyle.UNDECORATED);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root, 720, 780));
            stage.setResizable(true);
            stage.setMinWidth(680);
            stage.setMinHeight(700);
            stage.setOnCloseRequest(ev -> ctrl.stopCountdown());
            stage.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ===== Tạo Card =====
    private VBox createCard(AuctionItem item) {
        VBox card = new VBox(12);
        card.setPrefWidth(340);
        card.setPadding(new Insets(20));
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 15;" +
                        "-fx-border-radius: 15;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 14, 0, 0, 4);"
        );

        // ── Tiêu đề + Nút Tim ─────────────────────────────────
        HBox titleRow = new HBox();
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label lblTitle = new Label(item.title);
        lblTitle.setStyle("-fx-font-size: 15; -fx-font-weight: bold; -fx-text-fill: #222;");
        lblTitle.setWrapText(true);
        HBox.setHgrow(lblTitle, Priority.ALWAYS);

        // ✅ Nút tim toggle
        Button btnHeart = new Button(item.favorited ? "♥" : "♡");
        btnHeart.setStyle(heartStyle(item.favorited));
        btnHeart.setOnAction(e -> {
            item.favorited = !item.favorited;          // toggle trạng thái
            btnHeart.setText(item.favorited ? "♥" : "♡");
            btnHeart.setStyle(heartStyle(item.favorited));
        });

        titleRow.getChildren().addAll(lblTitle, btnHeart);

        // ── Grid thông tin ────────────────────────────────────
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(9);

        ColumnConstraints cc0 = new ColumnConstraints();
        cc0.setHgrow(Priority.ALWAYS);
        ColumnConstraints cc1 = new ColumnConstraints();
        cc1.setHgrow(Priority.NEVER);
        grid.getColumnConstraints().addAll(cc0, cc1);

        addRow(grid, 0, "Giá khởi điểm:",          item.giaKhoiDiem,             false);
        addRow(grid, 1, "*Giá cao nhất hiện tại:",  item.giaCaoNhat,              true);
        addRow(grid, 2, "Thời gian:",               item.thoiGian,                false);
        addRow(grid, 3, "Thầu thứ:",                String.valueOf(item.thauThu), false);
        addRow(grid, 4, "Hạn đăng kí đến:",         item.hanDangKi,              false);

        // ── Nút đấu giá ───────────────────────────────────────
        boolean loggedIn = UserSession.getInstance().isLoggedIn();
        Button btnBid = new Button(loggedIn ? "Đấu giá" : "Đăng kí đấu giá");
        btnBid.setMaxWidth(Double.MAX_VALUE);
        btnBid.setStyle(
                "-fx-background-color: #c0392b; -fx-text-fill: white;" +
                        "-fx-font-size: 14; -fx-font-weight: bold;" +
                        "-fx-padding: 12; -fx-background-radius: 8; -fx-cursor: hand;"
        );
        btnBid.setOnAction(e -> {
            if (!UserSession.getInstance().isLoggedIn()) {
                handleDangNhap();
            } else {
                openAuctionDetail(item); // Đã đăng nhập → mở thẳng giao diện đấu giá
            }
        });

        // ✅ Lưu vào danh sách để đổi text khi login/logout
        allBidButtons.add(btnBid);

        // ── Link xem chi tiết ────────────────────────────────
        Label lblLink = new Label("Xem chi tiết & Lịch sử");
        lblLink.setStyle("-fx-text-fill: #e05252; -fx-cursor: hand; -fx-font-size: 13;");
        lblLink.setOnMouseClicked(e -> openAuctionDetail(item));

        VBox linkBox = new VBox(lblLink);
        linkBox.setAlignment(Pos.CENTER);

        card.getChildren().addAll(titleRow, new Separator(), grid, btnBid, linkBox);
        return card;
    }

    // ── Kiểu nút tim ──────────────────────────────────────────
    private String heartStyle(boolean favorited) {
        return favorited
                ? "-fx-background-color: transparent; -fx-text-fill: #e05252;" +
                  "-fx-font-size: 18; -fx-cursor: hand; -fx-padding: 0 0 0 8;"
                : "-fx-background-color: transparent; -fx-text-fill: #222;" +
                  "-fx-font-size: 18; -fx-cursor: hand; -fx-padding: 0 0 0 8;";
    }

    private void addRow(GridPane grid, int row,
                        String labelText, String valueText, boolean highlight) {
        Label lbl = new Label(labelText);
        Label val = new Label(valueText);

        if (highlight) {
            lbl.setStyle("-fx-text-fill: #e05252; -fx-font-weight: bold; -fx-font-size: 13;");
            val.setStyle("-fx-text-fill: #e05252; -fx-font-weight: bold; -fx-font-size: 14;");
        } else {
            lbl.setStyle("-fx-text-fill: #555555; -fx-font-size: 13;");
            val.setStyle("-fx-text-fill: #111111; -fx-font-weight: bold; -fx-font-size: 13;");
        }
        grid.add(lbl, 0, row);
        grid.add(val, 1, row);
    }

    /**
     * Gọi từ bên ngoài (vd: SellerDashboard logout) để reset Home
     * về trạng thái khách chưa đăng nhập.
     */
    public void resetToGuest() {
        // Reset session (phòng trường hợp chưa clear)
        UserSession.getInstance().logout();

        // Ẩn userBox, hiện guestBox
        userBox.setVisible(false);
        userBox.setManaged(false);
        guestBox.setVisible(true);
        guestBox.setManaged(true);

        // Đổi tất cả nút "Đấu giá" → "Đăng kí đấu giá"
        for (Button btn : allBidButtons) {
            btn.setText("Đăng kí đấu giá");
        }

        // Reset về tab Tất cả
        currentCategory = "Tất cả";
        renderCards(currentCategory);
    }

    /**
     * Cập nhật nhãn số dư ví — gọi sau mỗi lần balance thay đổi.
     * Chỉ hiển thị khi role là BIDDER; ẩn với SELLER / ADMIN.
     */
    public void refreshBalanceLabel() {
        if (lblBalance == null) return;

        UserSession session = UserSession.getInstance();
        boolean isBidder = "BIDDER".equalsIgnoreCase(session.getRole());

        if (isBidder) {
            String formatted = String.format("💰 Số dư: %,.0f VNĐ", session.getBalance());
            lblBalance.setText(formatted);
            lblBalance.setVisible(true);
            lblBalance.setManaged(true);
        } else {
            lblBalance.setText("");
            lblBalance.setVisible(false);
            lblBalance.setManaged(false);
        }
    }

    @FXML
    private void handleOpenWallet() {
        if (!"BIDDER".equalsIgnoreCase(UserSession.getInstance().getRole())) {
            ToastNotification.warning(
                    rootPane.getScene().getWindow(),
                    "Chức năng ví chỉ dành cho Bidder");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/sample/wallet.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Ví của tôi");
            stage.setScene(new Scene(root, 920, 700));
            stage.initModality(Modality.APPLICATION_MODAL);
            // Sau khi đóng ví, làm mới số dư trên navbar
            stage.setOnHidden(e -> refreshBalanceLabel());
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private double parseAmount(String formatted) {
        if (formatted == null) return 0;
        try {
            return Double.parseDouble(
                    formatted.replace(".", "")
                            .replace(",", "")
                            .replace(" VND", "")
                            .replace(" VNĐ", "")
                            .trim()
            );
        } catch (Exception e) {
            return 0;
        }
    }

}