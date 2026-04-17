package com.auction.server.network;


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