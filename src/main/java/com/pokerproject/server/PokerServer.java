package com.pokerproject.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PokerServer {

    private static final int PORT = 12345;

    // thread manager
    private final ExecutorService threadPool = Executors.newCachedThreadPool();

    public void start() {
        // create a server socket
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started on port " + PORT);

            while (true) {
                // wait for accept incoming connections
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress());

                ClientHandler handler = new ClientHandler(clientSocket);

                // create a new thread to handle the client
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
