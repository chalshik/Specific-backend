package com.Specific.Specific.Except;

public class GameNotActiveException extends RuntimeException {
    public GameNotActiveException(String message) {
        super(message);
    }
}
