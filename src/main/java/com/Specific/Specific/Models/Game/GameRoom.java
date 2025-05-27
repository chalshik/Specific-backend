package com.Specific.Specific.Models.Game;

import com.Specific.Specific.Except.GameNotActiveException;
import com.Specific.Specific.Models.Player;
import com.Specific.Specific.Models.Question;
import org.aspectj.weaver.patterns.TypePatternQuestions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class GameRoom {
    public enum GameStatus {
        WAITING, ACTIVE, FINISHED
    }
    private ConcurrentHashMap<Player,Boolean> answeredPlayers = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Player,Integer> scores = new ConcurrentHashMap<>();
    private String roomcode;
    private GameStatus status;
    private List<Player> players = new ArrayList<>();
    private List<Question> questions = new ArrayList<>();
    private int questionIndex;
    public GameRoom(String roomcode, List<Question> questions) {
        this.roomcode = roomcode;
        this.questions = questions;
        this.status = GameStatus.WAITING;
        this.questionIndex = 0;
    }
    public Integer getAnsweredPlayersCount(){
        return answeredPlayers.size();
    }
    public boolean submitAns(Player player, Integer answer) {
        if (status != GameStatus.ACTIVE) {
            throw new GameNotActiveException("Game is not active, cannot submit answer");
        }

        answeredPlayers.put(player, true);

        // Check if answer is correct
        if(getCurrentQuestion().getAnswer().equals(answer)) {
            scores.put(player, scores.getOrDefault(player, 0) + 1);
        };

        // All players have answered
        if(answeredPlayers.size() == players.size()) {
            // If this was the last question
            if(questionIndex >= questions.size() - 1) {
                finishGame();
                return true; // Signal that game should end
            }
            // More questions remain
            else {
                questionIndex++;
                answeredPlayers.clear();
                return true; // Signal to move to next question
            }
        }
        return false; // Not all players have answered
    }
    public Map<String,Integer> getScores(){
       Map<String, Integer> playerScoresById = scores.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> entry.getKey().getId(),  // key: player ID
                        Map.Entry::getValue               // value: score
                ));

        return playerScoresById;
    }
    public boolean finishGame(){
        status = GameStatus.FINISHED;
        return true;
    }
    public void addPlayer(Player player){
        players.add(player);
    }
    public String getRoomcode() {
        return roomcode;
    }

    public void setRoomcode(String roomcode) {
        this.roomcode = roomcode;
    }

    public GameStatus getStatus() {
        return status;
    }

    public void setStatus(GameStatus status) {
        this.status = status;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public void setPlayers(List<Player> players) {
        this.players = players;
    }

    public List<Question> getQuestions() {
        return questions;
    }

    public void setQuestions(List<Question> questions) {
        this.questions = questions;
    }





    public int getQuestionIndex() {
        return questionIndex;
    }

    public void setQuestionIndex(int questionIndex) {
        this.questionIndex = questionIndex;
    }
    public Question getCurrentQuestion(){
        if(questionIndex>questions.size()-1){
        return null;
        }
        return questions.get(questionIndex);
    }
}
