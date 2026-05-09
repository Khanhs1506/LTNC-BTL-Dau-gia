package sample;

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

    private ContextMenu khacMenu;

    // ===== State =====
    private String currentCategory = "Tất cả"; // danh mục đang lọc
    private final List<Button> allBidButtons = new ArrayList<>(); // để đổi text khi login/logout

    // ===== Model =====
    static class AuctionItem {
        String title, giaKhoiDiem, giaCaoNhat, thoiGian, hanDangKi;
        int thauThu;
        String category;    // dùng để lọc
        boolean favorited = false;

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

        // ── Dữ liệu mẫu có category ──────────────────────────
        allItems = Arrays.asList(
                new AuctionItem("G.5 - BKS 30K - 888.88",
                        "120.000.000 VNĐ", "215.000.000 VNĐ", "22:01:45", 36, "23/12/2026",
                        "Biển số xe"),
                new AuctionItem("H.5 - BKS 51K - 777.77",
                        "80.000.000 VNĐ", "150.000.000 VNĐ", "18:30:00", 12, "15/11/2026",
                        "Biển số xe"),
                new AuctionItem("Biệt thự Hồ Tây",
                        "5.000.000.000 VNĐ", "6.200.000.000 VNĐ", "10:00:00", 8, "01/08/2026",
                        "Bất động sản"),
                new AuctionItem("Căn hộ Quận 1 - Tầng 15",
                        "3.500.000.000 VNĐ", "4.100.000.000 VNĐ", "14:30:00", 5, "20/07/2026",
                        "Bất động sản"),
                new AuctionItem("Tranh sơn dầu - Bùi Xuân Phái",
                        "200.000.000 VNĐ", "380.000.000 VNĐ", "09:15:00", 19, "10/06/2026",
                        "Nghệ thuật"),
                new AuctionItem("Tượng đồng cổ - Thế kỷ 18",
                        "450.000.000 VNĐ", "520.000.000 VNĐ", "16:00:00", 7, "25/06/2026",
                        "Nghệ thuật"),
                new AuctionItem("Mercedes-Benz S500 2020",
                        "2.800.000.000 VNĐ", "3.100.000.000 VNĐ", "11:00:00", 14, "05/07/2026",
                        "Xe cộ"),
                new AuctionItem("Porsche 911 GT3 2022",
                        "6.500.000.000 VNĐ", "7.200.000.000 VNĐ", "17:45:00", 22, "30/06/2026",
                        "Xe cộ"),
                new AuctionItem("Rolex Submariner Date",
                        "180.000.000 VNĐ", "245.000.000 VNĐ", "13:20:00", 31, "18/06/2026",
                        "Đồng hồ"),
                new AuctionItem("Patek Philippe Nautilus",
                        "950.000.000 VNĐ", "1.200.000.000 VNĐ", "20:00:00", 11, "12/07/2026",
                        "Đồng hồ")
        );

        // Thêm dữ liệu random từ factory nếu cần 100 items
        // allItems.addAll(AuctionDataFactory.generate(90));

        renderCards(currentCategory);
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
        if (btnBienSoXe   != null)
            btnBienSoXe.setStyle(category.equals("Biển số xe")     ? selected : normal);
        if (btnBatDongSan != null)
            btnBatDongSan.setStyle(category.equals("Bất động sản") ? selected : normal);
    }

    // ===== Xử lý nút menu =====
    @FXML private void handleTienSanh()    { currentCategory = "Tất cả";        renderCards(currentCategory); }
    @FXML private void handleBienSoXe()   { currentCategory = "Biển số xe";     renderCards(currentCategory); }
    @FXML private void handleBatDongSan() { currentCategory = "Bất động sản";   renderCards(currentCategory); }

    // ===== Menu Khác =====
    private void buildKhacMenu() {
        khacMenu = new ContextMenu();

        // Danh mục có trong dữ liệu sẽ lọc được
        String[] categories = {
                "Nghệ thuật", "Đồng hồ", "Trang sức",
                "Xe cộ", "Túi xách", "Thời trang",
                "Thiết bị điện tử", "Điện thoại", "Máy tính",
                "Nội thất", "Sách và tài liệu sưu tầm",
                "Đồ lưu niệm", "Nhạc cụ", "Đồ chơi sưu tầm",
                "Thẻ bài / mô hình", "Tiền xu / tem",
                "Vật phẩm game", "NFT / tài sản số",
                "Đồ gia dụng", "Máy móc", "Vé sự kiện"
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

        // ✅ Đổi tất cả nút "Đăng kí đấu giá" → "Đấu giá"
        for (Button btn : allBidButtons) {
            btn.setText("Đấu giá");
        }
    }

    @FXML
    private void handleDangXuat() {
        UserSession.getInstance().logout();
        userBox.setVisible(false);
        userBox.setManaged(false);
        guestBox.setVisible(true);
        guestBox.setManaged(true);

        // ✅ Đổi lại "Đăng kí đấu giá"
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

            AuctionItemDTO dto = new AuctionItemDTO(
                    0, item.title,
                    parseAmount(item.giaKhoiDiem),
                    parseAmount(item.giaCaoNhat),
                    item.hanDangKi + " 23:59:59",
                    "RUNNING"
            );

            AuctionDetailController ctrl = loader.getController();
            ctrl.setAuction(dto);

            Stage stage = new Stage();
            stage.initStyle(StageStyle.UNDECORATED);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root, 700, 620));
            stage.setOnCloseRequest(ev -> ctrl.stopCountdown());
            stage.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private double parseAmount(String formatted) {
        try {
            return Double.parseDouble(
                    formatted.replace(".", "").replace(",", "")
                            .replace(" VNĐ", "").trim());
        } catch (Exception e) { return 0; }
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
}