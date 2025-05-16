package com.Specific.Specific.Services;

import com.Specific.Specific.Controllers.GameController;
import com.Specific.Specific.Models.Game.GameRoom;
import com.Specific.Specific.Models.Player;
import com.Specific.Specific.Models.Question;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
@Service
public class GameService {
    private ConcurrentHashMap<String, GameRoom> activeGames = new ConcurrentHashMap();
    public GameRoom createGame(Player player1) {
        String code = generateGameCode();
        GameRoom newRoom = new GameRoom(code, QuestionBank.getQuestions());
        newRoom.addPlayer(player1);
        activeGames.put(code, newRoom);
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
        return activeGames.get(gameCode);
    }
    public boolean joinRoom(Player player2, String gameCode) {
        GameRoom gameRoom = activeGames.get(gameCode);
        if (gameRoom == null) {
            System.out.println("Room not found: " + gameCode);
            return false; // Room not found
        }

        synchronized (gameRoom) {
            if (!gameRoom.getStatus().equals(GameRoom.GameStatus.WAITING)) {
                System.out.println("Game not in WAITING status: " + gameRoom.getStatus());
                return false; // Game already started or finished
            }

            if (gameRoom.getPlayers().contains(player2)) {
                System.out.println("Player already joined: " + player2.getUsername());
                return false; // Player already joined
            }

            gameRoom.addPlayer(player2);
        }
        System.out.println("Player joined successfully: " + player2.getUsername());
        return true;
    }

}
