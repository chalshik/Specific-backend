package com.Specific.Specific.Services;

import com.Specific.Specific.Models.Game.GameRoom;
import com.Specific.Specific.Models.Player;
import com.Specific.Specific.Models.Question;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
@Service
public class GameService {
    private ConcurrentHashMap<String, GameRoom> activeGames = new ConcurrentHashMap();
    public GameRoom createGame(Player player1) {
        String code = "1234";
        GameRoom newRoom = new GameRoom(code, QuestionBank.getQuestions());
        newRoom.addPlayer(player1);
        activeGames.put(code, newRoom);
        return newRoom;
    }
}
