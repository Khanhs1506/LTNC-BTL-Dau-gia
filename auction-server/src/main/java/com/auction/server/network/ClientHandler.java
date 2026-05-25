package com.auction.server.network;

import com.auction.server.exception.AuctionClosedException;
import com.auction.server.exception.InvalidBidException;
import com.auction.server.model.*;
import com.auction.server.repository.*;
import com.auction.server.service.AuctionManager;
import com.auction.server.service.AuctionObserver;
import com.auction.server.service.BiddingEngine;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import com.auction.server.network.handler.WalletHandler;

public class ClientHandler implements Runnable, AuctionObserver {

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
    private final WalletHandler walletHandler = new WalletHandler();

    private User currentUser;

    public ClientHandler(Socket socket) throws Exception {
        this.socket = socket;
        connectedClients.add(this);
    }

    @Override
    public void onNewBidPlaced(int auctionId, String bidderUsername, double newBidAmount) {
        if (writer != null) {
            String notification = String.format(
                    "BID_UPDATE==={\"auctionId\":%d,\"bidder\":\"%s\",\"amount\":%.2f}",
                    auctionId, bidderUsername, newBidAmount);
            writer.println(notification);
        }
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

                case "CREATE_ITEM":
                    handlerCreateItem(json);
                    break;

                case "DELETE_ITEM":
                    handlerDeleteItem(json);
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

                case "GET_BID_HISTORY":
                    handleGetBidHistory(json);
                    break;


                case "GET_USERS":
                    handleGetUsers();
                    break;

                case "BAN_USER":
                    handleBanUser(json);
                    break;

                case "UNBAN_USER":
                    handleUnbanUser(json);
                    break;

                case "GET_BALANCE":
                case "DEPOSIT":
                case "PAYMENT":
                case "REFUND":
                case "BID_HOLD":
                case "BID_RELEASE":
                case "GET_TX_HISTORY":
                    handleWallet(action, json);
                    break;

                default:
                    writer.println("UNKNOWN ACTION");
            }
        } catch (Exception e) {
            e.printStackTrace();
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

        if (newUser == null) {
            writer.println("REGISTER FAIL");
            return;
        }
        boolean success = userRepo.registerUser(newUser);
        writer.println(success ? "REGISTER SUCCESS" : "REGISTER FAIL");
    }

    //GỬI TỚI CHO WALLETHANDLER XỬ LÍ
    private void handleWallet(String command, String json) {
        if (currentUser == null) {
            writer.println("ERROR===Chưa đăng nhập");
            return;
        }
        String userId = String.valueOf(currentUser.getId());
        String result = walletHandler.handle(command, userId, json);
        writer.println("WALLET_" + command + "===" + result);
        System.out.println("WALLET_" + command + "===" + result);
    }

    // TẠO SẢN PHẨM
    private void handlerCreateItem(String json) {

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
                    int year = obj.has("year") ? obj.get("year").getAsInt() : 0;
                    item = new VehicleItem(null, name, startPrice, brand, year);
                }
                default -> {
                    // "OTHER" → dùng ArtItem với artist rỗng làm fallback
                    item = new ArtItem(null, name, startPrice, "");
                }
            }

            int itemId = itemRepo.insertItem(item, currentUser.getId());

            if (itemId > 0) {
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                LocalDateTime startTime = obj.has("startTime") && !obj.get("startTime").getAsString().isBlank()
                        ? LocalDateTime.parse(obj.get("startTime").getAsString(), fmt)
                        : LocalDateTime.now();

                LocalDateTime endTime = obj.has("endTime") && !obj.get("endTime").getAsString().isBlank()
                        ? LocalDateTime.parse(obj.get("endTime").getAsString(), fmt)
                        : LocalDateTime.now().plusDays(7);

                //LƯU VÀO DB VÀ RAM
                int auctionId = AuctionManager.getInstance().createAuction(itemId, startTime, endTime);
                if (auctionId > 0) {
                    System.out.println("[Server] Tạo phiên đấu giá id=" + auctionId + " cho item id=" + itemId);
                    writer.println("CREATE_ITEM_SUCCESS");
                } else {
                    writer.println("CREATE_ITEM_FAIL");
                }
            } else {
                writer.println("CREATE_ITEM_FAIL");
            }

        } catch (Exception e) {
            e.printStackTrace();
            writer.println("CREATE_ITEM_FAIL");
        }
    }

    //XÓA SẢN PHẨM
    private void handlerDeleteItem(String json) {
        if (!(currentUser instanceof Seller)) {
            writer.println("ONLY_SELLER CAN DELETE");
            return;
        }
        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            int itemId = obj.get("itemId").getAsInt();
            boolean success = AuctionManager.getInstance().deleteItemAndAuction(itemId, itemRepo);
            if (success) {
                writer.println("DELETE_ITEM_SUCCESS");
                String notification = String.format("DELETE_ITEM_NOTIFY==={\"itemId\":%d}", itemId);
                for (ClientHandler client : connectedClients) {
                    if (client != this) client.writer.println(notification);
                }
                System.out.println("[Server] Seller \"" + currentUser.getUsername() + "\" đã xóa item id=" + itemId);
            } else {
                writer.println("DELETE_ITEM_FAIL===DB_ERROR");
            }
        } catch (Exception e) {
            e.printStackTrace();
            writer.println("DELETE_ITEM_FAIL===SERVER_ERROR");
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

        try {
            PlacedBidResquest res = gson.fromJson(json, PlacedBidResquest.class);
            boolean success = BiddingEngine.getInstance().processBid(res.auctionId, currentUser.getUsername(), res.amount);

            if (success) {
                writer.println("BID SUCCESS");
            } else {
                writer.println("BID FAIL");
            }
        } catch (AuctionClosedException e) {
            writer.println("BID_FAIL===AUCTION_CLOSED");
        } catch (InvalidBidException e) {
            writer.println("BID_FAIL===INVALID_BID: " + e.getMessage());
        } catch (Exception e) {
            writer.println("BID_FAIL===" + e.getMessage());
        }
    }

    //ĐĂNG XUẤT
    private void handleLogout() {
        currentUser = null;
        writer.println("LOGOUT SUCCESS");
    }

    //LẤY LỊCH SỬ ĐẶT GIÁ
    private void handleGetBidHistory(String json) {
        try {
            int auctionId = JsonParser.parseString(json).getAsJsonObject().get("auctionId").getAsInt();
            List<BidTransaction> bids = bidRepo.getBidsByAuctionId(auctionId);
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");
            JsonArray arr = new JsonArray();
            for (BidTransaction b : bids) {
                JsonObject item = new JsonObject();
                item.addProperty("bidder", b.getBidderUsername());
                item.addProperty("amount", b.getBidAmount());
                item.addProperty("time",   b.getTimestamp().format(fmt));
                arr.add(item);
            }
            writer.println("BID_HISTORY===" + arr);
        } catch (Exception e) {
            e.printStackTrace();
            writer.println("BID_HISTORY===[]");
        }
    }

    //LẤY DANH SACH NGƯỜI DÙNG
    private void handleGetUsers() {
        if (!(currentUser instanceof Admin)) {
            writer.println("GET_USERS===[]"); return;
        }
        try {
            List<UserDaoImpl.UserInfo> users = ((UserDaoImpl) userRepo).getAllUserInfos();
            writer.println("GET_USERS===" + gson.toJson(users));
        } catch (Exception e) {
            e.printStackTrace();
            writer.println("GET_USERS===[]");
        }
    }

    //BAN NGƯỜI DÙNG
    private void handleBanUser(String json) {
        if (!(currentUser instanceof Admin)) { writer.println("BAN_USER===FAIL"); return; }
        try {
            String username = JsonParser.parseString(json).getAsJsonObject().get("username").getAsString();
            boolean ok = userRepo.setUserStatus(username, "banned");
            writer.println(ok ? "BAN_USER===OK" : "BAN_USER===FAIL");
            System.out.println("[Admin] Khóa user: " + username + " → " + (ok ? "OK" : "FAIL"));
        } catch (Exception e) {
            writer.println("BAN_USER===FAIL");
        }
    }

    //BỎ BAN NGƯỜI DÙNG
    private void handleUnbanUser(String json) {
        if (!(currentUser instanceof Admin)) { writer.println("UNBAN_USER===FAIL"); return; }
        try {
            String username = JsonParser.parseString(json).getAsJsonObject().get("username").getAsString();
            boolean ok = userRepo.setUserStatus(username, "active");
            writer.println(ok ? "UNBAN_USER===OK" : "UNBAN_USER===FAIL");
            System.out.println("[Admin] Mở khóa user: " + username + " → " + (ok ? "OK" : "FAIL"));
        } catch (Exception e) {
            writer.println("UNBAN_USER===FAIL");
        }
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
            AuctionSummary s = new AuctionSummary();
            s.auctionId = a.getId();
            s.itemId = a.getItem().getId();
            s.itemName = a.getItem().getName();
            s.itemType = a.getItem().getType_item();
            s.startingPrice = a.getItem().getStartingPrice();
            s.currentHighestBid = a.getCurrentHighestBid();
            s.currentWinnerUsername = a.getCurrentWinnerUsername();
            s.startTime = a.getStartTime().format(fmt);
            s.endTime = a.getEndTime().format(fmt);
            s.status = a.getStatus().name();
            summaries.add(s);
        }
        return summaries;
    }
}
//    // ── Ví / Wallet ────────────────────────────────────────────────────
//    private void handleGetWallet() {
//        if (currentUser == null) { writer.println("ERROR===Chưa đăng nhập"); return; }
//        try {
//            Connection conn = DatabaseManager.getInstance().getConnection();
//            writer.println(server.WalletHandler.handleGetWallet(currentUser.getUsername(), conn));
//        } catch (Exception e) { writer.println("ERROR===" + e.getMessage()); }
//    }
//
//    private void handleDeposit(String json) {
//        if (currentUser == null) { writer.println("ERROR===Chưa đăng nhập"); return; }
//        try {
//            Connection conn = DatabaseManager.getInstance().getConnection();
//            writer.println(server.WalletHandler.handleDeposit(currentUser.getUsername(), json, conn));
//        } catch (Exception e) { writer.println("ERROR===" + e.getMessage()); }
//    }
//
//    private void handleBidHold(String json) {
//        if (!(currentUser instanceof Bidder)) { writer.println("ERROR===Chỉ Bidder mới đặt cọc"); return; }
//        try {
//            Connection conn = DatabaseManager.getInstance().getConnection();
//            writer.println(server.WalletHandler.handleBidHold(currentUser.getUsername(), json, conn));
//        } catch (Exception e) { writer.println("ERROR===" + e.getMessage()); }
//    }
//
//    private void handleGetTransactions(String json) {
//        if (currentUser == null) { writer.println("ERROR===Chưa đăng nhập"); return; }
//        try {
//            Connection conn = DatabaseManager.getInstance().getConnection();
//            writer.println(server.WalletHandler.handleGetTransactions(currentUser.getUsername(), json, conn));
//        } catch (Exception e) { writer.println("ERROR===" + e.getMessage()); }
//    }
//}


