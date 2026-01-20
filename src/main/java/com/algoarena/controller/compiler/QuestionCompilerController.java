// src/main/java/com/algoarena/controller/compiler/QuestionCompilerController.java
package com.algoarena.controller.compiler;

import com.algoarena.dto.compiler.*;
import com.algoarena.model.User;
import com.algoarena.service.compiler.QuestionCompilerService;
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
     * Run code with selected testcases (1-5)
     * POST /api/question-compiler/questions/{questionId}/run
     */
    @PostMapping("/questions/{questionId}/run")
    public ResponseEntity<Map<String, Object>> runCode(
            @PathVariable String questionId,
            @Valid @RequestBody RunCodeRequest request,
            Authentication authentication) {
        
        User currentUser = (User) authentication.getPrincipal();
        
        try {
            CodeExecutionResult result = questionCompilerService.runCode(
                questionId, 
                request, 
                currentUser.getId()
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("verdict", result.getVerdict());
            response.put("message", result.getMessage());
            
            if (result.isSuccess()) {
                response.put("testCaseResults", result.getTestCaseResults());
                response.put("metrics", result.getMetrics());
            } else {
                response.put("error", result.getError());
            }
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalStateException e) {
            // Container busy
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(503).body(response); // Service Unavailable
        }
    }

    /**
     * Submit code - tests ALL testcases and saves approach
     * POST /api/question-compiler/questions/{questionId}/submit
     */
    @PostMapping("/questions/{questionId}/submit")
    public ResponseEntity<Map<String, Object>> submitCode(
            @PathVariable String questionId,
            @Valid @RequestBody SubmitCodeRequest request,
            Authentication authentication) {
        
        User currentUser = (User) authentication.getPrincipal();
        
        try {
            CodeExecutionResult result = questionCompilerService.submitCode(
                questionId, 
                request, 
                currentUser
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("verdict", result.getVerdict());
            response.put("message", result.getMessage());
            
            if (result.isSuccess()) {
                response.put("testCaseResults", result.getTestCaseResults());
                response.put("metrics", result.getMetrics());
                if (result.getFailedTestCaseIndex() != null) {
                    response.put("failedTestCaseIndex", result.getFailedTestCaseIndex());
                }
            } else {
                response.put("error", result.getError());
            }
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalStateException e) {
            // Container busy
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(503).body(response);
        }
    }
}