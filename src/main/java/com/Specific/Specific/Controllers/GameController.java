package com.Specific.Specific.Controllers;

import com.Specific.Specific.Models.Entities.Card;
import com.Specific.Specific.Models.Entities.User;
import com.Specific.Specific.Models.Game.GameMessage;
import com.Specific.Specific.Models.Game.GameMessage.MessageType;
import com.Specific.Specific.Models.Game.GameRoom;
import com.Specific.Specific.Models.Game.GameResult;
import com.Specific.Specific.Services.GameService;
import com.Specific.Specific.Services.UserService;
import com.Specific.Specific.utils.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Controller
@Slf4j
public class GameController {

    private final SimpMessagingTemplate messagingTemplate;
    private final GameService gameService;
    private final SecurityUtils securityUtils;
    private final UserService userService;
    
    // Queue destination prefix for personal messages
    private static final String PERSONAL_QUEUE = "/queue/game";
    
    // Topic destination prefix for room-wide messages
    private static final String ROOM_TOPIC = "/topic/game.room.";

    public GameController(SimpMessagingTemplate messagingTemplate, 
                          GameService gameService, 
                          SecurityUtils securityUtils,
                          UserService userService) {
        this.messagingTemplate = messagingTemplate;
        this.gameService = gameService;
        this.securityUtils = securityUtils;
        this.userService = userService;
    }

    /**
     * REST endpoint to create a new game room
     */
    @PostMapping("/api/game/room")
    @ResponseBody
    public GameRoom createRoom(@RequestParam(required = false) String firebaseUid) {
        try {
            // Get user either from Firebase UID or security context
            User user;
            if (firebaseUid != null && !firebaseUid.isEmpty()) {
                // Use userService to find user by Firebase UID
                user = userService.findUserByFirebaseUid(firebaseUid);
                if (user == null) {
                    throw new RuntimeException("User with Firebase UID not found: " + firebaseUid);
                }
            } else {
                // Fallback to security context
                user = securityUtils.getCurrentUser();
            }
            
            return gameService.createRoom(user);
        } catch (Exception e) {
            log.error("Error creating game room: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * WebSocket endpoint to handle joining a room
     */
    @MessageMapping("/game.join")
    public void joinRoom(@Payload GameMessage message, SimpMessageHeaderAccessor headerAccessor) {
        String roomCode = message.getRoomCode();
        String senderUsername = message.getSenderUsername();
        
        log.info("Received join request for room {}: {}", roomCode, senderUsername);
        
        GameRoom room = gameService.getRoom(roomCode);
        
        if (room == null) {
            log.warn("Room not found: {}", roomCode);
            sendErrorToUser(senderUsername, "Room not found: " + roomCode);
            return;
        }
        
        if (room.isRoomFull()) {
            log.warn("Room {} is already full", roomCode);
            sendErrorToUser(senderUsername, "Room is already full");
            return;
        }
        
        // Store key information in the session for later use
        headerAccessor.getSessionAttributes().put("roomCode", roomCode);
        headerAccessor.getSessionAttributes().put("username", senderUsername);
        
        User user = null;
        
        try {
            // Get user from Firebase UID or security context
            String firebaseUid = headerAccessor.getFirstNativeHeader("firebaseUid");
            if (firebaseUid != null && !firebaseUid.isEmpty()) {
                user = userService.findUserByFirebaseUid(firebaseUid);
            } else {
                user = securityUtils.getCurrentUser();
            }
            
            if (user == null) {
                log.warn("User not found for join request: {}", senderUsername);
                sendErrorToUser(senderUsername, "User not found. Please log in again.");
                return;
            }
            
            // Update the room with the guest
            boolean success = gameService.joinUserToRoom(roomCode, user);
            
            if (!success) {
                log.warn("Failed to join room {}: {}", roomCode, senderUsername);
                sendErrorToUser(senderUsername, "Failed to join room");
                return;
            }
            
            // Notify room about the new player
            GameMessage joinedMessage = new GameMessage();
            joinedMessage.setType(MessageType.ROOM_JOINED);
            joinedMessage.setRoomCode(room.getRoomCode());
            joinedMessage.setSenderId(user.getId());
            joinedMessage.setSenderUsername(senderUsername);
            joinedMessage.setContent(senderUsername + " joined the room");
            
            // Send to everyone in the room including the sender
            sendMessageToRoom(room.getRoomCode(), joinedMessage);
            
            // Also send a direct confirmation to the user who joined
            sendMessageToUser(senderUsername, joinedMessage);
            
            // Notify host directly to ensure they see the guest
            if (room.getHost() != null && !room.getHost().getUsername().equals(senderUsername)) {
                GameMessage hostNotification = new GameMessage();
                hostNotification.setType(MessageType.GUEST_JOINED);
                hostNotification.setRoomCode(room.getRoomCode());
                hostNotification.setSenderId(user.getId());
                hostNotification.setSenderUsername(senderUsername);
                hostNotification.setContent("Guest " + senderUsername + " joined your room");
                
                sendMessageToUser(room.getHost().getUsername(), hostNotification);
            }
            
            log.info("User {} successfully joined room {}", senderUsername, roomCode);
        } catch (Exception e) {
            log.error("Error joining room {}: {}", roomCode, e.getMessage(), e);
            sendErrorToUser(senderUsername, "Error joining room: " + e.getMessage());
        }
    }

    /**
     * WebSocket endpoint to handle starting a game
     */
    @MessageMapping("/game.start")
    public void startGame(@Payload GameMessage message) {
        GameRoom room = gameService.startGame(message.getRoomCode());
        
        if (room == null) {
            sendErrorToUser(message.getSenderUsername(), 
                "Failed to start game. Make sure room exists and has two players.");
            return;
        }
        
        // Notify room that the game has started
        GameMessage startedMessage = new GameMessage();
        startedMessage.setType(MessageType.GAME_STARTED);
        startedMessage.setRoomCode(room.getRoomCode());
        startedMessage.setContent("Game started");
        
        // Send to everyone in the room
        sendMessageToRoom(room.getRoomCode(), startedMessage);
        
        // Start the first round automatically
        moveToNextRound(room);
    }

    /**
     * WebSocket endpoint to handle moving to the next round
     * This is a manual override for the host to force next round
     */
    @MessageMapping("/game.nextRound")
    public void nextRound(SimpMessageHeaderAccessor headerAccessor) {
        User currentUser = securityUtils.getCurrentUser();
        String roomCode = headerAccessor.getFirstNativeHeader("roomCode");
        GameRoom room = gameService.getRoom(roomCode);
        
        if (room == null || !room.isGameStarted() || room.isGameEnded()) {
            return;
        }
        
        // Only proceed if it's the host's turn
        if (!room.getHost().equals(currentUser)) {
            return;
        }
        
        // Move to the next round manually
        moveToNextRound(room);
    }

    /**
     * WebSocket endpoint to handle submitting an answer
     */
    @MessageMapping("/game.submitAnswer")
    public void submitAnswer(SimpMessageHeaderAccessor headerAccessor, int selectedOptionIndex) {
        User currentUser = securityUtils.getCurrentUser();
        String roomCode = headerAccessor.getFirstNativeHeader("roomCode");
        GameRoom room = gameService.getRoom(roomCode);
        
        if (room == null || !room.isGameStarted() || room.isGameEnded()) {
            return;
        }
        
        // Check if the selected option is correct
        boolean isCorrect = (selectedOptionIndex == room.getCorrectOptionIndex());
        
        // Submit answer and update score in game room
        room.submitAnswer(currentUser, isCorrect);
        
        // Notify both players about the answer
        GameMessage answerMessage = new GameMessage();
        answerMessage.setType(MessageType.ANSWER_SUBMITTED);
        answerMessage.setRoomCode(roomCode);
        answerMessage.setSenderUsername(currentUser.getUsername());
        answerMessage.setContent(String.valueOf(selectedOptionIndex));
        answerMessage.setHostScore(room.getHostScore());
        answerMessage.setGuestScore(room.getGuestScore());
        
        // Send to all players in the room
        sendMessageToUsers(room, answerMessage);
        
        // Check if both players have answered
        if (room.haveAllPlayersAnswered()) {
            // Wait 2 seconds to show the results before moving to the next card
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // Move to the next round automatically
            moveToNextRound(room);
        }
    }

    /**
     * WebSocket endpoint to handle leaving a room
     */
    @MessageMapping("/game.leave")
    public void leaveRoom(@Payload GameMessage message) {
        boolean success = gameService.leaveRoom(message.getRoomCode());
        
        if (!success) {
            sendErrorToUser(message.getSenderUsername(), "Failed to leave room");
            return;
        }
        
        // Notify room that the player left
        GameMessage leaveMessage = new GameMessage();
        leaveMessage.setType(MessageType.LEAVE_ROOM);
        leaveMessage.setRoomCode(message.getRoomCode());
        leaveMessage.setSenderId(message.getSenderId());
        leaveMessage.setSenderUsername(message.getSenderUsername());
        leaveMessage.setContent(message.getSenderUsername() + " left the room");
        
        // Send to everyone in the room
        sendMessageToRoom(message.getRoomCode(), leaveMessage);
    }
    
    /**
     * Helper method to send game over message to players
     */
    private void sendGameOverMessage(GameRoom room) {
        if (room == null) return;
        
        GameResult result = room.endGame();
        
        GameMessage gameOverMessage = new GameMessage();
        gameOverMessage.setType(MessageType.GAME_OVER);
        gameOverMessage.setRoomCode(room.getRoomCode());
        gameOverMessage.setHostScore(result.getHostScore());
        gameOverMessage.setGuestScore(result.getGuestScore());
        gameOverMessage.setContent("Game over! Winner: " + result.getWinnerUsername());
        gameOverMessage.setGameResult(result);
        
        // Send to all players in the room
        sendMessageToUsers(room, gameOverMessage);
    }

    /**
     * Helper method to move to the next round
     */
    private void moveToNextRound(GameRoom room) {
        Card nextCard = room.nextCard();
        if (nextCard == null) {
            sendGameOverMessage(room);
            return;
        }
        
        // Prepare next round message
        GameMessage nextRoundMessage = new GameMessage();
        nextRoundMessage.setType(MessageType.NEXT_ROUND);
        nextRoundMessage.setRoomCode(room.getRoomCode());
        nextRoundMessage.setCurrentCard(nextCard);
        nextRoundMessage.setRoundNumber(room.getRoundNumber());
        nextRoundMessage.setHostScore(room.getHostScore());
        nextRoundMessage.setGuestScore(room.getGuestScore());
        nextRoundMessage.setCardOptions(room.getCurrentCardOptions());
        nextRoundMessage.setCorrectOptionIndex(room.getCorrectOptionIndex());
        
        // Send to all players in the room
        sendMessageToUsers(room, nextRoundMessage);
    }
    
    /**
     * Helper method to send an error message to a specific user
     */
    private void sendErrorToUser(String username, String errorMessage) {
        GameMessage errorMsg = new GameMessage(MessageType.ERROR, errorMessage);
        messagingTemplate.convertAndSendToUser(
            username,
            PERSONAL_QUEUE,
            errorMsg
        );
    }
    
    /**
     * Helper method to send a message to all players in a room
     */
    private void sendMessageToUsers(GameRoom room, GameMessage message) {
        if (room.getHost() != null) {
            messagingTemplate.convertAndSendToUser(
                room.getHost().getUsername(),
                PERSONAL_QUEUE,
                message
            );
        }
        
        if (room.getGuest() != null) {
            messagingTemplate.convertAndSendToUser(
                room.getGuest().getUsername(),
                PERSONAL_QUEUE,
                message
            );
        }
    }
    
    /**
     * Helper method to send a message to a room topic
     */
    private void sendMessageToRoom(String roomCode, GameMessage message) {
        try {
            log.info("Sending message to room {}: type={}, sender={}", 
                    roomCode, message.getType(), message.getSenderUsername());
                    
            messagingTemplate.convertAndSend(
                ROOM_TOPIC + roomCode,
                message
            );
        } catch (Exception e) {
            log.error("Error sending message to room {}: {}", roomCode, e.getMessage(), e);
        }
    }

    /**
     * REST endpoint to check if a room exists
     */
    @GetMapping("/api/game/room/{roomCode}/exists")
    @ResponseBody
    public Map<String, Object> roomExists(@PathVariable String roomCode) {
        Map<String, Object> response = new HashMap<>();
        GameRoom room = gameService.getRoom(roomCode);
        boolean exists = (room != null);
        
        response.put("exists", exists);
        if (exists) {
            response.put("isFull", room.isRoomFull());
            response.put("hostUsername", room.getHost().getUsername());
        }
        
        log.info("Room {} exists check: {}", roomCode, exists);
        return response;
    }
    
    /**
     * REST endpoint to join a room
     */
    @PostMapping("/api/game/room/{roomCode}/join")
    @ResponseBody
    public GameRoom joinRoomViaApi(@PathVariable String roomCode, 
                                   @RequestParam(required = false) String firebaseUid,
                                   @RequestBody(required = false) Map<String, String> body) {
        try {
            // Get user either from Firebase UID or security context
            User user;
            
            // First try from request body
            String uidFromBody = body != null ? body.get("firebaseUid") : null;
            if (uidFromBody != null && !uidFromBody.isEmpty()) {
                user = userService.findUserByFirebaseUid(uidFromBody);
            }
            // Then try from request parameter
            else if (firebaseUid != null && !firebaseUid.isEmpty()) {
                user = userService.findUserByFirebaseUid(firebaseUid);
            }
            // Fallback to security context
            else {
                user = securityUtils.getCurrentUser();
            }
            
            if (user == null) {
                throw new RuntimeException("User not found");
            }
            
            // Join the room with the specific user
            GameRoom room = gameService.joinRoomWithUser(roomCode, user);
            
            if (room == null) {
                throw new RuntimeException("Room not found or already full");
            }
            
            // Notify room about the new player via WebSocket
            GameMessage joinedMessage = new GameMessage();
            joinedMessage.setType(MessageType.ROOM_JOINED);
            joinedMessage.setRoomCode(room.getRoomCode());
            joinedMessage.setSenderId(user.getId());
            joinedMessage.setSenderUsername(user.getUsername());
            joinedMessage.setContent(user.getUsername() + " joined the room");
            
            // Send to everyone in the room
            sendMessageToRoom(room.getRoomCode(), joinedMessage);
            
            log.info("User {} joined room {} via API", user.getUsername(), roomCode);
            return room;
            
        } catch (Exception e) {
            log.error("Error joining room via API: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Helper method to send a message to a specific user
     */
    private void sendMessageToUser(String username, GameMessage message) {
        try {
            log.info("Sending direct message to user {}: type={}", username, message.getType());
            
            messagingTemplate.convertAndSendToUser(
                username,
                PERSONAL_QUEUE,
                message
            );
        } catch (Exception e) {
            log.error("Error sending message to user {}: {}", username, e.getMessage(), e);
        }
    }
} 