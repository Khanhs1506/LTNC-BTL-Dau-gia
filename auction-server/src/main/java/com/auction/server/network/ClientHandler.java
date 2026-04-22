package com.auction.server.network;

<<<<<<< HEAD
public class ClientHandler {
}
=======
import com.auction.server.model.*;
import com.auction.server.repository.DatabaseManager;
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

public class ClientHandler implements Runnable {

    // KẾT NỐI ĐẾN DATABASE.
    private final String DB_URL = "jdbc:mysql://localhost:3306/he_thong_dau_gia";
    private final String USERNAME = "root";
    private final String PASS = "123456";

    public static int NumberOfClient = 0;
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private Connection con = DatabaseManager.getInstance().getConnection();
    private Gson gson = new Gson();
    private User user;

    public ClientHandler(Socket socket) throws Exception {
        this.socket = socket;
    }

    @Override
    public void run() {
        reader = null;
        writer = null;
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);
            String clientMessage;
            while ((clientMessage = reader.readLine()) != null) {
                System.out.println("Khách gửi: " + clientMessage);
                String[] parts =  clientMessage.split("===");

                if (parts.length == 2){
                    String action = parts[0];
                    String jsonData = parts[1];

                    switch (action) {
                        case "LOGIN":
                            handlerLogin(jsonData);
                            break;
                    }
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

    // ĐĂNG NHẬP TÀI KHOẢN
    public void handlerLogin(String clientMessage) {    //sau đổi String thành User

        User user = gson.fromJson(clientMessage, Seller.class);


        try {
            PreparedStatement pstLogin = con.prepareStatement("SELECT * FROM users WHERE username = ? AND password = ?");
            pstLogin.setString(1, user.getUsername());
            pstLogin.setString(2, user.getPassword());
            ResultSet rs = pstLogin.executeQuery();
            if (rs.next()){
                writer.println("LOGIN SUCCESS");
                System.out.println("Login thanh cong");
                this.user = new Seller("001", user.getUsername(), user.getPassword());
            } else {
                writer.println("FAIL");
                System.out.println("Login that bai");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ĐĂNG KÍ TÀI KHOẢN
    public boolean handlerRegister(String clientMessage) { //sau đổi String thành User
        String[] data = clientMessage.split("\\|");

        String username = data[1];
        String password = data[2];


        try {
            // Kiểm tra đã tồn tại chưa.
            PreparedStatement pstCheck = con.prepareStatement("SELECT * FROM users WHERE username = ?");

            pstCheck.setString(1, username);
            ResultSet rs = pstCheck.executeQuery();

            if (rs.next()){
                System.out.println("Tên đăng nhập đã tồn tại. Hãy nhập lại.");
                return false;
            } else {

                //Tạo tài khoản
                PreparedStatement pstInsert = con.prepareStatement("INSERT INTO users (username, password, role) VALUES (?, ?, 'USER')");

                pstInsert.setString(1, username);
                pstInsert.setString(2, password);
                pstInsert.executeUpdate();
                System.out.println("Tạo tài khoản thành công.");
                return true;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // TẠO SẢN PHẨM
    public boolean handlerPostItem(Item item){
        try {

            con.setAutoCommit(false);

            try {
                PreparedStatement pstInsertIt = con.prepareStatement("INSERT INTO Items (name, item_type, startingPrice, currentHighestBid, status, seller_id) VALUES (?, ?, ?, ?, 'thêm sau', 'them sau')");
                pstInsertIt.setString(1, item.getName());
                pstInsertIt.setString(2, item.getType_item());
                pstInsertIt.setDouble(3, item.getStartingPrice());
                pstInsertIt.executeUpdate();
                System.out.println("Tạo sản phẩm thành công.");

                ResultSet rs = pstInsertIt.getGeneratedKeys();

                if (rs.next()) {
                    int idItem = rs.getInt(1);
                    if (item instanceof ElectronicsItem) {
                        ElectronicsItem elItem = (ElectronicsItem) item;
                        try (PreparedStatement pstEle = con.prepareStatement("INSERT INTO Electronics_Items (item_id, warranty_months) VALUES (?, ?)")) {
                            pstEle.setInt(1, idItem);
                            pstEle.setInt(2, elItem.getWarrantyMonths());
                            pstEle.executeUpdate();
                        }
                    } else if (item instanceof ArtItem) {
                        ArtItem artItem = (ArtItem) item;
                        try (PreparedStatement pstArt = con.prepareStatement("INSERT INTO Art_Items (item_id, artist_name) VALUES (?, ?)")) {
                            pstArt.setInt(1, idItem);
                            pstArt.setString(2, artItem.getArtist());
                            pstArt.executeUpdate();
                        }
                    } else if (item instanceof VehicleItem) {
                        VehicleItem vehicleItem = (VehicleItem) item;
                        try (PreparedStatement pstVeh = con.prepareStatement("INSERT INTO Vehicle_Items (item_id, brand, year) VALUES (?, ?, ?)")) {
                            pstVeh.setInt(1, idItem);
                            pstVeh.setString(2, vehicleItem.getBrand());
                            pstVeh.setInt(3, vehicleItem.getYear());
                            pstVeh.executeUpdate();
                        }
                    }
                }
                con.commit();
                System.out.println("Tạo sản phẩm thành công.");
                return true;

            } catch (SQLException e){
                con.rollback();
                System.out.println("Tạo sản phẩm thất bại.");
                e.printStackTrace();
                return false;
            }
        } catch (SQLException e){
            e.printStackTrace();
            return false;
        }
    }


    // ĐẶT GIÁ SẢN PHẨM
    public synchronized boolean handlerPlaceBid(Item item, double newBid){
        if (this.user == null) {
            return false;
        }

        try {
            con.setAutoCommit(false);
            PreparedStatement pstCheck = con.prepareStatement("SELECT * FROM Items WHERE id = ? AND name = ?");
            pstCheck.setString(1, item.getId());
            pstCheck.setString(2, item.getName());
            ResultSet rs = pstCheck.executeQuery();

            if(rs.next()){
                double currentPrice = rs.getDouble("current_highest_bid");
                if (newBid <= currentPrice){
                    con.rollback();
                    return false;
                }
            } else {
                return false;
            }
            PreparedStatement pstAddBid = con.prepareStatement("INSERT INTO bids (item_id, bidder_id, bid_amount, created_at) VALUES (?, ?, ?, NOW())");
            pstAddBid.setString(1, item.getId());
            pstAddBid.setString(2, user.getId());
            pstAddBid.setDouble(3, newBid);
            pstAddBid.executeUpdate();

            PreparedStatement pstPlaceBid = con.prepareStatement("UPDATE items SET current_highest_bid = ? WHERE id = ? and name = ?");
            pstPlaceBid.setDouble(1, newBid);
            pstAddBid.setString(2, item.getId());
            pstPlaceBid.executeUpdate();

            con.commit();
            return true;
        } catch (SQLException e){
            e.printStackTrace();
            return false;
        }
    }
}
>>>>>>> 817410f54e5bfcefbf958f5c1aab6ba102d2f415
