package com.pokerproject.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Game {

    public enum Round {
        PREFLOP, FLOP, TURN, RIVER, SHOWDOWN
    }

    public enum Action {
        FOLD, CHECK, CALL, BET, RAISE
    }

    private String gameId;
    private List<Player> players;
    private List<Player> activePlayers;
    private Deck deck;
    private int dealerPosition;
    private int currentPlayerIndex;
    private int smallBlindAmount;
    private int bigBlindAmount;
    private boolean gameStarted;
    private Round currentRound;
    private List<Card> communityCards;
    private int pot;
    private int currentBet;
    private int lastRaiseAmount;
    private Player lastRaiser;
    private Map<Player, Integer> playerBets = new HashMap<>();

    private static final int MAX_PLAYERS = 4;

    public Game(String gameId, int smallBlindAmount, int bigBlindAmount) {
        this.gameId = gameId;
        this.players = new ArrayList<>();
        this.activePlayers = new ArrayList<>();
        this.deck = new Deck();
        this.smallBlindAmount = smallBlindAmount;
        this.bigBlindAmount = bigBlindAmount;
        this.gameStarted = false;
        this.communityCards = new ArrayList<>();
        this.pot = 0;

        if (!gameStarted) {
            initializeFirstDealer();
        }
    }

    public boolean addPlayer(Player player) {
        if (players.size() < MAX_PLAYERS ) {
            players.add(player);
            return true;
        }
        return false;
    }

    public void removePlayer(Player player) {
        players.remove(player);
    }

    public boolean removePlayerByName(String name) {
        return players.removeIf(p -> p.getName().equals(name));
    }

    public List<Player> getPlayers() {
        return players;
    }

    public String getGameId() {
        return gameId;
    }

    public boolean isPlayerNameExists(String name) {
        return players.stream().anyMatch(player -> player.getName().equalsIgnoreCase(name));
    }

    private void initializeFirstDealer() {
        if (players.isEmpty()) {
            return;
        }

        Random random = new Random();
        dealerPosition = random.nextInt(players.size());

        assignPositions();

        gameStarted = true;
    }

    private void assignPositions() {
        for (Player player : players) {
            player.resetRoles();
        }

        int playerCount = players.size();

        players.get(dealerPosition).setDealer(true);

        int smallBlindPosition = (dealerPosition + 1) % playerCount;
        players.get(smallBlindPosition).setSmallBlind(true);

        int bigBlindPosition = (smallBlindPosition + 1) % playerCount;
        players.get(bigBlindPosition).setBigBlind(true);
    }

    public void rotateDealer() {
        dealerPosition = (dealerPosition + 1) % players.size();
        assignPositions();
    }

    public void startNewHand() {
        if (gameStarted) {
            rotateDealer();
        }

        pot = 0;
        currentBet = 0;
        lastRaiseAmount = 0;
        lastRaiser = null;
        communityCards.clear();
        activePlayers.clear();
        activePlayers.addAll(players);
        playerBets.clear(); 

        deck.reset();
        deck.shuffle();

        for (Player player : players) {
            player.clearCards();
        }

        collectBlinds();

        currentRound = Round.PREFLOP;

        int smallBlindPosition = (dealerPosition + 1) % players.size();
        deck.dealCardsFromPosition(players, smallBlindPosition, 2);

        int bigBlindPosition = (smallBlindPosition + 1) % players.size();
        currentPlayerIndex = (bigBlindPosition + 1) % players.size();
    }

    private void collectBlinds() {
        for (Player player : players) {
            if (player.isSmallBlind()) {
                int amount = Math.min(smallBlindAmount, player.getChips());
                player.removeChips(amount);
                pot += amount;
                currentBet = smallBlindAmount;
                playerBets.put(player, amount);
            }

            if (player.isBigBlind()) {
                int amount = Math.min(bigBlindAmount, player.getChips());
                player.removeChips(amount);
                pot += amount;
                currentBet = bigBlindAmount;
                lastRaiser = player;
                playerBets.put(player, amount);
            }
        }
    }

    public boolean processPlayerAction(Player player, Action action, int amount) {
        if (player != getCurrentPlayer()) {
            return false;
        }

        switch (action) {
            case FOLD:
                activePlayers.remove(player);
                break;

            case CHECK:
                if (currentBet > 0) {
                    return false;
                }
                break;

            case CALL:
                int callAmount = currentBet - getPlayerBet(player);
                if (callAmount > 0) {
                    callAmount = Math.min(callAmount, player.getChips());
                    player.removeChips(callAmount);
                    pot += callAmount;
                    playerBets.put(player, getPlayerBet(player) + callAmount);
                }
                break;

            case BET:
                if (currentBet > 0) {
                    return false;
                }
                if (amount < bigBlindAmount || amount > player.getChips()) {
                    return false;
                }
                player.removeChips(amount);
                pot += amount;
                currentBet = amount;
                lastRaiseAmount = amount;
                lastRaiser = player;
                playerBets.put(player, amount);
                break;

            case RAISE:
                int minRaise = currentBet + lastRaiseAmount;
                if (amount < minRaise || amount > player.getChips()) {
                    return false;
                }
                int raiseAmount = amount - getPlayerBet(player);
                player.removeChips(raiseAmount);
                pot += raiseAmount;
                lastRaiseAmount = amount - currentBet;
                currentBet = amount;
                lastRaiser = player;
                playerBets.put(player, amount);
                break;
        }

        moveToNextPlayer();

        if (isRoundComplete()) {
            advanceToNextRound();
        }

        return true;
    }

    private void moveToNextPlayer() {
        do {
            currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        } while (!activePlayers.contains(players.get(currentPlayerIndex)));
    }

    private boolean isRoundComplete() {
        if (activePlayers.size() == 1) {
            return true;
        }
        if (lastRaiser == null) {
            int startingPlayerIndex = (dealerPosition + 1) % players.size();
            return currentPlayerIndex == startingPlayerIndex;
        } else {
            int lastRaiserIndex = players.indexOf(lastRaiser);
            int nextPlayerIndex = (lastRaiserIndex + 1) % players.size();

            return currentPlayerIndex == nextPlayerIndex;
        }
    }

    

    private PokerHand findBestHand(List<Card> cards) {
        PokerHand bestHand = null;
        for (int a = 0; a < cards.size() - 4; a++) {
            for (int b = a + 1; b < cards.size() - 3; b++) {
                for (int c = b + 1; c < cards.size() - 2; c++) {
                    for (int d = c + 1; d < cards.size() - 1; d++) {
                        for (int e = d + 1; e < cards.size(); e++) {
                            List<Card> combination = new ArrayList<>();
                            combination.add(cards.get(a));
                            combination.add(cards.get(b));
                            combination.add(cards.get(c));
                            combination.add(cards.get(d));
                            combination.add(cards.get(e));

                            PokerHand hand = new PokerHand(combination);

                            if (bestHand == null || hand.compareTo(bestHand) > 0) {
                                bestHand = hand;
                            }
                        }
                    }
                }
            }
        }
        return bestHand;
    }

    private void determineWinner() {
        if (activePlayers.size() == 1) {
            Player winner = activePlayers.get(0);
            winner.addChips(pot);
            return;
        }

        Map<Player, PokerHand> playerHands = new HashMap<>();

        for (Player player : activePlayers) {
            List<Card> allCards = new ArrayList<>(player.getHoleCards());
            allCards.addAll(communityCards);

            PokerHand bestHand = findBestHand(allCards);
            playerHands.put(player, bestHand);
        }

        Player winner = null;
        PokerHand bestHand = null;

        for (Map.Entry<Player, PokerHand> entry : playerHands.entrySet()) {
            if (bestHand == null || entry.getValue().compareTo(bestHand) > 0) {
                winner = entry.getKey();
                bestHand = entry.getValue();
            }
        }

        List<Player> tiedWinners = new ArrayList<>();
        if (winner != null) {
            tiedWinners.add(winner);

            for (Map.Entry<Player, PokerHand> entry : playerHands.entrySet()) {
                if (entry.getKey() != winner && entry.getValue().compareTo(bestHand) == 0) {
                    tiedWinners.add(entry.getKey());
                }
            }

            int splitAmount = pot / tiedWinners.size();
            for (Player tiedWinner : tiedWinners) {
                tiedWinner.addChips(splitAmount);
            }
        }
    }

    private void advanceToNextRound() {
        currentBet = 0;
        lastRaiseAmount = bigBlindAmount;
        lastRaiser = null;
        playerBets.clear();

        switch (currentRound) {
            case PREFLOP:
                currentRound = Round.FLOP;
                dealFlop();
                break;

            case FLOP:
                currentRound = Round.TURN;
                dealTurn();
                break;

            case TURN:
                currentRound = Round.RIVER;
                dealRiver();
                break;

            case RIVER:
                currentRound = Round.SHOWDOWN;
                determineWinner();
                break;

            case SHOWDOWN:

                startNewHand();
                break;
        }

        if (currentRound != Round.SHOWDOWN) {
            currentPlayerIndex = (dealerPosition + 1) % players.size();

            while (!activePlayers.contains(players.get(currentPlayerIndex))) {
                currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
            }
        }
    }

    private void dealFlop() {
        deck.dealCard();
        for (int i = 0; i < 3; i++) {
            communityCards.add(deck.dealCard());
        }
    }

    private void dealTurn() {
        deck.dealCard();
        communityCards.add(deck.dealCard());
    }

    private void dealRiver() {
        deck.dealCard();

        communityCards.add(deck.dealCard());
    }

    public Player getCurrentPlayer() {
        return players.get(currentPlayerIndex);
    }

    public Round getCurrentRound() {
        return currentRound;
    }

    public List<Card> getCommunityCards() {
        return new ArrayList<>(communityCards);
    }

    public int getPot() {
        return pot;
    }

    private int getPlayerBet(Player player) {
        return playerBets.getOrDefault(player, 0);
    }

    public int getCurrentBet() {
        return currentBet;
    }

    public List<Player> getActivePlayers() {
        return new ArrayList<>(activePlayers);
    }

    public int getDealerPosition() {
        return dealerPosition;
    }

    public Player getDealer() {
        return players.get(dealerPosition);
    }

    public Player getSmallBlind() {
        int smallBlindPosition = (dealerPosition + 1) % players.size();
        return players.get(smallBlindPosition);
    }

    public Player getBigBlind() {
        int smallBlindPosition = (dealerPosition + 1) % players.size();
        int bigBlindPosition = (smallBlindPosition + 1) % players.size();
        return players.get(bigBlindPosition);
    }
}
