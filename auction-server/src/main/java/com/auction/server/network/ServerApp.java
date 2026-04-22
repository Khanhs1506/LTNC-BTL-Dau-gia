package com.auction.server.network;

<<<<<<< HEAD
public class ServerApp {
    public static void main(String[] args) {
        System.out.println("Server is starting...");
    }
}
=======

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerApp {
    public static void main(String[] args) throws Exception{


        ServerSocket server = new ServerSocket(9999);
        System.out.println("Server dang chay...");
        while (true) {
            Socket socket = server.accept();
            ClientHandler.NumberOfClient++;
            System.out.println("Có " + ClientHandler.NumberOfClient + " khách đang kết nối!");
            Thread phucvu = new Thread(new ClientHandler(socket));
            phucvu.start();
        }
    }
}
// sendLogin
>>>>>>> 817410f54e5bfcefbf958f5c1aab6ba102d2f415
