// src/main/java/com/algoarena/controller/compiler/QuestionCompilerController.java
package com.algoarena.controller.compiler;

import com.algoarena.dto.compiler.runmode.RunCodeRequest;
import com.algoarena.dto.compiler.runmode.RunCodeResponse;
import com.algoarena.dto.compiler.submitmode.SubmitCodeRequest;
import com.algoarena.dto.compiler.submitmode.SubmitCodeResponse;
import com.algoarena.model.User;
import com.algoarena.service.compiler.runmode.RunModeService;
import com.algoarena.service.compiler.submitmode.SubmitModeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/question-compiler")
@PreAuthorize("isAuthenticated()")
public class QuestionCompilerController {

    @Autowired
    private RunModeService runModeService;

    @Autowired
    private SubmitModeService submitModeService;

    /**
     * Run code with user-provided test cases (1-5 custom)
     * POST /api/question-compiler/{questionId}/run
     */
    @PostMapping("/{questionId}/run")
    public ResponseEntity<RunCodeResponse> runCode(
            @PathVariable String questionId,
            @Valid @RequestBody RunCodeRequest request) {
        
        try {
            RunCodeResponse response = runModeService.executeRunMode(questionId, request);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            RunCodeResponse errorResponse = new RunCodeResponse(false, "WRONG_ANSWER", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
            
        } catch (Exception e) {
            RunCodeResponse errorResponse = new RunCodeResponse(false, "WRONG_ANSWER", "Execution failed: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Submit code with all testcases from DB + create approach
     * POST /api/question-compiler/{questionId}/submit
     */
    @PostMapping("/{questionId}/submit")
    public ResponseEntity<SubmitCodeResponse> submitCode(
            @PathVariable String questionId,
            @Valid @RequestBody SubmitCodeRequest request,
            Authentication authentication) {
        
        try {
            User currentUser = (User) authentication.getPrincipal();
            SubmitCodeResponse response = submitModeService.executeSubmitMode(questionId, request, currentUser);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            SubmitCodeResponse errorResponse = new SubmitCodeResponse(false, "WRONG_ANSWER", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
            
        } catch (Exception e) {
            SubmitCodeResponse errorResponse = new SubmitCodeResponse(false, "WRONG_ANSWER", "Execution failed: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}