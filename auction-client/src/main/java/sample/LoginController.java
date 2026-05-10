package sample;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class LoginController implements Initializable {

    // ===== FXML Fields =====
    @FXML private Button        cancelButton;
    @FXML private Label         loginMessageLabel;
    @FXML private ImageView     brandingImageView;
    @FXML private ImageView     lockImageView;
    @FXML private TextField     usernameTextField;
    @FXML private PasswordField enterPasswordField;

    // ===== Initialize: Load ảnh =====
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            URL brandingUrl = getClass().getResource("/images/logo_new.png");
            if (brandingUrl != null) {
                brandingImageView.setImage(new Image(brandingUrl.toString()));
            } else {
                System.out.println("⚠ Không tìm thấy: /images/logo_new.png");
            }

            URL lockUrl = getClass().getResource("/images/o_khoa.png");
            if (lockUrl != null) {
                lockImageView.setImage(new Image(lockUrl.toString()));
            } else {
                System.out.println("⚠ Không tìm thấy: /images/o_khoa.png");
            }

        } catch (Exception e) {
            System.out.println("Lỗi khi tải ảnh!");
            e.printStackTrace();
        }
    }

    // ===== Nút Login =====
    @FXML
    public void loginButtonOnAction(ActionEvent event) {
        String username = usernameTextField.getText().trim();
        String password = enterPasswordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            loginMessageLabel.setText("⚠ Không được bỏ trống thông tin!");
            return;
        }

        // Disable nút tránh click nhiều lần khi đang chờ server
        cancelButton.setDisable(true);
        loginMessageLabel.setText("Đang kết nối...");

        validateLogin(username, password);

        cancelButton.setDisable(false);
    }

    // ===== Xác thực với Server =====
    private void validateLogin(String username, String password) {
        try {
            // Gửi request tới server, nhận response
            String response = ServerConnection.getInstance().login(username, password, "Seller");

            // ✅ Server trả về "LOGIN SUCCESS" (khớp với ClientHandler)
            if ("LOGIN SUCCESS".equals(response)) {

                // 1. Lưu session
                UserSession.getInstance().login(username);

                // 2. Cập nhật Navbar trang Home nếu đang mở
                if (HomeController.getInstance() != null) {
                    HomeController.getInstance().onLoginSuccess(username);
                }

                // 3. Đóng cửa sổ Login
                closeWindow();

            } else if ("LOGIN FAIL".equals(response)) {
                // ❌ Sai username hoặc password
                loginMessageLabel.setText("❌ Sai tên đăng nhập hoặc mật khẩu!");
                enterPasswordField.clear();

            } else {
                // Trường hợp server trả về gì đó không mong đợi
                loginMessageLabel.setText("⚠ Phản hồi không hợp lệ từ server: " + response);
            }

        } catch (java.net.ConnectException e) {
            loginMessageLabel.setText("⚠ Không thể kết nối tới server!");
        } catch (Exception e) {
            loginMessageLabel.setText("⚠ Lỗi: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ===== Nút Cancel =====
    @FXML
    public void cancelButtonAction(ActionEvent event) {
        closeWindow();
    }

    // ===== Helper: Đóng cửa sổ hiện tại =====
    private void closeWindow() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }
}