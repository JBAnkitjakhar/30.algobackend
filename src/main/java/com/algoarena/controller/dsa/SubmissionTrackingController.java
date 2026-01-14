// src/main/java/com/algoarena/controller/dsa/SubmissionTrackingController.java
package com.algoarena.controller.dsa;

import com.algoarena.dto.dsa.SubmissionHistoryDTO;
import com.algoarena.model.User;
import com.algoarena.service.dsa.SubmissionTrackingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

// so this endpoint not coverd by explicity in securtiy config but we also have .any request so it is covered in else condition if we say and here also preauthorize then it will be double security check 
@RestController
@RequestMapping("/submissions")
@PreAuthorize("isAuthenticated()")
public class SubmissionTrackingController {

    @Autowired
    private SubmissionTrackingService submissionTrackingService;

    @GetMapping("/history")
    public ResponseEntity<SubmissionHistoryDTO> getMySubmissionHistory(Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        SubmissionHistoryDTO history = submissionTrackingService.getUserSubmissionHistory(currentUser.getId());
        return ResponseEntity.ok(history);
    }
}