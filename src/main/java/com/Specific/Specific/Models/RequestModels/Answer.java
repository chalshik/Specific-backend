package com.Specific.Specific.Models.RequestModels;

public class Answer {
    private String answer;
    private String question;
    private String username;
    private String gameroom;

    public String getGameroom() {
        return gameroom;
    }

    public void setGameroom(String gameroom) {
        this.gameroom = gameroom;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }
}
