package com.Specific.Specific.Models;

public class Question {
    private String question;
    private  Integer index;
    private String[] options;

    public Question(String question, Integer index, String[] options) {
        this.question = question;
        this.index = index;
        this.options = options;
    }
    public String getQuestion() {
        return question;
    }
    public Integer getAnswer() {
        return index;
    }
    public String[] getOptions() {
        return options;
    }
    public void setOptions(String[] options) {
        this.options = options;
    }
    public void setQuestion(String question) {
        this.question = question;
    }
    public void setAnswer(Integer index) {
        this.index = index;
    }
}
