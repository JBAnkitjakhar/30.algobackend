// // src/main/java/com/algoarena/controller/admin/QuestionMethodNameMigrationController.java
// package com.algoarena.controller.admin;

// import com.algoarena.service.migration.QuestionMethodNameMigrationService;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.http.ResponseEntity;
// import org.springframework.security.access.prepost.PreAuthorize;
// import org.springframework.web.bind.annotation.*;

// import java.time.LocalDateTime;
// import java.util.Map;

// @RestController
// @RequestMapping("/admin/migrations/question-methodname")
// @PreAuthorize("hasRole('SUPERADMIN')")
// public class QuestionMethodNameMigrationController {

//     private static final Logger logger = LoggerFactory.getLogger(QuestionMethodNameMigrationController.class);

//     @Autowired
//     private QuestionMethodNameMigrationService migrationService;

//     /**
//      * Remove methodName field from all questions
//      * POST /api/admin/migrations/question-methodname/remove-field
//      */
//     @PostMapping("/remove-field")
//     public ResponseEntity<Map<String, Object>> removeMethodNameField() {
//         logger.info("🔍 ========================================");
//         logger.info("🔍 Migration: Remove methodName Field CALLED!");
//         logger.info("🔍 Time: {}", LocalDateTime.now());
//         logger.info("🔍 ========================================");

//         Map<String, Object> result = migrationService.removeMethodNameField();
        
//         if ((boolean) result.get("success")) {
//             return ResponseEntity.ok(result);
//         } else {
//             return ResponseEntity.status(500).body(result);
//         }
//     }

//     /**
//      * Verify migration completed successfully
//      * GET /api/admin/migrations/question-methodname/verify
//      */
//     @GetMapping("/verify")
//     public ResponseEntity<Map<String, Object>> verifyMigration() {
//         logger.info("🔍 Verifying methodName field removal...");
        
//         Map<String, Object> result = migrationService.verifyMigration();
//         return ResponseEntity.ok(result);
//     }

//     /**
//      * Get migration statistics
//      * GET /api/admin/migrations/question-methodname/stats
//      */
//     @GetMapping("/stats")
//     public ResponseEntity<Map<String, Object>> getMigrationStats() {
//         logger.info("📊 Getting migration statistics...");
        
//         Map<String, Object> stats = migrationService.getMigrationStats();
//         return ResponseEntity.ok(stats);
//     }
// }
// // ```

// // ## Usage in Postman:

// // **1. Check Stats First (Optional):**
// // ```
// // GET http://localhost:8080/api/admin/migrations/question-methodname/stats
// // ```

// // **2. Run Migration:**
// // ```
// // POST http://localhost:8080/api/admin/migrations/question-methodname/remove-field
// // ```

// // **3. Verify Migration:**
// // ```
// // GET http://localhost:8080/api/admin/migrations/question-methodname/verify