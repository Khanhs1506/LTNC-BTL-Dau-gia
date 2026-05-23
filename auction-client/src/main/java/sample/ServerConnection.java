package sample;

import com.google.gson.Gson;
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
        LocalDateTime endTime   = dto.endTime   != null ? dto.endTime   : LocalDateTime.now().plusDays(7);
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


    public String getItems() throws Exception {
        return sendRequest("GET_ITEMS", "{}");
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

    //LẤY LỊCH SỬ ĐẶT GIÁ
    public String getBidHistory(int auctionId) throws Exception {
        String json = String.format("{\"auctionId\":%d}", auctionId);
        return sendRequest("GET_BID_HISTORY", json);
    }

    public String logout() throws Exception {
        return sendRequest("LOGOUT", "{}");
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
                        // Parse JSON: {"auctionId":1,"bidder":"Alice","amount":250000000.00}
                        String json = line.split("===")[1];
                        PlacedBidRequest res = gson.fromJson(json, PlacedBidRequest.class);
                        String msg = "🔔 " + res.bidder + " vừa đặt giá " + formatVND(res.amount);

                        // Thêm vào NotificationManager (nó sẽ gọi callback của HomeController)
                        javafx.application.Platform.runLater(() ->
                                NotificationManager.getInstance().addNotification(msg)
                        );

                    } else if (line.startsWith("NOTIFY===")) {
                        String[] parts = line.split("===");
                        javafx.application.Platform.runLater(() -> {
                            switch (parts[1]) {
                                case "BID_REFUND"  -> WalletController.notifyAuctionLost(
                                        parts[2], Double.parseDouble(parts[3]));
                                case "AUCTION_WON" -> WalletController.notifyAuctionWon(parts[2], 0);
                            }
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
        t.setDaemon(true); // tự tắt khi app đóng
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

    // ── Ví / Wallet ────────────────────────────────────────────────────
    public String getWallet() throws Exception {
        return sendRequest("GET_WALLET", "{}");
    }

    public String deposit(double amount, String paymentMethod) throws Exception {
        String json = String.format(
                "{\"amount\":%.2f,\"paymentMethod\":\"%s\"}", amount, paymentMethod);
        return sendRequest("DEPOSIT", json);
    }

    public String bidHold(String auctionId, double bidAmount,
                          double depositAmount) throws Exception {
        String json = String.format(
                "{\"auctionId\":\"%s\",\"bidAmount\":%.2f,\"depositAmount\":%.2f}",
                auctionId, bidAmount, depositAmount);
        return sendRequest("BID_HOLD", json);
    }

    public String getTransactions(String type, String status,
                                  String dateFrom, String dateTo,
                                  int page, int pageSize) throws Exception {
        String json = String.format(
                "{\"type\":\"%s\",\"status\":\"%s\",\"dateFrom\":\"%s\"," +
                        "\"dateTo\":\"%s\",\"page\":%d,\"pageSize\":%d}",
                type     != null ? type     : "ALL",
                status   != null ? status   : "ALL",
                dateFrom != null ? dateFrom : "",
                dateTo   != null ? dateTo   : "",
                page, pageSize);
        return sendRequest("GET_TRANSACTIONS", json);
    }
}