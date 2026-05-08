package sample;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class RegisterController implements Initializable {

    // ===== FXML Fields =====
    @FXML private ImageView   shieldImageView;
    @FXML private TextField   firstnameTextField;
    @FXML private TextField   lastnameTextField;
    @FXML private TextField   usernameTextField;
    @FXML private PasswordField setPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Button      closeButton;
    @FXML private Label       registerMessageLabel;
    @FXML private ComboBox<String> roleComboBox;

    // ===== Initialize: Load ảnh =====
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            URL shieldUrl = getClass().getResource("/images/shield.png");
            if (shieldUrl != null) {
                shieldImageView.setImage(new Image(shieldUrl.toString()));
            } else {
                System.out.println("⚠ Không tìm thấy: /images/shield.png");
            }
        } catch (Exception e) {
            System.out.println("Lỗi khi tải ảnh shield!");
            e.printStackTrace();
        }
        if (roleComboBox != null) {
            roleComboBox.getItems().addAll("BIDDER", "SELLER");
            // Đặt BIDDER làm giá trị mặc định để tránh người dùng quên chọn
            roleComboBox.setValue("BIDDER");
        }
    }

    // ===== Nút Register =====
    @FXML
    public void registerButtonOnAction(ActionEvent event) {
        String firstname = firstnameTextField.getText().trim();
        String lastname  = lastnameTextField.getText().trim();
        String username  = usernameTextField.getText().trim();
        String password  = setPasswordField.getText().trim();
        String confirm   = confirmPasswordField.getText().trim();
        String role      = roleComboBox.getValue();


        // Kiểm tra bỏ trống
        if (firstname.isEmpty() || lastname.isEmpty() || username.isEmpty()
                || password.isEmpty() || confirm.isEmpty()) {
            registerMessageLabel.setText("⚠ Vui lòng điền đầy đủ thông tin!");
            return;
        }

        // Kiểm tra mật khẩu khớp
        if (!password.equals(confirm)) {
            registerMessageLabel.setText("❌ Mật khẩu xác nhận không khớp!");
            confirmPasswordField.clear();
            return;
        }

        // Gửi yêu cầu đăng ký lên server
        try {
            String response = ServerConnection.getInstance().register(username, password, role);

            if (response != null && response.equalsIgnoreCase("REGISTER SUCCESS")) {
                registerMessageLabel.setStyle("-fx-text-fill: #27ae60;");
                registerMessageLabel.setText("✅ Đăng ký thành công! Đang đóng...");

                // Đóng cửa sổ sau 1.5 giây
                new Thread(() -> {
                    try {
                        Thread.sleep(1500);
                        javafx.application.Platform.runLater(this::closeWindow);
                    } catch (InterruptedException ignored) {}
                }).start();

            } else {
                registerMessageLabel.setStyle("-fx-text-fill: #e05252;");
                registerMessageLabel.setText("❌ Tên đăng nhập đã tồn tại!");
                usernameTextField.clear();
            }

        } catch (java.net.ConnectException e) {
            registerMessageLabel.setStyle("-fx-text-fill: #e05252;");
            registerMessageLabel.setText("⚠ Lỗi kết nối server!");
        } catch (Exception e) {
            registerMessageLabel.setStyle("-fx-text-fill: #e05252;");
            registerMessageLabel.setText("⚠ Có lỗi xảy ra!");
            e.printStackTrace();
        }
    }

    // ===== Nút Close =====
    @FXML
    public void closeButtonOnAction(ActionEvent event) {
        closeWindow();
    }

    // ===== Helper: Đóng cửa sổ =====
    private void closeWindow() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }
}
