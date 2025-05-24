package src.main.java.com.pokerproject.client;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class PokerGUI {
    public static void main(String[] args) {
        new PokerGUI().createAndShowGUI();
    }

    private void createAndShowGUI() {
        JFrame frame = new JFrame("Poker Client");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 400);

        CardLayout cardLayout = new CardLayout();
        JPanel mainPanel = new JPanel(cardLayout);

        JPanel loginPage = createMainPage(cardLayout, mainPanel);
        JPanel gamePage = createGamePage(cardLayout, mainPanel);

        mainPanel.add(loginPage, "Login");
        mainPanel.add(gamePage, "Game");

        frame.add(mainPanel);
        frame.setVisible(true);
    }

    private JPanel createMainPage(CardLayout cardLayout, JPanel mainPanel) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel label = new JLabel("Poker Game - CN311");
        JButton playButton = new JButton("Play");
        JButton exitButton = new JButton("Exit");

        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        playButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        exitButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        playButton.addActionListener(e -> {
            cardLayout.show(mainPanel, "Game");
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


    private JPanel createGamePage(CardLayout cardLayout, JPanel mainPanel) {
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
            System.out.println("Joining room: " + roomId);

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
}

