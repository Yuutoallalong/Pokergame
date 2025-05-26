package src.main.java.com.pokerproject.model;

import java.util.ArrayList;
import java.util.List;

public class Player {
    private String id;
    private String name;
    private List<Card> holeCards;
    private int chips;
    private boolean isDealer;
    private boolean isSmallBlind;
    private boolean isBigBlind;
    private boolean isActive; //current player
    
    public Player(String id, String name, int startingChips) {
        this.id = id;
        this.name = name;
        this.chips = startingChips;
        this.holeCards = new ArrayList<>();
        this.isDealer = false;
        this.isSmallBlind = false;
        this.isBigBlind = false;
        this.isActive = true;
    }

    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public List<Card> getHoleCards() {
        return new ArrayList<>(holeCards);
    }

    public void addCard(Card card) {
        this.holeCards.add(card);
    }
    
    public void clearCards() {
        this.holeCards.clear();
    }
    
    public int getChips() {
        return chips;
    }
    
    public void addChips(int amount) {
        this.chips += amount;
    }
    
    public boolean removeChips(int amount) {
        if (amount > chips) {
            return false;
        }
        this.chips -= amount;
        return true;
    }
    
    public boolean isDealer() {
        return isDealer;
    }

    public void setDealer(boolean dealer) {
        this.isDealer = dealer;
    }
    
    public boolean isSmallBlind() {
        return isSmallBlind;
    }
    
    public void setSmallBlind(boolean smallBlind) {
        this.isSmallBlind = smallBlind;
    }
    
    public boolean isBigBlind() {
        return isBigBlind;
    }
    
    public void setBigBlind(boolean bigBlind) {
        this.isBigBlind = bigBlind;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public void setActive(boolean active) {
        this.isActive = active;
    }

    public void resetRoles() {
        this.isDealer = false;
        this.isSmallBlind = false;
        this.isBigBlind = false;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(" (").append(chips).append(" chips)");
        
        if (isDealer) sb.append(" [D]");
        if (isSmallBlind) sb.append(" [SB]");
        if (isBigBlind) sb.append(" [BB]");
        
        return sb.toString();
    }
}
