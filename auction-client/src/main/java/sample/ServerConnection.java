package sample;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;


public class ServerConnection {
    private static final String host = "localhost";
    private static final int port = 9999;
    private static ServerConnection instance;

    public static ServerConnection getInstance() throws Exception {
        if (instance == null || instance.isClosed()) {
            instance = new ServerConnection();
        }
        return instance;
    }

    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;

    private ServerConnection() throws Exception {
        socket = new Socket(host, port);
        writer = new PrintWriter(socket.getOutputStream(), true);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    public boolean isClosed() {
        return socket == null || socket.isClosed();
    }

    private synchronized String sendRequest(String action, String json) throws Exception {
        writer.println(action + "===" + json);
        return reader.readLine();
    }

    public String login(String username, String password) throws Exception {
        String json = String.format("{\"username\":\"%s\",\"password\":\"%s\"}", username, password);
        return sendRequest("LOGIN", json);
    }

    public String register(String username, String password) throws Exception {
        String json = String.format(
                "{\"username\":\"%s\",\"password\":\"%s\"}", username, password);
        return sendRequest("REGISTER", json);
    }

    public String placeBid(int auctionId, String username, double amount) throws Exception {
        String json = String.format(
                "{\"auctionId\":%d,\"username\":\"%s\",\"amount\":%.2f}",
                auctionId, username, amount
        );
        return sendRequest("BID", json);
    }

    public void disconnect() {
        try {
            if (socket != null) socket.close();
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
