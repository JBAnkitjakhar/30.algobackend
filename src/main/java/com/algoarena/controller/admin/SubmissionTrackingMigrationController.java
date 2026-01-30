// // src/main/java/com/algoarena/controller/admin/SubmissionTrackingMigrationController.java
// package com.algoarena.controller.admin;

// import com.algoarena.service.migration.SubmissionTrackingMigrationService;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.http.ResponseEntity;
// import org.springframework.security.access.prepost.PreAuthorize;
// import org.springframework.web.bind.annotation.*;

// import java.time.LocalDateTime;
// import java.util.Map;

// @RestController
// @RequestMapping("/admin/migrations/submission-tracking")
// @PreAuthorize("hasRole('SUPERADMIN')")
// public class SubmissionTrackingMigrationController {

//     private static final Logger logger = LoggerFactory.getLogger(SubmissionTrackingMigrationController.class);

//     @Autowired
//     private SubmissionTrackingMigrationService migrationService;

//     /**
//      * ✅ Migrate ALL users to simple date format (timezone-independent)
//      * POST /api/admin/migrations/submission-tracking/migrate-all-dates
//      * 
//      * This converts dates from DateTime format to simple "YYYY-MM-DD" strings
//      */
//     @PostMapping("/migrate-all-dates")
//     public ResponseEntity<Map<String, Object>> migrateAllUsersToSimpleDates() {
//         logger.info("🔍 ========================================");
//         logger.info("🔍 Date Format Migration: ALL Users!");
//         logger.info("🔍 Time: {}", LocalDateTime.now());
//         logger.info("🔍 ========================================");

//         Map<String, Object> result = migrationService.migrateAllUsersToSimpleDates();

//         if ((boolean) result.get("success")) {
//             return ResponseEntity.ok(result);
//         } else {
//             return ResponseEntity.status(500).body(result);
//         }
//     }

//     /**
//      * ✅ Verify specific user's submission history
//      * GET /api/admin/migrations/submission-tracking/verify/{userId}
//      * 
//      * Example: GET /api/admin/migrations/submission-tracking/verify/689d7a14c43a5e52aa3eb295
//      */
//     @GetMapping("/verify/{userId}")
//     public ResponseEntity<Map<String, Object>> verifyUser(@PathVariable String userId) {
//         logger.info("🔍 Verifying submission history for user: {}", userId);

//         Map<String, Object> result = migrationService.verifyUserSubmissions(userId);
//         return ResponseEntity.ok(result);
//     }

//     /**
//      * ✅ Get migration statistics for all users
//      * GET /api/admin/migrations/submission-tracking/stats
//      */
//     @GetMapping("/stats")
//     public ResponseEntity<Map<String, Object>> getMigrationStats() {
//         logger.info("📊 Getting submission tracking migration statistics...");

//         Map<String, Object> stats = migrationService.getMigrationStats();
//         return ResponseEntity.ok(stats);
//     }
// }