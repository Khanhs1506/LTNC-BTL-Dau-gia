package com.auction.client.controller;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class SocketClient{
    protected Socket socket;
    protected PrintWriter out;
    protected BufferedReader in;
    protected Gson gson = new Gson();

    public SocketClient(){}

    public SocketClient(Socket socket, PrintWriter out, BufferedReader in) {
        this.socket = socket;
        this.out = out;
        this.in = in;
    }

    // Kết nối client tới server
    public void connect() throws IOException{
        socket = new Socket("localhost", 9999);
        
        out = new PrintWriter(socket.getOutputStream(), true);

        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    // Gửi dữ liệu tới server
    public void send(String message){
        // System.out.println(message); // Hiện tại đang ra màn hình để chạy thử.
        out.println(message);
    }

    // Nhận dữ liệu từ server
    public String receive() throws IOException{
        String serverMes = in.readLine();
        return serverMes;
    }

    public void sendRequest(String action, Object data){
        try {

            String jsonData = gson.toJson(data);
            String req = action + "===" + jsonData;
            out.println(req);
        } catch (Exception e){
            e.printStackTrace();
        }
    }
//     public static void main(String[] args) throws Exception {
//     SocketClient client = new SocketClient();
//     client.connect();
// }
}

// LUỒNG CHẠY: 
/*1. Client tạo object
        ↓
2. connect()
        ↓
3. send(request)
        ↓
4. receive(response) */
