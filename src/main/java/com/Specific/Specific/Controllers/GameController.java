package com.Specific.Specific.Controllers;

import com.Specific.Specific.Except.InvalidGameInputException;
import com.Specific.Specific.Models.Game.GameRoom;
import com.Specific.Specific.Models.Player;
import com.Specific.Specific.Models.Question;
import com.Specific.Specific.Models.RequestModels.Answer;
import com.Specific.Specific.Models.ResponseModels.ApiResponse;
import com.Specific.Specific.Models.ResponseModels.CreatedRoom;
import com.Specific.Specific.Services.GameService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/game")
public class GameController {
    private static final Logger logger = LoggerFactory.getLogger(GameController.class);
    private final SimpMessagingTemplate messagingTemplate;
    private final GameService gameService;
    
    public GameController(GameService gameService, SimpMessagingTemplate messagingTemplate) {
        this.gameService = gameService;
        this.messagingTemplate = messagingTemplate;
    }
    
    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    public CreatedRoom createGame(@RequestBody Player player) {
        logger.info("Received request to create a game from player: {}", player.getId());
        
        if (player.getId() == null || player.getId().isEmpty()) {
            logger.warn("Create game request with invalid player data");
            throw new InvalidGameInputException("Player ID cannot be empty");
        }
        
        GameRoom gameRoom = gameService.createGame(player);
        return new CreatedRoom(gameRoom.getRoomcode());
    }

    @PostMapping("/join/{gameCode}")
    public ResponseEntity<ApiResponse> joinGame(@RequestBody Player player, @PathVariable String gameCode) {
        logger.info("Player {} requesting to join game: {}", player.getId(), gameCode);
        
        if (player.getId() == null || player.getId().isEmpty()) {
            logger.warn("Join game request with invalid player data");
            throw new InvalidGameInputException("Player ID cannot be empty");
        }
        
        if (gameCode == null || gameCode.isEmpty()) {
            logger.warn("Join game request with invalid game code");
            throw new InvalidGameInputException("Game code cannot be empty");
        }
        
        gameService.joinRoom(player, gameCode);
        GameRoom gameRoom = gameService.getGameRoom(gameCode);
        
        // Notify all players about the new player
        messagingTemplate.convertAndSend("/topic/game/"+gameCode+"/players", gameRoom.getPlayers());
        
        return ResponseEntity.ok(ApiResponse.success("Joined game successfully"));
    }
    
    @MessageMapping("/game/submit")
    public void submitAnswer(Answer answer) {
        logger.info("Received answer from player {} for game {}", 
            answer.getUsername(), answer.getGameroom());
            
        if (answer.getGameroom() == null || answer.getUsername() == null ||
            answer.getAnswer() == null || answer.getQuestion() == null) {
            logger.warn("Invalid answer submission data");
            throw new InvalidGameInputException("Invalid answer submission data");
        }
        
        GameRoom gameRoom = gameService.getGameRoom(answer.getGameroom());
        Player player = new Player(answer.getUsername());
        boolean shouldMoveNext = gameRoom.submitAns(player);

        if (shouldMoveNext) {
            Question nextQuestion = gameRoom.getCurrentQuestion();
            if (nextQuestion != null) {
                logger.info("Moving to next question in game: {}", answer.getGameroom());
                messagingTemplate.convertAndSend(
                    "/topic/game/" + gameRoom.getRoomcode() + "/questions",
                    nextQuestion
                );
            } else {
                logger.info("Game {} has ended, no more questions", answer.getGameroom());
                gameRoom.setStatus(GameRoom.GameStatus.FINISHED);
                messagingTemplate.convertAndSend(
                    "/topic/game/" + gameRoom.getRoomcode() + "/end",
                    "Game has ended"
                );
            }
        } else {
            messagingTemplate.convertAndSend(
                "/topic/game/" + gameRoom.getRoomcode() + "/answer",
                answer.getUsername()
            );
        }
    }
    
    @MessageExceptionHandler
    public void handleException(Exception e) {
        logger.error("WebSocket error: {}", e.getMessage(), e);
    }
    
    @PostMapping("/start/{gameCode}")
    public ResponseEntity<ApiResponse> startGame(@PathVariable String gameCode) {
        logger.info("Request to start game: {}", gameCode);
        
        if (gameCode == null || gameCode.isEmpty()) {
            logger.warn("Start game request with invalid game code");
            throw new InvalidGameInputException("Game code cannot be empty");
        }
        
        boolean started = gameService.startGame(gameCode);
        
        if (started) {
            GameRoom gameRoom = gameService.getGameRoom(gameCode);
            Question firstQuestion = gameRoom.getCurrentQuestion();
            
            // Notify all clients that the game has started with the first question
            messagingTemplate.convertAndSend(
                "/topic/game/" + gameCode + "/questions",
                firstQuestion
            );
            
            return ResponseEntity.ok(ApiResponse.success("Game started successfully"));
        } else {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("Game could not be started"));
        }
    }
}
