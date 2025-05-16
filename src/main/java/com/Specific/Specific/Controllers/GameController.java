package com.Specific.Specific.Controllers;

import com.Specific.Specific.Models.Game.GameRoom;
import com.Specific.Specific.Models.Player;
import com.Specific.Specific.Services.GameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GameController {

    private final SimpMessagingTemplate messagingTemplate;
    private GameService gameService;
    GameController(GameService gameService, SimpMessagingTemplate messagingTemplate){
        this.gameService = gameService;
        this.messagingTemplate = messagingTemplate;
    }
    @PostMapping("/game/create")
    public GameRoom createGame(@RequestBody Player player){
       return gameService.createGame(player);
    }

    @PostMapping("/game/join/{gameCode}")
    public Boolean joinGame(@RequestBody Player player, @PathVariable String gameCode){
       boolean joined =  gameService.joinRoom(player,gameCode);
       GameRoom gameRoom = gameService.getGameRoom(gameCode);

       if (joined){
           messagingTemplate.convertAndSend("/topic/game/"+gameCode+"/players",gameRoom.getPlayers());
       }
       return joined;
    }
}
