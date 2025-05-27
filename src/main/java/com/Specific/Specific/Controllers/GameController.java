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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
    @GetMapping("/rooms")
    public List<String> getRooms(){
        return gameService.getRooms();
    }
    @PostMapping("/join/{gameCode}")
    public ResponseEntity<Map<String, Object>> joinGame(
            @RequestBody Player player,
            @PathVariable String gameCode) {

        // Input validation
        if (player.getId() == null || player.getId().isEmpty()) {
            throw new InvalidGameInputException("Player ID cannot be empty");
        }

        if (gameCode == null || gameCode.isEmpty()) {
            throw new InvalidGameInputException("Game code cannot be empty");
        }

        // Join room
        gameService.joinRoom(player, gameCode);
        GameRoom gameRoom = gameService.getGameRoom(gameCode);

        // Notify all players
        messagingTemplate.convertAndSend("/topic/game/"+gameCode+"/players", gameRoom.getPlayers());

        // Create response
        Map<String, Object> response = new HashMap<>();
        response.put("status", "Joined game successfully");
        response.put("players", gameRoom.getPlayers());

        // Return with explicit status code
        return new ResponseEntity<>(response, HttpStatus.OK); // 200
        // OR for creation semantics:
        // return new ResponseEntity<>(response, HttpStatus.CREATED); // 201
    }
//    @GetMapping("rooms/players")
//    public List<Map<String,List<String>>> getPlayers(){
//        return
//    }
    @MessageMapping("/game/submit")
    public void submitAnswer(Answer answer) {
        logger.info("Received answer from player {} for game {}", 
            answer.getUsername(), answer.getGameroom());
            
        if (answer.getGameroom() == null || answer.getUsername() == null ||
            answer.getIndex() == null) {
            logger.warn("Invalid answer submission data");
            throw new InvalidGameInputException("Invalid answer submission data");
        }
        
        GameRoom gameRoom = gameService.getGameRoom(answer.getGameroom());
        Player player = new Player(answer.getUsername());
        boolean shouldMoveNext = gameRoom.submitAns(player, answer.getIndex());

        if (shouldMoveNext) {
            if(gameRoom.getStatus() == GameRoom.GameStatus.FINISHED) {
                // Game ended
                messagingTemplate.convertAndSend(
                        "/topic/game/" + gameRoom.getRoomcode() + "/end",
                        gameRoom.getScores()
                );
            } else {
                // Send next question
                messagingTemplate.convertAndSend(
                        "/topic/game/" + gameRoom.getRoomcode() + "/questions",
                        gameRoom.getCurrentQuestion()
                );
            }
        } else {
            // Send answer count update
            messagingTemplate.convertAndSend(
                    "/topic/game/" + gameRoom.getRoomcode() + "/answer",
                    gameRoom.getAnsweredPlayersCount()
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

            // 1. Immediately notify clients that game is starting
            messagingTemplate.convertAndSend(
                    "/topic/game/" + gameCode + "/status",
                    Map.of("status", "STARTING", "message", "Game will begin in 5 seconds")
            );

            // 2. Schedule the first question after 5 seconds
            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.schedule(() -> {
                try {
                    messagingTemplate.convertAndSend(
                            "/topic/game/" + gameCode + "/questions",
                            firstQuestion
                    );

                    // Optional: Log that question was sent
                    logger.info("Sent first question to game: {}", gameCode);
                } catch (Exception e) {
                    logger.error("Failed to send first question to game {}: {}", gameCode, e.getMessage());
                }
            }, 5, TimeUnit.SECONDS);

            // Shutdown the scheduler when done
            scheduler.shutdown();

            return ResponseEntity.ok(ApiResponse.success("Game started successfully"));
        } else {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("Game could not be started"));
        }
    }
}
