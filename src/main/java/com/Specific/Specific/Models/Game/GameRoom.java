package com.Specific.Specific.Models.Game;

import com.Specific.Specific.Models.Player;
import com.Specific.Specific.Models.Question;
import org.aspectj.weaver.patterns.TypePatternQuestions;

import java.util.ArrayList;
import java.util.List;

public class GameRoom {
    public enum GameStatus {
        WAITING, ACTIVE, FINISHED
    }
    private String roomcode;
    private GameStatus status;
    private List<Player> players = new ArrayList<>();
    private List<Question> questions = new ArrayList<>();

    private int currentRound;
    private int maxRound = 10;
    private int questionIndex;
    public GameRoom(String roomcode, List<Question> questions) {
        this.roomcode = roomcode;
        this.questions = questions;
        this.status = GameStatus.WAITING;
        this.currentRound = 0;
        this.questionIndex = 0;
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

    public int getCurrentRound() {
        return currentRound;
    }

    public void setCurrentRound(int currentRound) {
        this.currentRound = currentRound;
    }

    public int getMaxRound() {
        return maxRound;
    }

    public void setMaxRound(int maxRound) {
        this.maxRound = maxRound;
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
