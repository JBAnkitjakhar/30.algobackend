// // src/main/java/com/algoarena/controller/admin/UserApproachesMigrationController.java
// package com.algoarena.controller.admin;

// import com.algoarena.service.migration.UserApproachesMigrationService;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.http.ResponseEntity;
// import org.springframework.security.access.prepost.PreAuthorize;
// import org.springframework.web.bind.annotation.*;

// import java.time.LocalDateTime;
// import java.util.Map;

// @RestController
// @RequestMapping("/admin/migrations/user-approaches")
// @PreAuthorize("hasRole('SUPERADMIN')")
// public class UserApproachesMigrationController {

//     private static final Logger logger = LoggerFactory.getLogger(UserApproachesMigrationController.class);

//     @Autowired
//     private UserApproachesMigrationService migrationService;

//     /**
//      * ✅ Step 1: Convert List structure to Map structure
//      * POST /api/admin/migrations/user-approaches/list-to-map
//      */
//     @PostMapping("/list-to-map")
//     public ResponseEntity<Map<String, Object>> migrateListToMap() {
//         logger.info("🔍 ========================================");
//         logger.info("🔍 Migration: List → Map CALLED!");
//         logger.info("🔍 Time: {}", LocalDateTime.now());
//         logger.info("🔍 ========================================");

//         Map<String, Object> result = migrationService.migrateListToMap();
        
//         if ((boolean) result.get("success")) {
//             return ResponseEntity.ok(result);
//         } else {
//             return ResponseEntity.status(500).body(result);
//         }
//     }

//     /**
//      * ✅ Step 2: Fix totalApproaches count for all users
//      * POST /api/admin/migrations/user-approaches/fix-counts
//      */
//     @PostMapping("/fix-counts")
//     public ResponseEntity<Map<String, Object>> fixTotalApproachesCount() {
//         logger.info("🔍 ========================================");
//         logger.info("🔍 Fixing totalApproaches counts CALLED!");
//         logger.info("🔍 Time: {}", LocalDateTime.now());
//         logger.info("🔍 ========================================");

//         Map<String, Object> result = migrationService.fixTotalApproachesCount();
//         return ResponseEntity.ok(result);
//     }

//     /**
//      * ✅ Step 3: Verify migration completed successfully
//      * GET /api/admin/migrations/user-approaches/verify
//      */
//     @GetMapping("/verify")
//     public ResponseEntity<Map<String, Object>> verifyMigration() {
//         logger.info("🔍 Verifying UserApproaches migration...");
        
//         Map<String, Object> result = migrationService.verifyMigration();
//         return ResponseEntity.ok(result);
//     }

//     /**
//      * ✅ Get migration statistics
//      * GET /api/admin/migrations/user-approaches/stats
//      */
//     @GetMapping("/stats")
//     public ResponseEntity<Map<String, Object>> getMigrationStats() {
//         logger.info("📊 Getting migration statistics...");
        
//         Map<String, Object> stats = migrationService.getMigrationStats();
//         return ResponseEntity.ok(stats);
//     }
// }