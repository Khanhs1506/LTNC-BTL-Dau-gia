package sample;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.File;
import java.net.URL;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ResourceBundle;

public class RegisterController implements Initializable {

    @FXML
    private ImageView shieldImageView;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        File shieldFile = new File("images/shield.png");
        Image shieldImage = new Image(shieldFile.toURI().toString());
        shieldImageView.setImage(shieldImage);
//        // CÓ THỂ KHÔNG CẦN VIẾT CODE PHẦN NÀy
//        try {
//            // Ảnh khiên
//            URL shieldUrl = getClass().getResource("images/shield.png");
//            if (shieldUrl != null) {
//                // Biến URL thành Image và nhét vào khung ImageView
//                Image shieldImage = new Image(shieldUrl.toString());
//                shieldImageView.setImage(shieldImage);
//            } else {
//                System.out.println("Lỗi: Không tìm thấy ảnh logo_new.png");
//            }
//
//
//
//        } catch (Exception e) {
//            System.out.println("Có lỗi xảy ra khi tải ảnh!");
//            e.printStackTrace();
//        }
    }




}
