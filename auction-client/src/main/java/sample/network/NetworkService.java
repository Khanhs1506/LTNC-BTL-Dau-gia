package sample.network;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class NetworkService {

    private static NetworkService instance;
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;

    private NetworkService() {}

    public static NetworkService getInstance() {
        if (instance == null) instance = new NetworkService();
        return instance;
    }

    public void connect(String host, int port) throws Exception {
        socket = new Socket(host, port);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new PrintWriter(socket.getOutputStream(), true);
    }

    public void send(String message) {
        writer.println(message);
    }

    public String receive() throws Exception {
        return reader.readLine();
    }

    public void disconnect() throws Exception {
        if (socket != null) socket.close();
    }
}