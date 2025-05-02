package com.Specific.Specific.Controllers;

import com.Specific.Specific.Except.BookNotFoundException;
import com.Specific.Specific.Except.CardNotFoundException;
import com.Specific.Specific.Except.DeckNotFoundException;
import com.Specific.Specific.Except.UnauthorizedAccessException;
import com.Specific.Specific.Except.UserNotFoundException;
import com.Specific.Specific.Models.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
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
    
    @ExceptionHandler(UnauthorizedAccessException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse handleUnauthorizedAccess(UnauthorizedAccessException ex) {
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

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse handleGeneralException(Exception ex) {
        return ApiResponse.error("An unexpected error occurred");
    }
}
