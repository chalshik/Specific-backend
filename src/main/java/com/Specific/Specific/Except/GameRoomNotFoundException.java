package com.Specific.Specific.Except;

public class GameRoomNotFoundException extends RuntimeException {
    public GameRoomNotFoundException(String message) {
        super(message);
    }
} 