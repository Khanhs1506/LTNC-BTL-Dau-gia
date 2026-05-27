package com.auction.server.network;


import com.auction.server.network.ClientHandler;
import com.auction.server.service.AuctionManager;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ServerApp {
    public static void main(String[] args) throws Exception{

        AuctionManager.getInstance().loadFromDatabase();
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> AuctionManager.getInstance().checkAndUpdateStatuses(), 0, 5, TimeUnit.SECONDS);
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