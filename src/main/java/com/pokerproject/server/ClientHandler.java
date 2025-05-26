package src.main.java.com.pokerproject.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private BufferedReader in;
    private PrintWriter out;
    private Player player;
    private Game currentGame;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
        try {
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            out.println("Welcome to the Poker Server!");

            // ขอชื่อผู้เล่น
            out.println("Enter your name:");
            String playerName = in.readLine();
            if (playerName == null || playerName.isEmpty()) {
                out.println("Invalid name. Disconnecting...");
                return;
            }

            // ขอ Game ID
            out.println("Enter Game ID to join or create:");
            String gameId = in.readLine();
            if (gameId == null || gameId.isEmpty()) {
                out.println("Invalid Game ID. Disconnecting...");
                return;
            }

            // สร้าง Player และเชื่อมต่อกับ GameManager
            this.player = new Player(playerName, this);
            GameManager manager = GameManager.getInstance();
            currentGame = manager.joinOrCreateGame(gameId, player);

            if (currentGame == null) {
                out.println("Game is full. Disconnecting...");
                return;
            }

            out.println("Joined game: " + gameId);
            broadcastToGame(playerName + " has joined the game!");

            // รับข้อความจาก client
            String message;
            while ((message = in.readLine()) != null) {
                System.out.println("[" + playerName + "] " + message);

                if (message.equalsIgnoreCase("exit")) {
                    out.println("Goodbye!");
                    break;
                } else {
                    // ตัวอย่าง: broadcast ข้อความไปทุกคนในห้อง
                    broadcastToGame("[" + playerName + "]: " + message);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            cleanup();
        }
    }

    public void sendMessage(String message) {
        out.println(message);
    }

    private void broadcastToGame(String message) {
        if (currentGame != null) {
            for (Player p : currentGame.getPlayers()) {
                p.getHandler().sendMessage(message);
            }
        }
    }

    private void cleanup() {
        try {
            if (currentGame != null && player != null) {
                currentGame.removePlayer(player);
                broadcastToGame(player.getName() + " has left the game.");
            }
            in.close();
            out.close();
            clientSocket.close();
            System.out.println("Client disconnected");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
