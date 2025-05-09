package com.Specific.Specific.Services;

import com.Specific.Specific.Models.Entities.Card;
import com.Specific.Specific.Models.Entities.User;
import com.Specific.Specific.Models.Game.GameRoom;
import com.Specific.Specific.Models.Game.GameResult;
import com.Specific.Specific.utils.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Slf4j
public class GameService {

    private final Map<String, GameRoom> activeRooms = new ConcurrentHashMap<>();
    private final CardService cardService;
    private final SecurityUtils securityUtils;
    
    // Expiration time for inactive rooms (30 minutes in milliseconds)
    private static final long ROOM_EXPIRATION_TIME = 30 * 60 * 1000;

    public GameService(CardService cardService, SecurityUtils securityUtils) {
        this.cardService = cardService;
        this.securityUtils = securityUtils;
    }

    /**
     * Create a new game room for the current user
     * 
     * @return The created room
     */
    public GameRoom createRoom() {
        User currentUser = securityUtils.getCurrentUser();
        GameRoom room = new GameRoom(currentUser);
        
        // Get user's cards for the game
        List<Card> userCards = cardService.getUserCards();
        room.setHostCards(userCards);
        
        activeRooms.put(room.getRoomCode(), room);
        return room;
    }

    /**
     * Join an existing room as a guest
     * 
     * @param roomCode The code of the room to join
     * @return The joined room, or null if room not found or full
     */
    public GameRoom joinRoom(String roomCode) {
        GameRoom room = activeRooms.get(roomCode);
        if (room == null) return null;
        
        User currentUser = securityUtils.getCurrentUser();
        
        // Prevent joining if already in room
        if (room.isPlayerInRoom(currentUser)) {
            return room;
        }
        
        // Prevent joining if room is full
        if (room.isRoomFull()) {
            return null;
        }
        
        // Join as guest
        room.setGuest(currentUser);
        
        // Get user's cards for the game
        List<Card> userCards = cardService.getUserCards();
        room.setGuestCards(userCards);
        
        room.setLastActivityTimestamp(System.currentTimeMillis());
        return room;
    }

    /**
     * Get a room by its code
     */
    public GameRoom getRoom(String roomCode) {
        return activeRooms.get(roomCode);
    }

    /**
     * Start the game in a room
     * Only the host can start the game
     * 
     * @param roomCode The room code
     * @return The started game room, or null if room not found/not ready
     */
    public GameRoom startGame(String roomCode) {
        GameRoom room = activeRooms.get(roomCode);
        if (room == null || !room.isRoomFull()) {
            return null;
        }
        
        User currentUser = securityUtils.getCurrentUser();
        
        // Only host can start the game
        if (!room.getHost().equals(currentUser)) {
            return null;
        }
        
        try {
            room.prepareGame();
            return room;
        } catch (Exception e) {
            log.error("Error starting game in room {}: {}", roomCode, e.getMessage());
            return null;
        }
    }

    /**
     * Get the next card for a room
     * Any player in the room can request the next card
     * 
     * @param roomCode The room code
     * @return The next card, or null if game ended or error
     */
    public Card nextRound(String roomCode) {
        GameRoom room = activeRooms.get(roomCode);
        if (room == null || !room.isGameStarted() || room.isGameEnded()) {
            return null;
        }
        
        User currentUser = securityUtils.getCurrentUser();
        
        // Verify user is in the room
        if (!room.isPlayerInRoom(currentUser)) {
            return null;
        }
        
        return room.nextCard();
    }

    /**
     * End the game
     * 
     * @param roomCode The room code
     * @return The game result, or null if room not found
     */
    public GameResult endGame(String roomCode) {
        GameRoom room = activeRooms.get(roomCode);
        if (room == null) {
            return null;
        }
        
        User currentUser = securityUtils.getCurrentUser();
        
        // Verify user is in the room
        if (!room.isPlayerInRoom(currentUser)) {
            return null;
        }
        
        GameResult result = room.endGame();
        room.setLastActivityTimestamp(System.currentTimeMillis());

        return result;
    }

    /**
     * Leave a room
     * 
     * @param roomCode The room code
     * @return true if successfully left, false otherwise
     */
    public boolean leaveRoom(String roomCode) {
        GameRoom room = activeRooms.get(roomCode);
        if (room == null) {
            return false;
        }
        
        User currentUser = securityUtils.getCurrentUser();
        
        // If host leaves, remove the room
        if (room.getHost() != null && room.getHost().equals(currentUser)) {
            activeRooms.remove(roomCode);
            return true;
        }
        
        // If guest leaves, just remove guest
        if (room.getGuest() != null && room.getGuest().equals(currentUser)) {
            room.setGuest(null);
            room.setGameStarted(false);
            room.setGameEnded(true);
            room.setLastActivityTimestamp(System.currentTimeMillis());
            return true;
        }
        
        return false;
    }

    /**
     * Get all active rooms
     */
    public List<GameRoom> getActiveRooms() {
        return new ArrayList<>(activeRooms.values());
    }

    /**
     * Cleanup expired rooms every 15 minutes
     */
    @Scheduled(fixedRate = 15 * 60 * 1000)
    public void cleanupExpiredRooms() {
        long currentTime = System.currentTimeMillis();
        List<String> expiredRooms = activeRooms.entrySet().stream()
            .filter(entry -> (currentTime - entry.getValue().getLastActivityTimestamp()) > ROOM_EXPIRATION_TIME)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
        
        for (String roomCode : expiredRooms) {
            log.info("Removing expired room: {}", roomCode);
            activeRooms.remove(roomCode);
        }
    }
} 