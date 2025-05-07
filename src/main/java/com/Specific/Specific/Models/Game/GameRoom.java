package com.Specific.Specific.Models.Game;

import com.Specific.Specific.Models.Entities.Card;
import com.Specific.Specific.Models.Entities.User;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Getter
@Setter
public class GameRoom {
    // Room properties
    private String roomCode;
    private User host;
    private User guest;
    private boolean isGameStarted;
    private boolean isGameEnded;
    private long lastActivityTimestamp;
    
    // Game state
    private List<Card> gameCards = new ArrayList<>();
    private List<Card> hostCards = new ArrayList<>();
    private List<Card> guestCards = new ArrayList<>();
    private Card currentCard;
    private int hostScore;
    private int guestScore;
    private int roundNumber;
    private int totalRounds = 10; // Default
    
    // Multiple choice
    private List<String> currentCardOptions;
    private int correctOptionIndex;
    
    // Answer tracking
    private boolean hostAnswered;
    private boolean guestAnswered;
    
    public GameRoom(User host) {
        this.roomCode = generateRoomCode();
        this.host = host;
        this.lastActivityTimestamp = System.currentTimeMillis();
    }

    public boolean isRoomFull() {
        return host != null && guest != null;
    }

    public boolean isPlayerInRoom(User user) {
        if (user == null) return false;
        return (host != null && host.getId() == user.getId()) || 
               (guest != null && guest.getId() == user.getId());
    }

    public boolean haveAllPlayersAnswered() {
        return hostAnswered && guestAnswered;
    }

    public void prepareGame() {
        if (!isRoomFull()) {
            throw new IllegalStateException("Room must have two players to start");
        }

        // Create deck by combining cards from both players (limited to 30 per player max)
        this.gameCards.clear();
        if (!hostCards.isEmpty()) {
            this.gameCards.addAll(hostCards.subList(0, Math.min(hostCards.size(), 30)));
        }
        if (!guestCards.isEmpty()) {
            this.gameCards.addAll(guestCards.subList(0, Math.min(guestCards.size(), 30)));
        }
        
        // Reset game state
        shuffleCards();
        roundNumber = 0;
        hostScore = 0;
        guestScore = 0;
        isGameStarted = true;
        isGameEnded = false;
        hostAnswered = false;
        guestAnswered = false;
        lastActivityTimestamp = System.currentTimeMillis();
    }

    public Card nextCard() {
        if (roundNumber >= totalRounds) {
            endGame();
            return null;
        }
        
        roundNumber++;
        hostAnswered = false;
        guestAnswered = false;
        lastActivityTimestamp = System.currentTimeMillis();
        
        // Get primary source for cards (host's cards or game cards)
        List<Card> cardSource = !gameCards.isEmpty() ? gameCards : hostCards;
        if (cardSource.isEmpty()) {
            return null;
        }
        
        // Select random correct card
        Random random = new Random();
        currentCard = cardSource.get(random.nextInt(cardSource.size()));
        
        // Prepare multiple choice options
        currentCardOptions = new ArrayList<>();
        String correctAnswer = currentCard.getBack();
        currentCardOptions.add(correctAnswer);
        
        // Add incorrect option
        List<Card> possibleIncorrectCards = new ArrayList<>(cardSource);
        possibleIncorrectCards.remove(currentCard);
        
        if (!possibleIncorrectCards.isEmpty()) {
            Card incorrectCard = possibleIncorrectCards.get(random.nextInt(possibleIncorrectCards.size()));
            currentCardOptions.add(incorrectCard.getBack());
        } else {
            currentCardOptions.add("This is not the correct answer");
        }
        
        // Shuffle and track correct answer
        Collections.shuffle(currentCardOptions);
        correctOptionIndex = currentCardOptions.indexOf(correctAnswer);
        
        return currentCard;
    }

    public void submitAnswer(User user, boolean isCorrect) {
        if (host.equals(user)) {
            hostAnswered = true;
            if (isCorrect) hostScore++;
        } else if (guest.equals(user)) {
            guestAnswered = true;
            if (isCorrect) guestScore++;
        }
        lastActivityTimestamp = System.currentTimeMillis();
    }

    public GameResult endGame() {
        isGameEnded = true;
        lastActivityTimestamp = System.currentTimeMillis();
        
        String winnerUsername;
        if (hostScore > guestScore) {
            winnerUsername = host.getUsername();
        } else if (guestScore > hostScore) {
            winnerUsername = guest.getUsername();
        } else {
            winnerUsername = "Draw";
        }
        
        return new GameResult(
            roomCode,
            host.getUsername(),
            guest.getUsername(),
            hostScore,
            guestScore,
            winnerUsername
        );
    }
    
    private String generateRoomCode() {
        return UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
    
    private void shuffleCards() {
        Collections.shuffle(gameCards);
    }
} 