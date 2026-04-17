package com.auction.server.network;

import com.auction.server.model.User;
import com.google.gson.JsonObject;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.sql.*;
import java.util.zip.DataFormatException;

public class ClientHandler extends Thread {

    // KẾT NỐI ĐẾN DATABASE.
    private final String DB_URL = "jdbc:mysql://localhost:3306/he_thong_dau_gia";
    private final String USERNAME = "root";
    private final String PASS = "123456";
    private Connection con;

    public static int NumberOfClient = 0;
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private Gson gson;

    private User currentUser;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        gson = new Gson();
        try {
            con = DriverManager.getConnection(DB_URL, USERNAME, PASS);
            System.out.println("Kết nối DTB thành công");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        reader = null;
        writer = null;
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);
            String clientMessage;
            JsonObject req;
            while ((clientMessage = reader.readLine()) != null) {

                System.out.println("Khách gửi: " + clientMessage);
                if (clientMessage.startsWith("LOGIN")) {
                    handlerLogin(clientMessage);
                }


            }
            NumberOfClient--;
        } catch (Exception e) {
            NumberOfClient--;
            System.out.println("Khách hàng mất mạng hoặc ngắt kết nối đột ngột");
        } finally {
            System.out.println("Có " + NumberOfClient + " khách đang kết nối");
        }

    }

    public void handlerLogin(String clientMessage) {
        String[] data = clientMessage.split("\\|");

        String username = data[1];
        String password = data[2];

        System.out.println("Username: " + username);
        System.out.println("Password: " + password);

        // kiểm tra login (demo)


        if (username.equals("admin") && password.equals("123")) {

            writer.println("LOGIN SUCCESS");
            System.out.println("Login thanh cong");

        } else {

            writer.println("FAIL");
            System.out.println("Login that bai");

        }
    }

    public void handlerRegister(String clientMessage){
        String[] data = clientMessage.split("\\|");

        String username = data[1];
        String password = data[2];


        try {
            // Kiểm tra đã tồn tại chưa.
            PreparedStatement pstCheck = con.prepareStatement("SELECT * FROM TaiKhoan WHERE username = ?");

            pstCheck.setString(1, username);
            ResultSet rs = pstCheck.executeQuery();

            if (rs.next()){
                System.out.println("Tên đăng nhập đã tồn tại. Hãy nhập lại.");
            } else {

                //Tạo tài khoản
                PreparedStatement pstInsert = con.prepareStatement("INSERT INTO TaiKhoan (username, password, role) VALUES (?, ?, 'USER')");

                pstInsert.setString(1, username);
                pstInsert.setString(2, password);
                pstInsert.executeUpdate();
                System.out.println("Tạo tài khoản thành công.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


}