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

    private ContextMenu khacMenu;

    // ===== Model =====
    static class AuctionItem {
        String title, giaKhoiDiem, giaCaoNhat, thoiGian, hanDangKi;
        int thauThu;

        AuctionItem(String title, String giaKhoiDiem, String giaCaoNhat,
                    String thoiGian, int thauThu, String hanDangKi) {
            this.title       = title;
            this.giaKhoiDiem = giaKhoiDiem;
            this.giaCaoNhat  = giaCaoNhat;
            this.thoiGian    = thoiGian;
            this.thauThu     = thauThu;
            this.hanDangKi   = hanDangKi;
        }
    }

    // ===== Initialize =====
    @FXML
    public void initialize() {
        instance = this;

        guestBox.setVisible(true);
        guestBox.setManaged(true);
        userBox.setVisible(false);
        userBox.setManaged(false);

        buildKhacMenu();

        List<AuctionItem> items = Arrays.asList(
                new AuctionItem("G.5 - BKS 30K - 888.88",
                        "120.000.000 VNĐ", "215.000.000 VNĐ", "22:01:45", 36, "23/12/2026"),
                new AuctionItem("H.5 - BKS 51K - 777.77",
                        "80.000.000 VNĐ",  "150.000.000 VNĐ", "18:30:00", 12, "15/11/2026"),
                new AuctionItem("A.1 - BKS 43K - 999.99",
                        "200.000.000 VNĐ", "320.000.000 VNĐ", "10:15:30", 24, "01/06/2026")
        );

        for (AuctionItem item : items) {
            flowPane.getChildren().add(createCard(item));
        }
    }

    // ===== Menu Khác =====
    private void buildKhacMenu() {
        khacMenu = new ContextMenu();

        String[] categories = {
                "Đồ cổ", "Trang sức", "Tác phẩm nghệ thuật", "Đồng hồ",
                "Túi xách", "Thời trang", "Thiết bị điện tử", "Điện thoại",
                "Máy tính", "Xe cộ", "Nội thất", "Sách và tài liệu sưu tầm",
                "Đồ lưu niệm", "Nhạc cụ", "Đồ chơi sưu tầm", "Thẻ bài / mô hình",
                "Tiền xu / tem", "Vật phẩm game", "NFT / tài sản số",
                "Đồ gia dụng", "Thiết bị công nghiệp", "Máy móc",
                "Động vật cảnh", "Vé sự kiện"
        };

        for (String cat : categories) {
            MenuItem menuItem = new MenuItem(cat);
            menuItem.setStyle("-fx-font-size: 13px; -fx-padding: 6 16;");
            menuItem.setOnAction(e -> handleCategorySelected(cat));
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

    private void handleCategorySelected(String category) {
        System.out.println("Danh mục đã chọn: " + category);
        // TODO: lọc flowPane theo category
    }

    // ===== Login / Logout =====
    public void onLoginSuccess(String username) {
        guestBox.setVisible(false);
        guestBox.setManaged(false);
        lblUsername.setText(username);
        userBox.setVisible(true);
        userBox.setManaged(true);
        System.out.println("✅ Đăng nhập thành công - Chào " + username);
    }

    @FXML
    private void handleDangXuat() {
        UserSession.getInstance().logout();
        userBox.setVisible(false);
        userBox.setManaged(false);
        guestBox.setVisible(true);
        guestBox.setManaged(true);
        System.out.println("→ Đã đăng xuất");
    }

    // ===== Helper mở cửa sổ chung =====
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
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Lỗi");
            alert.setHeaderText(null);
            alert.setContentText("Không thể mở " + title + "!\nLỗi: " + e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void handleDangNhap() {
        openWindow("/sample/login.fxml", "Đăng Nhập", 551, 400);
    }

    @FXML
    private void handleDangKi() {
        openWindow("/sample/register.fxml", "Đăng Ký", 520, 600);
    }

    @FXML
    private void handleSearch() {
        String keyword = txtSearch.getText().trim();
        if (keyword.isEmpty()) return;
        System.out.println("🔍 Tìm kiếm: " + keyword);
        // TODO: gọi server lọc theo keyword
    }

    // ===== Mở AuctionDetail =====
    private void openAuctionDetail(AuctionItem item) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/sample/AuctionDetail.fxml"));
            Parent root = loader.load();

            AuctionItemDTO dto = new AuctionItemDTO(
                    0,
                    item.title,
                    parseAmount(item.giaKhoiDiem),
                    parseAmount(item.giaCaoNhat),
                    item.hanDangKi + " 23:59:59",
                    "RUNNING"
            );

            AuctionDetailController ctrl = loader.getController();
            ctrl.setAuction(dto);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root, 700, 620));
            stage.setTitle("Chi tiết: " + item.title);
            stage.setOnCloseRequest(ev -> ctrl.stopCountdown());
            stage.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Lỗi");
            alert.setHeaderText(null);
            alert.setContentText("Không thể mở chi tiết!\nLỗi: " + e.getMessage());
            alert.showAndWait();
        }
    }

    // Helper: "120.000.000 VNĐ" → 120000000.0
    private double parseAmount(String formatted) {
        try {
            return Double.parseDouble(
                    formatted.replace(".", "")
                            .replace(",", "")
                            .replace(" VNĐ", "")
                            .trim()
            );
        } catch (Exception e) {
            return 0;
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

        // Tiêu đề + tim
        HBox titleRow = new HBox();
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label lblTitle = new Label(item.title);
        lblTitle.setStyle("-fx-font-size: 15; -fx-font-weight: bold; -fx-text-fill: #222;");
        lblTitle.setWrapText(true);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label lblHeart = new Label("♥");
        lblHeart.setStyle("-fx-text-fill: #e05252; -fx-font-size: 17; -fx-cursor: hand;");

        titleRow.getChildren().addAll(lblTitle, spacer, lblHeart);

        // Grid thông tin
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(9);

        ColumnConstraints cc0 = new ColumnConstraints();
        cc0.setHgrow(Priority.ALWAYS);
        ColumnConstraints cc1 = new ColumnConstraints();
        cc1.setHgrow(Priority.NEVER);
        grid.getColumnConstraints().addAll(cc0, cc1);

        addRow(grid, 0, "Giá khởi điểm:",         item.giaKhoiDiem,             false);
        addRow(grid, 1, "*Giá cao nhất hiện tại:", item.giaCaoNhat,              true);
        addRow(grid, 2, "Thời gian:",              item.thoiGian,                false);
        addRow(grid, 3, "Thầu thứ:",               String.valueOf(item.thauThu), false);
        addRow(grid, 4, "Hạn đăng kí đến:",        item.hanDangKi,              false);

        // Nút đăng kí đấu giá
        Button btnDangKi = new Button("Đăng kí đấu giá");
        btnDangKi.setMaxWidth(Double.MAX_VALUE);
        btnDangKi.setStyle(
                "-fx-background-color: #c0392b; -fx-text-fill: white;" +
                        "-fx-font-size: 14; -fx-font-weight: bold;" +
                        "-fx-padding: 12; -fx-background-radius: 8; -fx-cursor: hand;"
        );
        btnDangKi.setOnAction(e -> {
            if (!UserSession.getInstance().isLoggedIn()) {
                handleDangNhap();
            } else {
                System.out.println("→ Đăng kí đấu giá: " + item.title);
                // TODO: ServerConnection.placeBid(...)
            }
        });

        // Link xem chi tiết  ← gọi openAuctionDetail
        Label lblLink = new Label("Xem chi tiết & Lịch sử");
        lblLink.setStyle("-fx-text-fill: #e05252; -fx-cursor: hand; -fx-font-size: 13;");
        lblLink.setOnMouseClicked(e -> openAuctionDetail(item));

        VBox linkBox = new VBox(lblLink);
        linkBox.setAlignment(Pos.CENTER);

        card.getChildren().addAll(titleRow, new Separator(), grid, btnDangKi, linkBox);
        return card;
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