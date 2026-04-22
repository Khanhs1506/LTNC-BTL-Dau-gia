package com.auction.client.controller;

import com.auction.server.model.Seller;
import com.auction.server.model.User;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.IOException;

public class LoginController{

    private SocketClient socketClient;
    private GlassLogin view;

    public LoginController(SocketClient socketClient, GlassLogin view){
        this.socketClient = socketClient;
        this.view = view;

        // Gán sự kiện cho Button: "Login".
        // Cách 1: Action Listener:
        view.getLoginButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e){
                handleLogin(); // Code ở bên dưới: Gửi dữ liêu đăng nhập từ Client tới Server;
        
            }
        });
        // Cách 2: Lamda - Java 8+
        // this.view.getLoginButton().addActionListener((e -> handleLogin()));
    }

    private void handleLogin(){
        String username = view.getUsername();
        String password = view.getPassword();

        // Kiểm tra kiểu dữ liệu trước khi gửi đến Server
        if (username.isEmpty() || password.isEmpty()){
            view.showError("Please enter username and password"); // Hiện thị lỗi không nhâp đủ username và password;
            return;
        }
        
        try{
            // Gửi dữ liệu tới Server
            socketClient.sendRequest("LOGIN", new Seller("001","admin", "123"));

            String response = socketClient.receive(); // Nhận dữ liệu từ Server;

            // 🚨Chú ý: Khi code Server (kết nôi) nhận dữ liểu thành công thì code "LOGIN SUCCESS".
            if ("LOGIN SUCCESS".equals(response)){
                view.showMessage("Login success");
            }
            else {
                view.showMessage("Login failed");
            }
        } catch (IOException e){
            e.printStackTrace();// Hiển thị chi tiết lỗi.
            System.out.println("Cannot conncet to server");
        }
    }


    
    
    // public static void main(String[] args) {
        
    // }
}