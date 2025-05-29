package com.pokerproject.app;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.Window;
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

    public static String getPlayerRole(Player player) {
        if (player.isDealer()) {
            return "Dealer";
        } else if (player.isSmallBlind()) {
            return "Small Blind";
        } else if (player.isBigBlind()) {
            return "Big Blind";
        } else {
            return "";
        }
    }

    private void startListeningFromServer() {
        stopListeningFromServer();

        listeningThread = new Thread(() -> {
            try {
                String message;
                while ((message = client.readMessage()) != null && !Thread.currentThread().isInterrupted()) {
                    // System.out.println("Received message: " + message); // Debug

                    if (message.startsWith("UPDATE_GAME:")) {
                        String gameJson = message.substring("UPDATE_GAME:".length());
                        currentGame = gson.fromJson(gameJson, Game.class);
                        // System.out.println("ClientSide: " +
                        // currentGame.getCurrentPlayer().getName());
                        SwingUtilities.invokeLater(() -> {
                            for (int i = 0; i < mainPanel.getComponentCount(); i++) {
                                Component comp = mainPanel.getComponent(i);
                                if (comp instanceof JPanel) {
                                    try {
                                        mainPanel.remove(2);
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
                    } else if (message.startsWith("END")) {

                        Window window = SwingUtilities.getWindowAncestor(mainPanel);
                        if (window != null) {
                            window.dispose();
                        }
                    }
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
                listeningThread.join(1000);
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

                    try {
                        mainPanel.remove(2);
                    } catch (Exception ex) {
                    }

                    JPanel gamePage = createGamePage();
                    mainPanel.add(gamePage, "Game");
                    cardLayout.show(mainPanel, "Game");

                    startListeningFromServer();
                }
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(null, "Failed to connect to server.");
                ex.printStackTrace();

                try {
                    if (client != null) {
                        client.close();
                        client = null;
                    }
                } catch (Exception closeEx) {
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
                    if (client == null) {
                        client = new ClientSocket("localhost", 12345);
                    }

                    client.sendMessage("JOIN:" + playerName + ":" + roomId);
                    String response = client.readMessage();
                    String gameInfo = client.readMessage();

                    // System.out.println("Join response: " + response);
                    JOptionPane.showMessageDialog(null, response);

                    if (response.startsWith("Joined game") && !"".equals(gameInfo)) {
                        currentPlayerName = playerName;
                        currentGame = gson.fromJson(gameInfo, Game.class);

                        try {
                            mainPanel.remove(2);
                        } catch (Exception ex) {
                        }

                        JPanel gamePage = createGamePage();
                        mainPanel.add(gamePage, "Game");
                        cardLayout.show(mainPanel, "Game");

                        startListeningFromServer();

                        roomField.setVisible(false);
                        confirmJoinButton.setVisible(false);
                        roomField.setText("");
                        panel.revalidate();
                        panel.repaint();
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(null, "Failed to join game: " + ex.getMessage());

                    try {
                        if (client != null) {
                            client.close();
                            client = null;
                        }
                    } catch (Exception closeEx) {
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

    private JPanel createGamePage() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(34, 45, 65));
        JButton startButton = new JButton("Start Game");
        JLabel tableLabel = new JLabel("Poker Game");
        tableLabel.setForeground(Color.WHITE);

        JLabel potLabel = new JLabel();
        potLabel.setForeground(Color.ORANGE);
        potLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel roundLabel = new JLabel();
        roundLabel.setForeground(Color.ORANGE);
        roundLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        if (currentGame != null) {
            potLabel = new JLabel("Pot: " + currentGame.getPot());
            potLabel.setForeground(Color.WHITE);
            roundLabel = new JLabel("Round: " + currentGame.getCurrentRound());
            roundLabel.setForeground(Color.WHITE);
            tableLabel = new JLabel("Poker Game - " + currentGame.getGameId());
            tableLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            tableLabel.setFont(tableLabel.getFont().deriveFont(22.0f).deriveFont(Font.BOLD));
            tableLabel.setForeground(Color.ORANGE);
        }

        // ======= Community Cards =======
        JPanel communityPanel = new JPanel();
        communityPanel.setBackground(new Color(34, 45, 65));
        communityPanel.setLayout(new BoxLayout(communityPanel, BoxLayout.X_AXIS));
        if (currentGame != null && currentGame.getState() == Game.State.PLAYING
                && !currentGame.getCommunityCards().isEmpty()) {
            communityPanel.removeAll();
            for (int i = 0; i < currentGame.getCommunityCards().size(); i++) {
                JLabel cardLabel = new JLabel();
                if (currentGame.getCurrentRound() != Game.Round.PREFLOP) {
                    cardLabel.setIcon(loadCardImage(currentGame.getCommunityCards().get(i)));
                } else {
                    cardLabel.setIcon(loadCardImage(null));
                }
                communityPanel.add(cardLabel);
                communityPanel.add(Box.createHorizontalStrut(10));
            }
            communityPanel.revalidate();
            communityPanel.repaint();
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

                String labelMessage = player.getName() + " - " + getPlayerRole(player);

                if (player.getName().equals(currentPlayerName)) {
                    labelMessage += " (You)";
                }

                if (currentGame.getWinner() != null && player.getName().equals(currentGame.getWinner().getName())) {
                    labelMessage += "🥇";
                }

                JLabel playerLabel = new JLabel(labelMessage);

                if (player.getName().equals(currentPlayerName)) {
                    playerLabel.setForeground(new Color(255, 215, 0));
                    playerLabel.setFont(playerLabel.getFont().deriveFont(Font.BOLD, 14f));
                } else {
                    playerLabel.setForeground(Color.WHITE);
                    playerLabel.setFont(playerLabel.getFont().deriveFont(Font.PLAIN, 14f));
                }

                if (!player.getIsActive()) {
                    playerLabel.setForeground(Color.GRAY);
                    playerLabel.setFont(playerLabel.getFont().deriveFont(Font.ITALIC, 14f));
                }

                JLabel card1 = null;
                JLabel card2 = null;
                if (currentGame.getState() == Game.State.PLAYING) {
                    if (currentGame.getCurrentRound() == Game.Round.SHOWDOWN) {
                        if (player.getName().equals(currentPlayerName) || player.getName().equals(currentGame.getWinner().getName())) {
                            if (player.getHoleCards() != null && player.getHoleCards().size() >= 2) {
                                card1 = new JLabel(loadCardImage(player.getHoleCards().get(0)));
                                card2 = new JLabel(loadCardImage(player.getHoleCards().get(1)));
                            } else {
                                card1 = new JLabel(loadCardImage(null));
                                card2 = new JLabel(loadCardImage(null));
                            }
                        } else {
                            card1 = new JLabel(loadCardImage(null));
                            card2 = new JLabel(loadCardImage(null));
                        }

                        playerRow.add(card1);
                        playerRow.add(card2);

                    } else {
                        if (player.getName().equals(currentPlayerName)) {
                            card1 = new JLabel(loadCardImage(player.getHoleCards().get(0)));
                            card2 = new JLabel(loadCardImage(player.getHoleCards().get(1)));
                        } else {
                            card1 = new JLabel(loadCardImage(null));
                            card2 = new JLabel(loadCardImage(null));
                        }
                        playerRow.add(card1);
                        playerRow.add(card2);
                    }
                }

                JLabel chips = new JLabel("Chips: " + player.getChips());
                chips.setForeground(Color.GREEN);

                playerRow.add(playerLabel);
                playerRow.add(Box.createHorizontalStrut(10));
                if (currentGame.getState() == Game.State.PLAYING && card1 != null) {
                    playerRow.add(card1);
                }
                playerRow.add(Box.createHorizontalStrut(10));
                if (currentGame.getState() == Game.State.PLAYING && card2 != null) {
                    playerRow.add(card2);
                }
                playerRow.add(Box.createHorizontalGlue());
                playerRow.add(chips);

                playersPanel.add(playerRow);
                playersPanel.add(Box.createVerticalStrut(10));
            }
        }

        // ======= Action Buttons =======
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(new Color(34, 45, 65));

        if (currentGame != null && currentGame.getCreaterPlayer().getName().equals(currentPlayerName)
                && currentGame.getPlayers().size() > 1) {
            if (currentGame.getState() == Game.State.PLAYING) {
                startButton.setEnabled(false);
            }
            startButton.setEnabled(true);
        } else {
            startButton.setEnabled(false);
        }

        startButton.addActionListener((e) -> {
            client.sendMessage("START_GAME:" + currentGame.getGameId());
        });

        JButton callButton = new JButton();
        JButton foldButton = new JButton("Fold");
        JButton raiseButton = new JButton("Raise");
        JButton checkButton = new JButton("Check");
        JButton betButton = new JButton("Bet");
        JButton nextGameButton = new JButton("Next game");
        JButton exitGameButton = new JButton("Exit game");

        JButton[] buttons = {foldButton, raiseButton, checkButton, betButton, nextGameButton, exitGameButton,
            startButton};
        for (JButton btn : buttons) {
            btn.setBackground(new Color(70, 130, 180));
            btn.setForeground(Color.WHITE);
            btn.setFocusPainted(false);
            btn.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        }

        // ======= Buttton Panel =======
        if (currentGame != null) {

            int callAmount = currentGame.getCurrentBet() - currentGame.getPlayerBet(currentGame.getCurrentPlayer());
            callButton = new JButton("Call (" + callAmount + ")");
            callButton.setBackground(new Color(70, 130, 180));
            callButton.setForeground(Color.WHITE);
            callButton.setFocusPainted(false);
            callButton.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));

            boolean isFirst = currentGame.getCurrentPlayer().getName().equals(currentPlayerName);
            buttonPanel.removeAll();

            if (currentGame.getState() == Game.State.WAITING) {
                buttonPanel.add(startButton);
            } else {
                if (isFirst && currentGame.getCurrentRound() != Game.Round.SHOWDOWN) {
                    buttonPanel.add(foldButton);

                    int currentBet = currentGame.getCurrentBet();
                    Player currentPlayer = currentGame.getCurrentPlayer();
                    if (currentGame.getPlayerBet(currentPlayer) < currentBet) {
                        buttonPanel.add(callButton);
                    } else {
                        buttonPanel.add(checkButton);
                    }

                    if (currentBet == 0) {
                        buttonPanel.add(betButton);
                    } else {
                        buttonPanel.add(raiseButton);
                    }
                }

                if (currentGame.getCreaterPlayer().getName().equals(currentPlayerName)
                        && currentGame.getCurrentRound() == Game.Round.SHOWDOWN) {
                    buttonPanel.add(nextGameButton);
                }
            }

            buttonPanel.add(exitGameButton);
            buttonPanel.revalidate();
            buttonPanel.repaint();
        }

        // ======= Action Buttons Listener =======
        if (currentGame != null) {

            callButton.addActionListener(e -> {
                int callAmount = currentGame.getCurrentBet()
                        - currentGame.getPlayerBet(currentGame.getPlayerByName(currentPlayerName));
                if (callAmount <= 0) {
                    callAmount = 0;
                }
                client.sendMessage("CALL:" + currentGame.getGameId() + ":" + currentPlayerName + ":" + callAmount);
            });

            foldButton.addActionListener(
                    e -> client.sendMessage("FOLD:" + currentGame.getGameId() + ":" + currentPlayerName));

            raiseButton.addActionListener(e -> {
                String raiseAmount = JOptionPane.showInputDialog("Raise: ");
                int raiseAmountInt = 0;
                try {
                    raiseAmountInt = Integer.parseInt(raiseAmount);
                } catch (NumberFormatException err) {
                    JOptionPane.showMessageDialog(null, "Please enter a valid number!");
                }
                if (raiseAmountInt <= 0) {
                    JOptionPane.showMessageDialog(null, "Raise amount must be greater than 0.");
                } else if (raiseAmountInt > currentGame.getPlayerByName(currentPlayerName).getChips()) {
                    JOptionPane.showMessageDialog(null, "You don't have enough chips.");
                } else if (raiseAmountInt > currentGame.getCurrentBet()) {
                    JOptionPane.showMessageDialog(null, "Raise amount must be greater than current bet " + currentGame.getCurrentBet());
                } else {
                    client.sendMessage("RAISE:" + currentGame.getGameId() + ":" + currentPlayerName + ":" + raiseAmount);
                }

            });

            checkButton.addActionListener(
                    e -> client.sendMessage("CHECK:" + currentGame.getGameId() + ":" + currentPlayerName));

            betButton.addActionListener(e -> {
                String betAmount = JOptionPane.showInputDialog("Bet: ");
                int betAmountInt = 0;
                try {
                    betAmountInt = Integer.parseInt(betAmount);
                } catch (NumberFormatException err) {
                    JOptionPane.showMessageDialog(null, "Please enter a valid number!");
                }
                if (betAmountInt <= 0) {
                    JOptionPane.showMessageDialog(null, "Bet amount must be greater than 0.");
                } else if (betAmountInt > currentGame.getPlayerByName(currentPlayerName).getChips()) {
                    JOptionPane.showMessageDialog(null, "You don't have enough chips.");
                } else {
                    client.sendMessage("BET:" + currentGame.getGameId() + ":" + currentPlayerName + ":" + betAmount);
                }
            });

            nextGameButton.addActionListener(
                    e -> client.sendMessage("NEXTGAME:" + currentGame.getGameId() + ":" + currentPlayerName));
        }

        // ======= Back Button Action =======
        exitGameButton.addActionListener(e -> {
            if (currentGame != null && currentPlayerName != null) {
                client.sendMessage("LEAVE_GAME:" + currentPlayerName + ":" + currentGame.getGameId());
                stopListeningFromServer();
                currentGame = null;
                currentPlayerName = null;
                cardLayout.show(mainPanel, "Menu");
            }

            Window window = SwingUtilities.getWindowAncestor(panel);
            if (window != null) {
                window.dispose();
            }
        });

        // ======= Assemble =======
        panel.add(Box.createVerticalStrut(20));
        panel.add(tableLabel);
        panel.add(Box.createVerticalStrut(20));
        panel.add(potLabel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(roundLabel);
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
            path = "/images/cards/card_back.png"; // default card
        } else {
            String rank = card.getRank().toString().toLowerCase();
            String suit = card.getSuit().toString().toLowerCase();
            path = "/images/cards/" + rank + "_of_" + suit + ".png";
        }

        try {
            Image img = new ImageIcon(getClass().getResource(path)).getImage().getScaledInstance(60, 90,
                    Image.SCALE_SMOOTH);
            return new ImageIcon(img);
        } catch (Exception e) {
            System.err.println("Error loading image: " + path);
            return new ImageIcon(); // fallback เป็น icon เปล่า
        }
    }

}
