package com.Specific.Specific.Models.Game;

import com.Specific.Specific.Models.Entities.Card;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GameMessage {
    public enum MessageType {
        JOIN_ROOM,
        ROOM_JOINED,
        START_GAME,
        GAME_STARTED,
        NEXT_ROUND,
        ANSWER_SUBMITTED,
        ROUND_RESULT,
        GAME_OVER,
        ERROR,
        LEAVE_ROOM
    }
    
    private MessageType type;
    private String roomCode;
    private String senderUsername;
    private Long senderId;
    private String content;
    private Card currentCard;
    private List<String> cardOptions;
    private int correctOptionIndex;
    private int roundNumber;
    private int hostScore;
    private int guestScore;
    private GameResult gameResult;
    
    // Constructor for simple messages
    public GameMessage(MessageType type, String content) {
        this.type = type;
        this.content = content;
    }
    
    // Constructor for room joining
    public GameMessage(MessageType type, String roomCode, Long senderId, String senderUsername) {
        this.type = type;
        this.roomCode = roomCode;
        this.senderId = senderId;
        this.senderUsername = senderUsername;
    }
} 