package sample;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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

    // ===== Static instance để LoginController gọi lại =====
    private static HomeController instance;
    public static HomeController getInstance() { return instance; }

    // ===== FXML fields =====
    @FXML private FlowPane flowPane;
    @FXML private TextField txtSearch;

    // Navbar
    @FXML private HBox   guestBox;   // Chứa nút Đăng kí / Đăng nhập
    @FXML private HBox   userBox;    // Chứa avatar + tên + Đăng xuất
    @FXML private Label  lblUsername;

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
        instance = this; // ← Lưu instance để LoginController gọi được

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

    // ===== Được gọi từ LoginController sau khi đăng nhập thành công =====
    public void onLoginSuccess(String username) {
        // Ẩn nhóm nút khách
        guestBox.setVisible(false);
        guestBox.setManaged(false);

        // Hiện nhóm user
        lblUsername.setText(username);
        userBox.setVisible(true);
        userBox.setManaged(true);

        System.out.println("✅ Navbar đã cập nhật - Chào " + username);
    }

    // ===== Đăng xuất =====
    @FXML
    private void handleDangXuat() {
        UserSession.getInstance().logout();

        // Ẩn nhóm user, hiện lại nhóm khách
        userBox.setVisible(false);
        userBox.setManaged(false);

        guestBox.setVisible(true);
        guestBox.setManaged(true);

        System.out.println("→ Đã đăng xuất");
    }

    // ===== Mở cửa sổ Đăng nhập =====
    @FXML
    private void handleDangNhap() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/sample/login.fxml")
            );
            Parent root = loader.load();

            Stage loginStage = new Stage();
            loginStage.initStyle(StageStyle.UNDECORATED);
            loginStage.initModality(Modality.APPLICATION_MODAL); // Chặn home khi login
            loginStage.setScene(new Scene(root, 551, 400));
            loginStage.setTitle("Đăng nhập");
            loginStage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ===== Mở cửa sổ Đăng kí =====
    @FXML
    private void handleDangKi() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/sample/register.fxml")
            );
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.initStyle(StageStyle.UNDECORATED);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root, 520, 527));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSearch() {
        String keyword = txtSearch.getText().trim();
        System.out.println("Tìm kiếm: " + keyword);
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

        Separator sep = new Separator();

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

        Button btnDangKi = new Button("Đăng kí đấu giá");
        btnDangKi.setMaxWidth(Double.MAX_VALUE);
        btnDangKi.setStyle(
                "-fx-background-color: #c0392b; -fx-text-fill: white;" +
                        "-fx-font-size: 14; -fx-font-weight: bold;" +
                        "-fx-padding: 12; -fx-background-radius: 8; -fx-cursor: hand;"
        );
        btnDangKi.setOnAction(e -> {
            if (!UserSession.getInstance().isLoggedIn()) {
                handleDangNhap(); // Chưa login → mở trang login
            } else {
                System.out.println("→ Đăng kí đấu giá: " + item.title);
            }
        });

        Label lblLink = new Label("Xem chi tiết & Lịch sử");
        lblLink.setStyle("-fx-text-fill: #e05252; -fx-cursor: hand; -fx-font-size: 13;");
        VBox linkBox = new VBox(lblLink);
        linkBox.setAlignment(Pos.CENTER);

        card.getChildren().addAll(titleRow, sep, grid, btnDangKi, linkBox);
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