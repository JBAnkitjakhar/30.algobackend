// src/main/java/com/algoarena/controller/admin/MigrationController.java
package com.algoarena.controller.admin;

import com.algoarena.service.migration.QuestionMigrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin/migrations")
@PreAuthorize("hasRole('SUPERADMIN')")
public class MigrationController {

    @Autowired
    private QuestionMigrationService migrationService;

    /**
     * Remove isExample, explanation, and functionSignature fields
     * 
     * POST http://localhost:8080/admin/migrations/questions/cleanup-testcase-fields
     */
    @PostMapping("/questions/cleanup-testcase-fields")
    public ResponseEntity<Map<String, Object>> cleanupTestcaseFields() {
        Map<String, Object> result = migrationService.cleanupTestcaseFields();
        return ResponseEntity.ok(result);
    }

    /**
     * Initialize empty code templates for questions
     * 
     * POST http://localhost:8080/admin/migrations/questions/init-code-templates
     */
    @PostMapping("/questions/init-code-templates")
    public ResponseEntity<Map<String, Object>> initializeCodeTemplates() {
        Map<String, Object> result = migrationService.initializeCodeTemplates();
        return ResponseEntity.ok(result);
    }

    /**
     * ⭐ IMPROVED: Initialize default time limits for all testcases
     * 
     * POST http://localhost:8080/admin/migrations/questions/init-time-limits
     */
    @PostMapping("/questions/init-time-limits")
    @CacheEvict(value = { "questionDetail", "adminQuestionsSummary", "questionsMetadata" }, allEntries = true)
    public ResponseEntity<Map<String, Object>> initializeTimeLimits() {
        Map<String, Object> result = migrationService.initializeTimeLimits();
        return ResponseEntity.ok(result);
    }
}