package com.Specific.Specific.Controllers;

import com.Specific.Specific.Except.*;
import com.Specific.Specific.Models.ResponseModels.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse handleUserNotFound(UserNotFoundException ex) {
        return ApiResponse.error(ex.getMessage());
    }
    
    @ExceptionHandler(DeckNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse handleDeckNotFound(DeckNotFoundException ex) {
        return ApiResponse.error(ex.getMessage());
    }
    
    @ExceptionHandler(CardNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse handleCardNotFound(CardNotFoundException ex) {
        return ApiResponse.error(ex.getMessage());
    }
    
    @ExceptionHandler(BookNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse handleBookNotFound(BookNotFoundException ex) {
        return ApiResponse.error(ex.getMessage());
    }
    
    @ExceptionHandler(ReviewNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse handleReviewNotFound(ReviewNotFoundException ex) {
        return ApiResponse.error(ex.getMessage());
    }
    
    @ExceptionHandler(InvalidReviewRatingException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse handleInvalidReviewRating(InvalidReviewRatingException ex) {
        return ApiResponse.error(ex.getMessage());
    }
    
    @ExceptionHandler(UnauthorizedAccessException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse handleUnauthorizedAccess(UnauthorizedAccessException ex) {
        return ApiResponse.error(ex.getMessage());
    }
    
    @ExceptionHandler(GameRoomNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse handleGameRoomNotFound(GameRoomNotFoundException ex) {
        return ApiResponse.error(ex.getMessage());
    }
    
    @ExceptionHandler(GameAlreadyStartedException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponse handleGameAlreadyStarted(GameAlreadyStartedException ex) {
        return ApiResponse.error(ex.getMessage());
    }
    
    @ExceptionHandler(PlayerAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponse handlePlayerAlreadyExists(PlayerAlreadyExistsException ex) {
        return ApiResponse.error(ex.getMessage());
    }
    
    @ExceptionHandler(InvalidGameInputException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse handleInvalidGameInput(InvalidGameInputException ex) {
        return ApiResponse.error(ex.getMessage());
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse handleValidationExceptions(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        
        return ApiResponse.error("Validation failed: " + errorMessage);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse handleIllegalArgument(IllegalArgumentException ex) {
        return ApiResponse.error(ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse handleGeneralException(Exception ex) {
        return ApiResponse.error("An unexpected error occurred");
    }
}
