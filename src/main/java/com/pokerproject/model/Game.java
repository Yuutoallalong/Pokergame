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

    public enum State {
        WAITING, PLAYING
    }

    private String gameId;
    private List<Player> players;
    // private List<Player> activePlayers;
    private Deck deck;
    private int dealerPosition;
    private int currentPlayerIndex;
    private int smallBlindAmount;
    private int bigBlindAmount;
    private Round currentRound;
    private List<Card> communityCards;
    private int pot;
    private int currentBet;
    private int lastRaiseAmount;
    private Player lastRaiser;
    private Map<Player, Integer> playerBets = new HashMap<>();
    private State state;
    private boolean isAllFolded;


    private static final int MAX_PLAYERS = 8;

    public Game(String gameId, int smallBlindAmount, int bigBlindAmount) {
        this.gameId = gameId;
        this.players = new ArrayList<>();
        // this.activePlayers = new ArrayList<>();
        this.deck = new Deck();
        this.smallBlindAmount = smallBlindAmount;
        this.bigBlindAmount = bigBlindAmount;
        this.communityCards = new ArrayList<>();
        this.pot = 0;
        this.state = State.WAITING;
        this.isAllFolded = false;
        this.currentRound = Round.PREFLOP;
    }

    public int getCurrentPlayerIndex() {
        System.out.println("getCurrentPlayerIndex called, currentPlayerIndex = " + currentPlayerIndex);
        return currentPlayerIndex;
    }

    public void setCurrentPlayerIndex(int currentPlayerIndex) {
        System.out.println("setCurrentPlayerIndex called with: " + currentPlayerIndex);
        Thread.dumpStack();
        this.currentPlayerIndex = currentPlayerIndex;
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

    public Deck getDeck(){
        return this.deck;
    }

    public boolean removePlayerByName(String name) {
        return players.removeIf(p -> p.getName().equals(name));
    }

    public List<Player> getPlayers() {
        return players;
    }

    public Player getCreaterPlayer() {
        return players.stream().filter(p -> p.isCreater).findFirst().orElse(null);
    }

    public String getGameId() {
        return gameId;
    }

    public boolean isPlayerNameExists(String name) {
        return players.stream().anyMatch(player -> player.getName().equalsIgnoreCase(name));
    }

    public Player getPlayerByName(String name) {
        return players.stream().filter(p -> p.getName().equals(name)).findFirst().orElse(null);
    }

    public void setState(State state){
        this.state = state;
    }

    public State getState() {
        return state;
    }

    public void initializeFirstDealer() {
        if (players.isEmpty()) {
            return;
        }

        Random random = new Random();
        dealerPosition = random.nextInt(players.size());

        assignPositions();

        this.state = State.PLAYING;
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
        // currentPlayerIndex = (bigBlindPosition + 1) % players.size();
        setCurrentPlayerIndex((bigBlindPosition + 1) % players.size());
    }

    public void rotateDealer() {
        dealerPosition = (dealerPosition + 1) % players.size();
        assignPositions();
    }

    public void startNewHand() {
        if (this.state == State.PLAYING) {
            rotateDealer();
        }

        pot = 0;
        currentBet = 0;
        lastRaiseAmount = 0;
        lastRaiser = null;
        communityCards.clear();
        // activePlayers.clear();
        // activePlayers.addAll(players);
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
        // currentPlayerIndex = (bigBlindPosition + 1) % players.size();
        setCurrentPlayerIndex((bigBlindPosition + 1) % players.size());
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
                // activePlayers.remove(player);
                player.setIsActive(false);
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
                int playerCurrentBet = getPlayerBet(player);
                int callAmountRaise = currentBet - playerCurrentBet;
                int minRaise = callAmountRaise + lastRaiseAmount;
                if (amount < minRaise || amount > player.getChips() + playerCurrentBet) {
                    return false;
                }
                lastRaiseAmount = amount - callAmountRaise;
                player.removeChips(amount - playerCurrentBet);
                pot += amount - playerCurrentBet;
                playerBets.put(player, amount);
                currentBet = amount;
                lastRaiser = player;
                break;

        }

        moveToNextPlayer();

        if (isRoundComplete()) {
            advanceToNextRound();
        }

        return true;
    }

    private void moveToNextPlayer() {
        System.out.println("moveToNextPlayer");
        System.out.println("currentPlayerIndexBefore: " + getCurrentPlayerIndex());
        System.out.println("PlayersBefore: " + players);
        System.out.println("getCurrentPlayerBefore: " + getCurrentPlayer());

        if (getUnfoldPlayer().size() <= 1) {
            isAllFolded = true;
            return;
        }

        int totalPlayers = players.size();
        int count = 0;

        do {
            this.setCurrentPlayerIndex((getCurrentPlayerIndex() + 1) % totalPlayers);
            // this.currentPlayerIndex = (this.currentPlayerIndex + 1) % totalPlayers;
            count++;
        } while (!players.get(getCurrentPlayerIndex()).getIsActive() && count < totalPlayers);

        System.out.println("currentPlayerIndexAfter: " + getCurrentPlayerIndex());
        System.out.println("PlayersAfter: " + players);
        System.out.println("getCurrentPlayerAfter: " + getCurrentPlayer());
    }



    private boolean isRoundComplete() {
        if (isAllFolded) {
            return true;
        }
        if (lastRaiser == null || !lastRaiser.getIsActive()) {
            int startingPlayerIndex = (dealerPosition + 1) % players.size();
            return getCurrentPlayerIndex() == startingPlayerIndex;
        } else {
            int lastRaiserIndex = players.indexOf(lastRaiser);
            int nextPlayerIndex = (lastRaiserIndex + 1) % players.size();
            return getCurrentPlayerIndex() == nextPlayerIndex;
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

    private List<Player> getUnfoldPlayer(){
        List<Player> unFoldPlayers = new ArrayList<>();
        for (Player player : players) {
            if (player.getIsActive() == true) {
                unFoldPlayers.add(player);
            }
        }
        return unFoldPlayers;
    }

    private void determineWinner() {
       if (isAllFolded) {
        List<Player> winners = getUnfoldPlayer();
        if(winners.size() == 1){
            winners.get(0).addChips(pot);
            isAllFolded = false;
            return;
        }
    }


        Map<Player, PokerHand> playerHands = new HashMap<>();

        for (Player player : players) {
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
            setCurrentPlayerIndex((dealerPosition + 1) % players.size());

            int totalPlayers = players.size();
            int count = 0;

            // หา player ที่ active คนถัดไป
            while (!players.get(getCurrentPlayerIndex()).getIsActive() && count < totalPlayers) {
                setCurrentPlayerIndex((getCurrentPlayerIndex() + 1) % totalPlayers);
                count++;
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
        return players.get(getCurrentPlayerIndex());
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

    // public List<Player> getActivePlayers() {
    //     return new ArrayList<>(activePlayers);
    // }

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
