package com.pokerproject.app;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import com.google.gson.Gson;
import com.pokerproject.model.Card;
import com.pokerproject.model.Game;
import com.pokerproject.model.Player;
import com.pokerproject.server.ClientSocket;

public class AppGUI {

    private CardLayout cardLayout;
    private JPanel mainPanel;
    private static final Gson gson = new Gson();
    private Game currentGame;
    private ClientSocket client;
    private String currentPlayerName;
    private volatile boolean listeningThreadStarted = false;
    private Thread listeningThread;

    public static void main(String[] args) {
        new AppGUI().createAndShowGUI();
    }

    private void startListeningFromServer() {
        stopListeningFromServer();

        listeningThread = new Thread(() -> {
            try {
                String message;
                while ((message = client.readMessage()) != null && !Thread.currentThread().isInterrupted()) {
                    System.out.println("Received message: " + message); // Debug

                    if (message.startsWith("UPDATE_GAME:")) {
                        String gameJson = message.substring("UPDATE_GAME:".length());
                        currentGame = gson.fromJson(gameJson, Game.class);

                        SwingUtilities.invokeLater(() -> {
                            // หา game panel และลบออก
                            for (int i = 0; i < mainPanel.getComponentCount(); i++) {
                                Component comp = mainPanel.getComponent(i);
                                if (comp instanceof JPanel) {
                                    // ใช้ index ของ Game panel (ควรเป็น index 2)
                                    try {
                                        mainPanel.remove(2); // Game panel index
                                        break;
                                    } catch (ArrayIndexOutOfBoundsException e) {
                                        break;
                                    }
                                }
                            }

                            JPanel gamePage = createGamePage();
                            mainPanel.add(gamePage, "Game");
                            cardLayout.show(mainPanel, "Game");
                            mainPanel.revalidate();
                            mainPanel.repaint();
                        });
                    } else if (message.startsWith("LEAVE_GAME_SUCCESS")) {
                        // รับ confirmation จาก server ว่า leave สำเร็จ
                        System.out.println("Leave game confirmed by server");
                    }
                    // else handle ข้อความอื่น ๆ
                }
            } catch (IOException e) {
                if (!Thread.currentThread().isInterrupted()) {
                    System.err.println("Connection error in listening thread: " + e.getMessage());
                }
            }
        });

        listeningThread.setDaemon(true);
        listeningThread.start();
        listeningThreadStarted = true;
    }

    public void stopListeningFromServer() {
        listeningThreadStarted = false;
        if (listeningThread != null && listeningThread.isAlive()) {
            listeningThread.interrupt();
            try {
                listeningThread.join(1000); // รอ 1 วินาที
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            listeningThread = null;
        }
    }

    private void createAndShowGUI() {
        JFrame frame = new JFrame("Poker Client");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 400);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        JPanel loginPage = createMainPage();
        JPanel menuPage = createMenuPage();
        JPanel gamePage = createGamePage();

        mainPanel.add(loginPage, "Login");
        mainPanel.add(menuPage, "Menu");
        mainPanel.add(gamePage, "Game");

        frame.add(mainPanel);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        cardLayout.show(mainPanel, "Login");
    }

    private JPanel createMainPage() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel label = new JLabel("Poker Game - CN311");
        JButton playButton = new JButton("Play");
        JButton exitButton = new JButton("Exit");

        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        playButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        exitButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        playButton.addActionListener(e -> {
            cardLayout.show(mainPanel, "Menu");
        });

        exitButton.addActionListener(e -> {
            System.exit(0);
        });

        panel.add(Box.createVerticalStrut(20));
        panel.add(label);
        panel.add(Box.createVerticalStrut(10));
        panel.add(playButton);
        panel.add(Box.createVerticalStrut(10));
        panel.add(exitButton);

        return panel;
    }

    private JPanel createMenuPage() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        JButton createButton = new JButton("Create Game");
        JButton joinButton = new JButton("Join Game");
        JButton backButton = new JButton("Back");
        JTextField roomField = new JTextField(10);
        JButton confirmJoinButton = new JButton("Confirm Join");

        roomField.setMaximumSize(new Dimension(200, 30));
        roomField.setAlignmentX(Component.CENTER_ALIGNMENT);
        confirmJoinButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        createButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        joinButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        backButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        createButton.addActionListener(e -> {
            String playerName = JOptionPane.showInputDialog("Enter your name:");
            if (playerName != null) {
                playerName = playerName.trim();
            }
            if (playerName == null || playerName.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Player name cannot be empty.");
                return;
            }
            try {
                if (client == null) {
                    client = new ClientSocket("localhost", 12345);
                }

                client.sendMessage("CREATE:" + playerName);
                String response = client.readMessage();
                String gameInfo = client.readMessage();
                JOptionPane.showMessageDialog(null, response);

                if (response.startsWith("Game created successfully") && !"".equals(gameInfo)) {
                    currentPlayerName = playerName;
                    currentGame = gson.fromJson(gameInfo, Game.class);

                    // ลบ game panel เก่า
                    try {
                        mainPanel.remove(2);
                    } catch (Exception ex) {
                        // ignore
                    }

                    JPanel gamePage = createGamePage();
                    mainPanel.add(gamePage, "Game");
                    cardLayout.show(mainPanel, "Game");

                    // เริ่ม listening thread
                    startListeningFromServer();
                }
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(null, "Failed to connect to server.");
                ex.printStackTrace();

                // ปิด connection ที่มีปัญหา
                try {
                    if (client != null) {
                        client.close();
                        client = null;
                    }
                } catch (Exception closeEx) {
                    // ignore
                }
            }
        });

        joinButton.addActionListener(e -> {
            roomField.setVisible(true);
            confirmJoinButton.setVisible(true);
            panel.revalidate();
            panel.repaint();
        });

        backButton.addActionListener(e -> {
            cardLayout.show(mainPanel, "Login");
        });

        confirmJoinButton.addActionListener(e -> {
            String roomId = roomField.getText();
            String playerName = JOptionPane.showInputDialog("Enter your name:");
            if (playerName != null) {
                playerName = playerName.trim();
            }
            if (playerName == null || playerName.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Player name cannot be empty.");
                return;
            }
            if (roomId != null && !roomId.isEmpty()) {
                try {
                    // สร้าง connection ใหม่หรือใช้ connection เดิม
                    if (client == null) {
                        client = new ClientSocket("localhost", 12345);
                    }

                    System.out.println("Attempting to join game: " + roomId + " with player: " + playerName);

                    client.sendMessage("JOIN:" + playerName + ":" + roomId);
                    String response = client.readMessage();
                    String gameInfo = client.readMessage();

                    System.out.println("Join response: " + response);
                    JOptionPane.showMessageDialog(null, response);

                    if (response.startsWith("Joined game") && !"".equals(gameInfo)) {
                        currentPlayerName = playerName;
                        currentGame = gson.fromJson(gameInfo, Game.class);

                        // ลบ game panel เก่าออก (ถ้ามี)
                        try {
                            mainPanel.remove(2); // Game panel อยู่ที่ index 2
                        } catch (Exception ex) {
                            // ไม่ต้องทำอะไรถ้าไม่มี panel
                        }

                        JPanel gamePage = createGamePage();
                        mainPanel.add(gamePage, "Game");
                        cardLayout.show(mainPanel, "Game");

                        // เริ่ม listening thread ใหม่
                        startListeningFromServer();

                        // ซ่อน join fields
                        roomField.setVisible(false);
                        confirmJoinButton.setVisible(false);
                        roomField.setText(""); // เคลียร์ field
                        panel.revalidate();
                        panel.repaint();
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(null, "Failed to join game: " + ex.getMessage());

                    // ปิด connection ที่มีปัญหาและสร้างใหม่
                    try {
                        if (client != null) {
                            client.close();
                            client = null;
                        }
                    } catch (Exception closeEx) {
                        // ignore
                    }
                }
            }
        });

        roomField.setVisible(false);
        confirmJoinButton.setVisible(false);

        panel.add(Box.createVerticalStrut(20));
        panel.add(createButton);
        panel.add(Box.createVerticalStrut(10));
        panel.add(joinButton);
        panel.add(Box.createVerticalStrut(10));
        panel.add(roomField);
        panel.add(Box.createVerticalStrut(10));
        panel.add(confirmJoinButton);
        panel.add(Box.createVerticalStrut(80));
        panel.add(backButton);

        return panel;
    }

    public void closeConnection() {
        try {
            stopListeningFromServer();
            if (client != null) {
                client.close();
                client = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private JPanel createGamePage() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(34, 45, 65)); // Dark poker table theme

        JLabel tableLabel = new JLabel("Poker Game");
        tableLabel.setForeground(Color.WHITE);
        if (currentGame != null) {
            tableLabel = new JLabel("Poker Game - " + currentGame.getGameId());
            tableLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            tableLabel.setFont(tableLabel.getFont().deriveFont(22.0f).deriveFont(Font.BOLD));
            tableLabel.setForeground(Color.ORANGE);
        }

        // ======= Community Cards =======
        JPanel communityPanel = new JPanel();
        communityPanel.setBackground(new Color(34, 45, 65));
        communityPanel.setLayout(new BoxLayout(communityPanel, BoxLayout.X_AXIS));
        if (currentGame != null) {
            for (int i = 0; i < 5; i++) {
                JLabel cardLabel = new JLabel();
                cardLabel.setIcon(loadCardImage(null)); // Default back
                communityPanel.add(cardLabel);
                communityPanel.add(Box.createHorizontalStrut(10));
            }
        }

        communityPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // ======= Player Slots =======
        JPanel playersPanel = new JPanel();
        playersPanel.setLayout(new BoxLayout(playersPanel, BoxLayout.Y_AXIS));
        playersPanel.setBackground(new Color(34, 45, 65));

        if (currentGame != null) {
            for (Player player : currentGame.getPlayers()) {
                JPanel playerRow = new JPanel();
                playerRow.setBackground(new Color(44, 55, 75));
                playerRow.setLayout(new BoxLayout(playerRow, BoxLayout.X_AXIS));
                playerRow.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

                JLabel playerLabel = new JLabel("Player " + player.getName() + ": ");
                playerLabel.setForeground(Color.WHITE);
                playerLabel.setFont(playerLabel.getFont().deriveFont(Font.PLAIN, 14f));

                JLabel card1 = new JLabel(loadCardImage(null));
                JLabel card2 = new JLabel(loadCardImage(null));

                JLabel chips = new JLabel("Chips: " + player.getChips());
                chips.setForeground(Color.GREEN);

                playerRow.add(playerLabel);
                playerRow.add(Box.createHorizontalStrut(10));
                playerRow.add(card1);
                playerRow.add(Box.createHorizontalStrut(10));
                playerRow.add(card2);
                playerRow.add(Box.createHorizontalGlue());
                playerRow.add(chips);

                playersPanel.add(playerRow);
                playersPanel.add(Box.createVerticalStrut(10));
            }
        }

        // ======= Action Buttons =======
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(new Color(34, 45, 65));

        JButton callButton = new JButton("Call");
        JButton foldButton = new JButton("Fold");
        JButton raiseButton = new JButton("Raise");
        JButton exitGameButton = new JButton("Exit game");

        JButton[] buttons = {callButton, foldButton, raiseButton, exitGameButton};
        for (JButton btn : buttons) {
            btn.setBackground(new Color(70, 130, 180));
            btn.setForeground(Color.WHITE);
            btn.setFocusPainted(false);
            btn.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        }

        buttonPanel.add(callButton);
        buttonPanel.add(foldButton);
        buttonPanel.add(raiseButton);
        buttonPanel.add(exitGameButton);

        // ======= Back Button Action =======
        exitGameButton.addActionListener(e -> {
            if (currentGame != null && currentPlayerName != null) {
                client.sendMessage("LEAVE_GAME:" + currentPlayerName + ":" + currentGame.getGameId());
                stopListeningFromServer();
                currentGame = null;
                currentPlayerName = null;
                cardLayout.show(mainPanel, "Menu");
            }
        });

        // ======= Assemble =======
        panel.add(Box.createVerticalStrut(20));
        panel.add(tableLabel);
        panel.add(Box.createVerticalStrut(15));
        panel.add(communityPanel);
        panel.add(Box.createVerticalStrut(20));
        panel.add(playersPanel);
        panel.add(Box.createVerticalStrut(20));
        panel.add(buttonPanel);
        panel.add(Box.createVerticalStrut(20));

        return panel;
    }

// ======= Load Card Image Helper =======
    private ImageIcon loadCardImage(Card card) {
        String path;
        if (card == null) {
            path = "/images/cards/2_of_clubs.png";  // default card
        } else {
            String rank = card.getRank().toString().toLowerCase();
            String suit = card.getSuit().toString().toLowerCase();
            path = "/images/cards/" + rank + "_of_" + suit + ".png";
        }

        try {
            Image img = new ImageIcon(getClass().getResource(path)).getImage().getScaledInstance(60, 90, Image.SCALE_SMOOTH);
            return new ImageIcon(img);
        } catch (Exception e) {
            System.err.println("Error loading image: " + path);
            return new ImageIcon(); // fallback เป็น icon เปล่า
        }
    }



}
