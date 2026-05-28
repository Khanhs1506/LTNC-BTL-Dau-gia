package sample;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import sample.model.PlacedBidRequest;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.LinkedBlockingDeque;

public class ServerConnection {
    private static final String HOST = "localhost";
    private static final int PORT = 9999;
    private static ServerConnection instance;

    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;
    private final LinkedBlockingDeque<String> responseQueue = new LinkedBlockingDeque<>();

    private ServerConnection() throws Exception {
        socket = new Socket(HOST, PORT);
        writer = new PrintWriter(socket.getOutputStream(), true);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        startListenerThread();
    }

    public static ServerConnection getInstance() throws Exception {
        if (instance == null || instance.isClosed()) {
            instance = new ServerConnection();
        }
        return instance;
    }

    public boolean isClosed() {
        return socket == null || socket.isClosed();
    }

    public synchronized String sendRequest(String action, String json) throws Exception {
        writer.println(action + "===" + json);
        return responseQueue.take();
    }

    //ĐĂNG NHẬP
    public String login(String username, String password) throws Exception {
        String json = String.format("{\"username\":\"%s\",\"password\":\"%s\"}",
                username, password);
        return sendRequest("LOGIN", json);
    }

    //ĐĂNG KÍ
    public String register(String username, String password, String role) throws Exception {
        String json = String.format("{\"username\":\"%s\",\"password\":\"%s\",\"role\":\"%s\"}",
                username, password, role);
        return sendRequest("REGISTER", json);
    }

    //ĐẶT GIÁ
    public String placeBid(int auctionId, String username, double amount) throws Exception {
        String json = String.format("{\"auctionId\":%d,\"username\":\"%s\",\"amount\":%.2f}",
                auctionId, username, amount);
        return sendRequest("PLACE_BID", json);
    }

    //THÊM SẢN PHẨM
    public String createItem(AuctionItemDTO dto) throws Exception {
        LocalDateTime startTime = dto.startTime != null ? dto.startTime : LocalDateTime.now();
        LocalDateTime endTime = dto.endTime != null ? dto.endTime : LocalDateTime.now().plusDays(7);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        String json = String.format("{\"name\":\"%s\",\"itemType\":\"%s\",\"startingPrice\":%.2f," +
                        "\"description\":\"%s\",\"warrantyMonths\":%d," +
                        "\"artist\":\"%s\",\"brand\":\"%s\",\"year\":%d," +
                        "\"startTime\":\"%s\",\"endTime\":\"%s\"}",
                escape(dto.title),
                mapCategoryToItemType(dto.category),
                dto.startingPrice,
                escape(dto.description),
                dto.warrantyMonths,
                escape(dto.artist),
                escape(dto.brand),
                dto.year,
                startTime.format(fmt),
                endTime.format(fmt));
        return sendRequest("CREATE_ITEM", json);
    }

    // XÓA SẢN PHẨM
    public String deleteItem(int itemId) throws Exception {
        String json = String.format("{\"itemId\":%d}", itemId);
        return sendRequest("DELETE_ITEM", json);
    }

    //LẤY PHIÊN ĐẤU GIÁ
    public String getAuctions() throws Exception {
        return sendRequest("GET_AUCTIONS", "{}");
    }

    //LẤY PHIÊN ĐẤU GIÁ SELLER ĐANG ĐĂNG NHẬP
    public String getAuctionsBySeller() throws Exception {
        return sendRequest("GET_AUCTIONS_BY_SELLER", "{}");
    }

    //LẤY CÁC PHIÊN ĐÃ THANH TOÁN CỦA SELLER
    public String getSellerPaidAuctions() throws Exception {
        return sendRequest("GET_SELLER_PAID_AUCTIONS", "{}");
    }

    public String markAuctionPaid(int auctionId) throws Exception {
        String json = String.format("{\"auctionId\":%d}", auctionId);
        return sendRequest("MARK_AUCTION_PAID", json);
    }

    //LẤY LỊCH SỬ ĐẶT GIÁ
    public String getBidHistory(int auctionId) throws Exception {
        String json = String.format("{\"auctionId\":%d}", auctionId);
        return sendRequest("GET_BID_HISTORY", json);
    }

    public String logout() throws Exception {
        return sendRequest("LOGOUT", "");
    }

    // ADMIN: lấy danh sách user thật
    public String getUsers() throws Exception {
        return sendRequest("GET_USERS", "{}");
    }

    // ADMIN: khóa user
    public String banUser(String username) throws Exception {
        String json = String.format("{\"username\":\"%s\"}", username);
        return sendRequest("BAN_USER", json);
    }

    // ADMIN: mở khóa user
    public String unbanUser(String username) throws Exception {
        String json = String.format("{\"username\":\"%s\"}", username);
        return sendRequest("UNBAN_USER", json);
    }

    //ADMIN: lấy phiên đấu giá
    public String getAdminAuctions() throws Exception {
        return sendRequest("GET_ADMIN_AUCTIONS", "{}");
    }

    //hủy phiên
    public String cancelAuction(int auctionId) throws Exception {
        String json = String.format("{\"auctionId\":%d}", auctionId);
        return sendRequest("CANCEL_AUCTION", json);
    }

    //thống kê cho admin
    public String getAdminStats() throws Exception {
        return sendRequest("GET_ADMIN_STATS", "{}");
    }

    //lịch sử đặt giá
    public String getAdminBids() throws Exception {
        return sendRequest("GET_ADMIN_BIDS", "{}");
    }

    //báo cáo vi phạm
    public String getReports() throws Exception {
        return sendRequest("GET_REPORTS", "{}");
    }

    //báo cáo đã xử lí
    public String resolveReport(String reportId) throws Exception {
        String json = String.format("{\"reportId\":%s}", reportId);
        return sendRequest("RESOLVE_REPORT", json);
    }

    //người dùng báo cáo
    public String submitReport(String targetUsername, String reason) throws Exception {
        String json = String.format("{\"targetUsername\":\"%s\",\"reason\":\"%s\"}",
                escape(targetUsername), escape(reason));
        return sendRequest("SUBMIT_REPORT", json);
    }

    public void disconnect() {
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getItemsByCategory(String category) throws Exception {
        String json = String.format("{\"category\":\"%s\"}", category);
        return sendRequest("GET_ITEMS_BY_CATEGORY", json);
    }

    private void startListenerThread() {
        Thread t = new Thread(() -> {
            try {
                Gson gson = new Gson();
                String line;
                while ((line = reader.readLine()) != null) {

                    if (line.startsWith("BID_UPDATE===")) {
                        String json = line.split("===")[1];
                        PlacedBidRequest res = gson.fromJson(json, PlacedBidRequest.class);
                        String msg = "🔔 " + res.bidder + " vừa đặt giá " + formatVND(res.amount);
                        javafx.application.Platform.runLater(() -> {
                            NotificationManager.getInstance().addNotification(msg);
                            NotificationManager.getInstance().notifyBidUpdate(res);
                        });

                    } else if (line.startsWith("DELETE_ITEM_NOTIFY===")) {
                        String json = line.split("===", 2)[1];
                        int deletedItemId = gson.fromJson(json, JsonObject.class).get("itemId").getAsInt();
                        javafx.application.Platform.runLater(() -> {
                            HomeController home = HomeController.getInstance();
                            if (home != null) {
                                home.removeAuctionCard(deletedItemId); // xóa card khỏi UI
                            }
                            NotificationManager.getInstance().addNotification("🗑️ Một sản phẩm vừa bị xóa khỏi danh sách");
                        });
                    } else if (line.startsWith("TIME_EXTENDED===")) {
                        // Anti-sniping: server đã gia hạn thời gian phiên đấu giá
                        String json = line.split("===", 2)[1];
                        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
                        int auctionId = obj.get("auctionId").getAsInt();
                        String newEndTimeStr = obj.get("newEndTime").getAsString();
                        int extensionMinutes = obj.has("extensionMinutes") ? obj.get("extensionMinutes").getAsInt() : 5;

                        java.time.LocalDateTime newEndTime = java.time.LocalDateTime.parse(
                                newEndTimeStr,
                                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

                        NotificationManager.TimeExtendedEvent event =
                                new NotificationManager.TimeExtendedEvent(auctionId, newEndTime, extensionMinutes);

                        javafx.application.Platform.runLater(() -> {
                            NotificationManager.getInstance().notifyTimeExtended(event);
                            NotificationManager.getInstance().addNotification(
                                    "⏰ Phiên đấu giá #" + auctionId + " được gia hạn thêm " + extensionMinutes + " phút!");
                        });

                    } else if (line.startsWith("NEW_AUCTION_NOTIFY===")) {
                        String json = line.split("===", 2)[1];
                        javafx.application.Platform.runLater(() -> {
                            HomeController home = HomeController.getInstance();
                            if (home != null) {
                                home.addNewAuctionCard(json); // thêm card trực tiếp, không reload
                            }
                            NotificationManager.getInstance().addNotification(
                                    "🆕 Phiên đấu giá mới vừa được mở!"
                            );
                        });
                    } else if (line.startsWith("FORCE_LOGOUT===")) {
                        String reason = line.split("===",2)[1];
                        Platform.runLater(() -> {
                            UserSession.getInstance().logout();
                            Alert alert = new Alert(Alert.AlertType.WARNING);
                            alert.setTitle("Tài khoản bị khóa");
                            alert.setHeaderText("Bạn đã bị đăng xuất");
                            alert.setContentText(reason);
                            alert.showAndWait();
                            try {
                                java.net.URL fxmlUrl = HomeController.class.getResource("/sample/home_demo.fxml");
                                if (fxmlUrl == null) fxmlUrl = HomeController.class.getResource("home.fxml");
                                if (fxmlUrl != null) {
                                    javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(fxmlUrl);
                                    javafx.scene.Parent root = loader.load();
                                    HomeController homeCtrl = loader.getController();
                                    homeCtrl.resetToGuest();
                                    // Tìm stage hiện tại từ bất kỳ node nào
                                    javafx.stage.Stage stage = (javafx.stage.Stage)
                                            javafx.stage.Stage.getWindows().stream()
                                                    .filter(w -> w instanceof javafx.stage.Stage && w.isShowing())
                                                    .findFirst().orElse(null);
                                    if (stage != null) {
                                        stage.setScene(new javafx.scene.Scene(root, 1200, 800));
                                        stage.setTitle("TINY HOARDER'S KEY MARKET");
                                        stage.centerOnScreen();
                                    }
                                }
                            } catch (Exception ex) { ex.printStackTrace(); }
                        });
                    } else {
                        // Response thông thường — sendRequest() đang chờ
                        responseQueue.put(line);
                    }
                }
            } catch (Exception e) {
                System.out.println("Mất kết nối server: " + e.getMessage());
            }
        }, "ServerListener");
        t.setDaemon(true);
        t.start();
    }

    private String formatVND(double amount) {
        return String.format("%,.0f VNĐ", amount);
    }

    //PHÂN LOẠI SẢN PHẨM
    private String mapCategoryToItemType(String category) {
        if (category == null) return "OTHER";
        return switch (category) {
            case "ArtItem" -> "ART";
            case "ElectronicsItem" -> "ELECTRONICS";
            case "VehicleItem" -> "VEHICLE";
            default -> "OTHER";
        };
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "");
    }

    //LẤY SỐ DƯ HIỆN TẠI
    public String getBalance() throws Exception {
        return sendRequest("GET_BALANCE", "{}");
    }

    public String deposit(double amount, String note) throws Exception {
        String json = String.format("{\"amount\":%.2f,\"note\":\"%s\"}", amount, escape(note));
        return sendRequest("DEPOSIT", json);
    }

    public String bidHold(int auctionId, double amount) throws Exception {
        String json = String.format("{\"auctionId\":\"%d\",\"amount\":%.2f}", auctionId, amount);
        return sendRequest("BID_HOLD", json);
    }

    public String bidRelease(int auctionId, double amount) throws Exception {
        String json = String.format("{\"auctionId\":%d,\"amount\":%.2f}", auctionId, amount);
        return sendRequest("BID_RELEASE", json);
    }

    public String payment(int auctionId, double amount) throws Exception {
        String json = String.format("{\"auctionId\":%d,\"amount\":%.2f}", auctionId, amount);
        return sendRequest("PAYMENT", json);
    }

    public String refund(int auctionId, double amount) throws Exception {
        String json = String.format("{\"auctionId\":%d,\"amount\":%.2f}", auctionId, amount);
        return sendRequest("REFUND", json);
    }

    public String getTransactionHistory(int limit) throws Exception {
        String json = String.format("{\"limit\":%d}", limit);
        return sendRequest("GET_TX_HISTORY", json);
    }

    public String addFavorite(int auctionId) throws Exception {
        String json = String.format("{\"auctionId\":%d}", auctionId);
        return sendRequest("ADD_FAVORITE", json);
    }

    public String removeFavorite(int auctionId) throws Exception {
        String json = String.format("{\"auctionId\":%d}", auctionId);
        return sendRequest("REMOVE_FAVORITE", json);
    }

    public String getFavorites() throws Exception {
        return sendRequest("GET_FAVORITES", "{}");
    }

    public String registerAutoBid(int auctionId, double maxBid, double increment, int minutesTrigger) throws Exception {
        String json = String.format("{\"auctionId\":%d,\"maxBid\":%.2f,\"increment\":%.2f,\"minutesTrigger\":%d}",
                auctionId, maxBid, increment, minutesTrigger);
        return sendRequest("REGISTER_AUTO_BID", json);
    }

    public String cancelAutoBid(int auctionId) throws Exception {
        String json = String.format("{\"auctionId\":%d}", auctionId);
        return sendRequest("CANCEL_AUTO_BID", json);
    }

    public String getAutoBid(int auctionId) throws Exception {
        String json = String.format("{\"auctionId\":%d}", auctionId);
        return sendRequest("GET_AUTO_BID", json);
    }
}