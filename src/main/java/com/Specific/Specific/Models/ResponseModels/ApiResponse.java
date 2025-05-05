package com.Specific.Specific.Models.ResponseModels;

/**
 * Standard API response object
 */
public class ApiResponse {
    private String message;
    private String status;
    
    public ApiResponse() {
    }
    
    public ApiResponse(String message, String status) {
        this.message = message;
        this.status = status;
    }
    
    /**
     * Create a success response
     * 
     * @param message Success message
     * @return ApiResponse with SUCCESS status
     */
    public static ApiResponse success(String message) {
        return new ApiResponse(message, "SUCCESS");
    }
    
    /**
     * Create an error response
     * 
     * @param message Error message
     * @return ApiResponse with ERROR status
     */
    public static ApiResponse error(String message) {
        return new ApiResponse(message, "ERROR");
    }
    
    // Getters and setters
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
} 