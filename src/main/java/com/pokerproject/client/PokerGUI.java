    package com.pokerproject.client;

    import java.awt.CardLayout;
    import java.awt.Component;
    import java.awt.Dimension;
    import java.io.IOException;

    import javax.swing.Box;
    import javax.swing.BoxLayout;
    import javax.swing.JButton;
    import javax.swing.JFrame;
    import javax.swing.JLabel;
    import javax.swing.JOptionPane;
    import javax.swing.JPanel;
    import javax.swing.JTextField;
    import javax.swing.SwingUtilities;

    import com.google.gson.Gson;
    import com.pokerproject.common.Game;
    import com.pokerproject.common.Player;


    public class PokerGUI {
        private CardLayout cardLayout;
        private JPanel mainPanel;
        private static final Gson gson = new Gson();
        private Game currentGame;
        private ClientSocket client;
        private String currentPlayerName;

        public static void main(String[] args) {
            new PokerGUI().createAndShowGUI();
        }
        private void startListeningFromServer() {    
            // ส่งข้อความหา Server    
            new Thread(() -> {
                try {
                    String message;
                    while ((message = client.readMessage()) != null) {
                        if (message.startsWith("UPDATE_GAME:")) {
                            String gameJson = message.substring("UPDATE_GAME:".length());
                            currentGame = gson.fromJson(gameJson, Game.class);

                            SwingUtilities.invokeLater(() -> {
                                mainPanel.remove(mainPanel.getComponent(2));
                                JPanel gamePage = createGamePage();
                                mainPanel.add(gamePage, "Game");
                                cardLayout.show(mainPanel, "Game");
                                mainPanel.revalidate();
                                mainPanel.repaint();
                            });
                        }
                        // else handle ข้อความอื่น ๆ
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
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
                        client = new ClientSocket("localhost", 12345); // สร้างแค่ครั้งเดียว
                    }
                    client.sendMessage("CREATE:" + playerName);
                    String response = client.readMessage();
                    String gameInfo = client.readMessage();
                    JOptionPane.showMessageDialog(null, response);
                    if (response.startsWith("Game created successfully")) {
                        currentPlayerName = playerName;
                        currentGame = gson.fromJson(gameInfo, Game.class);
                        // Reset currentGame State
                        mainPanel.remove(mainPanel.getComponent(2));
                        JPanel gamePage = createGamePage();
                        mainPanel.add(gamePage, "Game");
                        cardLayout.show(mainPanel, "Game");
                        startListeningFromServer();
                    }
                    
                    // client.close();
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(null, "Failed to connect to server.");
                    ex.printStackTrace();
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
                            client = new ClientSocket("localhost", 12345); // สร้างแค่ครั้งเดียว
                        }
                        client.sendMessage("JOIN:" + playerName + ":" + roomId);
                        String response = client.readMessage();
                        String gameInfo = client.readMessage();
                        JOptionPane.showMessageDialog(null, response);
                        if (response.startsWith("Joined game")) {
                            currentPlayerName = playerName;
                            currentGame = gson.fromJson(gameInfo, Game.class);
                            // Reset currentGame State
                            mainPanel.remove(mainPanel.getComponent(2));
                            JPanel gamePage = createGamePage();
                            mainPanel.add(gamePage, "Game");
                            cardLayout.show(mainPanel, "Game");
                            startListeningFromServer();
                        }
                        // client.close();
                    } catch (IOException ex) {
                        ex.printStackTrace();
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

            JLabel tableLabel = new JLabel("Poker Game - Table View");
            tableLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            tableLabel.setFont(tableLabel.getFont().deriveFont(18.0f));

            // ======= Community Cards (5 Cards Mock) =======
            JPanel communityPanel = new JPanel();
            communityPanel.setLayout(new BoxLayout(communityPanel, BoxLayout.X_AXIS));
            communityPanel.add(new JLabel("[🂠]")); // Card Back Placeholder
            communityPanel.add(Box.createHorizontalStrut(10));
            communityPanel.add(new JLabel("[🂠]"));
            communityPanel.add(Box.createHorizontalStrut(10));
            communityPanel.add(new JLabel("[🂠]"));
            communityPanel.add(Box.createHorizontalStrut(10));
            communityPanel.add(new JLabel("[🂠]"));
            communityPanel.add(Box.createHorizontalStrut(10));
            communityPanel.add(new JLabel("[🂠]"));
            communityPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

            // ======= Player Slots =======
            JPanel playersPanel = new JPanel();
            playersPanel.setLayout(new BoxLayout(playersPanel, BoxLayout.Y_AXIS));
            if (currentGame != null) {
                System.out.println("currentGame: "+ currentGame);
                for (int i = 1; i <= currentGame.getPlayers().size(); i++) {
                    Player player = currentGame.getPlayers().get(i - 1);
                    System.out.println(player);
                    JPanel playerRow = new JPanel();
                    playerRow.setLayout(new BoxLayout(playerRow, BoxLayout.X_AXIS));

                    JLabel playerLabel = new JLabel("Player " + player.getName() + ": ");
                    JLabel card1 = new JLabel("[🂠]");
                    JLabel card2 = new JLabel("[🂠]");
                    JLabel chips = new JLabel("Chips: " + player.getChips());

                    playerRow.add(playerLabel);
                    playerRow.add(Box.createHorizontalStrut(5));
                    playerRow.add(card1);
                    playerRow.add(Box.createHorizontalStrut(5));
                    playerRow.add(card2);
                    playerRow.add(Box.createHorizontalGlue());
                    playerRow.add(chips);

                    playersPanel.add(playerRow);
                    playersPanel.add(Box.createVerticalStrut(10));
                }
            }
            // ======= Action Buttons =======
            JPanel buttonPanel = new JPanel();
            JButton callButton = new JButton("Call");
            JButton foldButton = new JButton("Fold");
            JButton raiseButton = new JButton("Raise");
            JButton exitGameButton = new JButton("Exit game");

            buttonPanel.add(callButton);
            buttonPanel.add(foldButton);
            buttonPanel.add(raiseButton);
            buttonPanel.add(exitGameButton);

            // ======= Back Button Action =======
            exitGameButton.addActionListener(e -> {
                client.sendMessage("EXIT_GAME:" + currentPlayerName + ":" + currentGame.getGameId());
                cardLayout.show(mainPanel, "Menu");
                try {
                    client.close();
                } catch (IOException er) {
                    er.printStackTrace();
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

    }
