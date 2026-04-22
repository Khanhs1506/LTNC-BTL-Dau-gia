package sample;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.event.ActionEvent;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import javafx.scene.image.ImageView;
import javafx.stage.StageStyle;
import org.w3c.dom.Text;
import javafx.scene.control.TextField;     // <-- THÊM MỚI
import javafx.scene.control.PasswordField; // <-- THÊM MỚI

import javafx.application.Application;

import javafx.scene.image.Image;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ResourceBundle;

public class LoginController extends Application implements Initializable {

    @FXML
    private Button cancelButton;

    @FXML
    private Label loginMessageLabel;

    @FXML
    private ImageView brandingImageView;

    @FXML
    private ImageView lockImageView;

    @FXML
    private TextField usernameTextField;
    @FXML
    private PasswordField enterPasswordField;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // CÓ THỂ KHÔNG CẦN VIẾT CODE PHẦN NÀy
        try {
            // Ảnh con sóc
            URL brandingUrl = getClass().getResource("images/logo_new.png");
            if (brandingUrl != null) {
                // Biến URL thành Image và nhét vào khung ImageView
                Image brandingImage = new Image(brandingUrl.toString());
                brandingImageView.setImage(brandingImage);
            } else {
                System.out.println("Lỗi: Không tìm thấy ảnh logo_new.png");
            }

            // Ảnh ổ khóa màu cam
            URL lockUrl = getClass().getResource("/images/o_khoa.png");
            if (lockUrl != null) {
                Image lockImage = new Image(lockUrl.toString());
                lockImageView.setImage(lockImage);
            } else {
                System.out.println("Lỗi: Không tìm thấy ảnh o_khoa.png");
            }

        } catch (Exception e) {
            System.out.println("Có lỗi xảy ra khi tải ảnh!");
            e.printStackTrace();
        }
    }

    public void loginButtonOnAction(ActionEvent event) throws IOException {
        if (usernameTextField.getText().isEmpty() == false && enterPasswordField.getText().isEmpty() == false) {
            validateLogin();
        } else {
            loginMessageLabel.setText("Không được bỏ trống thông tin");
        }
    }

    // Xác thực đăng nhập (Kiểm tra đăng nhập)
    private void validateLogin() {
        DatabaseConnection connectNow = new DatabaseConnection();
        Connection connectDB = connectNow.getConnection();

        String verifyLogin = "SELECT count(1) FROM user_account WHERE username = '" + usernameTextField.getText() + "' AND password = '" + enterPasswordField.getText() + "'";

        try {
            Statement statement = connectDB.createStatement();
            ResultSet queryReaults = statement.executeQuery(verifyLogin);

            while (queryReaults.next()) {
                if (queryReaults.getInt(1) == 1) {
                    //loginMessageLabel.setText("Đăng nhập thành công!");
                    createAccountForm();
                } else {
                    loginMessageLabel.setText("Đăng nhập không hợp lệ.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            e.getCause();
        }

    }

    public void cancelButtonAction(ActionEvent event) throws IOException {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

    public void createAccountForm() throws IOException {

        Parent root = FXMLLoader.load(getClass().getResource("/sample/register.fxml"));
        Stage registerStage = new Stage();
        registerStage.initStyle(StageStyle.UNDECORATED);
        registerStage.setScene(new Scene(root, 520, 527));
        registerStage.show();

        // Đóng cửa sổ đăng nhập hiện tại (nếu muốn)
        Stage loginStage = (Stage) loginMessageLabel.getScene().getWindow();
        loginStage.close();

    }

    @Override
    public void start(Stage primaryStage) throws Exception {

    }
}