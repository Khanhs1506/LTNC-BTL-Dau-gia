package com.auction.server.network;

import com.auction.server.model.*;
import com.auction.server.repository.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.*;
import java.util.List;

public class ClientHandler implements Runnable {

    public static int NumberOfClient = 0;

    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private Gson gson = new Gson();

    //Repository Layer
    private IUserDAO userRepo = new UserDaoImpl();
    private IItemDAO itemRepo = new ItemDaoImpl();
    private IAuctionDAO auctionRepo = new AuctionDaoImpl();
    private IBidTransactionDAO bidRepo = new BidTransactionDaoImpl();

    private User currentUser;

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
            String request;
            while ((request = reader.readLine()) != null) {
                System.out.println("Khách gửi: " + request);

                handlerRequest(request);
            }

            socket.close();
        } catch (Exception e) {
            System.out.println("Khách hàng mất mạng hoặc ngắt kết nối đột ngột");
        } finally {
            NumberOfClient--;
            System.out.println("Có " + NumberOfClient + " khách đang kết nối");

        }
    }

    private void handlerRequest(String request){
        try {
            String[] parts = request.split("===", 2);

            if (parts.length != 2){
                writer.println("INVAILD FORMAT");
                return;
            }

            String action = parts[0];
            String json = parts[1];

            switch (action) {
                case "LOGIN":
                    handlerLogin(json);
                    break;

                case "REGISTER":
                    handlerRegister(json);
                    break;

                case "GET_ITEMS":
                    handlerGetItems();
                    break;

                case "PLACE_BID":
                    handlePlaceBid(json);
                    break;

                case "LOGOUT":
                    handleLogout();
                    break;

                default:
                    writer.println("UNKNOWN ACTION");
            }
        } catch (Exception e){
            writer.println("SERVER ERROR");
        }
    }

    // ĐĂNG NHẬP TÀI KHOẢN
    private void handlerLogin(String json) {

        User inputUser = gson.fromJson(json, Seller.class);

        User dbUser = userRepo.getUserByUsername(inputUser.getUsername());

        if (dbUser != null && dbUser.getPassword().equals(inputUser.getPassword())) {

            currentUser = dbUser;
            writer.println("LOGIN SUCCESS");
        } else {
            writer.println("LOGIN FAIL");
        }
    }

    // ĐĂNG KÍ TÀI KHOẢN
    private void handlerRegister(String json) {
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();

        String username = obj.get("username").getAsString();
        String password = obj.get("password").getAsString();
        String role = obj.get("role").getAsString();

        User newUser = null;

        switch (role) {
            case "ADMIN":
                newUser = new Admin(null, username, password);
                break;
            case "SELLER":
                newUser = new Seller(null, username, password);
                break;
            case "BIDDER":
                newUser = new Bidder(null, username, password, 0);
                break;
        }

        boolean success = userRepo.registerUser(newUser);

        writer.println(success ? "REGISTER SUCCESS" : "REGISTER FAIL");
    }

    // TẠO SẢN PHẨM
    private void handlerCreateItem(String json){
        if (!(currentUser instanceof Seller)) {
            writer.println("ONLY SELLER CAN CREATE ITEM");
            return;
        }

        Item item = gson.fromJson(json, Item.class);

        int itemId = itemRepo.insertItem(item, currentUser.getId());

        if (itemId > 0) {
            writer.println("CREATE ITEM SUCCESS");
        } else {
            writer.println("CREATE ITEM FAIL");
        }
    }

    //LẤY TOÀN BỘ SẢN PHẨM
    private void handlerGetItems() {
        List<Item> items = itemRepo.getAllItems();
        writer.println("ITEM===" + gson.toJson(items));
    }

   private void handlePlaceBid(String json) {

        if (!(currentUser instanceof Bidder)) {
            writer.println("ONLY BIDDER CAN BID");
            return;
        }

        BidTransaction bid = gson.fromJson(json, BidTransaction.class);
        boolean saveBid = bidRepo.insertBid(bid);
        boolean updateAuction = auctionRepo.updateHighestBid(bid.getAuctionId(), bid.getBidAmount(), currentUser.getUsername());

        if (saveBid && updateAuction){
            writer.println("BID SUCCESS");
        } else {
            writer.println("BID FAIL");
        }
    }

    //ĐĂNG XUẤT
    private void handleLogout() {
        currentUser = null;
        writer.println("LOGOUT SUCCESS");
    }
}