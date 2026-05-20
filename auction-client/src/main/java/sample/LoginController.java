package sample;

import com.google.gson.JsonObject;
import com.mysql.cj.xdevapi.JsonParser;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class LoginController implements Initializable {

    @FXML private Button        cancelButton;
    @FXML private Label         loginMessageLabel;
    @FXML private ImageView     brandingImageView;
    @FXML private ImageView     lockImageView;
    @FXML private TextField     usernameTextField;
    @FXML private PasswordField enterPasswordField;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Load ảnh
        try {
            URL brandingUrl = getClass().getResource("/images/logo_new.png");
            if (brandingUrl != null)
                brandingImageView.setImage(new Image(brandingUrl.toString()));

            URL lockUrl = getClass().getResource("/images/o_khoa.png");
            if (lockUrl != null)
                lockImageView.setImage(new Image(lockUrl.toString()));

        } catch (Exception e) {
            System.out.println("Lỗi khi tải ảnh!");
        }

        // Kết nối server
        try {
            ServerConnection.getInstance();
            System.out.println("✅ Kết nối server thành công!");
        } catch (Exception e) {
            System.out.println("❌ Không kết nối được server!");
        }
    }

    @FXML
    public void loginButtonOnAction(ActionEvent event) {
        String username = usernameTextField.getText().trim();
        String password = enterPasswordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            loginMessageLabel.setText("⚠ Không được bỏ trống thông tin!");
            return;
        }

        cancelButton.setDisable(true);
        loginMessageLabel.setText("Đang kết nối...");

        try {

            String response = ServerConnection.getInstance().login(username, password);

            if (response != null && response.startsWith("LOGIN SUCCESS")) {
                // Parse role từ response
                String json = response.split("===", 2)[1];
                JsonObject obj = JsonParser.parseString(json).getAsJsonObject();

                String role     = obj.get("role").getAsString();
                String uname    = obj.get("username").getAsString();
                //
                String role = response.split("===", 2)[1]; // trực tiếp lấy "ADMIN" / "SELLER" / "BIDDER"

                UserSession.getInstance().login(username, role);

                // Cập nhật Home nếu đang mở
                if (HomeController.getInstance() != null) {
                    HomeController.getInstance().onLoginSuccess(username);
                }

                // Điều hướng theo role
                navigateByRole(role);

            } else if ("LOGIN FAIL".equals(response)) {
                loginMessageLabel.setText("❌ Sai tên đăng nhập hoặc mật khẩu!");
                enterPasswordField.clear();
            } else {
                loginMessageLabel.setText("⚠ Phản hồi không hợp lệ: " + response);
            }

        } catch (Exception e) {
            loginMessageLabel.setText("⚠ Lỗi kết nối server!");
            e.printStackTrace();
        } finally {
            cancelButton.setDisable(false);
        }
    }

    // =====MỞ GIAO DIỆN THEO ĐÚNG ROLE=====
    private void navigateByRole(String role) {
        try {
            if (role.equalsIgnoreCase("SELLER")) {
                // Mở Seller Dashboard trong Stage mới 1200x800
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/sample/seller_dashboard.fxml"));
                Parent root = loader.load();

                Stage sellerStage = new Stage();
                sellerStage.setTitle("Seller Dashboard");
                sellerStage.setScene(new Scene(root, 1200, 800));
                sellerStage.setResizable(true);

                // Căn giữa màn hình
                sellerStage.centerOnScreen();
                sellerStage.show();

                // Đóng cửa sổ Login
                Stage loginStage = (Stage) cancelButton.getScene().getWindow();
                loginStage.close();

                // Đóng cửa sổ Home
                if (HomeController.getInstance() != null) {
                    Stage homeStage = (Stage) HomeController.getInstance()
                            .getRoot().getScene().getWindow();
                    homeStage.close();
                }

            } else if (role.equalsIgnoreCase("ADMIN")) {
                // Mở Seller Dashboard trong Stage mới 1200x800
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/sample/admin_dashboard.fxml"));
                Parent root = loader.load();

                Stage adminStage = new Stage();
                adminStage.setTitle("Admin Dashboard");
                adminStage.setScene(new Scene(root, 1200, 800));
                adminStage.setResizable(true);

                // Căn giữa màn hình
                adminStage.centerOnScreen();
                adminStage.show();

                // Đóng cửa sổ Login
                Stage loginStage = (Stage) cancelButton.getScene().getWindow();
                loginStage.close();

                // Đóng cửa sổ Home
                if (HomeController.getInstance() != null) {
                    Stage homeStage = (Stage) HomeController.getInstance()
                            .getRoot().getScene().getWindow();
                    homeStage.close();
                }

            } else {
                // BIDDER — chỉ đóng Login, giữ Home
                ((Stage) cancelButton.getScene().getWindow()).close();
            }

        } catch (Exception e) {
            loginMessageLabel.setText("⚠ Không tải được giao diện! (LoginController.java)");
            e.printStackTrace();
        }
    }

    @FXML
    public void cancelButtonAction(ActionEvent event) {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }
}