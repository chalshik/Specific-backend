package com.Specific.Specific.Models.Game;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;


public class GameResult {

    private String roomCode;
    private String hostUsername;
    private String guestUsername;
    private int hostScore;
    private int guestScore;
    private String winnerUsername;

    public GameResult(String roomCode, String hostUsername, String guestUsername, int hostScore, int guestScore, String winnerUsername) {
        this.roomCode = roomCode;
        this.hostUsername = hostUsername;
        this.guestUsername = guestUsername;
        this.hostScore = hostScore;
        this.guestScore = guestScore;
        this.winnerUsername = winnerUsername;
    }

    public String getRoomCode() {
        return roomCode;
    }

    public void setRoomCode(String roomCode) {
        this.roomCode = roomCode;
    }

    public String getHostUsername() {
        return hostUsername;
    }

    public void setHostUsername(String hostUsername) {
        this.hostUsername = hostUsername;
    }

    public String getGuestUsername() {
        return guestUsername;
    }

    public void setGuestUsername(String guestUsername) {
        this.guestUsername = guestUsername;
    }

    public int getHostScore() {
        return hostScore;
    }

    public void setHostScore(int hostScore) {
        this.hostScore = hostScore;
    }

    public int getGuestScore() {
        return guestScore;
    }

    public void setGuestScore(int guestScore) {
        this.guestScore = guestScore;
    }

    public String getWinnerUsername() {
        return winnerUsername;
    }

    public void setWinnerUsername(String winnerUsername) {
        this.winnerUsername = winnerUsername;
    }
}