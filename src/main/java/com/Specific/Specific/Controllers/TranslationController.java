package com.Specific.Specific.Controllers;

import com.Specific.Specific.Models.RequestModels.RequestTranslation;
import com.Specific.Specific.Models.ResponseModels.ResponseTranslation;
import com.Specific.Specific.Services.TranslationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/translation")
public class TranslationController {
    @Autowired
    private TranslationService translationService;
    
    @PostMapping
    public CompletableFuture<ResponseTranslation> getTranslation(@RequestBody RequestTranslation requestTranslation) {
        return translationService.getTranslation(requestTranslation);
    }

    @GetMapping
    public String testEndpoint() {
        return "Translation API is working!";
    }
}
