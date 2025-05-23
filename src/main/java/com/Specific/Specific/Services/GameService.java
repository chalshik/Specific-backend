package com.Specific.Specific.Services;

import com.Specific.Specific.Except.GameAlreadyStartedException;
import com.Specific.Specific.Except.GameRoomNotFoundException;
import com.Specific.Specific.Except.PlayerAlreadyExistsException;
import com.Specific.Specific.Controllers.GameController;
import com.Specific.Specific.Models.Game.GameRoom;
import com.Specific.Specific.Models.Player;
import com.Specific.Specific.Models.Question;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GameService {
    private static final Logger logger = LoggerFactory.getLogger(GameService.class);
    private ConcurrentHashMap<String, GameRoom> activeGames = new ConcurrentHashMap<>();
    public List<String> getRooms(){
        List<String> rooms = new ArrayList<>();
        Set<String> activeroom = activeGames.keySet();
        for(String code :activeroom){
            rooms.add(code);
        }
        return rooms;
    }
    public GameRoom createGame(Player player1) {
        logger.info("Creating new game room for player: {}", player1.getId());
        String code = generateGameCode();
        GameRoom newRoom = new GameRoom(code, QuestionBank.getQuestions());
        newRoom.addPlayer(player1);
        activeGames.put(code, newRoom);
        logger.info("Created game room with code: {}", code);
        return newRoom;
    }
    
    private String generateGameCode() {
        final String CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        final int CODE_LENGTH = 6;
        SecureRandom random = new SecureRandom();

        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CODE_CHARS.charAt(random.nextInt(CODE_CHARS.length())));
        }
        return sb.toString();
    }
    
    public GameRoom getGameRoom(String gameCode) {
        GameRoom gameRoom = activeGames.get(gameCode);
        if (gameRoom == null) {
            logger.error("Game room not found with code: {}", gameCode);
            throw new GameRoomNotFoundException("Game room not found with code: " + gameCode);
        }
        return gameRoom;
    }
    
    public boolean joinRoom(Player player2, String gameCode) {
        logger.info("Attempting to join room {} for player: {}", gameCode, player2.getId());
        
        GameRoom gameRoom = activeGames.get(gameCode);
        if (gameRoom == null) {
            logger.error("Room not found: {}", gameCode);
            throw new GameRoomNotFoundException("Game room not found with code: " + gameCode);
        }

        synchronized (gameRoom) {
            if (!gameRoom.getStatus().equals(GameRoom.GameStatus.WAITING)) {
                logger.error("Game not in WAITING status: {}", gameRoom.getStatus());
                throw new GameAlreadyStartedException("Game has already started or finished");
            }

            if (gameRoom.getPlayers().contains(player2)) {
                logger.error("Player already joined: {}", player2.getId());
                throw new PlayerAlreadyExistsException("Player already exists in this game room");
            }

            gameRoom.addPlayer(player2);
        }
        logger.info("Player {} joined successfully to room: {}", player2.getId(), gameCode);
        return true;
    }
    
    /**
     * Start a game room if in WAITING status
     * @param gameCode the code of the game room
     * @return true if game started, false otherwise
     */
   
    public boolean startGame(String gameCode) {
        try {
            GameRoom gameRoom = getGameRoom(gameCode);
            
            synchronized (gameRoom) {
                if (gameRoom.getStatus() == GameRoom.GameStatus.WAITING) {
                    gameRoom.setStatus(GameRoom.GameStatus.ACTIVE);
                    logger.info("Game started in room: {}", gameCode);
                    return true;
                }
                logger.warn("Cannot start game in room {}: game is in state {}", 
                    gameCode, gameRoom.getStatus());
                return false;
            }
        } catch (Exception e) {
            logger.error("Error starting game: {}", e.getMessage());
            return false;
        }
    }
}
