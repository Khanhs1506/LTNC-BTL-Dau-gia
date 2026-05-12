package com.auction.server.network;

import com.auction.server.model.*;
import com.auction.server.repository.*;
import com.auction.server.service.AuctionObserver;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;



import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


public class ClientHandler implements Runnable {

    public static int NumberOfClient = 0;
    public static final List<ClientHandler> connectedClients = new CopyOnWriteArrayList<>();

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
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new PrintWriter(socket.getOutputStream(), true);
        connectedClients.add(this);
    }

    @Override
    public void run() {
        try {
            String request;
            while ((request = reader.readLine()) != null) {
                System.out.println("Khách gửi: " + request);

                handlerRequest(request);
            }

            socket.close();
        } catch (Exception e) {
            System.out.println("Khách hàng mất mạng hoặc ngắt kết nối đột ngột");
        } finally {
            connectedClients.remove(this);
            NumberOfClient--;
            System.out.println("Có " + NumberOfClient + " khách đang kết nối");

        }
    }

    private void handlerRequest(String request) {
        try {
            String[] parts = request.split("===", 2);

            if (parts.length != 2) {
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

                case "GET_ITEMS_BY_CATEGORY":
                    handlerGetItemsByCategory(json);
                    break;

                default:
                    writer.println("UNKNOWN ACTION");
            }
        } catch (Exception e) {
            writer.println("SERVER ERROR");
        }
    }

    // ĐĂNG NHẬP TÀI KHOẢN
    private void handlerLogin(String json) {
        User user;
        if (json.contains("SELLER")) {
            user = gson.fromJson(json, Seller.class);
        } else if (json.contains("BIDDER")) {
            user = gson.fromJson(json, Bidder.class);
        } else {
            user = gson.fromJson(json, Admin.class);
        }
        User dbUser = userRepo.getUserByUsername(user.getUsername());

        if (dbUser != null && dbUser.getPassword().equals(user.getPassword())) {
            currentUser = dbUser;

            // Xác định role
            String role;
            if      (dbUser instanceof Seller) role = "SELLER";
            else if (dbUser instanceof Admin)  role = "ADMIN";
            else                               role = "BIDDER";

            // Trả về role cho client
            JsonObject response = new JsonObject();
            response.addProperty("role",     role);
            response.addProperty("username", dbUser.getUsername());

            writer.println("LOGIN SUCCESS===" + response);
        } else {
            writer.println("LOGIN FAIL");
        }
    }

    // ĐĂNG KÍ TÀI KHOẢN
    private void handlerRegister(String json) {
        User newUser = null;

        if (json.contains("SELLER")) {
            newUser = gson.fromJson(json, Seller.class);
        } else if (json.contains("BIDDER")) {
            newUser = gson.fromJson(json, Bidder.class);
        } else {
            newUser = gson.fromJson(json, Admin.class);
        }

        boolean success = userRepo.registerUser(newUser);

        writer.println(success ? "REGISTER SUCCESS" : "REGISTER FAIL");
    }

    // TẠO SẢN PHẨM
    private void handlerCreateItem(String json) {
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

    //ĐẶT GIÁ
    private void handlePlaceBid(String json) {

        if (!(currentUser instanceof Bidder)) {
            writer.println("ONLY BIDDER CAN BID");
            return;
        }
        PlaceBidRequest req = gson.fromJson(json, PlaceBidRequest.class);
        BidTransaction bid = new BidTransaction(req.auctionId, req.username, req.amount);
        boolean saveBid = bidRepo.insertBid(bid);
        boolean updateAuction = auctionRepo.updateHighestBid(bid.getAuctionId(), bid.getBidAmount(), currentUser.getUsername());

        if (saveBid && updateAuction) {
            writer.println("BID SUCCESS");

            String notification = String.format(
                    "BID_UPDATE==={\"auctionId\":%d,\"bidder\":\"%s\",\"amount\":%.2f}",
                    bid.getAuctionId(),
                    currentUser.getUsername(),
                    bid.getBidAmount()
            );
            System.out.println(notification);

            for (ClientHandler client : connectedClients) {
                if (client != this) { // không gửi lại cho người vừa đặt
                    client.writer.println(notification);
                }
            }

            System.out.println("Broadcast BID_UPDATE đến "
                    + (connectedClients.size() - 1) + " client(s)");

        } else {
            writer.println("BID FAIL");
        }
    }

    //ĐĂNG XUẤT
    private void handleLogout() {
        currentUser = null;
        writer.println("LOGOUT SUCCESS");
    }

    //LẤY DANH SÁCH THEO THƯ MỤC - Minh
    private void handlerGetItemsByCategory(String json) {
        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            String category = obj.get("category").getAsString();
            List<Item> items = itemRepo.getItemsByCategory(category);
            writer.println("ITEMS===" + gson.toJson(items));
        } catch (Exception e) {
            e.printStackTrace();
            writer.println("GET ITEMS FAIL");
        }
    }
};;