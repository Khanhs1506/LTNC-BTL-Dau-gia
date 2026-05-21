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
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
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
        connectedClients.add(this);
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
            connectedClients.remove(this);
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

                case "CREATE_ITEM":
                    handlerCreateItem(json);
                    break;

                case "LOGOUT":
                    handleLogout();
                    break;

                case "GET_ITEMS_BY_CATEGORY":
                    handlerGetItemsByCategory(json);
                    break;

                case "GET_AUCTIONS":
                    handlerGetAuctions();
                    break;

                case "GET_AUCTIONS_BY_SELLER":
                    handleGetAuctionsBySeller();
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
        JsonObject object = JsonParser.parseString(json).getAsJsonObject();
        String username = object.get("username").getAsString();
        String password = object.get("password").getAsString();
        User dbUser = userRepo.getUserByUsername(username);
        if (dbUser != null && dbUser.getPassword().equals(password)) {
            currentUser = dbUser;
            String role = dbUser.getRole();
            writer.println("LOGIN SUCCESS===" + role);
        } else {
            writer.println("LOGIN FAIL");
        }
    }

    // ĐĂNG KÍ TÀI KHOẢN
    private void handlerRegister(String json) { //sau đổi String thành User
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

            case "GET_ITEMS_BY_CATEGORY":
                handlerGetItemsByCategory(json);
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
        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            String name = obj.get("name").getAsString();
            String itemType = obj.get("itemType").getAsString();
            double startPrice = obj.get("startingPrice").getAsDouble();

            //TẠO SẢN PHẨM ĐÚNG LOẠI
            Item item;
            switch (itemType) {
                case "ART" -> {
                    String artist = obj.has("artist") ? obj.get("artist").getAsString() : "";
                    item = new ArtItem(null, name, startPrice, artist);
                }
                case "ELECTRONICS" -> {
                    int warranty = obj.has("warrantyMonths") ? obj.get("warrantyMonths").getAsInt() : 0;
                    item = new ElectronicsItem(null, name, startPrice, warranty);
                }
                case "VEHICLE" -> {
                    String brand = obj.has("brand") ? obj.get("brand").getAsString() : "";
                    int year = obj.has("year")  ? obj.get("year").getAsInt() : 0;
                    item = new VehicleItem(null, name, startPrice, brand, year);
                }
                default -> {
                    // "OTHER" → dùng ArtItem với artist rỗng làm fallback
                    item = new ArtItem(null, name, startPrice, "");
                }
            }

            int itemId = itemRepo.insertItem(item, currentUser.getId());

            if (itemId > 0) {
                System.out.println("[Server] Seller \"" + currentUser.getUsername() + "\" tạo sản phẩm \"" + name + "\" (id=" + itemId + ")");
                writer.println("CREATE_ITEM_SUCCESS");
            } else {
                writer.println("CREATE_ITEM_FAIL");
            }

        } catch (Exception e) {
            e.printStackTrace();
            writer.println("CREATE_ITEM_FAIL");
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
        PlacedBidResquest res = gson.fromJson(json, PlacedBidResquest.class);
        BidTransaction bid = new BidTransaction(res.auctionId, res.username, res.amount);
        boolean saveBid = bidRepo.insertBid(bid);
        boolean updateAuction = auctionRepo.updateHighestBid(bid.getAuctionId(), bid.getBidAmount(), currentUser.getUsername());

        if (saveBid && updateAuction){
            writer.println("BID SUCCESS");
            String notification = String.format(
                    "BID_UPDATE==={\"auctionId\":%d,\"bidder\":\"%s\",\"amount\":%.2f}",
                    bid.getAuctionId(),
                    currentUser.getUsername(),
                    bid.getBidAmount()
            );
            for (ClientHandler client : connectedClients) {
                if (client != this) { // không gửi lại cho người vừa đặt
                    client.writer.println(notification);
                }
            }
            System.out.println("BID_UPDATE đến "
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

    //LẤY PHIÊN ĐẤU GIÁ
    private void handlerGetAuctions() {
        List<Auction> auctions = auctionRepo.getAllAuctions();
        writer.println("AUCTIONS===" + gson.toJson(toSummaryList(auctions)));
    }

    //LẤY PHIÊN ĐẤU GIÁ THEO ID SELLER
    private void handleGetAuctionsBySeller() {
        if (!(currentUser instanceof Seller)) {
            writer.println("ONLY_SELLER_ERROR");
            return;
        }

        List<Auction> auctions = auctionRepo.getAuctionsBySellerId(currentUser.getId());
        writer.println("AUCTIONS===" + gson.toJson(toSummaryList(auctions)));
    }

    //CHUYỂN TỪ AUCTION SANG AUCTIONSUMMARY
    private List<AuctionSummary> toSummaryList(List<Auction> auctions) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        List<AuctionSummary> summaries = new java.util.ArrayList<>();
        for (Auction a : auctions) {
            AuctionSummary s       = new AuctionSummary();
            s.auctionId             = a.getId();
            s.itemId                = a.getItem().getId();
            s.itemName              = a.getItem().getName();
            s.itemType              = a.getItem().getType_item();
            s.startingPrice         = a.getItem().getStartingPrice();
            s.currentHighestBid     = a.getCurrentHighestBid();
            s.currentWinnerUsername = a.getCurrentWinnerUsername();
            s.startTime             = a.getStartTime().format(fmt);
            s.endTime               = a.getEndTime().format(fmt);
            s.status                = a.getStatus().name();
            summaries.add(s);
        }
        return summaries;
    }

}


