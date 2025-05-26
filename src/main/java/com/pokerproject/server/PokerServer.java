package com.pokerproject.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PokerServer {
    private static final int PORT = 12345;
    private final ExecutorService threadPool = Executors.newCachedThreadPool();

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress());
                
                ClientHandler handler = new ClientHandler(clientSocket);
                threadPool.submit(() -> {
                    System.out.println("Running handler in thread: " + Thread.currentThread().getName());
                    handler.run();
                });
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new PokerServer().start();
    }
}

