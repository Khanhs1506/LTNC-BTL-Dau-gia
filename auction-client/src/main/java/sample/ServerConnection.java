package sample;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ServerConnection {
    private static final String HOST = "localhost";
    private static final int PORT = 9999;
    private static ServerConnection instance;

    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;

    private ServerConnection() throws Exception {
        socket = new Socket(HOST, PORT);
        writer = new PrintWriter(socket.getOutputStream(), true);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
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

    private synchronized String sendRequest(String action, String json) throws Exception {
        writer.println(action + "===" + json);
        return reader.readLine();
    }

    /** Trả về "LOGIN SUCCESS" hoặc "LOGIN FAIL" */
    public String login(String username, String password, String role) throws Exception {
        String json = String.format(
                "{\"username\":\"%s\",\"password\":\"%s\",\"role\":\"%s\"}", username, password, role);
        return sendRequest("LOGIN", json);
    }

    /**
     * role: "ADMIN" | "SELLER" | "BIDDER"
     * Trả về "REGISTER SUCCESS" hoặc "REGISTER FAIL"
     */
    public String register(String username, String password, String role) throws Exception {
        String json = String.format(
                "{\"username\":\"%s\",\"password\":\"%s\",\"role\":\"%s\"}",
                username, password, role);
        return sendRequest("REGISTER", json);
    }

    public String placeBid(int auctionId, String username, double amount) throws Exception {
        String json = String.format(
                "{\"auctionId\":%d,\"username\":\"%s\",\"amount\":%.2f}",
                auctionId, username, amount);
        return sendRequest("PLACE_BID", json);
    }

    public String getItems() throws Exception {
        return sendRequest("GET_ITEMS", "{}");
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

    public String getBidHistory(int auctionId) throws Exception {
        String json = String.format("{\"auctionId\":%d}", auctionId);
        return sendRequest("GET_BID_HISTORY", json);
    }
}