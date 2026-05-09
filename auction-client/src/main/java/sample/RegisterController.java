package sample;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class RegisterController implements Initializable {

    @FXML private ImageView shieldImageView;
    @FXML private TextField hoTenTextField;
    @FXML private TextField usernameTextField;      // ★ MỚI
    @FXML private PasswordField matKhauField;
    @FXML private PasswordField xacThucMatKhauField;
    @FXML private ComboBox<String> chucNangComboBox;
    @FXML private Button registerButton;
    @FXML private Button closeButton;

    private static final java.util.Map<String, String> ROLE_MAP = new java.util.LinkedHashMap<>();
    static {
        ROLE_MAP.put("Quản trị viên", "ADMIN");
        ROLE_MAP.put("Người bán",     "SELLER");
        ROLE_MAP.put("Người đấu giá", "BIDDER");
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        File shieldFile = new File("images/shield.png");
        if (shieldFile.exists()) {
            shieldImageView.setImage(new Image(shieldFile.toURI().toString()));
        }
        chucNangComboBox.getItems().addAll(ROLE_MAP.keySet());
        registerButton.setOnAction(e -> handleRegister());
        closeButton.setOnAction(e -> handleClose());
    }

    private void handleRegister() {
        String hoTen    = hoTenTextField.getText().trim();
        String username = usernameTextField.getText().trim();   // ★ MỚI
        String matKhau  = matKhauField.getText();
        String xacThuc  = xacThucMatKhauField.getText();
        String chucNang = chucNangComboBox.getValue();

        // ── Validate ──────────────────────────────────────────
        if (hoTen.isEmpty() || username.isEmpty() || matKhau.isEmpty()
                || xacThuc.isEmpty() || chucNang == null) {
            showAlert(Alert.AlertType.WARNING, "Thiếu thông tin",
                    "Vui lòng điền đầy đủ tất cả các trường!");
            return;
        }
        if (!matKhau.equals(xacThuc)) {
            showAlert(Alert.AlertType.ERROR, "Lỗi mật khẩu",
                    "Mật khẩu xác thực không khớp!");
            return;
        }
        if (matKhau.length() < 6) {
            showAlert(Alert.AlertType.WARNING, "Mật khẩu yếu",
                    "Mật khẩu phải có ít nhất 6 ký tự!");
            return;
        }

        // ── Gửi lên server ────────────────────────────────────
        String role = ROLE_MAP.get(chucNang);
        try {
            // ★ Truyền username (không phải hoTen) lên server
            String response = ServerConnection.getInstance().register(username, matKhau, role);

            if ("REGISTER SUCCESS".equals(response)) {
                showAlert(Alert.AlertType.INFORMATION, "Thành công",
                        "Đăng ký tài khoản thành công!\nVui lòng đăng nhập.");
                navigateToLogin();
            } else {
                showAlert(Alert.AlertType.ERROR, "Đăng ký thất bại",
                        "Tên đăng nhập đã tồn tại hoặc có lỗi xảy ra.\nVui lòng thử lại!");
            }

        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Lỗi kết nối",
                    "Không thể kết nối tới server!\n" + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void navigateToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/sample/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) registerButton.getScene().getWindow();
            stage.setScene(new Scene(root, 520, 400));
            stage.setTitle("Đăng Nhập");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Lỗi",
                    "Không thể mở màn hình đăng nhập!");
        }
    }

    private void handleClose() {
        ((Stage) closeButton.getScene().getWindow()).close();
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}