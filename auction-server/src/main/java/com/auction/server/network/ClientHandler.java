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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import com.auction.server.network.handler.WalletHandler;
import com.auction.server.repository.DatabaseManager;
import com.auction.server.repository.ReportDaoImpl;
import com.auction.server.repository.IReportDAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

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
    private IReportDAO reportRepo = new ReportDaoImpl();
    private final WalletHandler walletHandler = new WalletHandler();

    private User currentUser;

    public ClientHandler(Socket socket) throws Exception {
        this.socket = socket;
        connectedClients.add(this);
        //Đăng ký observer để nhận BID_UPDATE từ BiddingEngine
        BiddingEngine.getInstance().addObserver(this);
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

    /**
     * Anti-sniping: gửi thông báo gia hạn thời gian đến client.
     */
    @Override
    public void onTimeExtended(int auctionId, java.time.LocalDateTime newEndTime) {
        if (writer != null) {
            String formatted = newEndTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String notification = String.format(
                    "TIME_EXTENDED==={\"auctionId\":%d,\"newEndTime\":\"%s\",\"extensionMinutes\":5}",
                    auctionId, formatted);
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
            //Huỷ đăng ký observer khi client ngắt kết nối tránh memory leak
            BiddingEngine.getInstance().removeObserver(this);
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

                case "GET_ADMIN_AUCTIONS":
                    handleGetAdminAuctions();
                    break;

                case "CANCEL_AUCTION":
                    handleCancelAuction(json);
                    break;

                case "GET_ADMIN_BIDS":
                    handleGetAdminBids();
                    break;

                case "GET_REPORTS":
                    handleGetReports();
                    break;

                case "RESOLVE_REPORT":
                    handleResolveReport(json);
                    break;

                case "SUBMIT_REPORT":
                    handleSubmitReport(json);
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
        if (dbUser == null || !dbUser.getPassword().equals(password)) {
            writer.println("LOGIN FAIL");
            return;
        }
        if ("banned".equalsIgnoreCase(dbUser.getStatus())) {
            writer.println("LOGIN BANNED");
            System.out.println("[Server] Tài khoản bị khóa cố đăng nhập: " + username);
            return;
        }
        currentUser = dbUser;
        writer.println("LOGIN SUCCESS===" + dbUser.getRole());
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

                //LƯU VÀO DTB VÀ RAM
                int auctionId = AuctionManager.getInstance().createAuction(itemId, startTime, endTime);
                if (auctionId > 0) {
                    System.out.println("[Server] Tạo phiên đấu giá id=" + auctionId + " cho item id=" + itemId);
                    Auction newAuction = auctionRepo.getAuctionById(auctionId);
                    String auctionJson = (newAuction != null)
                            ? gson.toJson(toSummaryList(List.of(newAuction)).get(0)) : "{}";
                    writer.println("CREATE_ITEM_SUCCESS===" + auctionJson);

                    if (newAuction != null) {
                        List<AuctionSummary> single = toSummaryList(List.of(newAuction));
                        String notify = "NEW_AUCTION_NOTIFY===" + gson.toJson(single.get(0));
                        for (ClientHandler client : connectedClients) {
                            if (client != this && client.writer != null) {
                                client.writer.println(notify);
                            }
                        }
                    }
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
            if (ok) {
                writer.println(ok ? "BAN_USER===OK" : "BAN_USER===FAIL");
                System.out.println("[Admin] Khóa user: " + username + " → " + (ok ? "OK" : "FAIL"));
                forceLogoutOnlineUser(username);
            } else {
                writer.println("BAN_USER===FAIL");
            }
        } catch (Exception e) {
            writer.println("BAN_USER===FAIL");
        }
    }

    //GỬI LỆNH TỚI KHÁCH
    private void forceLogoutOnlineUser(String username) {
        for (ClientHandler client : connectedClients) {
            if (client != this && client.currentUser != null && username.equals(client.currentUser.getUsername())) {
                client.writer.println("FORCE_LOGOUT===Tài khoản của bạn đã bị khóa bởi quản trị viên.");
                client.currentUser = null;
                System.out.println("[Admin] Đã force-logout user đang online: " + username);
            }
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

    //LẤY PHIÊN ĐẤU GIÁ
    private void handleGetAdminAuctions() {
        if (!(currentUser instanceof Admin)) {
            writer.println("GET_ADMIN_AUCTIONS===[]"); return;
        }
        try {
            List<Auction> auctions = auctionRepo.getAllAuctions();
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            JsonArray arr = new JsonArray();
            for (Auction a : auctions) {
                JsonObject obj = new JsonObject();
                obj.addProperty("auctionId", a.getId());
                obj.addProperty("itemName", a.getItem().getName());
                String sellerUsername = getSellerUsernameByItemId(a.getItem().getId());
                obj.addProperty("sellerUsername", sellerUsername != null ? sellerUsername : "—");
                obj.addProperty("currentHighestBid", a.getCurrentHighestBid());
                if (a.getCurrentWinnerUsername() != null)
                    obj.addProperty("currentWinnerUsername", a.getCurrentWinnerUsername());
                else
                    obj.add("currentWinnerUsername", com.google.gson.JsonNull.INSTANCE);
                obj.addProperty("status", a.getStatus().name());
                obj.addProperty("endTime", a.getEndTime().format(fmt));
                arr.add(obj);
            }
            writer.println("GET_ADMIN_AUCTIONS===" + arr.toString());
        } catch (Exception e) {
            e.printStackTrace();
            writer.println("GET_ADMIN_AUCTIONS===[]");
        }
    }

    // Helper: lấy username của seller theo item_id
    private String getSellerUsernameByItemId(String itemId) {
        String sql = "SELECT u.username FROM users u JOIN Items i ON u.id = i.seller_id WHERE i.id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, itemId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getString("username");
            }
        } catch (Exception e) {
            System.err.println("[ClientHandler] getSellerUsername error: " + e.getMessage());
        }
        return null;
    }

    //HỦY PHIÊN ĐẤU GIÁ
    private void handleCancelAuction(String json) {
        if (!(currentUser instanceof Admin)) {
            writer.println("CANCEL_AUCTION===FAIL"); return;
        }
        try {
            int auctionId = JsonParser.parseString(json).getAsJsonObject().get("auctionId").getAsInt();
            boolean ok = auctionRepo.updateStatus(auctionId, Auction.Status.CANCELED);
            if (ok) {
                // Đồng bộ trạng thái trong RAM (AuctionManager)
                Auction inMem = AuctionManager.getInstance().getAuction(auctionId);
                if (inMem != null) inMem.updateStatus(Auction.Status.CANCELED);
                writer.println("CANCEL_AUCTION===OK");
                System.out.println("[Admin] Hủy phiên #" + auctionId);
            } else {
                writer.println("CANCEL_AUCTION===FAIL");
            }
        } catch (Exception e) {
            e.printStackTrace();
            writer.println("CANCEL_AUCTION===FAIL");
        }
    }

    //THỐNG KÊ DỮ LIỆU CHO ADMIN
    private void handleGetAdminStats() {
        if (!(currentUser instanceof Admin)) {
            writer.println("GET_ADMIN_STATS==={}"); return;
        }
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {

            // 1. Tổng doanh thu: tổng tất cả giao dịch PAYMENT trong wallet_transactions
            double totalRevenue = 0;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COALESCE(SUM(amount), 0) AS total FROM wallet_transactions WHERE type = 'PAYMENT'");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) totalRevenue = rs.getDouble("total");
            }

            // 2. Số phiên đã thanh toán (auction status = FINISHED) và chưa thanh toán (OPEN hoặc RUNNING)
            int paidCount = 0, unpaidCount = 0;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT status, COUNT(*) AS cnt FROM auctions GROUP BY status");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String status = rs.getString("status");
                    int    cnt    = rs.getInt("cnt");
                    if ("FINISHED".equalsIgnoreCase(status)) paidCount   += cnt;
                    else if ("OPEN".equalsIgnoreCase(status) || "RUNNING".equalsIgnoreCase(status)) unpaidCount += cnt;
                }
            }

            // 3. Doanh thu từng tháng trong năm hiện tại
            JsonArray monthlyArr = new JsonArray();
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT MONTH(created_at) AS m, COALESCE(SUM(amount), 0) AS rev " +
                            "FROM wallet_transactions " +
                            "WHERE type = 'PAYMENT' AND YEAR(created_at) = YEAR(CURDATE()) " +
                            "GROUP BY MONTH(created_at) ORDER BY m")) {
                ResultSet rs = ps.executeQuery();
                // Chuẩn bị map tháng → doanh thu (mặc định 0)
                java.util.Map<Integer, Double> map = new java.util.LinkedHashMap<>();
                for (int i = 1; i <= 12; i++) map.put(i, 0.0);
                while (rs.next()) {
                    map.put(rs.getInt("m"), rs.getDouble("rev"));
                }
                // Chỉ xuất các tháng đã có dữ liệu (hoặc đến tháng hiện tại)
                int currentMonth = java.time.LocalDate.now().getMonthValue();
                for (int i = 1; i <= currentMonth; i++) {
                    JsonObject mo = new JsonObject();
                    mo.addProperty("month", "Th." + i);
                    mo.addProperty("revenue", map.get(i));
                    monthlyArr.add(mo);
                }
            }

            JsonObject result = new JsonObject();
            result.addProperty("totalRevenue", totalRevenue);
            result.addProperty("paidCount",    paidCount);
            result.addProperty("unpaidCount",  unpaidCount);
            result.add("monthlyRevenue", monthlyArr);

            writer.println("GET_ADMIN_STATS===" + result);
            System.out.println("[Admin] Stats: revenue=" + totalRevenue
                    + " paid=" + paidCount + " unpaid=" + unpaidCount);

        } catch (Exception e) {
            e.printStackTrace();
            writer.println("GET_ADMIN_STATS==={}");
        }
    }

    //LỊCH SỬ ĐẶT GIÁ
    private void handleGetAdminBids() {
        if (!(currentUser instanceof Admin)) {
            writer.println("GET_ADMIN_BIDS===[]"); return;
        }
        try {
            List<com.auction.server.model.BidTransaction> bids = bidRepo.getAllBids();
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
            JsonArray arr = new JsonArray();
            for (com.auction.server.model.BidTransaction b : bids) {
                JsonObject obj = new JsonObject();
                obj.addProperty("transactionId", b.getTransactionId());
                obj.addProperty("auctionId", b.getAuctionId());
                obj.addProperty("bidderUsername", b.getBidderUsername());
                obj.addProperty("bidAmount", b.getBidAmount());
                obj.addProperty("timestamp", b.getTimestamp().format(fmt));
                arr.add(obj);
            }
            writer.println("GET_ADMIN_BIDS===" + arr.toString());
        } catch (Exception e) {
            e.printStackTrace();
            writer.println("GET_ADMIN_BIDS===[]");
        }
    }

    //BÁO CÁO VI PHẠM
    private void handleGetReports() {
        if (!(currentUser instanceof Admin)) {
            writer.println("GET_REPORTS===[]"); return;
        }
        try {
            List<IReportDAO.ReportInfo> reports = reportRepo.getAllReports();
            JsonArray arr = new JsonArray();
            for (IReportDAO.ReportInfo r : reports) {
                JsonObject obj = new JsonObject();
                obj.addProperty("id", r.id);
                obj.addProperty("reporterUsername", r.reporterUsername);
                obj.addProperty("targetUsername", r.targetUsername);
                obj.addProperty("reason", r.reason);
                obj.addProperty("createdAt", r.createdAt);
                obj.addProperty("status", r.status);
                arr.add(obj);
            }
            writer.println("GET_REPORTS===" + arr.toString());
        } catch (Exception e) {
            e.printStackTrace();
            writer.println("GET_REPORTS===[]");
        }
    }

    //XỬ LÝ BÁO CÁO
    private void handleResolveReport(String json) {
        if (!(currentUser instanceof Admin)) {
            writer.println("RESOLVE_REPORT===FAIL"); return;
        }
        try {
            int reportId = JsonParser.parseString(json).getAsJsonObject().get("reportId").getAsInt();
            boolean ok = reportRepo.resolveReport(reportId);
            writer.println(ok ? "RESOLVE_REPORT===OK" : "RESOLVE_REPORT===FAIL");
            System.out.println("[Admin] Xử lý báo cáo #" + reportId + " → " + (ok ? "OK" : "FAIL"));
        } catch (Exception e) {
            e.printStackTrace();
            writer.println("RESOLVE_REPORT===FAIL");
        }
    }

    //GỬI BÁO CÁO
    private void handleSubmitReport(String json) {
        if (currentUser == null) {
            writer.println("SUBMIT_REPORT===FAIL===Chưa đăng nhập"); return;
        }
        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            String targetUsername = obj.get("targetUsername").getAsString();
            String reason         = obj.get("reason").getAsString();
            int newId = reportRepo.insertReport(currentUser.getUsername(), targetUsername, reason);
            if (newId > 0) {
                writer.println("SUBMIT_REPORT===OK===" + newId);
                System.out.println("[Report] " + currentUser.getUsername()
                        + " báo cáo " + targetUsername + " → id=" + newId);
            } else {
                writer.println("SUBMIT_REPORT===FAIL===DB_ERROR");
            }
        } catch (Exception e) {
            e.printStackTrace();
            writer.println("SUBMIT_REPORT===FAIL===SERVER_ERROR");
        }
    }


}