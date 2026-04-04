package com.auction.client.controller;

import com.auction.client.utils.SessionManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private Label lblError;

    @FXML
    public void handleLogin(ActionEvent event) {
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText().trim();

        // 1. Kiểm tra rỗng
        if (username.isEmpty() || password.isEmpty()) {
            lblError.setText("Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu!");
            return;
        }

        // 2. Tạo chuỗi JSON gửi lên Server (Nên dùng thư viện Gson/Jackson thay vì nối chuỗi thủ công)
        String jsonRequest = String.format("{\"action\":\"LOGIN\", \"username\":\"%s\", \"password\":\"%s\"}", username, password);

        // 3. Gửi lên Server và nhận kết quả
        // Ở đây mình giả lập (Mock) kết quả Server trả về để bạn test UI
        String serverResponse = mockServerNetworkCall(jsonRequest);

        // 4. Xử lý kết quả trả về
        processServerResponse(serverResponse, event);
    }

    private void processServerResponse(String jsonResponse, ActionEvent event) {
        // Giả sử server trả về JSON: {"status":"SUCCESS", "role":"BIDDER"} hoặc {"status":"ERROR", "message":"Sai mật khẩu"}
        // Tạm thời dùng String contains để check (bạn nhớ thay bằng Gson.fromJson khi làm thật)

        if (jsonResponse.contains("\"status\":\"SUCCESS\"")) {
            lblError.setTextFill(javafx.scene.paint.Color.GREEN);
            lblError.setText("Đăng nhập thành công!");

            // Bóc tách Role (Giả lập parse JSON)
            String role = "BIDDER";
            if (jsonResponse.contains("\"role\":\"SELLER\"")) role = "SELLER";
            else if (jsonResponse.contains("\"role\":\"ADMIN\"")) role = "ADMIN";

            // Lưu phiên đăng nhập (Singleton Pattern)
            SessionManager.getInstance().setSession(txtUsername.getText(), role);

            // Chuyển màn hình
            navigateToMainScreen(event, role);

        } else {
            lblError.setTextFill(javafx.scene.paint.Color.RED);
            lblError.setText("Sai tài khoản hoặc mật khẩu!");
        }
    }

    private void navigateToMainScreen(ActionEvent event, String role) {
        try {
            String fxmlFile = "";
            if (role.equals("BIDDER")) {
                fxmlFile = "/com/auction/client/view/Auction.fxml"; // Màn hình đấu giá
            } else if (role.equals("SELLER")) {
                fxmlFile = "/com/auction/server/view/ManageProduct.fxml"; // Màn hình quản lý
            }

            
            Parent root = FXMLLoader.load(getClass().getResource(fxmlFile));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
            */
            System.out.println("Đang điều hướng sang màn hình của: " + role);

        } catch (Exception e) {
            lblError.setText("Lỗi load màn hình: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void handleRegister(ActionEvent event) {
        System.out.println("Chuyển sang màn hình Đăng ký...");
        // Viết logic load FXML đăng ký tương tự hàm navigateToMainScreen
    }

    // --- HÀM GIẢ LẬP GIAO TIẾP MẠNG (Xoá đi khi tích hợp Socket/API thật) ---
    private String mockServerNetworkCall(String jsonRequest) {
        System.out.println("Client gửi: " + jsonRequest);
        // Giả lập logic kiểm tra ở Server
        if (jsonRequest.contains("admin") && jsonRequest.contains("123")) {
            return "{\"status\":\"SUCCESS\", \"role\":\"ADMIN\"}";
        } else if (jsonRequest.contains("seller") && jsonRequest.contains("123")) {
            return "{\"status\":\"SUCCESS\", \"role\":\"SELLER\"}";
        } else if (jsonRequest.contains("bidder") && jsonRequest.contains("123")) {
            return "{\"status\":\"SUCCESS\", \"role\":\"BIDDER\"}";
        }
        return "{\"status\":\"ERROR\", \"message\":\"Sai tài khoản hoặc mật khẩu\"}";
    }
}
