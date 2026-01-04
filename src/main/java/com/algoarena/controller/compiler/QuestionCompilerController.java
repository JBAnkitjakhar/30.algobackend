// src/main/java/com/algoarena/controller/compiler/QuestionCompilerController.java
package com.algoarena.controller.compiler;

import com.algoarena.dto.compiler.QuestionExecutionRequest;
import com.algoarena.dto.compiler.QuestionExecutionResponse;
import com.algoarena.service.compiler.QuestionCompilerService;
import com.algoarena.service.compiler.QueueService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/question-compiler")
@PreAuthorize("isAuthenticated()")
public class QuestionCompilerController {

    @Autowired
    private QuestionCompilerService questionCompilerService;

    /**
     * Execute question code with testcases
     */ 
    // http://localhost:8080/api/question-compiler/execute
    @PostMapping("/execute")
    public ResponseEntity<Map<String, Object>> executeQuestion(
            @Valid @RequestBody QuestionExecutionRequest request,
            Authentication authentication) {
        
        String userId = authentication.getName(); // Get user ID from auth
        
        QuestionExecutionResponse result = questionCompilerService.executeQuestion(request, userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", result.isSuccess());
        response.put("message", result.getMessage());
        
        if (result.isSuccess()) {
            response.put("testCaseResults", result.getTestCaseResults());
            response.put("metrics", result.getMetrics());
        } else {
            response.put("error", result.getError());
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get queue status
     */
    @GetMapping("/queue-status")
    public ResponseEntity<Map<String, Object>> getQueueStatus() {
        QueueService.QueueStatus status = questionCompilerService.getQueueStatus();
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", status);
        
        return ResponseEntity.ok(response);
    }
}
