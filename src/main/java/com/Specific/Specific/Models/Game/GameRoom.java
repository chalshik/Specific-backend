package com.Specific.Specific.Models.Game;

import com.Specific.Specific.Except.GameNotActiveException;
import com.Specific.Specific.Models.Player;
import com.Specific.Specific.Models.Question;
import org.aspectj.weaver.patterns.TypePatternQuestions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

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
    public boolean submitAns(Player player, Integer answer){
        if (status != GameStatus.ACTIVE) {
            throw new GameNotActiveException("Game is not active, cannot submit answer");
        }
        answeredPlayers.put(player,true);
        if(getCurrentQuestion().getAnswer().equals(answer)){
            scores.put(player,scores.getOrDefault(player,0)+1);
        };
       if(answeredPlayers.size()==players.size()&&questionIndex<questions.size()-1){
            questionIndex++;
            answeredPlayers.clear();
            return true;
        }
        return false;
    }
    public ConcurrentHashMap<Player,Integer> getScores(){
        return scores;
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
