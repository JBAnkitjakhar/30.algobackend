// src/main/java/com/algoarena/controller/compiler/QuestionCompilerController.java
package com.algoarena.controller.compiler;

import com.algoarena.dto.compiler.runmode.RunCodeRequest;
import com.algoarena.dto.compiler.runmode.RunCodeResponse;
import com.algoarena.service.compiler.runmode.RunModeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/question-compiler")
@PreAuthorize("isAuthenticated()")
public class QuestionCompilerController {

    @Autowired
    private RunModeService runModeService;

    /**
     * Run code with user-provided test cases
     * POST /api/question-compiler/questions/{questionId}/run
     */
    @PostMapping("/{questionId}/run")
    public ResponseEntity<RunCodeResponse> runCode(
            @PathVariable String questionId,
            @Valid @RequestBody RunCodeRequest request) {
        
        try {
            RunCodeResponse response = runModeService.executeRunMode(questionId, request);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            // Bad request (invalid language, etc.)
            RunCodeResponse errorResponse = new RunCodeResponse(false, "WRONG_ANSWER", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
            
        } catch (Exception e) {
            // Internal server error
            RunCodeResponse errorResponse = new RunCodeResponse(false, "WRONG_ANSWER", "Execution failed: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}