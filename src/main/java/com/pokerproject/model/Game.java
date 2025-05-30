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
        FOLD, CHECK, CALL, BET, RAISE, NEXT
    }

    public enum State {
        WAITING, PLAYING
    }

    private final String gameId;
    private final List<Player> players;
    private final Deck deck;
    private int dealerPosition;
    private int currentPlayerIndex;
    private final int smallBlindAmount;
    private final int bigBlindAmount;
    private Round currentRound;
    private List<Card> communityCards;
    private int pot;
    private int currentBet;
    private Player lastRaiser;
    private final Map<String, Integer> playerBets = new HashMap<>();
    private State state;
    private boolean isAllFolded;
    private Player winner;

    private static final int MAX_PLAYERS = 8;

    public Game(String gameId, int smallBlindAmount, int bigBlindAmount) {
        this.gameId = gameId;
        this.players = new ArrayList<>();
        this.deck = new Deck();
        this.smallBlindAmount = smallBlindAmount;
        this.bigBlindAmount = bigBlindAmount;
        this.communityCards = new ArrayList<>();
        this.pot = 0;
        this.state = State.WAITING;
        this.isAllFolded = false;
        this.currentRound = Round.PREFLOP;
        this.winner = null;
    }


    public void setCommunityCards(List<Card> communityCards) {
        this.communityCards = communityCards;
    }

    public int getCurrentPlayerIndex() {
        return currentPlayerIndex;
    }

    public void setCurrentPlayerIndex(int currentPlayerIndex) {
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
        startNewHand();
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
        lastRaiser = null;
        isAllFolded = false;
        communityCards.clear();
        playerBets.clear(); 

        deck.reset();
        deck.shuffle();

        for (Player player : players) {
            player.clearCards();
            player.setIsActive(true);
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
                // currentBet = smallBlindAmount;
                playerBets.put(player.getName(), amount);
            }

            if (player.isBigBlind()) {
                int amount = Math.min(bigBlindAmount, player.getChips());
                player.removeChips(amount);
                pot += amount;
                // currentBet = bigBlindAmount;
                lastRaiser = player;
                playerBets.put(player.getName(), amount);
            }
        }
        currentBet = bigBlindAmount;
    }

    public boolean processPlayerAction(Player player, Action action, int amount) {
        if (player != getCurrentPlayer() && action != Action.NEXT) {
            return false;
        }

        switch (action) {
            case FOLD:
                player.setIsActive(false);
                break;

            case CHECK:
                break;

            case CALL:
                int callAmount = currentBet - getPlayerBet(player);
                if (callAmount > 0) {
                    callAmount = Math.min(callAmount, player.getChips());
                    player.removeChips(callAmount);
                    pot += callAmount;
                    playerBets.put(player.getName(), getPlayerBet(player) + callAmount);
                }
                break;
            
            case BET:
                player.removeChips(amount);
                pot += amount;
                currentBet = amount;
                lastRaiser = player;
                playerBets.put(player.getName(), amount);
                break;

            case RAISE:
                int playerCurrentBet = getPlayerBet(player);
                int raiseAmount = amount - playerCurrentBet;
                player.removeChips(raiseAmount);
                pot += raiseAmount;
                playerBets.put(player.getName(), amount);
                currentBet = amount;
                lastRaiser = player;
                break;
            case NEXT:
                break;
        }

        if(action != Action.NEXT) {
            System.out.println("MOVE TO NEXT PLAYER");
            moveToNextPlayer();
        }

        if (isRoundComplete() || action == Action.NEXT) {
            System.out.println("IN ADVANCE TO NEXT ROUND");
            advanceToNextRound();
        }

        return true;
    }

    private void moveToNextPlayer() {

        if (getUnfoldPlayer().size() <= 1) {
            isAllFolded = true;
            return;
        }

        int totalPlayers = players.size();
        int count = 0;

        do {
            this.setCurrentPlayerIndex((getCurrentPlayerIndex() + 1) % totalPlayers);
            count++;
        } while (!players.get(getCurrentPlayerIndex()).getIsActive() && count < totalPlayers);
    }

    private boolean isRoundComplete() {

        if (isAllFolded) {
            return true;
        }

        // เช็คว่าทุกคนตอบสนองต่อ current bet แล้วหรือยัง
        boolean everyoneMatured = hasEveryoneMaturedCurrentBet();
        
        if (!everyoneMatured) {
            return false;
        }

        if (lastRaiser == null || !lastRaiser.getIsActive()) {
            int startingPlayerIndex = getNextActivePlayerIndex(dealerPosition);
            boolean positionMatch = getCurrentPlayerIndex() == startingPlayerIndex;
            return positionMatch;
        } else {
            int lastRaiserIndex = players.indexOf(lastRaiser);
            int nextActivePlayerIndex = getNextActivePlayerIndex(lastRaiserIndex);
            boolean positionMatch = getCurrentPlayerIndex() == nextActivePlayerIndex;
            return positionMatch;
        }
    }

    private boolean hasEveryoneMaturedCurrentBet() {
        
        for (Player player : players) {
            if (player.getIsActive()) {
                int playerBetAmount = getPlayerBet(player);
                
                if (playerBetAmount < currentBet && player.getChips() > 0) {
                    return false;
                }
            }
        }
        return true;
    }

    private int getNextActivePlayerIndex(int fromIndex) {
        int index = fromIndex;
        int totalPlayers = players.size();
        int count = 0;

        do {
            index = (index + 1) % totalPlayers;
            count++;
        } while (!players.get(index).getIsActive() && count < totalPlayers);

        return index;
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
        this.winner = null;
        
        if (isAllFolded) {
            List<Player> winners = getUnfoldPlayer();
            if(winners.size() == 1){
                winners.get(0).addChips(pot);
                pot = 0;
                this.winner = winners.get(0);
                isAllFolded = false;
                return;
            }
        }


        Map<Player, PokerHand> playerHands = new HashMap<>();

        for (Player player : players) {
            if (!player.getIsActive()) continue;

            List<Card> allCards = new ArrayList<>(player.getHoleCards());
            allCards.addAll(communityCards);
            PokerHand bestHand = findBestHand(allCards);
            playerHands.put(player, bestHand);
        }

        Player winnerLocal = null;
        PokerHand bestHand = null;

        for (Map.Entry<Player, PokerHand> entry : playerHands.entrySet()) {
            if (bestHand == null || entry.getValue().compareTo(bestHand) > 0) {
                winnerLocal = entry.getKey();
                bestHand = entry.getValue();
            }
        }

        List<Player> tiedWinners = new ArrayList<>();
        if (winnerLocal != null) {
            tiedWinners.add(winnerLocal);

            for (Map.Entry<Player, PokerHand> entry : playerHands.entrySet()) {
                if (entry.getKey() != winnerLocal && entry.getValue().compareTo(bestHand) == 0) {
                    tiedWinners.add(entry.getKey());
                }
            }

            int splitAmount = pot / tiedWinners.size();
            int remainder = pot % tiedWinners.size();

            for (int i = 0; i < tiedWinners.size(); i++) {
                int extra = (i < remainder) ? 1 : 0;
                tiedWinners.get(i).addChips(splitAmount + extra);
            }

            tiedWinners.add(winnerLocal);
            this.winner = winnerLocal;
            
        pot = 0;

        }
    }

    private void advanceToNextRound() {
        System.out.println("Advancing to next round: " + currentRound);
        currentBet = 0;
        lastRaiser = null;
        playerBets.clear();

        if(isAllFolded){
            currentRound = Round.RIVER;
            System.out.println("Advancing AllFolded: " + currentRound);
        }

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
            while (!players.get(getCurrentPlayerIndex()).getIsActive() && count < totalPlayers) {;
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

    public List<Card> getCommunityCards() {
        return new ArrayList<>(communityCards);
    }

    public int getPot() {
        return pot;
    }

    public int getPlayerBet(Player player) {
        return playerBets.getOrDefault(player.getName(), 0);
    }


    public int getCurrentBet() {
        return currentBet;
    }

    public Round getCurrentRound() {
        return currentRound;
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

    public Player getWinner() {
        return winner;
    }
}
