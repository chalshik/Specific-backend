package com.Specific.Specific.Controllers;

import com.Specific.Specific.Models.RequestModels.RequestTranslation;
import com.Specific.Specific.Models.ResponseModels.ApiResponse;
import com.Specific.Specific.Models.ResponseModels.ResponseTranslation;
import com.Specific.Specific.Services.TranslationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/translation")
public class TranslationController {
    private static final Logger logger = LoggerFactory.getLogger(TranslationController.class);
    
    @Autowired
    private TranslationService translationService;
    
    @PostMapping
    public ResponseEntity<?> getTranslation(@RequestBody RequestTranslation requestTranslation) {
        logger.info("Translation request received for word: '{}' to language: {}", 
            requestTranslation.getWord(), requestTranslation.getDest_lang());
        
        try {
            CompletableFuture<ResponseTranslation> future = translationService.getTranslation(requestTranslation);
            ResponseTranslation result = future.get();
            
            if (result == null || result.getText() == null) {
                logger.error("Translation result is null or incomplete");
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Could not translate the provided text"));
            }
            
            logger.info("Translation successful: '{}' → '{}' ({})", 
                requestTranslation.getWord(), result.getText(), result.getDet_lang());
            return ResponseEntity.ok(result);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Translation request interrupted", e);
            return ResponseEntity.status(500)
                .body(ApiResponse.error("Translation request was interrupted"));
                
        } catch (ExecutionException e) {
            logger.error("Translation execution error", e.getCause());
            return ResponseEntity.status(500)
                .body(ApiResponse.error("Translation failed: " + e.getCause().getMessage()));
                
        } catch (Exception e) {
            logger.error("Unexpected error during translation", e);
            return ResponseEntity.status(500)
                .body(ApiResponse.error("An unexpected error occurred during translation"));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse> testEndpoint() {
        logger.info("Translation API test endpoint called");
        return ResponseEntity.ok(ApiResponse.success("Translation API is working!"));
    }
}
