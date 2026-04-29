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
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ResourceBundle;

public class LoginController implements Initializable {

    // ===== FXML Fields =====
    @FXML private Button       cancelButton;
    @FXML private Label        loginMessageLabel;
    @FXML private ImageView    brandingImageView;
    @FXML private ImageView    lockImageView;
    @FXML private TextField    usernameTextField;
    @FXML private PasswordField enterPasswordField;

    // ===== Initialize: Load ảnh =====
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            // Ảnh logo bên trái
            URL brandingUrl = getClass().getResource("/images/logo_new.png");
            if (brandingUrl != null) {
                brandingImageView.setImage(new Image(brandingUrl.toString()));
            } else {
                System.out.println("⚠ Không tìm thấy: /images/logo_new.png");
            }

            // Ảnh ổ khóa
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

        validateLogin(username, password);
    }

    // ===== Xác thực với DB =====
    private void validateLogin(String username, String password) {
        try {
            DatabaseConnection connectNow = new DatabaseConnection();
            Connection connectDB = connectNow.getConnection();

            // ⚠ Thực tế nên dùng PreparedStatement để tránh SQL Injection
            String sql = "SELECT count(1) FROM user_account "
                    + "WHERE username = '" + username
                    + "' AND password = '" + password + "'";

            Statement statement   = connectDB.createStatement();
            ResultSet queryResult = statement.executeQuery(sql);

            if (queryResult.next()) {
                if (queryResult.getInt(1) == 1) {
                    // ✅ Đăng nhập thành công

                    // 1. Lưu session
                    UserSession.getInstance().login(username);

                    // 2. Cập nhật Navbar trang Home
                    if (HomeController.getInstance() != null) {
                        HomeController.getInstance().onLoginSuccess(username);
                    }

                    // 3. Đóng cửa sổ Login
                    closeWindow();

                } else {
                    // ❌ Sai thông tin
                    loginMessageLabel.setText("❌ Sai tên đăng nhập hoặc mật khẩu!");
                    enterPasswordField.clear();
                }
            }

            // Đóng kết nối
            queryResult.close();
            statement.close();
            connectDB.close();

        } catch (Exception e) {
            loginMessageLabel.setText("⚠ Lỗi kết nối cơ sở dữ liệu!");
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