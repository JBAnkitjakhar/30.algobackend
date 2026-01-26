// // src/main/java/com/algoarena/controller/admin/QuestionTemplateMigrationController.java
// package com.algoarena.controller.admin;

// import com.algoarena.service.migration.QuestionTemplateMigrationService;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.http.ResponseEntity;
// import org.springframework.security.access.prepost.PreAuthorize;
// import org.springframework.web.bind.annotation.*;

// import java.time.LocalDateTime;
// import java.util.Map;

// @RestController
// @RequestMapping("/admin/migrations/question-templates")
// @PreAuthorize("hasRole('SUPERADMIN')")
// public class QuestionTemplateMigrationController {

//     private static final Logger logger = LoggerFactory.getLogger(QuestionTemplateMigrationController.class);

//     @Autowired
//     private QuestionTemplateMigrationService migrationService;

//     /**
//      * ✅ Migrate: generalTemplate → submitTemplate, correctSolution → runTemplate
//      * POST /api/admin/migrations/question-templates/rename-fields
//      */
//     @PostMapping("/rename-fields")
//     public ResponseEntity<Map<String, Object>> migrateTemplateFields() {
//         logger.info("🔍 ========================================");
//         logger.info("🔍 Migration: Rename Template Fields CALLED!");
//         logger.info("🔍 Time: {}", LocalDateTime.now());
//         logger.info("🔍 ========================================");

//         Map<String, Object> result = migrationService.migrateTemplateFields();
        
//         if ((boolean) result.get("success")) {
//             return ResponseEntity.ok(result);
//         } else {
//             return ResponseEntity.status(500).body(result);
//         }
//     }

//     /**
//      * ✅ Verify migration completed successfully
//      * GET /api/admin/migrations/question-templates/verify
//      */
//     @GetMapping("/verify")
//     public ResponseEntity<Map<String, Object>> verifyMigration() {
//         logger.info("🔍 Verifying question template migration...");
        
//         Map<String, Object> result = migrationService.verifyMigration();
//         return ResponseEntity.ok(result);
//     }

//     /**
//      * ✅ Get migration statistics
//      * GET /api/admin/migrations/question-templates/stats
//      */
//     @GetMapping("/stats")
//     public ResponseEntity<Map<String, Object>> getMigrationStats() {
//         logger.info("📊 Getting migration statistics...");
        
//         Map<String, Object> stats = migrationService.getMigrationStats();
//         return ResponseEntity.ok(stats);
//     }
// }