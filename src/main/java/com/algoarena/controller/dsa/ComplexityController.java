//src/main/java/com/algoarena/controller/dsa/ComplexityController.java

package com.algoarena.controller.dsa;

import com.algoarena.dto.complexity.ComplexityAnalysisRequest;
import com.algoarena.dto.complexity.ComplexityAnalysisResponse;
import com.algoarena.service.complexity.ComplexityAnalysisService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/complexity")
@PreAuthorize("isAuthenticated()")
public class ComplexityController {

    @Autowired
    private ComplexityAnalysisService complexityAnalysisService;

    /**
     * POST /api/complexity/analyze
     * Analyze code complexity using Gemini AI
     */
    @PostMapping("/analyze")
    public ResponseEntity<Map<String, Object>> analyzeComplexity(
            @Valid @RequestBody ComplexityAnalysisRequest request,
            Authentication authentication) {

        try {
            ComplexityAnalysisResponse response = complexityAnalysisService.analyzeComplexity(request);
            
            Map<String, Object> successResponse = new HashMap<>();
            successResponse.put("success", true);
            successResponse.put("message", "Complexity analysis completed successfully");
            successResponse.put("data", response);
            
            return ResponseEntity.ok(successResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to analyze complexity");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}