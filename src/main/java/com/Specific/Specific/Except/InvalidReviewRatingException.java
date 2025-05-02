package com.Specific.Specific.Except;

public class InvalidReviewRatingException extends RuntimeException {
    public InvalidReviewRatingException(String message) {
        super(message);
    }
} 