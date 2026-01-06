// src/main/java/com/algoarena/controller/admin/MigrationController.java
package com.algoarena.controller.admin;

import com.algoarena.service.migration.SolutionMigrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/migration")
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPERADMIN')")
public class MigrationController {

    @Autowired
    private SolutionMigrationService solutionMigrationService;

    /**
     * GET /api/admin/migration/solutions/status
     * Check migration status
     */
    @GetMapping("/solutions/status")
    public ResponseEntity<Map<String, Object>> getMigrationStatus() {
        Map<String, Object> status = solutionMigrationService.getMigrationStatus();
        return ResponseEntity.ok(status);
    }

    /**
     * POST /api/admin/migration/solutions/migrate
     * Migrate all solutions to new format
     */
    @PostMapping("/solutions/migrate")
    public ResponseEntity<Map<String, Object>> migrateSolutions() {
        Map<String, Object> result = solutionMigrationService.migrateSolutionsToNewFormat();
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/admin/migration/solutions/rollback
     * Rollback migration (emergency use only)
     * WARNING: Only keeps first code template per language!
     */
    @PostMapping("/solutions/rollback")
    public ResponseEntity<Map<String, Object>> rollbackMigration() {
        Map<String, Object> result = solutionMigrationService.rollbackMigration();
        return ResponseEntity.ok(result);
    }
}