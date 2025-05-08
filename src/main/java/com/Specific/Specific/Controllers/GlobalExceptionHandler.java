package com.Specific.Specific.Controllers;

import com.Specific.Specific.Except.*;
import com.Specific.Specific.Models.ResponseModels.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.JDBCConnectionException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.persistence.PersistenceException;
import java.sql.SQLException;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
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
        log.error("Illegal argument exception", ex);
        return ApiResponse.error(ex.getMessage());
    }
    
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponse handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.error("Data integrity violation", ex);
        String message = "Data integrity violation: ";
        if (ex.getCause() instanceof org.hibernate.exception.ConstraintViolationException) {
            message += "A record with this information already exists.";
        } else {
            message += "A database constraint was violated.";
        }
        return ApiResponse.error(message);
    }
    
    @ExceptionHandler(JDBCConnectionException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ApiResponse handleJdbcConnectionException(JDBCConnectionException ex) {
        log.error("Database connection error", ex);
        return ApiResponse.error("Database connection failed. Please try again later.");
    }
    
    @ExceptionHandler(DataAccessException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ApiResponse handleDataAccessException(DataAccessException ex) {
        log.error("Data access error", ex);
        return ApiResponse.error("Database access error: " + ex.getMessage());
    }

    @ExceptionHandler(PersistenceException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ApiResponse handlePersistenceException(PersistenceException ex) {
        log.error("Persistence error", ex);
        return ApiResponse.error("Database persistence error: " + ex.getMessage());
    }
    
    @ExceptionHandler(SQLException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ApiResponse handleSQLException(SQLException ex) {
        log.error("SQL error", ex);
        return ApiResponse.error("SQL error: " + ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse handleGeneralException(Exception ex) {
        log.error("Unexpected error", ex);
        return ApiResponse.error("An unexpected error occurred: " + ex.getMessage());
    }
}
